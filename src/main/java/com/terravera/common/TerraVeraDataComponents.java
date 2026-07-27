/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common;

import java.util.function.UnaryOperator;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.terravera.TerraVera;
import com.terravera.common.component.Cordage;
import com.terravera.common.component.KnappedHead;
import com.terravera.common.component.ToolMetalState;

public final class TerraVeraDataComponents
{
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, TerraVera.MOD_ID);

    /** Attached to knapped tool heads. Describes what kind of working end was produced, and how well. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<KnappedHead>> KNAPPED_HEAD =
        register("knapped_head", builder -> builder.persistent(KnappedHead.CODEC).networkSynchronized(KnappedHead.STREAM_CODEC));

    /** Attached to cordage, and to tools hafted with it. Determines how well the lashing holds and how long it is. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Cordage>> CORDAGE =
        register("cordage", builder -> builder.persistent(Cordage.CODEC).networkSynchronized(Cordage.STREAM_CODEC));

    /** Attached to repaired metal tools. Tracks remaining metal mass and the current smithing operation. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ToolMetalState>> TOOL_METAL_STATE =
        register("tool_metal_state", builder -> builder.persistent(ToolMetalState.CODEC).networkSynchronized(ToolMetalState.STREAM_CODEC));

    /** Attached to hafted tools. Stores the speed modifier from cordage binding quality. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BindingBonus>> BINDING_SPEED_BONUS =
        register("binding_speed_bonus", builder -> builder.persistent(BindingBonus.CODEC).networkSynchronized(BindingBonus.STREAM_CODEC));

    /** Attached to hafted tools. Stores the damage modifier from cordage binding quality. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DamageBonus>> BINDING_DAMAGE_BONUS =
        register("binding_damage_bonus", builder -> builder.persistent(DamageBonus.CODEC).networkSynchronized(DamageBonus.STREAM_CODEC));

    /**
     * Attached to anything that holds drinking water. Records where the water came from and what has since been done
     * to it, so that a jug of boiled spring water and a jug of swamp water are not the same item.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.terravera.common.health.WaterTreatment>> WATER_TREATMENT =
        register("water_treatment", builder -> builder
            .persistent(com.terravera.common.health.WaterTreatment.CODEC)
            .networkSynchronized(com.terravera.common.health.WaterTreatment.STREAM_CODEC));

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(
        String name, UnaryOperator<DataComponentType.Builder<T>> builder)
    {
        return COMPONENTS.register(name, () -> builder.apply(DataComponentType.builder()).build());
    }

    private TerraVeraDataComponents() {}
    
    /**
     * Data component for speed bonus from binding quality.
     */
    public record BindingBonus(float speedModifier, float bindingQuality)
    {
        public static final Codec<BindingBonus> CODEC = Codec.FLOAT.xmap(
            f -> new BindingBonus(f, f),
            b -> b.speedModifier()
        );
        
        public static final StreamCodec<RegistryFriendlyByteBuf, BindingBonus> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, BindingBonus::speedModifier,
            ByteBufCodecs.FLOAT, BindingBonus::bindingQuality,
            BindingBonus::new
        );
    }
    
    /**
     * Data component for damage bonus from binding quality.
     */
    public record DamageBonus(float damageModifier, float bindingQuality)
    {
        public static final Codec<DamageBonus> CODEC = Codec.FLOAT.xmap(
            f -> new DamageBonus(f, f),
            b -> b.damageModifier()
        );
        
        public static final StreamCodec<RegistryFriendlyByteBuf, DamageBonus> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, DamageBonus::damageModifier,
            ByteBufCodecs.FLOAT, DamageBonus::bindingQuality,
            DamageBonus::new
        );
    }
}
