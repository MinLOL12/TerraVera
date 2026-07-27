/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera;

import org.junit.jupiter.api.Test;

import com.terravera.common.temperature.ClothingMaterial;
import com.terravera.common.temperature.GarmentSlot;
import com.terravera.common.temperature.ThermalModel;
import com.terravera.common.temperature.ThermalModel.Band;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the body temperature model.
 * <p>
 * The single behaviour these tests exist to protect is the one the design is built on: <strong>temperature is never
 * instant</strong>. A player who walks into a blizzard must have minutes, not seconds, before they are in real
 * trouble, and must be told about it long before anything is taken away from them. Everything else here can be
 * retuned; if exposure ever becomes an instant hazard, the design is broken.
 */
public class BodyTemperatureTest
{
    /** Simulates {@code minutes} of standing in a given environment, and returns the resulting core temperature. */
    private static float simulate(float ambient, float insulation, float wind, float wetness, float activity, float minutes)
    {
        return simulate(ambient, insulation, wind, wetness, activity, minutes, 1f);
    }

    /** As above, but for the cases where how well the clothing breathes is the thing under test. */
    private static float simulate(float ambient, float insulation, float wind, float wetness, float activity,
                                  float minutes, float breathability)
    {
        float core = ThermalModel.NEUTRAL_CORE;
        final float step = 1f; // one second per iteration, matching the live tick interval
        for (int i = 0; i < minutes * 60; i++)
        {
            final float felt = ThermalModel.feltTemperature(ambient, wind, wetness, 0f, 0f);
            final float effective = ThermalModel.effectiveInsulation(insulation, wetness);
            final boolean shivering = core < ThermalModel.SHIVER_THRESHOLD;
            final float produced = ThermalModel.heatProduced(activity, shivering, 0f);
            // Wind only leaks through clothing when there is wind; still air leaks nothing whatever you wear.
            final float windLeak = wind * (1f - Math.min(1f, insulation * 2f));
            float lost = ThermalModel.heatLoss(core, felt, effective, windLeak);
            lost += ThermalModel.sweatCooling(core, wetness, 1f, breathability);
            core = ThermalModel.step(core, produced, lost, step);
        }
        return core;
    }

    // ----- The core promise: nothing is instant --------------------------------------------------------------

    @Test
    public void anUnclothedPlayerInAFreezingGaleIsFineForTheFirstMinute()
    {
        final float core = simulate(-20f, 0f, 1f, 0f, 0.1f, 1f);
        assertTrue(ThermalModel.band(core).severity() >= -1,
            "one minute of exposure already caused serious cold: core was " + core);
    }

    @Test
    public void thatSamePlayerIsWarnedLongBeforeTheyAreHarmed()
    {
        // By the time they are in the impairing bands they must already have passed through the harmless warning
        // bands, which is what gives them the chance to react.
        float core = ThermalModel.NEUTRAL_CORE;
        boolean sawMildWarning = false;
        for (int second = 0; second < 60 * 30; second++)
        {
            final float felt = ThermalModel.feltTemperature(-20f, 1f, 0f, 0f, 0f);
            final boolean shivering = core < ThermalModel.SHIVER_THRESHOLD;
            core = ThermalModel.step(core,
                ThermalModel.heatProduced(0.1f, shivering, 0f),
                ThermalModel.heatLoss(core, felt, 0f, 1f), 1f);

            final Band band = ThermalModel.band(core);
            if (band == Band.MILD_COLD) sawMildWarning = true;
            if (band.severity() <= -2)
            {
                assertTrue(sawMildWarning, "reached impairing cold without ever passing through a warning band");
                assertTrue(second > 60, "reached impairing cold in only " + second + " seconds");
                return;
            }
        }
        fail("an unclothed player in a -20C gale never got cold at all in half an hour");
    }

    @Test
    public void hypothermiaTakesManyMinutesEvenAtTheWorst()
    {
        int second = 0;
        float core = ThermalModel.NEUTRAL_CORE;
        for (; second < 60 * 60 && ThermalModel.band(core) != Band.HYPOTHERMIA; second++)
        {
            final float felt = ThermalModel.feltTemperature(-30f, 1f, 1f, 0f, 0f);
            core = ThermalModel.step(core,
                ThermalModel.heatProduced(0f, core < ThermalModel.SHIVER_THRESHOLD, 0f),
                ThermalModel.heatLoss(core, felt, 0f, 1f), 1f);
        }
        assertTrue(second > 240, "hypothermia set in after only " + second + " seconds of the worst case");
    }

    @Test
    public void hydrationAndBreathableClothingMakeAHotClimateSurvivable()
    {
        // Heat must be beatable by behaviour rather than being a hard temperature ceiling.
        final float sensible = simulate(38f, ClothingMaterial.LINEN.insulation(), 0f, 0f, 0.35f, 40f,
            ClothingMaterial.LINEN.breathability());
        assertTrue(ThermalModel.band(sensible).severity() <= 1,
            "dressed sensibly and hydrated at 38C the player should be coping, not dying: core was " + sensible);
    }

