/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 */

package com.terravera.client.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import com.terravera.TerraVera;

/**
 * Geo model for all greenhouse tiers. The vent bones animate open/closed, the modern greenhouse has a subtle
 * climate-control panel animation, and the cold frame has a simple lid-lift animation.
 */
public class GreenhouseModel<T extends GeoAnimatable> extends GeoModel<T> {
    private final String variant;

    public GreenhouseModel(String variant) {
        this.variant = variant;
    }

    public GreenhouseModel() {
        this("cold_frame");
    }

    private String getActualVariant(T animatable) {
        if (animatable instanceof BlockEntity be) {
            String name = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()).getPath();
            if (name.equals("cold_frame") || name.equals("hoop_house") || name.equals("glass_greenhouse") || name.equals("modern_greenhouse")) {
                return name;
            }
        }
        return variant;
    }

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return TerraVera.identifier("geo/" + getActualVariant(animatable) + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return TerraVera.identifier("textures/block/" + getActualVariant(animatable) + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return TerraVera.identifier("animations/greenhouse.animation.json");
    }
}
