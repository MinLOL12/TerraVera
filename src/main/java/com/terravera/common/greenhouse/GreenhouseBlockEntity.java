/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.greenhouse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import com.terravera.common.blocks.TerraVeraBlockEntities;

/**
 * Block entity for all four greenhouse tiers. Simulates the interior microclimate: temperature, humidity,
 * soil moisture, ventilation, and irrigation. The greenhouse is a living system, not a passive yield boost.
 */
public class GreenhouseBlockEntity extends BlockEntity implements GeoBlockEntity
{
    private static final RawAnimation VENT_OPEN_ANIM = RawAnimation.begin().thenLoop("animation.greenhouse.vent_open");
    private static final RawAnimation VENT_CLOSED_ANIM = RawAnimation.begin().thenPlay("animation.greenhouse.vent_closed");
    private static final RawAnimation MODERN_IDLE = RawAnimation.begin().thenLoop("animation.greenhouse.modern_idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private GreenhouseClimate climate;
    private int plantCount;
    private boolean ventilationOpen;
    private int tickCounter;

    public GreenhouseBlockEntity(BlockPos pos, BlockState state)
    {
        super(TerraVeraBlockEntities.GREENHOUSE.get(), pos, state);
        this.climate = GreenhouseClimate.DEFAULT;
        this.plantCount = 0;
        this.ventilationOpen = state.getValue(GreenhouseBlock.VENT_OPEN);
        this.tickCounter = 0;
    }

    public GreenhouseClimate climate() { return climate; }
    public int plantCount() { return plantCount; }

    public void setVentilationOpen(boolean open)
    {
        this.ventilationOpen = open;
        this.climate = climate.setVentilation(open ? 1.0f : 0.0f);
        setChanged();
    }

    public void setIrrigation(boolean active)
    {
        this.climate = climate.setIrrigation(active);
        setChanged();
    }

    public void setHeating(boolean on)
    {
        this.climate = climate.setHeating(on);
        setChanged();
    }

    public void setCooling(boolean on)
    {
        this.climate = climate.setCooling(on);
        setChanged();
    }

    public void setPlantCount(int count)
    {
        this.plantCount = count;
    }

    /**
     * Server tick: update the greenhouse climate. We only do real work every 100 ticks (5 seconds) because
     * climate changes are slow processes, not frame-by-frame calculations.
     */
    public void serverTick(Level level, BlockPos pos, BlockState state)
    {
        tickCounter++;
        if (tickCounter % 100 != 0) return;

        int tier = state.getValue(GreenhouseBlock.TIER);
        GreenhouseTier gTier = GreenhouseTier.byLevel(tier);
        // Note: use GreenhouseBlock.tierFromState(state) for other code paths to avoid
        // relying on the constructor-stored tier which may be incorrect after codec deserialization.

        // Determine outside conditions from the level
        float outsideTemp = getOutsideTemperature(level, pos);
        float outsideHumidity = level.isRaining() ? 0.85f : 0.45f;
        boolean daytime = level.isDay();
        float sunlight = daytime ? (level.canSeeSky(pos.above()) ? 0.8f : 0.3f) : 0.0f;
        boolean raining = level.isRaining();

        // Update ventilation from block state
        boolean ventState = state.getValue(GreenhouseBlock.VENT_OPEN);
        float ventRate = ventState ? (gTier.supportsVentilation() ? 0.8f : 0.4f) : 0.0f;

        // Update climate with the new effective rate
        GreenhouseClimate currentClimate = climate.setVentilation(ventRate);

        // Tick the climate model forward
        climate = currentClimate.tick(outsideTemp, outsideHumidity, sunlight, daytime, raining, plantCount);

        // Re-sync block state if ventilation changed
        if (ventState != ventilationOpen)
        {
            ventilationOpen = ventState;
        }

        // Modern greenhouses with automation: self-regulate ventilation if overheating
        if (gTier.supportsAutomation())
        {
            if (climate.isOverheating() && !ventState)
            {
                level.setBlock(pos, state.setValue(GreenhouseBlock.VENT_OPEN, true), Block.UPDATE_ALL);
            }
            else if (!climate.isOverheating() && ventState && climate.temperatureC() < 25.0f)
            {
                level.setBlock(pos, state.setValue(GreenhouseBlock.VENT_OPEN, false), Block.UPDATE_ALL);
            }
        }

        setChanged();
    }

    /**
     * Estimate outside temperature from the level. In a full TFC integration this would read TFC's climate data.
     * For now we use a simplified model based on biome and time.
     */
    private float getOutsideTemperature(Level level, BlockPos pos)
    {
        // Use biome temperature as a proxy. TFC's real climate would give precise values.
        float biomeTemp = level.getBiome(pos).value().getBaseTemperature();
        float baseTemp = biomeTemp * 30.0f - 5.0f; // Scale to roughly -5..25°C range

        // Seasonal variation (simplified - uses game day count)
        long day = level.getDayTime() / 24000L;
        float seasonal = (float) Math.sin(day * Math.PI * 2.0 / 120.0) * 8.0f;

        return baseTemp + seasonal;
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        super.loadAdditional(nbt, provider);
        if (nbt.contains("climate"))
        {
            climate = GreenhouseClimate.CODEC.parse(
                net.minecraft.nbt.NbtOps.INSTANCE, nbt.get("climate")).result().orElse(GreenhouseClimate.DEFAULT);
        }
        plantCount = nbt.getInt("plant_count");
        ventilationOpen = nbt.getBoolean("vent_open");
        tickCounter = nbt.getInt("tick_counter");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        super.saveAdditional(nbt, provider);
        GreenhouseClimate.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, climate)
            .result().ifPresent(tag -> nbt.put("climate", tag));
        nbt.putInt("plant_count", plantCount);
        nbt.putBoolean("vent_open", ventilationOpen);
        nbt.putInt("tick_counter", tickCounter);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider)
    {
        return saveCustomOnly(provider);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ----- GeckoLib animation -----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers)
    {
        controllers.add(new AnimationController<>(this, "greenhouse", 5, this::animate));
    }

    private PlayState animate(software.bernie.geckolib.animation.AnimationState<GreenhouseBlockEntity> state)
    {
        int tier = getBlockState().getValue(GreenhouseBlock.TIER);
        if (tier >= 3)
        {
            // Modern greenhouse has a subtle idle animation
            state.getController().setAnimation(MODERN_IDLE);
            return PlayState.CONTINUE;
        }
        if (ventilationOpen)
        {
            state.getController().setAnimation(VENT_OPEN_ANIM);
        }
        else
        {
            state.getController().setAnimation(VENT_CLOSED_ANIM);
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache()
    {
        return cache;
    }
}
