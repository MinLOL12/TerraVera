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
import net.dries007.tfc.common.container.KnappingContainer;
import net.dries007.tfc.common.container.slot.CallbackSlot;
import net.dries007.tfc.util.data.KnappingPattern;
import net.dries007.tfc.util.data.KnappingType;

import com.terravera.common.TerraVeraDataComponents;
import com.terravera.common.component.KnappedHead;
import com.terravera.common.items.TerraVeraItems;
import com.terravera.common.knapping.HeadProfile;
import com.terravera.common.knapping.KnapAnalysis;
import com.terravera.common.knapping.KnapGrid;
import com.terravera.common.knapping.KnappableStone;

/**
 * TerraVera shaping (knapping) container that extends TFC's KnappingContainer.
 * <p>
 * This extends TFC's container to use its GUI system, but overrides the output calculation
 * to use TerraVera's function-based knapping analysis instead of TFC's pattern matching.
 */
public class ShapingContainer extends KnappingContainer implements ButtonHandlerContainer, ISlotCallback
{
    public static final int SLOT_OUTPUT = 0;

    private final KnappableStone stone;
    /** The reason the current shape is not usable, for feedback purposes. */
    @Nullable private String feedback;

    public static ShapingContainer create(ItemStack stack, KnappingType knappingType, KnappableStone stone, InteractionHand hand, int slot, Inventory inventory, int windowId)
    {
        return new ShapingContainer(net.dries007.tfc.common.container.TFCContainerTypes.KNAPPING.get(), knappingType, stone, windowId, inventory, stack, hand, slot).init(inventory, 20);
    }

    private ShapingContainer(MenuType<?> type, KnappingType knappingType, KnappableStone stone, int windowId, Inventory inventory, ItemStack stack, InteractionHand hand, int slot)
    {
        super(type, knappingType, windowId, inventory, stack, hand, slot);
        this.stone = stone;
    }

    public KnappableStone stone()
    {
        return stone;
    }

    @Nullable
    public String feedback()
    {
        return feedback;
    }

    @Override
    public void onButtonPress(int buttonId, @Nullable CompoundTag extraNbt)
    {
        // Call parent to update the pattern
        super.onButtonPress(buttonId, extraNbt);
        
        // Then recalculate output using TerraVera's analysis
        updateOutput();
    }

    /**
     * Re-measure the grid and set the output slot using TerraVera's function-based analysis.
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

        // Only update the slot on the server
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

        // Feedback is safe to calculate on both sides
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
        final KnappingPattern pattern = getPattern();
        final boolean[] cells = new boolean[5 * 5];
        for (int i = 0; i < cells.length; i++)
        {
            cells[i] = pattern.get(i);
        }
        return new KnapGrid(5, 5, cells);
    }

    @Override
    public void onSlotTake(Player player, int slot, ItemStack stack)
    {
        // Taking the head resets the pattern, ready for the next knapping
        getPattern().setAll(false);
        feedback = null;
        setRequiresReset(true);
    }

    @Override
    protected void addContainerSlots()
    {
        addSlot(new CallbackSlot(this, new ItemStackHandler(1), 0, 128, 46));
    }
}
