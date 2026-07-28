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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.terravera.common.blocks.TerraVeraBlockEntities;
import com.terravera.common.quality.SoilCondition;

/**
 * Stores the soil condition of a prepared farmland block. This is what makes soil preparation matter: two
 * otherwise identical beds can have very different growing conditions based on how they were prepared.
 */
public class PreparedFarmlandBlockEntity extends BlockEntity
{
    private SoilCondition soilCondition;

    public PreparedFarmlandBlockEntity(BlockPos pos, BlockState state)
    {
        super(TerraVeraBlockEntities.PREPARED_FARMLAND.get(), pos, state);
        this.soilCondition = SoilCondition.UNPREPARED;
    }

    public SoilCondition soilCondition() { return soilCondition; }

    public void setSoilCondition(SoilCondition condition)
    {
        this.soilCondition = condition;
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        super.loadAdditional(nbt, provider);
        if (nbt.contains("soil"))
        {
            soilCondition = SoilCondition.CODEC.parse(
                net.minecraft.nbt.NbtOps.INSTANCE, nbt.get("soil")).result().orElse(SoilCondition.UNPREPARED);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        super.saveAdditional(nbt, provider);
        SoilCondition.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, soilCondition)
            .result().ifPresent(tag -> nbt.put("soil", tag));
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
}
