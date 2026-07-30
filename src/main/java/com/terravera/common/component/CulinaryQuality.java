package com.terravera.common.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Preparation facts retained by cooked griddle food. It gives food-quality integrations a stable, datapack-visible
 * description of why a fermented, well-rested waffle is different from an unleavened emergency cake.
 */
public record CulinaryQuality(String leavening, int restingMinutes, float lightness, int taste)
{
    public static final Codec<CulinaryQuality> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("leavening").forGetter(CulinaryQuality::leavening),
        Codec.INT.fieldOf("resting_minutes").forGetter(CulinaryQuality::restingMinutes),
        Codec.FLOAT.fieldOf("lightness").forGetter(CulinaryQuality::lightness),
        Codec.INT.fieldOf("taste").forGetter(CulinaryQuality::taste)
    ).apply(i, CulinaryQuality::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CulinaryQuality> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(32), CulinaryQuality::leavening,
        ByteBufCodecs.VAR_INT, CulinaryQuality::restingMinutes,
        ByteBufCodecs.FLOAT, CulinaryQuality::lightness,
        ByteBufCodecs.VAR_INT, CulinaryQuality::taste,
        CulinaryQuality::new);
}
