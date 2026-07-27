/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.health;

import java.util.Locale;
import javax.annotation.Nullable;

/**
 * How an illness gets into you. Each vector corresponds to one specific thing the player did, and each is checked at
 * exactly one point in the code - there is no ambient "you are now sick" roll anywhere.
 * <p>
 * No Minecraft types here on purpose; this is part of the unit-testable core.
 */
public enum TransmissionVector
{
    /** Drinking untreated water - by hand from a source block, or from a container filled with untreated water. */
    WATER,
    /** Eating food that is rotten, was prepared with dirty hands, or has been sitting somewhere filthy. */
    FOOD,
    /** Eating meat or fish that was never cooked, or was cooked from a state it should not have been eaten from. */
    UNDERCOOKED_MEAT,
    /** Being near someone who is already ill and shedding. Colds and influenza only. */
    CONTACT,
    /** Taking a wound in a dirty state - low hygiene, or damage from something that was in the soil. */
    WOUND,
    /** Working in filth: handling waste, standing in mud and muck with open cuts, poor camp sanitation. */
    SANITATION;

    private final String id = name().toLowerCase(Locale.ROOT);

    public String id()
    {
        return id;
    }

    @Nullable
    public static TransmissionVector byId(String id)
    {
        for (TransmissionVector vector : values())
        {
            if (vector.id.equals(id)) return vector;
        }
        return null;
    }
}
