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
 * Tracks the condition of prepared farmland. Soil that has been cleared of stones, loosened, amended with compost,
 * and had its weeds removed produces better crops. Unprepared soil is hard, full of rocks and roots, and grows
 * poor crops.
 *
 * @param cleared     how well stones and roots have been removed, 0..1
 * @param loosened    how well the soil has been tilled and aerated, 0..1
 * @param fertility   nutrient level from compost/manure amendments, 0..1
 * @param weedFree    how clean the bed is of competing weeds, 0..1
 * @param moisture    current soil moisture, 0..1
 */
public record SoilCondition(float cleared, float loosened, float fertility, float weedFree, float moisture)
{
    public static final SoilCondition UNPREPARED = new SoilCondition(0.0f, 0.0f, 0.2f, 0.0f, 0.3f);
    public static final SoilCondition WELL_PREPARED = new SoilCondition(0.9f, 0.9f, 0.8f, 0.9f, 0.5f);

    public static final Codec<SoilCondition> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.FLOAT.fieldOf("cleared").forGetter(SoilCondition::cleared),
        Codec.FLOAT.fieldOf("loosened").forGetter(SoilCondition::loosened),
        Codec.FLOAT.fieldOf("fertility").forGetter(SoilCondition::fertility),
        Codec.FLOAT.fieldOf("weed_free").forGetter(SoilCondition::weedFree),
        Codec.FLOAT.fieldOf("moisture").forGetter(SoilCondition::moisture)
    ).apply(i, SoilCondition::new));

    public static final StreamCodec<ByteBuf, SoilCondition> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, SoilCondition::cleared,
        ByteBufCodecs.FLOAT, SoilCondition::loosened,
        ByteBufCodecs.FLOAT, SoilCondition::fertility,
        ByteBufCodecs.FLOAT, SoilCondition::weedFree,
        ByteBufCodecs.FLOAT, SoilCondition::moisture,
        SoilCondition::new
    );

    /**
     * Overall soil quality score used to scale crop growth and yield. A weighted combination of all factors.
     */
    public float overallQuality()
    {
        return (cleared * 0.20f + loosened * 0.25f + fertility * 0.30f + weedFree * 0.15f + moisture * 0.10f);
    }

    /** Soil condition label for tooltip display. */
    public String conditionLabel()
    {
        float q = overallQuality();
        if (q >= 0.8f) return "excellent";
        if (q >= 0.6f) return "good";
        if (q >= 0.4f) return "fair";
        if (q >= 0.2f) return "poor";
        return "unprepared";
    }

    /** Apply clearing: removes stones and roots. */
    public SoilCondition clear(float amount)
    {
        return new SoilCondition(Math.min(1.0f, cleared + amount), loosened, fertility, weedFree, moisture);
    }

    /** Apply loosening/tilling. */
    public SoilCondition loosen(float amount)
    {
        return new SoilCondition(cleared, Math.min(1.0f, loosened + amount), fertility, weedFree, moisture);
    }

    /** Apply compost or manure for fertility. */
    public SoilCondition amend(float amount)
    {
        return new SoilCondition(cleared, loosened, Math.min(1.0f, fertility + amount), weedFree, moisture);
    }

    /** Remove weeds. */
    public SoilCondition weed(float amount)
    {
        return new SoilCondition(cleared, loosened, fertility, Math.min(1.0f, weedFree + amount), moisture);
    }

    /** Update moisture (rain, irrigation). */
    public SoilCondition withMoisture(float newMoisture)
    {
        return new SoilCondition(cleared, loosened, fertility, weedFree, Math.min(1.0f, Math.max(0.0f, newMoisture)));
    }

    /** Natural decay: weeds grow back, fertility depletes, soil compacts slightly. */
    public SoilCondition decay(float rate)
    {
        return new SoilCondition(
            Math.max(0, cleared - rate * 0.02f),
            Math.max(0, loosened - rate * 0.05f),
            Math.max(0, fertility - rate * 0.03f),
            Math.max(0, weedFree - rate * 0.08f),
            moisture
        );
    }
}
