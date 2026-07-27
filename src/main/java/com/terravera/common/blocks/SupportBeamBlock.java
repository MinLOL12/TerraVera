/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A slim structural member. Its axis is chosen by the face used when placing it: place on top for a post, or against
 * a side for a lintel/purlin. StructuralIntegrity uses the axis to distinguish a foundation-to-roof column from a
 * spanning beam.
 */
public class SupportBeamBlock extends RotatedPillarBlock
{
    private static final VoxelShape POST = box(4, 0, 4, 12, 16, 12);
    private static final VoxelShape EAST_WEST = box(0, 4, 4, 16, 12, 12);
    private static final VoxelShape NORTH_SOUTH = box(4, 4, 0, 12, 12, 16);

    public SupportBeamBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return switch (state.getValue(AXIS))
        {
            case X -> EAST_WEST;
            case Z -> NORTH_SOUTH;
            case Y -> POST;
        };
    }
}
