package com.terravera.common.power;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Manual emergency power. Each turn keeps the crank generating for five seconds. */
public class HandCrankBlock extends Block {
    private static final Map<Level, Map<Long, Long>> TURNS = new WeakHashMap<>();
    public HandCrankBlock(Properties properties) { super(properties); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            TURNS.computeIfAbsent(level, ignored -> new ConcurrentHashMap<>()).put(pos.asLong(), level.getGameTime() + 100);
        }
        player.swing(InteractionHand.MAIN_HAND);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
    static boolean isTurning(Level level, BlockPos pos) {
        return TURNS.getOrDefault(level, Map.of()).getOrDefault(pos.asLong(), 0L) > level.getGameTime();
    }
}
