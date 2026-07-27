/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.temperature;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.terravera.TerraVera;

/**
 * TerraVera's wardrobe: the garments, their materials, and the armour materials behind them.
 *
 * <h2>Why this many garments</h2>
 * The point of the body temperature system is that <em>what you are wearing</em> is a decision you keep making, and a
 * decision needs options that are genuinely different from each other rather than strictly better. The roster is
 * built as a small number of material lines, each with its own reason to exist:
 *
 * <table>
 *     <tr><th>Line</th><th>When you make it</th><th>Why you would wear it</th></tr>
 *     <tr><td>Plant fibre</td><td>Day one, from the same fibre as your cordage</td>
 *         <td>Barely better than bare skin, but it is what you have.</td></tr>
 *     <tr><td>Straw</td><td>Day one</td><td>A wide hat is real sun protection, and nothing else you can make is.</td></tr>
 *     <tr><td>Burlap</td><td>Once you have jute and a needle</td><td>The first actual clothing.</td></tr>
 *     <tr><td>Leather</td><td>Once you can tan a hide</td><td>Stops wind and weather almost completely.</td></tr>
 *     <tr><td>Linen</td><td>Flax and a loom</td><td>The hot-climate answer: light, breathable, fast drying.</td></tr>
 *     <tr><td>Wool</td><td>Sheep and shears</td><td>The cold-climate milestone, and it still works soaked.</td></tr>
 *     <tr><td>Felt</td><td>Wool, worked further</td><td>Warmer and windproof, but it does not breathe.</td></tr>
 *     <tr><td>Oilskin</td><td>Leather and oil</td><td>The rain answer. It does not wet through, so it does not fail.</td></tr>
 *     <tr><td>Fur</td><td>Hunting, in the cold</td><td>The warmest thing there is, and a liability anywhere warm.</td></tr>
 *     <tr><td>Silk</td><td>Late, from a silk chain</td><td>Light and versatile; good in both directions.</td></tr>
 *     <tr><td>Quilted</td><td>Late, layered cloth and stuffing</td><td>The best cold gear that can be sewn.</td></tr>
 * </table>
 *
 * Each line covers the four body slots, which is where the bulk of the count comes from, plus a handful of
 * single-slot specialities (the straw sun hat, the fur-lined hood, the oilskin cloak) that only make sense as one
 * piece. Physical armour value is near-zero throughout on purpose: this is clothing, not an armour tier, and if a
 * linen shirt gave protection the clothing system would quietly become the best armour progression in the game.
 */
public final class TerraVeraClothing
{
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
        DeferredRegister.create(Registries.ARMOR_MATERIAL, TerraVera.MOD_ID);

    /** Every registered garment, in registration order, keyed by registry path. */
    private static final Map<String, DeferredHolder<Item, Item>> GARMENTS = new LinkedHashMap<>();

    /** One armour material per clothing material, so each line can have its own texture and equip sound. */
    private static final Map<ClothingMaterial, Holder<ArmorMaterial>> MATERIALS = new EnumMap<>(ClothingMaterial.class);

    // ----- Armour materials --------------------------------------------------------------------------------------

    static
    {
        // Soft goods get one armour point at most, and only on the chest. This is a coat, not a cuirass.
        material(ClothingMaterial.PLANT_FIBER, 0, 0, 0, 0, 1, SoundEvents.ARMOR_EQUIP_LEATHER);
        material(ClothingMaterial.STRAW, 0, 0, 0, 0, 1, SoundEvents.ARMOR_EQUIP_LEATHER);
        material(ClothingMaterial.BURLAP, 0, 1, 0, 0, 2, SoundEvents.ARMOR_EQUIP_LEATHER);
        material(ClothingMaterial.LINEN, 0, 1, 0, 0, 2, SoundEvents.ARMOR_EQUIP_LEATHER);
        material(ClothingMaterial.WOOL, 1, 1, 1, 0, 3, SoundEvents.ARMOR_EQUIP_LEATHER);
        material(ClothingMaterial.FELT, 1, 2, 1, 1, 4, SoundEvents.ARMOR_EQUIP_LEATHER);
        material(ClothingMaterial.LEATHER, 1, 2, 1, 1, 6, SoundEvents.ARMOR_EQUIP_LEATHER);
        material(ClothingMaterial.OILSKIN, 1, 2, 1, 1, 5, SoundEvents.ARMOR_EQUIP_LEATHER);
        material(ClothingMaterial.FUR, 1, 2, 1, 1, 5, SoundEvents.ARMOR_EQUIP_LEATHER);
        material(ClothingMaterial.SILK, 0, 1, 0, 0, 8, SoundEvents.ARMOR_EQUIP_LEATHER);
        material(ClothingMaterial.QUILTED, 1, 2, 1, 1, 5, SoundEvents.ARMOR_EQUIP_LEATHER);
    }

