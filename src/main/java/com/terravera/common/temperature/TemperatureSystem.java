/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.temperature;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec2;

import net.dries007.tfc.common.player.IPlayerInfo;
import net.dries007.tfc.util.climate.Climate;

import com.terravera.common.TerraVeraAttachments;
import com.terravera.common.TerraVeraDataComponents;
import com.terravera.config.TerraVeraConfig;

/**
 * The body temperature system: everything that has to look at the world.
 * <p>
 * The arithmetic lives in {@link ThermalModel}, which knows nothing about Minecraft and is unit tested. This class is
 * the bridge: once per interval it reads the climate, the sky, the weather, the wind, the water, the building, and
 * the player's clothing, boils all of it down to a handful of floats, and hands them to the model.
 *
 * <h2>Design commitments</h2>
 * <ul>
 *     <li><strong>No cold bar.</strong> The player is never shown a number. They are told "your hands feel stiff" and
 *     left to work out what to do about it - the same contract as the disease and taste systems.</li>
 *     <li><strong>No instant damage.</strong> Cold and heat never hurt the player directly on contact. Core
 *     temperature moves slowly; discomfort comes first, impairment second, and actual harm only at the far ends after
 *     the player has ignored several minutes of increasingly urgent symptoms.</li>
 *     <li><strong>Everything is a consequence.</strong> Wet clothing is not a debuff, it is a lower value in the
 *     insulation term. Heavy clothing in the desert is not punished, it simply also works in the desert.</li>
 * </ul>
 */
public final class TemperatureSystem
{
    /** How often the full simulation runs, in ticks. One second is plenty for a system measured in minutes. */
    public static final int TICK_INTERVAL = 20;
    /** How often the (relatively expensive) shelter survey is refreshed. */
    private static final int SHELTER_INTERVAL = 60;
    /** Minimum ticks between symptom messages, so the player is not nagged. */
    private static final int MESSAGE_COOLDOWN = 300;

    /** Cached shelter survey per player, so the flood fill does not run every second. */
    private static final java.util.Map<java.util.UUID, CachedShelter> SHELTER_CACHE =
        java.util.Collections.synchronizedMap(new java.util.HashMap<>());

    private record CachedShelter(Shelter shelter, long tick, long pos) {}

    private TemperatureSystem() {}

    // ----- Accessors ---------------------------------------------------------------------------------------------

    public static BodyTemperature get(Player player)
    {
        return player.getData(TerraVeraAttachments.BODY_TEMPERATURE.get());
    }

    public static void set(Player player, BodyTemperature state)
    {
        player.setData(TerraVeraAttachments.BODY_TEMPERATURE.get(), state);
    }

    public static void addStamina(Player player, float amount)
    {
        BodyTemperature state = get(player);
        state = state.withStamina(Mth.clamp(state.stamina() + amount, 0f, 100f));
        set(player, state);
    }

    // ----- The tick ----------------------------------------------------------------------------------------------

