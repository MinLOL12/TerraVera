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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import com.terravera.common.temperature.ClothingMaterial;
import com.terravera.common.temperature.GarmentSlot;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the shipped wardrobe.
 * <p>
 * A garment that has no recipe, no model, or no texture is dead content that only shows up as a missing-texture cube
 * in someone's creative tab. These tests make the roster, the recipes, and the assets prove they agree with each
 * other, and check that the progression the design promises is actually the one the recipes encode.
 */
public class ClothingResourceTest
{
    /**
     * The complete shipped wardrobe: registry name, material, and slot. If a garment is added or removed, it goes
     * here too - which is the point: this list is the contract.
     */
    private static final List<Garment> WARDROBE = List.of(
        new Garment("fiber_cap", ClothingMaterial.PLANT_FIBER, GarmentSlot.HEAD),
        new Garment("fiber_poncho", ClothingMaterial.PLANT_FIBER, GarmentSlot.CHEST),
        new Garment("fiber_leggings", ClothingMaterial.PLANT_FIBER, GarmentSlot.LEGS),
        new Garment("fiber_sandals", ClothingMaterial.PLANT_FIBER, GarmentSlot.FEET),
        new Garment("straw_sun_hat", ClothingMaterial.STRAW, GarmentSlot.HEAD),
        new Garment("straw_rain_cape", ClothingMaterial.STRAW, GarmentSlot.CHEST),
        new Garment("burlap_hood", ClothingMaterial.BURLAP, GarmentSlot.HEAD),
        new Garment("burlap_tunic", ClothingMaterial.BURLAP, GarmentSlot.CHEST),
        new Garment("burlap_trousers", ClothingMaterial.BURLAP, GarmentSlot.LEGS),
        new Garment("burlap_shoes", ClothingMaterial.BURLAP, GarmentSlot.FEET),
        new Garment("linen_headwrap", ClothingMaterial.LINEN, GarmentSlot.HEAD),
        new Garment("linen_shirt", ClothingMaterial.LINEN, GarmentSlot.CHEST),
        new Garment("linen_trousers", ClothingMaterial.LINEN, GarmentSlot.LEGS),
        new Garment("linen_shoes", ClothingMaterial.LINEN, GarmentSlot.FEET),
        new Garment("wool_cap", ClothingMaterial.WOOL, GarmentSlot.HEAD),
        new Garment("wool_sweater", ClothingMaterial.WOOL, GarmentSlot.CHEST),
        new Garment("wool_trousers", ClothingMaterial.WOOL, GarmentSlot.LEGS),
        new Garment("wool_socks", ClothingMaterial.WOOL, GarmentSlot.FEET),
        new Garment("felt_hat", ClothingMaterial.FELT, GarmentSlot.HEAD),
        new Garment("felt_coat", ClothingMaterial.FELT, GarmentSlot.CHEST),
        new Garment("felt_leggings", ClothingMaterial.FELT, GarmentSlot.LEGS),
        new Garment("felt_boots", ClothingMaterial.FELT, GarmentSlot.FEET),
        new Garment("leather_cap", ClothingMaterial.LEATHER, GarmentSlot.HEAD),
        new Garment("leather_jerkin", ClothingMaterial.LEATHER, GarmentSlot.CHEST),
        new Garment("leather_trousers", ClothingMaterial.LEATHER, GarmentSlot.LEGS),
        new Garment("leather_boots", ClothingMaterial.LEATHER, GarmentSlot.FEET),
        new Garment("oilskin_hat", ClothingMaterial.OILSKIN, GarmentSlot.HEAD),
        new Garment("oilskin_cloak", ClothingMaterial.OILSKIN, GarmentSlot.CHEST),
        new Garment("oilskin_leggings", ClothingMaterial.OILSKIN, GarmentSlot.LEGS),
        new Garment("oilskin_boots", ClothingMaterial.OILSKIN, GarmentSlot.FEET),
        new Garment("fur_hood", ClothingMaterial.FUR, GarmentSlot.HEAD),
        new Garment("fur_parka", ClothingMaterial.FUR, GarmentSlot.CHEST),
        new Garment("fur_leggings", ClothingMaterial.FUR, GarmentSlot.LEGS),
        new Garment("fur_boots", ClothingMaterial.FUR, GarmentSlot.FEET),
        new Garment("silk_veil", ClothingMaterial.SILK, GarmentSlot.HEAD),
        new Garment("silk_robe", ClothingMaterial.SILK, GarmentSlot.CHEST),
        new Garment("silk_trousers", ClothingMaterial.SILK, GarmentSlot.LEGS),
        new Garment("silk_slippers", ClothingMaterial.SILK, GarmentSlot.FEET),
        new Garment("quilted_hood", ClothingMaterial.QUILTED, GarmentSlot.HEAD),
        new Garment("quilted_coat", ClothingMaterial.QUILTED, GarmentSlot.CHEST),
        new Garment("quilted_leggings", ClothingMaterial.QUILTED, GarmentSlot.LEGS),
        new Garment("quilted_boots", ClothingMaterial.QUILTED, GarmentSlot.FEET)
    );

