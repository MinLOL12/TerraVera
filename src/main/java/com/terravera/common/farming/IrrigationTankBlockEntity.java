/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.farming;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import com.terravera.common.blocks.TerraVeraBlockEntities;

/**
 * A rainwater storage tank that feeds drip irrigation or provides water for watering cans. Collects rain from its
 * open top and stores it for later use. Capacity depends on the construction material (wood barrel, stone cistern,
 * or modern metal tank).
 */
public class IrrigationTankBlockEntity extends BlockEntity implements GeoBlockEntity
{
    private static final int MAX_WATER = 4000; // mB

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int waterStored;
    private int tickCounter;

    public IrrigationTankBlockEntity(BlockPos pos, BlockState state)
    {
        super(TerraVeraBlockEntities.IRRIGATION_TANK.get(), pos, state);
        this.waterStored = 0;
        this.tickCounter = 0;
    }

    public int waterStored() { return waterStored; }
    public int capacity() { return MAX_WATER; }
    public float fillFraction() { return (float) waterStored / MAX_WATER; }

    /**
     * Server tick: collect rain and distribute water to connected drip irrigation below.
     */
    public void serverTick(Level level, BlockPos pos, BlockState state)
    {
        tickCounter++;

        // Collect rain every second if it's raining and sky is visible
        if (tickCounter % 20 == 0 && level.isRainingAt(pos.above()) && waterStored < MAX_WATER)
        {
            waterStored = Math.min(MAX_WATER, waterStored + 50);
            setChanged();
        }

        // Distribute water to drip irrigation below every 5 seconds
        if (tickCounter % 100 == 0 && waterStored > 0)
        {
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            if (belowState.getBlock() instanceof DripIrrigationBlock)
            {
                DripIrrigationBlock.distributeWater(level, below);
                waterStored = Math.max(0, waterStored - 100);
                setChanged();
            }
        }
    }

    /** Add water to the tank (from a bucket or other source). */
    public int addWater(int amount)
    {
        int accepted = Math.min(amount, MAX_WATER - waterStored);
        waterStored += accepted;
        setChanged();
        return accepted;
    }

    /** Remove water from the tank (for irrigation or watering can filling). */
    public int removeWater(int amount)
    {
        int removed = Math.min(amount, waterStored);
        waterStored -= removed;
        setChanged();
        return removed;
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        super.loadAdditional(nbt, provider);
        waterStored = nbt.getInt("water_stored");
        tickCounter = nbt.getInt("tick_counter");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        super.saveAdditional(nbt, provider);
        nbt.putInt("water_stored", waterStored);
        nbt.putInt("tick_counter", tickCounter);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) { return saveCustomOnly(provider); }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