    /**
     * Advances the player's thermal state by one interval.
     * <p>
     * Read top to bottom, this is the whole system: gather the environment, gather what is worn, produce and lose
     * heat, integrate, then report symptoms.
     */
    public static void tick(Player player)
    {
        final Level level = player.level();
        if (level.isClientSide() || player.isCreative() || player.isSpectator()) return;
        if (!TerraVeraConfig.SERVER.enableBodyTemperature.get()) return;
        if (player.tickCount % TICK_INTERVAL != 0) return;

        BodyTemperature state = get(player);
        final BlockPos pos = player.blockPosition();

        // --- 1. What the environment is doing ---
        final Shelter shelter = shelter(player, level, pos);
        final float ambient = ambientTemperature(level, pos);
        final float averageAmbient = averageTemperature(level, pos);
        final float immersion = immersion(player);
        final float rain = rainExposure(level, pos, shelter);
        final float wind = windExposure(level, pos, shelter, immersion);
        final float sun = sunExposure(level, pos, shelter);

        // --- 2. What the player is wearing, and how wet it is ---
        final Garments garments = garments(player);
        updateGarmentWetness(player, shelter, ambient, rain, immersion, level.getGameTime());

        // Skin wetness is separate from clothing wetness: you can be soaked under a dry oilskin, or bone dry inside a
        // wet wool coat that is doing its job.
        state = state.withSkinWetness(updateSkinWetness(state.skinWetness(), rain, immersion, garments, shelter, ambient));

        // --- 3. What the player is doing ---
        final float exertion = updateExertion(player, state.exertion());
        state = state.withExertion(exertion);

        // --- 4. Run the model ---
        float felt = ThermalModel.feltTemperature(ambient, wind, state.skinWetness(), sun, immersion);
        felt = shelter.isIndoors() ? shelter.moderate(felt, averageAmbient) : felt + shelter.openFireWarmth();
        // Vapor-compression cooling changes room air by moving heat through the condenser; it never edits core heat.
        felt = com.terravera.common.climate.ClimateControlSystem.condition(level, pos, shelter, felt);
        // Being in water means the fire on the bank is not warming you, whatever the survey said.
        if (immersion > 0.6f) felt = Math.min(felt, ambient);

        final float insulation = ThermalModel.effectiveInsulation(garments.insulation, garments.wetness);
        final float windLeak = Mth.clamp(1f - garments.windProof, 0f, 1f) * wind;

        final boolean shivering = state.core() < ThermalModel.SHIVER_THRESHOLD;
        final float hydration = hydration(player);

        float produced = ThermalModel.heatProduced(exertion, shivering, feverHeat(player));
        float lost = ThermalModel.heatLoss(state.core(), felt, insulation, windLeak);

        // Sweat is throttled by how well the clothing breathes - which is what makes a fur parka genuinely dangerous
        // in a hot climate rather than merely suboptimal.
        final float sweat = ThermalModel.sweatCooling(state.core(),
            Math.max(state.skinWetness(), garments.wetness), hydration, garments.breathability);
        lost += sweat;

        final float seconds = (TICK_INTERVAL / 20f)
            * TerraVeraConfig.SERVER.temperatureRateMultiplier.get().floatValue();
        state = state.withCore(ThermalModel.step(state.core(), produced, lost, seconds));

        // --- 5. Costs and consequences ---
        applyPhysiology(player, state, shivering, sweat, seconds);
        state = report(player, state);

        set(player, state);
    }

    // ----- Environment -------------------------------------------------------------------------------------------

    /**
     * The air temperature at the player, from TFC's climate model. That already folds in biome climate, latitude,
     * altitude, season, and time of day, so TerraVera does not re-derive any of it - it would only disagree with the
     * thermometer the player is already reading.
     */
    public static float ambientTemperature(Level level, BlockPos pos)
    {
        try
        {
            return Climate.getInstantTemperature(level, pos);
        }
        catch (RuntimeException fallback)
        {
            // A dimension without a TFC climate model (or a unit-test level) still needs an answer.
            return vanillaTemperature(level, pos);
        }
    }

    /** The local annual mean, which is what heavy stone buildings settle towards. */
    public static float averageTemperature(Level level, BlockPos pos)
    {
        try
        {
            return Climate.getAverageTemperature(level, pos);
        }
        catch (RuntimeException fallback)
        {
            return vanillaTemperature(level, pos);
        }
    }

    private static float vanillaTemperature(Level level, BlockPos pos)
    {
        return (level.getBiome(pos).value().getBaseTemperature() - 0.15f) / 0.0217f;
    }

    /** How much of the player is under water. Deep water is a different problem from wet feet. */
    private static float immersion(Player player)
    {
        if (player.isUnderWater()) return 1f;
        if (player.isInWater()) return player.isSwimming() ? 0.9f : 0.45f;
        if (player.isInWaterOrRain() && player.getFluidHeight(FluidTags.WATER) > 0.1) return 0.25f;
        return 0f;
    }

    /** How much rain is actually landing on the player. A roof stops it; so does standing under a tree. */
    private static float rainExposure(Level level, BlockPos pos, Shelter shelter)
    {
        if (!level.isRainingAt(pos)) return 0f;
        return Mth.clamp(1f - shelter.enclosure(), 0f, 1f) * (level.isThundering() ? 1f : 0.75f);
    }

