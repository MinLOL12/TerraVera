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
import com.terravera.common.component.BarkProperties;
import com.terravera.common.component.Cordage;
import com.terravera.common.TerraVeraDataComponents;

public final class TerraVeraItems
{
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerraVera.MOD_ID);

    // ----- Smithing stations ---------------------------------------------------------------------------------

    /** Portable maintenance surface for hot-work repairs. Use with a metal hammer; flux is reserved for welds. */
    public static final DeferredHolder<Item, BlockItem> WORKPLATE = ITEMS.register("workplate",
        () -> new BlockItem(TerraVeraBlocks.WORKPLATE.get(), new Item.Properties()));

    // ----- Industrial climate control ------------------------------------------------------------------------
    // The animated machines use GeoMachineItem so the held item renders the full GeckoLib model, not a flat sprite.
    public static final DeferredHolder<Item, BlockItem> AIR_CONDITIONER = ITEMS.register("air_conditioner",
        () -> new GeoMachineItem(TerraVeraBlocks.AIR_CONDITIONER.get(), new Item.Properties(),
            com.terravera.client.model.AirConditionerModel::new));
    public static final DeferredHolder<Item, BlockItem> GENERATOR = ITEMS.register("generator",
        () -> new BlockItem(TerraVeraBlocks.GENERATOR.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> HAND_CRANK = ITEMS.register("hand_crank",
        () -> new GeoMachineItem(TerraVeraBlocks.HAND_CRANK.get(), new Item.Properties(),
            com.terravera.client.model.HandCrankModel::new));
    public static final DeferredHolder<Item, BlockItem> WIND_TURBINE = ITEMS.register("wind_turbine",
        () -> new GeoMachineItem(TerraVeraBlocks.WIND_TURBINE.get(), new Item.Properties(),
            com.terravera.client.model.WindTurbineModel::new));
    public static final DeferredHolder<Item, BlockItem> SINGLE_WIRE = ITEMS.register("single_wire",
        () -> new BlockItem(TerraVeraBlocks.SINGLE_WIRE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> COPPER_WIRE = ITEMS.register("copper_wire",
        () -> new BlockItem(TerraVeraBlocks.COPPER_WIRE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> WIRE_INTERSECTION = ITEMS.register("wire_intersection",
        () -> new BlockItem(TerraVeraBlocks.WIRE_INTERSECTION.get(), new Item.Properties()));
    /** A wired logic board. It is consumed when installed, preventing an unprogrammed compressor from operating. */
    public static final DeferredHolder<Item, Item> PROGRAMMED_CIRCUIT = ITEMS.registerSimpleItem("programmed_circuit");
    public static final DeferredHolder<Item, Item> REFRIGERANT_CANISTER = ITEMS.registerSimpleItem("refrigerant_canister");
    public static final DeferredHolder<Item, Item> AIR_FILTER = ITEMS.registerSimpleItem("air_filter");

    // ----- Primitive water collection -----------------------------------------------------------------------
    public static final DeferredHolder<Item, BlockItem> RAIN_CATCHER = ITEMS.register("rain_catcher",
        () -> new GeoMachineItem(TerraVeraBlocks.RAIN_CATCHER.get(), new Item.Properties(),
            () -> new com.terravera.client.model.WaterCollectorModel<>(com.terravera.common.water.CollectorType.RAIN_CATCHER)));
    public static final DeferredHolder<Item, BlockItem> DEW_COLLECTOR = ITEMS.register("dew_collector",
        () -> new GeoMachineItem(TerraVeraBlocks.DEW_COLLECTOR.get(), new Item.Properties(),
            () -> new com.terravera.client.model.WaterCollectorModel<>(com.terravera.common.water.CollectorType.DEW_COLLECTOR)));
    public static final DeferredHolder<Item, BlockItem> ROCK_BASIN = ITEMS.register("rock_basin",
        () -> new GeoMachineItem(TerraVeraBlocks.ROCK_BASIN.get(), new Item.Properties(),
            () -> new com.terravera.client.model.WaterCollectorModel<>(com.terravera.common.water.CollectorType.ROCK_BASIN)));
    public static final DeferredHolder<Item, BlockItem> SOLAR_STILL = ITEMS.register("solar_still",
        () -> new GeoMachineItem(TerraVeraBlocks.SOLAR_STILL.get(), new Item.Properties(),
            () -> new com.terravera.client.model.WaterCollectorModel<>(com.terravera.common.water.CollectorType.SOLAR_STILL)));

    // ----- Structural construction ---------------------------------------------------------------------------

    public static final DeferredHolder<Item, BlockItem> RUBBLE_FOUNDATION = ITEMS.register("rubble_foundation",
        () -> new BlockItem(TerraVeraBlocks.RUBBLE_FOUNDATION.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> WOODEN_SUPPORT_BEAM = ITEMS.register("wooden_support_beam",
        () -> new BlockItem(TerraVeraBlocks.WOODEN_SUPPORT_BEAM.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> WROUGHT_IRON_SUPPORT_BEAM = ITEMS.register("wrought_iron_support_beam",
        () -> new BlockItem(TerraVeraBlocks.WROUGHT_IRON_SUPPORT_BEAM.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> BARK_ROOFING = ITEMS.register("bark_roofing",
        () -> new BlockItem(TerraVeraBlocks.BARK_ROOFING.get(), new Item.Properties()));

    // ----- Handles, grips, and field knowledge ---------------------------------------------------------------

    /** Thick strips cut and laced around an existing wooden handle; reliable in wet work. */
    public static final DeferredHolder<Item, Item> LEATHER_TOOL_GRIP = ITEMS.register("leather_tool_grip",
        () -> new GripItem("leather", new Item.Properties()));
    /** Natural latex, collected by tapping a latex-bearing tree before it is compounded and cured. */
    public static final DeferredHolder<Item, Item> RAW_LATEX = ITEMS.registerSimpleItem("raw_latex");
    /** Sulfur-cured rubber wrap; costs more to make, but damps shock and improves tool control. */
    public static final DeferredHolder<Item, Item> RUBBER_TOOL_GRIP = ITEMS.register("rubber_tool_grip",
        () -> new GripItem("rubber", new Item.Properties()));
    /** A pocket field notebook used to review the practical knowledge learned through play. */
    public static final DeferredHolder<Item, Item> FIELD_NOTES = ITEMS.register("field_notes",
        () -> new FieldNotesItem(new Item.Properties().stacksTo(1)));

    // ----- Bark harvesting ----------------------------------------------------------------------------------

    public static final BarkProperties OAK_BARK_PROPERTIES = new BarkProperties("oak", 0.62f, 0.90f, 0.45f, 0.75f, 3.5f);
    public static final BarkProperties HEMLOCK_BARK_PROPERTIES = new BarkProperties("hemlock", 0.68f, 0.95f, 0.35f, 0.72f, 4.0f);
    public static final BarkProperties WILLOW_BARK_PROPERTIES = new BarkProperties("willow", 0.72f, 0.55f, 0.85f, 0.62f, 2.0f);
    public static final BarkProperties BIRCH_BARK_PROPERTIES = new BarkProperties("birch", 0.55f, 0.35f, 0.92f, 0.96f, 1.5f);
    public static final BarkProperties BAST_BARK_PROPERTIES = new BarkProperties("bast", 0.70f, 0.30f, 0.95f, 0.66f, 2.0f);
    public static final BarkProperties MIXED_BARK_PROPERTIES = new BarkProperties("mixed", 0.65f, 0.40f, 0.50f, 0.70f, 3.0f);

    // Dry forms are registered first so fresh BarkItems can safely point at their conversion target.
    public static final DeferredHolder<Item, Item> DRIED_OAK_BARK = driedBark("dried_oak_bark", OAK_BARK_PROPERTIES);
    public static final DeferredHolder<Item, Item> DRIED_HEMLOCK_BARK = driedBark("dried_hemlock_bark", HEMLOCK_BARK_PROPERTIES);
    public static final DeferredHolder<Item, Item> DRIED_WILLOW_BARK = driedBark("dried_willow_bark", WILLOW_BARK_PROPERTIES);
    public static final DeferredHolder<Item, Item> DRIED_BIRCH_BARK = driedBark("dried_birch_bark", BIRCH_BARK_PROPERTIES);
    public static final DeferredHolder<Item, Item> DRIED_BAST_BARK = driedBark("dried_bast_bark", BAST_BARK_PROPERTIES);
    public static final DeferredHolder<Item, Item> DRIED_BARK = driedBark("dried_bark", MIXED_BARK_PROPERTIES);

    public static final DeferredHolder<Item, Item> OAK_BARK = freshBark("oak_bark", OAK_BARK_PROPERTIES, DRIED_OAK_BARK);
    public static final DeferredHolder<Item, Item> HEMLOCK_BARK = freshBark("hemlock_bark", HEMLOCK_BARK_PROPERTIES, DRIED_HEMLOCK_BARK);
    /** Salicin-bearing willow bark remains usable fresh as a basic remedy, but must be dried for fuel and cordage. */
    public static final DeferredHolder<Item, Item> WILLOW_BARK = freshBark("willow_bark", WILLOW_BARK_PROPERTIES, DRIED_WILLOW_BARK);
    public static final DeferredHolder<Item, Item> BIRCH_BARK = freshBark("birch_bark", BIRCH_BARK_PROPERTIES, DRIED_BIRCH_BARK);
    public static final DeferredHolder<Item, Item> BAST_BARK = freshBark("bast_bark", BAST_BARK_PROPERTIES, DRIED_BAST_BARK);
    public static final DeferredHolder<Item, Item> BARK = freshBark("bark", MIXED_BARK_PROPERTIES, DRIED_BARK);

    /** A light folded vessel for dry food and supplies; it is deliberately not a sealed fluid container. */
    public static final DeferredHolder<Item, Item> BIRCH_BARK_CONTAINER = ITEMS.registerSimpleItem("birch_bark_container");

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

    // ----- Clothing ------------------------------------------------------------------------------------------
    // The wardrobe is large enough to deserve its own file. See TerraVeraClothing for the material lines and the
    // reasoning behind them; registering it here keeps every TerraVera item in one registry pass.

    static
    {
        com.terravera.common.temperature.TerraVeraClothing.register(ITEMS);
    }

    /** Raw and intermediate textile goods that the sewing and weaving recipes turn into garments. */
    public static final DeferredHolder<Item, Item> PLANT_FIBER_CLOTH = ITEMS.registerSimpleItem("plant_fiber_cloth");
    public static final DeferredHolder<Item, Item> STRAW_MAT = ITEMS.registerSimpleItem("straw_mat");
    public static final DeferredHolder<Item, Item> FELT_CLOTH = ITEMS.registerSimpleItem("felt_cloth");
    public static final DeferredHolder<Item, Item> LINEN_CLOTH = ITEMS.registerSimpleItem("linen_cloth");
    public static final DeferredHolder<Item, Item> OILSKIN_CLOTH = ITEMS.registerSimpleItem("oilskin_cloth");
    public static final DeferredHolder<Item, Item> QUILTED_CLOTH = ITEMS.registerSimpleItem("quilted_cloth");
    /** Hide with the fur still on it, scraped but not tanned bald. The input to every fur garment. */
    public static final DeferredHolder<Item, Item> FUR_PELT = ITEMS.registerSimpleItem("fur_pelt");
    /** Rendered fat or pressed oil, worked into leather to make it shed water. */
    public static final DeferredHolder<Item, Item> DUBBIN = ITEMS.registerSimpleItem("dubbin");
    /** Carded wool or cattail down, used as the trapped-air filling in quilted clothing. */
    public static final DeferredHolder<Item, Item> BATTING = ITEMS.registerSimpleItem("batting");
    /**
     * Cut-and-sewn garment panels, made at a TFC sewing table.
     * <p>
     * These are the join between TFC's sewing minigame and TerraVera's wardrobe. The sewing table can only tell light
     * cloth from dark cloth, so it cannot know whether the bolt you fed it was wool or linen - which means sewing
     * alone could never produce a material-correct garment. Instead the table produces the <em>shape</em>: you stitch
     * out a hood, a body, a pair of legs, or a pair of feet, and then face those panels with the actual material you
     * want the garment made of. Both halves are real work, and the pattern you sew is genuinely reusable across every
     * material line.
     */
    public static final DeferredHolder<Item, Item> SEWN_HOOD_PANEL = ITEMS.registerSimpleItem("sewn_hood_panel");
    public static final DeferredHolder<Item, Item> SEWN_BODY_PANEL = ITEMS.registerSimpleItem("sewn_body_panel");
    public static final DeferredHolder<Item, Item> SEWN_LEG_PANEL = ITEMS.registerSimpleItem("sewn_leg_panel");
    public static final DeferredHolder<Item, Item> SEWN_FOOT_PANEL = ITEMS.registerSimpleItem("sewn_foot_panel");

    /** A drying frame for wet clothes. Hang a soaked coat near a fire rather than wearing it dry. */
    public static final DeferredHolder<Item, BlockItem> DRYING_RACK = ITEMS.register("drying_rack",
        () -> new BlockItem(TerraVeraBlocks.DRYING_RACK.get(), new Item.Properties()));

    // ----- Greenhouse structures --------------------------------------------------------------------------

    /** Cold frame: a small glazed box that protects seedlings from frost. */
    public static final DeferredHolder<Item, BlockItem> COLD_FRAME = ITEMS.register("cold_frame",
        () -> new GeoMachineItem(TerraVeraBlocks.COLD_FRAME.get(), new Item.Properties(),
            () -> new com.terravera.client.model.GreenhouseModel<>("cold_frame")));

    /** Hoop house: wood frame with fabric/oiled cloth covering for season extension. */
    public static final DeferredHolder<Item, BlockItem> HOOP_HOUSE = ITEMS.register("hoop_house",
        () -> new GeoMachineItem(TerraVeraBlocks.HOOP_HOUSE.get(), new Item.Properties(),
            () -> new com.terravera.client.model.GreenhouseModel<>("hoop_house")));

    /** Glass greenhouse: durable structure with good temperature control and ventilation. */
    public static final DeferredHolder<Item, BlockItem> GLASS_GREENHOUSE = ITEMS.register("glass_greenhouse",
        () -> new GeoMachineItem(TerraVeraBlocks.GLASS_GREENHOUSE.get(), new Item.Properties(),
            () -> new com.terravera.client.model.GreenhouseModel<>("glass_greenhouse")));

    /** Modern greenhouse: powered ventilation, heating, irrigation, and climate control. */
    public static final DeferredHolder<Item, BlockItem> MODERN_GREENHOUSE = ITEMS.register("modern_greenhouse",
        () -> new GeoMachineItem(TerraVeraBlocks.MODERN_GREENHOUSE.get(), new Item.Properties(),
            () -> new com.terravera.client.model.GreenhouseModel<>("modern_greenhouse")));

    // ----- Greenhouse materials ---------------------------------------------------------------------------

    /** A single pane of greenhouse glass, the building block of glass greenhouse walls and roofs. */
    public static final DeferredHolder<Item, Item> GREENHOUSE_GLASS = ITEMS.registerSimpleItem("greenhouse_glass");

    /** Woven oiled cloth for hoop house covering. Lets light through, sheds rain, traps some heat. */
    public static final DeferredHolder<Item, Item> OILED_CLOTH_COVERING = ITEMS.registerSimpleItem("oiled_cloth_covering");

    /** A wooden hoop frame section for hoop houses. */
    public static final DeferredHolder<Item, Item> HOOP_FRAME = ITEMS.registerSimpleItem("hoop_frame");

    /** Iron frame section for modern greenhouse structure. */
    public static final DeferredHolder<Item, Item> GREENHOUSE_FRAME = ITEMS.registerSimpleItem("greenhouse_frame");

    /** Thermostat controller for automated temperature management in modern greenhouses. */
    public static final DeferredHolder<Item, Item> THERMOSTAT = ITEMS.registerSimpleItem("thermostat");

    /** Automated irrigation controller. */
    public static final DeferredHolder<Item, Item> IRRIGATION_CONTROLLER = ITEMS.registerSimpleItem("irrigation_controller");

    // ----- Soil preparation tools and materials -----------------------------------------------------------

    /** A wooden digging stick for breaking soil. The simplest soil preparation tool. */
    public static final DeferredHolder<Item, Item> DIGGING_STICK = ITEMS.registerSimpleItem("digging_stick");

    /** Compost: decomposed organic matter. The primary soil amendment. Improves fertility. */
    public static final DeferredHolder<Item, Item> COMPOST = ITEMS.registerSimpleItem("compost");

    /** Well-rotted manure. Richer than compost but rarer. Major fertility boost. */
    public static final DeferredHolder<Item, Item> AGED_MANURE = ITEMS.registerSimpleItem("aged_manure");

    /** Sand added to heavy clay soil to improve drainage and workability. */
    public static final DeferredHolder<Item, Item> HORTICULTURAL_SAND = ITEMS.registerSimpleItem("horticultural_sand");

    /** Crushed limestone for adjusting soil pH. */
    public static final DeferredHolder<Item, Item> AGRICULTURAL_LIME = ITEMS.registerSimpleItem("agricultural_lime");

    /** A stone- or bone-tipped rake for clearing stones and debris from prepared beds. */
    public static final DeferredHolder<Item, Item> SOIL_RAKE = ITEMS.registerSimpleItem("soil_rake");

    // ----- Seed system -----------------------------------------------------------------------------------

    /** Standard seed. Quality varies. Used for replanting crops. */
    public static final DeferredHolder<Item, Item> SEED = ITEMS.registerSimpleItem("seed");

    /** Select seed: saved from the best plants. Higher quality, better yields. */
    public static final DeferredHolder<Item, Item> SELECT_SEED = ITEMS.registerSimpleItem("select_seed");

    /** Prize seed: generations of careful selection. The best genetics available. */
    public static final DeferredHolder<Item, Item> PRIZE_SEED = ITEMS.registerSimpleItem("prize_seed");

    // ----- Irrigation ------------------------------------------------------------------------------------

    /** Drip irrigation pipe section. Distributes water to nearby prepared farmland. */
    public static final DeferredHolder<Item, BlockItem> DRIP_IRRIGATION = ITEMS.register("drip_irrigation",
        () -> new BlockItem(TerraVeraBlocks.DRIP_IRRIGATION.get(), new Item.Properties()));

    /** Prepared farmland block item (for creative/debug placement). */
    public static final DeferredHolder<Item, BlockItem> PREPARED_FARMLAND = ITEMS.register("prepared_farmland",
        () -> new BlockItem(TerraVeraBlocks.PREPARED_FARMLAND.get(), new Item.Properties()));

    /** Irrigation storage tank. Collects rainwater and feeds drip irrigation. */
    public static final DeferredHolder<Item, BlockItem> IRRIGATION_TANK = ITEMS.register("irrigation_tank",
        () -> new GeoMachineItem(TerraVeraBlocks.IRRIGATION_TANK.get(), new Item.Properties(),
            () -> new com.terravera.client.model.IrrigationTankModel<>()));

    /** A watering can. Simple hand irrigation tool. */
    public static final DeferredHolder<Item, Item> WATERING_CAN = ITEMS.registerSimpleItem("watering_can");

    // ----- Crop disease treatment -------------------------------------------------------------------------

    /** A natural fungicide made from copper sulfate and lime. Controls fungal diseases on crops. */
    public static final DeferredHolder<Item, Item> BORDEAUX_MIXTURE = ITEMS.registerSimpleItem("bordeaux_mixture");

    /** Neem oil extract. Natural pesticide that controls aphids, whiteflies, and other soft-bodied insects. */
    public static final DeferredHolder<Item, Item> NEEM_OIL = ITEMS.registerSimpleItem("neem_oil");

    /** A companion planting guide. Planting certain crops together reduces pest pressure naturally. */
    public static final DeferredHolder<Item, Item> COMPANION_CHART = ITEMS.registerSimpleItem("companion_chart");

    private static DeferredHolder<Item, Item> driedBark(String id, BarkProperties properties)
    {
        return ITEMS.register(id, () -> new BarkItem(properties.dried(), true, null, new Item.Properties()));
    }

    private static DeferredHolder<Item, Item> freshBark(String id, BarkProperties properties,
                                                        Supplier<? extends Item> dried)
    {
        return ITEMS.register(id, () -> new BarkItem(properties, false, dried, new Item.Properties()));
    }

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
