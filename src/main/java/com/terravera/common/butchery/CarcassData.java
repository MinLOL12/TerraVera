/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.butchery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Everything a carcass remembers about itself: what it was, when it died, how far it has been taken apart, and how
 * well that work has gone so far.
 * <p>
 * {@code workmanship} is the running average quality of the cuts made so far. It is carried forward rather than
 * recomputed per stage because a carcass that was badly skinned is a worse carcass to draw and to break down - the
 * hide is nicked, the muscle is torn, and no amount of care later fully recovers that. It is the mechanical reason
 * to slow down and do the early stages properly.
 *
 * @param species     size and build class of the animal
 * @param stage       how far the butchering has progressed
 * @param deathTime   the game time, in ticks, at which the animal died
 * @param workmanship running average of cut quality so far, 0..1
 * @param waste       fraction of the animal already lost to bad knife work, 0..1
 */
public record CarcassData(CarcassSpecies species, ButcheryStage stage, long deathTime, float workmanship, float waste)
{
    public static final CarcassData DEFAULT =
        new CarcassData(CarcassSpecies.SMALL_GAME, ButcheryStage.INTACT, 0L, 0.5f, 0f);

    /** Ticks in an in-game hour. */
    public static final long TICKS_PER_HOUR = 1000L;

    public static final Codec<CarcassData> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("species").forGetter(d -> d.species().id()),
        Codec.STRING.fieldOf("stage").forGetter(d -> d.stage().id()),
        Codec.LONG.fieldOf("death_time").forGetter(CarcassData::deathTime),
        Codec.FLOAT.optionalFieldOf("workmanship", 0.5f).forGetter(CarcassData::workmanship),
        Codec.FLOAT.optionalFieldOf("waste", 0f).forGetter(CarcassData::waste)
    ).apply(i, (species, stage, death, work, waste) ->
        new CarcassData(CarcassSpecies.byId(species), ButcheryStage.byId(stage), death, work, waste)));

    public static final StreamCodec<ByteBuf, CarcassData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(32), d -> d.species().id(),
        ByteBufCodecs.stringUtf8(32), d -> d.stage().id(),
        ByteBufCodecs.VAR_LONG, CarcassData::deathTime,
        ByteBufCodecs.FLOAT, CarcassData::workmanship,
        ByteBufCodecs.FLOAT, CarcassData::waste,
        (species, stage, death, work, waste) ->
            new CarcassData(CarcassSpecies.byId(species), ButcheryStage.byId(stage), death, work, waste));

    public CarcassData
    {
        workmanship = clamp(workmanship);
        waste = clamp(waste);
    }

    /** How many in-game hours this carcass has been dead at the given game time. */
    public float hoursDead(long gameTime)
    {
        return Math.max(0f, (gameTime - deathTime) / (float) TICKS_PER_HOUR);
    }

    public Freshness freshness(long gameTime, float ambientC)
    {
        return Freshness.of(hoursDead(gameTime), ambientC);
    }

    /** Advance to the next stage, folding this stage's cut quality into the running workmanship average. */
    public CarcassData advanced(float cutQuality, float wastedThisStage)
    {
        final float blended = workmanship * 0.6f + clamp(cutQuality) * 0.4f;
        return new CarcassData(species, stage.next(), deathTime, blended, clamp(waste + wastedThisStage));
    }

    private static float clamp(float value)
    {
        return value < 0f ? 0f : value > 1f ? 1f : value;
    }
}
