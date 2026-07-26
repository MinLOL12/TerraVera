/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import net.dries007.tfc.client.screen.KnappingScreen;

import com.terravera.common.container.TerraVeraContainers;

public final class TerraVeraClient
{
    public static void init(IEventBus bus)
    {
        bus.addListener(TerraVeraClient::registerScreens);
    }

    private static void registerScreens(RegisterMenuScreensEvent event)
    {
        // ShapingContainer extends TFC's KnappingContainer, so TFC's own tested screen can handle it directly.
        event.register(TerraVeraContainers.SHAPING.get(), KnappingScreen::new);
    }

    private TerraVeraClient() {}
}
