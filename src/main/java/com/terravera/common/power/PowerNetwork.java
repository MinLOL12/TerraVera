package com.terravera.common.power;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * TerraVera's low-voltage grid, modelled as a small local network rather than a magic global buffer.
 * <p>
 * A scan starts at an appliance and flood-fills through wire blocks, following only the connections the wire itself
 * declares on its faces. Everything the flood touches is one grid: the sources feeding it, the appliances drawing
 * from it, and the conductors in between. From that we derive three numbers:
 * <ul>
 *     <li><b>supply</b> - the watts the sources are producing right now,</li>
 *     <li><b>demand</b> - the watts the appliances on the grid are asking for,</li>
 *     <li><b>capacity</b> - the rating of the weakest feeder conductor (the wire bolted directly to a machine).</li>
 * </ul>
 * If the grid is asked for more than its conductors can carry, the overloaded feeders heat up and eventually burn
 * out - realistic fault behaviour rather than silent throttling. If demand simply exceeds supply, everything
 * browns out: an under-fed compressor will not half-run.
 */
public final class PowerNetwork {
    /** Maximum number of wire blocks in one run before the scan gives up. Keeps floods local and cheap. */
    public static final int MAX_WIRE_RUN = 128;
    /** How long a feeder must stay overloaded (in server ticks) before it fails catastrophically. */
    private static final int OVERLOAD_FAILURE_TICKS = 80;
    /** Grids are re-scanned at most this often; between scans the cached result is reused. */
    private static final long CACHE_TTL_TICKS = 20;

    /** The result of scanning everything electrically connected to a point on the grid. */
    public record Grid(int supply, int demand, int capacity, List<BlockPos> wires, List<BlockPos> feeders) {
        public boolean brownout() {
            return demand > supply;
        }

        /** Watts actually deliverable to an appliance right now, after supply and conductor limits. */
        public int deliverable() {
            if (demand > supply) return 0;
            return Math.min(supply, capacity);
        }

        public boolean overloaded() {
            return demand > capacity && demand > 0;
        }
    }

    private static final class GridCache {
        final Map<Long, Grid> grids = new HashMap<>();
        final Map<Long, Long> expiresAt = new HashMap<>();
        final Map<Long, Integer> overloadTicks = new HashMap<>();
    }

    private static final Map<Level, GridCache> CACHES = new WeakHashMap<>();

    private PowerNetwork() {}

    private static synchronized GridCache cache(Level level) {
        return CACHES.computeIfAbsent(level, ignored -> new GridCache());
    }

    /**
     * The watts available to the appliance at {@code appliance}. A contiguous, face-connected conductor run must
     * exist from producing sources to this machine; being near a generator does nothing.
     */
    public static int availablePower(Level level, BlockPos appliance) {
        if (level.isClientSide()) return 0;
        GridCache cache = cache(level);
        long key = appliance.asLong();
        long now = level.getGameTime();
        Long expiry = cache.expiresAt.get(key);
        if (expiry == null || expiry <= now) {
            Grid grid = scan(level, appliance);
            cache.grids.put(key, grid);
            cache.expiresAt.put(key, now + CACHE_TTL_TICKS);
            tickProtection(level, grid, cache);
            markEnergised(level, grid);
            return grid.deliverable();
        }
        Grid grid = cache.grids.get(key);
        tickProtection(level, grid, cache);
        return grid.deliverable();
    }

