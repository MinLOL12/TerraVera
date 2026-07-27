/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-12
 */

package com.terravera.common.container;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.component.heat.HeatCapability;
import net.dries007.tfc.common.component.heat.IHeatView;

import com.terravera.common.TerraVeraDataComponents;
import com.terravera.common.blocks.TerraVeraBlocks;
import com.terravera.common.component.ToolMetalState;
import com.terravera.common.smithing.SmithingOperation;
import com.terravera.common.smithing.ToolSmithing;

/**
 * Server-side container for the Workplate GUI.
 * <p>
 * Three workplate slots sit beside the standard player inventory:
 * <ul>
 *   <li><strong>Slot 0 (Hammer)</strong> — accepts items in the {@code terravera:metal_hammers} tag.</li>
 *   <li><strong>Slot 1 (Tool)</strong> — accepts damageable metal tools with TFC heat data.</li>
 *   <li><strong>Slot 2 (Flux)</strong> — accepts welding flux; only consumed by the forge-weld operation.</li>
 * </ul>
 * <p>
 * The selected operation is synced to the client via a {@link DataSlot}, so the screen can highlight the active
 * button without having to re-read the tool's data component every frame.
 */
public class WorkplateContainer extends AbstractContainerMenu
{
    public static final int SLOT_HAMMER = 0;
    public static final int SLOT_TOOL = 1;
    public static final int SLOT_FLUX = 2;
    private static final int WORKPLATE_SLOTS = 3;

    /** Button ids for operation selection: 0–6 map to {@link SmithingOperation#ordinal()}. */
    public static final int BUTTON_STRIKE = 7;

    private final ContainerLevelAccess access;
    private final BlockPos blockPos;
    private final ItemStackHandler workplateSlots;

    /** Synced: the ordinal of the currently selected smithing operation. */
    private final DataSlot selectedOperation = DataSlot.standalone();

    /**
     * Server-side constructor: the player has right-clicked a placed workplate at {@code pos}.
     */
    public WorkplateContainer(int windowId, Inventory playerInventory, BlockPos pos)
    {
        super(TerraVeraContainers.WORKPLATE.get(), windowId);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.blockPos = pos;
        this.workplateSlots = createWorkplateSlots();

        addWorkplateSlots();
        addPlayerSlots(playerInventory);
        addDataSlot(selectedOperation);
    }

    /**
     * Client-side constructor, called from the network data that {@code ServerPlayer.openMenu} sends.
     */
    public static WorkplateContainer fromNetwork(int windowId, Inventory inventory, RegistryFriendlyByteBuf buf)
    {
        return new WorkplateContainer(windowId, inventory, buf.readBlockPos());
    }

    public BlockPos blockPos()
    {
        return blockPos;
    }

    public ItemStack getHammer()
    {
        return workplateSlots.getStackInSlot(SLOT_HAMMER);
    }

    public ItemStack getTool()
    {
        return workplateSlots.getStackInSlot(SLOT_TOOL);
    }

    public ItemStack getFlux()
    {
        return workplateSlots.getStackInSlot(SLOT_FLUX);
    }

    public int selectedOperationOrdinal()
    {
        return selectedOperation.get();
    }

    /**
     * Returns the tool's current heat view, or {@code null} if the tool has no heat data or is not in the slot.
     */
    @Nullable
    public IHeatView toolHeat()
    {
        return HeatCapability.view(getTool());
    }

    /**
     * Returns the tool's {@link ToolMetalState}, defaulting to a fresh initial state if none is stored yet.
     */
    public ToolMetalState toolMetalState()
    {
        final ItemStack tool = getTool();
        if (tool.isEmpty()) return ToolMetalState.initial(tool);
        return tool.getOrDefault(TerraVeraDataComponents.TOOL_METAL_STATE.get(), ToolMetalState.initial(tool));
    }

    // ───────────────────────────────── button handling ─────────────────────────────────

    /**
     * Called on the server when the client screen sends a button click.
     * <p>
     * Button ids 0–6 select an operation; button id 7 performs a strike with the currently selected operation.
     */
    @Override
    public boolean clickMenuButton(Player player, int id)
    {
        if (id >= 0 && id < SmithingOperation.values().length)
        {
            selectOperation(player, SmithingOperation.values()[id]);
            return true;
        }
        if (id == BUTTON_STRIKE)
        {
            performStrike(player);
            return true;
        }
        return false;
    }

    private void selectOperation(Player player, SmithingOperation operation)
    {
        final ItemStack tool = getTool();
        if (tool.isEmpty()) return;

        final ToolMetalState state = tool.getOrDefault(TerraVeraDataComponents.TOOL_METAL_STATE.get(), ToolMetalState.initial(tool));
        tool.set(TerraVeraDataComponents.TOOL_METAL_STATE.get(), state.withOperation(operation.id()));
        selectedOperation.set(operation.ordinal());

        player.displayClientMessage(
            Component.translatable("terravera.smithing.selected", operation.displayName(), operation.description()),
            true);
    }

