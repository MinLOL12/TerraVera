package com.terravera.common.food;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Taste System for TerraVera
 * Adds taste values (-100 to 100) that modify saturation gained from food.
 * Integrates with TFC-style food (hunger, saturation, nutrition).
 */
public class TasteSystem {

    // Taste scale constants
    public static final int TASTE_DISGUSTING = -100;
    public static final int TASTE_BAD = -50;
    public static final int TASTE_OKAY = 0;
    public static final int TASTE_PRETTY_GOOD = 25;
    public static final int TASTE_GOOD = 50;
    public static final int TASTE_DELICIOUS = 75;
    public static final int TASTE_EXCEPTIONAL = 100;

    // Hunger threshold constants
    private static final float HUNGER_THRESHOLD_FULL = 0.95f;   // 95% full - taste penalty
    private static final float HUNGER_THRESHOLD_STARVING = 0.15f; // Starving - taste bonus

    // Monotony tracking: last eaten foods and their taste degradation
    private static final Map<UUID, Map<String, Integer>> playerFoodHistory = new HashMap<>();
    private static final int MONOTONY_DAYS = 7; // Days until taste starts dropping
    private static final int MAX_MONOTONY_DROP = 40; // Max taste drop from eating same food

    /**
     * Core method: Calculates effective saturation after taste modification.
     * @param baseSaturation Vanilla/TFC saturation value
     * @param tasteValue Taste from -100 to 100
     * @param playerHungerPercent Current hunger (0.0 = empty, 1.0 = full)
     * @param isStarving Whether player is starving
     * @return Modified saturation value
     */
    public static float calculateSatisfaction(float baseSaturation, int tasteValue, float playerHungerPercent, boolean isStarving) {
        float tasteMultiplier = getTasteMultiplier(tasteValue);

        // Hunger state modifiers
        if (playerHungerPercent >= HUNGER_THRESHOLD_FULL) {
            // Full stomach - everything tastes worse
            tasteMultiplier *= 0.6f;
        } else if (isStarving || playerHungerPercent <= HUNGER_THRESHOLD_STARVING) {
            // Starving - even bad food tastes better
            tasteMultiplier = Math.max(tasteMultiplier, 0.4f);
            if (tasteValue < 0) {
                tasteMultiplier *= 1.5f; // Bad food feels better when starving
            }
        }

        return baseSaturation * tasteMultiplier;
    }

    private static float getTasteMultiplier(int taste) {
        // Linear mapping from taste to multiplier
        // -100 -> 0.3x, 0 -> 1.0x, 100 -> 1.7x
        if (taste <= TASTE_DISGUSTING) return 0.3f;
        if (taste >= TASTE_EXCEPTIONAL) return 1.7f;

        float normalized = (taste + 100) / 200.0f; // 0.0 to 1.0
        return 0.3f + (normalized * 1.4f);
    }

    /**
     * Apply monotony penalty (eating same food repeatedly)
     */
    public static int applyMonotonyPenalty(Player player, String foodId, int baseTaste) {
        UUID playerId = player.getUUID();
        playerFoodHistory.putIfAbsent(playerId, new HashMap<>());
        Map<String, Integer> history = playerFoodHistory.get(playerId);

        int count = history.getOrDefault(foodId, 0) + 1;
        history.put(foodId, count);

        // Every 3 consecutive days of same food, drop taste by 8 (up to MAX)
        int penalty = Math.min((count / 3) * 8, MAX_MONOTONY_DROP);
        return Math.max(baseTaste - penalty, TASTE_DISGUSTING);
    }

    /**
     * Non-mutating preview of what {@link #applyMonotonyPenalty} would currently return for this player/food,
     * for use in tooltips where we don't want looking at an item to count as "eating" it again.
     */
    public static int peekMonotonyAdjustedTaste(Player player, String foodId, int baseTaste) {
        Map<String, Integer> history = playerFoodHistory.get(player.getUUID());
        int count = history == null ? 0 : history.getOrDefault(foodId, 0);
        int penalty = Math.min((count / 3) * 8, MAX_MONOTONY_DROP);
        return Math.max(baseTaste - penalty, TASTE_DISGUSTING);
    }


