/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.greenhouse;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A greenhouse structure block. Comes in four tiers: cold frame, hoop house, glass greenhouse, and modern greenhouse.
 * The tier is baked into the block variant at registration time. The block state tracks ventilation open/closed
 * and a simple age/growth counter for visual changes.
 * <p>
 * Right-clicking with an empty hand toggles ventilation. Right-clicking with a watering can or while crouching
 * opens the climate control panel on modern greenhouses.
 */
public class GreenhouseBlock extends BaseEntityBlock
{
    public static final MapCodec<GreenhouseBlock> CODEC = simpleCodec(GreenhouseBlock::new);

    public static final IntegerProperty TIER = IntegerProperty.create("tier", 0, 3);
    public static final BooleanProperty VENT_OPEN = BooleanProperty.create("vent_open");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private final GreenhouseTier greenhouseTier;

    public GreenhouseBlock(GreenhouseTier tier, Properties properties)
    {
        super(properties.noOcclusion().strength(tier.level() == 3 ? 4.0f : 2.0f));
        this.greenhouseTier = tier;
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(TIER, tier.level())
            .setValue(VENT_OPEN, false)
            .setValue(POWERED, false));
    }

    public GreenhouseBlock(Properties properties)
    {
        this(GreenhouseTier.COLD_FRAME, properties);
    }

    public GreenhouseTier greenhouseTier() { return greenhouseTier; }

    /**
     * Get the greenhouse tier from a specific block state. This reads from the block state property rather than
     * the constructor-stored tier, so it works correctly even when the block instance was created by a codec
     * that doesn't know which variant it is.
     */
    public static GreenhouseTier tierFromState(BlockState state)
    {
        return GreenhouseTier.byLevel(state.getValue(TIER));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(TIER, VENT_OPEN, POWERED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state)
    {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new GreenhouseBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        if (level.isClientSide()) return null;
        if (type != com.terravera.common.blocks.TerraVeraBlockEntities.GREENHOUSE.get()) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof GreenhouseBlockEntity greenhouse)
            {
                greenhouse.serverTick(lvl, pos, st);
            }
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit)
    {
        // Right-click with shears or a stick toggles ventilation on non-automated greenhouses
        if (stack.is(net.minecraft.world.item.Items.SHEARS) || stack.is(net.minecraft.tags.ItemTags.SAPLINGS))
        {
            if (!level.isClientSide())
            {
                boolean currentlyOpen = state.getValue(VENT_OPEN);
                level.setBlock(pos, state.setValue(VENT_OPEN, !currentlyOpen), Block.UPDATE_ALL);
                if (level.getBlockEntity(pos) instanceof GreenhouseBlockEntity be)
                {
                    be.setVentilationOpen(!currentlyOpen);
                }
                player.displayClientMessage(
                    Component.translatable("terravera.greenhouse.vent_" + (!currentlyOpen ? "opened" : "closed")),
                    true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit)
    {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        // Toggle ventilation with empty hand
        if (!player.isShiftKeyDown())
        {
            boolean currentlyOpen = state.getValue(VENT_OPEN);
            level.setBlock(pos, state.setValue(VENT_OPEN, !currentlyOpen), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof GreenhouseBlockEntity be)
            {
                be.setVentilationOpen(!currentlyOpen);
            }
            player.displayClientMessage(
                Component.translatable("terravera.greenhouse.vent_" + (!currentlyOpen ? "opened" : "closed")),
                true);
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // Shift+click opens climate info
        if (level.getBlockEntity(pos) instanceof GreenhouseBlockEntity be)
        {
            GreenhouseClimate climate = be.climate();
            player.displayClientMessage(
                Component.translatable("terravera.greenhouse.info",
                    String.format("%.1f°C", climate.temperatureC()),
                    String.format("%.0f%%", climate.humidity() * 100),
                    String.format("%.0f%%", climate.soilMoisture() * 100),
                    climate.growthModifier() >= 0.7f ? "excellent" :
                    climate.growthModifier() >= 0.4f ? "fair" : "poor"),
                false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return defaultBlockState()
            .setValue(TIER, greenhouseTier.level())
            .setValue(VENT_OPEN, false)
            .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }
}
