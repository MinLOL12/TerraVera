/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.client.model;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import com.terravera.TerraVera;

/**
 * Geo model for the hand crank generator. The crank bone carries the arm and the turned wooden grip; each
 * interaction plays a one-shot full revolution, and a slow driving loop runs while the crank is supplying power.
 */
public class HandCrankModel<T extends GeoAnimatable> extends GeoModel<T> {
    @Override
    public ResourceLocation getModelResource(T animatable) {
        return TerraVera.identifier("geo/hand_crank.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return TerraVera.identifier("textures/block/hand_crank_unit.png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return TerraVera.identifier("animations/hand_crank.animation.json");
    }
}
