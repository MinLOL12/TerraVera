/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 */

package com.terravera;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Resource tests for the farming system: verifies that every recipe, data file, model, blockstate,
 * and animation referenced by the new farming and greenhouse code actually exists in the resource tree.
 */
public class FarmingResourceTest
{
    private static final Path ROOT = Path.of("src/main/resources");

    // ----- Recipe files exist -----

    @Test
    public void allGreenhouseRecipesExist() throws IOException
    {
        String[] expected = {
            "recipe/greenhouse/greenhouse_glass.json",
            "recipe/greenhouse/greenhouse_glass_from_sand.json",
            "recipe/greenhouse/cold_frame.json",
            "recipe/greenhouse/hoop_frame.json",
            "recipe/greenhouse/oiled_cloth_covering.json",
            "recipe/greenhouse/oiled_cloth_from_linen.json",
            "recipe/greenhouse/hoop_house.json",
            "recipe/greenhouse/greenhouse_frame.json",
            "recipe/greenhouse/glass_greenhouse.json",
            "recipe/greenhouse/thermostat.json",
            "recipe/greenhouse/modern_greenhouse.json",
            "recipe/greenhouse/irrigation_controller.json"
        };
        for (String recipe : expected)
        {
            assertTrue(Files.exists(ROOT.resolve(recipe)), "Missing recipe: " + recipe);
        }
    }

    @Test
    public void allSoilPreparationRecipesExist() throws IOException
    {
        String[] expected = {
            "recipe/soil/digging_stick.json",
            "recipe/soil/digging_stick_from_bone.json",
            "recipe/soil/soil_rake.json",
            "recipe/soil/compost_from_food.json",
            "recipe/soil/compost_from_plants.json",
            "recipe/soil/aged_manure.json",
            "recipe/soil/horticultural_sand.json",
            "recipe/soil/agricultural_lime.json"
        };
        for (String recipe : expected)
        {
            assertTrue(Files.exists(ROOT.resolve(recipe)), "Missing recipe: " + recipe);
        }
    }

    @Test
    public void allSeedRecipesExist() throws IOException
    {
        String[] expected = {
            "recipe/seed/seed_from_wheat.json",
            "recipe/seed/seed_from_barley.json",
            "recipe/seed/seed_from_oat.json",
            "recipe/seed/seed_from_rye.json",
            "recipe/seed/seed_from_maize.json",
            "recipe/seed/seed_from_rice.json",
            "recipe/seed/seed_from_wheat_grain.json",
            "recipe/seed/select_seed.json",
            "recipe/seed/prize_seed.json"
        };
        for (String recipe : expected)
        {
            assertTrue(Files.exists(ROOT.resolve(recipe)), "Missing recipe: " + recipe);
        }
    }

    @Test
    public void allIrrigationRecipesExist() throws IOException
    {
        String[] expected = {
            "recipe/irrigation/watering_can.json",
            "recipe/irrigation/watering_can_from_copper.json",
            "recipe/irrigation/irrigation_tank.json",
            "recipe/irrigation/irrigation_tank_stone.json",
            "recipe/irrigation/drip_irrigation.json",
            "recipe/irrigation/drip_irrigation_copper.json"
        };
        for (String recipe : expected)
        {
            assertTrue(Files.exists(ROOT.resolve(recipe)), "Missing recipe: " + recipe);
        }
    }

    @Test
    public void allDiseaseTreatmentRecipesExist() throws IOException
    {
        String[] expected = {
            "recipe/disease_treatment/bordeaux_mixture.json",
            "recipe/disease_treatment/bordeaux_mixture_from_treated_water.json",
            "recipe/disease_treatment/neem_oil.json",
            "recipe/disease_treatment/companion_chart.json"
        };
        for (String recipe : expected)
        {
            assertTrue(Files.exists(ROOT.resolve(recipe)), "Missing recipe: " + recipe);
        }
    }

    @Test
    public void allPotRecipesExist() throws IOException
    {
        String[] expected = {
            "recipe/pot/bordeaux_mixture_pot.json",
            "recipe/pot/compost_tea.json",
            "recipe/pot/neem_oil_extraction.json"
        };
        for (String recipe : expected)
        {
            assertTrue(Files.exists(ROOT.resolve(recipe)), "Missing recipe: " + recipe);
        }
    }

    // ----- Total recipe count exceeds 30 -----