    private static void material(ClothingMaterial material, int head, int chest, int legs, int feet,
                                 int enchantability, Holder<net.minecraft.sounds.SoundEvent> equipSound)
    {
        final Holder<ArmorMaterial> holder = ARMOR_MATERIALS.register(material.id(), () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.HELMET, head);
                map.put(ArmorItem.Type.CHESTPLATE, chest);
                map.put(ArmorItem.Type.LEGGINGS, legs);
                map.put(ArmorItem.Type.BOOTS, feet);
                map.put(ArmorItem.Type.BODY, chest);
            }),
            enchantability,
            equipSound,
            // Repaired with the cloth or hide it was sewn from, via a per-material tag packs can extend.
            () -> Ingredient.of(net.minecraft.tags.TagKey.create(Registries.ITEM,
                TerraVera.identifier("clothing_repair/" + material.id()))),
            List.of(new ArmorMaterial.Layer(TerraVera.identifier(material.id()))),
            0f,
            0f
        ));
        MATERIALS.put(material, holder);
    }

    public static Holder<ArmorMaterial> armorMaterial(ClothingMaterial material)
    {
        return MATERIALS.get(material);
    }

    // ----- The garments ------------------------------------------------------------------------------------------

    /**
     * Registers the whole wardrobe against the mod's item register.
     * <p>
     * Called from {@link com.terravera.common.items.TerraVeraItems} so that garments land in the same registry pass
     * as everything else and appear in the creative tab in a sensible order.
     */
    public static void register(DeferredRegister.Items items)
    {
        // --- Plant fibre: what you can make on day one, out of the fibre you are already gathering ---
        garment(items, "fiber_cap", ClothingMaterial.PLANT_FIBER, GarmentSlot.HEAD, 26);
        garment(items, "fiber_poncho", ClothingMaterial.PLANT_FIBER, GarmentSlot.CHEST, 34);
        garment(items, "fiber_leggings", ClothingMaterial.PLANT_FIBER, GarmentSlot.LEGS, 30);
        garment(items, "fiber_sandals", ClothingMaterial.PLANT_FIBER, GarmentSlot.FEET, 24);

        // --- Straw: the sun hat, which is the only reason to weave straw and a genuinely good one ---
        garment(items, "straw_sun_hat", ClothingMaterial.STRAW, GarmentSlot.HEAD, 30);
        garment(items, "straw_rain_cape", ClothingMaterial.STRAW, GarmentSlot.CHEST, 30);

        // --- Burlap: the first real fabric, coarse and cheap ---
        garment(items, "burlap_hood", ClothingMaterial.BURLAP, GarmentSlot.HEAD, 46);
        garment(items, "burlap_tunic", ClothingMaterial.BURLAP, GarmentSlot.CHEST, 58);
        garment(items, "burlap_trousers", ClothingMaterial.BURLAP, GarmentSlot.LEGS, 52);
        garment(items, "burlap_shoes", ClothingMaterial.BURLAP, GarmentSlot.FEET, 44);

        // --- Linen: light, breathable, and the correct answer to a hot climate ---
        garment(items, "linen_headwrap", ClothingMaterial.LINEN, GarmentSlot.HEAD, 52);
        garment(items, "linen_shirt", ClothingMaterial.LINEN, GarmentSlot.CHEST, 66);
        garment(items, "linen_trousers", ClothingMaterial.LINEN, GarmentSlot.LEGS, 60);
        garment(items, "linen_shoes", ClothingMaterial.LINEN, GarmentSlot.FEET, 50);

        // --- Wool: the cold-weather milestone. Still insulates when soaked ---
        garment(items, "wool_cap", ClothingMaterial.WOOL, GarmentSlot.HEAD, 66);
        garment(items, "wool_sweater", ClothingMaterial.WOOL, GarmentSlot.CHEST, 82);
        garment(items, "wool_trousers", ClothingMaterial.WOOL, GarmentSlot.LEGS, 74);
        garment(items, "wool_socks", ClothingMaterial.WOOL, GarmentSlot.FEET, 60);

        // --- Felt: matted wool. Warmer and windproof, but airless ---
        garment(items, "felt_hat", ClothingMaterial.FELT, GarmentSlot.HEAD, 74);
        garment(items, "felt_coat", ClothingMaterial.FELT, GarmentSlot.CHEST, 92);
        garment(items, "felt_leggings", ClothingMaterial.FELT, GarmentSlot.LEGS, 84);
        garment(items, "felt_boots", ClothingMaterial.FELT, GarmentSlot.FEET, 70);

        // --- Leather: modest warmth, near-total wind and weather protection ---
        garment(items, "leather_cap", ClothingMaterial.LEATHER, GarmentSlot.HEAD, 92);
        garment(items, "leather_jerkin", ClothingMaterial.LEATHER, GarmentSlot.CHEST, 112);
        garment(items, "leather_trousers", ClothingMaterial.LEATHER, GarmentSlot.LEGS, 102);
        garment(items, "leather_boots", ClothingMaterial.LEATHER, GarmentSlot.FEET, 88);

        // --- Oilskin: the rain answer. It does not wet through, so it does not stop working ---
        garment(items, "oilskin_hat", ClothingMaterial.OILSKIN, GarmentSlot.HEAD, 100);
        garment(items, "oilskin_cloak", ClothingMaterial.OILSKIN, GarmentSlot.CHEST, 124);
        garment(items, "oilskin_leggings", ClothingMaterial.OILSKIN, GarmentSlot.LEGS, 112);
        garment(items, "oilskin_boots", ClothingMaterial.OILSKIN, GarmentSlot.FEET, 96);

        // --- Fur: the warmest thing available, and unwearable anywhere warm ---
        garment(items, "fur_hood", ClothingMaterial.FUR, GarmentSlot.HEAD, 108);
        garment(items, "fur_parka", ClothingMaterial.FUR, GarmentSlot.CHEST, 134);
        garment(items, "fur_leggings", ClothingMaterial.FUR, GarmentSlot.LEGS, 122);
        garment(items, "fur_boots", ClothingMaterial.FUR, GarmentSlot.FEET, 104);

        // --- Silk: light, strong, and unusually good in both directions ---
        garment(items, "silk_veil", ClothingMaterial.SILK, GarmentSlot.HEAD, 84);
        garment(items, "silk_robe", ClothingMaterial.SILK, GarmentSlot.CHEST, 104);
        garment(items, "silk_trousers", ClothingMaterial.SILK, GarmentSlot.LEGS, 94);
        garment(items, "silk_slippers", ClothingMaterial.SILK, GarmentSlot.FEET, 80);

        // --- Quilted: the best cold-weather clothing that can be sewn ---
        garment(items, "quilted_hood", ClothingMaterial.QUILTED, GarmentSlot.HEAD, 116);
        garment(items, "quilted_coat", ClothingMaterial.QUILTED, GarmentSlot.CHEST, 146);
        garment(items, "quilted_leggings", ClothingMaterial.QUILTED, GarmentSlot.LEGS, 132);
        garment(items, "quilted_boots", ClothingMaterial.QUILTED, GarmentSlot.FEET, 112);
    }

    private static void garment(DeferredRegister.Items items, String name, ClothingMaterial material,
                                GarmentSlot slot, int durability)
    {
        GARMENTS.put(name, items.register(name, () -> new ClothingItem(
            armorMaterial(material), material, slot,
            new Item.Properties().durability(durability))));
    }

    /** Every garment, for the creative tab and for tests that check the roster is complete. */
    public static Map<String, DeferredHolder<Item, Item>> garments()
    {
        return java.util.Collections.unmodifiableMap(GARMENTS);
    }

    /** The registry names of every garment, in registration order. */
    public static List<String> names()
    {
        return new ArrayList<>(GARMENTS.keySet());
    }

    private TerraVeraClothing() {}
}
