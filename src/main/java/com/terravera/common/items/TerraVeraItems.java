/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.items;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
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
    /** A crude, hand-twisted grass cord made directly from gathered plant fibre. */
    public static final DeferredHolder<Item, Item> PRIMITIVE_CORDAGE = ITEMS.register("primitive_cordage",
        () -> new CordageItem(new Item.Properties()
            .component(TerraVeraDataComponents.CORDAGE.get(), new Cordage(0.25f, "mixed", 250))));
    /** A twisted two-ply cord. The gate on every stone tool in the mod. 
     * Default length: 350mm (35cm) - standard twisted cordage */
    public static final DeferredHolder<Item, Item> CORDAGE = ITEMS.register("cordage",
        () -> new CordageItem(new Item.Properties()
            .component(TerraVeraDataComponents.CORDAGE.get(), new Cordage(0.5f, "mixed", 350))));
    /** Several cords laid together. Used for the heaviest hafts, and for rope.
     * Default length: 550mm (55cm) - longer and stronger heavy cordage */
    public static final DeferredHolder<Item, Item> HEAVY_CORDAGE = ITEMS.register("heavy_cordage",
        () -> new CordageItem(new Item.Properties()
            .component(TerraVeraDataComponents.CORDAGE.get(), new Cordage(0.85f, "mixed", 550))));

    // ----- Knapped heads -------------------------------------------------------------------------------------
    // One item per working end, not one per (rock category x tool). The stone is in the component.

    public static final Map<String, DeferredHolder<Item, Item>> HEADS = registerHeads(
        "wedge",  // splitting edge, backed by mass - axes, adzes
        "point",  // narrow converging point - javelins, awls, picks
        "blade",  // long straight edge - knives, scrapers
        "broad",  // wide flat edge - shovels, hoes
        "maul"    // blunt mass, no working edge at all - hammers
    );

    private static Map<String, DeferredHolder<Item, Item>> registerHeads(String... kinds)
    {
        final Map<String, DeferredHolder<Item, Item>> map = new LinkedHashMap<>();
        for (String kind : kinds)
        {
            map.put(kind, ITEMS.register("head/" + kind, () -> new HeadItem(kind, new Item.Properties().stacksTo(16))));
        }
        return Map.copyOf(map);
    }

    public static Supplier<Item> head(String kind)
    {
        final DeferredHolder<Item, Item> holder = HEADS.get(kind);
        if (holder == null) throw new IllegalArgumentException("Unknown head kind: " + kind);
        return holder;
    }

    private TerraVeraItems() {}
}
