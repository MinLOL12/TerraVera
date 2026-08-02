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
 * whatever went in and with any treatment that has since been applied.
 * <p>
 * The {@link Treatment} ladder is deliberately long. It models the real hierarchy of water treatment:
 * <ul>
 *     <li><strong>Settling, cloth, flocculation, acid</strong> - cheap and available on day one. They take the
 *     turbidity and much of the load out, and stop nothing by themselves.</li>
 *     <li><strong>Filtration</strong> - sand and charcoal remove the protozoa (Giardia, Cryptosporidium) that make a
 *     long journey miserable, but not the bacteria.</li>
 *     <li><strong>Heat and sunlight</strong> - pasteurization and SODIS kill most bacteria and viruses but not the
 *     hardy cysts; boiling kills everything.</li>
 *     <li><strong>Chemicals</strong> - iodine and chlorine are field classics; they kill bacteria and viruses quickly,
 *     but chlorine in particular leaves Cryptosporidium alone.</li>
 *     <li><strong>UV, reverse osmosis, distillation</strong> - the machines. UV is not quite total; RO and distillation
 *     are, because there is nothing left in the water at all.</li>
 * </ul>
 * Because every treatment keeps the source contamination attached, a player can look at a jug of "solar-disinfected
 * pond water" and know exactly what it is - and what it would become if they boiled it instead.
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
     * Every treatment is modelled as a proportional reduction, never a cure - except the ones that genuinely are:
     * distillation and a rolling boil leave nothing at all. Filtered swamp water is much better than swamp water, but
     * it is still swamp water.
     */
    public float effectiveContamination()
    {
        return sourceContamination * treatment.multiplier();
    }

    /**
     * @return which pathogens can still get through. Boiling, distillation, RO, and UV are the only treatments that
     * block everything; chemicals and filters leave the hardy protozoan cysts behind.
     */
    public boolean allows(TransmissionVector vector)
    {
        return treatment != Treatment.BOILED && treatment != Treatment.DISTILLED
            && treatment != Treatment.RO_PURIFIED && treatment != Treatment.UV_STERILIZED;
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
            .withStyle(treatment.color());
    }

    /**
     * What has been done to this water, in ascending order of effectiveness. The rank is what "apply the best
     * treatment you have" compares, so a jug of chlorinated water that is later boiled becomes boiled water - the
     * strongest treatment always wins and nothing is ever lost by treating water twice.
     */
    public enum Treatment
    {
        /** Filled from the world. Carries whatever the source carried. */
        UNTREATED("untreated", 0, 1.00f, ChatFormatting.GRAY),
        /** Left to stand. Suspended matter falls out and takes a fraction of the load with it. */
        SETTLED("settled", 1, 0.85f, ChatFormatting.GRAY),
        /** Run through any woven cloth. Removes grit and some parasites. */
        CLOTH_FILTERED("cloth_filtered", 2, 0.60f, ChatFormatting.GRAY),
        /** Soured with citrus or vinegar. The classic emergency field treatment; it buys time, not safety. */
        ACIDIFIED("acidified", 3, 0.55f, ChatFormatting.GRAY),
        /** Flocculated with moringa or alum and then settled. The big particles go, and much of the load goes with them. */
        FLOCCULATED("flocculated", 4, 0.45f, ChatFormatting.GRAY),
        /** Run through sand and charcoal. Removes protozoa and most of the load; leaves the bacteria. */
        FILTERED("filtered", 5, 0.30f, ChatFormatting.GREEN),
        /** Slow bio-sand filtration. A living layer on the sand eats most pathogens; not all of them. */
        BIO_FILTERED("bio_filtered", 6, 0.22f, ChatFormatting.GREEN),
        /** Through a fired ceramic candle. Small enough to strain out bacteria; not viruses. */
        CERAMIC_FILTERED("ceramic_filtered", 7, 0.12f, ChatFormatting.GREEN),
        /** Ionic silver. Slow, mild, but persistent - it keeps working while the water sits. */
        SILVERED("silvered", 8, 0.38f, ChatFormatting.GREEN),
        /** Held at 63-99C for a long time. Kills most bacteria and viruses; the hardy cysts survive. */
        PASTEURIZED("pasteurized", 9, 0.08f, ChatFormatting.GOLD),
        /** Bottles in full sun. The same story as pasteurization - most things, not the cysts. */
        SOLAR_DISINFECTED("solar_disinfected", 10, 0.07f, ChatFormatting.GOLD),
        /** Iodine tincture. Fast and reliable against bacteria and viruses. */
        IODIZED("iodized", 11, 0.05f, ChatFormatting.GOLD),
        /** Potassium permanganate. Strong oxidiser; also makes the water taste of iron. */
        PERMANGANATE("permanganate", 12, 0.04f, ChatFormatting.GOLD),
        /** Chlorine. The standard of the world - but Cryptosporidium shrugs it off. */
        CHLORINATED("chlorinated", 13, 0.04f, ChatFormatting.GOLD),
        /** Ultraviolet light. Damages the DNA of almost everything; a few tough spores persist. */
        UV_STERILIZED("uv_sterilized", 14, 0.02f, ChatFormatting.DARK_AQUA),
        /** Reverse osmosis. Strains everything bigger than a water molecule; only the very persistence of the idea is left. */
        RO_PURIFIED("ro_purified", 15, 0.01f, ChatFormatting.DARK_AQUA),
        /** Boiled off and condensed back. Nothing survives; there is nothing left to survive. */
        DISTILLED("distilled", 16, 0f, ChatFormatting.AQUA),
        /** Held at a rolling boil. The answer to everything, and it has been for ten thousand years. */
        BOILED("boiled", 17, 0f, ChatFormatting.AQUA);

        private final String id;
        private final int rank;
        private final float multiplier;
        private final ChatFormatting color;

        Treatment(String id, int rank, float multiplier, ChatFormatting color)
        {
            this.id = id;
            this.rank = rank;
            this.multiplier = multiplier;
            this.color = color;
        }

        public String id()
        {
            return id;
        }

        public int rank()
        {
            return rank;
        }

        public float multiplier()
        {
            return multiplier;
        }

        public ChatFormatting color()
        {
            return color;
        }

        public String translationKey()
        {
            return "terravera.water_treatment." + id;
        }

        /** The stronger of two treatments, so "treat water twice" always upgrades, never downgrades. */
        public static Treatment best(Treatment a, Treatment b)
        {
            return a.rank >= b.rank ? a : b;
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