    /**
     * Reset monotony when eating a different food
     */
    public static void resetMonotonyForNewFood(Player player, String foodId) {
        UUID playerId = player.getUUID();
        if (playerFoodHistory.containsKey(playerId)) {
            // Only keep recent food in history
            Map<String, Integer> history = playerFoodHistory.get(playerId);
            history.entrySet().removeIf(entry -> !entry.getKey().equals(foodId));
        }
    }

    /**
     * Built-in taste table: vanilla anchors plus <b>every food item in TerraFirmaCraft</b> (the complete
     * 1.20.x/1.21.x roster - fruit-tree and berry fruits, vegetables, grains and their products, raw and
     * cooked meats, eggs, dairy, prepared meals, and jarred preserves), so that no TFC food falls back to
     * "Plain" out of ignorance. Values follow the vanilla anchors above: raw and unprocessed foods trend
     * negative-to-neutral, properly prepared food is their reward.
     * <p>
     * Keys are full registry ids ({@code "namespace:path"}); {@link #getBaseTaste} additionally falls back
     * to the plain path for convenience. Additional values can be registered at runtime via
     * {@link #registerTaste}; loading this table from datapack JSON is planned and would supersede it.
     */
    public static final Map<String, Integer> DEFAULT_TASTES = createDefaultTastes();

