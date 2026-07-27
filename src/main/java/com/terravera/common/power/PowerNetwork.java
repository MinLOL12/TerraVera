package com.terravera.common.power;

import com.terravera.common.blocks.TerraVeraBlocks;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** A deliberately small, local low-voltage network. Wires must make a continuous face-adjacent path from a source
 * to an appliance; being merely nearby does not supply electricity. */
public final class PowerNetwork {
    private static final int MAX_WIRE_RUN = 96;
    private PowerNetwork() {}

    public static int availablePower(Level level, BlockPos appliance) {
        ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        frontier.add(appliance);
        visited.add(appliance);
        int watts = 0;
        while (!frontier.isEmpty() && visited.size() <= MAX_WIRE_RUN) {
            BlockPos current = frontier.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (!visited.add(next)) continue;
                BlockState state = level.getBlockState(next);
                if (state.is(TerraVeraBlocks.COPPER_WIRE.get())) {
                    frontier.addLast(next);
                } else {
                    watts += output(level, next, state);
                }
            }
        }
        return watts;
    }

    private static int output(Level level, BlockPos pos, BlockState state) {
        if (state.is(TerraVeraBlocks.GENERATOR.get())) return 360;
        if (state.is(TerraVeraBlocks.HAND_CRANK.get())) return HandCrankBlock.isTurning(level, pos) ? 140 : 0;
        // A turbine requires open sky, so it cannot become an infinite indoor power source.
        if (state.is(TerraVeraBlocks.WIND_TURBINE.get()) && level.canSeeSky(pos.above())) return 220;
        return 0;
    }
}
