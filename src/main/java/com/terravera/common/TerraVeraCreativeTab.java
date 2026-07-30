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
        event.accept(TerraVeraItems.SINGLE_WIRE.get());
        event.accept(TerraVeraItems.COPPER_WIRE.get());
        event.accept(TerraVeraItems.WIRE_INTERSECTION.get());
        event.accept(TerraVeraItems.PROGRAMMED_CIRCUIT.get());
        event.accept(TerraVeraItems.REFRIGERANT_CANISTER.get());
        event.accept(TerraVeraItems.AIR_FILTER.get());
        event.accept(TerraVeraItems.RAIN_CATCHER.get());
        event.accept(TerraVeraItems.DEW_COLLECTOR.get());
        event.accept(TerraVeraItems.ROCK_BASIN.get());
        event.accept(TerraVeraItems.SOLAR_STILL.get());
        event.accept(TerraVeraItems.WORKPLATE.get());
        event.accept(TerraVeraItems.RUBBLE_FOUNDATION.get());
        event.accept(TerraVeraItems.WOODEN_SUPPORT_BEAM.get());
        event.accept(TerraVeraItems.WROUGHT_IRON_SUPPORT_BEAM.get());
        event.accept(TerraVeraItems.BARK_ROOFING.get());
        event.accept(TerraVeraItems.FIELD_NOTES.get());
        event.accept(TerraVeraItems.LEATHER_TOOL_GRIP.get());
        event.accept(TerraVeraItems.RAW_LATEX.get());
        event.accept(TerraVeraItems.RUBBER_TOOL_GRIP.get());

        event.accept(TerraVeraItems.OAK_BARK.get());
        event.accept(TerraVeraItems.HEMLOCK_BARK.get());
        event.accept(TerraVeraItems.WILLOW_BARK.get());
        event.accept(TerraVeraItems.BIRCH_BARK.get());
        event.accept(TerraVeraItems.BAST_BARK.get());
        event.accept(TerraVeraItems.BARK.get());
        event.accept(TerraVeraItems.DRIED_OAK_BARK.get());
        event.accept(TerraVeraItems.DRIED_HEMLOCK_BARK.get());
        event.accept(TerraVeraItems.DRIED_WILLOW_BARK.get());
        event.accept(TerraVeraItems.DRIED_BIRCH_BARK.get());
        event.accept(TerraVeraItems.DRIED_BAST_BARK.get());
        event.accept(TerraVeraItems.DRIED_BARK.get());
        event.accept(TerraVeraItems.BIRCH_BARK_CONTAINER.get());

        event.accept(TerraVeraItems.PLANT_FIBER.get());
        event.accept(TerraVeraItems.RETTED_FIBER.get());
        event.accept(TerraVeraItems.BAST_FIBER.get());
        event.accept(TerraVeraItems.PRIMITIVE_CORDAGE.get());
        event.accept(TerraVeraItems.CORDAGE.get());
        event.accept(TerraVeraItems.HEAVY_CORDAGE.get());

        // Natural glue is an alternative to a lashing where a joint needs bonding rather than tying.
        event.accept(TerraVeraItems.PINE_PITCH.get());
        event.accept(TerraVeraItems.BIRCH_TAR.get());
        event.accept(TerraVeraItems.HIDE_GLUE.get());
        event.accept(TerraVeraItems.FISH_GLUE.get());
        event.accept(TerraVeraItems.CASEIN_GLUE.get());
        event.accept(TerraVeraItems.PANCAKE_BATTER.get());
        event.accept(TerraVeraItems.WAFFLE_BATTER.get());
        event.accept(TerraVeraItems.CREPE_BATTER.get());
        event.accept(TerraVeraItems.FLATBREAD_DOUGH.get());
        event.accept(TerraVeraItems.BISCUIT_DOUGH.get());
        event.accept(TerraVeraItems.FRITTER_BATTER.get());
        event.accept(TerraVeraItems.JOHNNYCAKE_BATTER.get());
        event.accept(TerraVeraItems.HONEYBERRY_BATTER.get());
        event.accept(TerraVeraItems.PANCAKES.get());
        event.accept(TerraVeraItems.WAFFLES.get());
        event.accept(TerraVeraItems.CREPES.get());
        event.accept(TerraVeraItems.GRIDDLE_FLATBREAD.get());
        event.accept(TerraVeraItems.BUTTERMILK_BISCUITS.get());
        event.accept(TerraVeraItems.APPLE_FRITTERS.get());
        event.accept(TerraVeraItems.JOHNNYCAKES.get());
        event.accept(TerraVeraItems.HONEYBERRY_CAKES.get());

        // Health, hygiene, and water treatment, in rough progression order.
        event.accept(TerraVeraItems.BITTER_HERBS.get());
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

        // Writing: inks, instruments, and written records.
        event.accept(TerraVeraItems.IRON_GALL_INK.get());
        event.accept(TerraVeraItems.CHARCOAL_INK.get());
        event.accept(TerraVeraItems.QUILL.get());
        event.accept(TerraVeraItems.WRITTEN_PAPER.get());
        event.accept(TerraVeraItems.WRITTEN_PARCHMENT.get());
        event.accept(TerraVeraItems.WRITTEN_BARK.get());
        event.accept(TerraVeraItems.WRITTEN_STONE.get());

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

        // Greenhouse structures and materials, in progression order.
        event.accept(TerraVeraItems.COLD_FRAME.get());
        event.accept(TerraVeraItems.HOOP_HOUSE.get());
        event.accept(TerraVeraItems.GLASS_GREENHOUSE.get());
        event.accept(TerraVeraItems.MODERN_GREENHOUSE.get());
        event.accept(TerraVeraItems.GREENHOUSE_GLASS.get());
        event.accept(TerraVeraItems.OILED_CLOTH_COVERING.get());
        event.accept(TerraVeraItems.HOOP_FRAME.get());
        event.accept(TerraVeraItems.GREENHOUSE_FRAME.get());
        event.accept(TerraVeraItems.THERMOSTAT.get());
        event.accept(TerraVeraItems.IRRIGATION_CONTROLLER.get());

        // Soil preparation tools and amendments.
        event.accept(TerraVeraItems.DIGGING_STICK.get());
        event.accept(TerraVeraItems.SOIL_RAKE.get());
        event.accept(TerraVeraItems.COMPOST.get());
        event.accept(TerraVeraItems.AGED_MANURE.get());
        event.accept(TerraVeraItems.HORTICULTURAL_SAND.get());
        event.accept(TerraVeraItems.AGRICULTURAL_LIME.get());
        event.accept(TerraVeraItems.PREPARED_FARMLAND.get());

        // Butchering: the carcass, the parts that come off it, and what those parts become.
        event.accept(TerraVeraItems.CARCASS.get());
        event.accept(TerraVeraItems.CARCASS_RACK.get());
        event.accept(TerraVeraItems.WROUGHT_IRON_BUTCHERS_KNIFE.get());
        event.accept(TerraVeraItems.STEEL_BUTCHERS_KNIFE.get());
        event.accept(TerraVeraItems.BLACK_STEEL_BUTCHERS_KNIFE.get());
        event.accept(TerraVeraItems.BLUE_STEEL_BUTCHERS_KNIFE.get());
        event.accept(TerraVeraItems.RED_STEEL_BUTCHERS_KNIFE.get());
        event.accept(TerraVeraItems.SHOULDER_CUT.get());
        event.accept(TerraVeraItems.RIB_CUT.get());
        event.accept(TerraVeraItems.LOIN_CUT.get());
        event.accept(TerraVeraItems.LEG_CUT.get());
        event.accept(TerraVeraItems.TRIM_MEAT.get());
        event.accept(TerraVeraItems.CURED_MEAT.get());
        event.accept(TerraVeraItems.DRIED_MEAT_STRIPS.get());
        event.accept(TerraVeraItems.HEART.get());
        event.accept(TerraVeraItems.LIVER.get());
        event.accept(TerraVeraItems.KIDNEYS.get());
        event.accept(TerraVeraItems.STOMACH.get());
        event.accept(TerraVeraItems.BLOOD.get());
        event.accept(TerraVeraItems.BLOOD_MEAL.get());
        event.accept(TerraVeraItems.ANIMAL_FAT.get());
        event.accept(TerraVeraItems.SUET.get());
        event.accept(TerraVeraItems.RENDERED_TALLOW.get());
        event.accept(TerraVeraItems.TALLOW_CANDLE.get());
        event.accept(TerraVeraItems.SINEW.get());
        event.accept(TerraVeraItems.TENDON.get());
        event.accept(TerraVeraItems.SINEW_CORD.get());
        event.accept(TerraVeraItems.SINEW_BOWSTRING.get());
        event.accept(TerraVeraItems.MARROW_BONE.get());
        event.accept(TerraVeraItems.BONE_MARROW.get());
        event.accept(TerraVeraItems.BONE_NEEDLE.get());
        event.accept(TerraVeraItems.BONE_AWL.get());

        // Irrigation equipment.
        event.accept(TerraVeraItems.DRIP_IRRIGATION.get());
        event.accept(TerraVeraItems.IRRIGATION_TANK.get());
        event.accept(TerraVeraItems.WATERING_CAN.get());

        // Crop disease treatment.
        event.accept(TerraVeraItems.BORDEAUX_MIXTURE.get());
        event.accept(TerraVeraItems.NEEM_OIL.get());
        event.accept(TerraVeraItems.COMPANION_CHART.get());

        // Greenhouse accessories.
        event.accept(TerraVeraItems.SEED_TRAY.get());
        event.accept(TerraVeraItems.TRELLIS.get());
        event.accept(TerraVeraItems.SHADE_CLOTH.get());
        event.accept(TerraVeraItems.THERMAL_MASS_BARREL.get());
        event.accept(TerraVeraItems.MULCH_LAYER.get());

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
