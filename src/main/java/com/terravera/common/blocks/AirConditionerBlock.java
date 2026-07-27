package com.terravera.common.blocks;

import com.terravera.common.climate.ClimateControlSystem;
import com.terravera.common.container.ClimateControllerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Compressor/condenser cabinet. It is deliberately inert until a programmed control circuit is installed. */
public class AirConditionerBlock extends Block {
    public AirConditionerBlock(Properties properties) { super(properties); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer server) server.openMenu(new MenuProvider() {
            public Component getDisplayName() { return Component.translatable("block.terravera.air_conditioner"); }
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new ClimateControllerMenu(id, inv, pos); }
        }, b -> b.writeBlockPos(pos));
        return InteractionResult.CONSUME;
    }
    @Override public void onRemove(BlockState old, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!old.is(replacement.getBlock())) ClimateControlSystem.remove(pos);
        super.onRemove(old, level, pos, replacement, moving);
    }
}
