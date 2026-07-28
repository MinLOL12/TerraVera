/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.farming;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A simple drip irrigation pipe that distributes water from a storage tank to nearby prepared farmland.
 * Connects to water containers on adjacent sides and moistens soil in a small radius below.
 * This is the mid-tier irrigation solution between hand watering and full automation.
 */
public class DripIrrigationBlock extends Block
{
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public DripIrrigationBlock(Properties properties)
    {
        super(properties.strength(0.8f).noOcclusion());
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(NORTH, false).setValue(SOUTH, false)
            .setValue(EAST, false).setValue(WEST, false)
            .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(NORTH, SOUTH, EAST, WEST, WATERLOGGED);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving)
    {
        if (!level.isClientSide())
        {
            updateConnections(level, pos, state);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving)
    {
        if (!level.isClientSide())
        {
            updateConnections(level, pos, state);
        }
    }

    private void updateConnections(Level level, BlockPos pos, BlockState state)
    {
        boolean north = isWaterSource(level, pos.north()) || level.getBlockState(pos.north()).is(this);
        boolean south = isWaterSource(level, pos.south()) || level.getBlockState(pos.south()).is(this);
        boolean east = isWaterSource(level, pos.east()) || level.getBlockState(pos.east()).is(this);
        boolean west = isWaterSource(level, pos.west()) || level.getBlockState(pos.west()).is(this);

        BlockState newState = state.setValue(NORTH, north).setValue(SOUTH, south)
            .setValue(EAST, east).setValue(WEST, west);
        if (newState != state)
        {
            level.setBlock(pos, newState, Block.UPDATE_ALL);
        }
    }

    private boolean isWaterSource(Level level, BlockPos pos)
    {
        BlockState state = level.getBlockState(pos);
        return state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)
            || state.getBlock() instanceof com.terravera.common.water.WaterCollectorBlock;
    }

    /**
     * Moistens nearby farmland. Called from the farming event handler on a slow tick.
     */
    public static void distributeWater(Level level, BlockPos pos)
    {
        if (level.isClientSide()) return;

        // Moisten prepared farmland in a 3x3 area below the pipe
        for (int dx = -1; dx <= 1; dx++)
        {
            for (int dz = -1; dz <= 1; dz++)
            {
                BlockPos below = pos.below().offset(dx, 0, dz);
                if (level.getBlockEntity(below) instanceof PreparedFarmlandBlockEntity be)
                {
                    var soil = be.soilCondition();
                    be.setSoilCondition(soil.withMoisture(Math.min(1.0f, soil.moisture() + 0.05f)));
                }
            }
        }
    }
}
