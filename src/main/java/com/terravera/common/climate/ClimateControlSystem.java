package com.terravera.common.climate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.terravera.common.temperature.Shelter;
import com.terravera.common.power.PowerNetwork;

/** Server-authoritative late-game room climate controller. Cooling is heat transfer: the indoor coil removes heat
 * only while the outdoor condenser has somewhere warmer to reject it, and poor shells leak that work away. */
public final class ClimateControlSystem {
    public record Controller(int target, int speed, boolean programmed, float maintenance) {
        public static final Controller FACTORY = new Controller(22, 2, false, 1f);
        public Controller target(int value) { return new Controller(Mth.clamp(value, 16, 30), speed, programmed, maintenance); }
        public Controller speed(int value) { return new Controller(target, Mth.clamp(value, 1, 5), programmed, maintenance); }
        public Controller program() { return new Controller(target, speed, true, maintenance); }
        public Controller service() { return new Controller(target, speed, programmed, 1f); }
    }
    private static final Map<Long, Controller> CONTROLLERS = new ConcurrentHashMap<>();
    private ClimateControlSystem() {}
    public static Controller get(BlockPos pos) { return CONTROLLERS.getOrDefault(pos.asLong(), Controller.FACTORY); }
    public static void put(BlockPos pos, Controller controller) { CONTROLLERS.put(pos.asLong(), controller); }
    public static void remove(BlockPos pos) { CONTROLLERS.remove(pos.asLong()); }

    /** Applies nearby unit output to a player's room. Returns the conditioned air temperature, not a magic body-temp edit. */
    public static float condition(Level level, BlockPos player, Shelter shell, float ambient) {
        BlockPos unit = nearestUnit(level, player);
        if (unit == null) return ambient;
        Controller c = get(unit);
        if (!c.programmed() || c.maintenance() < .12f || !shell.isIndoors()) return ambient;
        // Sealing and insulation are independent: stone alone is heavy, but a leaky stone hall is expensive to cool.
        float building = shell.sealing() * (.25f + .75f * shell.insulation());
        if (building < .18f) return ambient;
        float demand = demand(c, ambient, building);
        if (availablePower(level, unit) < demand) return ambient; // Brownout: compressor cannot start.
        float moved = Math.min(Math.max(0, ambient - c.target()), c.speed() * 1.35f * building * c.maintenance());
        // The condenser receives the same heat plus compressor work. This is intentionally exposed as a local heat load.
        rejectHeat(level, unit, moved + demand * .08f);
        put(unit, new Controller(c.target(), c.speed(), c.programmed(), Math.max(0f, c.maintenance() - demand * .000035f)));
        return ambient - moved;
    }
    public static int demand(Controller c, float ambient, float building) {
        return Math.round((35 + 24 * c.speed() + 7 * Math.max(0, ambient - c.target())) * (1.15f - building) / Math.max(.2f, c.maintenance()));
    }
    private static BlockPos nearestUnit(Level level, BlockPos origin) {
        BlockPos best = null; double distance = 81;
        for (BlockPos pos : CONTROLLERS.keySet().stream().map(BlockPos::of).toList()) {
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("terravera") || !BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().equals("air_conditioner")) continue;
            double d = pos.distSqr(origin); if (d < distance) { distance = d; best = pos; }
        }
        return best;
    }
    /** The compressor needs an actual contiguous copper-wire run, not merely a generator somewhere nearby. */
    private static int availablePower(Level level, BlockPos origin) {
        return PowerNetwork.availablePower(level, origin);
    }
    private static void rejectHeat(Level level, BlockPos unit, float heat) { /* Hook for future regional microclimate / condenser particles. */ }
}
