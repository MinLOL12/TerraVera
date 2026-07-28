package com.terravera.common.power;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import com.terravera.common.blockentity.HandCrankBlockEntity;

/**
 * Manual emergency power. Each turn of the handle drives the crankshaft one full revolution - animated on the
 * client - and keeps the generator head producing 140 W for five seconds. The {@code powered} state mirrors the
 * server-side timer so the grid and the client animation agree on whether the crank is turning.
 */
public class HandCrankBlock extends Block implements EntityBlock, PowerSource {
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final int OUTPUT_WATTS = 140;
    /** How long one turn keeps the crank generating, in server ticks. */
    public static final int POWER_DURATION_TICKS = 100;

    public HandCrankBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(POWERED, true), 3);
            level.scheduleTick(pos, this, POWER_DURATION_TICKS);
            level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, .6f, .7f + level.getRandom().nextFloat() * .2f);
            if (level.getBlockEntity(pos) instanceof HandCrankBlockEntity crank) {
                crank.turn();
            }
        }
        player.swing(InteractionHand.MAIN_HAND);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, false), 3);
        }
    }

    @Override
    public boolean canConnectWires(Direction side) {
        // Output lugs run out through the sides of the wooden base plate.
        return side.getAxis() != Direction.Axis.Y;
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                                                  BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        // Base plate, bearing cheeks and axle; the handle's sweep extends a little above this, like a lever.
        return net.minecraft.world.phys.shapes.Shapes.or(box(4, 0, 4, 12, 2, 12), box(4.5, 2, 7, 11.5, 7.5, 9));
    }

    @Override
    public int powerOutput(Level level, BlockPos pos, BlockState state) {
        return state.getValue(POWERED) ? OUTPUT_WATTS : 0;
    }

    /** Convenience for server-side logic that wants to know if the crank is mid-turn. */
    public static boolean isTurning(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.hasProperty(POWERED) && state.getValue(POWERED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HandCrankBlockEntity(pos, state);
    }
}