    private void performStrike(Player player)
    {
        final ItemStack hammer = getHammer();
        final ItemStack tool = getTool();
        final net.minecraft.world.level.block.state.BlockState surface = player.level().getBlockState(blockPos);

        if (hammer.isEmpty() || !ToolSmithing.isMetalHammer(hammer))
        {
            player.displayClientMessage(Component.translatable("terravera.workplate.gui.need_hammer"), true);
            return;
        }
        if (tool.isEmpty())
        {
            player.displayClientMessage(Component.translatable("terravera.smithing.need_tool"), true);
            return;
        }

        final SmithingOperation operation = SmithingOperation.byId(toolMetalState().operation());

        // Forge welding has special input requirements (flux + hot stock) that the existing
        // ToolSmithing logic already validates against the player's inventory. We temporarily
        // move flux into a reachable inventory slot so the existing code can find it.
        final ItemStack fluxStack = getFlux();
        final boolean hasFluxInSlot = !fluxStack.isEmpty() && fluxStack.is(TFCTags.Items.WELDING_FLUX);

        // Delegate to the existing battle-tested smithing logic. It handles all validation,
        // heat checks, mass loss, durability repair, and chat feedback.
        if (operation == SmithingOperation.FORGE_WELD && hasFluxInSlot)
        {
            // ToolSmithing deliberately owns all welding validation/consumption. Lend it exactly ONE flux item,
            // not the whole stack, and return that same item in a finally-style path if the strike is rejected. The
            // old implementation moved the full stack through a transient inventory slot; a failed repair/close could
            // leave items only in that temporary handler and make them appear to vanish.
            final int freeSlot = findFreeInventorySlot(player);
            if (freeSlot < 0)
            {
                player.displayClientMessage(Component.translatable("terravera.workplate.gui.inventory_full"), true);
                return;
            }

            final ItemStack oneFlux = workplateSlots.extractItem(SLOT_FLUX, 1, false);
            player.getInventory().setItem(freeSlot, oneFlux);
            try
            {
                ToolSmithing.useSurface(player.level(), player, hammer, tool, surface);
            }
            finally
            {
                final ItemStack unconsumed = player.getInventory().getItem(freeSlot);
                player.getInventory().setItem(freeSlot, ItemStack.EMPTY);
                if (!unconsumed.isEmpty())
                {
                    final ItemStack remainder = workplateSlots.insertItem(SLOT_FLUX, unconsumed, false);
                    if (!remainder.isEmpty()) player.getInventory().placeItemBackInInventory(remainder);
                }
            }
        }
        else
        {
            ToolSmithing.useSurface(player.level(), player, hammer, tool, surface);
        }

        // Sync the operation ordinal after the strike (the tool's component may have changed).
        if (!tool.isEmpty())
        {
            final ToolMetalState after = tool.getOrDefault(TerraVeraDataComponents.TOOL_METAL_STATE.get(), ToolMetalState.initial(tool));
            selectedOperation.set(SmithingOperation.byId(after.operation()).ordinal());
        }
    }

    private int findFreeInventorySlot(Player player)
    {
        final Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++)
        {
            if (inv.getItem(i).isEmpty()) return i;
        }
        return -1;
    }

    // ───────────────────────────────── slot setup ─────────────────────────────────

    private ItemStackHandler createWorkplateSlots()
    {
        return new ItemStackHandler(WORKPLATE_SLOTS)
        {
            @Override
            public boolean isItemValid(int slot, ItemStack stack)
            {
                return switch (slot)
                {
                    case SLOT_HAMMER -> ToolSmithing.isMetalHammer(stack);
                    case SLOT_TOOL -> stack.isDamageableItem() && ToolSmithing.isMetalTool(stack);
                    case SLOT_FLUX -> stack.is(TFCTags.Items.WELDING_FLUX);
                    default -> false;
                };
            }

            @Override
            public int getSlotLimit(int slot)
            {
                return slot == SLOT_FLUX ? 16 : 1;
            }
        };
    }

    private void addWorkplateSlots()
    {
        addSlot(new SlotItemHandler(workplateSlots, SLOT_HAMMER, 26, 25));
        addSlot(new SlotItemHandler(workplateSlots, SLOT_TOOL, 26, 50));
        addSlot(new SlotItemHandler(workplateSlots, SLOT_FLUX, 26, 75));
    }

    private void addPlayerSlots(Inventory playerInventory)
    {
        // Main inventory (3 rows of 9)
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 12 + col * 18, 158 + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++)
        {
            addSlot(new Slot(playerInventory, col, 12 + col * 18, 216));
        }
    }

    // ───────────────────────────────── standard overrides ─────────────────────────────────

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(access, player, TerraVeraBlocks.WORKPLATE.get());
    }

    /**
     * Workplate slots are intentionally ephemeral, rather than an inventory stored in the block. Always hand their
     * contents back when the menu closes (including range-close, death, disconnect, and a rejected repair) so a tool,
     * hammer, or flux stack can never be stranded in the container's private ItemStackHandler.
     */
    @Override
    public void removed(Player player)
    {
        super.removed(player);
        if (player.level().isClientSide()) return;

        for (int slot = 0; slot < WORKPLATE_SLOTS; slot++)
        {
            final ItemStack stack = workplateSlots.extractItem(slot, Integer.MAX_VALUE, false);
            if (!stack.isEmpty()) player.getInventory().placeItemBackInInventory(stack);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        final Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();

        if (index < WORKPLATE_SLOTS)
        {
            // Workplate → player inventory
            if (!moveItemStackTo(stack, WORKPLATE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        }
        else
        {
            // Player inventory → workplate (try each slot in order)
            if (ToolSmithing.isMetalHammer(stack))
            {
                if (!moveItemStackTo(stack, SLOT_HAMMER, SLOT_HAMMER + 1, false)) { /* fall through */ }
            }
            else if (stack.isDamageableItem() && ToolSmithing.isMetalTool(stack))
            {
                if (!moveItemStackTo(stack, SLOT_TOOL, SLOT_TOOL + 1, false)) { /* fall through */ }
            }
            else if (stack.is(TFCTags.Items.WELDING_FLUX))
            {
                if (!moveItemStackTo(stack, SLOT_FLUX, SLOT_FLUX + 1, false)) { /* fall through */ }
            }
        }

        if (stack.isEmpty())
        {
            slot.set(ItemStack.EMPTY);
        }
        else
        {
            slot.setChanged();
        }
        return original;
    }
}
