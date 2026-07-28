/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

import com.terravera.client.model.AirConditionerModel;
import com.terravera.client.model.HandCrankModel;
import com.terravera.client.model.WindTurbineModel;
import com.terravera.client.model.WaterCollectorModel;
import com.terravera.common.blocks.TerraVeraBlockEntities;

/**
 * Client-side initialization for TerraVera.
 * <p>
 * Registers a custom knapping screen that uses TFC's native buttons and textures
 * but displays TerraVera's function-based knapping feedback, and the GeckoLib block renderers that animate the
 * wind turbine rotor, the air-conditioner fan, the hand-crank handle, and layered water collectors.
 */
public final class TerraVeraClient
{
    public static void init(IEventBus bus)
    {
        bus.addListener(TerraVeraClient::registerScreens);
        bus.addListener(TerraVeraClient::registerBlockRenderers);
    }

    /** GeckoLib renderers for TerraVera's animated machines. */
    private static void registerBlockRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerBlockEntityRenderer(TerraVeraBlockEntities.WIND_TURBINE.get(),
            context -> new GeoBlockRenderer<>(new WindTurbineModel<>()));
        event.registerBlockEntityRenderer(TerraVeraBlockEntities.AIR_CONDITIONER.get(),
            context -> new GeoBlockRenderer<>(new AirConditionerModel<>()));
        event.registerBlockEntityRenderer(TerraVeraBlockEntities.HAND_CRANK.get(),
            context -> new GeoBlockRenderer<>(new HandCrankModel<>()));
        event.registerBlockEntityRenderer(TerraVeraBlockEntities.WATER_COLLECTOR.get(),
            context -> new GeoBlockRenderer<>(new WaterCollectorModel<>()));
        // Greenhouse tiers: each has its own model variant with distinct vent animations.
        event.registerBlockEntityRenderer(TerraVeraBlockEntities.GREENHOUSE.get(),
            context -> new GeoBlockRenderer<>(new com.terravera.client.model.GreenhouseModel<>()));
        event.registerBlockEntityRenderer(TerraVeraBlockEntities.IRRIGATION_TANK.get(),
            context -> new GeoBlockRenderer<>(new com.terravera.client.model.IrrigationTankModel<>()));
    }

    private static void registerScreens(RegisterMenuScreensEvent event)
    {
        // Register our custom screen for TerraVera's shaping container (our own menu type)
        // This screen uses TFC's native button textures and click sounds
        // but provides TerraVera's custom feedback
        event.register(com.terravera.common.container.TerraVeraContainers.SHAPING.get(), ShapingScreen::new);

        // Workplate repair GUI: a clean TFC-style screen that walks the player through every step of
        // metal tool maintenance — placing the hammer and tool, selecting an operation, and striking.
        event.register(com.terravera.common.container.TerraVeraContainers.WORKPLATE.get(), WorkplateScreen::new);
        event.register(com.terravera.common.container.TerraVeraContainers.CLIMATE_CONTROLLER.get(), ClimateControllerScreen::new);
    }

    private TerraVeraClient() {}
}
