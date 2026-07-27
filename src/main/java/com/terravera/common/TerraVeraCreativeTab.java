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
import com.terravera.common.component.KnappedHead;
import com.terravera.common.items.TerraVeraItems;

public final class TerraVeraCreativeTab
{
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TerraVera.MOD_ID);

    /** The rock categories heads can be made of, matching TerraVera's shipped {@code knappable_stone} data. */
    private static final String[] MATERIALS = {
        "igneous_intrusive", "igneous_extrusive", "sedimentary", "metamorphic", "obsidian"
    };

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_TABS.register("terravera",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("terravera.creative_tab.terravera"))
            .icon(() -> new ItemStack(TerraVeraItems.CORDAGE.get()))
            .displayItems((params, output) -> {})
            .build());

    public static void onBuildTabContents(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() != TAB.getKey()) return;

        event.accept(TerraVeraItems.AIR_CONDITIONER.get());
        event.accept(TerraVeraItems.GENERATOR.get());
        event.accept(TerraVeraItems.HAND_CRANK.get());
        event.accept(TerraVeraItems.WIND_TURBINE.get());
        event.accept(TerraVeraItems.COPPER_WIRE.get());
        event.accept(TerraVeraItems.PROGRAMMED_CIRCUIT.get());
        event.accept(TerraVeraItems.REFRIGERANT_CANISTER.get());
        event.accept(TerraVeraItems.AIR_FILTER.get());
        event.accept(TerraVeraItems.WORKPLATE.get());
        event.accept(TerraVeraItems.RUBBLE_FOUNDATION.get());
        event.accept(TerraVeraItems.WOODEN_SUPPORT_BEAM.get());
        event.accept(TerraVeraItems.WROUGHT_IRON_SUPPORT_BEAM.get());
        event.accept(TerraVeraItems.FIELD_NOTES.get());
        event.accept(TerraVeraItems.LEATHER_TOOL_GRIP.get());
        event.accept(TerraVeraItems.RAW_LATEX.get());
        event.accept(TerraVeraItems.RUBBER_TOOL_GRIP.get());

        event.accept(TerraVeraItems.PLANT_FIBER.get());
        event.accept(TerraVeraItems.RETTED_FIBER.get());
        event.accept(TerraVeraItems.BAST_FIBER.get());
        event.accept(TerraVeraItems.PRIMITIVE_CORDAGE.get());
        event.accept(TerraVeraItems.CORDAGE.get());
        event.accept(TerraVeraItems.HEAVY_CORDAGE.get());

        // Health, hygiene, and water treatment, in rough progression order.
        event.accept(TerraVeraItems.BITTER_HERBS.get());
        event.accept(TerraVeraItems.WILLOW_BARK.get());
        event.accept(TerraVeraItems.WORMWOOD.get());
        event.accept(TerraVeraItems.WATER_FILTER.get());
        event.accept(TerraVeraItems.CLEAN_DRESSING.get());
        event.accept(TerraVeraItems.LYE_CONCENTRATE.get());
        event.accept(TerraVeraItems.SOAP_CURD.get());
        event.accept(TerraVeraItems.SOAP.get());
        event.accept(TerraVeraItems.ACTIVATED_CHARCOAL.get());
        event.accept(TerraVeraItems.SALICYLATE_EXTRACT.get());
        event.accept(TerraVeraItems.WORMWOOD_TINCTURE.get());
        event.accept(TerraVeraItems.ANTISEPTIC_TINCTURE.get());
        event.accept(TerraVeraItems.REHYDRATION_SALTS.get());
        event.accept(TerraVeraItems.MEDICINE.get());

        // Textile chain and the wardrobe, in roughly the order a player unlocks them.
        event.accept(TerraVeraItems.PLANT_FIBER_CLOTH.get());
        event.accept(TerraVeraItems.STRAW_MAT.get());
        event.accept(TerraVeraItems.LINEN_CLOTH.get());
        event.accept(TerraVeraItems.FELT_CLOTH.get());
        event.accept(TerraVeraItems.FUR_PELT.get());
        event.accept(TerraVeraItems.DUBBIN.get());
        event.accept(TerraVeraItems.OILSKIN_CLOTH.get());
        event.accept(TerraVeraItems.BATTING.get());
        event.accept(TerraVeraItems.QUILTED_CLOTH.get());
        event.accept(TerraVeraItems.SEWN_HOOD_PANEL.get());
        event.accept(TerraVeraItems.SEWN_BODY_PANEL.get());
        event.accept(TerraVeraItems.SEWN_LEG_PANEL.get());
        event.accept(TerraVeraItems.SEWN_FOOT_PANEL.get());
        event.accept(TerraVeraItems.DRYING_RACK.get());
        com.terravera.common.temperature.TerraVeraClothing.garments()
            .values().forEach(holder -> event.accept(holder.get()));

        // One entry per (head kind x stone), with the component filled in, so that creative and JEI show real heads
        // rather than blank ones that no lashing recipe will accept.
        TerraVeraItems.HEADS.forEach((kind, holder) -> {
            for (String material : MATERIALS)
            {
                final ItemStack stack = new ItemStack(holder.get());
                stack.set(TerraVeraDataComponents.KNAPPED_HEAD.get(),
                    new KnappedHead(TerraVera.identifier(kind), material, 0.75f));
                event.accept(stack);
            }
        });
    }

    private TerraVeraCreativeTab() {}
}