    @Test
    public void heatIsAlsoGradual()
    {
        final float afterOneMinute = simulate(45f, 0f, 0f, 0f, 0.2f, 1f);
        assertTrue(ThermalModel.band(afterOneMinute).severity() <= 1,
            "one minute in 45C already caused serious overheating");
    }

    // ----- Clothing has to actually matter --------------------------------------------------------------------

    @Test
    public void woolKeepsAPlayerComfortableWhereBareSkinDoesNot()
    {
        final float bare = simulate(0f, 0f, 0.2f, 0f, 0.15f, 20f);
        final float clothed = simulate(0f, ClothingMaterial.WOOL.insulation(), 0.2f, 0f, 0.15f, 20f);

        assertTrue(ThermalModel.band(bare).isCold(), "twenty minutes naked at 0C should be cold");
        assertEquals(Band.COMFORTABLE, ThermalModel.band(clothed), "a full wool outfit should cope with 0C");
    }

    @Test
    public void plantFibreIsBarelyBetterThanNothing()
    {
        final float bare = simulate(0f, 0f, 0.2f, 0f, 0.15f, 20f);
        final float fibre = simulate(0f, ClothingMaterial.PLANT_FIBER.insulation(), 0.2f, 0f, 0.15f, 20f);

        assertTrue(fibre > bare, "plant fibre should help at least a little");
        assertTrue(ThermalModel.band(fibre).isCold(), "plant fibre should not solve a 0C day");
    }

    @Test
    public void heavyClothingIsDangerousInAHotClimate()
    {
        final float light = simulate(38f, ClothingMaterial.LINEN.insulation(), 0f, 0f, 0.5f, 40f,
            ClothingMaterial.LINEN.breathability());
        final float heavy = simulate(38f, ClothingMaterial.FUR.insulation(), 0f, 0f, 0.5f, 40f,
            ClothingMaterial.FUR.breathability());

        assertTrue(heavy > light, "a fur parka should be hotter than linen in the desert");
        // The difference has to be one the player can actually feel, i.e. a whole band, not a rounding error.
        assertTrue(ThermalModel.band(heavy).severity() > ThermalModel.band(light).severity(),
            "fur and linen in 38C heat landed in the same band: " + heavy + " vs " + light);
        assertTrue(ThermalModel.band(heavy).severity() >= 2, "fur in 38C heat should genuinely overheat the player");
    }

    @Test
    public void airlessClothingBlocksTheBodysOnlyCoolingMechanism()
    {
        // The reason a parka is dangerous in the heat is not only that it is warm - it stops you sweating.
        final float breathing = ThermalModel.sweatCooling(39f, 0f, 1f, 1f);
        final float sealed = ThermalModel.sweatCooling(39f, 0f, 1f, 0f);
        assertTrue(sealed < breathing * 0.35f, "sealed clothing barely reduced sweat cooling");
        assertTrue(sealed > 0f, "even sealed clothing should let a little sweat escape");
    }

    @Test
    public void wetClothingLosesMostOfItsInsulation()
    {
        final float dry = ThermalModel.effectiveInsulation(0.6f, 0f);
        final float soaked = ThermalModel.effectiveInsulation(0.6f, 1f);
        assertTrue(soaked < dry * 0.35f, "soaked clothing kept too much of its insulation");

        final float dryPlayer = simulate(2f, 0.58f, 0.3f, 0f, 0.15f, 20f);
        final float wetPlayer = simulate(2f, 0.58f, 0.3f, 1f, 0.15f, 20f);
        assertTrue(wetPlayer < dryPlayer - 0.5f, "being soaked barely mattered");
    }

    @Test
    public void windMakesColdMeaningfullyWorse()
    {
        final float still = ThermalModel.feltTemperature(-5f, 0f, 0f, 0f, 0f);
        final float gale = ThermalModel.feltTemperature(-5f, 1f, 0f, 0f, 0f);
        assertTrue(gale < still - 5f, "a gale at -5C barely changed the felt temperature");
    }

    @Test
    public void windAndWetTogetherAreWorseThanEither()
    {
        final float dryGale = ThermalModel.feltTemperature(5f, 1f, 0f, 0f, 0f);
        final float wetGale = ThermalModel.feltTemperature(5f, 1f, 1f, 0f, 0f);
        assertTrue(wetGale < dryGale, "being soaked in a gale was not worse than being dry in one");
    }

