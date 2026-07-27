/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.health;

import java.util.List;
import javax.annotation.Nullable;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.dries007.tfc.util.data.DataManager;

import com.terravera.TerraVera;

/**
 * Something you can consume or apply that shortens or blunts an illness.
 * <p>
 * Remedies are the progression spine of the health system, and they are tiered so that the answer to "I am ill" gets
 * better as the player advances, rather than existing or not existing:
 * <ol>
 *     <li><strong>Herbal.</strong> Available on day one. Bitter plants, tannin, garlic. Shortens an illness by a
 *     fraction and takes the edge off the symptoms. Never cures outright.</li>
 *     <li><strong>Prepared.</strong> Needs a pot, salt, or a barrel - rehydration solutions, decoctions, wound
 *     dressings. Substantially effective, and the rehydration line is the difference between surviving cholera and
 *     not.</li>
 *     <li><strong>Medicine.</strong> Needs the late-game chain - distillation, glassware, refined extracts. Cures
 *     almost anything outright.</li>
 * </ol>
 * As with illnesses, these live in JSON ({@code data/<ns>/terravera/remedy/<name>.json}) and load through TFC's data
 * manager, so the tiers are tunable and a modpack can slot its own medicine in at the right rung.
 *
 * @param ingredient        the item tag this remedy matches
 * @param treats            illnesses this remedy works on. Empty means it works on everything
 * @param treatsSymptoms    symptoms this remedy relieves even when it cannot treat the underlying illness
 * @param shortenTicks      how many ticks it removes from the remaining course of the illness
 * @param severityReduction how much it blunts symptom intensity, as a fraction
 * @param cures             whether it ends the illness outright
 * @param tier              which progression rung this belongs to, used for the field guide and sorting
 */
public record Remedy(
    TagKey<Item> ingredient,
    List<ResourceLocation> treats,
    List<Symptom> treatsSymptoms,
    int shortenTicks,
    float severityReduction,
    boolean cures,
    Tier tier
) {
    public static final Codec<Remedy> CODEC = RecordCodecBuilder.create(i -> i.group(
        TagKey.codec(Registries.ITEM).fieldOf("ingredient").forGetter(Remedy::ingredient),
        ResourceLocation.CODEC.listOf().optionalFieldOf("treats", List.of()).forGetter(Remedy::treats),
        Codec.STRING.listOf().optionalFieldOf("treats_symptoms", List.of()).xmap(
            list -> list.stream().map(Symptom::byId).filter(java.util.Objects::nonNull).toList(),
            list -> list.stream().map(Symptom::id).toList()
        ).forGetter(Remedy::treatsSymptoms),
        Codec.INT.optionalFieldOf("shorten_ticks", 6000).forGetter(Remedy::shortenTicks),
        Codec.FLOAT.optionalFieldOf("severity_reduction", 0.15f).forGetter(Remedy::severityReduction),
        Codec.BOOL.optionalFieldOf("cures", false).forGetter(Remedy::cures),
        Codec.STRING.optionalFieldOf("tier", "herbal").xmap(Tier::byId, Tier::id).forGetter(Remedy::tier)
    ).apply(i, Remedy::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Remedy> STREAM_CODEC = StreamCodec.of(
        (buf, value) -> {
            buf.writeResourceLocation(value.ingredient.location());
            buf.writeVarInt(value.treats.size());
            for (ResourceLocation id : value.treats) buf.writeResourceLocation(id);
            buf.writeVarInt(value.treatsSymptoms.size());
            for (Symptom symptom : value.treatsSymptoms) buf.writeUtf(symptom.id(), 32);
            buf.writeVarInt(value.shortenTicks);
            buf.writeFloat(value.severityReduction);
            buf.writeBoolean(value.cures);
            buf.writeEnum(value.tier);
        },
        buf -> {
            final TagKey<Item> ingredient = TagKey.create(Registries.ITEM, buf.readResourceLocation());
            final int treatCount = buf.readVarInt();
            final java.util.List<ResourceLocation> treats = new java.util.ArrayList<>(treatCount);
            for (int i = 0; i < treatCount; i++) treats.add(buf.readResourceLocation());
            final int symptomCount = buf.readVarInt();
            final java.util.List<Symptom> symptoms = new java.util.ArrayList<>(symptomCount);
            for (int i = 0; i < symptomCount; i++)
            {
                final Symptom symptom = Symptom.byId(buf.readUtf(32));
                if (symptom != null) symptoms.add(symptom);
            }
            return new Remedy(ingredient, List.copyOf(treats), List.copyOf(symptoms),
                buf.readVarInt(), buf.readFloat(), buf.readBoolean(), buf.readEnum(Tier.class));
        }
    );

    public static final DataManager<Remedy> MANAGER = new DataManager<>(
        ResourceLocation.fromNamespaceAndPath(TerraVera.MOD_ID, "remedy"), CODEC, STREAM_CODEC);

    /** @return {@code true} if this remedy does anything at all for {@code illnessId}. */
    public boolean appliesTo(ResourceLocation illnessId, Illness illness)
    {
        if (treats.contains(illnessId)) return true;
        if (!treats.isEmpty()) return false;
        // A remedy with no explicit illness list is a general one; it still has to match on symptoms if it declares any.
        if (treatsSymptoms.isEmpty()) return true;
        return illness.symptoms().stream().anyMatch(treatsSymptoms::contains);
    }

    /** @return the best remedy in {@code stack} for the given illness, or {@code null} if the item treats nothing. */
    @Nullable
    public static Remedy find(ItemStack stack, ResourceLocation illnessId, Illness illness)
    {
        Remedy best = null;
        for (Remedy candidate : MANAGER.getValues())
        {
            if (!stack.is(candidate.ingredient)) continue;
            if (!candidate.appliesTo(illnessId, illness)) continue;
            if (best == null || candidate.rank() > best.rank()) best = candidate;
        }
        return best;
    }

    /** @return {@code true} if this item is a remedy for anything, used to decide whether to show a tooltip. */
    public static boolean isRemedy(ItemStack stack)
    {
        for (Remedy candidate : MANAGER.getValues())
        {
            if (stack.is(candidate.ingredient)) return true;
        }
        return false;
    }

    private int rank()
    {
        return (cures ? 1000 : 0) + tier.ordinal() * 100 + shortenTicks / 1000;
    }

    /** Progression rung. Purely descriptive - the numbers do the work - but it drives sorting and the field guide. */
    public enum Tier
    {
        /** Chewed leaves and bark. Available immediately. */
        HERBAL("herbal"),
        /** Needs fire, a pot, salt, or a barrel. */
        PREPARED("prepared"),
        /** Needs the full late-game chain. */
        MEDICINE("medicine");

        private final String id;

        Tier(String id)
        {
            this.id = id;
        }

        public String id()
        {
            return id;
        }

        public String translationKey()
        {
            return "terravera.remedy_tier." + id;
        }

        public static Tier byId(String id)
        {
            for (Tier tier : values())
            {
                if (tier.id.equals(id)) return tier;
            }
            return HERBAL;
        }
    }
}
