package com.terravera.common.power;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import com.terravera.common.blocks.TerraVeraBlockEntities;
import com.terravera.common.blockentity.WindTurbineBlockEntity;

/**
 * Outdoor wind generator. Its rotor is only free to spin when there is open sky directly above the mast, and the
 * {@code spinning} block state mirrors that - the client reads it to animate the blades, and the grid reads it to
 * decide whether the turbine is producing. Wire attaches to the terminal lugs on the four sides of the mast base
 * and on the bottom, never to the blades themselves.
 */
public class WindTurbineBlock extends HorizontalDirectionalBlock implements EntityBlock, PowerSource {
    public static final com.mojang.serialization.MapCodec<WindTurbineBlock> CODEC = simpleCodec(WindTurbineBlock::new);

    public static final BooleanProperty SPINNING = BooleanProperty.create("spinning");
    public static final int OUTPUT_WATTS = 220;

    public WindTurbineBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(SPINNING, false));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SPINNING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean spinning = context.getLevel().canSeeSky(context.getClickedPos().above());
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(SPINNING, spinning);
    }

    @Override
    public boolean canConnectWires(Direction side) {
        // Terminal lugs: the four mast faces plus the underside of the base plate.
        return true;
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                                                  BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        // Mast and nacelle column; the blades sweep freely outside the block.
        return box(4, 0, 4, 12, 16, 12);
    }

    @Override
    public int powerOutput(Level level, BlockPos pos, BlockState state) {
        return state.getValue(SPINNING) ? OUTPUT_WATTS : 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WindTurbineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, TerraVeraBlockEntities.WIND_TURBINE.get(), WindTurbineBlockEntity::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
        BlockEntityType<A> actualType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker
    ) {
        return expectedType == actualType ? (BlockEntityTicker<A>) ticker : null;
    }
}
