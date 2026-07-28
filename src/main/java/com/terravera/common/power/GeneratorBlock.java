package com.terravera.common.power;

import net.minecraft.core.Direction;

/**
 * Constant-output engine-driven generator for a dedicated generator room. It produces 360 W whenever it is placed,
 * and exposes brass terminal posts on its four sides for attaching wire. Intended for a fuel/progression pack or a
 * stationary power house - the hand crank and wind turbine are the field-portable sources.
 */
public class GeneratorBlock extends net.minecraft.world.level.block.Block implements PowerSource {
    public static final int OUTPUT_WATTS = 360;

    public GeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canConnectWires(Direction side) {
        return side.getAxis() != Direction.Axis.Y;
    }

    @Override
    public int powerOutput(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                           net.minecraft.world.level.block.state.BlockState state) {
        return OUTPUT_WATTS;
    }
}
