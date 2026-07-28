/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 */

package com.terravera.common.water;

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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.dries007.tfc.common.fluids.FluidHelpers;

import com.terravera.common.blocks.TerraVeraBlockEntities;
import com.terravera.common.health.WaterTreatment;

/** A passive collector with a real fluid tank, species-appropriate water quality, and layered GeckoLib geometry. */
public class WaterCollectorBlock extends HorizontalDirectionalBlock implements EntityBlock
{
    public static final MapCodec<WaterCollectorBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.STRING.fieldOf("collector_type").xmap(CollectorType::byId, CollectorType::id)
            .forGetter(WaterCollectorBlock::collectorType),
        propertiesCodec()
    ).apply(i, WaterCollectorBlock::new));

    /** Four visible fill bands. The fluid itself is in the block entity tank. */
    public static final IntegerProperty WATER_LEVEL = IntegerProperty.create("water_level", 0, 4);

    private static final VoxelShape RAIN_SHAPE = Shapes.or(
        box(1, 0, 1, 4, 16, 4), box(12, 0, 1, 15, 16, 4),
        box(1, 0, 12, 4, 16, 15), box(12, 0, 12, 15, 16, 15),
        box(2, 0, 2, 14, 4, 14));
    private static final VoxelShape DEW_SHAPE = Shapes.or(
        box(1, 0, 6, 3, 14, 10), box(13, 0, 6, 15, 14, 10), box(2, 0, 5, 14, 3, 11));
    private static final VoxelShape BASIN_SHAPE = Shapes.or(
        box(0, 0, 0, 16, 5, 3), box(0, 0, 13, 16, 5, 16),
        box(0, 0, 3, 3, 5, 13), box(13, 0, 3, 16, 5, 13), box(2, 0, 2, 14, 2, 14));
    private static final VoxelShape STILL_SHAPE = Shapes.or(
        box(0, 0, 0, 16, 4, 3), box(0, 0, 13, 16, 4, 16),
        box(0, 0, 3, 3, 4, 13), box(13, 0, 3, 16, 4, 13), box(2, 0, 2, 14, 2, 14),
        box(1, 3, 1, 15, 10, 15));

    private final CollectorType collectorType;

    public WaterCollectorBlock(CollectorType collectorType, Properties properties)
    {
        super(properties);
        this.collectorType = collectorType;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATER_LEVEL, 0));
    }

    public CollectorType collectorType()
    {
        return collectorType;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec()
    {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING, WATER_LEVEL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return switch (collectorType)
        {
            case RAIN_CATCHER -> RAIN_SHAPE;
            case DEW_COLLECTOR -> DEW_SHAPE;
            case ROCK_BASIN -> BASIN_SHAPE;
            case SOLAR_STILL -> STILL_SHAPE;
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (!(level.getBlockEntity(pos) instanceof WaterCollectorBlockEntity collector))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        final int before = collector.waterAmount();
        if (!FluidHelpers.transferBetweenBlockEntityAndItem(stack, collector, player, hand))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        // If the tank went down, the item was filled from this source. Preserve that source's realistic health risk.
        if (!level.isClientSide() && collector.waterAmount() < before)
        {
            final ItemStack filled = player.getItemInHand(hand);
            if (!filled.isEmpty())
            {
                WaterTreatment.set(filled, new WaterTreatment(WaterTreatment.Treatment.UNTREATED,
                    collectorType.contamination()));
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
    {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof WaterCollectorBlockEntity collector)
        {
            player.displayClientMessage(Component.translatable("terravera.water_collector.status",
                collector.waterAmount(), collector.capacity()).withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new WaterCollectorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> actual)
    {
        return level.isClientSide() ? null
            : createTicker(actual, TerraVeraBlockEntities.WATER_COLLECTOR.get(), WaterCollectorBlockEntity::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTicker(
        BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker)
    {
        return actual == expected ? (BlockEntityTicker<A>) ticker : null;
    }
}
