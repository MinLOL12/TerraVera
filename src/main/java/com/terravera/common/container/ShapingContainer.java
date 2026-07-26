/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.container;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import net.dries007.tfc.common.container.ButtonHandlerContainer;
import net.dries007.tfc.common.container.ISlotCallback;
import net.dries007.tfc.common.container.ItemStackContainer;
import net.dries007.tfc.common.container.slot.CallbackSlot;

import com.terravera.common.TerraVeraDataComponents;
import com.terravera.common.component.KnappedHead;
import com.terravera.common.items.TerraVeraItems;
import com.terravera.common.knapping.HeadProfile;
import com.terravera.common.knapping.KnapAnalysis;
import com.terravera.common.knapping.KnapGrid;
import com.terravera.common.knapping.KnappableStone;

/**
 * The TerraVera shaping (knapping) container.
 * <p>
 * Structurally this mirrors TerraFirmaCraft's {@code KnappingContainer} - a 5x5 grid of buttons that start "on" and
 * are clicked "off" - but the matching step is completely different. TFC compares the grid to a stored picture; we
 * hand the grid to {@link KnapAnalysis}, which measures it and decides what sort of working end, if any, the player
 * has produced. The consequence is that there is no single correct axe-head pattern: any shape with a sturdy base and
 * a wedge-shaped tip is an axe head.
 */
public class ShapingContainer extends ItemStackContainer implements ButtonHandlerContainer, ISlotCallback
{
    public static final int SLOT_OUTPUT = 0;
    public static final int GRID = 5;

    public static ShapingContainer create(ItemStack stack, KnappableStone stone, InteractionHand hand, int slot, Inventory inventory, int windowId)
    {
        return new ShapingContainer(TerraVeraContainers.SHAPING.get(), stone, windowId, inventory, stack, hand, slot).init(inventory, 20);
    }

    private final KnappableStone stone;
    private final boolean[] cells = new boolean[GRID * GRID];
    private final ItemStack originalStack;

    private boolean requiresReset;
    private boolean hasBeenModified;
    /** The reason the current shape is not usable, for the "keep going" hint in the screen. */
    @Nullable private String feedback;

    public ShapingContainer(MenuType<?> type, KnappableStone stone, int windowId, Inventory inventory, ItemStack stack, InteractionHand hand, int slot)
    {
        super(type, windowId, inventory, stack, hand, slot);
        this.stone = stone;
        this.originalStack = stack.copy();
        java.util.Arrays.fill(cells, true);
        setRequiresReset(false);
    }

    public KnappableStone stone()
    {
        return stone;
    }

    public ItemStack originalStack()
    {
        return originalStack;
    }

    public boolean cell(int index)
    {
        return cells[index];
    }

    @Nullable
    public String feedback()
    {
        return feedback;
    }

    @Override
    public void onButtonPress(int buttonId, @Nullable CompoundTag extraNbt)
    {
        if (buttonId < 0 || buttonId >= cells.length) return;
        cells[buttonId] = false;

        if (!hasBeenModified)
        {
            if (player != null && !player.isCreative())
            {
                stack.shrink(stone.consume());
            }
            hasBeenModified = true;
        }

        updateOutput();
    }

    /**
     * Re-measure the grid and set the output slot. Called after every flake removed, which is what makes the process
     * feel like knapping - you can watch the piece become an axe head as you work it.
     */
    private void updateOutput()
    {
        final KnapGrid grid = toGrid();
        final List<KnapAnalysis.Ranked.Candidate> candidates = new ArrayList<>();
        for (Map.Entry<ResourceLocation, HeadProfile> entry : HeadProfile.MANAGER.getElements().entrySet())
        {
            candidates.add(new KnapAnalysis.Ranked.Candidate(entry.getValue(), entry.getKey()));
        }

        final List<KnapAnalysis.Ranked> ranked = KnapAnalysis.rank(grid, candidates);
        final Slot slot = slots.get(SLOT_OUTPUT);

        // Only update the slot on the server. The client is synced from the server,
        // and updating it on the client can result in overriding the slot to EMPTY
        // if HeadProfile.MANAGER elements haven't finished syncing to the client yet.
        if (player == null || !player.level().isClientSide())
        {
            if (!ranked.isEmpty() && ranked.getFirst().outcome().success())
            {
                final KnapAnalysis.Ranked best = ranked.getFirst();
                final ResourceLocation id = (ResourceLocation) best.candidate().owner();
                final String kind = id.getPath();
                final ItemStack head = new ItemStack(TerraVeraItems.head(kind).get());
                head.set(TerraVeraDataComponents.KNAPPED_HEAD.get(),
                    new KnappedHead(id, stone.material(), best.outcome().quality()));
                slot.set(head);
            }
            else
            {
                slot.set(ItemStack.EMPTY);
            }
        }

        // Feedback is safe to calculate on both sides (so the client UI gets the near-miss reason)
        if (!ranked.isEmpty() && ranked.getFirst().outcome().success())
        {
            feedback = null;
        }
        else
        {
            feedback = ranked.isEmpty() ? null : ranked.getFirst().outcome().reason();
        }
        setRequiresReset(true);
    }

    private KnapGrid toGrid()
    {
        final boolean[] copy = new boolean[cells.length];
        System.arraycopy(cells, 0, copy, 0, cells.length);
        return new KnapGrid(GRID, GRID, copy);
    }

    @Override
    public boolean stillValid(Player player)
    {
        return !getTargetStack().isEmpty() || hasBeenModified;
    }

    @Override
    public void removed(Player player)
    {
        final ItemStack output = slots.get(SLOT_OUTPUT).getItem();
        if (!output.isEmpty() && !player.level().isClientSide())
        {
            player.getInventory().placeItemBackInInventory(output);
        }
        super.removed(player);
    }

    @Override
    public void onSlotTake(Player player, int slot, ItemStack stack)
    {
        // Taking the head resets the stone, ready for the next lump
        java.util.Arrays.fill(cells, true);
        feedback = null;
        setRequiresReset(true);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack)
    {
        return false;
    }

    public boolean requiresReset()
    {
        return requiresReset;
    }

    public void setRequiresReset(boolean requiresReset)
    {
        this.requiresReset = requiresReset;
    }

    @Override
    protected boolean moveStack(ItemStack stack, int slotIndex)
    {
        return switch (typeOf(slotIndex))
        {
            case CONTAINER -> !moveItemStackTo(stack, containerSlots, containerSlots + 36, true);
            case HOTBAR -> !moveItemStackTo(stack, containerSlots, containerSlots + 27, false);
            case MAIN_INVENTORY -> !moveItemStackTo(stack, containerSlots + 27, containerSlots + 36, false);
        };
    }

    @Override
    protected void addContainerSlots()
    {
        addSlot(new CallbackSlot(this, new ItemStackHandler(1), 0, 128, 46));
    }
}
