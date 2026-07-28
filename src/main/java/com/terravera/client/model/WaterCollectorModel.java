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
import com.terravera.common.water.CollectorType;
import com.terravera.common.water.WaterCollectorBlock;
import com.terravera.common.water.WaterCollectorBlockEntity;

/** Shared GeckoLib model selector for the four layered water-collection structures. */
public class WaterCollectorModel<T extends GeoAnimatable> extends GeoModel<T>
{
    private final CollectorType itemType;

    /** Dynamic in-world renderer constructor. */
    public WaterCollectorModel()
    {
        this.itemType = CollectorType.ROCK_BASIN;
    }

    /** Fixed held-item renderer constructor. */
    public WaterCollectorModel(CollectorType itemType)
    {
        this.itemType = itemType;
    }

    private CollectorType type(T animatable)
    {
        return animatable instanceof WaterCollectorBlockEntity collector ? collector.collectorType() : itemType;
    }

    @Override
    public ResourceLocation getModelResource(T animatable)
    {
        return TerraVera.identifier("geo/" + type(animatable).id() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable)
    {
        return TerraVera.identifier("textures/block/water_collectors.png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable)
    {
        return TerraVera.identifier("animations/water_collector.animation.json");
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState)
    {
        super.setCustomAnimations(animatable, instanceId, animationState);
        final int level = animatable instanceof WaterCollectorBlockEntity collector
            && collector.getBlockState().hasProperty(WaterCollectorBlock.WATER_LEVEL)
            ? collector.getBlockState().getValue(WaterCollectorBlock.WATER_LEVEL) : 0;
        getBone("water").ifPresent(water -> water.setHidden(level == 0));
    }
}
