/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.temperature;

import java.util.Locale;
import net.minecraft.world.item.ArmorItem;

/**
 * Which part of the body a garment covers, and how much of the body's heat loss that part accounts for.
 * <p>
 * The coverage weights are roughly the real surface-area shares, which is why a coat is worth more than a hat and a
 * hat is nevertheless worth more than boots. They sum to one, so a full outfit of one material insulates exactly as
 * much as that material's headline value - the table in {@link ClothingMaterial} can be read directly.
 */
public enum GarmentSlot
{
    HEAD(ArmorItem.Type.HELMET, 0.18f),
    CHEST(ArmorItem.Type.CHESTPLATE, 0.42f),
    LEGS(ArmorItem.Type.LEGGINGS, 0.28f),
    FEET(ArmorItem.Type.BOOTS, 0.12f);

    private final ArmorItem.Type type;
    private final float coverage;

    GarmentSlot(ArmorItem.Type type, float coverage)
    {
        this.type = type;
        this.coverage = coverage;
    }

    public ArmorItem.Type type()
    {
        return type;
    }

    /** Share of total body heat loss this slot is responsible for. */
    public float coverage()
    {
        return coverage;
    }

    public String id()
    {
        return name().toLowerCase(Locale.ROOT);
    }
}
