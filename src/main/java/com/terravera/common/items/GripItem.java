/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.items;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A pre-cut wrap ready to be bound over a wooden tool handle. */
public class GripItem extends Item
{
    private final String material;

    public GripItem(String material, Properties properties)
    {
        super(properties.stacksTo(16));
        this.material = material;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
    {
        tooltip.add(Component.translatable("terravera.tooltip.grip.requires_handle").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("terravera.tooltip.grip." + material).withStyle(ChatFormatting.DARK_GREEN));
    }
}