    private record Garment(String name, ClothingMaterial material, GarmentSlot slot) {}

    /** The four reusable sewn shapes every garment is built on. */
    private static final List<String> PANELS =
        List.of("sewn_hood_panel", "sewn_body_panel", "sewn_leg_panel", "sewn_foot_panel");

    /** The textile chain that feeds the wardrobe. */
    private static final List<String> INTERMEDIATES = List.of(
        "plant_fiber_cloth", "straw_mat", "linen_cloth", "felt_cloth",
        "oilskin_cloth", "quilted_cloth", "fur_pelt", "dubbin", "batting"
    );

    // ----- The roster ---------------------------------------------------------------------------------------

    @Test
    public void theWardrobeIsLargeEnoughToBeARealChoice()
    {
        assertTrue(WARDROBE.size() > 30,
            "the point of the clothing system is having options; only " + WARDROBE.size() + " garments exist");
    }

    @Test
    public void everyMaterialLineCoversTheWholeBody()
    {
        // A line that cannot dress you head to toe is a novelty, not a strategy. The two deliberate exceptions are
        // straw, which only ever made sense as a hat and a cape.
        for (ClothingMaterial material : ClothingMaterial.values())
        {
            if (material == ClothingMaterial.BARE || material == ClothingMaterial.STRAW) continue;

            for (GarmentSlot slot : GarmentSlot.values())
            {
                assertTrue(WARDROBE.stream().anyMatch(g -> g.material() == material && g.slot() == slot),
                    material + " has no garment for " + slot);
            }
        }
    }

    @Test
    public void noTwoGarmentsShareARegistryName()
    {
        final Set<String> names = new java.util.HashSet<>();
        for (Garment garment : WARDROBE)
        {
            assertTrue(names.add(garment.name()), "duplicate garment name " + garment.name());
        }
    }

    // ----- Assets -------------------------------------------------------------------------------------------

    @Test
    public void everyGarmentHasAModelPointingAtItsOwnTexture() throws IOException
    {
        for (Garment garment : WARDROBE)
        {
            final JsonObject model = read("/assets/terravera/models/item/" + garment.name() + ".json");
            assertEquals("item/generated", model.get("parent").getAsString(), garment.name() + " model parent");

            final String layer = model.getAsJsonObject("textures").get("layer0").getAsString();
            assertEquals("terravera:item/clothing/" + garment.name(), layer,
                garment.name() + " model points at the wrong texture");
            assertResourceExists("/assets/terravera/textures/item/clothing/" + garment.name() + ".png");
        }
    }

    @Test
    public void everyMaterialLineHasBothWornArmourLayers()
    {
        // Without these the garment equips and then renders as nothing at all on the player.
        for (ClothingMaterial material : ClothingMaterial.values())
        {
            if (material == ClothingMaterial.BARE) continue;
            assertResourceExists("/assets/terravera/textures/models/armor/" + material.id() + "_layer_1.png");
            assertResourceExists("/assets/terravera/textures/models/armor/" + material.id() + "_layer_2.png");
        }
    }

    @Test
    public void everyGarmentAndIntermediateIsNamedInEnglish() throws IOException
    {
        final JsonObject lang = read("/assets/terravera/lang/en_us.json");
        for (Garment garment : WARDROBE)
        {
            assertTrue(lang.has("item.terravera." + garment.name()),
                garment.name() + " has no en_us name and will show as a translation key");
        }
        for (String name : INTERMEDIATES)
        {
            assertTrue(lang.has("item.terravera." + name), name + " has no en_us name");
        }
        for (String name : PANELS)
        {
            assertTrue(lang.has("item.terravera." + name), name + " has no en_us name");
        }
    }

    @Test
    public void everyMaterialAndWetnessBandIsTranslated() throws IOException
    {
        final JsonObject lang = read("/assets/terravera/lang/en_us.json");
        for (ClothingMaterial material : ClothingMaterial.values())
        {
            assertTrue(lang.has(material.translationKey()), material + " has no translated name");
        }
        for (String key : List.of("damp", "wet", "soaked"))
        {
            assertTrue(lang.has("terravera.clothing.wetness." + key), "wetness band " + key + " is untranslated");
        }
    }

