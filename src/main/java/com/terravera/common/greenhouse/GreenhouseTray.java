/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.greenhouse;

import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One planted tray inside a greenhouse: what was sown in it, and how far along it is.
 * <p>
 * Trays used to be a bare counter, which is why nothing could ever be collected from them - the greenhouse knew how
 * many seeds had gone in but not what they were or whether they had finished. Keeping the seed item and a 0..1
 * progress value is the minimum needed for the tray to actually produce a crop, and it means a tray of tomatoes and
 * a tray of barley behave differently, which is the whole point of the specialisation table.
 *
 * @param seed     the seed item that was sown
 * @param progress growth completion, 0..1
 */
public record GreenhouseTray(Item seed, float progress)
{
    /** How much progress a tray makes per greenhouse tick (every 100 ticks) under perfect conditions. */
    private static final float BASE_GROWTH_PER_TICK = 0.02f;

    public static GreenhouseTray sow(ItemStack seedStack)
    {
        return new GreenhouseTray(seedStack.getItem(), 0f);
    }

    public boolean mature()
    {
        return progress >= 1f;
    }

    /**
     * Advance the tray. Growth is the product of the greenhouse's own climate quality and how well this particular
     * crop suits a greenhouse, so a cold, unlit hoop house genuinely stalls rather than quietly ticking along.
     */
    public GreenhouseTray grown(GreenhouseClimate climate, String season)
    {
        if (mature()) return this;

        final ItemStack seedStack = new ItemStack(seed);
        final float multiplier = CropSpecialization.yieldMultiplier(seedStack, season, climate);
        final float step = BASE_GROWTH_PER_TICK * Math.max(0f, multiplier);
        return new GreenhouseTray(seed, Math.min(1f, progress + step));
    }

    /**
     * What this tray yields when collected.
     * <p>
     * Resolved from the seed's own id rather than a lookup table, so it works for every TFC crop and for anything
     * an addon registers that follows the same {@code seeds/x -> food/x} convention. If nothing matches, the seed
     * comes back rather than vanishing - a greenhouse should never eat a player's seed stock.
     */
    public ItemStack harvest(int count)
    {
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(seed);
        final String path = id.getPath();

        // TFC: "seeds/wheat" -> "food/wheat".
        if (path.startsWith("seeds/"))
        {
            final Optional<Item> food = BuiltInRegistries.ITEM.getOptional(
                ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "food/" + path.substring("seeds/".length())));
            if (food.isPresent()) return new ItemStack(food.get(), count);
        }

        // Vanilla: "wheat_seeds" -> "wheat", "beetroot_seeds" -> "beetroot".
        if (path.endsWith("_seeds"))
        {
            final Optional<Item> crop = BuiltInRegistries.ITEM.getOptional(
                ResourceLocation.fromNamespaceAndPath(id.getNamespace(), path.substring(0, path.length() - 6)));
            if (crop.isPresent()) return new ItemStack(crop.get(), count);
        }

        return new ItemStack(seed, count);
    }

    public CompoundTag save()
    {
        final CompoundTag tag = new CompoundTag();
        tag.putString("seed", BuiltInRegistries.ITEM.getKey(seed).toString());
        tag.putFloat("progress", progress);
        return tag;
    }

    public static GreenhouseTray load(CompoundTag tag)
    {
        final ResourceLocation id = ResourceLocation.tryParse(tag.getString("seed"));
        final Item seed = id == null
            ? net.minecraft.world.item.Items.WHEAT_SEEDS
            : BuiltInRegistries.ITEM.getOptional(id).orElse(net.minecraft.world.item.Items.WHEAT_SEEDS);
        return new GreenhouseTray(seed, tag.getFloat("progress"));
    }
}
