package com.terravera.common.power;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Base class for TerraVera's low-voltage conductors.
 * <p>
 * Wire blocks carry an explicit connection property per face. A connection is only made when the neighbouring block
 * agrees: another wire always mates, and machines only mate through faces they expose as terminals
 * ({@link PowerMachine#canConnectWires(Direction)}). {@link PowerNetwork} walks exactly these declared connections,
 * so the electricity you see is the electricity you get - nothing jumps gaps, and nothing secretly powers a machine
 * that a wire merely brushes past on a non-terminal face.
 * <p>
 * The {@code powered} property is a client-visible flag the grid sets on wires that are carrying current, so an
 * energised run glows faintly and a dead one does not.
 */
public abstract class WireBlock extends Block {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = new EnumMap<>(Direction.class);
    static {
        PROPERTY_BY_DIRECTION.put(Direction.NORTH, NORTH);
        PROPERTY_BY_DIRECTION.put(Direction.EAST, EAST);
        PROPERTY_BY_DIRECTION.put(Direction.SOUTH, SOUTH);
        PROPERTY_BY_DIRECTION.put(Direction.WEST, WEST);
        PROPERTY_BY_DIRECTION.put(Direction.UP, UP);
        PROPERTY_BY_DIRECTION.put(Direction.DOWN, DOWN);
    }

    protected static final VoxelShape CORE = box(6, 0, 6, 10, 3, 10);
    protected static final VoxelShape ARM_UP = box(7, 2, 7, 9, 16, 9);
    protected static final VoxelShape ARM_NORTH = box(7, 0, 0, 9, 3, 8);
    protected static final VoxelShape ARM_SOUTH = box(7, 0, 8, 9, 3, 16);
    protected static final VoxelShape ARM_WEST = box(0, 0, 7, 8, 3, 9);
    protected static final VoxelShape ARM_EAST = box(8, 0, 7, 16, 3, 9);

    protected WireBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(NORTH, false).setValue(EAST, false)
            .setValue(SOUTH, false).setValue(WEST, false).setValue(UP, false).setValue(DOWN, false)
            .setValue(POWERED, false));
    }

    /** Maximum continuous throughput of this conductor, in watts. Exceeding it heats the wire until it fails. */
    public abstract int capacity();

    /** Whether this conductor can make vertical runs through its own block space. Junctions can; simple wire cannot. */
    public boolean allowsVerticalRuns() {
        return false;
    }

    /** Whether the given face of this wire may connect at all. Simple wire is limited to horizontal + upward. */
    public boolean canHaveConnection(Direction side) {
        if (side == Direction.DOWN) return false;
        if (side == Direction.UP) return allowsVerticalRuns();
        return true;
    }

    /**
     * Whether this wire, at {@code pos}, should connect out through {@code side}. A connection requires this face to
     * be usable, and the neighbour to be a wire, or a machine exposing a terminal on the mating face.
     */
    public boolean shouldConnect(BlockGetter level, BlockPos pos, Direction side) {
        if (!canHaveConnection(side)) return false;
        BlockState neighbour = level.getBlockState(pos.relative(side));
        if (neighbour.getBlock() instanceof WireBlock other) {
            return other.canHaveConnection(side.getOpposite());
        }
        if (neighbour.getBlock() instanceof PowerMachine machine) {
            return machine.canConnectWires(side.getOpposite());
        }
        return false;
    }

    public BlockState connectionState(BlockGetter level, BlockPos pos, BlockState state) {
        for (Direction side : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(side), shouldConnect(level, pos, side));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connectionState(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction side, BlockState neighbourState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighbourPos) {
        if (!canHaveConnection(side)) return state.setValue(PROPERTY_BY_DIRECTION.get(side), false);
        boolean connected;
        if (neighbourState.getBlock() instanceof WireBlock other) {
            connected = other.canHaveConnection(side.getOpposite());
        } else if (neighbourState.getBlock() instanceof PowerMachine machine) {
            connected = machine.canConnectWires(side.getOpposite());
        } else {
            connected = false;
        }
        return state.setValue(PROPERTY_BY_DIRECTION.get(side), connected);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        if (state.getValue(UP)) shape = Shapes.or(shape, ARM_UP);
        if (state.getValue(NORTH)) shape = Shapes.or(shape, ARM_NORTH);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, ARM_SOUTH);
        if (state.getValue(WEST)) shape = Shapes.or(shape, ARM_WEST);
        if (state.getValue(EAST)) shape = Shapes.or(shape, ARM_EAST);
        return shape;
    }

    /** Reads the declared connection for one face straight off the block state. */
    public static boolean isConnected(BlockState state, Direction side) {
        return state.getValue(PROPERTY_BY_DIRECTION.get(side));
    }
}