    /**
     * Wind at the player, after shelter. TFC's climate model carries a real wind vector; where it is absent, exposure
     * is inferred from how much open sky and open air is around, which is the same thing a player would judge by eye.
     */
    private static float windExposure(Level level, BlockPos pos, Shelter shelter, float immersion)
    {
        if (immersion > 0.8f) return 0f; // under water there is no wind

        float wind;
        try
        {
            final Vec2 vector = Climate.get(level).getWind(level, pos);
            wind = Mth.clamp(vector.length(), 0f, 1f);
        }
        catch (RuntimeException fallback)
        {
            wind = 0.35f;
        }

        // Exposure: open ground in a storm is a gale, a hollow in the lee of a hill is not.
        if (!level.canSeeSky(pos)) wind *= 0.45f;
        if (level.isRaining()) wind = Math.min(1f, wind + 0.2f);
        if (level.isThundering()) wind = Math.min(1f, wind + 0.25f);

        // Altitude: it is always windier on a ridge.
        wind = Math.min(1f, wind + Mth.clampedMap(pos.getY(), 90, 200, 0f, 0.3f));

        return Mth.clamp(wind * (1f - shelter.windShelter()), 0f, 1f);
    }

    /**
     * Direct sun on the player. Sunlight is worth several degrees in the open, nothing in shade, and nothing at
     * night - which is what makes shade a real resource in a hot climate and a clear night colder than a cloudy one.
     */
    private static float sunExposure(Level level, BlockPos pos, Shelter shelter)
    {
        if (!level.canSeeSky(pos) || shelter.isIndoors()) return 0f;
        if (level.isRaining()) return 0.05f;

        // Sky light accounts for being under leaves or an overhang; sky darken accounts for the time of day. The
        // two together are why noon in a clearing is hot, noon under a canopy is not, and midnight never is.
        final float sky = level.getBrightness(LightLayer.SKY, pos) / 15.0f;
        final float daylight = Mth.clampedMap(level.getSkyDarken(), 4f, 0f, 0f, 1f);
        return Mth.clamp(sky * daylight, 0f, 1f);
    }

    // ----- Clothing ----------------------------------------------------------------------------------------------

    /** The summed thermal properties of what the player is wearing. */
    private record Garments(float insulation, float windProof, float breathability, float wetness, float coverage) {}

    /**
     * Sums the worn garments.
     * <p>
     * TerraVera clothing carries its material explicitly. Anything else worn in an armour slot - a TFC or vanilla
     * breastplate, another mod's coat - is given a conservative estimate from its own properties, so wearing plate
     * mail in the snow is neither a free win nor a bug.
     */
    private static Garments garments(Player player)
    {
        float insulation = 0f;
        float windProof = 0f;
        float breathability = 0f;
        float wetnessWeighted = 0f;
        float coverage = 0f;

        for (GarmentSlot slot : GarmentSlot.values())
        {
            final ItemStack stack = player.getItemBySlot(slot.type().getSlot());
            if (stack.isEmpty())
            {
                // Bare skin. Fully breathable, no insulation, no wind protection: the model's default.
                breathability += ClothingMaterial.BARE.breathability() * slot.coverage();
                continue;
            }

            coverage += slot.coverage();

            if (stack.getItem() instanceof ClothingItem clothing)
            {
                insulation += clothing.insulation();
                windProof += clothing.windProofing();
                breathability += clothing.breathability();

                final Wetness wet = stack.get(TerraVeraDataComponents.GARMENT_WETNESS.get());
                wetnessWeighted += (wet == null ? 0f : wet.wetness()) * slot.coverage();
            }
            else
            {
                final Estimate estimate = estimate(stack);
                insulation += estimate.insulation() * slot.coverage();
                windProof += estimate.windProof() * slot.coverage();
                breathability += estimate.breathability() * slot.coverage();

                final Wetness wet = stack.get(TerraVeraDataComponents.GARMENT_WETNESS.get());
                wetnessWeighted += (wet == null ? 0f : wet.wetness()) * slot.coverage();
            }
        }
        return new Garments(insulation, windProof, breathability,
            coverage > 0f ? wetnessWeighted / Math.max(coverage, 0.001f) : 0f, coverage);
    }

