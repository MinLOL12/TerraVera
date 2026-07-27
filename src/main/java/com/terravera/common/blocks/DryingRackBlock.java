/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.blocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.terravera.common.TerraVeraDataComponents;
import com.terravera.common.temperature.Shelter;
import com.terravera.common.temperature.TemperatureSystem;
import com.terravera.common.temperature.Wetness;

/**
 * A frame for hanging wet clothes on.
 * <p>
 * Wearing a soaked coat dry is slow, cold, and stupid, which is exactly why the temperature system makes it slow: a
 * garment only dries meaningfully near a fire, and you cannot be wearing it and warming it at the same time. This
 * block is the answer. Hold a wet garment and click the rack, and it dries at a rate set by how much heat is nearby -
 * fast beside a lit firepit, slowly in a warm sealed room, essentially not at all in a frozen field.
 * <p>
 * It is deliberately an instant, item-in-hand interaction rather than an inventory block. The lesson is "get your
 * clothes near a fire", not "manage another storage UI".
 */
public class DryingRackBlock extends HorizontalDirectionalBlock
{
    public static final com.mojang.serialization.MapCodec<DryingRackBlock> CODEC = simpleCodec(DryingRackBlock::new);

    private static final VoxelShape SHAPE = Block.box(0, 0, 6, 16, 16, 10);

    public DryingRackBlock(Properties properties)
    {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
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
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context)
    {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit)
    {
        final Wetness wetness = stack.get(TerraVeraDataComponents.GARMENT_WETNESS.get());
        if (wetness == null || wetness.isDry())
        {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        // Heat is what dries cloth. The rack itself does nothing; a rack beside a fire does a great deal.
        final Shelter shelter = Shelter.survey(level, pos.above());
        final float ambient = TemperatureSystem.ambientTemperature(level, pos);

        float dried = 0.05f;
        if (shelter.hasFire()) dried += 0.30f;
        if (shelter.isIndoors()) dried += 0.05f;
        if (ambient > 20f) dried += 0.10f;
        if (ambient < 0f) dried *= 0.2f;

        stack.set(TerraVeraDataComponents.GARMENT_WETNESS.get(),
            wetness.drier(dried, level.getGameTime()));

        final Wetness after = stack.get(TerraVeraDataComponents.GARMENT_WETNESS.get());
        player.displayClientMessage(Component.translatable(
            after != null && after.isDry() ? "terravera.clothing.dried" : "terravera.clothing.drying")
            .withStyle(ChatFormatting.GRAY), true);
        level.playSound(null, pos, SoundType.WOOL.getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 1.1f);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
    {
        if (!level.isClientSide())
        {
            final Shelter shelter = Shelter.survey(level, pos.above());
            player.displayClientMessage(Component.translatable(shelter.hasFire()
                ? "terravera.clothing.rack.warm"
                : "terravera.clothing.rack.cold").withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