    /** The symptom wording is the entire player-facing surface of the temperature system. It has to be there. */
    @Test
    public void everyTemperatureSymptomHasWording() throws IOException
    {
        final JsonObject lang = read("/assets/terravera/lang/en_us.json");
        for (com.terravera.common.temperature.ThermalModel.Band band :
            com.terravera.common.temperature.ThermalModel.Band.values())
        {
            assertTrue(lang.has(band.messageKey()), band + " has no symptom message");
            assertTrue(lang.has(band.descriptorKey()), band + " has no field-notes descriptor");
        }
    }

    @Test
    public void theSymptomWordingIsThePhrasingTheDesignAsksFor() throws IOException
    {
        final JsonObject lang = read("/assets/terravera/lang/en_us.json");
        assertEquals("You feel slightly chilled.", lang.get("terravera.temperature.mild_cold").getAsString());
        assertEquals("Your hands feel stiff.", lang.get("terravera.temperature.moderate_cold").getAsString());
        assertEquals("You are struggling to stay warm.", lang.get("terravera.temperature.severe_cold").getAsString());
        assertEquals("You feel overheated.", lang.get("terravera.temperature.moderate_heat").getAsString());
    }

    /** No numbers anywhere in the player-facing temperature wording. This is a symptom system, not a cold bar. */
    @Test
    public void noSymptomMessageQuotesANumber() throws IOException
    {
        final JsonObject lang = read("/assets/terravera/lang/en_us.json");
        for (com.terravera.common.temperature.ThermalModel.Band band :
            com.terravera.common.temperature.ThermalModel.Band.values())
        {
            final String message = lang.get(band.messageKey()).getAsString();
            assertFalse(message.matches(".*[0-9%].*"),
                "the " + band + " message quotes a number, which turns the system into a readout: " + message);
        }
    }

    // ----- Recipes ------------------------------------------------------------------------------------------

    @Test
    public void everyGarmentIsCraftable() throws IOException
    {
        for (Garment garment : WARDROBE)
        {
            final JsonObject recipe = read("/data/terravera/recipe/crafting/clothing/" + garment.name() + ".json");
            assertEquals("minecraft:crafting_shapeless", recipe.get("type").getAsString());
            assertEquals("terravera:" + garment.name(), recipe.getAsJsonObject("result").get("id").getAsString(),
                garment.name() + " recipe produces the wrong item");
        }
    }

    /**
     * The central promise of the crafting design: the sewn panel decides the <em>shape</em>, and the facing material
     * decides how the garment behaves. Every recipe has to consume both, or a player could sew a wool coat out of
     * grass.
     */
    @Test
    public void everyGarmentNeedsBothASewnPanelAndItsOwnMaterial() throws IOException
    {
        for (Garment garment : WARDROBE)
        {
            final JsonArray ingredients =
                read("/data/terravera/recipe/crafting/clothing/" + garment.name() + ".json")
                    .getAsJsonArray("ingredients");

            final List<String> items = new ArrayList<>();
            for (JsonElement element : ingredients)
            {
                final JsonObject ingredient = element.getAsJsonObject();
                items.add(ingredient.has("item") ? ingredient.get("item").getAsString()
                    : "#" + ingredient.get("tag").getAsString());
            }

            assertTrue(items.stream().anyMatch(i -> i.startsWith("terravera:sewn_")),
                garment.name() + " does not consume a sewn panel");
            assertTrue(items.stream().anyMatch(i -> i.startsWith("#terravera:cordage")),
                garment.name() + " is not bound with anything");
            assertTrue(items.size() >= 3, garment.name() + " is too cheap to be a real garment");

            // The panel must be the one for the slot the garment actually occupies.
            final String expectedPanel = switch (garment.slot())
            {
                case HEAD -> "terravera:sewn_hood_panel";
                case CHEST -> "terravera:sewn_body_panel";
                case LEGS -> "terravera:sewn_leg_panel";
                case FEET -> "terravera:sewn_foot_panel";
            };
            assertTrue(items.contains(expectedPanel),
                garment.name() + " is a " + garment.slot() + " garment but does not use " + expectedPanel);
        }
    }

    @Test
    public void aChestGarmentCostsMoreMaterialThanAHat() throws IOException
    {
        // Coverage should be legible in the price, not only in the model.
        final int hat = ingredientCount(WARDROBE.stream()
            .filter(g -> g.material() == ClothingMaterial.WOOL && g.slot() == GarmentSlot.HEAD)
            .findFirst().orElseThrow().name());
        final int coat = ingredientCount(WARDROBE.stream()
            .filter(g -> g.material() == ClothingMaterial.WOOL && g.slot() == GarmentSlot.CHEST)
            .findFirst().orElseThrow().name());
        assertTrue(coat > hat, "a wool sweater should cost more than a wool cap");
    }

