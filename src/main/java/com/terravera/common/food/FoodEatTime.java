/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.food;

import net.minecraft.world.item.ItemStack;

import net.dries007.tfc.common.component.size.IItemSize;
import net.dries007.tfc.common.component.size.ItemSizeManager;
import net.dries007.tfc.common.component.size.Size;
import net.dries007.tfc.common.component.size.Weight;

import com.terravera.config.TerraVeraConfig;

/**
 * Scales how long it takes to eat a piece of food based on how big a bite it actually is.
 * <p>
 * Vanilla (and TFC, which doesn't touch this) has every food take exactly 1.6 seconds (32 ticks) to eat, whether
 * it's a single berry or a whole roast. That's the thing this class fixes: it reads the item's TFC
 * {@link Size} (and, as a secondary factor, its {@link Weight}) - see the size and weight documentation on the TFC
 * wiki - and derives a duration from it, so a tiny mouthful is a couple of seconds and a huge cut of meat is closer
 * to a minute.
 * <p>
 * Bands (before the weight adjustment, at {@link Weight#MEDIUM}):
 * <ul>
 *     <li>{@code TINY} / {@code VERY_SMALL} - a bite-sized snack, ~4 seconds.</li>
 *     <li>{@code SMALL} - ~10-15 seconds.</li>
 *     <li>{@code NORMAL} - ~15-20 seconds, i.e. most ordinary meals.</li>
 *     <li>{@code LARGE} / {@code VERY_LARGE} / {@code HUGE} - ~20-50 seconds, for things you'd actually have to sit
 *     down and work through.</li>
 * </ul>
 */
public final class FoodEatTime
{
    private static final int TICKS_PER_SECOND = 20;

    /** {@code {base, min, max}} eating duration in ticks, indexed by {@link Size#ordinal()}. */
    private static final int[][] SIZE_TICKS = {
        // TINY
        {4 * TICKS_PER_SECOND, 3 * TICKS_PER_SECOND + 10, 4 * TICKS_PER_SECOND + 10},
        // VERY_SMALL
        {4 * TICKS_PER_SECOND + 10, 4 * TICKS_PER_SECOND, 5 * TICKS_PER_SECOND},
        // SMALL
        {11 * TICKS_PER_SECOND, 8 * TICKS_PER_SECOND, 15 * TICKS_PER_SECOND},
        // NORMAL
        {17 * TICKS_PER_SECOND, 10 * TICKS_PER_SECOND, 20 * TICKS_PER_SECOND},
        // LARGE
        {28 * TICKS_PER_SECOND, 20 * TICKS_PER_SECOND, 35 * TICKS_PER_SECOND},
        // VERY_LARGE
        {38 * TICKS_PER_SECOND, 28 * TICKS_PER_SECOND, 45 * TICKS_PER_SECOND},
        // HUGE
        {47 * TICKS_PER_SECOND, 35 * TICKS_PER_SECOND, 50 * TICKS_PER_SECOND},
    };

    /** Weight only nudges the duration within its size's band - a heavy item of the same size is a bit slower to get through. */
    private static float weightMultiplier(Weight weight)
    {
        return switch (weight)
        {
            case VERY_LIGHT -> 0.85f;
            case LIGHT -> 0.93f;
            case MEDIUM -> 1.0f;
            case HEAVY -> 1.1f;
            case VERY_HEAVY -> 1.25f;
        };
    }

    /**
     * @return how long, in ticks, eating {@code stack} should take. Tiny and very small items skip the weight
     * adjustment entirely, since a "heavy" crumb is still a single bite.
     */
    public static int getEatDurationTicks(ItemStack stack)
    {
        final IItemSize size = ItemSizeManager.get(stack);
        final Size itemSize = size.getSize(stack);
        final int[] band = SIZE_TICKS[itemSize.ordinal()];

        if (itemSize == Size.TINY || itemSize == Size.VERY_SMALL)
        {
            return scale(band[0]);
        }

        final Weight weight = size.getWeight(stack);
        final int scaled = Math.round(band[0] * weightMultiplier(weight));
        final int clamped = Math.max(band[1], Math.min(band[2], scaled));
        return scale(clamped);
    }

    private static int scale(int ticks)
    {
        final double multiplier = TerraVeraConfig.SERVER.foodEatTimeMultiplier.get();
        return Math.max(1, (int) Math.round(ticks * multiplier));
    }

    private FoodEatTime() {}
}
