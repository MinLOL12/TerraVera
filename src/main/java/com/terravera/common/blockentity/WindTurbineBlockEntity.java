package com.terravera.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
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
import com.terravera.common.power.WindTurbineBlock;

/**
 * Client-side animation host for the wind turbine. The server updates the {@code spinning} block state once a
 * second from the sky check; the rotor controller reads that state and spins the blades while it is true,
 * coasting to a stop through the controller's transition when the wind is cut off.
 */
public class WindTurbineBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("animation.wind_turbine.spin");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public WindTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(TerraVeraBlockEntities.WIND_TURBINE.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Five-tick transition lets the rotor ease up to speed and coast down instead of snapping.
        controllers.add(new AnimationController<>(this, "rotor", 5, this::rotorState));
    }

    private PlayState rotorState(AnimationState<WindTurbineBlockEntity> state) {
        BlockState block = getBlockState();
        if (block.hasProperty(WindTurbineBlock.SPINNING) && block.getValue(WindTurbineBlock.SPINNING)) {
            state.getController().setAnimation(SPIN);
            return PlayState.CONTINUE;
        }
        state.getController().setAnimation(null);
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** Periodically re-evaluates open sky above the mast and publishes it on the block state. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, WindTurbineBlockEntity blockEntity) {
        if (level.getGameTime() % 20 != 0) return;
        boolean spinning = level.canSeeSky(pos.above());
        if (state.getValue(WindTurbineBlock.SPINNING) != spinning) {
            level.setBlock(pos, state.setValue(WindTurbineBlock.SPINNING, spinning), 3);
        }
    }
}
