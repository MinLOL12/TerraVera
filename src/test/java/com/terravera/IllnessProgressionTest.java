/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import com.terravera.common.health.Illness;
import com.terravera.common.health.Infection;
import com.terravera.common.health.PlayerHealth;
import com.terravera.common.health.Symptom;
import com.terravera.common.health.TransmissionVector;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the state machine at the heart of the disease system.
 * <p>
 * The single behaviour this file exists to protect is that <strong>illness is never instant</strong>. Everything else
 * in the mod can be retuned; if an infection ever becomes symptomatic at the moment of exposure, the design is broken.
 */
public class IllnessProgressionTest
{
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("terravera", "test_illness");

    private static Illness illness(int incubation, int duration)
    {
        return new Illness(
            List.of(TransmissionVector.WATER), 0.5f, incubation, duration,
            Illness.Severity.MODERATE, List.of(Symptom.FEVER, Symptom.FATIGUE),
            false, 0, false, List.of());
    }

    @Test
    public void anInfectionIsSilentForTheWholeIncubationPeriod()
    {
        final Illness illness = illness(24000, 48000);
        final Infection infection = Infection.contract(ID, 1000L, 1f);

        assertEquals(Infection.Stage.INCUBATING, infection.stage(illness, 1000L), "symptomatic at the moment of exposure");
        assertEquals(Infection.Stage.INCUBATING, infection.stage(illness, 1000L + 1));
        assertEquals(Infection.Stage.INCUBATING, infection.stage(illness, 1000L + 23999));
        assertEquals(Infection.Stage.SYMPTOMATIC, infection.stage(illness, 1000L + 24000));
    }

    @Test
    public void anInfectionResolvesAfterIncubationPlusDuration()
    {
        final Illness illness = illness(24000, 48000);
        final Infection infection = Infection.contract(ID, 0L, 1f);

        assertEquals(Infection.Stage.SYMPTOMATIC, infection.stage(illness, 71999L));
        assertEquals(Infection.Stage.RESOLVED, infection.stage(illness, 72000L));
        assertEquals(illness.totalTicks(), 72000);
    }

    @Test
    public void noSymptomsAreAppliedDuringIncubation()
    {
        final Illness illness = illness(24000, 48000);
        final Infection infection = Infection.contract(ID, 0L, 1f);

        // Intensity has to be zero throughout incubation, since it is what scales every applied effect.
        assertEquals(0f, infection.intensity(illness, 0L));
        assertEquals(0f, infection.intensity(illness, 12000L));
        assertEquals(0f, infection.intensity(illness, 23999L));
        assertTrue(infection.intensity(illness, 30000L) > 0f, "should be symptomatic once incubation has passed");
    }

    @Test
    public void symptomIntensityRampsUpAndBackDown()
    {
        final Illness illness = illness(0, 100000);
        final Infection infection = Infection.contract(ID, 0L, 1f);

        final float onset = infection.intensity(illness, 2000L);      // 2% in
        final float peak = infection.intensity(illness, 40000L);      // 40% in
        final float recovering = infection.intensity(illness, 95000L); // 95% in

        assertTrue(onset < peak, "illness should build rather than arrive at full strength");
        assertTrue(recovering < peak, "illness should fade as the player convalesces");
        assertTrue(onset > 0f && recovering > 0f);
    }

    @Test
    public void treatmentShortensTheRemainingCourse()
    {
        final Illness illness = illness(10000, 50000);
        final Infection untreated = Infection.contract(ID, 0L, 1f);
        final Infection treated = untreated.treated(20000, 0.3f);

        assertTrue(treated.ticksRemaining(illness, 10000L) < untreated.ticksRemaining(illness, 10000L),
            "a remedy must actually shorten the illness");
        assertTrue(treated.severityScale() < untreated.severityScale(),
            "a remedy must actually blunt the symptoms");
    }

    @Test
    public void treatmentCanResolveAnIllnessEarly()
    {
        final Illness illness = illness(10000, 50000);
        final Infection infection = Infection.contract(ID, 0L, 1f).treated(60000, 0f);

        assertEquals(Infection.Stage.RESOLVED, infection.stage(illness, 1000L));
    }

    @Test
    public void severityScaleIsClamped()
    {
        assertEquals(1.5f, Infection.contract(ID, 0L, 99f).severityScale());
        assertEquals(0.25f, Infection.contract(ID, 0L, -5f).severityScale());
    }

    // ----- PlayerHealth -------------------------------------------------------------------------------------

    @Test
    public void immunityPreventsReinfectionUntilItLapses()
    {
        PlayerHealth health = PlayerHealth.EMPTY;
        assertTrue(health.isSusceptibleTo(ID, 0L));

        health = health.withImmunity(ID, 5000L);
        assertFalse(health.isSusceptibleTo(ID, 0L), "should be immune while immunity is in force");
        assertFalse(health.isSusceptibleTo(ID, 4999L));
        assertTrue(health.isSusceptibleTo(ID, 5000L), "immunity should lapse on schedule");
    }

    @Test
    public void youCannotCatchWhatYouAlreadyHave()
    {
        final PlayerHealth health = PlayerHealth.EMPTY.withInfection(Infection.contract(ID, 0L, 1f));
        assertFalse(health.isSusceptibleTo(ID, 100L));
        assertTrue(health.hasInfection(ID));
    }

    @Test
    public void infectionsAreNotDuplicated()
    {
        final PlayerHealth health = PlayerHealth.EMPTY
            .withInfection(Infection.contract(ID, 0L, 1f))
            .withInfection(Infection.contract(ID, 500L, 1f));

        assertEquals(1, health.infections().size());
        assertEquals(500L, health.infections().getFirst().contractedTick(), "the newer exposure should win");
    }

    @Test
    public void lapsedImmunitiesArePruned()
    {
        final ResourceLocation other = ResourceLocation.fromNamespaceAndPath("terravera", "other");
        final PlayerHealth health = PlayerHealth.EMPTY
            .withImmunity(ID, 1000L)
            .withImmunity(other, 9000L)
            .prunedImmunities(5000L);

        assertEquals(1, health.immunities().size());
        assertTrue(health.immunities().containsKey(other));
    }

    @Test
    public void deathClearsInfectionsButKeepsImmunity()
    {
        final PlayerHealth health = PlayerHealth.EMPTY
            .withInfection(Infection.contract(ID, 0L, 1f))
            .withImmunity(ID, 100000L)
            .onDeath();

        assertTrue(health.infections().isEmpty(), "dying should clear acute infections");
        assertEquals(1, health.immunities().size(), "dying should not throw away acquired immunity");
    }

    // ----- Hygiene ------------------------------------------------------------------------------------------

    @Test
    public void hygieneScalesInfectionRiskMonotonically()
    {
        final float clean = PlayerHealth.EMPTY.withHygiene(1.0f).hygieneRiskMultiplier();
        final float middling = PlayerHealth.EMPTY.withHygiene(0.5f).hygieneRiskMultiplier();
        final float filthy = PlayerHealth.EMPTY.withHygiene(0.0f).hygieneRiskMultiplier();

        assertTrue(clean < middling, "washing must always be worth something");
        assertTrue(middling < filthy);
        assertTrue(clean < 1.0f, "being clean should reduce risk below baseline");
        assertTrue(filthy > 1.0f, "being filthy should raise risk above baseline");
    }

    @Test
    public void hygieneIsClampedToItsRange()
    {
        assertEquals(1.0f, PlayerHealth.EMPTY.washed(99f, 0L).hygiene());
        assertEquals(0.0f, PlayerHealth.EMPTY.soiled(99f).hygiene());
    }
}
