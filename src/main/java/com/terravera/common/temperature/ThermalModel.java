/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.temperature;

/**
 * The physics half of the body temperature system, expressed as plain arithmetic on floats.
 * <p>
 * Nothing in this class touches a Minecraft type, which is deliberate: the balance of the whole system lives here and
 * can therefore be unit tested directly. Everything that has to look at the world - the weather, what you are wearing,
 * whether you are standing in a hut with a fire in it - lives in the classes around it and ends up here as a handful
 * of numbers.
 *
 * <h2>The model</h2>
 * A human is treated as a mass at some core temperature, sitting inside an insulating shell, producing heat and
 * losing it to the environment:
 * <pre>
 *   heat produced  = basal metabolism x activity
 *   heat lost      = (core - felt ambient) / (skin resistance + clothing insulation)
 *   dCore/dt       = (produced - lost) x thermal inertia
 * </pre>
 * The important consequences fall out of that single equation rather than being special-cased:
 * <ul>
 *     <li><strong>Nothing is instant.</strong> Core temperature has inertia. Stepping outside into a blizzard does not
 *     hurt you; standing in it for ten minutes does, and you get several minutes of warning symptoms first.</li>
 *     <li><strong>Insulation is a divisor, not a bonus.</strong> Doubling your clothing halves your heat loss, so the
 *     first layer matters enormously and the fifth barely at all - which is why wool is a milestone and a sixth coat
 *     is not.</li>
 *     <li><strong>Clothing is symmetric.</strong> The same term that keeps heat in during a winter night traps it
 *     during summer work. Heavy clothing in a hot climate is actively dangerous, with no extra rule needed.</li>
 *     <li><strong>Work is heat.</strong> Mining and running raise the metabolic term, which is why you overheat
 *     chopping wood in a coat and freeze the moment you stop.</li>
 * </ul>
 */
public final class ThermalModel
{
    /** Normal human core temperature, in degrees Celsius. */
    public static final float NEUTRAL_CORE = 37.0f;

    /** Basal heat production, in the model's arbitrary flux units, for a player standing still. */
    public static final float BASAL_METABOLISM = 22f;

    /**
     * Thermal resistance of bare skin. With no clothing at all this puts the comfortable ambient temperature in the
     * low twenties Celsius, which is about right for an unclothed human at rest.
     */
    public static final float BARE_SKIN_RESISTANCE = 0.55f;

    /**
     * How much a full outfit at maximum insulation multiplies the body's thermal resistance.
     * <p>
     * Clothing insulation is quoted in {@code [0, 1]} because that is a legible scale to author a material table on,
     * but the underlying physics is a multiple of bare-skin resistance: real clothing runs from about 0.7 clo (naked)
     * to 4 clo (heavy winter kit). This constant is what maps one onto the other, and it is the single knob that
     * decides how far a good coat gets you. At 3.0, a full wool outfit is comfortable at rest down to about -6 C and
     * a fur parka to around -19 C, which is roughly where real garments sit.
     */
    public static final float CLOTHING_RESISTANCE_SCALE = 3.0f;

    /**
     * How fast core temperature responds to an imbalance, in degrees per flux unit per second.
     * <p>
     * This is the constant that enforces the design's central promise. It is tuned so that an unclothed player in a
     * freezing gale - the worst case in the game - takes over three minutes to reach the first impairing band and the
     * better part of ten to reach hypothermia. There is no environment anywhere in which cold or heat is sudden.
     */
    public static final float THERMAL_INERTIA = 0.000035f;

    /** Above this core temperature the body starts sweating in earnest. */
    public static final float SWEAT_THRESHOLD = 37.4f;

    /** Below this core temperature the body starts shivering, which produces heat at a cost. */
    public static final float SHIVER_THRESHOLD = 36.3f;

    /** Shivering is worth roughly a third again of basal metabolism, and costs food to sustain. */
    public static final float SHIVER_HEAT = 8f;

    /**
     * The most heat full-tilt sweating can shed, in flux units.
     * <p>
     * A working human can evaporate several times their basal heat output, which is precisely why people can labour
     * in genuinely hot places at all - and why the whole thing collapses the moment they run out of water or put on
     * something that will not breathe. It is set high on purpose: heat should be survivable through <em>behaviour</em>
     * (shade, hydration, the right clothes) rather than simply being lethal above some temperature.
     */
    public static final float MAX_SWEAT_COOLING = 70f;

    private ThermalModel() {}

    // ----- Environment ------------------------------------------------------------------------------------------

