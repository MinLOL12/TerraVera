/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * A replaceable outer wrap fitted over an already-hafted tool's wooden handle.
 * Leather improves control in wet or cold work; rubber damps impact and vibration a little better. The component is
 * on the tool, not the player, so a carefully fitted handle remains useful when traded.
 */
public record ToolGrip(String material, float speedMultiplier, float efficiencyMultiplier)
{
    public static final Codec<ToolGrip> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.optionalFieldOf("material", "leather").forGetter(ToolGrip::material),
        Codec.FLOAT.optionalFieldOf("speed_multiplier", 1.03f).forGetter(ToolGrip::speedMultiplier),
        Codec.FLOAT.optionalFieldOf("efficiency_multiplier", 1.04f).forGetter(ToolGrip::efficiencyMultiplier)
    ).apply(i, ToolGrip::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToolGrip> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(24), ToolGrip::material,
        ByteBufCodecs.FLOAT, ToolGrip::speedMultiplier,
        ByteBufCodecs.FLOAT, ToolGrip::efficiencyMultiplier,
        ToolGrip::new
    );

    public ToolGrip
    {
        material = material == null || material.isBlank() ? "leather" : material;
        speedMultiplier = Mth.clamp(speedMultiplier, 1f, 1.20f);
        efficiencyMultiplier = Mth.clamp(efficiencyMultiplier, 1f, 1.25f);
    }

    public static ToolGrip leather()
    {
        return new ToolGrip("leather", 1.035f, 1.05f);
    }

    public static ToolGrip rubber()
    {
        return new ToolGrip("rubber", 1.075f, 1.12f);
    }
}
