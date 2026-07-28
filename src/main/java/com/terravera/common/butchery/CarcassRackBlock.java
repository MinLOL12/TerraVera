/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.butchery;

import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.terravera.common.TerraVeraDataComponents;

/**
 * Carcass Rack block.
 * <p>
 * A dedicated hanging rack where animal carcasses can be suspended for butchering.
 * When a player right-clicks an empty rack with a Carcass item, it hangs on the rack.
 * Using a Butcher's Knife on the hanging carcass performs the next butchery stage,
 * causing the animal's anatomical layers and pixels to wear off realistically while dropping loot.
 */
public class CarcassRackBlock extends HorizontalDirectionalBlock implements EntityBlock
{
    public static final com.mojang.serialization.MapCodec<CarcassRackBlock> CODEC = simpleCodec(CarcassRackBlock::new);

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 30, 15);

    public CarcassRackBlock(Properties properties)
    {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec()
    {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPE;
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
        return new CarcassRackBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit)
    {
        final BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CarcassRackBlockEntity rack))
        {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        if (player.isShiftKeyDown() && rack.hasCarcass() && stack.isEmpty())
        {
            final ItemStack removed = rack.removeCarcass();
            if (!player.getInventory().add(removed))
            {
                player.drop(removed, false);
            }
            player.displayClientMessage(Component.translatable("terravera.butchery.rack.removed")
                .withStyle(ChatFormatting.GRAY), true);
            level.playSound(null, pos, SoundType.WOOD.getBreakSound(), SoundSource.BLOCKS, 0.6f, 1.0f);
            return ItemInteractionResult.SUCCESS;
        }

        if (!rack.hasCarcass())
        {
            final CarcassData data = stack.get(TerraVeraDataComponents.CARCASS.get());
            if (data != null)
            {
                rack.setCarcassStack(stack.split(1));
                player.displayClientMessage(Component.translatable("terravera.butchery.rack.hung",
                    data.species().displayName()).withStyle(ChatFormatting.GREEN), true);
                level.playSound(null, pos, SoundType.WOOD.getPlaceSound(), SoundSource.BLOCKS, 0.7f, 0.9f);
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Rack has a carcass. Check if player is holding a butchery blade.
        final ButcheryTool tool = ButcheryTool.of(stack);
        if (!tool.isBlade())
        {
            player.displayClientMessage(Component.translatable("terravera.butchery.rack.need_blade")
                .withStyle(ChatFormatting.RED), true);
            return ItemInteractionResult.SUCCESS;
        }

        // Butcher the carcass hanging on the rack!
        ButcherySystem.butcherOnRack(player, rack, stack, hand);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit)
    {
        final BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CarcassRackBlockEntity rack))
        {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown() && rack.hasCarcass())
        {
            final ItemStack removed = rack.removeCarcass();
            if (!player.getInventory().add(removed))
            {
                player.drop(removed, false);
            }
            player.displayClientMessage(Component.translatable("terravera.butchery.rack.removed")
                .withStyle(ChatFormatting.GRAY), true);
            level.playSound(null, pos, SoundType.WOOD.getBreakSound(), SoundSource.BLOCKS, 0.6f, 1.0f);
            return InteractionResult.SUCCESS;
        }

        if (rack.hasCarcass())
        {
            final CarcassData data = rack.getCarcassData();
            if (data != null)
            {
                player.displayClientMessage(Component.translatable("terravera.butchery.rack.status",
                    data.species().displayName(), data.stage().displayName())
                    .withStyle(ChatFormatting.GRAY), true);
            }
            else
            {
                player.displayClientMessage(Component.translatable("terravera.butchery.rack.need_blade")
                    .withStyle(ChatFormatting.RED), true);
            }
        }
        else
        {
            player.displayClientMessage(Component.translatable("terravera.butchery.rack.empty")
                .withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResult.SUCCESS;
    }
}
