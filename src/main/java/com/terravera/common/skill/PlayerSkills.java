/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

/**
 * Persistent practical experience for one player.
 * <p>
 * Experience is deliberately a continuous, diminishing-return measure rather than a row of universal RPG levels.
 * Knowing a little about ore does not make someone a better surgeon, and even an expert continues to learn without
 * gaining implausible linear bonuses. {@link #proficiency(SkillType)} is the only value gameplay code should use.
 */
public record PlayerSkills(float mining, float smithing, float building, float cooking, float medicine, float butchery)
{
    public static final float MAX_EXPERIENCE = 10_000f;
    public static final PlayerSkills EMPTY = new PlayerSkills(0f, 0f, 0f, 0f, 0f, 0f);

    public static final Codec<PlayerSkills> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.FLOAT.optionalFieldOf("mining", 0f).forGetter(PlayerSkills::mining),
        Codec.FLOAT.optionalFieldOf("smithing", 0f).forGetter(PlayerSkills::smithing),
        Codec.FLOAT.optionalFieldOf("building", 0f).forGetter(PlayerSkills::building),
        Codec.FLOAT.optionalFieldOf("cooking", 0f).forGetter(PlayerSkills::cooking),
        Codec.FLOAT.optionalFieldOf("medicine", 0f).forGetter(PlayerSkills::medicine),
        Codec.FLOAT.optionalFieldOf("butchery", 0f).forGetter(PlayerSkills::butchery)
    ).apply(i, PlayerSkills::new));

    public PlayerSkills
    {
        mining = clamp(mining);
        smithing = clamp(smithing);
        building = clamp(building);
        cooking = clamp(cooking);
        medicine = clamp(medicine);
        butchery = clamp(butchery);
    }

    public float experience(SkillType skill)
    {
        return switch (skill)
        {
            case MINING -> mining;
            case SMITHING -> smithing;
            case BUILDING -> building;
            case COOKING -> cooking;
            case MEDICINE -> medicine;
            case BUTCHERY -> butchery;
        };
    }

    /**
     * A smooth, diminishing knowledge curve. At 120 experience the player is halfway to the practical ceiling; no
     * field ever becomes a magic "max level" that invalidates careful work.
     */
    public float proficiency(SkillType skill)
    {
        final float experience = experience(skill);
        return experience <= 0f ? 0f : experience / (experience + 120f);
    }

    public PlayerSkills withExperience(SkillType skill, float value)
    {
        final float next = clamp(value);
        return switch (skill)
        {
            case MINING -> new PlayerSkills(next, smithing, building, cooking, medicine, butchery);
            case SMITHING -> new PlayerSkills(mining, next, building, cooking, medicine, butchery);
            case BUILDING -> new PlayerSkills(mining, smithing, next, cooking, medicine, butchery);
            case COOKING -> new PlayerSkills(mining, smithing, building, next, medicine, butchery);
            case MEDICINE -> new PlayerSkills(mining, smithing, building, cooking, next, butchery);
            case BUTCHERY -> new PlayerSkills(mining, smithing, building, cooking, medicine, next);
        };
    }

    public PlayerSkills learned(SkillType skill, float amount)
    {
        if (amount <= 0f) return this;
        return withExperience(skill, experience(skill) + amount);
    }

    /** Human-readable knowledge bands for the field notebook and focused tooltips. */
    public String knowledgeKey(SkillType skill)
    {
        final float experience = experience(skill);
        if (experience < 15f) return "terravera.knowledge.unfamiliar";
        if (experience < 45f) return "terravera.knowledge.familiar";
        if (experience < 100f) return "terravera.knowledge.practiced";
        if (experience < 240f) return "terravera.knowledge.skilled";
        return "terravera.knowledge.expert";
    }

    private static float clamp(float value)
    {
        return Mth.clamp(value, 0f, MAX_EXPERIENCE);
    }
}
