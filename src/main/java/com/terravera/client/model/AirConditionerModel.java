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
 * Geo model for the vapor-compression air conditioner. The fan bone under the roof grille idles or spins fast
 * depending on the unit's {@code powered}/{@code running} block states.
 */
public class AirConditionerModel<T extends GeoAnimatable> extends GeoModel<T> {
    @Override
    public ResourceLocation getModelResource(T animatable) {
        return TerraVera.identifier("geo/air_conditioner.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return TerraVera.identifier("textures/block/air_conditioner_unit.png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return TerraVera.identifier("animations/air_conditioner.animation.json");
    }
}
