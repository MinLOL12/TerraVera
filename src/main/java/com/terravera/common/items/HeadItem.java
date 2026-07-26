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
import com.terravera.common.component.KnappedHead;

/**
 * A knapped stone head. There is exactly one of these items per working-end kind ({@code wedge}, {@code point},
 * {@code blade}, {@code broad}, {@code maul}), and the actual stone it is made of lives in the
 * {@link KnappedHead} component rather than in the item id.
 * <p>
 * This is the second half of the "you don't need to knap the exact shape" idea. In TerraFirmaCraft there are
 * 4 rock categories x 6 tool heads = 24 distinct head items, each with its own picture to reproduce. Here there are
 * five, and the one you get depends on the geometry you happened to produce, not on which recipe you were aiming at.
 */
public class HeadItem extends Item
{
    private final String kindPath;

    public HeadItem(String kindPath, Properties properties)
    {
        super(properties);
        this.kindPath = kindPath;
    }

    public String kindPath()
    {
        return kindPath;
    }

    @Override
    public Component getName(ItemStack stack)
    {
        final KnappedHead head = stack.get(TerraVeraDataComponents.KNAPPED_HEAD.get());
        if (head == null) return super.getName(stack);
        // e.g. "Igneous Intrusive Wedge"
        return Component.translatable("terravera.head." + kindPath + ".named",
            Component.translatable("terravera.material." + head.material()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
    {
        final KnappedHead head = stack.get(TerraVeraDataComponents.KNAPPED_HEAD.get());
        if (head != null)
        {
            tooltip.add(Component.translatable("terravera.tooltip.workmanship",
                    Component.translatable("terravera.quality." + head.qualityDescriptor()))
                .withStyle(ChatFormatting.GRAY));
            if (flag.isAdvanced())
            {
                tooltip.add(Component.literal("quality = %.2f".formatted(head.quality())).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        tooltip.add(Component.translatable("terravera.tooltip.needs_lashing").withStyle(ChatFormatting.DARK_GRAY));
    }
}
