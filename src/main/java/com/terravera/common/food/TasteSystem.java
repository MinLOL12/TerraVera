package com.terravera.common.food;

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
     * Example taste values (can be loaded from JSON later).
     * Keys can be full "namespace:path" or just "path".
     */
    public static final Map<String, Integer> DEFAULT_TASTES = Map.ofEntries(
            // Bad / survival foods
            Map.entry("minecraft:grass", -80),
            Map.entry("minecraft:tall_grass", -80),
            Map.entry("minecraft:fern", -70),
            Map.entry("minecraft:dead_bush", -90),
            Map.entry("minecraft:kelp", -30),
            Map.entry("minecraft:dried_kelp", 10),
            Map.entry("minecraft:rotten_flesh", -95),
            Map.entry("minecraft:spider_eye", -70),
            Map.entry("minecraft:poisonous_potato", -60),
            Map.entry("minecraft:pufferfish", -100),

            // Foraged / raw
            Map.entry("minecraft:apple", 35),
            Map.entry("minecraft:sweet_berries", 40),
            Map.entry("minecraft:glow_berries", 45),
            Map.entry("minecraft:melon_slice", 30),
            Map.entry("minecraft:carrot", 25),
            Map.entry("minecraft:potato", -10),
            Map.entry("minecraft:beetroot", 20),
            Map.entry("minecraft:brown_mushroom", -15),
            Map.entry("minecraft:red_mushroom", -25),
            Map.entry("minecraft:raw_beef", -20),
            Map.entry("minecraft:raw_porkchop", -18),
            Map.entry("minecraft:raw_chicken", -35),
            Map.entry("minecraft:raw_mutton", -22),
            Map.entry("minecraft:raw_cod", -15),
            Map.entry("minecraft:raw_salmon", -12),

            // Cooked / prepared (better taste)
            Map.entry("minecraft:cooked_beef", 65),
            Map.entry("minecraft:cooked_porkchop", 62),
            Map.entry("minecraft:cooked_chicken", 55),
            Map.entry("minecraft:cooked_mutton", 58),
            Map.entry("minecraft:cooked_cod", 50),
            Map.entry("minecraft:cooked_salmon", 52),
            Map.entry("minecraft:baked_potato", 55),
            Map.entry("minecraft:bread", 60),
            Map.entry("minecraft:cookie", 45),
            Map.entry("minecraft:pumpkin_pie", 75),
            Map.entry("minecraft:mushroom_stew", 50),
            Map.entry("minecraft:beetroot_soup", 48),
            Map.entry("minecraft:rabbit_stew", 70),

            // High end / rare
            Map.entry("minecraft:golden_carrot", 85),
            Map.entry("minecraft:golden_apple", 90),
            Map.entry("minecraft:enchanted_golden_apple", 100),

            // TerraVera / mod examples
            Map.entry("terravera:plant_fiber", -50),   // not really food but if someone eats it
            Map.entry("leaves", -60),
            Map.entry("raw_grass", -100),
            Map.entry("raw_mushroom", -20),
            Map.entry("berries", 40),
            Map.entry("roasted_meat", 55),
            Map.entry("fresh_bread", 70),
            Map.entry("cheese", 80),
            Map.entry("chocolate", 95),
            Map.entry("pizza", 100),
            Map.entry("burnt_steak", 5),
            Map.entry("properly_cooked_steak", 70),
            Map.entry("raw_steak", -15)
    );

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

    private static String getFoodId(ItemStack stack) {
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
