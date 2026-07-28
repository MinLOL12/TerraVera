/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 */

package com.terravera;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Every butchering product must be a real, renderable, translated item, and every product must lead somewhere.
 * A part with no recipe attached to it is just clutter, so the last test here asserts that each of the six product
 * families the system advertises actually feeds a downstream recipe.
 */
public class ButcheryResourceTest
{
    private static final Path ROOT = Path.of("src/main/resources");

    private static final List<String> PRODUCTS = List.of(
        "carcass", "shoulder_cut", "rib_cut", "loin_cut", "leg_cut", "trim_meat",
        "cured_meat", "dried_meat_strips", "animal_fat", "suet", "rendered_tallow", "tallow_candle",
        "sinew", "tendon", "sinew_cord", "sinew_bowstring", "blood", "blood_meal",
        "heart", "liver", "kidneys", "stomach", "marrow_bone", "bone_marrow", "bone_needle", "bone_awl");

    @Test
    public void everyProductHasAnItemModel()
    {
        for (String product : PRODUCTS)
        {
            assertTrue(Files.exists(ROOT.resolve("assets/terravera/models/item/" + product + ".json")),
                "Missing item model for " + product);
        }
    }

    @Test
    public void everyProductHasATexture()
    {
        for (String product : PRODUCTS)
        {
            assertTrue(Files.exists(ROOT.resolve("assets/terravera/textures/item/" + product + ".png")),
                "Missing texture for " + product);
        }
    }

    @Test
    public void everyProductIsTranslated() throws IOException
    {
        final String lang = Files.readString(ROOT.resolve("assets/terravera/lang/en_us.json"));
        for (String product : PRODUCTS)
        {
            assertTrue(lang.contains("\"item.terravera." + product + "\""),
                "Missing translation for item.terravera." + product);
        }
    }

    @Test
    public void everyStageFreshnessAndSpeciesIsTranslated() throws IOException
    {
        final String lang = Files.readString(ROOT.resolve("assets/terravera/lang/en_us.json"));
        for (com.terravera.common.butchery.ButcheryStage stage : com.terravera.common.butchery.ButcheryStage.values())
        {
            assertTrue(lang.contains("\"terravera.butchery.stage." + stage.id() + "\""),
                "Missing translation for stage " + stage.id());
        }
        for (com.terravera.common.butchery.Freshness freshness : com.terravera.common.butchery.Freshness.values())
        {
            assertTrue(lang.contains("\"terravera.freshness." + freshness.id() + "\""),
                "Missing translation for freshness " + freshness.id());
        }
        for (String species : List.of("small_game", "fowl", "sheep", "goat", "pig", "deer",
            "cattle", "large_game", "predator"))
        {
            assertTrue(lang.contains("\"terravera.carcass.species." + species + "\""),
                "Missing translation for species " + species);
        }
        assertTrue(lang.contains("\"terravera.skill.butchery\""), "Butchery skill needs a name");
    }

    @Test
    public void everyProductFamilyFeedsAnotherSystem() throws IOException
    {
        final String recipes = allButcheryRecipes();

        // Fat -> soap, candles, cooking.
        assertTrue(recipes.contains("terravera:rendered_tallow"), "fat should render into tallow");
        assertTrue(recipes.contains("terravera:tallow_candle"), "fat should make candles");
        assertTrue(recipes.contains("terravera:soap_curd"), "fat should reach the soap chain");

        // Sinew -> cordage, bowstrings, sewing.
        assertTrue(recipes.contains("terravera:sinew_cord"), "sinew should make cordage");
        assertTrue(recipes.contains("terravera:sinew_bowstring"), "sinew should make bowstrings");
        assertTrue(recipes.contains("minecraft:string"), "sinew should reach the sewing chain");

        // Bone -> meal, needles, broth.
        assertTrue(recipes.contains("minecraft:bone_meal"), "bone should make bone meal");
        assertTrue(recipes.contains("terravera:bone_needle"), "bone should make needles");
        assertTrue(recipes.contains("terravera:bone_marrow"), "bone should make broth/marrow");

        // Blood -> fertiliser and food.
        assertTrue(recipes.contains("terravera:blood_meal"), "blood should make fertiliser");

        // Organs -> food and medicine.
        assertTrue(recipes.contains("terravera:medicine"), "organs should reach the medicine chain");

        // Meat -> preservation.
        assertTrue(recipes.contains("terravera:cured_meat"), "meat should be preservable");
        assertTrue(recipes.contains("terravera:dried_meat_strips"), "meat should be dryable");
    }

    @Test
    public void bloodMealIsASoilAmendmentAndSinewIsCordage() throws IOException
    {
        assertTrue(Files.readString(ROOT.resolve("data/terravera/tags/item/soil_amendments.json"))
            .contains("terravera:blood_meal"), "blood meal should feed the farming system");
        assertTrue(Files.readString(ROOT.resolve("data/terravera/tags/item/cordage.json"))
            .contains("terravera:sinew_cord"), "sinew cord should count as cordage for lashing");
    }

    @Test
    public void rawCutsAndOffalAreTreatedAsRiskyToEatRaw() throws IOException
    {
        final String risky = Files.readString(ROOT.resolve("data/terravera/tags/item/risky_raw_meat.json"));
        for (String raw : List.of("loin_cut", "liver", "heart", "kidneys", "blood"))
        {
            assertTrue(risky.contains("terravera:" + raw), raw + " should carry raw-eating risk");
        }
    }

    /** The seed system was removed; nothing should reference the items it used to register. */
    @Test
    public void theCustomSeedSystemIsFullyGone() throws IOException
    {
        assertFalse(Files.exists(ROOT.resolve("assets/terravera/models/item/seed.json")));
        assertFalse(Files.exists(ROOT.resolve("assets/terravera/models/item/select_seed.json")));
        assertFalse(Files.exists(ROOT.resolve("assets/terravera/models/item/prize_seed.json")));
        assertFalse(Files.exists(ROOT.resolve("assets/terravera/blockstates/crop.json")));
        assertFalse(Files.exists(ROOT.resolve("data/terravera/loot_table/blocks/crop.json")));

        // The seed recipes now produce TFC seeds instead.
        final String wheat = Files.readString(ROOT.resolve("data/terravera/recipe/seed/seed_from_wheat.json"));
        assertTrue(wheat.contains("tfc:seeds/wheat"), "seed recipes should hand back TFC seeds");
        assertFalse(wheat.contains("terravera:seed"), "no TerraVera seed item should remain");
    }

    private static String allButcheryRecipes() throws IOException
    {
        final Path dir = ROOT.resolve("data/terravera/recipe/butchery");
        assertTrue(Files.isDirectory(dir), "butchery recipes should exist");
        final StringBuilder all = new StringBuilder();
        try (var stream = Files.walk(dir))
        {
            for (Path path : stream.filter(p -> p.toString().endsWith(".json")).toList())
            {
                all.append(Files.readString(path));
            }
        }
        return all.toString();
    }
}
