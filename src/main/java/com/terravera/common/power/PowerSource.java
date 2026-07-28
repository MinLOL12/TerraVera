package com.terravera.common.power;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** A machine that pushes watts into the grid: generators, cranks, turbines. */
public interface PowerSource extends PowerMachine {
    /**
     * Current output in watts. Sources are expected to return 0 when they are not actually producing - a turbine with
     * no sky, or a crank nobody is turning, supplies nothing.
     */
    int powerOutput(Level level, BlockPos pos, BlockState state);
}
