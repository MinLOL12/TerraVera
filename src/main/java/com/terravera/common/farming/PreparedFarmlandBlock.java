/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.farming;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Farmland that has been cleared of stones and roots, loosened, and optionally amended with compost or manure.
 * The quality of the soil preparation directly affects crop growth and yield. Unprepared dirt grows poor crops;
 * well-prepared soil grows strong ones.
 * <p>
 * Soil condition is stored in a block entity so that different beds can have different preparation levels.
 */
public class PreparedFarmlandBlock extends Block implements EntityBlock
{
    /** Preparation level: 0 = just tilled, 1 = cleared, 2 = cleared + loosened, 3 = fully prepared. */
    public static final IntegerProperty PREPARATION = IntegerProperty.create("preparation", 0, 3);

    public PreparedFarmlandBlock(Properties properties)
    {
        super(properties.strength(0.6f).randomTicks());
        this.registerDefaultState(this.stateDefinition.any().setValue(PREPARATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(PREPARATION);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new PreparedFarmlandBlockEntity(pos, state);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random)
    {
        // Weeds slowly grow back, soil slowly compacts
        if (level.getBlockEntity(pos) instanceof PreparedFarmlandBlockEntity be)
        {
            var current = be.soilCondition();
            var decayed = current.decay(1.0f);
            be.setSoilCondition(decayed);

            // Rain adds moisture
            if (level.isRaining())
            {
                be.setSoilCondition(decayed.withMoisture(Math.min(1.0f, decayed.moisture() + 0.05f)));
            }
            else if (level.isDay())
            {
                // Sunlight dries soil slowly
                be.setSoilCondition(decayed.withMoisture(Math.max(0, decayed.moisture() - 0.02f)));
            }
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (!handlesSoilInteraction(stack))
        {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof PreparedFarmlandBlockEntity be))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        // Hoe: improve clearing and loosening
        if (stack.is(net.minecraft.tags.ItemTags.HOES))
        {
            var current = be.soilCondition();
            be.setSoilCondition(current.clear(0.2f).loosen(0.2f));
            level.setBlock(pos, state.setValue(PREPARATION, Math.min(3, state.getValue(PREPARATION) + 1)), Block.UPDATE_ALL);
            if (!player.isCreative()) stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("terravera.soil.tilled"),
                true);
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        // Compost/rotten food: improves fertility
        if (isCompostMaterial(stack))
        {
            var current = be.soilCondition();
            be.setSoilCondition(current.amend(0.3f));
            level.setBlock(pos, state.setValue(PREPARATION, Math.min(3, state.getValue(PREPARATION) + 1)), Block.UPDATE_ALL);
            if (!player.isCreative()) stack.shrink(1);
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("terravera.soil.amended"),
                true);
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        // Bucket of water: adds moisture
        if (stack.is(Items.WATER_BUCKET))
        {
            var current = be.soilCondition();
            be.setSoilCondition(current.withMoisture(Math.min(1.0f, current.moisture() + 0.4f)));
            if (!player.isCreative())
            {
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            }
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("terravera.soil.watered"),
                true);
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private boolean handlesSoilInteraction(ItemStack stack)
    {
        return stack.is(net.minecraft.tags.ItemTags.HOES) || isCompostMaterial(stack) || stack.is(Items.WATER_BUCKET);
    }

    private boolean isCompostMaterial(ItemStack stack)
    {
        // Accept any compost-like item from TFC, vanilla, or TerraVera's own soil amendment tag.
        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return stack.is(Items.ROTTEN_FLESH)
            || stack.is(Items.BONE_MEAL)
            || stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("terravera", "soil_amendments")))
            || path.contains("compost")
            || path.contains("manure");
    }
}
