/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.recipes;

import java.util.List;
import javax.annotation.Nullable;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.util.data.DataManager;

import com.terravera.TerraVera;

/**
 * Declares that a block yields plant fibre when harvested, how much, and how good that fibre is.
 * <p>
 * Lives in {@code data/<namespace>/terravera/fibre_source/<name>.json}. This is what makes "cultivated from plants and
 * grass" data-driven rather than hardcoded - a modpack can point it at any block or block tag.
 *
 * @param blocks   a block tag whose members drop this fibre
 * @param chance   base chance per harvest, before the knife bonus
 * @param min      minimum fibre dropped when the roll succeeds
 * @param max      maximum fibre dropped when the roll succeeds
 * @param strength the strength, in [0, 1], of cordage twisted from this fibre. Grass is weak, nettle bast is strong.
 * @param source   a name for this fibre, shown in the cordage tooltip
 * @param requiresKnife if true, this source yields nothing unless harvested with a bladed tool
 */
public record FibreSource(
    TagKey<Block> blocks,
    float chance,
    int min,
    int max,
    float strength,
    String source,
    boolean requiresKnife
) {
    public static final Codec<FibreSource> CODEC = RecordCodecBuilder.create(i -> i.group(
        TagKey.codec(Registries.BLOCK).fieldOf("blocks").forGetter(FibreSource::blocks),
        Codec.FLOAT.optionalFieldOf("chance", 1f).forGetter(FibreSource::chance),
        Codec.INT.optionalFieldOf("min", 1).forGetter(FibreSource::min),
        Codec.INT.optionalFieldOf("max", 1).forGetter(FibreSource::max),
        Codec.FLOAT.optionalFieldOf("strength", 0.5f).forGetter(FibreSource::strength),
        Codec.STRING.optionalFieldOf("source", "grass").forGetter(FibreSource::source),
        Codec.BOOL.optionalFieldOf("requires_knife", false).forGetter(FibreSource::requiresKnife)
    ).apply(i, FibreSource::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FibreSource> STREAM_CODEC = StreamCodec.of(
        (buf, value) -> {
            buf.writeResourceLocation(value.blocks.location());
            buf.writeFloat(value.chance);
            buf.writeVarInt(value.min);
            buf.writeVarInt(value.max);
            buf.writeFloat(value.strength);
            buf.writeUtf(value.source, 64);
            buf.writeBoolean(value.requiresKnife);
        },
        buf -> new FibreSource(
            TagKey.create(Registries.BLOCK, buf.readResourceLocation()),
            buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readFloat(),
            buf.readUtf(64), buf.readBoolean())
    );

    public static final DataManager<FibreSource> MANAGER = new DataManager<>(
        ResourceLocation.fromNamespaceAndPath(TerraVera.MOD_ID, "fibre_source"), CODEC, STREAM_CODEC);

    /**
     * @return the best matching fibre source for a block state, or {@code null} if the block yields no fibre. When
     * several sources match, the strongest wins - a nettle is still a nettle even if it is also tagged as a plant.
     */
    @Nullable
    public static FibreSource get(BlockState state)
    {
        FibreSource best = null;
        for (FibreSource candidate : MANAGER.getValues())
        {
            if (state.is(candidate.blocks) && (best == null || candidate.strength > best.strength))
            {
                best = candidate;
            }
        }
        return best;
    }

    public static List<FibreSource> all()
    {
        return List.copyOf(MANAGER.getValues());
    }
}
