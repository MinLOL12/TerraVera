package com.terravera.common.power;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** A machine that draws watts from the grid, such as the vapor-compression air conditioner. */
public interface PowerConsumer extends PowerMachine {
    /** Current demand in watts. Machines that are switched off or not yet programmed should return 0. */
    int powerDemand(Level level, BlockPos pos, BlockState state);
}