    @Test
    public void shadeIsWorthSomethingInTheHeat()
    {
        final float shade = ThermalModel.feltTemperature(35f, 0f, 0f, 0f, 0f);
        final float sun = ThermalModel.feltTemperature(35f, 0f, 0f, 1f, 0f);
        assertTrue(sun > shade + 4f, "full sun was worth less than four degrees");
    }

    @Test
    public void waterStripsHeatFarFasterThanAirAtTheSameTemperature()
    {
        final float air = ThermalModel.feltTemperature(10f, 0f, 0f, 0f, 0f);
        final float submerged = ThermalModel.feltTemperature(10f, 0f, 0f, 0f, 1f);
        assertTrue(submerged < air - 5f, "being submerged in 10C water felt no worse than standing in 10C air");
    }

    // ----- Work, sweat, and hydration ---------------------------------------------------------------------------

    @Test
    public void heavyWorkProducesRealHeat()
    {
        final float resting = ThermalModel.heatProduced(0f, false, 0f);
        final float working = ThermalModel.heatProduced(1f, false, 0f);
        assertTrue(working > resting * 2f, "heavy labour should more than double heat production");
    }

    @Test
    public void workingInWinterClothingCanOverheatYou()
    {
        final float resting = simulate(5f, ClothingMaterial.FUR.insulation(), 0f, 0f, 0.05f, 25f);
        final float labouring = simulate(5f, ClothingMaterial.FUR.insulation(), 0f, 0f, 1f, 25f);
        assertTrue(labouring > resting, "working hard in a fur parka should be hotter than standing in one");
        assertTrue(ThermalModel.band(labouring).isHot(), "sustained heavy labour in fur at 5C should overheat");
    }

    @Test
    public void sweatingRequiresWater()
    {
        final float hydrated = ThermalModel.sweatCooling(39f, 0f, 1f, 1f);
        final float parched = ThermalModel.sweatCooling(39f, 0f, 0f, 1f);
        assertTrue(hydrated > 0f, "an overheating, hydrated player should be sweating");
        assertEquals(0f, parched, 0.001f, "a player with no water left should not be able to sweat");
    }

    @Test
    public void sweatDoesNothingAtANormalTemperature()
    {
        assertEquals(0f, ThermalModel.sweatCooling(ThermalModel.NEUTRAL_CORE, 0f, 1f, 1f), 0.001f);
    }

    @Test
    public void shiveringSlowsCoolingButDoesNotStopIt()
    {
        final float withShivering = ThermalModel.heatProduced(0.1f, true, 0f);
        final float without = ThermalModel.heatProduced(0.1f, false, 0f);
        assertTrue(withShivering > without, "shivering should produce heat");

        final float core = simulate(-25f, 0f, 0.5f, 0f, 0.1f, 30f);
        assertTrue(ThermalModel.band(core).severity() <= -2,
            "shivering should not be able to save a naked player at -25C indefinitely");
    }

    // ----- Bands and messaging ----------------------------------------------------------------------------------

    @Test
    public void bandsCoverTheWholeRangeInOrder()
    {
        assertEquals(Band.HYPOTHERMIA, ThermalModel.band(30f));
        assertEquals(Band.SEVERE_COLD, ThermalModel.band(34f));
        assertEquals(Band.MODERATE_COLD, ThermalModel.band(35.2f));
        assertEquals(Band.MILD_COLD, ThermalModel.band(36.1f));
        assertEquals(Band.COMFORTABLE, ThermalModel.band(ThermalModel.NEUTRAL_CORE));
        assertEquals(Band.MILD_HEAT, ThermalModel.band(38f));
        assertEquals(Band.MODERATE_HEAT, ThermalModel.band(39f));
        assertEquals(Band.SEVERE_HEAT, ThermalModel.band(40f));
        assertEquals(Band.HEAT_STROKE, ThermalModel.band(41f));
    }

    @Test
    public void theFirstTwoBandsEitherSideAreWarningsOnly()
    {
        // These are the bands that must never do anything mechanical - they exist so the player is told first.
        assertEquals(1, Math.abs(Band.MILD_COLD.severity()));
        assertEquals(1, Math.abs(Band.MILD_HEAT.severity()));
        assertFalse(Band.COMFORTABLE.isNotable());
        assertTrue(Band.MILD_COLD.isNotable());
    }

    @Test
    public void theShippedSymptomWordingIsWiredUp()
    {
        // The exact strings the design calls for, by their translation keys.
        assertEquals("terravera.temperature.mild_cold", Band.MILD_COLD.messageKey());
        assertEquals("terravera.temperature.moderate_cold", Band.MODERATE_COLD.messageKey());
        assertEquals("terravera.temperature.severe_cold", Band.SEVERE_COLD.messageKey());
        assertEquals("terravera.temperature.moderate_heat", Band.MODERATE_HEAT.messageKey());
    }