    private static Map<String, Integer> createDefaultTastes()
    {
        final Map<String, Integer> tastes = new HashMap<>();

        // ---- Vanilla: bad / survival foods -------------------------------------------------------------
        tastes.put("minecraft:grass", -80);
        tastes.put("minecraft:tall_grass", -80);
        tastes.put("minecraft:fern", -70);
        tastes.put("minecraft:dead_bush", -90);
        tastes.put("minecraft:kelp", -30);
        tastes.put("minecraft:dried_kelp", 10);
        tastes.put("minecraft:rotten_flesh", -95);
        tastes.put("minecraft:spider_eye", -70);
        tastes.put("minecraft:poisonous_potato", -60);
        tastes.put("minecraft:pufferfish", -100);

        // ---- Vanilla: foraged / raw --------------------------------------------------------------------
        tastes.put("minecraft:apple", 35);
        tastes.put("minecraft:sweet_berries", 40);
        tastes.put("minecraft:glow_berries", 45);
        tastes.put("minecraft:melon_slice", 30);
        tastes.put("minecraft:carrot", 25);
        tastes.put("minecraft:potato", -10);
        tastes.put("minecraft:beetroot", 20);
        tastes.put("minecraft:brown_mushroom", -15);
        tastes.put("minecraft:red_mushroom", -25);
        tastes.put("minecraft:raw_beef", -20);
        tastes.put("minecraft:raw_porkchop", -18);
        tastes.put("minecraft:raw_chicken", -35);
        tastes.put("minecraft:raw_mutton", -22);
        tastes.put("minecraft:raw_cod", -15);
        tastes.put("minecraft:raw_salmon", -12);

        // ---- Vanilla: cooked / prepared (better taste) --------------------------------------------------
        tastes.put("minecraft:cooked_beef", 65);
        tastes.put("minecraft:cooked_porkchop", 62);
        tastes.put("minecraft:cooked_chicken", 55);
        tastes.put("minecraft:cooked_mutton", 58);
        tastes.put("minecraft:cooked_cod", 50);
        tastes.put("minecraft:cooked_salmon", 52);
        tastes.put("minecraft:baked_potato", 55);
        tastes.put("minecraft:bread", 60);
        tastes.put("minecraft:cookie", 45);
        tastes.put("minecraft:pumpkin_pie", 75);
        tastes.put("minecraft:mushroom_stew", 50);
        tastes.put("minecraft:beetroot_soup", 48);
        tastes.put("minecraft:rabbit_stew", 70);

        // ---- TerraVera: griddle cooking ------------------------------------------------------------------
        // Recipe ingredients are intentionally varied (dairy, egg, fat, fruit and spice) so cooked meals earn
        // both a higher taste score and an escape from the monotony of plain grain.
        tastes.put("terravera:pancakes", 72);
        tastes.put("terravera:waffles", 82);
        tastes.put("terravera:crepes", 74);
        tastes.put("terravera:griddle_flatbread", 62);
        tastes.put("terravera:buttermilk_biscuits", 77);
        tastes.put("terravera:apple_fritters", 84);
        tastes.put("terravera:johnnycakes", 70);
        tastes.put("terravera:honeyberry_cakes", 88);

        // ---- Vanilla: high end / rare -------------------------------------------------------------------
        tastes.put("minecraft:golden_carrot", 85);
        tastes.put("minecraft:golden_apple", 90);
        tastes.put("minecraft:enchanted_golden_apple", 100);

        // ---- TerraFirmaCraft: fruit (fruit trees, berry bushes, melons) ---------------------------------
        // Fruit is the forager's reward - generally pleasant, best fresh off the tree.
        tastes.put("tfc:food/banana", 38);
        tastes.put("tfc:food/blackberry", 40);
        tastes.put("tfc:food/blueberry", 40);
        tastes.put("tfc:food/bunchberry", 28);
        tastes.put("tfc:food/cherry", 42);
        tastes.put("tfc:food/cloudberry", 42);
        tastes.put("tfc:food/cranberry", 22);         // mouth-puckeringly tart raw
        tastes.put("tfc:food/elderberry", 25);        // rather bitter raw, better cooked
        tastes.put("tfc:food/gooseberry", 32);
        tastes.put("tfc:food/green_apple", 30);       // crisp but tart
        tastes.put("tfc:food/lemon", 15);             // face-scrunchingly sour
        tastes.put("tfc:food/olive", 5);              // bitter straight off the tree (wants brining)
        tastes.put("tfc:food/orange", 42);
        tastes.put("tfc:food/peach", 45);
        tastes.put("tfc:food/plum", 35);
        tastes.put("tfc:food/raspberry", 42);
        tastes.put("tfc:food/red_apple", 35);
        tastes.put("tfc:food/snowberry", 18);         // watery and bland
        tastes.put("tfc:food/strawberry", 45);
        tastes.put("tfc:food/wintergreen_berry", 30); // minty, mildly medicinal
        tastes.put("tfc:food/melon_slice", 30);
        tastes.put("tfc:food/pumpkin_chunks", -5);    // stringy raw squash flesh
        tastes.put("tfc:melon", 30);                  // whole, uncut melon - same snack, bigger commitment
        tastes.put("tfc:pumpkin", -10);               // eating a whole raw pumpkin is self-inflicted

        // ---- TerraFirmaCraft: vegetables ----------------------------------------------------------------
        // Mostly bland-to-meh raw; the fiery aromatics (onion, garlic) bite back until cooked in something.
        tastes.put("tfc:food/beet", 20);
        tastes.put("tfc:food/cabbage", 18);
        tastes.put("tfc:food/carrot", 25);
        tastes.put("tfc:food/garlic", -20);           // a whole raw garlic bulb is punishment
        tastes.put("tfc:food/green_bean", 20);
        tastes.put("tfc:food/green_bell_pepper", 25);
        tastes.put("tfc:food/onion", -10);            // raw onion, straight
        tastes.put("tfc:food/potato", -10);           // raw potato
        tastes.put("tfc:food/baked_potato", 55);
        tastes.put("tfc:food/red_bell_pepper", 30);   // ripe and sweet
        tastes.put("tfc:food/yellow_bell_pepper", 25);
        tastes.put("tfc:food/tomato", 35);
        tastes.put("tfc:food/soybean", 10);           // beany, chalky
        tastes.put("tfc:food/squash", 12);
        tastes.put("tfc:food/sugarcane", 45);         // chewing raw cane: grass-flavoured sugar
        tastes.put("tfc:food/cattail_root", 5);       // starchy survival forage
        tastes.put("tfc:food/taro_root", -15);        // stinging oxalate crystals until cooked
        tastes.put("tfc:food/fresh_seaweed", -10);    // slimy, fishy
        tastes.put("tfc:food/dried_seaweed", 12);     // drying helps a lot
        tastes.put("tfc:food/dried_kelp", 10);

        // ---- TerraFirmaCraft: grains and grain products --------------------------------------------------
        // Grain is for making bread, not for nibbling - taste climbs with every processing step.
        tastes.put("tfc:food/barley", -20);           // whole ears: husks and awns
        tastes.put("tfc:food/maize", -18);            // raw maize ear, hard kernels
        tastes.put("tfc:food/oat", -20);
        tastes.put("tfc:food/rice", -20);
        tastes.put("tfc:food/rye", -20);
        tastes.put("tfc:food/wheat", -20);
        tastes.put("tfc:food/barley_grain", -15);     // threshed grain: hard, raw starch
        tastes.put("tfc:food/maize_grain", -15);
        tastes.put("tfc:food/oat_grain", -15);
        tastes.put("tfc:food/rice_grain", -15);
        tastes.put("tfc:food/rye_grain", -15);
        tastes.put("tfc:food/wheat_grain", -15);
        tastes.put("tfc:food/barley_flour", -25);     // mouthful of dry flour
        tastes.put("tfc:food/maize_flour", -25);
        tastes.put("tfc:food/oat_flour", -25);
        tastes.put("tfc:food/rice_flour", -25);
        tastes.put("tfc:food/rye_flour", -25);
        tastes.put("tfc:food/wheat_flour", -25);
        tastes.put("tfc:food/barley_dough", -10);     // raw dough
        tastes.put("tfc:food/maize_dough", -10);
        tastes.put("tfc:food/oat_dough", -10);
        tastes.put("tfc:food/rice_dough", -10);
        tastes.put("tfc:food/rye_dough", -10);
        tastes.put("tfc:food/wheat_dough", -10);
        tastes.put("tfc:food/cooked_rice", 35);       // plain steamed rice
        tastes.put("tfc:food/barley_bread", 55);      // coarse, dense
        tastes.put("tfc:food/maize_bread", 60);       // cornbread
        tastes.put("tfc:food/oat_bread", 55);
        tastes.put("tfc:food/rice_bread", 58);
        tastes.put("tfc:food/rye_bread", 62);         // dark and malty
        tastes.put("tfc:food/wheat_bread", 65);       // the good stuff

        // ---- TerraFirmaCraft: raw meat, poultry and fish -------------------------------------------------
        // Uncooked flesh. Livestock anchors match the vanilla raw meats above; predators, scavengers and
        // carrion-eaters taste wrong even before the food poisoning.
        tastes.put("tfc:food/beef", -20);
        tastes.put("tfc:food/pork", -18);
        tastes.put("tfc:food/chicken", -35);
        tastes.put("tfc:food/mutton", -22);
        tastes.put("tfc:food/chevon", -22);           // goat
        tastes.put("tfc:food/camelidae", -22);        // llama/alpaca/camel
        tastes.put("tfc:food/venison", -20);
        tastes.put("tfc:food/rabbit", -22);
        tastes.put("tfc:food/horse_meat", -25);
        tastes.put("tfc:food/quail", -22);
        tastes.put("tfc:food/pheasant", -25);
        tastes.put("tfc:food/grouse", -25);
        tastes.put("tfc:food/peafowl", -25);
        tastes.put("tfc:food/duck", -25);
        tastes.put("tfc:food/turkey", -30);           // so very dry even before cooking
        tastes.put("tfc:food/turtle", -22);
        tastes.put("tfc:food/frog_legs", -30);
        tastes.put("tfc:food/bear", -30);
        tastes.put("tfc:food/wolf", -35);
        tastes.put("tfc:food/gran_feline", -35);      // big cat
        tastes.put("tfc:food/fox", -35);
        tastes.put("tfc:food/hyena", -55);            // scavenger musk: legendarily bad
        tastes.put("tfc:food/cod", -15);
        tastes.put("tfc:food/salmon", -12);
        tastes.put("tfc:food/rainbow_trout", -12);
        tastes.put("tfc:food/lake_trout", -13);
        tastes.put("tfc:food/largemouth_bass", -13);
        tastes.put("tfc:food/smallmouth_bass", -13);
        tastes.put("tfc:food/bluegill", -15);
        tastes.put("tfc:food/crappie", -15);
        tastes.put("tfc:food/tropical_fish", -18);    // tiny, bony
        tastes.put("tfc:food/calamari", -15);         // rubbery raw squid
        tastes.put("tfc:food/shellfish", -12);

        // ---- TerraFirmaCraft: cooked meat, poultry and fish ----------------------------------------------
        // The firepit's redemption arc, anchored to the vanilla cooked meats above.
        tastes.put("tfc:food/cooked_beef", 65);
        tastes.put("tfc:food/cooked_pork", 62);
        tastes.put("tfc:food/cooked_chicken", 55);
        tastes.put("tfc:food/cooked_mutton", 58);
        tastes.put("tfc:food/cooked_chevon", 55);
        tastes.put("tfc:food/cooked_camelidae", 52);
        tastes.put("tfc:food/cooked_venison", 60);
        tastes.put("tfc:food/cooked_rabbit", 55);
        tastes.put("tfc:food/cooked_horse_meat", 50);
        tastes.put("tfc:food/cooked_quail", 60);
        tastes.put("tfc:food/cooked_pheasant", 60);
        tastes.put("tfc:food/cooked_grouse", 58);
        tastes.put("tfc:food/cooked_peafowl", 55);
        tastes.put("tfc:food/cooked_turkey", 62);     // a proper roast
        tastes.put("tfc:food/cooked_duck", 65);       // rich, crispy skin
        tastes.put("tfc:food/cooked_turtle", 48);
        tastes.put("tfc:food/cooked_frog_legs", 48);  // a delicacy, allegedly
        tastes.put("tfc:food/cooked_bear", 45);       // fatty, strong, but real food
        tastes.put("tfc:food/cooked_wolf", 28);       // survivable
        tastes.put("tfc:food/cooked_gran_feline", 30);
        tastes.put("tfc:food/cooked_fox", 28);
        tastes.put("tfc:food/cooked_hyena", 22);      // cooking only does so much
        tastes.put("tfc:food/cooked_cod", 50);
        tastes.put("tfc:food/cooked_salmon", 52);
        tastes.put("tfc:food/cooked_rainbow_trout", 55);
        tastes.put("tfc:food/cooked_lake_trout", 52);
        tastes.put("tfc:food/cooked_largemouth_bass", 52);
        tastes.put("tfc:food/cooked_smallmouth_bass", 52);
        tastes.put("tfc:food/cooked_bluegill", 50);
        tastes.put("tfc:food/cooked_crappie", 50);
        tastes.put("tfc:food/cooked_tropical_fish", 45);
        tastes.put("tfc:food/cooked_calamari", 55);
        tastes.put("tfc:food/cooked_shellfish", 52);

        // ---- TerraFirmaCraft: eggs and dairy --------------------------------------------------------------
        tastes.put("tfc:food/boiled_egg", 55);
        tastes.put("tfc:food/cooked_egg", 58);        // fried
        tastes.put("tfc:food/cheese", 75);            // aged to perfection

        // ---- TerraFirmaCraft: prepared meals ---------------------------------------------------------------
        // Assembled food beats ingredients - soups are comfort food, salads are fresh but cold.
        tastes.put("tfc:food/barley_bread_sandwich", 72);
        tastes.put("tfc:food/maize_bread_sandwich", 72);
        tastes.put("tfc:food/oat_bread_sandwich", 72);
        tastes.put("tfc:food/rice_bread_sandwich", 72);
        tastes.put("tfc:food/rye_bread_sandwich", 72);
        tastes.put("tfc:food/wheat_bread_sandwich", 72);
        tastes.put("tfc:food/barley_bread_jam_sandwich", 78);  // bread AND jam: hard to beat
        tastes.put("tfc:food/maize_bread_jam_sandwich", 78);
        tastes.put("tfc:food/oat_bread_jam_sandwich", 78);
        tastes.put("tfc:food/rice_bread_jam_sandwich", 78);
        tastes.put("tfc:food/rye_bread_jam_sandwich", 78);
        tastes.put("tfc:food/wheat_bread_jam_sandwich", 78);
        tastes.put("tfc:food/vegetables_soup", 68);
        tastes.put("tfc:food/protein_soup", 72);
        tastes.put("tfc:food/grain_soup", 65);
        tastes.put("tfc:food/fruit_soup", 75);        // stewed fruit = dessert soup
        tastes.put("tfc:food/dairy_soup", 70);
        tastes.put("tfc:food/vegetables_salad", 55);
        tastes.put("tfc:food/protein_salad", 60);
        tastes.put("tfc:food/grain_salad", 58);
        tastes.put("tfc:food/fruit_salad", 68);
        tastes.put("tfc:food/dairy_salad", 58);

        // ---- TerraFirmaCraft: jarred preserves -------------------------------------------------------------
        // Sealed jars aren't eaten directly (their TFC food definition grants nothing until opened and used
        // in a jam sandwich), but the flavour of slow-cooked fruit and sugar is here for when they do meet
        // the tongue. Jam is the fruit, plus sugar and time. Sealed and opened variants taste identical.
        tastes.put("tfc:jar/blackberry", 60);
        tastes.put("tfc:jar/blackberry_unsealed", 60);
        tastes.put("tfc:jar/raspberry", 62);
        tastes.put("tfc:jar/raspberry_unsealed", 62);
        tastes.put("tfc:jar/blueberry", 60);
        tastes.put("tfc:jar/blueberry_unsealed", 60);
        tastes.put("tfc:jar/elderberry", 45);
        tastes.put("tfc:jar/elderberry_unsealed", 45);
        tastes.put("tfc:jar/bunchberry", 48);
        tastes.put("tfc:jar/bunchberry_unsealed", 48);
        tastes.put("tfc:jar/gooseberry", 52);
        tastes.put("tfc:jar/gooseberry_unsealed", 52);
        tastes.put("tfc:jar/snowberry", 40);
        tastes.put("tfc:jar/snowberry_unsealed", 40);
        tastes.put("tfc:jar/cloudberry", 62);
        tastes.put("tfc:jar/cloudberry_unsealed", 62);
        tastes.put("tfc:jar/strawberry", 65);
        tastes.put("tfc:jar/strawberry_unsealed", 65);
        tastes.put("tfc:jar/wintergreen_berry", 50);
        tastes.put("tfc:jar/wintergreen_berry_unsealed", 50);
        tastes.put("tfc:jar/cranberry", 45);          // sugar tames the tartness
        tastes.put("tfc:jar/cranberry_unsealed", 45);
        tastes.put("tfc:jar/banana", 58);
        tastes.put("tfc:jar/banana_unsealed", 58);
        tastes.put("tfc:jar/cherry", 62);
        tastes.put("tfc:jar/cherry_unsealed", 62);
        tastes.put("tfc:jar/green_apple", 50);
        tastes.put("tfc:jar/green_apple_unsealed", 50);
        tastes.put("tfc:jar/lemon", 40);              // marmalade territory
        tastes.put("tfc:jar/lemon_unsealed", 40);
        tastes.put("tfc:jar/olive", 30);              // even sugar can't fix what brine should have
        tastes.put("tfc:jar/olive_unsealed", 30);
        tastes.put("tfc:jar/orange", 60);
        tastes.put("tfc:jar/orange_unsealed", 60);
        tastes.put("tfc:jar/peach", 65);
        tastes.put("tfc:jar/peach_unsealed", 65);
        tastes.put("tfc:jar/plum", 55);
        tastes.put("tfc:jar/plum_unsealed", 55);
        tastes.put("tfc:jar/red_apple", 55);
        tastes.put("tfc:jar/red_apple_unsealed", 55);
        tastes.put("tfc:jar/pumpkin_chunks", 30);     // candied pumpkin
        tastes.put("tfc:jar/pumpkin_chunks_unsealed", 30);
        tastes.put("tfc:jar/melon_slice", 50);
        tastes.put("tfc:jar/melon_slice_unsealed", 50);

        // ---- TerraVera / generic development fallbacks ------------------------------------------------------
        // Bare-path keys that match any namespace - mostly codegen examples and dev foods.
        tastes.put("terravera:plant_fiber", -50);   // not really food but if someone eats it
        tastes.put("leaves", -60);
        tastes.put("raw_grass", -100);
        tastes.put("raw_mushroom", -20);
        tastes.put("berries", 40);
        tastes.put("roasted_meat", 55);
        tastes.put("fresh_bread", 70);
        tastes.put("cheese", 80);
        tastes.put("chocolate", 95);
        tastes.put("pizza", 100);
        tastes.put("burnt_steak", 5);
        tastes.put("properly_cooked_steak", 70);
        tastes.put("raw_steak", -15);

        return tastes;
    }

