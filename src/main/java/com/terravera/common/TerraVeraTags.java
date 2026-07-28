package com.terravera.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import com.terravera.TerraVera;

public final class TerraVeraTags
{
    public static final class Blocks
    {
        public static final TagKey<Block> RESIN_TREES = blockTag("resin_trees");

        private static TagKey<Block> blockTag(String name)
        {
            return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(TerraVera.MOD_ID, name));
        }
    }

    public static final class Items
    {
        public static final TagKey<Item> TAPE = itemTag("tape");
        public static final TagKey<Item> GLUES = itemTag("glues");
        public static final TagKey<Item> ADHESIVES = itemTag("adhesives");
        public static final TagKey<Item> RESIN = itemTag("resin");

        private static TagKey<Item> itemTag(String name)
        {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TerraVera.MOD_ID, name));
        }
    }

    private TerraVeraTags() {}
}
