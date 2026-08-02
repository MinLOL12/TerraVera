/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.sterilization;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.dries007.tfc.common.fluids.FluidHelpers;

import com.terravera.common.blocks.TerraVeraBlockEntities;
import com.terravera.common.health.WaterTreatment;

/**
 * A placeable water-sterilization machine with a real fluid tank and a GeckoLib model.
 * <p>
 * Pour untreated water in with a container, let the machine work, and draw treated water out. The five
 * {@link SterilizerType}s share this class and the {@link SterilizerBlockEntity}; everything that differs between a
 * SODIS rack and a copper still is data on the enum.
 */
public class SterilizerBlock extends HorizontalDirectionalBlock implements EntityBlock
{
    public static final MapCodec<SterilizerBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.STRING.fieldOf("sterilizer_type").xmap(SterilizerType::byId, SterilizerType::id)
            .forGetter(SterilizerBlock::sterilizerType),
        propertiesCodec()
    ).apply(i, SterilizerBlock::new));

    /** Four visible fill bands for the fluid in the tank. */
    public static final IntegerProperty WATER_LEVEL = IntegerProperty.create("water_level", 0, 4);
    /** {@code true} while a batch is actively being processed - drives the "working" animation. */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private static final VoxelShape SODIS_SHAPE = Shapes.or(
        box(1, 0, 1, 4, 16, 4), box(12, 0, 1, 15, 16, 4),
        box(1, 0, 12, 4, 16, 15), box(12, 0, 12, 15, 16, 15),
        box(2, 0, 2, 14, 3, 14), box(2, 14, 2, 14, 16, 14));
    private static final VoxelShape BIO_SAND_SHAPE = Shapes.or(
        box(2, 0, 2, 14, 14, 14), box(1, 14, 1, 15, 16, 15), box(3, 1, 13, 9, 4, 16));
    private static final VoxelShape STILL_SHAPE = Shapes.or(
        box(2, 0, 2, 14, 8, 14), box(4, 8, 4, 12, 12, 12), box(3, 12, 3, 13, 15, 13),
        box(4, 12, 12, 12, 15, 16), box(10, 4, 12, 15, 10, 16));
    private static final VoxelShape UV_SHAPE = Shapes.or(
        box(1, 0, 1, 15, 2, 15), box(2, 2, 2, 14, 12, 14), box(0, 12, 0, 16, 14, 16));
    private static final VoxelShape CLARIFIER_SHAPE = Shapes.or(
        box(0, 0, 0, 16, 10, 16), box(1, 10, 1, 15, 12, 15), box(2, 12, 2, 14, 14, 14));

    private final SterilizerType sterilizerType;

    public SterilizerBlock(SterilizerType sterilizerType, Properties properties)
    {
        super(properties);
        this.sterilizerType = sterilizerType;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
            .setValue(WATER_LEVEL, 0).setValue(ACTIVE, false));
    }

    public SterilizerType sterilizerType()
    {
        return sterilizerType;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec()
    {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING, WATER_LEVEL, ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return switch (sterilizerType)
        {
            case SODIS_RACK -> SODIS_SHAPE;
            case BIO_SAND_FILTER -> BIO_SAND_SHAPE;
            case DISTILLATION_STILL -> STILL_SHAPE;
            case UV_STERILIZER -> UV_SHAPE;
            case CLARIFIER -> CLARIFIER_SHAPE;
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (!(level.getBlockEntity(pos) instanceof SterilizerBlockEntity sterilizer))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        final int before = sterilizer.waterAmount();
        // Capture what the player is holding before the transfer; if it goes into the tank, that is the water's origin.
        final WaterTreatment incoming = WaterTreatment.get(stack);

        if (!FluidHelpers.transferBetweenBlockEntityAndItem(stack, sterilizer, player, hand))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (!level.isClientSide())
        {
            if (sterilizer.waterAmount() > before)
            {
                // The tank gained water - stamp its provenance and restart the batch, because a batch is only as
                // clean as its dirtiest addition.
                sterilizer.onInputFilled(incoming);
            }
            else if (sterilizer.waterAmount() < before)
            {
                // The tank lost water - the player's container now holds whatever the machine has produced.
                sterilizer.stampOutput(player.getItemInHand(hand));
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
    {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SterilizerBlockEntity sterilizer)
        {
            final int percent = sterilizer.processTicks() == 0 ? 0
                : sterilizer.progress() * 100 / sterilizer.processTicks();
            player.displayClientMessage(Component.translatable("terravera.sterilizer.status",
                sterilizer.waterAmount(), sterilizer.capacity(), percent,
                Component.translatable(sterilizer.isTreated()
                    ? "terravera.sterilizer.status_done" : "terravera.sterilizer.status_waiting"))
                .withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new SterilizerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> actual)
    {
        return level.isClientSide() ? null
            : createTicker(actual, TerraVeraBlockEntities.STERILIZER.get(), SterilizerBlockEntity::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTicker(
        BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker)
    {
        return actual == expected ? (BlockEntityTicker<A>) ticker : null;
    }
}
