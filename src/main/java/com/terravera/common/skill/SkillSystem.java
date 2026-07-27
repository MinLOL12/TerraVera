/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.skill;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import com.terravera.common.TerraVeraAttachments;
import com.terravera.common.TerraVeraDataComponents;
import com.terravera.common.component.ToolGrip;

/**
 * The small set of hooks where practical knowledge affects play. Bonuses stay conservative: they make a practiced
 * player more reliable and more observant, but never turn a field into a generic damage or health level.
 */
public final class SkillSystem
{
    public static void init()
    {
        NeoForge.EVENT_BUS.register(SkillSystem.class);
    }

    public static PlayerSkills get(Player player)
    {
        return player.getData(TerraVeraAttachments.PLAYER_SKILLS);
    }

    public static void set(Player player, PlayerSkills skills)
    {
        player.setData(TerraVeraAttachments.PLAYER_SKILLS, skills);
    }

    public static void award(Player player, SkillType skill, float amount)
    {
        if (player.level().isClientSide() || player.isCreative() || amount <= 0f) return;
        set(player, get(player).learned(skill, amount));
    }

    public static float proficiency(Player player, SkillType skill)
    {
        return get(player).proficiency(skill);
    }

    /** Mining practice comes mostly from mineral-bearing stone; ordinary digging still teaches a little. */
    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event)
    {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) return;
        final Player player = event.getPlayer();
        if (player == null) return;

        final BlockState state = event.getState();
        if (isOre(state)) award(player, SkillType.MINING, 1.5f);
        else if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) award(player, SkillType.MINING, 0.18f);
    }

    /**
     * A practiced miner reads a vein and swings efficiently. A fitted grip only helps tools that actually have one;
     * the two modifiers multiply rather than turning either into a universal haste effect.
     */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event)
    {
        final Player player = event.getEntity();
        final ItemStack tool = player.getMainHandItem();
        final ToolGrip grip = tool.get(TerraVeraDataComponents.TOOL_GRIP.get());
        // The item component is synced and can be shown client-side. Player knowledge remains authoritative on the
        // server; keeping the client at the baseline avoids relying on unsynchronised attachment state.
        final float miningEfficiency = player.level().isClientSide()
            ? 1f
            : 1f + proficiency(player, SkillType.MINING) * 0.16f;
        final float gripEfficiency = grip != null ? grip.speedMultiplier() : 1f;
        event.setNewSpeed(event.getNewSpeed() * miningEfficiency * gripEfficiency);
    }

    /** Cooking is learned from completing food recipes, rather than passively from a combat or mining action. */
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event)
    {
        final ItemStack result = event.getCrafting();
        if (result.has(net.minecraft.core.component.DataComponents.FOOD))
        {
            award(event.getEntity(), SkillType.COOKING, 0.8f);
        }
    }

    /**
     * Ore labels become more specific as the player learns to recognise mineral-bearing rock. This is information,
     * not an x-ray: the player still has to expose and inspect the block/item themselves.
     */
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event)
    {
        final Player player = event.getEntity();
        if (player == null || !isOre(event.getItemStack())) return;

        final PlayerSkills skills = get(player);
        final float familiarity = skills.proficiency(SkillType.MINING);
        final Component insight = familiarity < 0.12f
            ? Component.translatable("terravera.skill.ore.untrained")
            : familiarity < 0.45f
                ? Component.translatable("terravera.skill.ore.familiar")
                : Component.translatable("terravera.skill.ore.practiced");
        event.getToolTip().add(insight.withStyle(ChatFormatting.DARK_AQUA));
    }

    public static boolean isOre(BlockState state)
    {
        return isOre(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    private static boolean isOre(ItemStack stack)
    {
        return isOre(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static boolean isOre(ResourceLocation id)
    {
        final String path = id.getPath();
        // TFC calls many exposed ores "ore/..." or "deposit/..."; vanilla uses *_ore. Keeping this id based makes
        // the recognition system work for both without pretending every stone is an ore.
        return path.contains("ore") || path.contains("native_") || path.contains("deposit/");
    }

    private SkillSystem() {}
}
