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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import net.dries007.tfc.util.data.DataManager;

import com.terravera.TerraVera;

/**
 * One disease: how you catch it, how long it hides, what it does to you, and how long it lasts.
 * <p>
 * Every illness in TerraVera is a JSON file in {@code data/<namespace>/terravera/illness/<name>.json}, loaded through
 * TerraFirmaCraft's own {@link DataManager}. That means the whole roster reloads with {@code /reload}, syncs to clients
 * automatically, and can be retuned or extended by a modpack without touching code - the same pattern the mod already
 * uses for head profiles and fibre sources.
 * <p>
 * The shipped roster is real. Each disease is modelled on how it actually behaves: the incubation periods are in the
 * right order of magnitude relative to each other, the symptom sets are the actual clinical picture rather than a
 * generic "you feel bad", and the transmission vectors are the real ones. Nothing here transmits instantly - every
 * illness has an {@link #incubationTicks() incubation period}, which is the single most important thing the request
 * asked for and the thing that makes the system teach rather than merely punish.
 *
 * @param vectors         how this illness can be caught
 * @param baseChance      probability of infection per exposure event, before any modifiers
 * @param incubationTicks how long after exposure before symptoms begin. Ticks; TFC's day is 24000
 * @param durationTicks   how long the symptomatic phase lasts once it starts
 * @param severity        how dangerous the illness is, which drives symptom amplifiers and the warning colour
 * @param symptoms        what the player actually experiences
 * @param contagious      whether an ill player can pass this to others by proximity
 * @param immunityTicks   how long the player is immune after recovering. Zero means no acquired immunity
 * @param lethal          whether the illness can kill directly if left untreated through its whole course
 * @param remedyTags      item tags that treat this illness, best first. Used by the remedy system and the field guide
 */
