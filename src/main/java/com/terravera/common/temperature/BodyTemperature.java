/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.temperature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

/**
 * The player's thermal state, stored as a serialised attachment so it survives logout.
 * <p>
 * There is deliberately no "cold bar" anywhere in the UI. This record is the entire state, and the player never sees
 * any of it directly - they see what their body is doing. The same approach as the disease and taste systems.
 *
 * @param core          core body temperature in Celsius
 * @param skinWetness   how wet the player's skin is, in {@code [0, 1]}, separate from their clothing
 * @param exertion      a smoothed measure of recent physical work, in {@code [0, 1]}
 * @param lastBand      the band the player was last told about, used to avoid repeating themselves
 * @param lastMessage   game tick of the last symptom message
 */
public record BodyTemperature(
    float core,
    float skinWetness,
    float exertion,
    int lastBand,
    long lastMessage
) {
    /** A player who has just spawned: comfortable, dry, rested. */
    public static final BodyTemperature EMPTY =
        new BodyTemperature(ThermalModel.NEUTRAL_CORE, 0f, 0f, 0, Long.MIN_VALUE);

    public static final Codec<BodyTemperature> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.FLOAT.optionalFieldOf("core", ThermalModel.NEUTRAL_CORE).forGetter(BodyTemperature::core),
        Codec.FLOAT.optionalFieldOf("skin_wetness", 0f).forGetter(BodyTemperature::skinWetness),
        Codec.FLOAT.optionalFieldOf("exertion", 0f).forGetter(BodyTemperature::exertion),
        Codec.INT.optionalFieldOf("last_band", 0).forGetter(BodyTemperature::lastBand),
        Codec.LONG.optionalFieldOf("last_message", Long.MIN_VALUE).forGetter(BodyTemperature::lastMessage)
    ).apply(i, BodyTemperature::new));

    public BodyTemperature
    {
        core = Mth.clamp(core, 27f, 43f);
        skinWetness = Mth.clamp(skinWetness, 0f, 1f);
        exertion = Mth.clamp(exertion, 0f, 1f);
    }

    public ThermalModel.Band band()
    {
        return ThermalModel.band(core);
    }

    public ThermalModel.Band lastAnnouncedBand()
    {
        for (ThermalModel.Band candidate : ThermalModel.Band.values())
        {
            if (candidate.severity() == lastBand) return candidate;
        }
        return ThermalModel.Band.COMFORTABLE;
    }

    public BodyTemperature withCore(float value)
    {
        return new BodyTemperature(value, skinWetness, exertion, lastBand, lastMessage);
    }

    public BodyTemperature withSkinWetness(float value)
    {
        return new BodyTemperature(core, value, exertion, lastBand, lastMessage);
    }

    public BodyTemperature withExertion(float value)
    {
        return new BodyTemperature(core, skinWetness, value, lastBand, lastMessage);
    }

    public BodyTemperature announced(ThermalModel.Band band, long tick)
    {
        return new BodyTemperature(core, skinWetness, exertion, band.severity(), tick);
    }

    /**
     * What survives death. A new body starts at a normal temperature - being resurrected mildly hypothermic would be
     * a strange and unhelpful punishment - but the player is dropped somewhere and their clothes are still wet.
     */
    public BodyTemperature onDeath()
    {
        return new BodyTemperature(ThermalModel.NEUTRAL_CORE, skinWetness, 0f, 0, Long.MIN_VALUE);
    }
}
