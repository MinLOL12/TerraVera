/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.temperature;

import java.util.Locale;

/**
 * What a garment is made of, and what that does for you.
 * <p>
 * The three numbers here are the whole of the clothing design, and they are chosen so that no material is simply
 * better than another:
 * <ul>
 *     <li><strong>insulation</strong> - how much it slows heat loss. Good in the cold, actively bad in the heat.</li>
 *     <li><strong>windProof</strong> - how well it stops moving air stripping the warm layer off your skin. Leather
 *     and oilskin are near-total; a loose knit is not, which is why a wool sweater under a leather jerkin is worth
 *     more than either alone.</li>
 *     <li><strong>breathability</strong> - how readily sweat evaporates through it. This is what makes linen and silk
 *     genuinely good hot-weather clothing and makes a fur parka a heat casualty waiting to happen.</li>
 * </ul>
 * Materials also differ in how badly they are hurt by getting wet. Wool famously keeps much of its insulation soaked
 * through; plant fibre and down do not.
 */
public enum ClothingMaterial
{
    /**
     * Nothing at all. Listed as a material so that "bare skin loses heat quickly" is a value in the same table as
     * everything else, rather than a special case somewhere in the tick loop.
     */
    BARE(0f, 0f, 1f, 1f, 0x000000),

    /**
     * Twisted grass, bast, and leaf matting. It is what you can make on day one and it is barely clothing: it stops
     * the wind a little and holds almost no heat.
     */
    PLANT_FIBER(0.14f, 0.20f, 0.90f, 1.0f, 0x9C8B4E),

    /** Woven straw. Nearly worthless against cold, but a wide straw brim is genuinely good sun protection. */
    STRAW(0.10f, 0.15f, 0.95f, 1.0f, 0xD8C271),

    /** Coarse jute sacking. A real fabric at last, if a scratchy and thin one. */
    BURLAP(0.26f, 0.35f, 0.75f, 0.95f, 0xA8875B),

    /** Woven flax. Light, cool, and fast-drying: the best thing to wear in a hot climate. */
    LINEN(0.22f, 0.30f, 0.98f, 0.55f, 0xE0DAC4),

    /** Knitted wool. The cold-weather milestone, and the one fabric that still works when soaked. */
    WOOL(0.58f, 0.45f, 0.60f, 0.45f, 0xC9BFA8),

    /** Wool matted into a dense sheet. Warmer and far more windproof than a knit, but it does not breathe. */
    FELT(0.68f, 0.72f, 0.35f, 0.55f, 0x8E8574),

    /** Tanned hide. Modest warmth, but it stops wind and weather almost completely. */
    LEATHER(0.34f, 0.92f, 0.25f, 0.60f, 0x7A5230),

    /** Leather worked with oil or wax. The pre-industrial answer to rain: it simply does not wet through. */
    OILSKIN(0.38f, 0.98f, 0.15f, 0.12f, 0x4F4636),

    /** Hide with the fur left on. The warmest thing available, and unwearable anywhere warm. */
    FUR(0.86f, 0.80f, 0.30f, 0.80f, 0x6B4A32),

    /** Woven silk. Light, strong, and unusually good at both retaining warmth and shedding it. */
    SILK(0.30f, 0.55f, 0.92f, 0.65f, 0xEFE6DA),

    /**
     * Layered fabric stitched with a trapped-air filling. The best cold-weather clothing that can be made without
     * synthetics, and the payoff for a full textile chain.
     */
    QUILTED(0.78f, 0.70f, 0.45f, 0.70f, 0x8A6E56);

    private final float insulation;
    private final float windProof;
    private final float breathability;
    private final float wetPenalty;
    private final int colour;

    ClothingMaterial(float insulation, float windProof, float breathability, float wetPenalty, int colour)
    {
        this.insulation = insulation;
        this.windProof = windProof;
        this.breathability = breathability;
        this.wetPenalty = wetPenalty;
        this.colour = colour;
    }

    /** Insulation contributed by a full-coverage garment of this material, before slot scaling. */
    public float insulation()
    {
        return insulation;
    }

    /** How much of the wind this material blocks, in {@code [0, 1]}. */
    public float windProof()
    {
        return windProof;
    }

    /** How freely sweat evaporates through it, in {@code [0, 1]}. */
    public float breathability()
    {
        return breathability;
    }

    /**
     * How much of this material's insulation is lost when it is soaked, in {@code [0, 1]}. Wool is the famous
     * outlier at less than half; oilskin barely wets at all.
     */
    public float wetPenalty()
    {
        return wetPenalty;
    }

    /** How readily this material soaks up water in the first place. Oiled and tight weaves shed most of it. */
    public float absorbency()
    {
        return Math.max(0.05f, wetPenalty);
    }

    public int colour()
    {
        return colour;
    }

    public String id()
    {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey()
    {
        return "terravera.clothing.material." + id();
    }
}
