/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.container.ItemStackContainerProvider;
import net.dries007.tfc.util.data.KnappingType;

import com.terravera.common.TerraVeraDataComponents;
import com.terravera.common.component.Cordage;
import com.terravera.common.component.ToolMetalState;
import com.terravera.common.container.ShapingContainer;
import com.terravera.common.knapping.KnappableStone;
import com.terravera.common.recipes.FibreSource;
import com.terravera.common.smithing.SmithingOperation;
import com.terravera.common.smithing.ToolSmithing;
import com.terravera.config.TerraVeraConfig;

public final class TerraVeraEventHandler
{
    public static void init()
    {
        NeoForge.EVENT_BUS.addListener(TerraVeraEventHandler::onBlockBroken);
        NeoForge.EVENT_BUS.addListener(TerraVeraEventHandler::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(TerraVeraEventHandler::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(TerraVeraEventHandler::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(TerraVeraEventHandler::onItemTooltip);
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

        final ItemStack fibre = new ItemStack(com.terravera.common.items.TerraVeraItems.PLANT_FIBER.get(), amount);
        // Carry the plant's fibre quality on the item, so that retting and twisting can preserve it
        // Use short length since raw plant fiber is short strands
        fibre.set(TerraVeraDataComponents.CORDAGE.get(), new Cordage(source.strength(), source.source(), 150));

        final BlockPos pos = event.getPos();
        Block.popResource(level, pos, fibre);
    }

    /**
     * Pulling a grass plant out by hand is deliberately slower than cutting it. This is kept in the break-speed event
     * rather than replacing the break interaction, so the normal block-breaking flow still handles drops, protection
     * checks, and the fibre roll in {@link #onBlockBroken(BlockEvent.BreakEvent)}.
     */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event)
    {
        if (!TerraVeraConfig.SERVER.plantsDropFibre.get()) return;

        final Player player = event.getEntity();
        if (!player.getMainHandItem().isEmpty()) return;

        final FibreSource source = FibreSource.get(event.getState());
        if (source == null || !"grass".equals(source.source())) return;

        event.setNewSpeed(event.getNewSpeed() * TerraVeraConfig.SERVER.handGatheringSpeed.get().floatValue());
    }

    /**
     * Right clicking air while holding knappable stone opens TerraVera's shaping screen.
     * Uses TFC's native knapping GUI components (buttons, textures, sounds) but with
     * TerraVera's function-based knapping analysis.
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event)
    {
        final Player player = event.getEntity();
        final ItemStack stack = event.getItemStack();
        final KnappableStone stone = KnappableStone.get(stack);
        if (stone == null || !stone.matches(stack)) return;

        if (player instanceof ServerPlayer serverPlayer)
        {
            // Get the TFC KnappingType for this stone
            final KnappingType knappingType = KnappingType.get(stack);
            if (knappingType == null) return;
            
            final InteractionHand hand = event.getHand();
            final int slot = hand == InteractionHand.MAIN_HAND ? serverPlayer.getInventory().selected : 40;
            // Open TerraVera's shaping container (our own menu type)
            new ItemStackContainerProvider(
                (target, usedHand, usedSlot, inventory, windowId) ->
                    ShapingContainer.create(target, knappingType, stone, usedHand, usedSlot, inventory, windowId),
                Component.translatable("tfc.screen.knapping")
            ).openScreen(serverPlayer, hand, buffer -> {
                buffer.writeResourceLocation(KnappingType.MANAGER.getIdOrThrow(knappingType));
                buffer.writeResourceLocation(com.terravera.common.knapping.KnappableStone.MANAGER.getIdOrThrow(stone));
                buffer.writeEnum(hand);
                buffer.writeVarInt(slot);
            });
        }
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
    }

    /**
     * Workplate/anvil quick-strike: if the player is holding a metal hammer in the main hand AND a tool in the off
     * hand, a single right-click performs the currently selected smithing operation without opening the GUI. This is
     * the power-user shortcut; the full GUI opens for every other right-click on the workplate.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
    {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        final Level level = event.getLevel();
        final BlockState state = level.getBlockState(event.getPos());
        if (!ToolSmithing.isSmithingSurface(state)) return;

        final Player player = event.getEntity();
        final ItemStack hammer = player.getMainHandItem();
        final ItemStack tool = player.getOffhandItem();
        if (!ToolSmithing.isMetalHammer(hammer)) return;

        // Only intercept for quick-strike when both a hammer and a tool are in hand.
        // With a hammer but no tool (or any other combination), fall through to the block's
        // useWithoutItem, which opens the GUI.
        if (tool.isEmpty()) return;

        final net.minecraft.world.InteractionResult result = ToolSmithing.useSurface(level, player, hammer, tool, state);
        if (result != net.minecraft.world.InteractionResult.PASS)
        {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide()));
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event)
    {
        final ToolMetalState state = event.getItemStack().get(TerraVeraDataComponents.TOOL_METAL_STATE.get());
        if (state == null) return;

        event.getToolTip().add(Component.translatable("terravera.tooltip.metal_mass",
            Math.round(state.remainingMassFraction() * 100f)));
        event.getToolTip().add(Component.translatable("terravera.tooltip.smithing_operation",
            SmithingOperation.byId(state.operation()).displayName()));
        event.getToolTip().add(Component.translatable("terravera.tooltip.smithing_shape",
            state.length(), state.width(), state.thickness(), state.bend(), state.edge(), state.strain()));
    }

    private TerraVeraEventHandler() {}
}