    @Test
    public void atLeast30NewFarmingRecipes() throws IOException
    {
        int count = 0;
        try (Stream<Path> stream = Files.walk(ROOT.resolve("data/terravera/recipe")))
        {
            count = (int) stream.filter(p -> p.toString().endsWith(".json") &&
                (p.toString().contains("/greenhouse/") || p.toString().contains("/soil/") ||
                 p.toString().contains("/seed/") || p.toString().contains("/irrigation/") ||
                 p.toString().contains("/disease_treatment/")))
                .count();
        }
        assertTrue(count >= 30, "Expected at least 30 new farming recipes, found " + count);
    }

    // ----- Model and animation files exist -----

    @Test
    public void allGreenhouseGeoModelsExist() throws IOException
    {
        String[] models = {
            "assets/terravera/geo/cold_frame.geo.json",
            "assets/terravera/geo/hoop_house.geo.json",
            "assets/terravera/geo/glass_greenhouse.geo.json",
            "assets/terravera/geo/modern_greenhouse.geo.json",
            "assets/terravera/geo/irrigation_tank.geo.json"
        };
        for (String model : models)
        {
            assertTrue(Files.exists(ROOT.resolve(model)), "Missing geo model: " + model);
        }
    }

    @Test
    public void allAnimationFilesExist() throws IOException
    {
        assertTrue(Files.exists(ROOT.resolve("assets/terravera/animations/greenhouse.animation.json")));
        assertTrue(Files.exists(ROOT.resolve("assets/terravera/animations/irrigation_tank.animation.json")));
    }

    @Test
    public void allBlockstateFilesExist() throws IOException
    {
        String[] blockstates = {
            "assets/terravera/blockstates/cold_frame.json",
            "assets/terravera/blockstates/hoop_house.json",
            "assets/terravera/blockstates/glass_greenhouse.json",
            "assets/terravera/blockstates/modern_greenhouse.json",
            "assets/terravera/blockstates/prepared_farmland.json",
            "assets/terravera/blockstates/drip_irrigation.json",
            "assets/terravera/blockstates/irrigation_tank.json"
        };
        for (String bs : blockstates)
        {
            assertTrue(Files.exists(ROOT.resolve(bs)), "Missing blockstate: " + bs);
        }
    }

    // ----- Data files exist -----

    @Test
    public void cropDiseaseDataFilesExist() throws IOException
    {
        assertTrue(Files.exists(ROOT.resolve("data/terravera/terravera/crop_disease/fungal_blight.json")));
        assertTrue(Files.exists(ROOT.resolve("data/terravera/terravera/crop_disease/root_rot.json")));
        assertTrue(Files.exists(ROOT.resolve("data/terravera/terravera/crop_disease/aphid_infestation.json")));
        assertTrue(Files.exists(ROOT.resolve("data/terravera/terravera/crop_disease/nitrogen_deficiency.json")));
    }

    @Test
    public void greenhouseCropDataFilesExist() throws IOException
    {
        assertTrue(Files.exists(ROOT.resolve("data/terravera/terravera/greenhouse_crop/tomato.json")));
        assertTrue(Files.exists(ROOT.resolve("data/terravera/terravera/greenhouse_crop/pepper.json")));
        assertTrue(Files.exists(ROOT.resolve("data/terravera/terravera/greenhouse_crop/strawberry.json")));
        assertTrue(Files.exists(ROOT.resolve("data/terravera/terravera/greenhouse_crop/lettuce.json")));
        assertTrue(Files.exists(ROOT.resolve("data/terravera/terravera/greenhouse_crop/wheat.json")));
    }

    @Test
    public void seasonalDataFilesExist() throws IOException
    {
        assertTrue(Files.exists(ROOT.resolve("data/terravera/terravera/seasonal/winter_growing.json")));
        assertTrue(Files.exists(ROOT.resolve("data/terravera/terravera/seasonal/summer_growing.json")));
    }

    // ----- Tags exist -----

    @Test
    public void blockTagsExist() throws IOException
    {
        assertTrue(Files.exists(ROOT.resolve("data/terravera/tags/block/greenhouse_structure.json")));
        assertTrue(Files.exists(ROOT.resolve("data/terravera/tags/block/crop_disease_hosts.json")));
        assertTrue(Files.exists(ROOT.resolve("data/terravera/tags/block/preparable_soil.json")));
    }

    @Test
    public void itemTagsExist() throws IOException
    {
        assertTrue(Files.exists(ROOT.resolve("data/terravera/tags/item/seeds.json")));
        assertTrue(Files.exists(ROOT.resolve("data/terravera/tags/item/soil_amendments.json")));
        assertTrue(Files.exists(ROOT.resolve("data/terravera/tags/item/crop_treatments.json")));
    }
}
