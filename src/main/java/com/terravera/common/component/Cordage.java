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

/**
 * Data attached to a length of cordage. Different plants give different fibre - nettle and dogbane give long, strong
 * bast; grass gives short, weak fibre that will do in a pinch but will not hold an axe head on for long.
 * <p>
 * Cordage also has a length (in millimeters) that affects how well it secures a tool head. Longer cordage wraps
 * around more of the haft, creating a tighter and more secure binding. This affects the tool's overall durability,
 * speed, and damage output.
 *
 * @param strength in [0, 1]. How much of a tool's durability the lashing supports.
 * @param source   the fibre source, used for the tooltip and for recipes that want a specific fibre.
 * @param lengthMM the length of the cordage in millimeters. Longer cordage provides a stronger binding.
 *                  Normal cordage is typically 300-400mm, heavy cordage is 500-600mm.
 */
public record Cordage(float strength, String source, int lengthMM)
{
    public static final Cordage DEFAULT = new Cordage(0.5f, "mixed", 300);

    public static final Codec<Cordage> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.FLOAT.optionalFieldOf("strength", 0.5f).forGetter(Cordage::strength),
        Codec.STRING.optionalFieldOf("source", "mixed").forGetter(Cordage::source),
        Codec.INT.optionalFieldOf("length_mm", 300).forGetter(Cordage::lengthMM)
    ).apply(i, Cordage::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Cordage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, Cordage::strength,
        ByteBufCodecs.stringUtf8(64), Cordage::source,
        ByteBufCodecs.VAR_INT, Cordage::lengthMM,
        Cordage::new
    );

    /**
     * Calculate the binding quality based on length. Longer cordage creates a more secure binding.
     * Length is measured in mm. The formula provides diminishing returns for very long cordage.
     * 
     * @return a multiplier between 0.5 and 1.5 based on cordage length
     */
    public float bindingQuality()
    {
        // 300mm is baseline (1.0), shorter is worse, longer is better
        // Each 100mm above/below 300mm adds/subtracts ~0.1
        // Cap at 0.5 (100mm) and 1.5 (800mm+)
        float quality = 0.5f + (lengthMM - 100) / 500f;
        return Math.max(0.5f, Math.min(1.5f, quality));
    }
}
