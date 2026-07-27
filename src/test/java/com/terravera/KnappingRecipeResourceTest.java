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

/**
 * Guards the representative recipes that expose TerraVera heads to TFC's Rock Knapping recipe integrations.
 * TerraVera accepts profiles rather than exact silhouettes at runtime, but TFC and JEI still need concrete
 * {@code tfc:knapping} recipes in order to discover and display each output.
 */
public class KnappingRecipeResourceTest
{
    private static final String[] KINDS = {"blade", "broad", "maul", "point", "wedge"};
    private static final String[] MATERIALS = {
        "igneous_extrusive", "igneous_intrusive", "metamorphic", "sedimentary"
    };

    @Test
    public void everyHeadAndRockCategoryHasATfcKnappingRecipe() throws IOException
    {
        for (String kind : KINDS)
        {
            for (String material : MATERIALS)
            {
                final String resource = "/data/terravera/recipe/knapping/stone/" + kind + "/" + material + ".json";
                try (InputStream stream = KnappingRecipeResourceTest.class.getResourceAsStream(resource))
                {
                    assertNotNull(stream, "Missing knapping recipe " + resource);
                    final JsonObject recipe = JsonParser.parseReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();

                    assertEquals("tfc:knapping", recipe.get("type").getAsString(), resource);
                    assertEquals("tfc:rock", recipe.get("knapping_type").getAsString(), resource);
                    assertEquals("tfc:stones/loose/" + material,
                        recipe.getAsJsonObject("ingredient").get("tag").getAsString(), resource);

                    final JsonArray pattern = recipe.getAsJsonArray("pattern");
                    assertEquals(5, pattern.size(), resource + " must show a full 5x5 pattern");
                    pattern.forEach(row -> assertEquals(5, row.getAsString().length(), resource));

                    final JsonObject result = recipe.getAsJsonObject("result");
                    assertEquals("terravera:head/" + kind, result.get("id").getAsString(), resource);
                    final JsonObject head = result.getAsJsonObject("components")
                        .getAsJsonObject("terravera:knapped_head");
                    assertEquals("terravera:" + kind, head.get("kind").getAsString(), resource);
                    assertEquals(material, head.get("material").getAsString(), resource);
                }
            }
        }
    }
}
