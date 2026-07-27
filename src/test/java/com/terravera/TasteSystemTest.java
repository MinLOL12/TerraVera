/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera;

import java.util.List;
import org.junit.jupiter.api.Test;

import com.terravera.common.food.TasteSystem;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the built-in taste table in {@link TasteSystem}: it must stay within the documented scale, keep
 * covering the full roster of TerraFirmaCraft foods (so nothing defaults to "Plain"), and stay internally
 * consistent - cooking should always improve on raw ingredients.
 */
public class TasteSystemTest
{
    /** The six TFC cereal grains, in the order TerraFirmaCraft enumerates their products. */
    private static final List<String> GRAINS = List.of("barley", "maize", "oat", "rice", "rye", "wheat");

    /** Every TFC animal that drops an edible meat (raw) or roast (cooked). */
    private static final List<String> MEATS = List.of(
        "beef", "pork", "chicken", "mutton", "chevon", "camelidae", "venison", "rabbit", "horse_meat",
        "quail", "pheasant", "grouse", "peafowl", "duck", "turkey", "turtle", "frog_legs",
        "bear", "wolf", "gran_feline", "fox", "hyena",
        "cod", "salmon", "rainbow_trout", "lake_trout", "largemouth_bass", "smallmouth_bass",
        "bluegill", "crappie", "tropical_fish", "calamari", "shellfish"
    );

    /** Every fruit TerraFirmaCraft can jar into preserves, in tag order. */
    private static final List<String> PRESERVABLE_FRUITS = List.of(
        "blackberry", "raspberry", "blueberry", "elderberry", "bunchberry", "gooseberry", "snowberry",
        "cloudberry", "strawberry", "wintergreen_berry", "cranberry", "banana", "cherry", "green_apple",
        "lemon", "olive", "orange", "peach", "plum", "red_apple", "pumpkin_chunks", "melon_slice"
    );

    private static final List<String> MEAL_TYPES = List.of("vegetables", "protein", "grain", "fruit", "dairy");

    @Test
    public void everyTasteValueIsWithinTheScale()
    {
        TasteSystem.DEFAULT_TASTES.forEach((id, taste) ->
            assertTrue(taste >= TasteSystem.TASTE_DISGUSTING && taste <= TasteSystem.TASTE_EXCEPTIONAL,
                () -> id + " has out-of-range taste " + taste));
    }

    @Test
    public void coversEveryTfcFoodItem()
    {
        // Fruit, vegetables, and the odd whole blocks
        for (final String id : new String[] {
            "banana", "blackberry", "blueberry", "bunchberry", "cherry", "cloudberry", "cranberry",
            "elderberry", "gooseberry", "green_apple", "lemon", "olive", "orange", "peach", "plum",
            "raspberry", "red_apple", "snowberry", "strawberry", "wintergreen_berry", "melon_slice",
            "pumpkin_chunks",
            "beet", "cabbage", "carrot", "garlic", "green_bean", "green_bell_pepper", "onion", "potato",
            "baked_potato", "red_bell_pepper", "yellow_bell_pepper", "tomato", "soybean", "squash",
            "sugarcane", "cattail_root", "taro_root", "fresh_seaweed", "dried_seaweed", "dried_kelp",
            "boiled_egg", "cooked_egg", "cheese", "cooked_rice"
        })
        {
            assertTasteRegistered("tfc:food/" + id);
        }
        assertTasteRegistered("tfc:melon");
        assertTasteRegistered("tfc:pumpkin");

        // Grains and every stage of the bread pipeline
        for (final String grain : GRAINS)
        {
            assertTasteRegistered("tfc:food/" + grain);
            for (final String stage : new String[] {"_grain", "_flour", "_dough", "_bread", "_bread_sandwich", "_bread_jam_sandwich"})
            {
                assertTasteRegistered("tfc:food/" + grain + stage);
            }
        }

        // Everything with legs (or fins), raw and cooked
        for (final String meat : MEATS)
        {
            assertTasteRegistered("tfc:food/" + meat);
            assertTasteRegistered("tfc:food/cooked_" + meat);
        }

        // Pot-and-bowl meals
        for (final String type : MEAL_TYPES)
        {
            assertTasteRegistered("tfc:food/" + type + "_soup");
            assertTasteRegistered("tfc:food/" + type + "_salad");
        }

        // Jarred preserves, sealed and opened
        for (final String fruit : PRESERVABLE_FRUITS)
        {
            assertTasteRegistered("tfc:jar/" + fruit);
            assertTasteRegistered("tfc:jar/" + fruit + "_unsealed");
        }
    }

    @Test
    public void cookingAlwaysTastesBetterThanRaw()
    {
        for (final String meat : MEATS)
        {
            final int raw = taste("tfc:food/" + meat);
            final int cooked = taste("tfc:food/cooked_" + meat);
            assertTrue(cooked > raw, () -> meat + " is not improved by cooking (" + raw + " -> " + cooked + ")");
        }
    }

    @Test
    public void breadBeatsDoughAndDoughBeatsFlour()
    {
        for (final String grain : GRAINS)
        {
            final int flour = taste("tfc:food/" + grain + "_flour");
            final int dough = taste("tfc:food/" + grain + "_dough");
            final int bread = taste("tfc:food/" + grain + "_bread");
            assertTrue(dough > flour, () -> grain + " dough should beat flour");
            assertTrue(bread > dough, () -> grain + " bread should beat dough");
        }
    }

    @Test
    public void jamIsNeverWorseThanTheFreshFruit()
    {
        for (final String fruit : PRESERVABLE_FRUITS)
        {
            final int fresh = taste("tfc:food/" + fruit);
            final int jam = taste("tfc:jar/" + fruit);
            final int openedJam = taste("tfc:jar/" + fruit + "_unsealed");
            assertTrue(jam >= fresh, () -> fruit + " jam (" + jam + ") should be at least as good as fresh (" + fresh + ")");
            assertEquals(jam, openedJam, () -> fruit + " preserves should not change flavour when the seal cracks");
        }
    }

    @Test
    public void descriptorsFollowTheDocumentedBands()
    {
        assertEquals("terravera.taste.exceptional", TasteSystem.getTasteDescriptorKey(100));
        assertEquals("terravera.taste.exceptional", TasteSystem.getTasteDescriptorKey(95));
        assertEquals("terravera.taste.delicious", TasteSystem.getTasteDescriptorKey(75));
        assertEquals("terravera.taste.good", TasteSystem.getTasteDescriptorKey(50));
        assertEquals("terravera.taste.pretty_good", TasteSystem.getTasteDescriptorKey(25));
        assertEquals("terravera.taste.okay", TasteSystem.getTasteDescriptorKey(0));
        assertEquals("terravera.taste.okay", TasteSystem.getTasteDescriptorKey(-49));
        assertEquals("terravera.taste.bad", TasteSystem.getTasteDescriptorKey(-50));
        assertEquals("terravera.taste.disgusting", TasteSystem.getTasteDescriptorKey(-90));
        assertEquals("terravera.taste.disgusting", TasteSystem.getTasteDescriptorKey(-100));
    }

    private static void assertTasteRegistered(String id)
    {
        assertTrue(TasteSystem.DEFAULT_TASTES.containsKey(id),
            () -> "No taste assigned for " + id + " (it would show as \"Plain\")");
    }

    private static int taste(String id)
    {
        assertTasteRegistered(id);
        return TasteSystem.DEFAULT_TASTES.get(id);
    }
}
