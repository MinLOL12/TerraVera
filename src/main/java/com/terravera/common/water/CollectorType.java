/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 */

package com.terravera.common.water;

/** Different passive collection mechanisms; rates are millibuckets per successful one-second collection tick. */
public enum CollectorType
{
    RAIN_CATCHER("rain_catcher", 4000, 12, 0.10f),
    DEW_COLLECTOR("dew_collector", 1000, 2, 0.04f),
    ROCK_BASIN("rock_basin", 2000, 5, 0.22f),
    SOLAR_STILL("solar_still", 1000, 1, 0.01f);

    private final String id;
    private final int capacity;
    private final int rate;
    private final float contamination;

    CollectorType(String id, int capacity, int rate, float contamination)
    {
        this.id = id;
        this.capacity = capacity;
        this.rate = rate;
        this.contamination = contamination;
    }

    public String id() { return id; }
    public int capacity() { return capacity; }
    public int rate() { return rate; }
    public float contamination() { return contamination; }

    public static CollectorType byId(String id)
    {
        for (CollectorType type : values()) if (type.id.equals(id)) return type;
        return ROCK_BASIN;
    }
}
