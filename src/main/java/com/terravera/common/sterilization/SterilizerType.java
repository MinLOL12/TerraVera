/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.sterilization;

import com.terravera.common.health.WaterTreatment;

/**
 * The five placeable, animated water-sterilization machines.
 * <p>
 * Like the four passive collectors, all five machines share one block class, one block entity, and one GeckoLib
 * renderer; the differences live here. Each machine is an honest model of a real device:
 * <ul>
 *     <li><strong>SODIS rack</strong> - glass bottles on a wooden rack, tilted into the sun. Sunlight alone,
 *     given a quarter day of clear sky, disinfects.</li>
 *     <li><strong>Bio-sand filter</strong> - a barrel of graded sand and gravel with a living layer on top. Gravity
 *     does the work; the biology does the killing.</li>
 *     <li><strong>Distillation still</strong> - a copper pot, a dome, and a condenser arm. Boil and condense; what
 *     comes out is as clean as physics allows.</li>
 *     <li><strong>UV sterilizer</strong> - a chamber with a UV lamp. The radiation shreds DNA; almost nothing survives.</li>
 *     <li><strong>Clarifier</strong> - a settling tank with a slow stirring paddle. Let the mud fall out; the mud
 *     takes the load with it.</li>
 * </ul>
 *
 * @param id             registry id, also used for the geo model, texture, and blockstate
 * @param capacity       tank capacity in millibuckets
 * @param processTicks   how many ticks a full batch takes (TFC's day is 24000 ticks, so a SODIS batch is a quarter day)
 * @param requiresSun    {@code true} if processing only advances in clear daylight (SODIS is a solar device)
 * @param output         the treatment a finished batch carries
 * @param method         the catalogue entry this machine embodies
 */
public enum SterilizerType
{
    SODIS_RACK("sodis_rack", 2000, 6000, true, WaterTreatment.Treatment.SOLAR_DISINFECTED, SterilizationMethods.SODIS),
    BIO_SAND_FILTER("bio_sand_filter", 4000, 2400, false, WaterTreatment.Treatment.BIO_FILTERED, SterilizationMethods.BIO_SAND),
    DISTILLATION_STILL("distillation_still", 4000, 3600, false, WaterTreatment.Treatment.DISTILLED, SterilizationMethods.DISTILLATION),
    UV_STERILIZER("uv_sterilizer", 2000, 1200, false, WaterTreatment.Treatment.UV_STERILIZED, SterilizationMethods.UV),
    CLARIFIER("clarifier", 8000, 4800, false, WaterTreatment.Treatment.SETTLED, SterilizationMethods.SETTLING);

    private final String id;
    private final int capacity;
    private final int processTicks;
    private final boolean requiresSun;
    private final WaterTreatment.Treatment output;
    private final SterilizationMethod method;

    SterilizerType(String id, int capacity, int processTicks, boolean requiresSun,
                   WaterTreatment.Treatment output, SterilizationMethod method)
    {
        this.id = id;
        this.capacity = capacity;
        this.processTicks = processTicks;
        this.requiresSun = requiresSun;
        this.output = output;
        this.method = method;
    }

    public String id() { return id; }
    public int capacity() { return capacity; }
    public int processTicks() { return processTicks; }
    public boolean requiresSun() { return requiresSun; }
    public WaterTreatment.Treatment output() { return output; }
    public SterilizationMethod method() { return method; }

    public static SterilizerType byId(String id)
    {
        for (SterilizerType type : values())
        {
            if (type.id.equals(id)) return type;
        }
        return SODIS_RACK;
    }
}