    /** The three numbers the model needs from any worn item, whether or not TerraVera made it. */
    private record Estimate(float insulation, float windProof, float breathability) {}

    /**
     * Best guess at the thermal behaviour of armour TerraVera did not make.
     * <p>
     * Metal plate is the interesting case, and the honest answer is that it is bad in both directions: steel is a
     * conductor, not an insulator, and a sealed harness does not breathe - so it neither keeps you warm in winter
     * nor lets you shed heat in summer, while still keeping the wind off. Soft armour is matched to the equivalent
     * TerraVera material by name, so another mod's wool cloak behaves like wool.
     */
    private static Estimate estimate(ItemStack stack)
    {
        if (!(stack.getItem() instanceof ArmorItem)) return of(ClothingMaterial.PLANT_FIBER);

        final String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (id.contains("leather") || id.contains("hide")) return of(ClothingMaterial.LEATHER);
        if (id.contains("wool") || id.contains("felt")) return of(ClothingMaterial.WOOL);
        if (id.contains("fur")) return of(ClothingMaterial.FUR);
        if (id.contains("silk")) return of(ClothingMaterial.SILK);
        if (id.contains("cloth") || id.contains("burlap") || id.contains("linen")) return of(ClothingMaterial.BURLAP);
        if (id.contains("quilt") || id.contains("gambeson") || id.contains("padded")) return of(ClothingMaterial.QUILTED);

        // Rigid metal: windproof, airless, and barely warmer than bare skin.
        return new Estimate(0.06f, 0.95f, 0.10f);
    }

    private static Estimate of(ClothingMaterial material)
    {
        return new Estimate(material.insulation(), material.windProof(), material.breathability());
    }

    /**
     * Rain and immersion wet your clothes; fire, sun, and time dry them again.
     * <p>
     * This is stored per garment, on the item, which is what makes "dry your clothes after the rain" and "carry a
     * spare set" into actual decisions rather than advice.
     */
    private static void updateGarmentWetness(Player player, Shelter shelter, float ambient, float rain,
                                             float immersion, long tick)
    {
        if (!TerraVeraConfig.SERVER.enableWetClothing.get()) return;

        for (GarmentSlot slot : GarmentSlot.values())
        {
            final ItemStack stack = player.getItemBySlot(slot.type().getSlot());
            if (stack.isEmpty()) continue;

            // How readily this garment takes water and gives it back up again. Oilskin barely wets; wool takes a
            // long time to dry; anything else worn in an armour slot is treated as an ordinary damp-able thing.
            final float absorbency;
            final float breathability;
            if (stack.getItem() instanceof ClothingItem clothing)
            {
                absorbency = clothing.material().absorbency();
                breathability = clothing.material().breathability();
            }
            else
            {
                final Estimate estimate = estimate(stack);
                absorbency = 0.8f;
                breathability = estimate.breathability();
            }

            final Wetness wetness = stack.getOrDefault(TerraVeraDataComponents.GARMENT_WETNESS.get(), Wetness.DRY);
            final float before = wetness.wetness();

            // Boots get soaked by standing in a puddle; a hat does not.
            final float slotImmersion = slot == GarmentSlot.FEET ? Math.min(1f, immersion * 2.5f) : immersion;
            final float soaking = (rain * 0.010f + slotImmersion * 0.045f) * absorbency;

            // Drying is faster by a fire, in the sun, in dry air, and in a breathable fabric.
            float drying = 0.0035f;
            if (shelter.hasFire()) drying += 0.020f;
            if (shelter.isIndoors()) drying += 0.004f;
            if (ambient > 20f) drying += 0.006f;
            if (ambient < 0f) drying *= 0.35f;   // nothing dries in a hard frost
            drying *= 0.5f + 0.5f * breathability;

            // Both apply. Standing in the rain by a roaring fire is a stalemate, which is the right answer.
            final float after = Mth.clamp(before + soaking - drying, 0f, 1f);
            if (Math.abs(after - before) > 0.001f)
            {
                stack.set(TerraVeraDataComponents.GARMENT_WETNESS.get(), new Wetness(after, tick));
            }
        }
    }

