/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 */

package com.terravera.common.greenhouse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import com.terravera.common.blocks.TerraVeraBlockEntities;
import com.terravera.common.farming.PreparedFarmlandBlockEntity;
import com.terravera.common.quality.SoilCondition;

/**
 * Scans the area around a player or position to find all greenhouse block entities and prepared farmland blocks.
 * Used by the farming event handler to apply greenhouse bonuses and soil quality to crop growth calculations.
 * This is the bridge between the greenhouse climate simulation and the crop growth mechanics.
 */
public final class GreenhouseScanner
{
    /** Maximum scan radius for finding greenhouse blocks. */
    private static final int SCAN_RADIUS = 5;

    /**
     * Scan the area around a position for greenhouse block entities. Returns the best growth modifier
     * from any greenhouse found, or 1.0 if none.
     */
    public static float scanForGreenhouseBonus(Level level, BlockPos pos)
    {
        if (level.isClientSide()) return 1.0f;

        float bestBonus = 0.0f;
        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++)
        {
            for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++)
            {
                for (int dy = 0; dy <= SCAN_RADIUS; dy++)
                {
                    BlockPos check = pos.offset(dx, dy, dz);
                    if (level.getBlockEntity(check) instanceof GreenhouseBlockEntity greenhouse)
                    {
                        bestBonus = Math.max(bestBonus, greenhouse.climate().growthModifier());
                    }
                }
            }
        }
        return bestBonus;
    }

    /**
     * Scan the area around a position for prepared farmland. Returns the best soil condition found,
     * or UNPREPARED if none.
     */
    public static SoilCondition scanForSoilCondition(Level level, BlockPos pos)
    {
        if (level.isClientSide()) return SoilCondition.UNPREPARED;

        SoilCondition best = SoilCondition.UNPREPARED;
        BlockPos below = pos.below();

        // Check directly below first (the block the crop is on)
        if (level.getBlockEntity(below) instanceof PreparedFarmlandBlockEntity be)
        {
            best = be.soilCondition();
        }

        // Also check adjacent soil blocks (crops share nutrients with neighbors)
        for (int dx = -1; dx <= 1; dx++)
        {
            for (int dz = -1; dz <= 1; dz++)
            {
                if (dx == 0 && dz == 0) continue;
                BlockPos adjacent = below.offset(dx, 0, dz);
                if (level.getBlockEntity(adjacent) instanceof PreparedFarmlandBlockEntity be)
                {
                    SoilCondition adjacent_soil = be.soilCondition();
                    // Average the fertility (crops share nutrients)
                    if (adjacent_soil.overallQuality() > best.overallQuality())
                    {
                        best = new SoilCondition(
                            (best.cleared() + adjacent_soil.cleared()) * 0.5f,
                            (best.loosened() + adjacent_soil.loosened()) * 0.5f,
                            (best.fertility() + adjacent_soil.fertility()) * 0.5f,
                            (best.weedFree() + adjacent_soil.weedFree()) * 0.5f,
                            (best.moisture() + adjacent_soil.moisture()) * 0.5f
                        );
                    }
                }
            }
        }
        return best;
    }

    /**
     * Count the number of different crop types growing near a greenhouse. Used to detect monoculture
     * which increases pest pressure.
     */
    public static int countCropDiversity(ServerLevel level, BlockPos center, int radius)
    {
        java.util.Set<String> cropTypes = new java.util.HashSet<>();
        for (int dx = -radius; dx <= radius; dx++)
        {
            for (int dz = -radius; dz <= radius; dz++)
            {
                BlockPos pos = center.offset(dx, 0, dz);
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof net.minecraft.world.level.block.CropBlock)
                {
                    cropTypes.add(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath());
                }
            }
        }
        return cropTypes.size();
    }

    private GreenhouseScanner() {}
}
