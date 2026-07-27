/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera;

import org.junit.jupiter.api.Test;

import com.terravera.common.skill.PlayerSkills;
import com.terravera.common.skill.SkillType;

import static org.junit.jupiter.api.Assertions.*;

/** Guards the non-RPG knowledge curve: fields stay independent and gains have diminishing returns. */
public class SkillProgressionTest
{
    @Test
    public void fieldsAreLearnedIndependently()
    {
        final PlayerSkills miner = PlayerSkills.EMPTY.learned(SkillType.MINING, 80f);
        assertEquals(80f, miner.experience(SkillType.MINING));
        assertEquals(0f, miner.experience(SkillType.SMITHING));
        assertEquals(0f, miner.experience(SkillType.BUILDING));
        assertEquals(0f, miner.experience(SkillType.COOKING));
        assertEquals(0f, miner.experience(SkillType.MEDICINE));
    }

    @Test
    public void practicalProficiencyHasDiminishingReturns()
    {
        final PlayerSkills beginner = PlayerSkills.EMPTY.learned(SkillType.SMITHING, 20f);
        final PlayerSkills practiced = PlayerSkills.EMPTY.learned(SkillType.SMITHING, 120f);
        final PlayerSkills expert = PlayerSkills.EMPTY.learned(SkillType.SMITHING, 300f);

        assertTrue(beginner.proficiency(SkillType.SMITHING) < practiced.proficiency(SkillType.SMITHING));
        assertTrue(practiced.proficiency(SkillType.SMITHING) < expert.proficiency(SkillType.SMITHING));
        assertTrue(expert.proficiency(SkillType.SMITHING) < 1f, "knowledge should approach, not exceed, its practical ceiling");
        assertTrue(expert.proficiency(SkillType.SMITHING) - practiced.proficiency(SkillType.SMITHING)
            < practiced.proficiency(SkillType.SMITHING) - beginner.proficiency(SkillType.SMITHING),
            "late experience should improve reliability more slowly than early learning");
    }

    @Test
    public void knowledgeBandsDescribeExperienceRatherThanAUniversalLevel()
    {
        assertEquals("terravera.knowledge.unfamiliar", PlayerSkills.EMPTY.knowledgeKey(SkillType.MEDICINE));
        assertEquals("terravera.knowledge.familiar", PlayerSkills.EMPTY.learned(SkillType.MEDICINE, 20f).knowledgeKey(SkillType.MEDICINE));
        assertEquals("terravera.knowledge.expert", PlayerSkills.EMPTY.learned(SkillType.MEDICINE, 300f).knowledgeKey(SkillType.MEDICINE));
    }
}
