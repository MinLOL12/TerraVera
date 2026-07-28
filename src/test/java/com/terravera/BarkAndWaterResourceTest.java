/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 */

package com.terravera;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import com.terravera.common.component.BarkProperties;

import static org.junit.jupiter.api.Assertions.*;

/** Guards the playable resource chains and the layered GeckoLib models introduced with bark and water collection. */
public class BarkAndWaterResourceTest
{
    private static final List<String> COLLECTORS =
        List.of("rain_catcher", "dew_collector", "rock_basin", "solar_still");
    private static final List<String> SPECIES =
        List.of("oak", "hemlock", "willow", "birch", "bast", "");

    @Test
    public void barkPropertiesClampAndActuallyDry()
    {
        final BarkProperties wet = new BarkProperties("oak", 1.4f, 2f, -1f, 0.75f, -3f);
        assertEquals(1f, wet.moisture());
        assertEquals(1f, wet.tannin());
        assertEquals(0f, wet.flexibility());
        assertEquals(0.1f, wet.thicknessMm());
        assertFalse(wet.isDry());
        assertTrue(wet.dried().isDry());
        assertEquals("oak", wet.dried().species());
    }

    @Test
    public void everyCollectorHasACompleteGeckoAssetSetAndRecipe()
    {
        for (String collector : COLLECTORS)
        {
            assertResource("/assets/terravera/geo/" + collector + ".geo.json");
            assertResource("/assets/terravera/models/block/" + collector + ".json");
            assertResource("/assets/terravera/models/item/" + collector + ".json");
            assertResource("/assets/terravera/blockstates/" + collector + ".json");
            assertResource("/data/terravera/loot_table/blocks/" + collector + ".json");
            assertResource("/data/terravera/recipe/crafting/" + collector + ".json");
        }
        assertResource("/assets/terravera/animations/water_collector.animation.json");
        assertResource("/assets/terravera/textures/block/water_collectors.png");
    }

    @Test
    public void collectorModelsAreLayeredAndHaveAnimatedWater() throws IOException
    {
        for (String collector : COLLECTORS)
        {
            final JsonArray bones = read("/assets/terravera/geo/" + collector + ".geo.json")
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
            assertTrue(bones.size() >= 2, collector + " is a flat placeholder rather than layered geometry");
            boolean water = false;
            int cubes = 0;
            for (JsonElement value : bones)
            {
                final JsonObject bone = value.getAsJsonObject();
                water |= "water".equals(bone.get("name").getAsString());
                if (bone.has("cubes")) cubes += bone.getAsJsonArray("cubes").size();
            }
            assertTrue(water, collector + " has no water bone for the ripple animation");
            assertTrue(cubes >= 10, collector + " does not have enough physical layers to read as a 3D structure");
        }
    }

    @Test
    public void everySpeciesHasFreshDryModelsAndADryingRecipe()
    {
        for (String species : SPECIES)
        {
            final String fresh = species.isEmpty() ? "bark" : species + "_bark";
            final String dry = species.isEmpty() ? "dried_bark" : "dried_" + species + "_bark";
            assertResource("/assets/terravera/models/item/" + fresh + ".json");
            assertResource("/assets/terravera/models/item/" + dry + ".json");
            assertResource("/assets/terravera/textures/item/" + fresh + ".png");
            assertResource("/assets/terravera/textures/item/" + dry + ".png");
            assertResource("/data/terravera/recipe/drying/" + fresh + ".json");
        }
    }

    @Test
    public void airConditionerGeometryIsCenteredOnItsBlock() throws IOException
    {
        final JsonArray bones = read("/assets/terravera/geo/air_conditioner.geo.json")
            .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
        final JsonArray baseOrigin = bones.get(0).getAsJsonObject().getAsJsonArray("cubes")
            .get(0).getAsJsonObject().getAsJsonArray("origin");
        assertEquals(-8, baseOrigin.get(0).getAsInt());
        assertEquals(-8, baseOrigin.get(2).getAsInt());
    }

    private static JsonObject read(String resource) throws IOException
    {
        try (InputStream stream = BarkAndWaterResourceTest.class.getResourceAsStream(resource))
        {
            assertNotNull(stream, "Missing resource " + resource);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static void assertResource(String resource)
    {
        try (InputStream stream = BarkAndWaterResourceTest.class.getResourceAsStream(resource))
        {
            assertNotNull(stream, "Missing resource " + resource);
        }
        catch (IOException e)
        {
            fail(e);
        }
    }
}
