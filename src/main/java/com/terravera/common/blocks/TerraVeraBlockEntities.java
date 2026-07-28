/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.blocks;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.terravera.TerraVera;
import com.terravera.common.blockentity.AirConditionerBlockEntity;
import com.terravera.common.blockentity.HandCrankBlockEntity;
import com.terravera.common.blockentity.WindTurbineBlockEntity;
import com.terravera.common.water.WaterCollectorBlockEntity;

/**
 * Block entity types for TerraVera's animated machines. The block entities exist purely to host GeckoLib animation
 * state (rotor, fan, crank handle) and, for the turbine, the periodic sky check that drives the spinning state.
 */
public final class TerraVeraBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> TYPES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TerraVera.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WindTurbineBlockEntity>> WIND_TURBINE =
        TYPES.register("wind_turbine", () -> BlockEntityType.Builder
            .of(WindTurbineBlockEntity::new, TerraVeraBlocks.WIND_TURBINE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AirConditionerBlockEntity>> AIR_CONDITIONER =
        TYPES.register("air_conditioner", () -> BlockEntityType.Builder
            .of(AirConditionerBlockEntity::new, TerraVeraBlocks.AIR_CONDITIONER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HandCrankBlockEntity>> HAND_CRANK =
        TYPES.register("hand_crank", () -> BlockEntityType.Builder
            .of(HandCrankBlockEntity::new, TerraVeraBlocks.HAND_CRANK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WaterCollectorBlockEntity>> WATER_COLLECTOR =
        TYPES.register("water_collector", () -> BlockEntityType.Builder
            .of(WaterCollectorBlockEntity::new,
                TerraVeraBlocks.RAIN_CATCHER.get(), TerraVeraBlocks.DEW_COLLECTOR.get(),
                TerraVeraBlocks.ROCK_BASIN.get(), TerraVeraBlocks.SOLAR_STILL.get()).build(null));

    public static void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event)
    {
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
            WATER_COLLECTOR.get(), (collector, side) -> collector.fluidHandler());
    }

    private TerraVeraBlockEntities() {}
}
