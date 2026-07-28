/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 */

package com.terravera.common.bark;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent per-dimension record of how heavily living trees have recently been stripped. */
public final class TreeHarvestData extends SavedData
{
    private static final String NAME = "terravera_bark_harvests";
    private static final Factory<TreeHarvestData> FACTORY = new Factory<>(TreeHarvestData::new, TreeHarvestData::load);

    private final Map<Long, Harvest> harvests = new HashMap<>();

    public static TreeHarvestData get(ServerLevel level)
    {
        return level.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    public Harvest get(long root, long now, long recoveryTicks)
    {
        final Harvest old = harvests.get(root);
        if (old == null || now - old.lastHarvest() >= recoveryTicks)
        {
            return new Harvest(0, now, old == null ? "mixed" : old.species());
        }
        return old;
    }

    public Harvest record(long root, long now, String species, long recoveryTicks)
    {
        final Harvest current = get(root, now, recoveryTicks);
        final Harvest next = new Harvest(current.strips() + 1, now, species);
        harvests.put(root, next);
        setDirty();
        return next;
    }

    public void remove(long root)
    {
        if (harvests.remove(root) != null) setDirty();
    }

    private static TreeHarvestData load(CompoundTag nbt, HolderLookup.Provider provider)
    {
        final TreeHarvestData data = new TreeHarvestData();
        final ListTag list = nbt.getList("trees", Tag.TAG_COMPOUND);
        for (Tag value : list)
        {
            final CompoundTag tree = (CompoundTag) value;
            data.harvests.put(tree.getLong("root"), new Harvest(
                tree.getInt("strips"), tree.getLong("last_harvest"), tree.getString("species")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider)
    {
        final ListTag list = new ListTag();
        harvests.forEach((root, harvest) -> {
            final CompoundTag tree = new CompoundTag();
            tree.putLong("root", root);
            tree.putInt("strips", harvest.strips());
            tree.putLong("last_harvest", harvest.lastHarvest());
            tree.putString("species", harvest.species());
            list.add(tree);
        });
        nbt.put("trees", list);
        return nbt;
    }

    public record Harvest(int strips, long lastHarvest, String species) {}
}
