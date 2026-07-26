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

import com.terravera.common.TerraVeraDataComponents;
import com.terravera.common.component.Cordage;

/**
 * Twisted plant cordage. Carries a {@link Cordage} component describing what it was twisted from and how strong the
 * result is, which flows through into the durability of anything lashed with it.
 */
public class CordageItem extends Item
{
    public CordageItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
    {
        final Cordage cordage = stack.getOrDefault(TerraVeraDataComponents.CORDAGE.get(), Cordage.DEFAULT);
        tooltip.add(Component.translatable("terravera.tooltip.cordage_source",
            Component.translatable("terravera.fibre." + cordage.source())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("terravera.tooltip.cordage_strength",
            Component.literal("%d%%".formatted(Math.round(cordage.strength() * 100)))).withStyle(ChatFormatting.GRAY));
    }
}
