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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import net.dries007.tfc.common.fluids.FluidHelpers;

import com.terravera.common.health.WaterTreatment;

/**
 * Any item that treats a container of water in the other hand: chlorine tablets, iodine drops, boiling stones,
 * a cloth filter, a ceramic filter candle, and so on.
 * <p>
 * The treatment ladder on {@link WaterTreatment.Treatment} decides whether using one of these improves the water at
 * all - treating already-boiled water with iodine is a waste, and the item says so instead of silently downgrading.
 * Each item is configured with the treatment it applies and a durability that is literally its use count: a cloth
 * filter is good for one container, boiling stones for a whole trip.
 */
public class TreatmentItem extends Item
{
    private final WaterTreatment.Treatment target;
    private final SoundEvent sound;

    /**
     * @param properties item properties; give {@code durability(n)} for {@code n} uses, or {@code durability(1)} for
     *                   a single-use item
     * @param target     the treatment this item applies
     * @param sound      sound played when a treatment succeeds
     */
    public TreatmentItem(Properties properties, WaterTreatment.Treatment target, SoundEvent sound)
    {
        super(properties);
        this.target = target;
        this.sound = sound;
    }

    public WaterTreatment.Treatment target()
    {
        return target;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand)
    {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        final ItemStack treatment = player.getItemInHand(hand);
        final InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        final ItemStack container = player.getItemInHand(other);

        if (container.isEmpty() || FluidHelpers.getContainedFluid(container).isEmpty())
        {
            if (!level.isClientSide())
            {
                player.displayClientMessage(
                    Component.translatable("terravera.water.treatment_needs_container").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.pass(treatment);
        }

        final WaterTreatment current = WaterTreatment.get(container);
        if (current.treatment().rank() >= target.rank())
        {
            if (!level.isClientSide())
            {
                player.displayClientMessage(
                    Component.translatable("terravera.water.already_better", current.describeTreatment())
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.pass(treatment);
        }

        if (!level.isClientSide())
        {
            WaterTreatment.set(container, new WaterTreatment(target, current.sourceContamination()));
            player.displayClientMessage(
                Component.translatable("terravera.water.treated_with", Component.translatable(target.translationKey()))
                    .withStyle(ChatFormatting.GREEN), true);
            level.playSound(null, player.blockPosition(), sound, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.1f);
            treatment.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            return InteractionResultHolder.success(treatment);
        }
        return InteractionResultHolder.success(treatment);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
    {
        tooltip.add(Component.translatable("terravera.tooltip.treatment_item", Component.translatable(target.translationKey()))
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("terravera.tooltip.treatment_how").withStyle(ChatFormatting.DARK_GRAY));
        if (stack.isDamageableItem())
        {
            tooltip.add(Component.translatable("terravera.tooltip.treatment_uses", stack.getMaxDamage())
                .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public boolean isEnchantable(ItemStack stack)
    {
        return false;
    }
}
