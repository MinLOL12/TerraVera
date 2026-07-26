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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.container.KnappingContainer;
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
 * TerraVera's function-based knapping menu.
 *
 * <p>The menu deliberately extends TFC's {@link KnappingContainer}, rather than duplicating its button and pattern
 * handling. That lets it use TFC's stock {@code KnappingScreen}; only the result calculation differs. Instead of
 * matching a recipe pattern, the retained tiles are evaluated as a potential tool head.</p>
 */
public class ShapingContainer extends KnappingContainer
{
    public static final int SLOT_OUTPUT = KnappingContainer.SLOT_OUTPUT;
    public static final int GRID = KnappingPattern.MAX_WIDTH;

    private static final ResourceLocation ROCK_KNAPPING_TYPE = ResourceLocation.fromNamespaceAndPath("tfc", "rock");

    public static ShapingContainer create(ItemStack stack, KnappableStone stone, InteractionHand hand, int slot, Inventory inventory, int windowId)
    {
        return new ShapingContainer(TerraVeraContainers.SHAPING.get(), stone, rockKnappingType(), windowId, inventory, stack, hand, slot)
            .init(inventory, 20);
    }

    private static KnappingType rockKnappingType()
    {
        // Reuse TFC's rock type for its sounds, textures, particles, and base consumption behaviour.
        return KnappingType.MANAGER.getOrThrow(ROCK_KNAPPING_TYPE);
    }

    private final KnappableStone stone;
    private boolean hasAdjustedConsumption;

    public ShapingContainer(MenuType<?> type, KnappableStone stone, KnappingType knappingType, int windowId, Inventory inventory,
        ItemStack stack, InteractionHand hand, int slot)
    {
        super(type, knappingType, windowId, inventory, stack, hand, slot);
        this.stone = stone;
    }

    public KnappableStone stone()
    {
        return stone;
    }

    @Override
    public void onButtonPress(int buttonId, @Nullable CompoundTag extraNbt)
    {
        // This performs all of TFC's normal pattern, consumption, and reset synchronization work. TFC's stone
        // knapping recipes are disabled by TerraVera's data pack, so replace its recipe result below.
        super.onButtonPress(buttonId, extraNbt);

        // The stock rock type consumes one loose rock. TerraVera's data can require more, so consume the remainder
        // on the same first modification that the parent consumes its one rock.
        if (!hasAdjustedConsumption)
        {
            if (!player.isCreative())
            {
                stack.shrink(Math.max(0, stone.consume() - getKnappingType().amountToConsume()));
            }
            hasAdjustedConsumption = true;
        }
        updateOutput();
    }

    /** Re-evaluate the TFC pattern using TerraVera's function-based head profiles. */
    private void updateOutput()
    {
        final List<KnapAnalysis.Ranked.Candidate> candidates = new ArrayList<>();
        for (Map.Entry<ResourceLocation, HeadProfile> entry : HeadProfile.MANAGER.getElements().entrySet())
        {
            candidates.add(new KnapAnalysis.Ranked.Candidate(entry.getValue(), entry.getKey()));
        }

        final List<KnapAnalysis.Ranked> ranked = KnapAnalysis.rank(toGrid(), candidates);
        // The parent container has already synchronised the normal TFC result. Replace it only on the server.
        if (player != null && !player.level().isClientSide())
        {
            final Slot slot = slots.get(SLOT_OUTPUT);
            if (!ranked.isEmpty() && ranked.getFirst().outcome().success())
            {
                final KnapAnalysis.Ranked best = ranked.getFirst();
                final ResourceLocation id = (ResourceLocation) best.candidate().owner();
                final ItemStack head = new ItemStack(TerraVeraItems.head(id.getPath()).get());
                head.set(TerraVeraDataComponents.KNAPPED_HEAD.get(),
                    new KnappedHead(id, stone.material(), best.outcome().quality()));
                slot.set(head);
            }
            else
            {
                slot.set(ItemStack.EMPTY);
            }
        }
    }

    private KnapGrid toGrid()
    {
        final KnappingPattern pattern = getPattern();
        final boolean[] cells = new boolean[GRID * KnappingPattern.MAX_HEIGHT];
        for (int index = 0; index < cells.length; index++)
        {
            cells[index] = pattern.get(index);
        }
        return new KnapGrid(GRID, KnappingPattern.MAX_HEIGHT, cells);
    }
}
