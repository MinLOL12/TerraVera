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
import com.terravera.common.blocks.AirConditionerBlock;
import com.terravera.common.blocks.TerraVeraBlockEntities;

/**
 * Animation host for the vapor-compression unit. The fan controller is a direct readout of the machine's state:
 * <ul>
 *     <li>no power or no circuit - the fan sits still,</li>
 *     <li>powered but the room is already at target - the fan idles slowly,</li>
 *     <li>actively pumping heat out of the room - the fan spins up fast, with a faint cabinet vibration.</li>
 * </ul>
 * Both flags are ordinary block states the server maintains, so what the player sees is what the grid is doing.
 */
public class AirConditionerBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation FAN_FAST = RawAnimation.begin().thenLoop("animation.air_conditioner.fan_fast");
    private static final RawAnimation FAN_SLOW = RawAnimation.begin().thenLoop("animation.air_conditioner.fan_slow");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AirConditionerBlockEntity(BlockPos pos, BlockState state) {
        super(TerraVeraBlockEntities.AIR_CONDITIONER.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "fan", 4, this::fanState));
    }

    private PlayState fanState(AnimationState<AirConditionerBlockEntity> state) {
        BlockState block = getBlockState();
        if (block.getValue(AirConditionerBlock.RUNNING)) {
            state.getController().setAnimation(FAN_FAST);
            return PlayState.CONTINUE;
        }
        if (block.getValue(AirConditionerBlock.POWERED)) {
            state.getController().setAnimation(FAN_SLOW);
            return PlayState.CONTINUE;
        }
        state.getController().setAnimation(null);
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
