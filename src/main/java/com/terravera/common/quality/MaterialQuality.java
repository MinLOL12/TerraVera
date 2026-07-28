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
 * Carries the natural quality of a harvested material on the item. Straight-grained sticks make better shafts, long
 * bast fibres twist into stronger cordage, dry wood burns hotter, and clean clay fires into stronger pottery.
 * <p>
 * Quality is a single float 0..1 that represents how good this piece of material is for its intended purpose. It is
 * determined at harvest time by the source, the tool used, and the conditions under which the material was gathered.
 *
 * @param quality     how good this piece is, 0 = poor, 1 = excellent
 * @param category    what kind of material: "stick", "fibre", "wood", "clay", "stone"
 * @param moisture    how wet the material is, 0 = bone dry, 1 = saturated. Dry wood burns, wet wood smokes.
 */
public record MaterialQuality(float quality, String category, float moisture)
{
    public static final MaterialQuality DEFAULT = new MaterialQuality(0.5f, "mixed", 0.5f);

    public static final Codec<MaterialQuality> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.FLOAT.fieldOf("quality").forGetter(MaterialQuality::quality),
        Codec.STRING.fieldOf("category").forGetter(MaterialQuality::category),
        Codec.FLOAT.fieldOf("moisture").forGetter(MaterialQuality::moisture)
    ).apply(i, MaterialQuality::new));

    public static final StreamCodec<ByteBuf, MaterialQuality> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, MaterialQuality::quality,
        ByteBufCodecs.stringUtf8(32), MaterialQuality::category,
        ByteBufCodecs.FLOAT, MaterialQuality::moisture,
        MaterialQuality::new
    );

    /** Quality tier labels for tooltip display. */
    public String tierLabel()
    {
        if (quality >= 0.85f) return "excellent";
        if (quality >= 0.65f) return "good";
        if (quality >= 0.45f) return "fair";
        if (quality >= 0.25f) return "poor";
        return "very poor";
    }

    /** Moisture label for tooltip display. */
    public String moistureLabel()
    {
        if (moisture <= 0.15f) return "bone dry";
        if (moisture <= 0.35f) return "dry";
        if (moisture <= 0.55f) return "seasoned";
        if (moisture <= 0.75f) return "damp";
        return "wet";
    }
}