    /**
     * The temperature the body actually experiences, as distinct from what a thermometer in a box would read.
     *
     * @param ambientC     dry-bulb air temperature at the player, in Celsius
     * @param windSpeed    wind exposure in {@code [0, 1]}: 0 is still air or fully sheltered, 1 is an open gale
     * @param wetness      how wet the player and their clothing are, in {@code [0, 1]}
     * @param sunExposure  direct sunlight on the player in {@code [0, 1]}; 0 is full shade, 1 is midday open sky
     * @param immersion    fraction of the body in water, in {@code [0, 1]}
     * @return the felt temperature in Celsius
     */
    public static float feltTemperature(float ambientC, float windSpeed, float wetness, float sunExposure, float immersion)
    {
        float felt = ambientC;

        // Wind chill only exists when the air is colder than the skin, and it is much worse when you are wet -
        // evaporation off a soaked shirt is the classic way to become hypothermic at temperatures well above freezing.
        if (ambientC < 33f)
        {
            final float gradient = 33f - ambientC;
            felt -= gradient * windSpeed * (0.28f + 0.35f * wetness);
            felt -= gradient * wetness * 0.16f;
        }
        else
        {
            // In genuinely hot air, moving air and damp skin help instead of hurting.
            felt -= (ambientC - 33f) * (windSpeed * 0.15f + wetness * 0.25f);
        }

        // Standing in the sun is worth several degrees; standing in the shade of a tree on the same afternoon is not.
        felt += sunExposure * 6.5f;

        // Water conducts heat away from a body roughly twenty times faster than air. Being in it is not "cold air".
        if (immersion > 0f)
        {
            felt = felt + (ambientC - felt) * (1f - immersion); // wind and sun stop mattering once you are under
            felt -= immersion * Math.max(0f, 30f - ambientC) * 0.55f;
        }
        return felt;
    }

    /**
     * Total insulation of what the player is wearing, after the wet-clothing penalty.
     * <p>
     * Wet insulation is the single most important survival lesson in the cold: a soaked wool coat retains only a
     * fraction of its dry value, which is why drying your clothes is a real decision and not busywork.
     *
     * @param dryInsulation summed insulation of the worn garments
     * @param wetness       how wet those garments are, in {@code [0, 1]}
     * @return effective insulation
     */
    public static float effectiveInsulation(float dryInsulation, float wetness)
    {
        // A soaked garment keeps about a quarter of its dry insulation. Down/plant fibre would be worse and wool
        // better in reality; the per-material differences are applied before this, in the garment values themselves.
        return dryInsulation * (1f - 0.75f * clamp(wetness, 0f, 1f));
    }

    /**
     * Heat lost to the environment per second, in flux units.
     *
     * @param core       current core temperature
     * @param felt       felt ambient temperature
     * @param insulation effective clothing insulation
     * @param windLeak   how much wind gets through the clothing, in {@code [0, 1]}; leather and tight weaves cut this
     */
    public static float heatLoss(float core, float felt, float insulation, float windLeak)
    {
        return (core - felt) / resistance(insulation, windLeak);
    }

    /**
     * The body's total thermal resistance: bare skin, multiplied up by clothing, then cut back by whatever wind is
     * getting through it.
     * <p>
     * Because insulation multiplies rather than adds, the first layer is worth far more than the fifth. That is both
     * physically true and the reason a single wool sweater is a milestone while a sixth coat is not.
     */
    public static float resistance(float insulation, float windLeak)
    {
        final float clothed = BARE_SKIN_RESISTANCE * (1f + CLOTHING_RESISTANCE_SCALE * Math.max(0f, insulation));
        return Math.max(0.12f, clothed * (1f - 0.35f * clamp(windLeak, 0f, 1f)));
    }

    /**
     * Heat produced per second, in flux units.
     *
     * @param activity     work rate in {@code [0, 1]}: 0 is sitting still, 1 is sustained heavy labour
     * @param shivering    whether the body is shivering
     * @param feverOffset  extra heat from illness, in flux units
     */
    public static float heatProduced(float activity, boolean shivering, float feverOffset)
    {
        return BASAL_METABOLISM * (1f + 1.9f * clamp(activity, 0f, 1f))
            + (shivering ? SHIVER_HEAT : 0f)
            + Math.max(0f, feverOffset);
    }

    /**
     * How much extra heat sweating can shed, in flux units.
     * <p>
     * Sweat only cools you if it can evaporate. That single fact is what makes heavy clothing dangerous in a hot
     * climate for a reason beyond its insulation: a fur parka does not merely trap heat, it stops the body's only
     * active cooling mechanism from working at all. Breathability is therefore part of the model rather than a
     * modifier applied outside it.
     *
     * @param core          current core temperature
     * @param wetness       existing wetness, which limits further evaporation
     * @param hydration     how much water the player has left, in {@code [0, 1]}
     * @param breathability how freely sweat can escape the clothing, in {@code [0, 1]}
     */
    public static float sweatCooling(float core, float wetness, float hydration, float breathability)
    {
        if (core <= SWEAT_THRESHOLD) return 0f;
        final float drive = Math.min(1f, (core - SWEAT_THRESHOLD) / 1.6f);
        // Even sealed in oilskin some evaporation happens at the neck and cuffs, hence the floor rather than zero.
        final float escape = 0.15f + 0.85f * clamp(breathability, 0f, 1f);
        return MAX_SWEAT_COOLING * drive * (1f - 0.6f * clamp(wetness, 0f, 1f)) * clamp(hydration, 0f, 1f) * escape;
    }

