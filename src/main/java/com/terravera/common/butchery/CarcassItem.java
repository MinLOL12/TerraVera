/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.butchery;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.terravera.common.TerraVeraDataComponents;

/**
 * A whole animal carcass, carried and worked as an item.
 * <p>
 * It is an item rather than a block or an entity on purpose. A carcass has to be portable - the entire freshness
 * mechanic is about getting it somewhere you can work before it turns - and making it a block would mean a player
 * who killed a deer on a mountainside had to butcher it there or lose it. As an item it stacks to one, carries its
 * own history in a data component, and can be hung on any surface the player likes without new block registration.
 * <p>
 * Right-clicking with a carcass in one hand and a blade in the other performs the next stage of butchering.
 */
public class CarcassItem extends Item
{
    public CarcassItem(Properties properties)
    {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        final ItemStack carcass = player.getItemInHand(hand);
        final InteractionHand other = hand == InteractionHand.MAIN_HAND
            ? InteractionHand.OFF_HAND
            : InteractionHand.MAIN_HAND;

        if (level.isClientSide())
        {
            return InteractionResultHolder.sidedSuccess(carcass, true);
        }

        final boolean worked = ButcherySystem.butcher(player, carcass, player.getItemInHand(other), other);
        return worked
            ? InteractionResultHolder.consume(carcass)
            : InteractionResultHolder.fail(carcass);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
    {
        final CarcassData data = stack.get(TerraVeraDataComponents.CARCASS.get());
        if (data == null) return;

        tooltip.add(data.species().displayName().copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("terravera.butchery.tooltip.stage", data.stage().displayName())
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("terravera.butchery.tooltip.rack_hint")
            .withStyle(ChatFormatting.DARK_GREEN));

        // Freshness is shown as the band the carcass would be in at a temperate 15 C. The real band is resolved
        // server-side against the actual climate; showing an estimate here keeps the tooltip honest without
        // pretending the client knows the local temperature.
        final Level level = context.level();
        if (level != null)
        {
            final Freshness freshness = data.freshness(level.getGameTime(), 15f);
            tooltip.add(Component.translatable("terravera.butchery.tooltip.freshness", freshness.displayName())
                .withStyle(freshness.edible() ? ChatFormatting.GREEN : ChatFormatting.RED));
        }

        if (data.waste() > 0.02f)
        {
            tooltip.add(Component.translatable("terravera.butchery.tooltip.waste", Math.round(data.waste() * 100f))
                .withStyle(ChatFormatting.DARK_RED));
        }
    }
}
