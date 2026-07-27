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
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.terravera.common.skill.PlayerSkills;
import com.terravera.common.skill.SkillSystem;
import com.terravera.common.skill.SkillType;

/** A compact, always-available view of the player's learned knowledge. */
public class FieldNotesItem extends Item
{
    public FieldNotesItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        final ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide())
        {
            final PlayerSkills skills = SkillSystem.get(player);
            player.sendSystemMessage(Component.translatable("terravera.skill.notes.title").withStyle(ChatFormatting.GOLD));
            for (SkillType type : SkillType.values())
            {
                player.sendSystemMessage(Component.translatable("terravera.skill.notes.line",
                    type.displayName(), Component.translatable(skills.knowledgeKey(type)), Math.round(skills.experience(type)))
                    .withStyle(ChatFormatting.GRAY));
            }

            // The one place a player can get a considered read on their thermal situation instead of a symptom.
            // Even here it is in words: what their clothes are worth, and what the building is doing.
            if (com.terravera.config.TerraVeraConfig.SERVER.enableBodyTemperature.get())
            {
                player.sendSystemMessage(Component.translatable("terravera.temperature.notes.title")
                    .withStyle(ChatFormatting.GOLD));
                com.terravera.common.temperature.TemperatureSystem.describe(player)
                    .forEach(player::sendSystemMessage);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
    {
        tooltip.add(Component.translatable("terravera.tooltip.field_notes").withStyle(ChatFormatting.GRAY));
    }
}
