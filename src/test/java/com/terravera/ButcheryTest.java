/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 */

package com.terravera;

import org.junit.jupiter.api.Test;

import com.terravera.common.butchery.ButcheryStage;
import com.terravera.common.butchery.ButcheryTool;
import com.terravera.common.butchery.ButcheryYield;
import com.terravera.common.butchery.CarcassData;
import com.terravera.common.butchery.CarcassSpecies;
import com.terravera.common.butchery.Freshness;
import com.terravera.common.skill.PlayerSkills;
import com.terravera.common.skill.SkillType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the claims the butchering system makes about itself: that skill matters, that tools matter, that neither
 * one alone is sufficient, and that a carcass left too long stops being worth cutting up. All of it is pure
 * arithmetic, so it runs without a game instance.
 */
public class ButcheryTest
{
    private static final ButcheryTool STONE_FLAKE = new ButcheryTool(0.35f, 1.0f, true, false);
    private static final ButcheryTool DULL_STEEL = new ButcheryTool(0.92f, 0.15f, true, false);
    private static final ButcheryTool SHARP_STEEL = new ButcheryTool(0.92f, 1.0f, true, false);

    private static CarcassData deerAt(ButcheryStage stage)
    {
        return new CarcassData(CarcassSpecies.DEER, stage, 0L, 0.5f, 0f);
    }

    // ----- Freshness -----

    @Test
    public void carcassesPassThroughTheBandsInOrder()
    {
        assertEquals(Freshness.FRESH, Freshness.of(0.5f, 15f));
        assertEquals(Freshness.COOL, Freshness.of(5f, 15f));
        assertEquals(Freshness.AGING, Freshness.of(20f, 15f));
        assertEquals(Freshness.SPOILING, Freshness.of(40f, 15f));
        assertEquals(Freshness.ROTTEN, Freshness.of(200f, 15f));
    }

    @Test
    public void heatSpoilsCarcassesFasterAndColdPreservesThem()
    {
        assertTrue(Freshness.spoilageRate(35f) > Freshness.spoilageRate(15f));
        assertTrue(Freshness.spoilageRate(-5f) < Freshness.spoilageRate(15f));

        // The same 20 hours is merely aged in the cold and past saving in the heat.
        assertEquals(Freshness.COOL, Freshness.of(20f, -10f));
        assertEquals(Freshness.ROTTEN, Freshness.of(20f, 40f));
    }

    @Test
    public void organsAreLostBeforeMeatAndDurablesOutlastBoth()
    {
        for (Freshness freshness : Freshness.values())
        {
            assertTrue(freshness.organYield() <= freshness.meatYield(),
                freshness + ": offal should never outlast the muscle");
            assertTrue(freshness.meatYield() <= freshness.durableYield(),
                freshness + ": hide and bone should outlast the meat");
        }
        assertFalse(Freshness.ROTTEN.edible());
        assertTrue(Freshness.ROTTEN.durableYield() > 0f, "a rotten carcass should still give up its hide");
    }

    // ----- Skill and tools -----

    @Test
    public void experiencedButchersCutBetterThanBeginners()
    {
        final CarcassData carcass = deerAt(ButcheryStage.PRIMALS);
        final float beginner = ButcheryYield.cutQuality(SHARP_STEEL, 0f, carcass, ButcheryStage.PRIMALS);
        final float expert = ButcheryYield.cutQuality(SHARP_STEEL, 0.9f, carcass, ButcheryStage.PRIMALS);
        assertTrue(expert > beginner, "practice should show in the cut");
    }

    @Test
    public void betterKnivesCutMoreCleanlyAndDullOnesWaste()
    {
        final CarcassData carcass = deerAt(ButcheryStage.PRIMALS);
        final float flake = ButcheryYield.cutQuality(STONE_FLAKE, 0.5f, carcass, ButcheryStage.PRIMALS);
        final float dull = ButcheryYield.cutQuality(DULL_STEEL, 0.5f, carcass, ButcheryStage.PRIMALS);
        final float sharp = ButcheryYield.cutQuality(SHARP_STEEL, 0.5f, carcass, ButcheryStage.PRIMALS);

        assertTrue(sharp > flake, "steel should beat a stone flake");
        assertTrue(sharp > dull, "a sharp knife should beat the same knife blunt");
        assertTrue(ButcheryYield.wastedFraction(dull, Freshness.COOL)
            > ButcheryYield.wastedFraction(sharp, Freshness.COOL), "a dull knife should waste more");
    }

    @Test
    public void dullToolsAlsoTakeLonger()
    {
        final int sharp = ButcheryYield.workTicks(SHARP_STEEL, 0.5f, CarcassSpecies.CATTLE, ButcheryStage.PRIMALS);
        final int dull = ButcheryYield.workTicks(DULL_STEEL, 0.5f, CarcassSpecies.CATTLE, ButcheryStage.PRIMALS);
        assertTrue(dull > sharp, "sawing at a carcass with a blunt knife should cost time as well as meat");
    }

