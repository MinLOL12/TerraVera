package com.terravera.common.items;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
 * BlockItem for TerraVera's GeckoLib objects, so machines and layered water collectors render as their full 3D
 * models while held, in the hand, and in the creative inventory - not as flat textures. The model
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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (getBlock() instanceof com.terravera.common.water.WaterCollectorBlock collector) {
            tooltip.add(Component.translatable("terravera.water_collector." + collector.collectorType().id() + "_hint")
                .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("terravera.water_collector.capacity", collector.collectorType().capacity())
                .withStyle(ChatFormatting.DARK_AQUA));
        }
        if (getBlock() instanceof com.terravera.common.sterilization.SterilizerBlock sterilizer) {
            final com.terravera.common.sterilization.SterilizerType type = sterilizer.sterilizerType();
            tooltip.add(Component.translatable("terravera.sterilizer." + type.id() + "_hint")
                .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("terravera.sterilizer.capacity", type.capacity())
                .withStyle(ChatFormatting.DARK_AQUA));
            tooltip.add(Component.translatable("terravera.sterilizer.process", type.processTicks() / 20)
                .withStyle(ChatFormatting.DARK_AQUA));
        }
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
