/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera;

import org.junit.jupiter.api.Test;

import com.terravera.common.health.WaterQuality;
import com.terravera.common.health.WaterTreatment;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the water model: that the risk ordering across source types is the one the design promises, and that boiling
 * and filtration do what the documentation says they do.
 */
public class WaterQualityTest
{
    @Test
    public void riskIncreasesFromMountainStreamToFoulWell()
    {
        // This ordering is the entire premise of the water system and must not drift.
        assertTrue(WaterQuality.CLEAN.contamination() < WaterQuality.PRISTINE.contamination());
        assertTrue(WaterQuality.PRISTINE.contamination() < WaterQuality.RUNNING.contamination());
        assertTrue(WaterQuality.RUNNING.contamination() < WaterQuality.STILL.contamination());
        assertTrue(WaterQuality.STILL.contamination() < WaterQuality.STAGNANT.contamination());
        assertTrue(WaterQuality.STAGNANT.contamination() < WaterQuality.SWAMP.contamination());
        assertTrue(WaterQuality.SWAMP.contamination() < WaterQuality.FOUL.contamination());
    }

    @Test
    public void treatedWaterCarriesNoRisk()
    {
        assertEquals(0f, WaterQuality.CLEAN.contamination());
        assertFalse(WaterQuality.CLEAN.isRisky());
        assertFalse(WaterQuality.PRISTINE.isRisky(), "a mountain stream should not be flagged as dangerous");
        assertFalse(WaterQuality.RUNNING.isRisky());
    }

    @Test
    public void stagnantAndSwampWaterAreFlaggedAsRisky()
    {
        assertTrue(WaterQuality.STILL.isRisky());
        assertTrue(WaterQuality.STAGNANT.isRisky());
        assertTrue(WaterQuality.SWAMP.isRisky());
        assertTrue(WaterQuality.FOUL.isRisky());
    }

    @Test
    public void everyContaminationValueMapsBackToABand()
    {
        for (float value = 0f; value <= 1f; value += 0.01f)
        {
            assertNotNull(WaterQuality.fromContamination(value));
        }
        assertEquals(WaterQuality.CLEAN, WaterQuality.fromContamination(0f));
        assertEquals(WaterQuality.FOUL, WaterQuality.fromContamination(1f));
        // Out-of-range values are clamped rather than throwing.
        assertEquals(WaterQuality.CLEAN, WaterQuality.fromContamination(-3f));
        assertEquals(WaterQuality.FOUL, WaterQuality.fromContamination(7f));
    }

    // ----- Treatment ----------------------------------------------------------------------------------------

    @Test
    public void boilingRemovesAllRisk()
    {
        final WaterTreatment swampWater = new WaterTreatment(
            WaterTreatment.Treatment.UNTREATED, WaterQuality.SWAMP.contamination());

        assertTrue(swampWater.effectiveContamination() > 0.5f);
        assertEquals(0f, swampWater.boiled().effectiveContamination(), "boiling must make any water safe");
        assertTrue(swampWater.boiled().isSafe());
    }

    @Test
    public void filtrationHelpsButDoesNotFullyPurify()
    {
        final WaterTreatment pond = new WaterTreatment(
            WaterTreatment.Treatment.UNTREATED, WaterQuality.STAGNANT.contamination());
        final WaterTreatment filtered = pond.filtered();

        assertTrue(filtered.effectiveContamination() < pond.effectiveContamination(),
            "filtration must be a real improvement");
        assertTrue(filtered.effectiveContamination() > 0f,
            "filtration must NOT be a substitute for boiling - this is the whole point of the two tiers");
        assertFalse(filtered.isSafe());
    }

    @Test
    public void filteringBoiledWaterDoesNotDowngradeIt()
    {
        final WaterTreatment boiled = new WaterTreatment(WaterTreatment.Treatment.BOILED, 0.8f);
        assertEquals(WaterTreatment.Treatment.BOILED, boiled.filtered().treatment());
        assertEquals(0f, boiled.filtered().effectiveContamination());
    }

    @Test
    public void filteringVeryCleanWaterIsAlreadySafe()
    {
        final WaterTreatment stream = new WaterTreatment(
            WaterTreatment.Treatment.UNTREATED, WaterQuality.PRISTINE.contamination());
        assertTrue(stream.filtered().isSafe(), "a filtered mountain stream should be fine to drink");
    }

    @Test
    public void treatmentIdsRoundTrip()
    {
        for (WaterTreatment.Treatment treatment : WaterTreatment.Treatment.values())
        {
            assertEquals(treatment, WaterTreatment.Treatment.byId(treatment.id()));
        }
        assertEquals(WaterTreatment.Treatment.UNTREATED, WaterTreatment.Treatment.byId("nonsense"));
    }
}
