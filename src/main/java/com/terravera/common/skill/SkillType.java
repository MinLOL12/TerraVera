/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.skill;

import net.minecraft.network.chat.Component;

/**
 * Fields of learned, practical knowledge. These intentionally are not character levels: each field advances only by
 * doing work in that field, and its effects are narrowly related to that work.
 */
public enum SkillType
{
    MINING("mining"),
    SMITHING("smithing"),
    BUILDING("building"),
    COOKING("cooking"),
    MEDICINE("medicine");

    private final String id;

    SkillType(String id)
    {
        this.id = id;
    }

    public String id()
    {
        return id;
    }

    public Component displayName()
    {
        return Component.translatable("terravera.skill." + id);
    }
}