    @Test
    public void everyPanelIsSewnAtASewingTable() throws IOException
    {
        for (String panel : PANELS)
        {
            final JsonObject recipe = read("/data/terravera/recipe/sewing/" + panel + ".json");
            assertEquals("tfc:sewing", recipe.get("type").getAsString(), panel + " must use TFC's sewing table");
            assertEquals("terravera:" + panel, recipe.getAsJsonObject("result").get("id").getAsString());

            // TFC's sewing recipe format is exact: 4 rows of 8 cloth squares, 5 rows of 9 stitch positions.
            final JsonArray squares = recipe.getAsJsonArray("squares");
            assertEquals(4, squares.size(), panel + " must have exactly 4 rows of squares");
            for (JsonElement row : squares)
            {
                final String text = row.getAsString();
                assertEquals(8, text.length(), panel + " square row must be 8 wide: '" + text + "'");
                assertTrue(text.matches("[ BW]*"), panel + " has an invalid square character in '" + text + "'");
            }

            final JsonArray stitches = recipe.getAsJsonArray("stitches");
            assertEquals(5, stitches.size(), panel + " must have exactly 5 rows of stitches");
            for (JsonElement row : stitches)
            {
                assertEquals(9, row.getAsString().length(), panel + " stitch row must be 9 wide");
            }
        }
    }

    @Test
    public void everyPanelActuallyHasClothAndStitchesInIt() throws IOException
    {
        for (String panel : PANELS)
        {
            final JsonObject recipe = read("/data/terravera/recipe/sewing/" + panel + ".json");

            int cloth = 0;
            for (JsonElement row : recipe.getAsJsonArray("squares"))
            {
                cloth += row.getAsString().replace(" ", "").length();
            }
            assertTrue(cloth >= 8, panel + " uses only " + cloth + " cloth squares; that is not a garment panel");

            int stitches = 0;
            for (JsonElement row : recipe.getAsJsonArray("stitches"))
            {
                stitches += row.getAsString().replace(" ", "").length();
            }
            assertTrue(stitches >= 8, panel + " has only " + stitches + " stitches");
        }
    }

    @Test
    public void everyIntermediateFabricIsCraftableAndNamed() throws IOException
    {
        for (String name : INTERMEDIATES)
        {
            assertResourceExists("/assets/terravera/models/item/" + name + ".json");
            assertResourceExists("/assets/terravera/textures/item/" + name + ".png");
        }
        // Each of these is a real step in the textile chain and must be reachable.
        for (String recipe : List.of("plant_fiber_cloth", "straw_mat", "felt_cloth", "oilskin_cloth",
            "quilted_cloth", "batting", "dubbin", "fur_pelt", "linen_cloth_from_bast"))
        {
            final JsonObject json = read("/data/terravera/recipe/crafting/" + recipe + ".json");
            assertTrue(json.get("type").getAsString().startsWith("minecraft:crafting"),
                recipe + " should be an ordinary crafting recipe");
        }
    }

    @Test
    public void everyMaterialLineCanBeMended() throws IOException
    {
        for (ClothingMaterial material : ClothingMaterial.values())
        {
            if (material == ClothingMaterial.BARE) continue;
            final JsonObject tag = read("/data/terravera/tags/item/clothing_repair/" + material.id() + ".json");
            assertTrue(tag.getAsJsonArray("values").size() > 0,
                material + " clothing has no repair material and will be unmendable");
        }
    }

    @Test
    public void theDryingRackExistsAndIsCraftable() throws IOException
    {
        // Without somewhere to dry clothes, "dry your clothes after the rain" is advice with no mechanism behind it.
        assertResourceExists("/assets/terravera/blockstates/drying_rack.json");
        assertResourceExists("/assets/terravera/models/block/drying_rack.json");
        assertResourceExists("/data/terravera/loot_table/blocks/drying_rack.json");

        final JsonObject recipe = read("/data/terravera/recipe/crafting/drying_rack.json");
        assertEquals("terravera:drying_rack", recipe.getAsJsonObject("result").get("id").getAsString());
    }

    // ----- Helpers ------------------------------------------------------------------------------------------

    private static int ingredientCount(String garment) throws IOException
    {
        return read("/data/terravera/recipe/crafting/clothing/" + garment + ".json")
            .getAsJsonArray("ingredients").size();
    }

    private static void assertResourceExists(String resource)
    {
        try (InputStream stream = ClothingResourceTest.class.getResourceAsStream(resource))
        {
            assertNotNull(stream, "Missing resource " + resource);
        }
        catch (IOException e)
        {
            fail("Could not read " + resource + ": " + e.getMessage());
        }
    }

    private static JsonObject read(String resource) throws IOException
    {
        try (InputStream stream = ClothingResourceTest.class.getResourceAsStream(resource))
        {
            assertNotNull(stream, "Missing resource " + resource);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
