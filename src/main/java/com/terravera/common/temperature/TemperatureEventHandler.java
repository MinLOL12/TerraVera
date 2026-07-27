/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.temperature;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import com.terravera.TerraVera;
import com.terravera.common.TerraVeraDataComponents;
import com.terravera.config.TerraVeraConfig;

/**
 * Where the body temperature system meets the rest of the game.
 * <p>
 * The tick is the bulk of it; the rest are the moments where temperature should visibly matter:
 * <ul>
 *     <li><strong>Sleeping</strong> - the single most dangerous thing a cold player can do is lie down in the open.
 *     Sleep is not blocked, but the player is warned, and their body keeps cooling while they are unconscious.</li>
 *     <li><strong>Tooltips</strong> - a garment says what it is made of and whether it is wet.</li>
 *     <li><strong>Death and respawn</strong> - the cache is cleared and the body starts fresh.</li>
 * </ul>
 */
public final class TemperatureEventHandler
{
    public static void init()
    {
        NeoForge.EVENT_BUS.register(TemperatureEventHandler.class);
        TerraVera.LOGGER.info("TerraVera body temperature system registered");
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event)
    {
        TemperatureSystem.tick(event.getEntity());
    }

    /**
     * Sleeping outdoors in the cold.
     * <p>
     * The player is never stopped from sleeping - being refused with no explanation is the worst possible version of
     * this mechanic. Instead they are told, before they commit, that this is a bad place to spend the night, and if
     * they do it anyway their core temperature keeps falling while they sleep. Bedding and a fire are the fix, and
     * the message says so.
     */
    @SubscribeEvent
    public static void onSleep(CanPlayerSleepEvent event)
    {
        if (!TerraVeraConfig.SERVER.enableBodyTemperature.get()) return;

        final Player player = event.getEntity();
        final Level level = player.level();
        if (level.isClientSide()) return;

        final BlockPos pos = event.getPos();
        final Shelter shelter = Shelter.survey(level, pos.above());
        final float ambient = TemperatureSystem.ambientTemperature(level, pos);
        final float bedding = beddingWarmth(level, pos);

        // Roughly: how cold will it be by dawn, given where you have chosen to lie down.
        final float nightPenalty = 6f;
        final float effective = shelter.isIndoors()
            ? shelter.moderate(ambient - nightPenalty, TemperatureSystem.averageTemperature(level, pos))
            : ambient - nightPenalty + shelter.openFireWarmth();

        if (effective + bedding * 8f < 4f)
        {
            player.displayClientMessage(Component.translatable("terravera.temperature.sleep.dangerous")
                .withStyle(ChatFormatting.RED), false);
        }
        else if (effective + bedding * 8f < 12f)
        {
            player.displayClientMessage(Component.translatable("terravera.temperature.sleep.chilly")
                .withStyle(ChatFormatting.GOLD), false);
        }
    }

    /**
     * How much the thing you are lying on is worth. Wool bedding over a raised floor is the difference between a
     * rough night and a dangerous one; bare stone or dirt is worse than useless.
     */
    private static float beddingWarmth(Level level, BlockPos pos)
    {
        final BlockState bed = level.getBlockState(pos);
        float warmth = bed.is(BlockTags.BEDS) ? 0.6f : 0.1f;

        final BlockState under = level.getBlockState(pos.below());
        if (under.is(BlockTags.WOOL) || under.is(BlockTags.WOOL_CARPETS)) warmth += 0.4f;
        else if (under.is(BlockTags.PLANKS) || under.is(BlockTags.WOODEN_SLABS)) warmth += 0.2f;
        else if (under.is(BlockTags.DIRT) || under.is(BlockTags.BASE_STONE_OVERWORLD)) warmth -= 0.25f;

        return Mth.clamp(warmth, -0.5f, 1f);
    }

    /**
     * Wet garments say so on their tooltip, whatever they are. This includes armour from other mods that has been
     * rained on, because the wetness component is applied by material, not by item identity.
     */
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event)
    {
        final ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof ClothingItem) return; // it prints its own, richer tooltip

        final Wetness wetness = stack.get(TerraVeraDataComponents.GARMENT_WETNESS.get());
        if (wetness != null && !wetness.isDry())
        {
            event.getToolTip().add(Component.translatable(wetness.descriptorKey()).withStyle(ChatFormatting.BLUE));
        }
    }

    /** A new body is a new body: drop the cached shelter survey and start from a normal temperature. */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event)
    {
        final Player player = event.getEntity();
        TemperatureSystem.forget(player);
        TemperatureSystem.set(player, TemperatureSystem.get(player).onDeath());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        TemperatureSystem.forget(event.getEntity());
    }

    private TemperatureEventHandler() {}
}
