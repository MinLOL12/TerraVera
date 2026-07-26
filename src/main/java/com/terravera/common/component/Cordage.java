/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Data attached to a length of cordage. Different plants give different fibre - nettle and dogbane give long, strong
 * bast; grass gives short, weak fibre that will do in a pinch but will not hold an axe head on for long.
 *
 * @param strength     in [0, 1]. How much of a tool's durability the lashing supports.
 * @param source       the fibre source, used for the tooltip and for recipes that want a specific fibre.
 */
public record Cordage(float strength, String source)
{
    public static final Cordage DEFAULT = new Cordage(0.5f, "mixed");

    public static final Codec<Cordage> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.FLOAT.optionalFieldOf("strength", 0.5f).forGetter(Cordage::strength),
        Codec.STRING.optionalFieldOf("source", "mixed").forGetter(Cordage::source)
    ).apply(i, Cordage::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Cordage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, Cordage::strength,
        ByteBufCodecs.stringUtf8(64), Cordage::source,
        Cordage::new
    );
}
