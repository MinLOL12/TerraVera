/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.sterilization;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import com.terravera.common.blocks.TerraVeraBlockEntities;
import com.terravera.common.health.WaterTreatment;

/**
 * Fluid storage and processing logic shared by all five sterilization machines.
 * <p>
 * The machine holds a batch of water in a tank, advances a timer while its working conditions are met (the SODIS rack
 * needs clear daylight; the others just need water), and flips the batch to the machine's treatment when the timer
 * completes. Filling and draining use TFC's own container handling, so TFC jugs, bottles, and buckets work with these
 * blocks exactly as they do with the collectors. The treatment state of anything drawn out is written onto the
 * container, which is what connects the machines to the disease system.
 */
public class SterilizerBlockEntity extends BlockEntity implements GeoBlockEntity
{
    /** Idle and working animation keys, cached once per machine. */
    private static final Map<String, RawAnimation> ANIMATIONS = new HashMap<>();

    private final SterilizerType type;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final FluidTank tank;
    private final IFluidHandler extractionView;

    private int progress;
    private boolean treated;
    private float sourceContamination;
    private WaterTreatment.Treatment tankTreatment = WaterTreatment.Treatment.UNTREATED;

    public SterilizerBlockEntity(BlockPos pos, BlockState state)
    {
        super(TerraVeraBlockEntities.STERILIZER.get(), pos, state);
        this.type = state.getBlock() instanceof SterilizerBlock sterilizer ? sterilizer.sterilizerType() : SterilizerType.CLARIFIER;
        this.tank = new FluidTank(type.capacity(), stack -> stack.is(FluidTags.WATER))
        {
            @Override
            protected void onContentsChanged()
            {
                SterilizerBlockEntity.this.onWaterChanged();
            }
        };
        this.extractionView = new IFluidHandler()
        {
            @Override public int getTanks() { return 1; }
            @Override public FluidStack getFluidInTank(int tankIndex) { return tank.getFluidInTank(tankIndex); }
            @Override public int getTankCapacity(int tankIndex) { return tank.getTankCapacity(tankIndex); }
            @Override public boolean isFluidValid(int tankIndex, FluidStack stack) { return false; }
            @Override public int fill(FluidStack resource, FluidAction action) { return 0; }
            @Override public FluidStack drain(FluidStack resource, FluidAction action) { return tank.drain(resource, action); }
            @Override public FluidStack drain(int maxDrain, FluidAction action) { return tank.drain(maxDrain, action); }
        };
    }

    public SterilizerType sterilizerType() { return type; }
    public int waterAmount() { return tank.getFluidAmount(); }
    public int capacity() { return tank.getCapacity(); }
    public int progress() { return progress; }
    public int processTicks() { return type.processTicks(); }
    public boolean isTreated() { return treated; }
    public IFluidHandler fluidHandler() { return extractionView; }

    /** Called when water has just been poured in. The batch restarts with the provenance of the newest addition. */
    public void onInputFilled(WaterTreatment incoming)
    {
        sourceContamination = incoming.sourceContamination();
        tankTreatment = incoming.treatment();
        // Water that is already at least as clean as this machine's output doesn't need reprocessing.
        treated = tankTreatment.rank() >= type.output().rank();
        progress = 0;
        setChanged();
    }

    /** Called when water has just been drawn out; stamps the machine's treatment state onto the container. */
    public void stampOutput(ItemStack filled)
    {
        if (!filled.isEmpty())
        {
            WaterTreatment.set(filled, new WaterTreatment(tankTreatment, sourceContamination));
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SterilizerBlockEntity sterilizer)
    {
        sterilizer.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state)
    {
        final boolean hasWater = !tank.isEmpty();
        if (!hasWater)
        {
            if (progress != 0 || treated || sourceContamination != 0f || tankTreatment != WaterTreatment.Treatment.UNTREATED)
            {
                progress = 0;
                treated = false;
                sourceContamination = 0f;
                tankTreatment = WaterTreatment.Treatment.UNTREATED;
                setChanged();
            }
            setActive(false);
            return;
        }

        if (treated)
        {
            setActive(false);
            return;
        }

        final boolean conditionsMet = !type.requiresSun()
            || (level.isDay() && level.canSeeSky(pos.above()));
        if (!conditionsMet)
        {
            setActive(false);
            return;
        }

        progress++;
        setActive(true);
        if (progress >= type.processTicks())
        {
            progress = 0;
            treated = true;
            tankTreatment = WaterTreatment.Treatment.best(tankTreatment, type.output());
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.7f, 1.3f);
            setChanged();
            setActive(false);
        }
    }

    private void setActive(boolean active)
    {
        if (level == null) return;
        final BlockState state = getBlockState();
        if (state.hasProperty(SterilizerBlock.ACTIVE) && state.getValue(SterilizerBlock.ACTIVE) != active)
        {
            level.setBlock(worldPosition, state.setValue(SterilizerBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
        }
    }

    private void onWaterChanged()
    {
        setChanged();
        if (level == null) return;

        final int amount = tank.getFluidAmount();
        final int visualLevel = amount == 0 ? 0 : Math.min(4, (amount * 4 + capacity() - 1) / capacity());
        final BlockState state = getBlockState();
        if (state.hasProperty(SterilizerBlock.WATER_LEVEL)
            && state.getValue(SterilizerBlock.WATER_LEVEL) != visualLevel)
        {
            level.setBlock(worldPosition, state.setValue(SterilizerBlock.WATER_LEVEL, visualLevel), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        super.loadAdditional(nbt, provider);
        tank.readFromNBT(provider, nbt.getCompound("tank"));
        progress = nbt.getInt("progress");
        treated = nbt.getBoolean("treated");
        sourceContamination = nbt.getFloat("contamination");
        tankTreatment = WaterTreatment.Treatment.byId(nbt.getString("tank_treatment"));
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        super.saveAdditional(nbt, provider);
        nbt.put("tank", tank.writeToNBT(provider, new CompoundTag()));
        nbt.putInt("progress", progress);
        nbt.putBoolean("treated", treated);
        nbt.putFloat("contamination", sourceContamination);
        nbt.putString("tank_treatment", tankTreatment.id());
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

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers)
    {
        controllers.add(new AnimationController<>(this, "sterilizer", 8, this::sterilizerAnimation));
    }

    private PlayState sterilizerAnimation(AnimationState<SterilizerBlockEntity> animation)
    {
        final BlockState state = getBlockState();
        final int level = state.hasProperty(SterilizerBlock.WATER_LEVEL)
            ? state.getValue(SterilizerBlock.WATER_LEVEL) : 0;
        final boolean active = state.hasProperty(SterilizerBlock.ACTIVE) && state.getValue(SterilizerBlock.ACTIVE);
        if (level > 0)
        {
            animation.getController().setAnimation(animationFor(type.id(), active));
            return PlayState.CONTINUE;
        }
        animation.getController().setAnimation(null);
        return PlayState.STOP;
    }

    private static RawAnimation animationFor(String id, boolean working)
    {
        final String key = id + (working ? ".working" : ".idle");
        return ANIMATIONS.computeIfAbsent(key,
            k -> RawAnimation.begin().thenLoop("animation.sterilizer." + k));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache()
    {
        return cache;
    }
}
