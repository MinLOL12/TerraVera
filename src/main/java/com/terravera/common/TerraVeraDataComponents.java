/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common;

import java.util.function.UnaryOperator;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.terravera.TerraVera;
import com.terravera.common.component.BarkProperties;
import com.terravera.common.component.Cordage;
import com.terravera.common.component.Adhesive;
import com.terravera.common.component.KnappedHead;
import com.terravera.common.component.ToolMetalState;
import com.terravera.common.component.ToolGrip;
import com.terravera.common.butchery.CarcassData;
import com.terravera.common.quality.CropHealth;
import com.terravera.common.quality.MaterialQuality;
import com.terravera.common.quality.SoilCondition;
import com.terravera.common.greenhouse.GreenhouseClimate;

public final class TerraVeraDataComponents
{
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, TerraVera.MOD_ID);

    /** Moisture and species-dependent material properties carried by harvested bark sheets. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BarkProperties>> BARK_PROPERTIES =
        register("bark_properties", builder -> builder.persistent(BarkProperties.CODEC).networkSynchronized(BarkProperties.STREAM_CODEC));

    /** Attached to knapped tool heads. Describes what kind of working end was produced, and how well. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<KnappedHead>> KNAPPED_HEAD =
        register("knapped_head", builder -> builder.persistent(KnappedHead.CODEC).networkSynchronized(KnappedHead.STREAM_CODEC));

    /** Attached to cordage, and to tools hafted with it. Determines how well the lashing holds and how long it is. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Cordage>> CORDAGE =
        register("cordage", builder -> builder.persistent(Cordage.CODEC).networkSynchronized(Cordage.STREAM_CODEC));

    /** Prepared glue carried by a batch and retained by a glued tool joint. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Adhesive>> ADHESIVE =
        register("adhesive", builder -> builder.persistent(Adhesive.CODEC).networkSynchronized(Adhesive.STREAM_CODEC));

    /** Attached to repaired metal tools. Tracks remaining metal mass and the current smithing operation. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ToolMetalState>> TOOL_METAL_STATE =
        register("tool_metal_state", builder -> builder.persistent(ToolMetalState.CODEC).networkSynchronized(ToolMetalState.STREAM_CODEC));

    /** Attached to a hafted tool after leather or rubber has been fitted over its wooden handle. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ToolGrip>> TOOL_GRIP =
        register("tool_grip", builder -> builder.persistent(ToolGrip.CODEC).networkSynchronized(ToolGrip.STREAM_CODEC));

    /** Attached to hafted tools. Stores the speed modifier from cordage binding quality. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BindingBonus>> BINDING_SPEED_BONUS =
        register("binding_speed_bonus", builder -> builder.persistent(BindingBonus.CODEC).networkSynchronized(BindingBonus.STREAM_CODEC));

    /** Attached to hafted tools. Stores the damage modifier from cordage binding quality. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DamageBonus>> BINDING_DAMAGE_BONUS =
        register("binding_damage_bonus", builder -> builder.persistent(DamageBonus.CODEC).networkSynchronized(DamageBonus.STREAM_CODEC));

    /**
     * Attached to anything that holds drinking water. Records where the water came from and what has since been done
     * to it, so that a jug of boiled spring water and a jug of swamp water are not the same item.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.terravera.common.health.WaterTreatment>> WATER_TREATMENT =
        register("water_treatment", builder -> builder
            .persistent(com.terravera.common.health.WaterTreatment.CODEC)
            .networkSynchronized(com.terravera.common.health.WaterTreatment.STREAM_CODEC));

    /**
     * Attached to worn garments. A soaked coat stays soaked until it is dried, which is what turns "dry your clothes"
     * into a real decision rather than a slogan, and makes carrying a spare set worthwhile.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.terravera.common.temperature.Wetness>> GARMENT_WETNESS =
        register("garment_wetness", builder -> builder
            .persistent(com.terravera.common.temperature.Wetness.CODEC)
            .networkSynchronized(com.terravera.common.temperature.Wetness.STREAM_CODEC));

    /**
     * Attached to natural materials (sticks, fibre, wood, clay). Carries the quality and moisture of the raw
     * material, which affects crafting results and burn characteristics.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MaterialQuality>> MATERIAL_QUALITY =
        register("material_quality", builder -> builder.persistent(MaterialQuality.CODEC).networkSynchronized(MaterialQuality.STREAM_CODEC));

    /**
     * Attached to carcass items. Records what the animal was, when it died, how far it has been butchered, and how
     * good the knife work has been so far. Everything the butchering system decides is derived from this.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CarcassData>> CARCASS =
        register("carcass", builder -> builder.persistent(CarcassData.CODEC).networkSynchronized(CarcassData.STREAM_CODEC));

    /**
     * Attached to prepared farmland block entities. Tracks how well the soil has been cleared, loosened,
     * fertilized, and weeded. Soil quality directly affects crop growth and yield.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SoilCondition>> SOIL_CONDITION =
        register("soil_condition", builder -> builder.persistent(SoilCondition.CODEC).networkSynchronized(SoilCondition.STREAM_CODEC));

    /**
     * Attached to crop block entities. Tracks vigour, disease pressure, pest damage, and nutrient levels.
     * Unhealthy crops grow slowly, yield poorly, and can die.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CropHealth>> CROP_HEALTH =
        register("crop_health", builder -> builder.persistent(CropHealth.CODEC).networkSynchronized(CropHealth.STREAM_CODEC));

    /**
     * Attached to greenhouse block entities. The complete climate state: temperature, humidity, ventilation,
     * irrigation, heating, and cooling.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GreenhouseClimate>> GREENHOUSE_CLIMATE =
        register("greenhouse_climate", builder -> builder.persistent(GreenhouseClimate.CODEC).networkSynchronized(GreenhouseClimate.STREAM_CODEC));

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(
        String name, UnaryOperator<DataComponentType.Builder<T>> builder)
    {
        return COMPONENTS.register(name, () -> builder.apply(DataComponentType.builder()).build());
    }

    private TerraVeraDataComponents() {}
    
    /**
     * Data component for speed bonus from binding quality.
     */
    public record BindingBonus(float speedModifier, float bindingQuality)
    {
        public static final Codec<BindingBonus> CODEC = Codec.FLOAT.xmap(
            f -> new BindingBonus(f, f),
            b -> b.speedModifier()
        );
        
        public static final StreamCodec<RegistryFriendlyByteBuf, BindingBonus> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, BindingBonus::speedModifier,
            ByteBufCodecs.FLOAT, BindingBonus::bindingQuality,
            BindingBonus::new
        );
    }
    
    /**
     * Data component for damage bonus from binding quality.
     */
    public record DamageBonus(float damageModifier, float bindingQuality)
    {
        public static final Codec<DamageBonus> CODEC = Codec.FLOAT.xmap(
            f -> new DamageBonus(f, f),
            b -> b.damageModifier()
        );
        
        public static final StreamCodec<RegistryFriendlyByteBuf, DamageBonus> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, DamageBonus::damageModifier,
            ByteBufCodecs.FLOAT, DamageBonus::bindingQuality,
            DamageBonus::new
        );
    }
}
