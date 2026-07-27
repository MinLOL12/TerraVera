/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.health;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.fluids.TFCFluids;
import net.dries007.tfc.util.climate.Climate;

import com.terravera.config.TerraVeraConfig;

/**
 * Works out how contaminated a particular block of water in the world is.
 * <p>
 * This is the "different water sources have different risks" half of the disease system, and it is deliberately
 * readable from the landscape rather than from a tooltip. The player should be able to look at where they are standing
 * and predict the answer:
 *
 * <table>
 *     <tr><th>What you're drinking from</th><th>Why</th></tr>
 *     <tr><td>Cold running water, high up</td><td>Flow and cold both suppress pathogen load. Safest natural water.</td></tr>
 *     <tr><td>A river</td><td>Moving, diluted, but drains everything upstream of it.</td></tr>
 *     <tr><td>A lake</td><td>Still, but deep and large enough to stay reasonable.</td></tr>
 *     <tr><td>A shallow pond or puddle</td><td>Warm, still, and small. Bacteria do very well in it.</td></tr>
 *     <tr><td>Water over mud in a lowland/swamp</td><td>The worst natural water in the game.</td></tr>
 *     <tr><td>Water near your own waste</td><td>Worse than anything natural. This is how you give yourself cholera.</td></tr>
 * </table>
 *
 * On top of the terrain read, warm climates raise contamination and freezing ones suppress it, because that is how
 * waterborne pathogen survival actually works.
 *
 * @param quality      the band this source falls into, after all modifiers
 * @param contamination the final {@code [0, 1]} contamination value used for infection rolls
 * @param salty        {@code true} for salt water, which TFC already punishes and which we do not double-punish
 */
public record WaterSource(WaterQuality quality, float contamination, boolean salty)
{
    /** A source that carries nothing at all - boiled, filtered, or otherwise treated. */
    public static final WaterSource TREATED = new WaterSource(WaterQuality.CLEAN, 0f, false);

    /** How far up/down we look to decide whether water is "shallow". */
    private static final int SHALLOW_DEPTH = 3;
    /** Radius of the neighbourhood sampled for mud, muck, and stagnation. */
    private static final int SAMPLE_RADIUS = 2;

    /**
     * Evaluates the water at {@code pos}. Only fluids that TFC considers drinkable water are given a quality; anything
     * else (lava, molten metal, milk in a barrel) returns {@code null}, and the caller should not apply any disease
     * logic to it.
     *
     * @return the contamination assessment, or {@code null} if this is not water the player would drink from the world
     */
    public static WaterSource evaluate(LevelReader level, BlockPos pos, BlockState state)
    {
        final FluidState fluid = state.getFluidState();
        if (fluid.isEmpty()) return null;

        final boolean salty = fluid.is(TFCTags.Fluids.SALT_WATER);
        final boolean fresh = fluid.is(FluidTags.WATER) || fluid.is(TFCTags.Fluids.ANY_FRESH_WATER);
        if (!salty && !fresh) return null;

        // Hot spring water is mineral-laden and already has its own TFC behaviour. Treat it as running water; it is
        // hot enough at the source that it is not a meaningful pathogen reservoir.
        if (fluid.getType() == TFCFluids.SPRING_WATER.getSource() || fluid.getType() == TFCFluids.SPRING_WATER.getFlowing())
        {
            return new WaterSource(WaterQuality.RUNNING, WaterQuality.RUNNING.contamination(), false);
        }

        WaterQuality base = baseQuality(level, pos, fluid);
        float contamination = base.contamination();

        // --- Terrain: what is this water sitting in, and what is sitting in it? --------------------------------
        final int muck = countMuck(level, pos);
        if (muck > 0)
        {
            // Water standing on and among mud, muck, and rotting plant matter. Each sampled muck block is a small
            // push towards swamp water.
            contamination += Math.min(0.30f, muck * 0.045f);
        }

        if (isShallow(level, pos))
        {
            // A puddle warms through, concentrates, and gets walked in. A deep pool does not.
            contamination += 0.10f;
        }

        // --- Climate: warm water grows things, frozen water does not ------------------------------------------
        if (level instanceof Level realLevel)
        {
            final float temperature = Climate.getAverageTemperature(realLevel, pos);
            // Below freezing, pathogen load collapses. Above ~25C it climbs sharply.
            contamination += Mth.clampedMap(temperature, -5f, 30f, -0.12f, 0.18f);

            // Very wet climates flush surface water; very dry ones concentrate whatever is in the last pool standing.
            final float rainfall = Climate.getAverageRainfall(realLevel, pos);
            contamination += Mth.clampedMap(rainfall, 50f, 450f, 0.06f, -0.06f);
        }

        contamination = Mth.clamp(contamination * TerraVeraConfig.SERVER.waterContaminationMultiplier.get().floatValue(), 0f, 1f);
        return new WaterSource(WaterQuality.fromContamination(contamination), contamination, salty);
    }

