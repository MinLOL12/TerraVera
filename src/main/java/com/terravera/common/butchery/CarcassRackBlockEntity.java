/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.butchery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import com.terravera.common.TerraVeraDataComponents;
import com.terravera.common.blocks.TerraVeraBlockEntities;

/**
 * Block entity for the Carcass Rack.
 * <p>
 * Holds a hanging animal carcass and syncs its state to clients so that as a player butchers it with a Butcher's Knife,
 * the 3D model anatomical layers wear off realistically and drop loot.
 */
public class CarcassRackBlockEntity extends BlockEntity implements GeoBlockEntity
{
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private ItemStack carcassStack = ItemStack.EMPTY;

    public CarcassRackBlockEntity(BlockPos pos, BlockState state)
    {
        super(TerraVeraBlockEntities.CARCASS_RACK.get(), pos, state);
    }

    public boolean hasCarcass()
    {
        return !carcassStack.isEmpty() && carcassStack.get(TerraVeraDataComponents.CARCASS.get()) != null;
    }

    public ItemStack getCarcassStack()
    {
        return carcassStack;
    }

    public void setCarcassStack(ItemStack stack)
    {
        this.carcassStack = stack == null ? ItemStack.EMPTY : stack.copy();
        setChanged();
        if (level != null && !level.isClientSide())
        {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public ItemStack removeCarcass()
    {
        final ItemStack removed = this.carcassStack;
        this.carcassStack = ItemStack.EMPTY;
        setChanged();
        if (level != null && !level.isClientSide())
        {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return removed;
    }

    public CarcassData getCarcassData()
    {
        return carcassStack.isEmpty() ? null : carcassStack.get(TerraVeraDataComponents.CARCASS.get());
    }

    public String getSpeciesId()
    {
        final CarcassData data = getCarcassData();
        return data != null ? data.species().id() : "cattle";
    }

    public String getStageId()
    {
        final CarcassData data = getCarcassData();
        return data != null ? data.stage().id() : "intact";
    }

    public float getWaste()
    {
        final CarcassData data = getCarcassData();
        return data != null ? data.waste() : 0f;
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        super.loadAdditional(nbt, provider);
        if (nbt.contains("carcass"))
        {
            carcassStack = ItemStack.parseOptional(provider, nbt.getCompound("carcass"));
        }
        else
        {
            carcassStack = ItemStack.EMPTY;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        super.saveAdditional(nbt, provider);
        if (!carcassStack.isEmpty())
        {
            nbt.put("carcass", carcassStack.saveOptional(provider));
        }
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
        // Static frame + subtle hanging sway animation driven by client model
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache()
    {
        return cache;
    }
}
