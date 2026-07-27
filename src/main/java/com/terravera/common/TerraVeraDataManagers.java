/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common;

import net.neoforged.neoforge.registries.DeferredRegister;

import net.dries007.tfc.util.data.DataManager;
import net.dries007.tfc.util.data.DataManagers;

import com.terravera.TerraVera;
import com.terravera.common.knapping.HeadProfile;
import com.terravera.common.knapping.KnappableStone;
import com.terravera.common.health.Illness;
import com.terravera.common.health.Remedy;
import com.terravera.common.recipes.FibreSource;

/**
 * TerraVera piggybacks on TerraFirmaCraft's {@link DataManager} registry, which means our data files reload with
 * {@code /reload}, sync to clients automatically, and show up in TFC's self tests alongside TFC's own data.
 */
public final class TerraVeraDataManagers
{
    public static final DeferredRegister<DataManager<?>> MANAGERS =
        DeferredRegister.create(DataManagers.KEY, TerraVera.MOD_ID);

    static
    {
        register(HeadProfile.MANAGER);
        register(KnappableStone.MANAGER);
        register(FibreSource.MANAGER);
        register(Illness.MANAGER);
        register(Remedy.MANAGER);
    }

    private static void register(DataManager<?> manager)
    {
        MANAGERS.register(manager.getName(), () -> manager);
    }

    private TerraVeraDataManagers() {}
}