    /** Skin wetness: soaked by rain and water, dried by fire, sun, warmth, and by sweating it off. */
    private static float updateSkinWetness(float current, float rain, float immersion, Garments garments,
                                           Shelter shelter, float ambient)
    {
        // Clothing keeps rain off the skin, but only if it sheds water; a soaked shirt is worse than no shirt.
        final float shed = Mth.clamp(garments.windProof * (1f - garments.wetness), 0f, 1f);
        float next = current + rain * 0.05f * (1f - shed) + immersion * 0.18f;

        float drying = 0.012f;
        if (shelter.hasFire()) drying += 0.05f;
        if (ambient > 22f) drying += 0.02f;
        if (ambient < 0f) drying *= 0.4f;
        // Drying always applies. It just loses to a downpour, which is the point: you dry off by getting out of it.
        next -= drying;

        return Mth.clamp(next, 0f, 1f);
    }

    // ----- Activity ----------------------------------------------------------------------------------------------

    /**
     * A smoothed measure of how hard the player is working.
     * <p>
     * Smoothing matters: a body does not cool the instant you stop swinging an axe, and it is precisely the interval
     * between stopping work and cooling down - soaked in your own sweat, in a cold wind - that kills people. Exertion
     * decays slowly so that moment exists in game too.
     */
    private static float updateExertion(Player player, float current)
    {
        float target = 0.05f;
        if (player.isSprinting()) target = 0.85f;
        else if (player.isSwimming()) target = 0.75f;
        else if (player.walkDist - player.walkDistO > 0.02f) target = 0.35f;
        if (player.swinging) target = Math.max(target, 0.7f);
        if (player.isSleeping() || player.isPassenger()) target = 0f;

        // Carrying a heavy load is work even standing still - approximated by how full the inventory is, which is a
        // reasonable stand-in for TFC's weight rules without duplicating them.
        target += Mth.clamp(occupiedSlots(player) / 36f, 0f, 1f) * 0.08f;

        // Rise fast, fall slow.
        final float rate = target > current ? 0.35f : 0.06f;
        return Mth.clamp(current + (target - current) * rate, 0f, 1f);
    }

    private static int occupiedSlots(Player player)
    {
        int count = 0;
        for (int i = 0; i < player.getInventory().items.size(); i++)
        {
            if (!player.getInventory().items.get(i).isEmpty()) count++;
        }
        return count;
    }

    /** How much water the player has, in {@code [0, 1]}. Sweating needs water, so heat and thirst are linked. */
    private static float hydration(Player player)
    {
        try
        {
            return Mth.clamp(IPlayerInfo.get(player).getThirst() / 100f, 0f, 1f);
        }
        catch (RuntimeException fallback)
        {
            return 1f;
        }
    }

    /** A fever is extra heat production, so being ill in the desert is genuinely worse than being ill at home. */
    private static float feverHeat(Player player)
    {
        final var fever = player.getEffect(com.terravera.common.health.effect.TerraVeraEffects.FEVER);
        return fever == null ? 0f : 6f * (fever.getAmplifier() + 1);
    }

    // ----- Consequences ------------------------------------------------------------------------------------------

    /**
     * The physiological costs of thermoregulating.
     * <p>
     * Note what is <em>not</em> here: there is no "you are cold, take damage". Shivering costs food, sweating costs
     * water, and impairment is applied as effects by {@link TemperatureEffects}. Only the two extreme bands do direct
     * harm, and by the time a player reaches either they have had minutes of escalating warnings.
     */
    private static void applyPhysiology(Player player, BodyTemperature state, boolean shivering, float sweat, float seconds)
    {
        // Shivering is expensive. It is the body burning calories to make heat, and it is why you get hungry in the
        // cold long before you get hurt by it.
        if (shivering)
        {
            player.causeFoodExhaustion(0.035f * seconds);
        }

        // Sweating costs water. In a hot climate this is the real constraint: you can survive the heat only as long
        // as you can keep drinking.
        if (sweat > 0f)
        {
            try
            {
                IPlayerInfo.get(player).addThirst(-sweat * 0.010f * seconds);
            }
            catch (RuntimeException ignored)
            {
                // No TFC player info (e.g. in a test harness). Thirst simply is not modelled there.
            }
        }

        TemperatureEffects.apply(player, state.band());
    }

