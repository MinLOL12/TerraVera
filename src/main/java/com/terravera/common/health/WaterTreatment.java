/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.health;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.terravera.common.TerraVeraDataComponents;

/**
 * The record, carried on a water container, of what has been done to the water inside it.
 * <p>
 * This is what turns "boil your water" from a slogan into a mechanic. Water in TerraFirmaCraft is a fluid with no
 * identity beyond its type, so a jug filled from a swamp and a jug filled from a spring are the same item stack. That
 * cannot be allowed to stay true in a mod about waterborne disease, so TerraVera stamps a container with the quality of
 * whatever went in and with any treatment that has since been applied:
 *
 * <ul>
 *     <li><strong>Untreated</strong> - filled from the world. Carries the contamination of wherever it was filled, and
 *     the player is drinking exactly the risk they scooped up.</li>
 *     <li><strong>Filtered</strong> - run through sand, charcoal, or cloth. Removes the parasites and most of the
 *     particulate load, but not bacteria. This is the mid-tier answer, and it deliberately does <em>not</em> make water
 *     safe from cholera or typhoid.</li>
 *     <li><strong>Boiled</strong> - held above 100C in a pot. Kills everything. This is the real answer, and it is
 *     available from the moment the player has a firepit and a clay pot.</li>
 * </ul>
 *
 * The two-tier split is the interesting part of the progression: filtration is cheap and portable and handles the
 * parasites that make you miserable, while boiling is the only thing that handles the bacteria that kill you.
 *
 * @param treatment the strongest treatment applied to this water
 * @param sourceContamination the contamination of the water at the moment it was collected, in {@code [0, 1]}
 */
public record WaterTreatment(Treatment treatment, float sourceContamination)
{
    /** Water that came out of a clean, treated supply - the default for anything the mod itself produces. */
    public static final WaterTreatment CLEAN = new WaterTreatment(Treatment.BOILED, 0f);

    public static final com.mojang.serialization.Codec<WaterTreatment> CODEC =
        com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
            com.mojang.serialization.Codec.STRING.optionalFieldOf("treatment", "untreated")
                .xmap(Treatment::byId, Treatment::id).forGetter(WaterTreatment::treatment),
            com.mojang.serialization.Codec.FLOAT.optionalFieldOf("contamination", 0f)
                .forGetter(WaterTreatment::sourceContamination)
        ).apply(i, WaterTreatment::new));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, WaterTreatment> STREAM_CODEC =
        net.minecraft.network.codec.StreamCodec.of(
            (buf, value) -> {
                buf.writeEnum(value.treatment);
                buf.writeFloat(value.sourceContamination);
            },
            buf -> new WaterTreatment(buf.readEnum(Treatment.class), buf.readFloat())
        );

    /**
     * The contamination that actually reaches the player's gut, after treatment.
     * <p>
     * Note that filtration is modelled as a proportional reduction with a floor, not as a cure: filtered swamp water is
     * much better than swamp water, but it is still swamp water.
     */
    public float effectiveContamination()
    {
        return switch (treatment)
        {
            case BOILED -> 0f;
            // Filtration removes protozoa and turbidity outright, but leaves a stubborn bacterial fraction behind.
            case FILTERED -> Math.max(0f, sourceContamination * 0.35f);
            case UNTREATED -> sourceContamination;
        };
    }

    /**
     * @return which pathogens can still get through. Filtration is specifically ineffective against the bacterial
     * diseases, which is what makes boiling worth the fuel.
     */
    public boolean allows(TransmissionVector vector)
    {
        return treatment != Treatment.BOILED;
    }

    /** @return {@code true} if this water is safe enough not to warn the player about. */
    public boolean isSafe()
    {
        return effectiveContamination() < 0.05f;
    }

    public WaterTreatment boiled()
    {
        return new WaterTreatment(Treatment.BOILED, sourceContamination);
    }

    public WaterTreatment filtered()
    {
        return treatment == Treatment.BOILED ? this : new WaterTreatment(Treatment.FILTERED, sourceContamination);
    }

    // ----- Item helpers -------------------------------------------------------------------------------------

    /**
     * Reads the treatment record off a container. A container with no record at all is assumed to hold untreated water
     * of moderate quality - this is the "some other mod filled this" fallback, and it errs towards the middle rather
     * than towards either extreme.
     */
    public static WaterTreatment get(ItemStack stack)
    {
        final WaterTreatment stored = stack.get(TerraVeraDataComponents.WATER_TREATMENT.get());
        return stored != null ? stored : new WaterTreatment(Treatment.UNTREATED, WaterQuality.STILL.contamination());
    }

    public static void set(ItemStack stack, WaterTreatment treatment)
    {
        stack.set(TerraVeraDataComponents.WATER_TREATMENT.get(), treatment);
    }

    /** Records that this container was just filled from a particular source. */
    public static void fillFrom(ItemStack stack, WaterSource source)
    {
        set(stack, new WaterTreatment(Treatment.UNTREATED, source.contamination()));
    }

    public Component describe()
    {
        final float effective = effectiveContamination();
        final WaterQuality quality = WaterQuality.fromContamination(effective);
        return Component.translatable("terravera.tooltip.water_quality",
                Component.translatable(quality.translationKey()))
            .withStyle(style -> style.withColor(quality.color()));
    }

    public Component describeTreatment()
    {
        return Component.translatable(treatment.translationKey())
            .withStyle(treatment == Treatment.BOILED ? ChatFormatting.AQUA
                : treatment == Treatment.FILTERED ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }

    /** What has been done to this water, in ascending order of effectiveness. */
    public enum Treatment
    {
        UNTREATED("untreated"),
        FILTERED("filtered"),
        BOILED("boiled");

        private final String id;

        Treatment(String id)
        {
            this.id = id;
        }

        public String id()
        {
            return id;
        }

        public String translationKey()
        {
            return "terravera.water_treatment." + id;
        }

        public static Treatment byId(String id)
        {
            for (Treatment treatment : values())
            {
                if (treatment.id.equals(id)) return treatment;
            }
            return UNTREATED;
        }
    }
}
