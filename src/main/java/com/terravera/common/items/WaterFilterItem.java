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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import com.terravera.common.health.BoilingHandler;

/**
 * A sand-and-charcoal filter. Used on a container of water in the other hand.
 * <p>
 * Filtration is the portable, fire-free half of water treatment. It handles the protozoa - Giardia and
 * Cryptosporidium, the two parasites that make a long journey miserable - and strips most of the load out of even
 * fairly bad water, but it does not touch the bacteria. Typhoid and cholera go straight through it.
 * <p>
 * That limitation is the design. If filtration were a full solution it would obsolete boiling the moment it was
 * crafted; as it stands the player carries a filter on the road and boils in camp, and both remain worth having for
 * the whole game.
 */
public class WaterFilterItem extends Item
{
    public WaterFilterItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, net.minecraft.world.entity.LivingEntity target, InteractionHand hand)
    {
        return InteractionResult.PASS;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, Player player, InteractionHand hand)
    {
        final ItemStack filter = player.getItemInHand(hand);
        final InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        final ItemStack container = player.getItemInHand(other);

        if (container.isEmpty())
        {
            if (!level.isClientSide())
            {
                player.displayClientMessage(
                    Component.translatable("terravera.water.filter_needs_container").withStyle(ChatFormatting.GRAY), true);
            }
            return net.minecraft.world.InteractionResultHolder.pass(filter);
        }

        if (!level.isClientSide() && BoilingHandler.filter(player, container))
        {
            filter.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            return net.minecraft.world.InteractionResultHolder.success(filter);
        }
        return net.minecraft.world.InteractionResultHolder.pass(filter);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
    {
        tooltip.add(Component.translatable("terravera.tooltip.water_filter").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("terravera.tooltip.water_filter.limit").withStyle(ChatFormatting.DARK_GRAY));
    }
}
