/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.temperature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * How wet one garment is, stored on the item itself.
 * <p>
 * Putting this on the stack rather than the player is what makes drying your clothes a real activity: a soaked coat
 * is still soaked tomorrow, still soaked if you take it off, and can be hung by a fire to dry while you wear a spare.
 * It also means "carry a dry change of clothes" is a genuine strategy rather than a slogan.
 *
 * @param wetness    how saturated the garment is, in {@code [0, 1]}
 * @param lastTick   the game tick this value was last updated, so drying can be evaluated lazily
 */
public record Wetness(float wetness, long lastTick)
{
    public static final Wetness DRY = new Wetness(0f, 0L);

    public static final Codec<Wetness> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.FLOAT.optionalFieldOf("wetness", 0f).forGetter(Wetness::wetness),
        Codec.LONG.optionalFieldOf("last_tick", 0L).forGetter(Wetness::lastTick)
    ).apply(i, Wetness::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Wetness> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, Wetness::wetness,
        ByteBufCodecs.VAR_LONG, Wetness::lastTick,
        Wetness::new
    );

    public Wetness
    {
        wetness = Mth.clamp(wetness, 0f, 1f);
    }

    public Wetness wetter(float amount, long tick)
    {
        return new Wetness(wetness + amount, tick);
    }

    public Wetness drier(float amount, long tick)
    {
        return new Wetness(wetness - amount, tick);
    }

    public boolean isDry()
    {
        return wetness <= 0.02f;
    }

    public String descriptorKey()
    {
        if (wetness >= 0.75f) return "terravera.clothing.wetness.soaked";
        if (wetness >= 0.45f) return "terravera.clothing.wetness.wet";
        return "terravera.clothing.wetness.damp";
    }
}
