package com.terravera.common.items;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * BlockItem for TerraVera's GeckoLib-animated machines, so the wind turbine, air conditioner and hand crank render
 * as their full 3D models while held, in the hand, and in the creative inventory - not as flat textures. The model
 * supplier hands over the same geo model the in-world block renderer uses.
 */
public class GeoMachineItem extends BlockItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<GeoModel<GeoMachineItem>> model;

    public GeoMachineItem(Block block, Properties properties, Supplier<GeoModel<GeoMachineItem>> model) {
        super(block, properties);
        this.model = model;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // The item form is a static display; all real animation happens on the placed block entity.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<GeoMachineItem> renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new GeoItemRenderer<>(model.get());
                }
                return this.renderer;
            }
        });
    }
}
