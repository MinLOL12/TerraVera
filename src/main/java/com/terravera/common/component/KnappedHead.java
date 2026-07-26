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
import net.minecraft.resources.ResourceLocation;

/**
 * The data attached to a knapped tool head. A head is not "an axe head" or "a shovel head" - it is a lump of a given
 * stone, worked into a given <em>kind</em> of working end, to a given standard.
 *
 * @param kind     the working end this piece has, e.g. {@code terravera:wedge} or {@code terravera:point}. Determines
 *                 which tools it can be hafted into.
 * @param material the TFC rock category the stone came from, e.g. {@code igneous_intrusive}. Determines the tier of
 *                 the resulting tool.
 * @param quality  in [0, 1]. How far past the minimum the knapping went - a broad, deep, unnotched base and a clean
 *                 taper score high. Scales the durability of the finished tool.
 */
public record KnappedHead(ResourceLocation kind, String material, float quality)
{
    public static final Codec<KnappedHead> CODEC = RecordCodecBuilder.create(i -> i.group(
        ResourceLocation.CODEC.fieldOf("kind").forGetter(KnappedHead::kind),
        Codec.STRING.fieldOf("material").forGetter(KnappedHead::material),
        Codec.FLOAT.optionalFieldOf("quality", 0.5f).forGetter(KnappedHead::quality)
    ).apply(i, KnappedHead::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, KnappedHead> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC, KnappedHead::kind,
        ByteBufCodecs.stringUtf8(64), KnappedHead::material,
        ByteBufCodecs.FLOAT, KnappedHead::quality,
        KnappedHead::new
    );

    /**
     * @return a coarse descriptor used for the item tooltip, so that players get feedback on their knapping without
     * being shown a raw float.
     */
    public String qualityDescriptor()
    {
        if (quality >= 0.8f) return "masterful";
        if (quality >= 0.6f) return "sound";
        if (quality >= 0.35f) return "serviceable";
        return "crude";
    }
}