    /**
     * Advances core temperature by {@code seconds}.
     *
     * @param core     current core temperature
     * @param produced heat produced per second
     * @param lost     heat lost per second
     * @param seconds  elapsed time
     * @return the new core temperature, clamped to a survivable-to-report range
     */
    public static float step(float core, float produced, float lost, float seconds)
    {
        final float next = core + (produced - lost) * THERMAL_INERTIA * seconds;
        return clamp(next, 27f, 43f);
    }

    /**
     * The ambient temperature at which this much insulation, at this work rate, is exactly comfortable.
     * <p>
     * This is the number that actually matters to a player planning a trip: "what can I survive in what I am
     * wearing". It is exposed so the field notes and tooltips can answer that question honestly.
     */
    public static float comfortableAmbient(float insulation, float activity)
    {
        // The equilibrium of the same equation the tick loop runs: produced == lost. Because it is derived from the
        // model rather than tabulated beside it, the number the field notes quote is guaranteed to be true.
        return NEUTRAL_CORE - heatProduced(activity, false, 0f) * resistance(insulation, 0f);
    }

    // ----- Symptoms ---------------------------------------------------------------------------------------------

    /**
     * The observable state of the body at a given core temperature.
     * <p>
     * Following the disease and taste systems, the player is never shown a number or a bar. They are told what their
     * body is doing, and they work out the rest.
     */
    public enum Band
    {
        /** Below 33.5 C. Confusion, exhaustion, real danger. */
        HYPOTHERMIA(-4, "hypothermia"),
        /** 33.5-34.8 C. "You are struggling to stay warm." */
        SEVERE_COLD(-3, "severe_cold"),
        /** 34.8-35.8 C. "Your hands feel stiff." */
        MODERATE_COLD(-2, "moderate_cold"),
        /** 35.8-36.5 C. "You feel slightly chilled." */
        MILD_COLD(-1, "mild_cold"),
        /** 36.5-37.6 C. Nothing to report. */
        COMFORTABLE(0, "comfortable"),
        /** 37.6-38.4 C. "You are sweating." */
        MILD_HEAT(1, "mild_heat"),
        /** 38.4-39.3 C. "You feel overheated." */
        MODERATE_HEAT(2, "moderate_heat"),
        /** 39.3-40.2 C. Heat exhaustion. */
        SEVERE_HEAT(3, "severe_heat"),
        /** Above 40.2 C. Heat stroke. */
        HEAT_STROKE(4, "heat_stroke");

        private final int severity;
        private final String id;

        Band(int severity, String id)
        {
            this.severity = severity;
            this.id = id;
        }

        /** Negative for cold, positive for heat, zero for comfortable. Magnitude is how bad it is. */
        public int severity()
        {
            return severity;
        }

        public String id()
        {
            return id;
        }

        public boolean isCold()
        {
            return severity < 0;
        }

        public boolean isHot()
        {
            return severity > 0;
        }

        /** @return {@code true} if this band should nag the player about it. */
        public boolean isNotable()
        {
            return this != COMFORTABLE;
        }

        public String messageKey()
        {
            return "terravera.temperature." + id;
        }

        public String descriptorKey()
        {
            return "terravera.temperature.state." + id;
        }
    }

    /**
     * Bands are deliberately asymmetric and narrower on the hot side, because human beings tolerate a couple of
     * degrees of cooling far better than a couple of degrees of warming.
     */
    public static Band band(float core)
    {
        if (core < 33.5f) return Band.HYPOTHERMIA;
        if (core < 34.8f) return Band.SEVERE_COLD;
        if (core < 35.8f) return Band.MODERATE_COLD;
        if (core < 36.5f) return Band.MILD_COLD;
        if (core <= 37.6f) return Band.COMFORTABLE;
        if (core <= 38.4f) return Band.MILD_HEAT;
        if (core <= 39.3f) return Band.MODERATE_HEAT;
        if (core <= 40.2f) return Band.SEVERE_HEAT;
        return Band.HEAT_STROKE;
    }

    /**
     * Hysteresis for messages. Once the player has been told they are cold, they should not be told again the moment
     * the value wobbles across a boundary - only when it genuinely gets worse, or clearly recovers.
     */
    public static boolean shouldAnnounce(Band previous, Band current)
    {
        if (previous == current) return false;
        // Recovering to comfortable is always worth saying once - it is the "you can stop worrying" signal.
        if (current == Band.COMFORTABLE) return true;
        // Otherwise only speak up when things get worse, or when the player crosses from cold to hot (or back),
        // which is a genuinely different problem needing a different response.
        return Math.abs(current.severity()) > Math.abs(previous.severity())
            || Integer.signum(current.severity()) != Integer.signum(previous.severity());
    }

    static float clamp(float value, float min, float max)
    {
        return value < min ? min : Math.min(value, max);
    }
}
