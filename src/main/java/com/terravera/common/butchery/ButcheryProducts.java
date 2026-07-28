/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.butchery;

import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.terravera.common.items.TerraVeraItems;

/**
 * Turns an abstract product id from {@link ButcheryYield} into a real item stack.
 * <p>
 * The split is deliberate. Anything TerraFirmaCraft already models well - hides, in particular, because the whole
 * soaking, scraping, and tanning chain hangs off them - resolves to the TFC item, so butchering feeds straight
 * into TFC's leather progression rather than running beside it. Everything TFC has no concept of (organs, sinew,
 * suet, blood) is a TerraVera item. TFC items are looked up by id at call time and fall back to a vanilla
 * equivalent if TFC is absent, so nothing here hard-fails in a stripped-down instance.
 */
public final class ButcheryProducts
{
    public static ItemStack stackFor(String productId, int count, CarcassSpecies species)
    {
        if (count <= 0) return ItemStack.EMPTY;

        return switch (productId)
        {
            case ButcheryYield.MEAT_SHOULDER -> new ItemStack(TerraVeraItems.SHOULDER_CUT.get(), count);
            case ButcheryYield.MEAT_RIBS -> new ItemStack(TerraVeraItems.RIB_CUT.get(), count);
            case ButcheryYield.MEAT_LOIN -> new ItemStack(TerraVeraItems.LOIN_CUT.get(), count);
            case ButcheryYield.MEAT_LEG -> new ItemStack(TerraVeraItems.LEG_CUT.get(), count);
            case ButcheryYield.TRIM_MEAT -> new ItemStack(TerraVeraItems.TRIM_MEAT.get(), count);

            case ButcheryYield.ANIMAL_FAT -> new ItemStack(TerraVeraItems.ANIMAL_FAT.get(), count);
            case ButcheryYield.SUET -> new ItemStack(TerraVeraItems.SUET.get(), count);
            case ButcheryYield.SINEW -> new ItemStack(TerraVeraItems.SINEW.get(), count);
            case ButcheryYield.TENDON -> new ItemStack(TerraVeraItems.TENDON.get(), count);
            case ButcheryYield.BLOOD -> new ItemStack(TerraVeraItems.BLOOD.get(), count);

            case ButcheryYield.HEART -> new ItemStack(TerraVeraItems.HEART.get(), count);
            case ButcheryYield.LIVER -> new ItemStack(TerraVeraItems.LIVER.get(), count);
            case ButcheryYield.KIDNEYS -> new ItemStack(TerraVeraItems.KIDNEYS.get(), count);
            case ButcheryYield.STOMACH -> new ItemStack(TerraVeraItems.STOMACH.get(), count);

            case ButcheryYield.MARROW_BONE -> new ItemStack(TerraVeraItems.MARROW_BONE.get(), count);
            // Plain bone is vanilla's, which TFC also uses, so bone meal and every existing bone recipe just work.
            case ButcheryYield.BONE -> new ItemStack(Items.BONE, count);

            case ButcheryYield.HIDE -> new ItemStack(hideItem(species), count);
            case ButcheryYield.FLEECE -> new ItemStack(tfcOrElse("wool", Items.WHITE_WOOL), count);

            default -> ItemStack.EMPTY;
        };
    }

    /**
     * TFC grades raw hides by animal size, and the tanning chain cares which one it gets, so a deer must not hand
     * back a cow's hide. Sizes follow TFC's own convention: small for game and goats, large for cattle upward.
     */
    private static Item hideItem(CarcassSpecies species)
    {
        final String size = species.carcassMass() >= 100f ? "large"
            : species.carcassMass() >= 30f ? "medium"
            : "small";
        return tfcOrElse(size + "_raw_hide", Items.LEATHER);
    }

    /** Resolve a TerraFirmaCraft item by path, falling back to a vanilla stand-in if TFC is not present. */
    private static Item tfcOrElse(String path, Item fallback)
    {
        final Optional<Item> found = BuiltInRegistries.ITEM
            .getOptional(ResourceLocation.fromNamespaceAndPath("tfc", path));
        return found.orElse(fallback);
    }

    private ButcheryProducts() {}
}
