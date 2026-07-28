/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 */

package com.terravera.common.water;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
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

/** Fluid storage and collection logic shared by all four passive collector models. */
public class WaterCollectorBlockEntity extends BlockEntity implements GeoBlockEntity
{
    private static final RawAnimation RIPPLE = RawAnimation.begin().thenLoop("animation.water_collector.ripple");

    private final CollectorType collectorType;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final FluidTank tank;
    private final IFluidHandler extractionView;

    public WaterCollectorBlockEntity(BlockPos pos, BlockState state)
    {
        super(TerraVeraBlockEntities.WATER_COLLECTOR.get(), pos, state);
        this.collectorType = state.getBlock() instanceof WaterCollectorBlock collector
            ? collector.collectorType() : CollectorType.ROCK_BASIN;
        this.tank = new FluidTank(collectorType.capacity(), stack -> stack.is(FluidTags.WATER))
        {
            @Override
            protected void onContentsChanged()
            {
                WaterCollectorBlockEntity.this.onWaterChanged();
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

    public CollectorType collectorType() { return collectorType; }
    public int waterAmount() { return tank.getFluidAmount(); }
    public int capacity() { return tank.getCapacity(); }
    public IFluidHandler fluidHandler() { return extractionView; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WaterCollectorBlockEntity collector)
    {
        if (level.getGameTime() % 20 != 0 || collector.tank.getFluidAmount() >= collector.capacity()) return;
        if (!level.canSeeSky(pos.above())) return;

        final boolean collect = switch (collector.collectorType)
        {
            case RAIN_CATCHER, ROCK_BASIN -> level.isRainingAt(pos.above());
            case DEW_COLLECTOR -> !level.isDay() && !level.isRainingAt(pos.above());
            case SOLAR_STILL -> level.isDay() && !level.isRainingAt(pos.above());
        };
        if (!collect) return;

        collector.tank.fill(new FluidStack(Fluids.WATER,
            Math.min(collector.collectorType.rate(), collector.capacity() - collector.waterAmount())),
            IFluidHandler.FluidAction.EXECUTE);
    }

    private void onWaterChanged()
    {
        setChanged();
        if (level == null) return;

        final int amount = tank.getFluidAmount();
        final int visualLevel = amount == 0 ? 0 : Math.min(4, (amount * 4 + capacity() - 1) / capacity());
        final BlockState state = getBlockState();
        if (state.hasProperty(WaterCollectorBlock.WATER_LEVEL)
            && state.getValue(WaterCollectorBlock.WATER_LEVEL) != visualLevel)
        {
            level.setBlock(worldPosition, state.setValue(WaterCollectorBlock.WATER_LEVEL, visualLevel), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        super.loadAdditional(nbt, provider);
        tank.readFromNBT(provider, nbt.getCompound("tank"));
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        super.saveAdditional(nbt, provider);
        nbt.put("tank", tank.writeToNBT(provider, new CompoundTag()));
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
        controllers.add(new AnimationController<>(this, "water", 8, this::waterAnimation));
    }

    private PlayState waterAnimation(AnimationState<WaterCollectorBlockEntity> animation)
    {
        if (getBlockState().hasProperty(WaterCollectorBlock.WATER_LEVEL)
            && getBlockState().getValue(WaterCollectorBlock.WATER_LEVEL) > 0)
        {
            animation.getController().setAnimation(RIPPLE);
            return PlayState.CONTINUE;
        }
        animation.getController().setAnimation(null);
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache()
    {
        return cache;
    }
}
