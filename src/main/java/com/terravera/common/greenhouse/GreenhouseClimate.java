/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.greenhouse;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The complete climate state of a greenhouse interior. Tracks temperature, humidity, ventilation, and irrigation
 * so that the greenhouse can be simulated tick by tick. The greenhouse is not a magic yield boost - it is a
 * microclimate that the player must manage.
 *
 * @param tier              the structural tier of the greenhouse
 * @param temperatureC      current internal temperature in Celsius
 * @param humidity          current internal humidity, 0..1
 * @param ventilationOpen   whether vents/windows are open (0 = closed, 1 = fully open)
 * @param irrigationActive  whether drip irrigation or automated watering is running
 * @param soilMoisture      current soil moisture level inside, 0..1
 * @param glassCoverage     fraction of the structure that is glazed, 0..1
 * @param orientationBonus  bonus from building orientation relative to sun, -0.2..0.2
 * @param heatingOn         whether the heater is active (modern greenhouse only)
 * @param coolingOn         whether mechanical cooling is active (modern greenhouse only)
 */
public record GreenhouseClimate(
    int tier,
    float temperatureC,
    float humidity,
    float ventilationOpen,
    boolean irrigationActive,
    float soilMoisture,
    float glassCoverage,
    float orientationBonus,
    boolean heatingOn,
    boolean coolingOn
)
{
    public static final GreenhouseClimate DEFAULT = new GreenhouseClimate(
        0, 20.0f, 0.5f, 0.0f, false, 0.3f, 0.5f, 0.0f, false, false
    );

    public static final Codec<GreenhouseClimate> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.fieldOf("tier").forGetter(GreenhouseClimate::tier),
        Codec.FLOAT.fieldOf("temperature_c").forGetter(GreenhouseClimate::temperatureC),
        Codec.FLOAT.fieldOf("humidity").forGetter(GreenhouseClimate::humidity),
        Codec.FLOAT.fieldOf("ventilation_open").forGetter(GreenhouseClimate::ventilationOpen),
        Codec.BOOL.fieldOf("irrigation_active").forGetter(GreenhouseClimate::irrigationActive),
        Codec.FLOAT.fieldOf("soil_moisture").forGetter(GreenhouseClimate::soilMoisture),
        Codec.FLOAT.fieldOf("glass_coverage").forGetter(GreenhouseClimate::glassCoverage),
        Codec.FLOAT.fieldOf("orientation_bonus").forGetter(GreenhouseClimate::orientationBonus),
        Codec.BOOL.fieldOf("heating_on").forGetter(GreenhouseClimate::heatingOn),
        Codec.BOOL.fieldOf("cooling_on").forGetter(GreenhouseClimate::coolingOn)
    ).apply(i, GreenhouseClimate::new));

    /**
     * Written by hand because {@link StreamCodec#composite} only supports up to six components and this record
     * has ten.
     */
    public static final StreamCodec<ByteBuf, GreenhouseClimate> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public GreenhouseClimate decode(ByteBuf buf)
        {
            return new GreenhouseClimate(
                ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.FLOAT.decode(buf),
                ByteBufCodecs.FLOAT.decode(buf),
                ByteBufCodecs.FLOAT.decode(buf),
                ByteBufCodecs.BOOL.decode(buf),
                ByteBufCodecs.FLOAT.decode(buf),
                ByteBufCodecs.FLOAT.decode(buf),
                ByteBufCodecs.FLOAT.decode(buf),
                ByteBufCodecs.BOOL.decode(buf),
                ByteBufCodecs.BOOL.decode(buf)
            );
        }

        @Override
        public void encode(ByteBuf buf, GreenhouseClimate value)
        {
            ByteBufCodecs.VAR_INT.encode(buf, value.tier());
            ByteBufCodecs.FLOAT.encode(buf, value.temperatureC());
            ByteBufCodecs.FLOAT.encode(buf, value.humidity());
            ByteBufCodecs.FLOAT.encode(buf, value.ventilationOpen());
            ByteBufCodecs.BOOL.encode(buf, value.irrigationActive());
            ByteBufCodecs.FLOAT.encode(buf, value.soilMoisture());
            ByteBufCodecs.FLOAT.encode(buf, value.glassCoverage());
            ByteBufCodecs.FLOAT.encode(buf, value.orientationBonus());
            ByteBufCodecs.BOOL.encode(buf, value.heatingOn());
            ByteBufCodecs.BOOL.encode(buf, value.coolingOn());
        }
    };

    /**
     * Tick the greenhouse climate forward one step. Called from the block entity's server tick.
     *
     * @param outsideTemp       current outside temperature in Celsius
     * @param outsideHumidity   current outside humidity, 0..1
     * @param sunlightExposure  sunlight intensity at current time of day, 0..1
     * @param daytime           whether it is currently daytime
     * @param raining           whether it is currently raining outside
     * @param plantCount        number of crop blocks inside the greenhouse
     */
    public GreenhouseClimate tick(float outsideTemp, float outsideHumidity, float sunlightExposure,
                                  boolean daytime, boolean raining, int plantCount)
    {
        GreenhouseTier t = GreenhouseTier.byLevel(tier);

        // Temperature: greenhouse effect from solar gain, modified by ventilation
        float baseTemp = t.effectiveTemperature(outsideTemp, sunlightExposure + orientationBonus, daytime);

        // Heating adds temperature (modern greenhouse only)
        if (heatingOn && t.supportsAutomation()) {
            baseTemp += 5.0f;
        }

        // Cooling removes temperature (modern greenhouse only)
        if (coolingOn && t.supportsAutomation()) {
            baseTemp = Math.max(outsideTemp, baseTemp - 8.0f);
        }

        // Ventilation moderates toward outside temperature
        float effectiveTemp = t.ventilatedTemperature(baseTemp, outsideTemp, ventilationOpen);

        // Humidity: plants transpire, ventilation exchanges with outside
        float transpiration = Math.min(1.0f, plantCount * 0.05f);
        float effectiveHumidity = t.effectiveHumidity(transpiration, ventilationOpen, outsideHumidity);

        // Rain outside raises outside humidity, which eventually seeps in
        if (raining) {
            effectiveHumidity = Math.min(1.0f, effectiveHumidity + 0.1f);
        }

        // Soil moisture: irrigation adds moisture, plants consume it, evaporation removes it
        float newSoilMoisture = soilMoisture;
        if (irrigationActive) {
            newSoilMoisture = Math.min(1.0f, newSoilMoisture + 0.02f);
        }
        // Plants consume water
        newSoilMoisture -= plantCount * 0.003f;
        // Evaporation from humidity differential
        if (effectiveHumidity < 0.4f) {
            newSoilMoisture -= 0.005f;
        }
        newSoilMoisture = Math.max(0, Math.min(1.0f, newSoilMoisture));

        return new GreenhouseClimate(tier, effectiveTemp, effectiveHumidity, ventilationOpen,
            irrigationActive, newSoilMoisture, glassCoverage, orientationBonus, heatingOn, coolingOn);
    }

    /** Whether conditions are suitable for plant growth (not too hot, not too cold). */
    public boolean isGrowable()
    {
        return temperatureC >= 2.0f && temperatureC <= 45.0f && soilMoisture >= 0.1f;
    }

    /** Whether conditions are too hot and need ventilation or cooling. */
    public boolean isOverheating()
    {
        return temperatureC > 38.0f;
    }

    /** Whether humidity is dangerously high, promoting fungal disease. */
    public boolean isTooHumid()
    {
        return humidity > 0.85f;
    }

    /** Growth rate modifier based on current conditions. 0 = no growth, 1 = optimal. */
    public float growthModifier()
    {
        if (!isGrowable()) return 0.0f;

        // Temperature sweet spot: 18-28°C
        float tempMod;
        if (temperatureC >= 18.0f && temperatureC <= 28.0f) {
            tempMod = 1.0f;
        } else if (temperatureC < 18.0f) {
            tempMod = Math.max(0.1f, (temperatureC - 2.0f) / 16.0f);
        } else {
            tempMod = Math.max(0.1f, 1.0f - (temperatureC - 28.0f) / 17.0f);
        }

        // Moisture: optimal around 0.4-0.7
        float moistMod = soilMoisture >= 0.4f && soilMoisture <= 0.7f ? 1.0f :
            Math.max(0.2f, 1.0f - Math.abs(soilMoisture - 0.55f) * 2.0f);

        // Humidity: too high promotes disease, too low wastes water
        float humidMod = humidity >= 0.4f && humidity <= 0.75f ? 1.0f :
            Math.max(0.3f, 1.0f - Math.abs(humidity - 0.6f) * 1.5f);

        return (tempMod * 0.5f + moistMod * 0.3f + humidMod * 0.2f);
    }

    public GreenhouseClimate setVentilation(float open)
    {
        return new GreenhouseClimate(tier, temperatureC, humidity, Math.max(0, Math.min(1, open)),
            irrigationActive, soilMoisture, glassCoverage, orientationBonus, heatingOn, coolingOn);
    }

    public GreenhouseClimate setIrrigation(boolean active)
    {
        return new GreenhouseClimate(tier, temperatureC, humidity, ventilationOpen,
            active, soilMoisture, glassCoverage, orientationBonus, heatingOn, coolingOn);
    }

    public GreenhouseClimate setHeating(boolean on)
    {
        return new GreenhouseClimate(tier, temperatureC, humidity, ventilationOpen,
            irrigationActive, soilMoisture, glassCoverage, orientationBonus, on, coolingOn);
    }

    public GreenhouseClimate setCooling(boolean on)
    {
        return new GreenhouseClimate(tier, temperatureC, humidity, ventilationOpen,
            irrigationActive, soilMoisture, glassCoverage, orientationBonus, heatingOn, on);
    }
}
