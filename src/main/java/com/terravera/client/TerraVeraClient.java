/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-side initialization for TerraVera.
 * <p>
 * Registers a custom knapping screen that uses TFC's native buttons and textures
 * but displays TerraVera's function-based knapping feedback.
 */
public final class TerraVeraClient
{
    public static void init(IEventBus bus)
    {
        bus.addListener(TerraVeraClient::registerScreens);
    }

    private static void registerScreens(RegisterMenuScreensEvent event)
    {
        // Register our custom screen for TerraVera's shaping container (our own menu type)
        // This screen uses TFC's native button textures and click sounds
        // but provides TerraVera's custom feedback
        event.register(com.terravera.common.container.TerraVeraContainers.SHAPING.get(), ShapingScreen::new);
    }

    private TerraVeraClient() {}
}
