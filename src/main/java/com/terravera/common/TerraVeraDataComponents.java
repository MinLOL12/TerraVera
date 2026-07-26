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
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.terravera.TerraVera;
import com.terravera.common.component.Cordage;

public final class TerraVeraDataComponents
{
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, TerraVera.MOD_ID);

    /** Attached to cordage, and to tools hafted with it. Determines how well the lashing holds. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Cordage>> CORDAGE =
        register("cordage", builder -> builder.persistent(Cordage.CODEC).networkSynchronized(Cordage.STREAM_CODEC));

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(
        String name, UnaryOperator<DataComponentType.Builder<T>> builder)
    {
        return COMPONENTS.register(name, () -> builder.apply(DataComponentType.builder()).build());
    }

    private TerraVeraDataComponents() {}
}