    /**
     * Coarse, human readable descriptor for a taste value, used to show flavor on food item tooltips without
     * exposing the raw number. Mirrors the bands implied by the {@code TASTE_*} constants.
     * @return a translation key under {@code terravera.taste.*}
     */
    public static String getTasteDescriptorKey(int taste)
    {
        if (taste >= TASTE_EXCEPTIONAL - 5) return "terravera.taste.exceptional";
        if (taste >= TASTE_DELICIOUS) return "terravera.taste.delicious";
        if (taste >= TASTE_GOOD) return "terravera.taste.good";
        if (taste >= TASTE_PRETTY_GOOD) return "terravera.taste.pretty_good";
        if (taste <= TASTE_DISGUSTING + 10) return "terravera.taste.disgusting";
        if (taste <= TASTE_BAD) return "terravera.taste.bad";
        return "terravera.taste.okay";
    }

    /**
     * Colour to render a taste descriptor in, so good and bad flavors are visually distinct at a glance.
     */
    public static ChatFormatting getTasteColor(int taste)
    {
        if (taste >= TASTE_EXCEPTIONAL - 5) return ChatFormatting.GOLD;
        if (taste >= TASTE_GOOD) return ChatFormatting.GREEN;
        if (taste >= TASTE_PRETTY_GOOD) return ChatFormatting.DARK_GREEN;
        if (taste <= TASTE_DISGUSTING + 10) return ChatFormatting.RED;
        if (taste <= TASTE_BAD) return ChatFormatting.GRAY;
        return ChatFormatting.WHITE;
    }