    @Test
    public void neitherSkillNorEquipmentAloneMatchesBoth()
    {
        final CarcassData carcass = deerAt(ButcheryStage.PRIMALS);
        final float both = ButcheryYield.cutQuality(SHARP_STEEL, 0.9f, carcass, ButcheryStage.PRIMALS);
        final float skillOnly = ButcheryYield.cutQuality(STONE_FLAKE, 0.9f, carcass, ButcheryStage.PRIMALS);
        final float toolOnly = ButcheryYield.cutQuality(SHARP_STEEL, 0.0f, carcass, ButcheryStage.PRIMALS);

        assertTrue(both > skillOnly);
        assertTrue(both > toolOnly);
    }

    @Test
    public void earlyMistakesFollowTheCarcassThroughLaterStages()
    {
        final CarcassData careful = new CarcassData(CarcassSpecies.DEER, ButcheryStage.PRIMALS, 0L, 0.95f, 0f);
        final CarcassData botched = new CarcassData(CarcassSpecies.DEER, ButcheryStage.PRIMALS, 0L, 0.10f, 0.4f);

        assertTrue(ButcheryYield.cutQuality(SHARP_STEEL, 0.9f, careful, ButcheryStage.PRIMALS)
            > ButcheryYield.cutQuality(SHARP_STEEL, 0.9f, botched, ButcheryStage.PRIMALS),
            "a mangled carcass should stay mangled no matter how carefully it is finished");
    }

    // ----- Yields -----

    @Test
    public void everyStageProducesWhatItShould()
    {
        assertTrue(perform(ButcheryStage.BLED, Freshness.FRESH).products().containsKey(ButcheryYield.BLOOD));
        assertTrue(perform(ButcheryStage.SKINNED, Freshness.FRESH).products().containsKey(ButcheryYield.HIDE));

        final var drawn = perform(ButcheryStage.EVISCERATED, Freshness.FRESH).products();
        assertTrue(drawn.containsKey(ButcheryYield.HEART));
        assertTrue(drawn.containsKey(ButcheryYield.LIVER));
        assertTrue(drawn.containsKey(ButcheryYield.KIDNEYS));

        final var broken = perform(ButcheryStage.PRIMALS, Freshness.FRESH).products();
        assertTrue(broken.containsKey(ButcheryYield.MEAT_LEG));
        assertTrue(broken.containsKey(ButcheryYield.MEAT_LOIN));

        final var stripped = perform(ButcheryStage.RENDERED, Freshness.FRESH).products();
        assertTrue(stripped.containsKey(ButcheryYield.BONE));
        assertTrue(stripped.containsKey(ButcheryYield.SINEW));
        assertTrue(stripped.containsKey(ButcheryYield.ANIMAL_FAT));
    }

    @Test
    public void aRottenCarcassStillGivesHideAndBoneButNoFood()
    {
        assertTrue(perform(ButcheryStage.SKINNED, Freshness.ROTTEN).products().containsKey(ButcheryYield.HIDE));
        assertTrue(perform(ButcheryStage.RENDERED, Freshness.ROTTEN).products().containsKey(ButcheryYield.BONE));
        assertTrue(perform(ButcheryStage.EVISCERATED, Freshness.ROTTEN).isEmpty(),
            "there are no usable organs in a rotten animal");
        assertFalse(perform(ButcheryStage.PRIMALS, Freshness.ROTTEN).products()
            .containsKey(ButcheryYield.MEAT_LOIN), "and no usable meat either");
    }

    @Test
    public void promptButcheringRecoversMoreThanDelayedButchering()
    {
        final int fresh = totalMeat(perform(ButcheryStage.PRIMALS, Freshness.COOL).products());
        final int late = totalMeat(perform(ButcheryStage.PRIMALS, Freshness.SPOILING).products());
        assertTrue(fresh > late, "leaving an animal to turn should cost the player meat");
    }

    @Test
    public void largerAnimalsGiveMorePrimals()
    {
        assertTrue(CarcassSpecies.CATTLE.primalCount() > CarcassSpecies.DEER.primalCount());
        assertTrue(CarcassSpecies.DEER.primalCount() > CarcassSpecies.SMALL_GAME.primalCount());
    }

    @Test
    public void pigsGiveFarMoreFatThanDeerOfComparableSize()
    {
        final var pig = ButcheryYield.perform(
            new CarcassData(CarcassSpecies.PIG, ButcheryStage.RENDERED, 0L, 0.9f, 0f),
            SHARP_STEEL, 0.8f, Freshness.COOL, 0f).products();
        final var deer = ButcheryYield.perform(
            new CarcassData(CarcassSpecies.DEER, ButcheryStage.RENDERED, 0L, 0.9f, 0f),
            SHARP_STEEL, 0.8f, Freshness.COOL, 0f).products();

        assertTrue(pig.getOrDefault(ButcheryYield.ANIMAL_FAT, 0)
            > deer.getOrDefault(ButcheryYield.ANIMAL_FAT, 0), "venison is lean; pork is not");
    }

