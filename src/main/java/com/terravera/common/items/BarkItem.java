/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 */

package com.terravera.common.items;

import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;

import com.terravera.common.TerraVeraDataComponents;
import com.terravera.common.component.BarkProperties;

/** A species-specific bark sheet, either fresh or air dried. */
public class BarkItem extends Item
{
    private final BarkProperties defaults;
    private final boolean driedForm;
    @Nullable private final Supplier<? extends Item> driedItem;

    public BarkItem(BarkProperties defaults, boolean driedForm, @Nullable Supplier<? extends Item> driedItem,
                    Properties properties)
    {
        super(properties.component(TerraVeraDataComponents.BARK_PROPERTIES.get(), defaults));
        this.defaults = defaults;
        this.driedForm = driedForm;
        this.driedItem = driedItem;
    }

    public BarkProperties properties(ItemStack stack)
    {
        return stack.getOrDefault(TerraVeraDataComponents.BARK_PROPERTIES.get(), defaults);
    }

    public boolean isDriedForm()
    {
        return driedForm;
    }

    /**
     * Removes moisture and, at the dry threshold, changes the stack to the matching dry material. The whole stack can
     * change together because only equal component values stack in the first place.
     */
    public ItemStack dry(ItemStack stack, float amount)
    {
        final BarkProperties before = properties(stack);
        final BarkProperties after = before.withMoisture(before.moisture() - Math.max(0f, amount));
        if (after.isDry() && !driedForm && driedItem != null)
        {
            final ItemStack result = new ItemStack(driedItem.get(), stack.getCount());
            result.set(TerraVeraDataComponents.BARK_PROPERTIES.get(), after.dried());
            return result;
        }
        stack.set(TerraVeraDataComponents.BARK_PROPERTIES.get(), after);
        return stack;
    }

    /** Dry bark is a quick, modest furnace fuel. Fresh bark deliberately returns zero and cannot catch. */
    @Override
    public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType)
    {
        final BarkProperties bark = properties(stack);
        if (!driedForm || !bark.isDry()) return 0;
        return Math.max(80, Math.round(260f * bark.flammability()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
    {
        final BarkProperties bark = properties(stack);
        tooltip.add(Component.translatable("terravera.tooltip.bark.species",
            Component.translatable("terravera.bark.species." + bark.species())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("terravera.tooltip.bark.moisture", bark.percentageMoisture() + "%")
            .withStyle(bark.isDry() ? ChatFormatting.GOLD : ChatFormatting.AQUA));
        tooltip.add(Component.translatable("terravera.tooltip.bark.tannin", rating(bark.tannin()))
            .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("terravera.tooltip.bark.flexibility", rating(bark.flexibility()))
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("terravera.tooltip.bark.flammability", rating(bark.flammability()))
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("terravera.tooltip.bark.thickness", String.format(java.util.Locale.ROOT, "%.1f", bark.thicknessMm()))
            .withStyle(ChatFormatting.DARK_GRAY));
        if (!bark.isDry())
        {
            tooltip.add(Component.translatable("terravera.tooltip.bark.needs_drying").withStyle(ChatFormatting.BLUE));
        }
    }

    private static Component rating(float value)
    {
        final String band = value >= 0.8f ? "very_high" : value >= 0.6f ? "high"
            : value >= 0.4f ? "moderate" : value >= 0.2f ? "low" : "very_low";
        return Component.translatable("terravera.bark.rating." + band);
    }
}
