/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.terravera.TerraVera;
import com.terravera.common.items.TerraVeraItems;

public final class TerraVeraCreativeTab
{
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TerraVera.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_TABS.register("terravera",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("terravera.creative_tab.terravera"))
            .icon(() -> new ItemStack(TerraVeraItems.CORDAGE.get()))
            .displayItems((params, output) -> {})
            .build());

    public static void onBuildTabContents(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() != TAB.getKey()) return;

        event.accept(TerraVeraItems.PLANT_FIBER.get());
        event.accept(TerraVeraItems.RETTED_FIBER.get());
        event.accept(TerraVeraItems.BAST_FIBER.get());
        event.accept(TerraVeraItems.CORDAGE.get());
        event.accept(TerraVeraItems.HEAVY_CORDAGE.get());
    }

    private TerraVeraCreativeTab() {}
}
