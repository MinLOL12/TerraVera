/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 */

package com.terravera.client.model;

import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import net.minecraft.resources.ResourceLocation;

import com.terravera.TerraVera;
import com.terravera.common.sterilization.SterilizerBlock;
import com.terravera.common.sterilization.SterilizerBlockEntity;
import com.terravera.common.sterilization.SterilizerType;

/**
 * Shared GeckoLib model selector for the five animated sterilization machines. Each machine has its own geometry and
 * its own texture atlas; the animation file is shared because every machine's idle and working loops live in one
 * place.
 */
public class SterilizerModel<T extends GeoAnimatable> extends GeoModel<T>
{
    private final SterilizerType itemType;

    /** Dynamic in-world renderer constructor. */
    public SterilizerModel()
    {
        this.itemType = SterilizerType.CLARIFIER;
    }

    /** Fixed held-item renderer constructor. */
    public SterilizerModel(SterilizerType itemType)
    {
        this.itemType = itemType;
    }

    private SterilizerType type(T animatable)
    {
        return animatable instanceof SterilizerBlockEntity sterilizer ? sterilizer.sterilizerType() : itemType;
    }

    @Override
    public ResourceLocation getModelResource(T animatable)
    {
        return TerraVera.identifier("geo/" + type(animatable).id() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable)
    {
        return TerraVera.identifier("textures/block/" + type(animatable).id() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable)
    {
        return TerraVera.identifier("animations/sterilizer.animation.json");
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState)
    {
        super.setCustomAnimations(animatable, instanceId, animationState);
        final int level = animatable instanceof SterilizerBlockEntity sterilizer
            && sterilizer.getBlockState().hasProperty(SterilizerBlock.WATER_LEVEL)
            ? sterilizer.getBlockState().getValue(SterilizerBlock.WATER_LEVEL) : 0;
        getBone("water").ifPresent(water -> water.setHidden(level == 0));
    }
}