public record Illness(
    List<TransmissionVector> vectors,
    float baseChance,
    int incubationTicks,
    int durationTicks,
    Severity severity,
    List<Symptom> symptoms,
    boolean contagious,
    int immunityTicks,
    boolean lethal,
    List<String> remedyTags
) {
    public static final Codec<Illness> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.listOf().fieldOf("vectors").xmap(
            list -> list.stream().map(TransmissionVector::byId).filter(java.util.Objects::nonNull).toList(),
            list -> list.stream().map(TransmissionVector::id).toList()
        ).forGetter(Illness::vectors),
        Codec.FLOAT.optionalFieldOf("base_chance", 0.15f).forGetter(Illness::baseChance),
        Codec.INT.optionalFieldOf("incubation_ticks", 24000).forGetter(Illness::incubationTicks),
        Codec.INT.optionalFieldOf("duration_ticks", 48000).forGetter(Illness::durationTicks),
        Severity.CODEC.optionalFieldOf("severity", Severity.MILD).forGetter(Illness::severity),
        Codec.STRING.listOf().fieldOf("symptoms").xmap(
            list -> list.stream().map(Symptom::byId).filter(java.util.Objects::nonNull).toList(),
            list -> list.stream().map(Symptom::id).toList()
        ).forGetter(Illness::symptoms),
        Codec.BOOL.optionalFieldOf("contagious", false).forGetter(Illness::contagious),
        Codec.INT.optionalFieldOf("immunity_ticks", 0).forGetter(Illness::immunityTicks),
        Codec.BOOL.optionalFieldOf("lethal", false).forGetter(Illness::lethal),
        Codec.STRING.listOf().optionalFieldOf("remedies", List.of()).forGetter(Illness::remedyTags)
    ).apply(i, Illness::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Illness> STREAM_CODEC = StreamCodec.of(
        (buf, value) -> {
            buf.writeVarInt(value.vectors.size());
            for (TransmissionVector vector : value.vectors) buf.writeUtf(vector.id(), 32);
            buf.writeFloat(value.baseChance);
            buf.writeVarInt(value.incubationTicks);
            buf.writeVarInt(value.durationTicks);
            buf.writeEnum(value.severity);
            buf.writeVarInt(value.symptoms.size());
            for (Symptom symptom : value.symptoms) buf.writeUtf(symptom.id(), 32);
            buf.writeBoolean(value.contagious);
            buf.writeVarInt(value.immunityTicks);
            buf.writeBoolean(value.lethal);
            buf.writeVarInt(value.remedyTags.size());
            for (String tag : value.remedyTags) buf.writeUtf(tag, 128);
        },
        buf -> {
            final int vectorCount = buf.readVarInt();
            final java.util.List<TransmissionVector> vectors = new java.util.ArrayList<>(vectorCount);
            for (int i = 0; i < vectorCount; i++)
            {
                final TransmissionVector vector = TransmissionVector.byId(buf.readUtf(32));
                if (vector != null) vectors.add(vector);
            }
            final float baseChance = buf.readFloat();
            final int incubation = buf.readVarInt();
            final int duration = buf.readVarInt();
            final Severity severity = buf.readEnum(Severity.class);
            final int symptomCount = buf.readVarInt();
            final java.util.List<Symptom> symptoms = new java.util.ArrayList<>(symptomCount);
            for (int i = 0; i < symptomCount; i++)
            {
                final Symptom symptom = Symptom.byId(buf.readUtf(32));
                if (symptom != null) symptoms.add(symptom);
            }
            final boolean contagious = buf.readBoolean();
            final int immunity = buf.readVarInt();
            final boolean lethal = buf.readBoolean();
            final int remedyCount = buf.readVarInt();
            final java.util.List<String> remedies = new java.util.ArrayList<>(remedyCount);
            for (int i = 0; i < remedyCount; i++) remedies.add(buf.readUtf(128));
            return new Illness(List.copyOf(vectors), baseChance, incubation, duration, severity,
                List.copyOf(symptoms), contagious, immunity, lethal, List.copyOf(remedies));
        }
    );

    public static final DataManager<Illness> MANAGER = new DataManager<>(
        ResourceLocation.fromNamespaceAndPath(TerraVera.MOD_ID, "illness"), CODEC, STREAM_CODEC);

    /** @return every illness that can be caught through {@code vector}. */
    public static List<Illness> byVector(TransmissionVector vector)
    {
        return MANAGER.getValues().stream().filter(illness -> illness.vectors.contains(vector)).toList();
    }

    @Nullable
    public static Illness get(ResourceLocation id)
    {
        return MANAGER.get(id);
    }

    @Nullable
    public static ResourceLocation idOf(Illness illness)
    {
        return MANAGER.getId(illness);
    }

    /** @return the total lifetime of an infection, from exposure to recovery. */
    public int totalTicks()
    {
        return incubationTicks + durationTicks;
    }

    public boolean hasSymptom(Symptom symptom)
    {
        return symptoms.contains(symptom);
    }

    /**
     * How bad an illness is. Drives the amplifier of the applied effects, the colour of the warning, and whether the
     * player gets a "you should do something about this" nudge.
     */
    public enum Severity implements StringRepresentable
    {
        /** A nuisance. A cold. You will get over it. */
        MILD("mild", 0, 0x99CC99),
        /** Genuinely debilitating for a few days. Flu, giardiasis. */
        MODERATE("moderate", 0, 0xE6C34D),
        /** Will wreck your week and can kill you if you ignore it. Typhoid, trichinosis. */
        SEVERE("severe", 1, 0xE07B39),
        /** Will kill you. Cholera, tetanus. Treat it or die. */
        CRITICAL("critical", 2, 0xCC4444);

        public static final Codec<Severity> CODEC = StringRepresentable.fromEnum(Severity::values);

        private final String id;
        private final int amplifierBonus;
        private final int color;

        Severity(String id, int amplifierBonus, int color)
        {
            this.id = id;
            this.amplifierBonus = amplifierBonus;
            this.color = color;
        }

        /** Extra amplifier levels added to every symptom effect this illness applies. */
        public int amplifierBonus()
        {
            return amplifierBonus;
        }

        public int color()
        {
            return color;
        }

        public String translationKey()
        {
            return "terravera.severity." + id;
        }

        @Override
        public String getSerializedName()
        {
            return id;
        }
    }
}