    /**
     * Flood-fills the grid from an appliance. The appliance itself may sit directly against a source; otherwise a
     * wire run must bridge the gap. Sources and consumers are only counted when attached through a declared
     * connection, so a wire brushing a non-terminal face of a turbine does not tap it.
     */
    public static Grid scan(Level level, BlockPos appliance) {
        Set<BlockPos> wires = new HashSet<>();
        Set<BlockPos> machines = new HashSet<>();
        List<BlockPos> feeders = new ArrayList<>();
        ArrayDeque<BlockPos> frontier = new ArrayDeque<>();

        machines.add(appliance);
        for (Direction side : Direction.values()) {
            BlockPos next = appliance.relative(side);
            BlockState state = level.getBlockState(next);
            if (state.getBlock() instanceof WireBlock wire && wire.isConnected(state, side.getOpposite())) {
                if (wires.add(next)) frontier.addLast(next);
            } else if (state.getBlock() instanceof PowerMachine machine && machine.canConnectWires(side.getOpposite())) {
                machines.add(next);
            }
        }

        int capacity = Integer.MAX_VALUE; // Direct machine-to-machine contact has no conductor to rate-limit it.
        while (!frontier.isEmpty() && wires.size() <= MAX_WIRE_RUN) {
            BlockPos current = frontier.removeFirst();
            BlockState wireState = level.getBlockState(current);
            if (!(wireState.getBlock() instanceof WireBlock wire)) continue;
            boolean isFeeder = false;
            for (Direction side : Direction.values()) {
                if (!wire.isConnected(wireState, side)) continue;
                BlockPos next = current.relative(side);
                BlockState state = level.getBlockState(next);
                if (state.getBlock() instanceof WireBlock other) {
                    if (other.isConnected(state, side.getOpposite()) && wires.add(next)) frontier.addLast(next);
                } else if (state.getBlock() instanceof PowerMachine machine && machine.canConnectWires(side.getOpposite())) {
                    if (machines.add(next)) isFeeder = true;
                }
            }
            if (isFeeder) {
                feeders.add(current);
                capacity = Math.min(capacity, wire.capacity());
            }
        }

        int supply = 0;
        int demand = 0;
        for (BlockPos machine : machines) {
            BlockState state = level.getBlockState(machine);
            if (state.getBlock() instanceof PowerSource source) supply += source.powerOutput(level, machine, state);
            if (state.getBlock() instanceof PowerConsumer consumer) demand += consumer.powerDemand(level, machine, state);
        }
        return new Grid(supply, demand, capacity == Integer.MAX_VALUE ? Integer.MAX_VALUE : capacity,
            new ArrayList<>(wires), feeders);
    }

    /**
     * Thermal protection. While a grid draws more than its weakest feeder can carry, that feeder accumulates heat;
     * sustained overload melts the insulation and destroys the wire with a shower of sparks and smoke. Cooling down
     * happens as soon as the load drops back under the rating.
     */
    private static void tickProtection(Level level, Grid grid, GridCache cache) {
        if (!grid.overloaded() || grid.feeders().isEmpty()) {
            cache.overloadTicks.clear();
            return;
        }
        for (BlockPos feeder : grid.feeders()) {
            long key = feeder.asLong();
            int heat = cache.overloadTicks.getOrDefault(key, 0) + 1;
            cache.overloadTicks.put(key, heat);
            if (level instanceof ServerLevel server && heat % 10 == 0) {
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                    feeder.getX() + .5, feeder.getY() + .2, feeder.getZ() + .5, 3, .15, .1, .15, .01);
                if (heat > OVERLOAD_FAILURE_TICKS / 2 && heat % 20 == 0) {
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                        feeder.getX() + .5, feeder.getY() + .2, feeder.getZ() + .5, 4, .1, .1, .1, .05);
                }
            }
            if (heat >= OVERLOAD_FAILURE_TICKS) {
                cache.overloadTicks.remove(key);
                level.destroyBlock(feeder, true);
                level.playSound(null, feeder, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, .6f, 1.2f);
                level.playSound(null, feeder, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, .5f, .8f);
            }
        }
    }

    /** Flips the {@code powered} visual state on wires to match whether the grid is actually energised. */
    private static void markEnergised(Level level, Grid grid) {
        boolean energised = grid.supply > 0 && !grid.brownout();
        for (BlockPos pos : grid.wires()) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof WireBlock && state.getValue(WireBlock.POWERED) != energised) {
                level.setBlock(pos, state.setValue(WireBlock.POWERED, energised), 2);
            }
        }
    }
}
