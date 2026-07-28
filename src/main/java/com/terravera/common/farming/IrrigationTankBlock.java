/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 */

package com.terravera.common.farming;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import com.terravera.common.blocks.TerraVeraBlockEntities;

/**
 * A rainwater storage tank that collects rain from its open top and stores it for irrigation use.
 * Has a block entity that ticks to collect water and distribute it to connected drip irrigation below.
 */
public class IrrigationTankBlock extends BaseEntityBlock
{
    public static final MapCodec<IrrigationTankBlock> CODEC = simpleCodec(IrrigationTankBlock::new);

    public IrrigationTankBlock(Properties properties)
    {
        super(properties.strength(2.0f, 4.0f).noOcclusion());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec()
    {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state)
    {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new IrrigationTankBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        if (level.isClientSide()) return null;
        if (type != TerraVeraBlockEntities.IRRIGATION_TANK.get()) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof IrrigationTankBlockEntity tank)
            {
                tank.serverTick(lvl, pos, st);
            }
        };
    }
}
