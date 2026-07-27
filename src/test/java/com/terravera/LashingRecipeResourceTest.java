/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Guards the rock-category-to-tool mappings used when TerraVera heads are lashed. */
public class LashingRecipeResourceTest
{
    private static final String[] MATERIALS = {
        "igneous_extrusive", "igneous_intrusive", "metamorphic", "sedimentary"
    };

    @Test
    public void everyLashableHeadProducesTheMatchingTfcStoneTool() throws IOException
    {
        assertStoneToolResults("axe", "wedge", "axe");
        assertStoneToolResults("hammer", "maul", "hammer");
        assertStoneToolResults("hoe", "broad", "hoe");
        assertStoneToolResults("javelin", "point", "javelin");
        assertStoneToolResults("knife", "blade", "knife");
        assertStoneToolResults("shovel", "broad", "shovel");
    }

    @Test
    public void nativeKnifeRecipesAlsoRequireCordage() throws IOException
    {
        for (String material : MATERIALS)
        {
            final JsonObject recipe = read("/data/tfc/recipe/crafting/stone/knife/" + material + ".json");
            assertEquals("minecraft:crafting_shapeless", recipe.get("type").getAsString());
            assertEquals("tfc:stone/knife/" + material, recipe.getAsJsonObject("result").get("id").getAsString());
            assertHasNormalCordage(recipe.getAsJsonArray("ingredients"));
        }

        final JsonObject obsidian = read("/data/tfc/recipe/crafting/obsidian_knife.json");
        assertEquals("tfc:obsidian_knife", obsidian.getAsJsonObject("result").get("id").getAsString());
        assertHasNormalCordage(obsidian.getAsJsonArray("ingredients"));
    }

    private static void assertStoneToolResults(String recipeName, String headKind, String tool) throws IOException
    {
        final JsonObject recipe = read("/data/terravera/recipe/lashing/" + recipeName + ".json");
        assertEquals("terravera:lashing", recipe.get("type").getAsString());
        assertEquals(headKind, recipe.get("head_kind").getAsString());

        final JsonArray results = recipe.getAsJsonArray("results");
        assertEquals(MATERIALS.length, results.size());
        for (String material : MATERIALS)
        {
            boolean found = false;
            for (int i = 0; i < results.size(); i++)
            {
                final JsonObject result = results.get(i).getAsJsonObject();
                if (material.equals(result.get("material").getAsString())
                    && ("tfc:stone/" + tool + "/" + material)
                        .equals(result.getAsJsonObject("result").get("id").getAsString()))
                {
                    found = true;
                    break;
                }
            }
            assertTrue(found, () -> recipeName + " is missing its " + material + " " + tool + " result");
        }
    }

    private static void assertHasNormalCordage(JsonArray ingredients)
    {
        boolean found = false;
        for (int i = 0; i < ingredients.size(); i++)
        {
            if ("terravera:normal_cordage".equals(ingredients.get(i).getAsJsonObject().get("tag").getAsString()))
            {
                found = true;
                break;
            }
        }
        assertTrue(found, "Knife recipe must require normal cordage");
    }

    private static JsonObject read(String resource) throws IOException
    {
        try (InputStream stream = LashingRecipeResourceTest.class.getResourceAsStream(resource))
        {
            assertNotNull(stream, "Missing recipe " + resource);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
