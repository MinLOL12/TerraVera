package com.terravera.common.items;

import com.terravera.common.TerraVeraDataComponents;
import com.terravera.common.component.Adhesive;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A small prepared batch of glue. Its values remain visible rather than hiding five glues behind one generic item. */
public final class AdhesiveItem extends Item
{
    private final Adhesive properties;

    public AdhesiveItem(Adhesive properties, Properties itemProperties)
    {
        // A default data component, rather than a tooltip-only field, means recipes and dropped stacks carry it too.
        super(itemProperties.stacksTo(16).component(TerraVeraDataComponents.ADHESIVE.get(), properties));
        this.properties = properties;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
    {
        Adhesive glue = stack.getOrDefault(TerraVeraDataComponents.ADHESIVE.get(), properties);
        tooltip.add(Component.translatable("tooltip.terravera.adhesive.stats", percent(glue.strength()), percent(glue.moistureResistance()), percent(glue.flexibility())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.terravera.adhesive.use." + glue.application()).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String percent(float value) { return Math.round(value * 100) + "%"; }
}
