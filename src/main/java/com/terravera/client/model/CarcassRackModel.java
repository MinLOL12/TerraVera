/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.client.model;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import com.terravera.TerraVera;
import com.terravera.common.butchery.CarcassRackBlockEntity;

/**
 * GeoModel for the Carcass Rack block entity and item.
 * <p>
 * Displays the hanging rack frame and any suspended animal carcass.
 * As the player uses a Butcher's Knife on the hanging carcass, {@link #setCustomAnimations}
 * dynamically hides anatomical layer bones (hide, head, organs, primal muscle meat, fat/sinew)
 * and scales remaining tissue cubes by the waste/wear fraction so that the animal's pixels
 * wear off realistically layer by layer down to the bare skeleton.
 */
public class CarcassRackModel<T extends GeoAnimatable> extends GeoModel<T>
{
    @Override
    public ResourceLocation getModelResource(T animatable)
    {
        return TerraVera.identifier("geo/carcass_rack.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable)
    {
        if (animatable instanceof CarcassRackBlockEntity be && be.hasCarcass())
        {
            return TerraVera.identifier("textures/block/carcass_rack/" + be.getSpeciesId() + ".png");
        }
        return TerraVera.identifier("textures/block/carcass_rack/default.png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable)
    {
        return TerraVera.identifier("animations/carcass_rack.animation.json");
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState)
    {
        super.setCustomAnimations(animatable, instanceId, animationState);

        if (animatable instanceof CarcassRackBlockEntity be)
        {
            final boolean hasCarcass = be.hasCarcass();
            setBoneHidden("carcass_body", !hasCarcass);
            if (!hasCarcass) return;

            final String stage = be.getStageId();
            final float waste = be.getWaste();

            final boolean hasHide = "intact".equals(stage) || "bled".equals(stage);
            final boolean hasBlood = "bled".equals(stage);
            final boolean hasOrgans = hasHide || "skinned".equals(stage);
            final boolean hasMeat = hasOrgans || "eviscerated".equals(stage);
            final boolean hasFat = hasMeat || "primals".equals(stage);

            setBoneHidden("carcass_hide", !hasHide);
            setBoneHidden("carcass_head", !hasHide);
            setBoneHidden("carcass_blood_marks", !hasBlood);
            setBoneHidden("carcass_organs", !hasOrgans);
            setBoneHidden("carcass_meat_shoulder", !hasMeat);
            setBoneHidden("carcass_meat_ribs", !hasMeat);
            setBoneHidden("carcass_meat_loin", !hasMeat);
            setBoneHidden("carcass_meat_leg", !hasMeat);
            setBoneHidden("carcass_fat_sinew", !hasFat);
            setBoneHidden("carcass_skeleton", false);

            final float wearScale = Math.max(0.65f, 1.0f - waste * 0.35f);
            scaleBone("carcass_meat_shoulder", wearScale);
            scaleBone("carcass_meat_ribs", wearScale);
            scaleBone("carcass_meat_loin", wearScale);
            scaleBone("carcass_meat_leg", wearScale);
            scaleBone("carcass_fat_sinew", wearScale);
        }
        else
        {
            // Item display in inventory: hide the animal carcass bones, show the rack frame.
            setBoneHidden("carcass_body", true);
        }
    }

    private void setBoneHidden(String boneName, boolean hidden)
    {
        final GeoBone bone = getAnimationProcessor().getBone(boneName);
        if (bone != null)
        {
            bone.setHidden(hidden);
        }
    }

    private void scaleBone(String boneName, float scale)
    {
        final GeoBone bone = getAnimationProcessor().getBone(boneName);
        if (bone != null)
        {
            bone.setScaleX(scale);
            bone.setScaleZ(scale);
        }
    }
}
