/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.health;

import net.minecraft.util.Mth;

/**
 * How dirty a mouthful of water is, on a single {@code [0, 1]} contamination scale.
 * <p>
 * The scale is the whole point of the water system: TerraFirmaCraft only distinguishes fresh water from salt water, so
 * every freshwater block in the world is equally safe to drink from. That is the thing this replaces. A cold, fast
 * mountain stream is genuinely close to safe; a warm stagnant pond in a swamp, downhill of everything, is not; and the
 * player can see the difference from the terrain before they drink.
 * <p>
 * Contamination is computed in {@link WaterSource} from the block, the fluid, the surrounding terrain, and the climate,
 * and it is then used for two things:
 * <ol>
 *     <li>the per-drink chance of picking up a waterborne pathogen, and</li>
 *     <li>the warning colour shown on the drink prompt, so a player learns the rule before they learn it the hard
 *     way.</li>
 * </ol>
 */
public enum WaterQuality
{
    /** Boiled, filtered, or otherwise treated. No pathogen risk at all. */
    CLEAN(0.00f, 0x55FF55, "clean"),
    /** Cold, fast, high-altitude running water. Very low risk, but not zero - Giardia lives in mountain streams. */
    PRISTINE(0.06f, 0x88FF88, "pristine"),
    /** Ordinary running water: a river, a stream with some flow. Low risk. */
    RUNNING(0.16f, 0xCCFF66, "running"),
    /** Still but open water - a large lake, a deep pool. Moderate risk. */
    STILL(0.34f, 0xFFDD55, "still"),
    /** Warm, shallow, stagnant water. Ponds, puddles, backwaters. High risk. */
    STAGNANT(0.58f, 0xFF8833, "stagnant"),
    /** Swamp water, muck, water sitting over mud in a lowland. Very high risk. */
    SWAMP(0.74f, 0xCC5522, "swamp"),
    /** Water that something has actually fouled - a well or pool downhill of waste. Near-certain infection. */
    FOUL(0.92f, 0x993333, "foul");

    private static final WaterQuality[] VALUES = values();

    private final float contamination;
    private final int color;
    private final String id;

    WaterQuality(float contamination, int color, String id)
    {
        this.contamination = contamination;
        this.color = color;
        this.id = id;
    }

    /** The baseline probability, per hand-drink, that this water carries something. Modified by climate and treatment. */
    public float contamination()
    {
        return contamination;
    }

    /** Colour used for the tooltip / action bar warning. Green through red as the water gets worse. */
    public int color()
    {
        return color;
    }

    public String id()
    {
        return id;
    }

    public String translationKey()
    {
        return "terravera.water_quality." + id;
    }

    /** @return {@code true} if drinking this untreated should warn the player. */
    public boolean isRisky()
    {
        return contamination >= STILL.contamination;
    }

    /**
     * Maps a raw contamination value back onto the nearest band. Used after climate and terrain modifiers have pushed
     * a source's contamination up or down off its starting band, so that the player-facing label stays honest.
     */
    public static WaterQuality fromContamination(float value)
    {
        final float clamped = Mth.clamp(value, 0f, 1f);
        WaterQuality best = CLEAN;
        float bestDistance = Float.MAX_VALUE;
        for (WaterQuality quality : VALUES)
        {
            final float distance = Math.abs(quality.contamination - clamped);
            if (distance < bestDistance)
            {
                bestDistance = distance;
                best = quality;
            }
        }
        return best;
    }
}
