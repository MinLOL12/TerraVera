/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.sterilization;

/**
 * One entry in TerraVera's catalogue of ways to make water worth drinking.
 * <p>
 * Every method is documented even when it shares an implementation with another - a "rolling boil" and a "boil" are
 * the same pot interaction, but they are two different things a player needs to know about. Methods carry the
 * translation keys for their name, a plain-language description, and the materials that method realistically needs.
 * The catalogue is deliberately real: these are the actual methods field guides and emergency-response manuals teach.
 *
 * @param id           stable identifier, used for lang keys and JEI-adjacent references
 * @param category     which branch of the treatment tree this belongs to
 * @param tier         0 is stone-age available, 5 is industrial. Drives nothing mechanically yet, but keeps the
 *                     catalogue ordered the way a player actually unlocks it
 * @param nameKey      lang key for the method name
 * @param descKey      lang key for the description
 * @param materialsKey lang key for the materials / where to get them
 * @param implemented  {@code true} if the method is live in-game (a block, item, or TFC interaction); {@code false}
 *                     if it is documented for the guidebook but not yet buildable
 */
public record SterilizationMethod(String id, SterilizationCategory category, int tier,
                                  String nameKey, String descKey, String materialsKey, boolean implemented)
{
    public SterilizationMethod(String id, SterilizationCategory category, int tier,
                               String nameKey, String descKey, String materialsKey)
    {
        this(id, category, tier, nameKey, descKey, materialsKey, true);
    }
}
