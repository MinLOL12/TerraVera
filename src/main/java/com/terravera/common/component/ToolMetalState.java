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
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Long-term metal condition attached to maintained tools.
 * <p>
 * Vanilla durability answers "how close is this tool to breaking right now?" This component answers the slower
 * blacksmithing questions: how much metal is left after repeated sharpening, and what shape has the smith pushed it
 * toward while correcting bends, edges, thickness, and strain?
 */
public record ToolMetalState(
    float mass,
    float originalMass,
    int length,
    int width,
    int thickness,
    int bend,
    int edge,
    int strain,
    String operation,
    int operations
)
{
    public static final Codec<ToolMetalState> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.FLOAT.optionalFieldOf("mass", 100f).forGetter(ToolMetalState::mass),
        Codec.FLOAT.optionalFieldOf("original_mass", 100f).forGetter(ToolMetalState::originalMass),
        Codec.INT.optionalFieldOf("length", 10).forGetter(ToolMetalState::length),
        Codec.INT.optionalFieldOf("width", 10).forGetter(ToolMetalState::width),
        Codec.INT.optionalFieldOf("thickness", 10).forGetter(ToolMetalState::thickness),
        Codec.INT.optionalFieldOf("bend", 0).forGetter(ToolMetalState::bend),
        Codec.INT.optionalFieldOf("edge", 5).forGetter(ToolMetalState::edge),
        Codec.INT.optionalFieldOf("strain", 0).forGetter(ToolMetalState::strain),
        Codec.STRING.optionalFieldOf("operation", "controlled_strike").forGetter(ToolMetalState::operation),
        Codec.INT.optionalFieldOf("operations", 0).forGetter(ToolMetalState::operations)
    ).apply(i, ToolMetalState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToolMetalState> STREAM_CODEC = StreamCodec.of(
        (buf, state) -> {
            buf.writeFloat(state.mass);
            buf.writeFloat(state.originalMass);
            buf.writeVarInt(state.length);
            buf.writeVarInt(state.width);
            buf.writeVarInt(state.thickness);
            buf.writeVarInt(state.bend);
            buf.writeVarInt(state.edge);
            buf.writeVarInt(state.strain);
            buf.writeUtf(state.operation);
            buf.writeVarInt(state.operations);
        },
        buf -> new ToolMetalState(
            buf.readFloat(),
            buf.readFloat(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readUtf(),
            buf.readVarInt()
        )
    );

    public static ToolMetalState initial(ItemStack stack)
    {
        final float mass = estimateStartingMass(stack);
        return new ToolMetalState(mass, mass, 10, 10, 10, 0, 5, 0, "controlled_strike", 0);
    }

    public ToolMetalState withOperation(String next)
    {
        return new ToolMetalState(mass, originalMass, length, width, thickness, bend, edge, strain, next, operations);
    }

    public ToolMetalState worked(String nextOperation, float massLoss, int dLength, int dWidth, int dThickness,
        int dBend, int dEdge, int dStrain)
    {
        return new ToolMetalState(
            Mth.clamp(mass - massLoss, 1f, originalMass),
            originalMass,
            Mth.clamp(length + dLength, 1, 20),
            Mth.clamp(width + dWidth, 1, 20),
            Mth.clamp(thickness + dThickness, 1, 20),
            Mth.clamp(bend + dBend, -10, 10),
            Mth.clamp(edge + dEdge, 0, 10),
            Mth.clamp(strain + dStrain, 0, 10),
            nextOperation,
            operations + 1
        );
    }

    public ToolMetalState welded(float addedMass)
    {
        final float cap = originalMass;
        return new ToolMetalState(
            Mth.clamp(mass + addedMass, 1f, cap),
            originalMass,
            length,
            width,
            thickness,
            bend,
            edge,
            Math.max(0, strain - 2),
            operation,
            operations + 1
        );
    }

    public float remainingMassFraction()
    {
        return originalMass <= 0f ? 1f : Mth.clamp(mass / originalMass, 0.05f, 1f);
    }

    private static float estimateStartingMass(ItemStack stack)
    {
        final int maxDamage = Math.max(1, stack.getMaxDamage());
        return Mth.clamp(maxDamage / 12f, 40f, 160f);
    }
}
