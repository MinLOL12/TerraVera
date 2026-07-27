/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.NetworkHooks;

import com.terravera.common.container.WorkplateContainer;

/** A low work surface: thick enough to take hammering, short enough to look like a plate on a stump or bench. */
public class WorkplateBlock extends Block
{
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 5.0, 15.0);

    public WorkplateBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPE;
    }

    /**
     * Right-clicking a placed workplate opens the repair GUI. The GUI centralises every step of the maintenance
     * process — hammer, hot tool, flux, operation selection, and striking — so the player does not have to remember
     * sneak-click shortcuts or juggle items between hands.
     * <p>
     * The quick-strike path (hammer in main hand + tool in offhand, handled by
     * {@link com.terravera.TerraVeraEventHandler#onRightClickBlock}) takes priority over this method, so players who
     * prefer the faster interaction keep it.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult)
    {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (player instanceof ServerPlayer serverPlayer)
        {
            NetworkHooks.openScreen(serverPlayer, new MenuProvider()
            {
                @Override
                public Component getDisplayName()
                {
                    return Component.translatable("block.terravera.workplate");
                }

                @Override
                public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player p)
                {
                    return new WorkplateContainer(windowId, inventory, pos);
                }
            }, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }
}
