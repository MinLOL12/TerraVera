/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.greenhouse;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
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
    /**
     * The planted trays. This replaces the old bare {@code trayPlantCount} counter: a count could record that a
     * seed had gone in but not what it was or whether it had finished, which is why nothing was ever collectable.
     */
    private final List<GreenhouseTray> trays = new ArrayList<>();
    private int nearbyCropCount;
    private float sunlightExposure;
    private boolean ventilationOpen;
    private int tickCounter;

    public GreenhouseBlockEntity(BlockPos pos, BlockState state)
    {
        super(TerraVeraBlockEntities.GREENHOUSE.get(), pos, state);
        int tier = state.getValue(GreenhouseBlock.TIER);
        this.climate = new GreenhouseClimate(tier, 20.0f, 0.5f, 0.0f, false, 0.3f,
            GreenhouseTier.byLevel(tier).solarCapture(), 0.0f, false, false);
        this.plantCount = 0;
        this.nearbyCropCount = 0;
        this.sunlightExposure = 0.0f;
        this.ventilationOpen = state.getValue(GreenhouseBlock.VENT_OPEN);
        this.tickCounter = 0;
    }

    public GreenhouseClimate climate() { return climate; }
    public int plantCount() { return plantCount; }
    public int trayPlantCount() { return trays.size(); }
    public List<GreenhouseTray> trays() { return trays; }

    /** How many trays have finished and are waiting to be collected. */
    public int matureTrayCount()
    {
        return (int) trays.stream().filter(GreenhouseTray::mature).count();
    }

    /** Growth of the least advanced unfinished tray, for the GUI progress readout. 1 when all are done. */
    public float trayProgress()
    {
        return trays.stream().filter(tray -> !tray.mature())
            .map(GreenhouseTray::progress)
            .min(Float::compare)
            .orElse(trays.isEmpty() ? 0f : 1f);
    }
    public int nearbyCropCount() { return nearbyCropCount; }
    public float sunlightExposure() { return sunlightExposure; }
    public boolean ventilationOpen() { return ventilationOpen; }

    public int trayCapacity()
    {
        return switch (GreenhouseBlock.tierFromState(getBlockState()))
        {
            case COLD_FRAME -> 4;
            case HOOP_HOUSE -> 8;
            case GLASS_GREENHOUSE -> 18;
            case MODERN_GREENHOUSE -> 32;
        };
    }

    public boolean tryPlantSeed(ItemStack seed, Player player)
    {
        if (trays.size() >= trayCapacity())
        {
            player.displayClientMessage(Component.translatable("terravera.greenhouse.plant_full"), true);
            return false;
        }
        trays.add(GreenhouseTray.sow(seed));
        plantCount = nearbyCropCount + trays.size();
        setChanged();
        player.displayClientMessage(
            Component.translatable("terravera.greenhouse.seed_planted", trays.size(), trayCapacity()), true);
        return true;
    }

    /**
     * Collect everything that has finished growing.
     * <p>
     * Yield scales with the greenhouse's climate at the moment of harvest rather than being a flat one-per-tray:
     * a well-run glasshouse should pay back the glass. A tray that is collected is emptied, so the player replants
     * it - the greenhouse is a workspace, not an automatic farm.
     *
     * @return the harvested stacks, empty if nothing was ready
     */
    public List<ItemStack> harvestMatureTrays()
    {
        final List<ItemStack> harvested = new ArrayList<>();
        if (level == null) return harvested;

        final float quality = climate.growthModifier();
        trays.removeIf(tray -> {
            if (!tray.mature()) return false;
            final int count = 1 + (quality >= 0.85f ? 2 : quality >= 0.55f ? 1 : 0);
            final ItemStack yield = tray.harvest(count);
            if (!yield.isEmpty()) harvested.add(yield);
            return true;
        });

        if (!harvested.isEmpty())
        {
            plantCount = nearbyCropCount + trays.size();
            setChanged();
        }
        return harvested;
    }

    public void setVentilationOpen(boolean open)
    {
        this.ventilationOpen = open;
        this.climate = climate.setVentilation(open ? 1.0f : 0.0f);
        if (level != null && getBlockState().hasProperty(GreenhouseBlock.VENT_OPEN)
            && getBlockState().getValue(GreenhouseBlock.VENT_OPEN) != open)
        {
            level.setBlock(worldPosition, getBlockState().setValue(GreenhouseBlock.VENT_OPEN, open), Block.UPDATE_ALL);
        }
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

        int scanRadius = scanRadius(gTier);
        nearbyCropCount = countNearbyCrops(level, pos, scanRadius);
        plantCount = nearbyCropCount + trays.size();

        // Determine outside conditions from the level
        float outsideTemp = getOutsideTemperature(level, pos);
        float outsideHumidity = level.isRaining() ? 0.85f : 0.45f;
        boolean daytime = level.isDay();
        float glassCoverage = calculateGlassCoverage(level, pos, gTier, scanRadius);
        sunlightExposure = daytime ? calculateSunlightExposure(level, pos, scanRadius) * (0.5f + glassCoverage * 0.5f) : 0.0f;
        boolean raining = level.isRaining();

        // Update ventilation from block state
        boolean ventState = state.getValue(GreenhouseBlock.VENT_OPEN);
        float ventRate = ventState ? (gTier.supportsVentilation() ? 0.8f : 0.4f) : 0.0f;

        // Update climate with current tier and scanned glass coverage. This lets a player build a larger glass shell
        // around the controller; sunlight and warmth pass through glass instead of being blocked by canSeeSky().
        GreenhouseClimate currentClimate = new GreenhouseClimate(tier, climate.temperatureC(), climate.humidity(),
            ventRate, climate.irrigationActive(), climate.soilMoisture(), glassCoverage, climate.orientationBonus(),
            climate.heatingOn(), climate.coolingOn());

        // Tick the climate model forward
        climate = currentClimate.tick(outsideTemp, outsideHumidity, sunlightExposure, daytime, raining, plantCount);

        // Re-sync block state if ventilation changed
        if (ventState != ventilationOpen)
        {
            ventilationOpen = ventState;
        }

        // Grow the planted trays against the climate we just simulated, rather than against a fixed rate. This is
        // the step that makes the climate controls matter: badly managed glass simply does not finish a crop.
        final String season = CropSpecialization.currentSeason(level.getDayTime());
        for (int i = 0; i < trays.size(); i++)
        {
            trays.set(i, trays.get(i).grown(climate, season));
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

    private int scanRadius(GreenhouseTier tier)
    {
        return switch (tier)
        {
            case COLD_FRAME -> 2;
            case HOOP_HOUSE -> 4;
            case GLASS_GREENHOUSE -> 6;
            case MODERN_GREENHOUSE -> 8;
        };
    }

    private int countNearbyCrops(Level level, BlockPos center, int radius)
    {
        int count = 0;
        for (int dx = -radius; dx <= radius; dx++)
        {
            for (int dz = -radius; dz <= radius; dz++)
            {
                for (int dy = -1; dy <= 2; dy++)
                {
                    BlockState cropState = level.getBlockState(center.offset(dx, dy, dz));
                    if (cropState.getBlock() instanceof CropBlock) count++;
                }
            }
        }
        return count;
    }

    private float calculateGlassCoverage(Level level, BlockPos center, GreenhouseTier tier, int radius)
    {
        int glassBlocks = 0;
        int structureBlocks = 0;
        for (int dx = -radius; dx <= radius; dx++)
        {
            for (int dz = -radius; dz <= radius; dz++)
            {
                for (int dy = 0; dy <= 5; dy++)
                {
                    BlockState scan = level.getBlockState(center.offset(dx, dy, dz));
                    if (isGlassLike(scan)) glassBlocks++;
                    else if (scan.getBlock() instanceof GreenhouseBlock) structureBlocks++;
                }
            }
        }

        float requiredGlass = switch (tier)
        {
            case COLD_FRAME -> 2.0f;
            case HOOP_HOUSE -> 6.0f;
            case GLASS_GREENHOUSE -> 18.0f;
            case MODERN_GREENHOUSE -> 28.0f;
        };
        float scannedCoverage = Math.min(1.0f, glassBlocks / requiredGlass);
        float controllerCoverage = Math.min(1.0f, tier.solarCapture() + structureBlocks * 0.02f);
        return Math.max(controllerCoverage, scannedCoverage);
    }

    private float calculateSunlightExposure(Level level, BlockPos center, int radius)
    {
        float total = 0.0f;
        int samples = 0;

        for (int dx = -radius; dx <= radius; dx++)
        {
            for (int dz = -radius; dz <= radius; dz++)
            {
                for (int dy = -1; dy <= 1; dy++)
                {
                    BlockPos base = center.offset(dx, dy, dz);
                    BlockState baseState = level.getBlockState(base);
                    if (baseState.getBlock() instanceof CropBlock)
                    {
                        total += sunlightThroughGlass(level, base.above());
                        samples++;
                    }
                    else if (isPlantableSoil(baseState))
                    {
                        total += sunlightThroughGlass(level, base.above());
                        samples++;
                    }
                }
            }
        }

        if (samples == 0)
        {
            return sunlightThroughGlass(level, center.above());
        }
        return total / samples;
    }

    private float sunlightThroughGlass(Level level, BlockPos start)
    {
        int glassLayers = 0;
        int maxY = Math.min(level.getMaxBuildHeight(), start.getY() + 32);
        for (int y = start.getY(); y < maxY; y++)
        {
            BlockPos scanPos = new BlockPos(start.getX(), y, start.getZ());
            if (level.canSeeSky(scanPos))
            {
                return Math.max(0.2f, 0.85f - glassLayers * 0.05f);
            }

            BlockState scan = level.getBlockState(scanPos);
            if (scan.isAir()) continue;
            if (isGlassLike(scan))
            {
                glassLayers++;
                continue;
            }
            if (!scan.canOcclude() && scan.getLightBlock(level, scanPos) < 15)
            {
                continue;
            }
            return 0.0f;
        }
        return 0.0f;
    }

    private boolean isPlantableSoil(BlockState state)
    {
        return state.getBlock() instanceof com.terravera.common.farming.PreparedFarmlandBlock
            || state.is(net.minecraft.world.level.block.Blocks.FARMLAND)
            || state.is(net.minecraft.tags.BlockTags.DIRT);
    }

    private boolean isGlassLike(BlockState state)
    {
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return path.contains("glass") || path.contains("greenhouse") || state.is(net.minecraft.world.level.block.Blocks.GLASS)
            || state.is(net.minecraft.world.level.block.Blocks.GLASS_PANE);
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
        trays.clear();
        final ListTag trayList = nbt.getList("trays", Tag.TAG_COMPOUND);
        for (int i = 0; i < trayList.size(); i++)
        {
            trays.add(GreenhouseTray.load(trayList.getCompound(i)));
        }
        nearbyCropCount = nbt.getInt("nearby_crop_count");
        sunlightExposure = nbt.getFloat("sunlight_exposure");
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
        final ListTag trayList = new ListTag();
        for (GreenhouseTray tray : trays) trayList.add(tray.save());
        nbt.put("trays", trayList);
        nbt.putInt("nearby_crop_count", nearbyCropCount);
        nbt.putFloat("sunlight_exposure", sunlightExposure);
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