    /**
     * Tells the player what their body is doing.
     * <p>
     * Messages are on the action bar, rate limited, and only sent when the situation genuinely changes - the same
     * restraint the illness onset messages use. A player who is coping should hear nothing at all.
     */
    private static BodyTemperature report(Player player, BodyTemperature state)
    {
        final ThermalModel.Band band = state.band();
        final ThermalModel.Band previous = state.lastAnnouncedBand();
        if (!ThermalModel.shouldAnnounce(previous, band)) return state;

        final long now = player.level().getGameTime();
        if (now - state.lastMessage() < MESSAGE_COOLDOWN && band.severity() != 0
            && Math.abs(band.severity()) <= Math.abs(previous.severity())) return state;

        if ((band.isNotable() || previous.isNotable()) && TerraVeraConfig.SERVER.showTemperatureSymptoms.get())
        {
            player.displayClientMessage(
                Component.translatable(band.messageKey()).withStyle(colour(band)), true);
        }
        return state.announced(band, now);
    }

    public static ChatFormatting colour(ThermalModel.Band band)
    {
        return switch (band)
        {
            case HYPOTHERMIA, HEAT_STROKE -> ChatFormatting.DARK_RED;
            case SEVERE_COLD -> ChatFormatting.BLUE;
            case MODERATE_COLD -> ChatFormatting.AQUA;
            case MILD_COLD -> ChatFormatting.GRAY;
            case COMFORTABLE -> ChatFormatting.GREEN;
            case MILD_HEAT -> ChatFormatting.YELLOW;
            case MODERATE_HEAT -> ChatFormatting.GOLD;
            case SEVERE_HEAT -> ChatFormatting.RED;
        };
    }

    // ----- Shelter cache -----------------------------------------------------------------------------------------

    /** The shelter survey is a flood fill; it is cached per player and only redone when they move or time passes. */
    public static Shelter shelter(Player player, Level level, BlockPos pos)
    {
        final CachedShelter cached = SHELTER_CACHE.get(player.getUUID());
        final long now = level.getGameTime();
        if (cached != null && cached.pos == pos.asLong() && now - cached.tick < SHELTER_INTERVAL)
        {
            return cached.shelter;
        }

        final Shelter shelter = Shelter.survey(level, pos);
        SHELTER_CACHE.put(player.getUUID(), new CachedShelter(shelter, now, pos.asLong()));
        return shelter;
    }

    public static void forget(Player player)
    {
        SHELTER_CACHE.remove(player.getUUID());
    }

    // ----- Reporting for the field notes -------------------------------------------------------------------------

    /**
     * A human-readable summary of the player's current thermal situation, for the field notes book.
     * <p>
     * This is the one place a player can get a considered answer rather than a symptom, and even here it is in words:
     * what the weather is doing, what their clothes are worth, and what they could reasonably survive in them.
     */
    public static java.util.List<Component> describe(Player player)
    {
        final java.util.List<Component> lines = new java.util.ArrayList<>();
        final BodyTemperature state = get(player);
        final Shelter shelter = shelter(player, player.level(), player.blockPosition());
        final Garments garments = garments(player);

        lines.add(Component.translatable(state.band().descriptorKey()).withStyle(colour(state.band())));

        final float insulation = ThermalModel.effectiveInsulation(garments.insulation, garments.wetness);
        final int comfortable = Math.round(ThermalModel.comfortableAmbient(insulation, 0.15f));
        lines.add(Component.translatable("terravera.temperature.notes.clothing", comfortable)
            .withStyle(ChatFormatting.GRAY));

        if (garments.wetness > 0.2f)
        {
            lines.add(Component.translatable("terravera.temperature.notes.wet_clothing").withStyle(ChatFormatting.BLUE));
        }
        if (shelter.isIndoors())
        {
            lines.add(Component.translatable(shelter.isDraughty()
                ? "terravera.temperature.notes.draughty"
                : "terravera.temperature.notes.sealed").withStyle(ChatFormatting.GRAY));
        }
        if (shelter.hasFire())
        {
            lines.add(Component.translatable("terravera.temperature.notes.fire").withStyle(ChatFormatting.GOLD));
        }
        return lines;
    }
}
