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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.terravera.TerraVera;
import com.terravera.common.blocks.TerraVeraBlocks;
import com.terravera.common.component.Cordage;
import com.terravera.common.TerraVeraDataComponents;

public final class TerraVeraItems
{
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerraVera.MOD_ID);

    // ----- Smithing stations ---------------------------------------------------------------------------------

    /** Portable maintenance surface for hot-work repairs. Use with a metal hammer; flux is reserved for welds. */
    public static final DeferredHolder<Item, BlockItem> WORKPLATE = ITEMS.register("workplate",
        () -> new BlockItem(TerraVeraBlocks.WORKPLATE.get(), new Item.Properties()));

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

    // ----- Health, hygiene, and water treatment --------------------------------------------------------------
    // The disease system's progression is expressed almost entirely in these items: what you can make determines
    // what you can defend yourself against. See com.terravera.common.health.

    /**
     * Bitter, tannin-rich leaves, gathered from the same plants that yield fibre. The day-one remedy: chew it and
     * feel slightly less awful. Never cures anything, and it is not supposed to.
     */
    public static final DeferredHolder<Item, Item> BITTER_HERBS = ITEMS.registerSimpleItem("bitter_herbs");
    /** Salicin-bearing bark, stripped from willow with a knife. Real aspirin - it manages fever and aching. */
    public static final DeferredHolder<Item, Item> WILLOW_BARK = ITEMS.registerSimpleItem("willow_bark");
    /** Anthelmintic herbs - wormwood and its relatives. The only stone-age answer to a tapeworm. */
    public static final DeferredHolder<Item, Item> WORMWOOD = ITEMS.registerSimpleItem("wormwood");
    /** Clean cloth boiled and kept dry, for dressing a wound before it turns. */
    public static final DeferredHolder<Item, Item> CLEAN_DRESSING = ITEMS.registerSimpleItem("clean_dressing");
    /** Caustic potash lye leached from wood ash and concentrated in a pot; the real alkali behind ash washing. */
    public static final DeferredHolder<Item, Item> LYE_CONCENTRATE = ITEMS.registerSimpleItem("lye_concentrate");
    /** Fresh soap curd straight out of the kettle. It still needs salting out / curing before it is a usable bar. */
    public static final DeferredHolder<Item, Item> SOAP_CURD = ITEMS.registerSimpleItem("soap_curd");
    /** Tallow or olive oil and lye, boiled and cured. The single largest hygiene improvement available. */
    public static final DeferredHolder<Item, Item> SOAP = ITEMS.registerSimpleItem("soap");
    /** Finely washed charcoal, used as a real adsorbent for gut poisons and as a filter medium. */
    public static final DeferredHolder<Item, Item> ACTIVATED_CHARCOAL = ITEMS.registerSimpleItem("activated_charcoal");
    /** Willow bark decoction reduced into a salicylate-rich extract: fever and pain medicine before aspirin. */
    public static final DeferredHolder<Item, Item> SALICYLATE_EXTRACT = ITEMS.registerSimpleItem("salicylate_extract");
    /** Wormwood steeped in alcohol: a prepared anthelmintic rather than a raw handful of leaves. */
    public static final DeferredHolder<Item, Item> WORMWOOD_TINCTURE = ITEMS.registerSimpleItem("wormwood_tincture");
    /** Alcohol, salt, and honey reduced into a wound wash; not sterile magic, but a real antiseptic preparation. */
    public static final DeferredHolder<Item, Item> ANTISEPTIC_TINCTURE = ITEMS.registerSimpleItem("antiseptic_tincture");
    /**
     * Sand, charcoal, and cloth in a frame. Removes the protozoa and most of the load from a container of water, but
     * deliberately not the bacteria - that still needs boiling.
     */
    public static final DeferredHolder<Item, Item> WATER_FILTER = ITEMS.register("water_filter",
        () -> new WaterFilterItem(new Item.Properties().durability(64)));
    /** Boiled water, salt, and a sweetener. Oral rehydration therapy; the thing that stops cholera killing you. */
    public static final DeferredHolder<Item, Item> REHYDRATION_SALTS = ITEMS.registerSimpleItem("rehydration_salts");
    /** A full apothecary kit made from prepared extracts. The end of the line: cures essentially anything. */
    public static final DeferredHolder<Item, Item> MEDICINE = ITEMS.registerSimpleItem("medicine");

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