    @Test
    public void messagesDoNotRepeatOrFlicker()
    {
        assertFalse(ThermalModel.shouldAnnounce(Band.MILD_COLD, Band.MILD_COLD), "repeated the same band");
        assertTrue(ThermalModel.shouldAnnounce(Band.MILD_COLD, Band.MODERATE_COLD), "silent as things got worse");
        assertFalse(ThermalModel.shouldAnnounce(Band.SEVERE_COLD, Band.MODERATE_COLD),
            "nagged the player while they were already recovering");
        assertTrue(ThermalModel.shouldAnnounce(Band.MILD_COLD, Band.COMFORTABLE), "never said they were fine again");
        assertTrue(ThermalModel.shouldAnnounce(Band.MILD_COLD, Band.MILD_HEAT), "silent on a cold-to-hot swing");
    }

    // ----- Clothing table sanity --------------------------------------------------------------------------------

    @Test
    public void everyMaterialIsWithinRange()
    {
        for (ClothingMaterial material : ClothingMaterial.values())
        {
            assertTrue(material.insulation() >= 0f && material.insulation() <= 1f, material + " insulation");
            assertTrue(material.windProof() >= 0f && material.windProof() <= 1f, material + " windProof");
            assertTrue(material.breathability() >= 0f && material.breathability() <= 1f, material + " breathability");
            assertTrue(material.wetPenalty() >= 0f && material.wetPenalty() <= 1f, material + " wetPenalty");
        }
    }

    @Test
    public void theMaterialLinesRankTheWayTheDesignSays()
    {
        // Bare skin loses heat fastest; plant fibre is minimal; wool is strong cold protection.
        assertEquals(0f, ClothingMaterial.BARE.insulation(), 0.001f);
        assertTrue(ClothingMaterial.PLANT_FIBER.insulation() < 0.2f, "plant fibre should be minimal protection");
        assertTrue(ClothingMaterial.WOOL.insulation() > 0.5f, "wool should be strong cold protection");
        assertTrue(ClothingMaterial.FUR.insulation() > ClothingMaterial.WOOL.insulation(), "fur should beat wool");

        // Leather protects from wind and weather rather than by being warm.
        assertTrue(ClothingMaterial.LEATHER.windProof() > ClothingMaterial.WOOL.windProof(),
            "leather should stop more wind than wool");
        assertTrue(ClothingMaterial.LEATHER.insulation() < ClothingMaterial.WOOL.insulation(),
            "leather should not be warmer than wool");

        // Wool is the one that keeps working soaked.
        assertTrue(ClothingMaterial.WOOL.wetPenalty() < ClothingMaterial.PLANT_FIBER.wetPenalty(),
            "wool should suffer less from being wet than plant fibre");
        assertTrue(ClothingMaterial.OILSKIN.wetPenalty() < ClothingMaterial.LEATHER.wetPenalty(),
            "oilskin should be the least affected by rain");

        // Advanced fabrics regulate better: they are warm without being airless.
        assertTrue(ClothingMaterial.QUILTED.insulation() > ClothingMaterial.FELT.insulation());
        assertTrue(ClothingMaterial.SILK.breathability() > ClothingMaterial.FELT.breathability());
        assertTrue(ClothingMaterial.LINEN.breathability() > 0.9f, "linen should be the hot-weather fabric");
    }

    @Test
    public void aFullOutfitOfOneMaterialInsulatesExactlyThatMaterialsValue()
    {
        // The coverage weights must sum to one, so the material table can be read directly.
        float total = 0f;
        for (GarmentSlot slot : GarmentSlot.values()) total += slot.coverage();
        assertEquals(1f, total, 0.001f, "garment coverage weights must sum to 1");
    }

    @Test
    public void aChestGarmentIsWorthMoreThanAHat()
    {
        assertTrue(GarmentSlot.CHEST.coverage() > GarmentSlot.HEAD.coverage());
        assertTrue(GarmentSlot.HEAD.coverage() > GarmentSlot.FEET.coverage());
    }

    @Test
    public void theComfortableAmbientReadoutIsHonest()
    {
        // Whatever number the field notes quote, standing at exactly that temperature must in fact be comfortable.
        for (ClothingMaterial material : ClothingMaterial.values())
        {
            final float quoted = ThermalModel.comfortableAmbient(material.insulation(), 0.15f);
            final float core = simulate(quoted, material.insulation(), 0f, 0f, 0.15f, 30f);
            assertEquals(Band.COMFORTABLE, ThermalModel.band(core),
                material + ": field notes quoted " + quoted + "C but the player ended at " + core);
        }
    }

    @Test
    public void bareSkinIsComfortableOnlyInGenuinelyWarmAir()
    {
        final float quoted = ThermalModel.comfortableAmbient(0f, 0.15f);
        assertTrue(quoted > 18f && quoted < 32f,
            "an unclothed human should be comfortable somewhere in the mid twenties, not " + quoted);
    }
}
