/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.container;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.dries007.tfc.common.container.ItemStackContainerProvider;

import com.terravera.TerraVera;
import com.terravera.common.knapping.KnappableStone;

public final class TerraVeraContainers
{
    public static final DeferredRegister<MenuType<?>> CONTAINERS =
        DeferredRegister.create(Registries.MENU, TerraVera.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ShapingContainer>> SHAPING =
        CONTAINERS.register("shaping", () -> new MenuType<>(
            (IContainerFactory<ShapingContainer>) (windowId, inventory, buffer) -> {
                final KnappableStone stone = KnappableStone.MANAGER.getOrThrow(buffer.readResourceLocation());
                final ItemStackContainerProvider.Info info = ItemStackContainerProvider.read(buffer, inventory);
                return ShapingContainer.create(info.stack(), stone, info.hand(), info.slot(), inventory, windowId);
            }, FeatureFlags.DEFAULT_FLAGS));

    private TerraVeraContainers() {}
}
