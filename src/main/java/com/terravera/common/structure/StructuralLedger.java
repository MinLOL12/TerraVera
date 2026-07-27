/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.structure;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Persistent list of player-built members participating in the structural model.
 * <p>
 * Keeping this separate from block state means TerraVera can make vanilla/TFC blocks structural without mutating their
 * state or deciding that every naturally generated cave ceiling is a failed player roof. The stored practice value is
 * the builder's knowledge at placement time; it makes a careful expert layout durable across logout without granting
 * an invisible bonus to a later, unrelated construction.
 */
public final class StructuralLedger extends SavedData
{
    public static final SavedData.Factory<StructuralLedger> FACTORY = new SavedData.Factory<>(StructuralLedger::new, StructuralLedger::load);

    /** Position -> builder knowledge [0, 1] at the time this structural member was installed. */
    private final Map<Long, Float> members = new HashMap<>();

    public static StructuralLedger load(CompoundTag tag, HolderLookup.Provider registries)
    {
        final StructuralLedger ledger = new StructuralLedger();
        final long[] positions = tag.getLongArray("members");
        final int[] practices = tag.getIntArray("practice");
        for (int i = 0; i < positions.length; i++)
        {
            // Older worlds/early dev snapshots may have positions but no practice array; they behave as novice work.
            final float knowledge = i < practices.length ? Mth.clamp(practices[i] / 1000f, 0f, 1f) : 0f;
            ledger.members.put(positions[i], knowledge);
        }
        return ledger;
    }

    public boolean contains(long pos)
    {
        return members.containsKey(pos);
    }

    public float knowledge(long pos)
    {
        return members.getOrDefault(pos, 0f);
    }

    public void add(long pos, float knowledge)
    {
        final float value = Mth.clamp(knowledge, 0f, 1f);
        if (!members.containsKey(pos) || Math.abs(members.get(pos) - value) > 0.001f)
        {
            members.put(pos, value);
            setDirty();
        }
    }

    public void remove(long pos)
    {
        if (members.remove(pos) != null) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries)
    {
        final long[] positions = new long[members.size()];
        final int[] practices = new int[members.size()];
        int index = 0;
        for (var entry : members.entrySet())
        {
            positions[index] = entry.getKey();
            practices[index] = Math.round(entry.getValue() * 1000f);
            index++;
        }
        tag.putLongArray("members", positions);
        tag.putIntArray("practice", practices);
        return tag;
    }
}