    /**
     * The starting band, from the fluid itself and how much it is moving. Flow is the single strongest natural signal:
     * water that is going somewhere is water that is not incubating anything.
     */
    private static WaterQuality baseQuality(LevelReader level, BlockPos pos, FluidState fluid)
    {
        // TFC's river water is, by construction, a flowing river.
        if (fluid.getType() == TFCFluids.RIVER_WATER.get())
        {
            return isCold(level, pos) && pos.getY() > 100 ? WaterQuality.PRISTINE : WaterQuality.RUNNING;
        }

        // Flowing (non-source) water is running water, wherever it came from.
        if (!fluid.isSource() && fluid.getType() != Fluids.EMPTY)
        {
            return isCold(level, pos) && pos.getY() > 100 ? WaterQuality.PRISTINE : WaterQuality.RUNNING;
        }

        // A standing source block. How big is the body of water it belongs to?
        final int neighbours = countAdjacentWater(level, pos);
        if (neighbours <= 2)
        {
            // A one- or two-block puddle. Nothing dilutes it and nothing moves it.
            return WaterQuality.STAGNANT;
        }
        return WaterQuality.STILL;
    }

    /** Cold, high water is the closest thing to safe natural water there is. */
    private static boolean isCold(LevelReader level, BlockPos pos)
    {
        return level instanceof Level realLevel && Climate.getAverageTemperature(realLevel, pos) < 8f;
    }

    private static int countAdjacentWater(LevelReader level, BlockPos pos)
    {
        int count = 0;
        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -SAMPLE_RADIUS; dx <= SAMPLE_RADIUS; dx++)
        {
            for (int dz = -SAMPLE_RADIUS; dz <= SAMPLE_RADIUS; dz++)
            {
                if (dx == 0 && dz == 0) continue;
                cursor.set(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
                if (!level.getFluidState(cursor).isEmpty()) count++;
            }
        }
        return count;
    }

    /**
     * Counts nearby blocks that indicate the water is sitting in filth - mud, muck, rotting plant matter, and TFC's
     * lowland soils. This is the mechanism that makes swamps dangerous without needing to hardcode a biome list.
     */
    private static int countMuck(LevelReader level, BlockPos pos)
    {
        int muck = 0;
        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -SAMPLE_RADIUS; dx <= SAMPLE_RADIUS; dx++)
        {
            for (int dy = -1; dy <= 1; dy++)
            {
                for (int dz = -SAMPLE_RADIUS; dz <= SAMPLE_RADIUS; dz++)
                {
                    cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    final BlockState neighbour = level.getBlockState(cursor);
                    if (neighbour.is(TFCTags.Blocks.MUD) || neighbour.is(TerraVeraHealthTags.Blocks.FOULS_WATER))
                    {
                        muck++;
                    }
                }
            }
        }
        return muck;
    }

    /** Shallow water warms through and gets stirred up; deep water does not. */
    private static boolean isShallow(LevelReader level, BlockPos pos)
    {
        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = 1; dy <= SHALLOW_DEPTH; dy++)
        {
            cursor.set(pos.getX(), pos.getY() - dy, pos.getZ());
            if (level.getFluidState(cursor).isEmpty())
            {
                return true;
            }
        }
        return false;
    }
}
