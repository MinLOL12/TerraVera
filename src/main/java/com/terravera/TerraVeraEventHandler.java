/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockEvent;

import net.dries007.tfc.common.TFCTags;

import com.terravera.common.TerraVeraDataComponents;
import com.terravera.common.component.Cordage;
import com.terravera.common.items.TerraVeraItems;
import com.terravera.common.recipes.FibreSource;
import com.terravera.config.TerraVeraConfig;

public final class TerraVeraEventHandler
{
    public static void init()
    {
        NeoForge.EVENT_BUS.addListener(TerraVeraEventHandler::onBlockBroken);
    }

    /**
     * Harvesting grass and plants yields plant fibre. This is the entry point of the whole progression - before you
     * can make a single stone tool you have to spend time pulling fibre out of the landscape, which is exactly the
     * kind of low-tech drudgery the stone age should involve.
     */
    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event)
    {
        if (!TerraVeraConfig.SERVER.plantsDropFibre.get()) return;
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) return;

        final Player player = event.getPlayer();
        if (player.isCreative()) return;

        final BlockState state = event.getState();
        final FibreSource source = FibreSource.get(state);
        if (source == null) return;

        final ItemStack tool = player.getMainHandItem();
        final boolean bladed = tool.is(TFCTags.Items.TOOLS_KNIFE) || tool.is(Tags.Items.TOOLS_SHEAR)
            || tool.is(TFCTags.Items.TOOLS_SCYTHE);
        if (source.requiresKnife() && !bladed) return;

        final RandomSource random = level.getRandom();
        double chance = source.chance() * TerraVeraConfig.SERVER.fibreDropChance.get();
        if (bladed) chance += TerraVeraConfig.SERVER.fibreKnifeBonus.get();
        if (random.nextDouble() > chance) return;

        final int amount = source.min() + random.nextInt(Math.max(1, source.max() - source.min() + 1));
        if (amount <= 0) return;

        final ItemStack fibre = new ItemStack(TerraVeraItems.PLANT_FIBER.get(), amount);
        // Carry the plant's fibre quality on the item, so that retting and twisting can preserve it
        fibre.set(TerraVeraDataComponents.CORDAGE.get(), new Cordage(source.strength(), source.source()));

        final BlockPos pos = event.getPos();
        Block.popResource(level, pos, fibre);
    }

    private TerraVeraEventHandler() {}
}
