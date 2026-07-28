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
 * Geo model for the wind turbine, shared by the in-world block renderer and the held-item renderer. The rotor bone
 * is animated by {@code animation.wind_turbine.spin} whenever the block's {@code spinning} state is true.
 */
public class WindTurbineModel<T extends GeoAnimatable> extends GeoModel<T> {
    @Override
    public ResourceLocation getModelResource(T animatable) {
        return TerraVera.identifier("geo/wind_turbine.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return TerraVera.identifier("textures/block/wind_turbine.png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return TerraVera.identifier("animations/wind_turbine.animation.json");
    }
}
