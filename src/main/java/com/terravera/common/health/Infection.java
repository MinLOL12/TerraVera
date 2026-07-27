/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.health;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A single infection the player is currently carrying, at some point along its course.
 * <p>
 * An infection is not a status effect. It is a clock. When the player is exposed, an {@code Infection} is created with
 * the tick it started; nothing at all happens to the player until {@code contractedTick + incubationTicks} passes, at
 * which point it becomes {@link Stage#SYMPTOMATIC} and starts applying effects. That gap is the point: you drink from
 * the swamp on day three and you fall ill on day five, so the lesson has to be learned by reasoning about what you did,
 * not by watching a health bar move.
 * <p>
 * The record is immutable; progression returns new instances. This keeps the codec trivial and makes the state machine
 * easy to test.
 *
 * @param illnessId      the illness this is an instance of
 * @param contractedTick the calendar tick at which exposure happened
 * @param treatmentTicks accumulated treatment progress, in ticks of shortening applied by remedies
 * @param severityScale  a multiplier in {@code [0.25, 1.5]} on symptom intensity, from nutrition and treatment
 * @param acknowledged   whether the player has been told they are ill, so we only announce onset once
 */
public record Infection(
    ResourceLocation illnessId,
    long contractedTick,
    int treatmentTicks,
    float severityScale,
    boolean acknowledged
) {
    public static final Codec<Infection> CODEC = RecordCodecBuilder.create(i -> i.group(
        ResourceLocation.CODEC.fieldOf("illness").forGetter(Infection::illnessId),
        Codec.LONG.fieldOf("contracted").forGetter(Infection::contractedTick),
        Codec.INT.optionalFieldOf("treatment", 0).forGetter(Infection::treatmentTicks),
        Codec.FLOAT.optionalFieldOf("severity_scale", 1f).forGetter(Infection::severityScale),
        Codec.BOOL.optionalFieldOf("acknowledged", false).forGetter(Infection::acknowledged)
    ).apply(i, Infection::new));

    public static Infection contract(ResourceLocation illnessId, long tick, float severityScale)
    {
        return new Infection(illnessId, tick, 0, Mth.clamp(severityScale, 0.25f, 1.5f), false);
    }

    /** Where along its course this infection currently is. */
    public enum Stage
    {
        /** Caught, but not yet showing. No effects, no tooltip, nothing. */
        INCUBATING,
        /** Actively ill. Effects are applied every tick. */
        SYMPTOMATIC,
        /** Run its course, or been treated to completion. Ready to be cleared. */
        RESOLVED
    }

    public Stage stage(Illness illness, long now)
    {
        final long elapsed = now - contractedTick + treatmentTicks;
        if (elapsed < illness.incubationTicks()) return Stage.INCUBATING;
        if (elapsed < illness.totalTicks()) return Stage.SYMPTOMATIC;
        return Stage.RESOLVED;
    }

    /**
     * How far through the symptomatic phase this infection is, in {@code [0, 1]}. Used to ramp symptoms up at onset and
     * back down as the player recovers, so an illness feels like it has a shape rather than switching on and off.
     */
    public float symptomaticProgress(Illness illness, long now)
    {
        final long elapsed = now - contractedTick + treatmentTicks - illness.incubationTicks();
        if (elapsed <= 0) return 0f;
        return Mth.clamp((float) elapsed / Math.max(1, illness.durationTicks()), 0f, 1f);
    }

    /**
     * Symptom intensity at this moment: a trapezoid that rises over the first fifth of the illness, plateaus, then
     * falls away over the last third as the player convalesces.
     */
    public float intensity(Illness illness, long now)
    {
        final float progress = symptomaticProgress(illness, now);
        final float shape;
        if (progress < 0.2f)
        {
            shape = Mth.clampedMap(progress, 0f, 0.2f, 0.35f, 1f);
        }
        else if (progress > 0.65f)
        {
            shape = Mth.clampedMap(progress, 0.65f, 1f, 1f, 0.25f);
        }
        else
        {
            shape = 1f;
        }
        return Mth.clamp(shape * severityScale, 0f, 1.5f);
    }

    /** Applies a remedy, shortening the remaining course and (optionally) blunting the symptoms. */
    public Infection treated(int ticksShortened, float severityReduction)
    {
        return new Infection(illnessId, contractedTick, treatmentTicks + Math.max(0, ticksShortened),
            Mth.clamp(severityScale - severityReduction, 0.15f, 1.5f), acknowledged);
    }

    public Infection acknowledge()
    {
        return acknowledged ? this : new Infection(illnessId, contractedTick, treatmentTicks, severityScale, true);
    }

    /** Ticks remaining until the player is well again. */
    public long ticksRemaining(Illness illness, long now)
    {
        return Math.max(0, illness.totalTicks() - (now - contractedTick + treatmentTicks));
    }
}
