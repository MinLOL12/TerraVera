package com.terravera.common.blocks;

import com.mojang.serialization.MapCodec;
import com.terravera.common.paper.PaperContent;
import com.terravera.common.TerraVeraDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A sheet of paper taped or glued to a wall. Stores PaperContent in its block entity.
 * The block itself is thin; the real rendering is done by PostedPaperBlockEntityRenderer
 * which draws the paper texture + user's strokes.
 */
public class PostedPaperBlock extends BaseEntityBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final MapCodec<PostedPaperBlock> CODEC = simpleCodec(PostedPaperBlock::new);

    // Thin sheet against wall: 1 pixel thick
    private static final VoxelShape SHAPE_NORTH = Block.box(2, 2, 15, 14, 14, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(2, 2, 0, 14, 14, 1);
    private static final VoxelShape SHAPE_EAST = Block.box(0, 2, 2, 1, 14, 14);
    private static final VoxelShape SHAPE_WEST = Block.box(15, 2, 2, 16, 14, 14);

    public PostedPaperBlock(Properties props)
    {
        super(props
            .mapColor(MapColor.COLOR_WHITE)
            .noCollission()
            .noOcclusion()
            .strength(0.1f)
        );
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec()
    {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx)
    {
        return switch (state.getValue(FACING))
        {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        Direction facing = state.getValue(FACING);
        BlockPos behind = pos.relative(facing.getOpposite());
        BlockState behindState = level.getBlockState(behind);
        return behindState.isFaceSturdy(level, behind, facing) || behindState.isSolid();
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos)
    {
        if (!state.canSurvive(level, pos))
        {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Override
    public RenderShape getRenderShape(BlockState state)
    {
        return RenderShape.ENTITYBLOCK_ANIMATED; // use BER
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
    {
        if (level.getBlockEntity(pos) instanceof PostedPaperBlockEntity be)
        {
            if (level.isClientSide())
            {
                // Open viewer/editor for posted paper
                PaperContent content = be.getContent();
                if (content == null) content = PaperContent.EMPTY;
                com.terravera.client.paper.PostedPaperScreen.open(pos, content);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving)
    {
        if (!state.is(newState.getBlock()))
        {
            if (level.getBlockEntity(pos) instanceof PostedPaperBlockEntity be)
            {
                PaperContent content = be.getContent();
                ItemStack drop = new ItemStack(com.terravera.common.items.TerraVeraItems.PAPER_SHEET.get());
                if (content != null && !content.isEmpty())
                {
                    drop.set(TerraVeraDataComponents.PAPER_CONTENT.get(), content);
                }
                Block.popResource(level, pos, drop);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new PostedPaperBlockEntity(pos, state);
    }
}
