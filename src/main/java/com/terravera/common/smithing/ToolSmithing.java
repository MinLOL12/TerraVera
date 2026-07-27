/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.smithing;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.component.heat.HeatCapability;
import net.dries007.tfc.common.component.heat.HeatDefinition;
import net.dries007.tfc.common.component.heat.IHeatView;

import com.terravera.TerraVera;

import com.terravera.common.TerraVeraDataComponents;
import com.terravera.common.blocks.TerraVeraBlocks;
import com.terravera.common.component.ToolMetalState;

public final class ToolSmithing
{
    /** Tools above this remaining durability are not worn enough to justify reforging. */
    private static final float MAINTENANCE_THRESHOLD = 0.40f;
    /** Very small workplates are maintenance tooling; TFC anvils get a small efficiency bonus. */
    private static final float ANVIL_REPAIR_MULTIPLIER = 1.25f;
    private static final TagKey<Item> METAL_HAMMERS = TagKey.create(Registries.ITEM, TerraVera.identifier("metal_hammers"));
    private static final TagKey<Item> METAL_RODS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "rods"));
    private static final TagKey<Item> METAL_SHEETS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "sheets"));

    public static boolean isSmithingSurface(BlockState state)
    {
        return state.is(TerraVeraBlocks.WORKPLATE.get()) || state.is(TFCTags.Blocks.ANVILS);
    }

    /**
     * Applies one player-selected smithing operation. The interaction convention is intentionally physical:
     * main hand = metal hammer, offhand = hot tool on the plate/anvil. Sneak-right-click cycles the operation rather
     * than performing it.
     */
    public static InteractionResult useSurface(Level level, Player player, ItemStack hammer, ItemStack tool, BlockState surface)
    {
        if (!isSmithingSurface(surface)) return InteractionResult.PASS;
        if (!isMetalHammer(hammer)) return InteractionResult.PASS;

        if (tool.isEmpty())
        {
            if (!level.isClientSide()) tell(player, Component.translatable("terravera.smithing.need_tool"));
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (!tool.isDamageableItem())
        {
            tell(player, Component.translatable("terravera.smithing.not_damageable"));
            return InteractionResult.CONSUME;
        }

        if (!isMetalTool(tool))
        {
            tell(player, Component.translatable("terravera.smithing.metal_only"));
            return InteractionResult.CONSUME;
        }

        final IHeatView heat = HeatCapability.view(tool);
        final float workingTemperature = workingTemperature(tool, heat);
        final float weldingTemperature = weldingTemperature(tool, heat);
        if (workingTemperature <= 0f)
        {
            tell(player, Component.translatable("terravera.smithing.no_heat_data"));
            return InteractionResult.CONSUME;
        }

        if (player.isShiftKeyDown())
        {
            cycleOperation(player, tool);
            return InteractionResult.CONSUME;
        }

        final boolean hasMetalState = tool.has(TerraVeraDataComponents.TOOL_METAL_STATE.get());
        final ToolMetalState state = tool.getOrDefault(TerraVeraDataComponents.TOOL_METAL_STATE.get(), ToolMetalState.initial(tool));
        final SmithingOperation operation = SmithingOperation.byId(state.operation());

        if (operation == SmithingOperation.FORGE_WELD)
        {
            if (weldingTemperature <= 0f)
            {
                tell(player, Component.translatable("terravera.smithing.no_heat_data"));
                return InteractionResult.CONSUME;
            }
            if (heat == null || !heat.canWeld())
            {
                tell(player, Component.translatable("terravera.smithing.too_cold_weld",
                    Math.round(temperature(heat)), Math.round(weldingTemperature)));
                return InteractionResult.CONSUME;
            }
            final int fluxSlot = findFlux(player);
            if (fluxSlot < 0)
            {
                tell(player, Component.translatable("terravera.smithing.need_flux"));
                return InteractionResult.CONSUME;
            }
            final int stockSlot = findHotWeldingStock(player, tool);
            if (stockSlot < 0)
            {
                tell(player, Component.translatable("terravera.smithing.need_welding_stock"));
                return InteractionResult.CONSUME;
            }
            consumeSlot(player, fluxSlot);
            consumeSlot(player, stockSlot);
            final ToolMetalState after = weld(tool, state, surface.is(TFCTags.Blocks.ANVILS));
            damageHammer(player, hammer);
            tell(player, Component.translatable("terravera.smithing.welded",
                Math.round(after.remainingMassFraction() * 100f))
                .withStyle(ChatFormatting.GOLD));
            return InteractionResult.CONSUME;
        }

        if (heat == null || !heat.canWork())
        {
            tell(player, Component.translatable("terravera.smithing.too_cold_work",
                Math.round(temperature(heat)), Math.round(workingTemperature)));
            return InteractionResult.CONSUME;
        }

        if ((!hasMetalState && !isWornEnough(tool)) || (hasMetalState && tool.getDamageValue() <= repairLimitDamage(tool, state)))
        {
            tell(player, Component.translatable("terravera.smithing.not_worn_enough"));
            return InteractionResult.CONSUME;
        }

        final ToolMetalState after = work(tool, state, operation, surface.is(TFCTags.Blocks.ANVILS));
        damageHammer(player, hammer);

        tell(player, Component.translatable("terravera.smithing.worked",
            operation.displayName(),
            Math.round((1f - (float) tool.getDamageValue() / tool.getMaxDamage()) * 100f),
            Math.round(after.remainingMassFraction() * 100f)));
        return InteractionResult.CONSUME;
    }

    public static void cycleOperation(Player player, ItemStack tool)
    {
        final ToolMetalState state = tool.getOrDefault(TerraVeraDataComponents.TOOL_METAL_STATE.get(), ToolMetalState.initial(tool));
        final SmithingOperation next = SmithingOperation.byId(state.operation()).next();
        tool.set(TerraVeraDataComponents.TOOL_METAL_STATE.get(), state.withOperation(next.id()));
        tell(player, Component.translatable("terravera.smithing.selected", next.displayName(), next.description())
            .withStyle(ChatFormatting.AQUA));
    }

    public static boolean isMetalHammer(ItemStack stack)
    {
        return stack.is(METAL_HAMMERS);
    }

    public static boolean isMetalTool(ItemStack stack)
    {
        return (HeatCapability.view(stack) != null || HeatCapability.getDefinition(stack) != null) && !stack.is(TFCTags.Items.TOOLS_STONE);
    }

    public static int repairLimitDamage(ItemStack tool, ToolMetalState state)
    {
        final int maxDamage = tool.getMaxDamage();
        final int maximumUsableDurability = Math.max(1, Math.round(maxDamage * state.remainingMassFraction()));
        return Math.max(0, maxDamage - maximumUsableDurability);
    }

    private static float workingTemperature(ItemStack stack, IHeatView heat)
    {
        if (heat != null) return heat.getWorkingTemperature();
        final HeatDefinition definition = HeatCapability.getDefinition(stack);
        return definition != null ? definition.forgingTemperature() : 0f;
    }

    private static float weldingTemperature(ItemStack stack, IHeatView heat)
    {
        if (heat != null) return heat.getWeldingTemperature();
        final HeatDefinition definition = HeatCapability.getDefinition(stack);
        return definition != null ? definition.weldingTemperature() : 0f;
    }

    private static float temperature(IHeatView heat)
    {
        return heat != null ? heat.getTemperature() : 0f;
    }

    private static ToolMetalState work(ItemStack tool, ToolMetalState state, SmithingOperation operation, boolean anvil)
    {
        final int maxDamage = tool.getMaxDamage();
        final int repair = Math.max(1, Math.round(maxDamage * operation.repairPercent() / 100f * (anvil ? ANVIL_REPAIR_MULTIPLIER : 1f)));

        final int bendCorrection = operation == SmithingOperation.STRAIGHTENING
            ? -Integer.signum(state.bend()) * Math.min(2, Math.abs(state.bend()))
            : operation.bendChange();

        final ToolMetalState after = state.worked(operation.id(), operation.massLoss(), operation.lengthChange(),
            operation.widthChange(), operation.thicknessChange(), bendCorrection, operation.edgeChange(), operation.strainChange());
        tool.set(TerraVeraDataComponents.TOOL_METAL_STATE.get(), after);

        final int limitDamage = repairLimitDamage(tool, after);
        tool.setDamageValue(Math.max(limitDamage, tool.getDamageValue() - repair));
        return after;
    }

    private static ToolMetalState weld(ItemStack tool, ToolMetalState state, boolean anvil)
    {
        final ToolMetalState after = state.welded(anvil ? 5f : 3f);
        tool.set(TerraVeraDataComponents.TOOL_METAL_STATE.get(), after);
        final int repair = Math.max(1, Math.round(tool.getMaxDamage() * (anvil ? 0.18f : 0.12f)));
        tool.setDamageValue(Math.max(repairLimitDamage(tool, after), tool.getDamageValue() - repair));
        return after;
    }

    private static boolean isWornEnough(ItemStack tool)
    {
        final int max = tool.getMaxDamage();
        if (max <= 0) return false;
        final float remaining = 1f - (float) tool.getDamageValue() / max;
        return remaining <= MAINTENANCE_THRESHOLD;
    }

    private static int findFlux(Player player)
    {
        if (player.isCreative()) return 0;
        final Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++)
        {
            if (inventory.getItem(i).is(TFCTags.Items.WELDING_FLUX)) return i;
        }
        return -1;
    }

    private static int findHotWeldingStock(Player player, ItemStack tool)
    {
        if (player.isCreative()) return 0;
        final Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++)
        {
            final ItemStack stack = inventory.getItem(i);
            if ((stack.is(METAL_RODS) || stack.is(METAL_SHEETS)) && isWeldingHot(stack) && sameMetalMaterial(tool, stack)) return i;
        }
        return -1;
    }

    private static boolean isWeldingHot(ItemStack stack)
    {
        final IHeatView heat = HeatCapability.view(stack);
        return heat != null && heat.canWeld();
    }

    private static boolean sameMetalMaterial(ItemStack tool, ItemStack stock)
    {
        final String toolMetal = metalMaterial(tool);
        final String stockMetal = metalMaterial(stock);
        return toolMetal.isEmpty() || stockMetal.isEmpty() || toolMetal.equals(stockMetal);
    }

    private static String metalMaterial(ItemStack stack)
    {
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        final String path = id.getPath();
        final int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : "";
    }

    private static void consumeSlot(Player player, int slot)
    {
        if (player.isCreative()) return;
        player.getInventory().getItem(slot).shrink(1);
    }

    private static void damageHammer(Player player, ItemStack hammer)
    {
        if (player.isCreative() || !hammer.isDamageableItem()) return;
        final int next = hammer.getDamageValue() + 1;
        if (next >= hammer.getMaxDamage())
        {
            hammer.shrink(1);
        }
        else
        {
            hammer.setDamageValue(next);
        }
    }

    private static void tell(Player player, Component message)
    {
        player.displayClientMessage(message, true);
    }

    private ToolSmithing() {}
}
