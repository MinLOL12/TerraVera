/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 */

package com.terravera.common.farming;

import com.terravera.common.blocks.TerraVeraBlocks;
import com.terravera.common.items.TerraVeraItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ItemLike;

/** A simple generic crop used by TerraVera's quality-bearing seeds until a specific crop type is known. */
public class TerraVeraCropBlock extends CropBlock
{
    public TerraVeraCropBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos)
    {
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return state.is(TerraVeraBlocks.PREPARED_FARMLAND.get())
            || state.is(Blocks.FARMLAND)
            || state.is(BlockTags.DIRT)
            || path.contains("loam")
            || path.contains("silt")
            || path.contains("soil")
            || path.contains("farmland");
    }

    @Override
    protected ItemLike getBaseSeedId()
    {
        return TerraVeraItems.SEED.get();
    }
}
