/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.health;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import com.terravera.TerraVera;

/**
 * Tags the health system reads. Everything the disease and sanitation systems care about is expressed as a tag, so a
 * modpack can point them at other mods' blocks and items without a code change.
 */
public final class TerraVeraHealthTags
{
    public static final class Blocks
    {
        /**
         * Blocks that make adjacent water dirtier. Mud, muck, rotting matter, and TerraVera's own waste blocks. This is
         * how a swamp reads as a swamp, and how a latrine dug next to your well ruins the well.
         */
        public static final TagKey<Block> FOULS_WATER = tag("fouls_water");

        /** Blocks that get you dirty to stand in or work with. Drives hygiene decay. */
        public static final TagKey<Block> SOILS_PLAYER = tag("soils_player");

        /** Blocks the player can wash at - a water source, a basin, a trough. */
        public static final TagKey<Block> WASHING_STATION = tag("washing_station");

        private static TagKey<Block> tag(String name)
        {
            return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(TerraVera.MOD_ID, name));
        }

        private Blocks() {}
    }

    public static final class Items
    {
        /** Containers whose contents count as treated water regardless of where they were filled. */
        public static final TagKey<Item> HOLDS_TREATED_WATER = tag("holds_treated_water");

        /** Anything that will clean your hands: soap, ash, lye, clean water. */
        public static final TagKey<Item> HYGIENE_ITEMS = tag("hygiene_items");

        /** Soap specifically - the best hygiene item, and a prerequisite for the sanitation tier. */
        public static final TagKey<Item> SOAP = tag("soap");

        /** Meat and fish that will make you ill if eaten raw. */
        public static final TagKey<Item> RISKY_RAW_MEAT = tag("risky_raw_meat");

        // --- Remedies, roughly in progression order -------------------------------------------------------

        /** Bitter, tannin-rich, or antimicrobial plant matter. The stone-age answer to a gut infection. */
        public static final TagKey<Item> HERBAL_REMEDY = tag("remedies/herbal");

        /** Rehydration: salted, sweetened water. Crude oral rehydration therapy, and it genuinely works. */
        public static final TagKey<Item> REHYDRATION_REMEDY = tag("remedies/rehydration");

        /** Anti-parasitic plants and preparations - wormwood, garlic, pumpkin seed. */
        public static final TagKey<Item> ANTIPARASITIC_REMEDY = tag("remedies/antiparasitic");

        /** Wound care: clean dressings, alcohol, honey. Prevents and treats infected wounds. */
        public static final TagKey<Item> WOUND_REMEDY = tag("remedies/wound");

        /** Fever management - willow bark and its relatives. Real salicylate, real antipyretic. */
        public static final TagKey<Item> ANTIPYRETIC_REMEDY = tag("remedies/antipyretic");

        /** Distilled, refined, late-game medicine. Cures nearly anything, and requires real infrastructure. */
        public static final TagKey<Item> MEDICINE = tag("remedies/medicine");

        private static TagKey<Item> tag(String name)
        {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TerraVera.MOD_ID, name));
        }

        private Items() {}
    }

    public static final class Fluids
    {
        /** Fluids that count as safe to drink no matter where they came from. */
        public static final TagKey<Fluid> TREATED = tag("treated_water");

        private static TagKey<Fluid> tag(String name)
        {
            return TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(TerraVera.MOD_ID, name));
        }

        private Fluids() {}
    }

    private TerraVeraHealthTags() {}
}
