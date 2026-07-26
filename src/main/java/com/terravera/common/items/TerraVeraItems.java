/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.items;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.terravera.TerraVera;
import com.terravera.common.component.Cordage;
import com.terravera.common.TerraVeraDataComponents;

public final class TerraVeraItems
{
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerraVera.MOD_ID);

    // ----- Cordage chain -------------------------------------------------------------------------------------
    // Raw fibre -> retted fibre -> cordage -> lashing on a tool.

    /** Torn straight off a plant. Useless until retted; it will rot through in days if you lash with it. */
    public static final DeferredHolder<Item, Item> PLANT_FIBER = ITEMS.registerSimpleItem("plant_fiber");
    /** Fibre after soaking. The pectin has broken down and the bast separates cleanly. */
    public static final DeferredHolder<Item, Item> RETTED_FIBER = ITEMS.registerSimpleItem("retted_fiber");
    /** Fibre stripped from bark, the strongest source available before agriculture. */
    public static final DeferredHolder<Item, Item> BAST_FIBER = ITEMS.registerSimpleItem("bast_fiber");
    /** A twisted two-ply cord. The gate on every stone tool in the mod. */
    public static final DeferredHolder<Item, Item> CORDAGE = ITEMS.register("cordage",
        () -> new CordageItem(new Item.Properties()
            .component(TerraVeraDataComponents.CORDAGE.get(), Cordage.DEFAULT)));
    /** Several cords laid together. Used for the heaviest hafts, and for rope. */
    public static final DeferredHolder<Item, Item> HEAVY_CORDAGE = ITEMS.register("heavy_cordage",
        () -> new CordageItem(new Item.Properties()
            .component(TerraVeraDataComponents.CORDAGE.get(), new Cordage(0.85f, "mixed"))));

    private TerraVeraItems() {}
}
