/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.greenhouse;

/**
 * The four tiers of greenhouse structure, from the simplest seedling protector to a fully climate-controlled
 * modern greenhouse. Each tier unlocks better temperature management, larger growing areas, and access to
 * more demanding crops.
 */
public enum GreenhouseTier
{
    /** A small glazed box that protects seedlings from frost. Minimal temperature buffer. */
    COLD_FRAME("cold_frame", 0, 0.2f, 0.1f, false, false),

    /** Wood frame covered with fabric or oiled cloth. Extends the season modestly. */
    HOOP_HOUSE("hoop_house", 1, 0.4f, 0.3f, false, false),

    /** A proper glass structure with good temperature control and ventilation options. */
    GLASS_GREENHOUSE("glass_greenhouse", 2, 0.75f, 0.6f, true, false),

    /** Powered ventilation, heating, automated irrigation, and full climate control. */
    MODERN_GREENHOUSE("modern_greenhouse", 3, 0.95f, 0.9f, true, true);

    private final String id;
    private final int level;
    private final float insulationRating;
    private final float solarCapture;
    private final boolean supportsVentilation;
    private final boolean supportsAutomation;

    GreenhouseTier(String id, int level, float insulationRating, float solarCapture,
                   boolean supportsVentilation, boolean supportsAutomation)
    {
        this.id = id;
        this.level = level;
        this.insulationRating = insulationRating;
        this.solarCapture = solarCapture;
        this.supportsVentilation = supportsVentilation;
        this.supportsAutomation = supportsAutomation;
    }

    public String id() { return id; }
    public int level() { return level; }
    public float insulationRating() { return insulationRating; }
    public float solarCapture() { return solarCapture; }
    public boolean supportsVentilation() { return supportsVentilation; }
    public boolean supportsAutomation() { return supportsAutomation; }

    /**
     * Returns the effective temperature inside an unventilated greenhouse given the outside temperature and
     * sunlight exposure. Solar gain is the greenhouse effect: sunlight passes through glass and is trapped as heat.
     */
    public float effectiveTemperature(float outsideTemp, float sunlightExposure, boolean daytime)
    {
        float solarGain = daytime ? sunlightExposure * solarCapture * 12.0f : 0.0f;
        float insulation = insulationRating;
        // The greenhouse traps heat proportional to its insulation. Poor insulation lets more escape.
        float retained = solarGain * insulation;
        // At night, the greenhouse loses heat more slowly than open air but still loses it.
        float nightLoss = daytime ? 0.0f : (1.0f - insulation) * 3.0f;
        return outsideTemp + retained - nightLoss;
    }

    /**
     * Returns the effective temperature after ventilation is applied. Venting reduces temperature by exchanging
     * inside air with outside air, but cannot go below outside temperature.
     */
    public float ventilatedTemperature(float insideTemp, float outsideTemp, float ventilationRate)
    {
        if (!supportsVentilation) return insideTemp;
        float cooling = (insideTemp - outsideTemp) * ventilationRate * 0.5f;
        return insideTemp - Math.max(0, cooling);
    }

    /**
     * Humidity inside the greenhouse. Plants transpire, raising humidity. Ventilation lowers it.
     */
    public float effectiveHumidity(float plantTranspiration, float ventilationRate, float outsideHumidity)
    {
        float baseHumidity = outsideHumidity * 0.3f + plantTranspiration * 0.7f;
        float ventilationReduction = ventilationRate * 0.4f;
        return Math.min(1.0f, Math.max(0.1f, baseHumidity - ventilationReduction));
    }

    public static GreenhouseTier byLevel(int level)
    {
        for (GreenhouseTier tier : values())
        {
            if (tier.level == level) return tier;
        }
        return COLD_FRAME;
    }
}