    /**
     * Gets base taste for an item. Extend this to read from data packs / JSON.
     * Uses the item's registry ID (e.g. "minecraft:apple" or "terravera:roasted_meat").
     */
    public static int getBaseTaste(ItemStack stack) {
        if (stack.isEmpty()) return TASTE_OKAY;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return TASTE_OKAY;
        String key = id.toString().toLowerCase();
        // Also try just the path for convenience (e.g. "apple")
        String path = id.getPath().toLowerCase();
        return DEFAULT_TASTES.getOrDefault(key,
                DEFAULT_TASTES.getOrDefault(path, TASTE_OKAY));
    }

    /**
     * Main hook point for food consumption.
     * Call this from your food eating event handler.
     * Returns the effective saturation that should be gained (taste + context adjusted).
     */
    public static float onFoodEaten(Player player, ItemStack food, float baseSaturation) {
        int baseTaste = getBaseTaste(food);
        String foodId = getFoodId(food);

        // Apply monotony (tracks consecutive/repeated consumption of same food)
        int finalTaste = applyMonotonyPenalty(player, foodId, baseTaste);

        // Only keep count for this food if it was the last one (simplified monotony)
        resetMonotonyForNewFood(player, foodId);

        float hungerPercent = player.getFoodData().getFoodLevel() / 20.0f;
        boolean starving = player.getFoodData().getFoodLevel() <= 4;

        return calculateSatisfaction(baseSaturation, finalTaste, hungerPercent, starving);
    }

    public static String getFoodId(ItemStack stack) {
        if (stack.isEmpty()) return "empty";
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return (id != null) ? id.toString() : stack.getItem().toString();
    }

    /**
     * Allows other code (or future datapack loaders) to register additional taste values.
     */
    public static void registerTaste(String itemId, int tasteValue) {
        // Normalize to lower case
        DEFAULT_TASTES.put(itemId.toLowerCase(), tasteValue);
    }

    /**
     * Allows registering taste by ResourceLocation.
     */
    public static void registerTaste(ResourceLocation id, int tasteValue) {
        if (id != null) {
            DEFAULT_TASTES.put(id.toString().toLowerCase(), tasteValue);
        }
    }
}
