/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.container;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.terravera.TerraVera;

public final class TerraVeraContainers
{
    public static final DeferredRegister<MenuType<?>> CONTAINERS =
        DeferredRegister.create(Registries.MENU, TerraVera.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ShapingContainer>> SHAPING =
        CONTAINERS.register("shaping",
            () -> IMenuTypeExtension.create(ShapingContainer::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<WorkplateContainer>> WORKPLATE =
        CONTAINERS.register("workplate",
            () -> IMenuTypeExtension.create(WorkplateContainer::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<ClimateControllerMenu>> CLIMATE_CONTROLLER =
        CONTAINERS.register("climate_controller", () -> IMenuTypeExtension.create(ClimateControllerMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<GreenhouseMenu>> GREENHOUSE =
        CONTAINERS.register("greenhouse", () -> IMenuTypeExtension.create(GreenhouseMenu::fromNetwork));

    private TerraVeraContainers() {}
}
