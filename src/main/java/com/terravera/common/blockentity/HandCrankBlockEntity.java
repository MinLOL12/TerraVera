package com.terravera.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import com.terravera.common.blocks.TerraVeraBlockEntities;
import com.terravera.common.power.HandCrankBlock;

/**
 * Animation host for the hand crank. Two things happen on this controller:
 * <ol>
 *     <li>Each interaction triggers a one-shot full revolution of the handle ({@link #turn()}), synced from the
 *     server through GeckoLib's block-entity animation packet, so every click visibly spins the handle once.</li>
 *     <li>While the crank is supplying power the handle keeps a slow, steady idle rotation, making it obvious at a
 *     glance that the machine is working.</li>
 * </ol>
 */
public class HandCrankBlockEntity extends BlockEntity implements GeoBlockEntity {
    public static final String CONTROLLER = "crank";
    public static final String TURN_ANIM = "turn";
    private static final RawAnimation TURN = RawAnimation.begin().thenPlay("animation.hand_crank.turn");
    private static final RawAnimation DRIVING = RawAnimation.begin().thenLoop("animation.hand_crank.driving");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public HandCrankBlockEntity(BlockPos pos, BlockState state) {
        super(TerraVeraBlockEntities.HAND_CRANK.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, 2, this::crankState)
            .triggerableAnim(TURN_ANIM, TURN));
    }

    private PlayState crankState(AnimationState<HandCrankBlockEntity> state) {
        if (getLevel() != null && HandCrankBlock.isTurning(getLevel(), getBlockPos())) {
            state.getController().setAnimation(DRIVING);
            return PlayState.CONTINUE;
        }
        state.getController().setAnimation(null);
        return PlayState.STOP;
    }

    /** Server-side entry point: plays the one-shot handle revolution on every client that can see the crank. */
    public void turn() {
        triggerAnim(CONTROLLER, TURN_ANIM);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