    // ----- Stage progression -----

    @Test
    public void stagesRunInAFixedOrderAndTerminate()
    {
        ButcheryStage stage = ButcheryStage.INTACT;
        int guard = 0;
        while (!stage.complete() && guard++ < 20) stage = stage.next();
        assertEquals(ButcheryStage.STRIPPED, stage);
        assertEquals(ButcheryStage.STRIPPED, ButcheryStage.STRIPPED.next());
    }

    @Test
    public void workmanshipIsCarriedForwardBetweenStages()
    {
        final CarcassData start = deerAt(ButcheryStage.BLED);
        final CarcassData good = start.advanced(1.0f, 0f);
        final CarcassData bad = start.advanced(0.0f, 0.3f);

        assertEquals(ButcheryStage.SKINNED, good.stage());
        assertTrue(good.workmanship() > start.workmanship());
        assertTrue(bad.workmanship() < start.workmanship());
        assertTrue(bad.waste() > good.waste());
    }

    @Test
    public void aBladeIsRequiredForTheRealWork()
    {
        assertFalse(ButcheryTool.BARE_HANDS.canPerform(ButcheryStage.PRIMALS));
        assertTrue(SHARP_STEEL.canPerform(ButcheryStage.PRIMALS));
        // A blunt knife can still open and skin an animal, just badly.
        assertTrue(DULL_STEEL.canPerform(ButcheryStage.SKINNED));
    }

    // ----- Skill integration -----

    @Test
    public void butcheryIsATrackedSkillIndependentOfTheOthers()
    {
        final PlayerSkills skills = PlayerSkills.EMPTY.learned(SkillType.BUTCHERY, 100f);
        assertEquals(100f, skills.experience(SkillType.BUTCHERY));
        assertEquals(0f, skills.experience(SkillType.COOKING));
        assertTrue(skills.proficiency(SkillType.BUTCHERY) > 0f);
        assertTrue(skills.proficiency(SkillType.BUTCHERY) < 1f);
    }

    @Test
    public void difficultAndBotchedWorkTeachesMore()
    {
        final float easy = ButcheryYield.experienceFor(ButcheryStage.BLED, CarcassSpecies.DEER, 0.9f);
        final float hard = ButcheryYield.experienceFor(ButcheryStage.PRIMALS, CarcassSpecies.DEER, 0.9f);
        final float botched = ButcheryYield.experienceFor(ButcheryStage.PRIMALS, CarcassSpecies.DEER, 0.1f);

        assertTrue(hard > easy);
        assertTrue(botched > hard, "you learn more from the cut that went wrong");
    }

    // ----- Species classification -----

    @Test
    public void animalsAreClassifiedByNameAndFallBackToSize()
    {
        assertEquals(CarcassSpecies.CATTLE, species("tfc:cow"));
        assertEquals(CarcassSpecies.PIG, species("tfc:pig"));
        assertEquals(CarcassSpecies.SHEEP, species("tfc:sheep"));
        assertEquals(CarcassSpecies.FOWL, species("tfc:chicken"));
        assertEquals(CarcassSpecies.DEER, species("tfc:deer"));

        // An unknown modded animal is sized from its hitbox rather than crashing or defaulting to a rabbit.
        assertEquals(CarcassSpecies.LARGE_GAME, CarcassSpecies.fromEntityId(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("somemod", "megafauna"), 9.0f));
        assertEquals(CarcassSpecies.SMALL_GAME, CarcassSpecies.fromEntityId(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("somemod", "critter"), 0.05f));
    }

    private static CarcassSpecies species(String id)
    {
        return CarcassSpecies.fromEntityId(
            net.minecraft.resources.ResourceLocation.parse(id), 1.0f);
    }

    private static ButcheryYield.Result perform(ButcheryStage stage, Freshness freshness)
    {
        return ButcheryYield.perform(
            new CarcassData(CarcassSpecies.DEER, stage, 0L, 0.9f, 0f),
            SHARP_STEEL, 0.8f, freshness, 0f);
    }

    private static int totalMeat(java.util.Map<String, Integer> products)
    {
        return products.getOrDefault(ButcheryYield.MEAT_LEG, 0)
            + products.getOrDefault(ButcheryYield.MEAT_LOIN, 0)
            + products.getOrDefault(ButcheryYield.MEAT_SHOULDER, 0)
            + products.getOrDefault(ButcheryYield.MEAT_RIBS, 0);
    }
}
