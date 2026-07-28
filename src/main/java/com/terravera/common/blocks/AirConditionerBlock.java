package com.terravera.common.blocks;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import com.terravera.common.blockentity.AirConditionerBlockEntity;
import com.terravera.common.climate.ClimateControlSystem;
import com.terravera.common.container.ClimateControllerMenu;
import com.terravera.common.power.PowerConsumer;

/**
 * Compressor/condenser cabinet. It is deliberately inert until a programmed control circuit is installed, and it
 * draws its electricity through the terminal block on its back face. Two server-maintained block states make the
 * machine legible from across the room: {@code powered} (the grid can feed it) and {@code running} (the compressor
 * is actively moving heat), which the client uses to idle or spin up the roof fan.
 */
public class AirConditionerBlock extends HorizontalDirectionalBlock implements EntityBlock, PowerConsumer {
    public static final com.mojang.serialization.MapCodec<AirConditionerBlock> CODEC = simpleCodec(AirConditionerBlock::new);

    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");

    public AirConditionerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWERED, false).setValue(RUNNING, false));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, RUNNING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer server) server.openMenu(new MenuProvider() {
            public Component getDisplayName() { return Component.translatable("block.terravera.air_conditioner"); }
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new ClimateControllerMenu(id, inv, pos); }
        }, b -> b.writeBlockPos(pos));
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean canConnectWires(Direction side) {
        // Terminal lugs around the base accept wire on any side; the roof fan and the underside do not.
        return side.getAxis() != Direction.Axis.Y;
    }

    @Override
    public int powerDemand(Level level, BlockPos pos, BlockState state) {
        return ClimateControlSystem.gridDemand(pos);
    }

    /** Server-side helper: publish the grid's verdict on this unit onto the block state, without redundant writes. */
    public static void setWorkState(Level level, BlockPos pos, boolean powered, boolean running) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof AirConditionerBlock)) return;
        if (state.getValue(POWERED) != powered || state.getValue(RUNNING) != running) {
            level.setBlock(pos, state.setValue(POWERED, powered).setValue(RUNNING, running), 3);
        }
    }

    @Override
    public void onRemove(BlockState old, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!old.is(replacement.getBlock())) ClimateControlSystem.remove(pos);
        super.onRemove(old, level, pos, replacement, moving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AirConditionerBlockEntity(pos, state);
    }
}
