/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.quality;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Tracks the genetic quality of a saved seed. Larger, healthier seeds produce stronger plants with better yields.
 * Saving the best seeds each harvest gradually improves crops over generations.
 *
 * @param quality     genetic vigour of this seed, 0..1. Larger seeds = higher quality.
 * @param generation  how many generations of selection this seed line has undergone
 * @param cropType    what crop this seed grows into, e.g. "wheat", "barley", "maize"
 * @param viability   how likely this seed is to germinate at all, 0..1
 */
public record SeedQuality(float quality, int generation, String cropType, float viability)
{
    public static final SeedQuality DEFAULT = new SeedQuality(0.5f, 0, "unknown", 0.7f);

    public static final Codec<SeedQuality> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.FLOAT.fieldOf("quality").forGetter(SeedQuality::quality),
        Codec.INT.fieldOf("generation").forGetter(SeedQuality::generation),
        Codec.STRING.fieldOf("crop_type").forGetter(SeedQuality::cropType),
        Codec.FLOAT.fieldOf("viability").forGetter(SeedQuality::viability)
    ).apply(i, SeedQuality::new));

    public static final StreamCodec<ByteBuf, SeedQuality> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, SeedQuality::quality,
        ByteBufCodecs.VAR_INT, SeedQuality::generation,
        ByteBufCodecs.stringUtf8(32), SeedQuality::cropType,
        ByteBufCodecs.FLOAT, SeedQuality::viability,
        SeedQuality::new
    );

    /** Quality tier label for tooltip display. */
    public String tierLabel()
    {
        if (quality >= 0.85f) return "prize";
        if (quality >= 0.65f) return "select";
        if (quality >= 0.45f) return "standard";
        return "cull";
    }

    /**
     * Produce a new seed quality that represents saving the best seed from this harvest. Generation increments,
     * quality trends upward if the parent was good, viability improves with selection.
     */
    public SeedQuality nextGeneration(float harvestedQuality)
    {
        // Selection pressure: the better the parent, the better the saved seed tends to be
        float newQuality = Math.min(1.0f, (quality * 0.4f + harvestedQuality * 0.6f) * 1.02f);
        int newGen = generation + 1;
        float newViability = Math.min(1.0f, viability + (1.0f - viability) * 0.05f);
        return new SeedQuality(newQuality, newGen, cropType, newViability);
    }
}
