/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.quality;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Tracks the health state of a growing crop, including disease pressure and pest damage. Unhealthy fields develop
 * fungal infections, insect problems, or nutrient deficiencies. Good sanitation, crop rotation, and healthy soil
 * reduce these risks.
 *
 * @param vigour           overall plant health, 0..1. Affects growth speed and yield.
 * @param fungalPressure   accumulated fungal disease pressure, 0..1. High humidity + poor ventilation increases this.
 * @param pestPressure     accumulated insect/pest pressure, 0..1. Affected by crop rotation and companion planting.
 * @param nutrientDeficit  how deficient the plant is in nutrients, 0..1. Related to soil fertility and rotation.
 * @param daysSinceRotation how many consecutive seasons the same crop family has been planted here, affects pests.
 */
public record CropHealth(float vigour, float fungalPressure, float pestPressure,
                         float nutrientDeficit, int daysSinceRotation)
{
    public static final CropHealth HEALTHY = new CropHealth(1.0f, 0.0f, 0.0f, 0.0f, 0);

    public static final Codec<CropHealth> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.FLOAT.fieldOf("vigour").forGetter(CropHealth::vigour),
        Codec.FLOAT.fieldOf("fungal_pressure").forGetter(CropHealth::fungalPressure),
        Codec.FLOAT.fieldOf("pest_pressure").forGetter(CropHealth::pestPressure),
        Codec.FLOAT.fieldOf("nutrient_deficit").forGetter(CropHealth::nutrientDeficit),
        Codec.INT.fieldOf("days_since_rotation").forGetter(CropHealth::daysSinceRotation)
    ).apply(i, CropHealth::new));

    public static final StreamCodec<ByteBuf, CropHealth> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, CropHealth::vigour,
        ByteBufCodecs.FLOAT, CropHealth::fungalPressure,
        ByteBufCodecs.FLOAT, CropHealth::pestPressure,
        ByteBufCodecs.FLOAT, CropHealth::nutrientDeficit,
        ByteBufCodecs.VAR_INT, CropHealth::daysSinceRotation,
        CropHealth::new
    );

    /** Overall disease risk: combination of fungal, pest, and nutrient issues. */
    public float diseaseRisk()
    {
        return (fungalPressure * 0.4f + pestPressure * 0.3f + nutrientDeficit * 0.3f);
    }

    /** Whether this crop has visible disease symptoms. */
    public boolean hasVisibleSymptoms()
    {
        return fungalPressure > 0.3f || pestPressure > 0.4f || nutrientDeficit > 0.5f || vigour < 0.3f;
    }

    /** Visible symptom description for tooltip. */
    public String symptomDescription()
    {
        if (fungalPressure > 0.6f) return "heavy fungal infection";
        if (fungalPressure > 0.3f) return "mild mildew";
        if (pestPressure > 0.6f) return "severe pest damage";
        if (pestPressure > 0.3f) return "minor insect damage";
        if (nutrientDeficit > 0.6f) return "severe nutrient deficiency";
        if (nutrientDeficit > 0.3f) return "yellowing leaves";
        if (vigour < 0.3f) return "stunted and wilting";
        if (vigour < 0.5f) return "weak growth";
        return "healthy";
    }

    /** Apply soil quality to improve vigour. Good soil = healthy plants. */
    public CropHealth withSoilQuality(float soilQuality)
    {
        float vigourBoost = soilQuality * 0.3f;
        return new CropHealth(
            Math.min(1.0f, vigour + vigourBoost),
            Math.max(0, fungalPressure - soilQuality * 0.1f),
            Math.max(0, pestPressure - soilQuality * 0.05f),
            Math.max(0, nutrientDeficit - soilQuality * 0.15f),
            daysSinceRotation
        );
    }

    /** Tick disease progression. Humidity and temperature affect fungal pressure. */
    public CropHealth tick(float humidity, float temperature, boolean cropRotated)
    {
        float newFungal = fungalPressure;
        float newPest = pestPressure;
        float newDeficit = nutrientDeficit;
        int newDays = cropRotated ? 0 : daysSinceRotation + 1;

        // High humidity + warm temperature = fungal growth
        if (humidity > 0.7f && temperature > 15.0f) {
            newFungal = Math.min(1.0f, newFungal + 0.02f * (humidity - 0.5f));
        }
        // Good ventilation reduces fungal pressure
        if (humidity < 0.5f) {
            newFungal = Math.max(0, newFungal - 0.01f);
        }

        // Monoculture increases pest pressure
        if (newDays > 30) {
            newPest = Math.min(1.0f, newPest + 0.005f);
        }

        // Nutrients deplete over time, faster for heavy feeders
        newDeficit = Math.min(1.0f, newDeficit + 0.003f);

        // Low vigour from disease
        float newVigour = vigour - diseaseRisk() * 0.01f;
        newVigour = Math.max(0, Math.min(1.0f, newVigour));

        return new CropHealth(newVigour, newFungal, newPest, newDeficit, newDays);
    }

    /** Apply a treatment (fungicide, pesticide, compost) to reduce disease pressure. */
    public CropHealth treat(String treatmentType)
    {
        return switch (treatmentType) {
            case "fungicide" -> new CropHealth(vigour, Math.max(0, fungalPressure - 0.3f), pestPressure, nutrientDeficit, daysSinceRotation);
            case "pesticide" -> new CropHealth(vigour, fungalPressure, Math.max(0, pestPressure - 0.3f), nutrientDeficit, daysSinceRotation);
            case "compost" -> new CropHealth(vigour, fungalPressure, pestPressure, Math.max(0, nutrientDeficit - 0.3f), daysSinceRotation);
            case "crop_rotation" -> new CropHealth(vigour, fungalPressure * 0.5f, Math.max(0, pestPressure - 0.5f), nutrientDeficit * 0.5f, 0);
            default -> this;
        };
    }
}
