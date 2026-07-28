/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 */

package com.terravera.common.greenhouse;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Defines which crops benefit most from greenhouse cultivation and which seasons they are normally available in.
 * High-value or climate-sensitive crops give players a reason to invest in greenhouse infrastructure rather than
 * growing everything under glass. Seasonal data tells the greenhouse which crops would normally be unavailable
 * at a given time of year.
 */
public final class CropSpecialization
{
    /** Crops that benefit significantly from greenhouse cultivation (high value or climate-sensitive). */
    private static final Map<String, CropProfile> PROFILES = new HashMap<>();

    static {
        // High-value greenhouse crops: tomatoes, peppers, cucumbers, strawberries
        register("tomato", 1.5f, Set.of("spring", "summer"), true);
        register("pepper", 1.6f, Set.of("summer"), true);
        register("cucumber", 1.4f, Set.of("summer"), true);
        register("strawberry", 1.7f, Set.of("spring", "summer"), true);

        // Moderate benefit: leafy greens, herbs
        register("cabbage", 1.2f, Set.of("spring", "autumn"), false);
        register("lettuce", 1.3f, Set.of("spring", "autumn"), false);
        register("spinach", 1.2f, Set.of("spring", "autumn"), false);
        register("onion", 1.1f, Set.of("spring", "summer"), false);

        // Low benefit: staple grains that grow well in open fields
        register("wheat", 1.05f, Set.of("spring", "summer", "autumn"), false);
        register("barley", 1.05f, Set.of("spring", "summer"), false);
        register("oat", 1.05f, Set.of("spring", "summer"), false);
        register("rye", 1.05f, Set.of("spring", "autumn"), false);
        register("maize", 1.1f, Set.of("summer"), false);
        register("rice", 1.05f, Set.of("summer"), false);

        // Root crops: moderate benefit in cold climates
        register("potato", 1.15f, Set.of("spring", "summer", "autumn"), false);
        register("carrot", 1.15f, Set.of("spring", "autumn"), false);
        register("beet", 1.1f, Set.of("spring", "autumn"), false);
        register("turnip", 1.1f, Set.of("spring", "autumn"), false);

        // Tropical / climate-sensitive crops: massive benefit in cold biomes
        register("sugarcane", 1.8f, Set.of("summer"), true);
        register("melon", 1.6f, Set.of("summer"), true);
        register("pumpkin", 1.2f, Set.of("summer", "autumn"), false);
        register("soybean", 1.3f, Set.of("summer"), false);
    }

    private static void register(String cropId, float greenhouseBonus, Set<String> naturalSeasons, boolean climateSensitive)
    {
        PROFILES.put(cropId, new CropProfile(cropId, greenhouseBonus, naturalSeasons, climateSensitive));
    }

    /**
     * Returns the growth rate bonus for growing a particular crop inside a greenhouse.
     * Staple crops get a negligible bonus; high-value or climate-sensitive crops get a large one.
     * This is what gives players a reason to invest in glass over just growing in open fields.
     */
    public static float greenhouseBonusFor(ItemStack seedStack)
    {
        String cropId = identifyCrop(seedStack);
        CropProfile profile = PROFILES.get(cropId);
        return profile != null ? profile.greenhouseBonus() : 1.0f;
    }

    /**
     * Whether this crop would normally fail to grow in the current season outside a greenhouse.
     * Greenhouses enable out-of-season growing for crops whose natural season has passed.
     */
    public static boolean isOutOfSeason(ItemStack seedStack, String currentSeason)
    {
        String cropId = identifyCrop(seedStack);
        CropProfile profile = PROFILES.get(cropId);
        if (profile == null) return false;
        return !profile.naturalSeasons().contains(currentSeason);
    }

    /**
     * Whether this crop is a high-value greenhouse specialist that justifies the investment.
     */
    public static boolean isGreenhouseSpecialist(ItemStack seedStack)
    {
        String cropId = identifyCrop(seedStack);
        CropProfile profile = PROFILES.get(cropId);
        return profile != null && profile.climateSensitive();
    }

    /**
     * The yield multiplier from growing in a greenhouse, incorporating both the base bonus and the
     * out-of-season penalty recovery.
     */
    public static float yieldMultiplier(ItemStack seedStack, String currentSeason, GreenhouseClimate climate)
    {
        float bonus = greenhouseBonusFor(seedStack);
        boolean outOfSeason = isOutOfSeason(seedStack, currentSeason);

        // In a greenhouse, out-of-season crops can still grow, but with reduced yield unless the
        // greenhouse climate is excellent
        float seasonMod = outOfSeason ? 0.6f : 1.0f;
        float climateMod = climate.growthModifier();

        return bonus * seasonMod * climateMod;
    }

    /** Identify a crop from its seed item. Returns a simple string ID. */
    private static String identifyCrop(ItemStack stack)
    {
        if (stack.isEmpty()) return "unknown";
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = id.getPath();

        // Match TFC crop seed names and vanilla crop names
        if (path.contains("tomato")) return "tomato";
        if (path.contains("pepper") || path.contains("bell_pepper") || path.contains("chili")) return "pepper";
        if (path.contains("cucumber")) return "cucumber";
        if (path.contains("strawberry") || path.contains("wintergreen")) return "strawberry";
        if (path.contains("cabbage")) return "cabbage";
        if (path.contains("lettuce") || path.contains("green_lettuce")) return "lettuce";
        if (path.contains("spinach") || path.contains("kale")) return "spinach";
        if (path.contains("onion")) return "onion";
        if (path.contains("wheat")) return "wheat";
        if (path.contains("barley")) return "barley";
        if (path.contains("oat")) return "oat";
        if (path.contains("rye")) return "rye";
        if (path.contains("maize") || path.contains("corn")) return "maize";
        if (path.contains("rice")) return "rice";
        if (path.contains("potato")) return "potato";
        if (path.contains("carrot")) return "carrot";
        if (path.contains("beet") || path.contains("beetroot")) return "beet";
        if (path.contains("turnip") || path.contains("rutabaga")) return "turnip";
        if (path.contains("sugarcane") || path.contains("sugar_cane")) return "sugarcane";
        if (path.contains("melon")) return "melon";
        if (path.contains("pumpkin")) return "pumpkin";
        if (path.contains("soybean") || path.contains("soy")) return "soybean";

        return "unknown";
    }

    /**
     * Determine the current season from the game day. Uses a simple 96-day year with 4 seasons.
     */
    public static String currentSeason(long dayTime)
    {
        long day = dayTime / 24000L;
        long dayInYear = day % 96;
        if (dayInYear < 24) return "spring";
        if (dayInYear < 48) return "summer";
        if (dayInYear < 72) return "autumn";
        return "winter";
    }

    /**
     * @param cropId        the crop identifier
     * @param greenhouseBonus how much faster this crop grows in a greenhouse (1.0 = no bonus)
     * @param naturalSeasons which seasons this crop normally grows in outside a greenhouse
     * @param climateSensitive whether this crop is highly sensitive to temperature (large greenhouse bonus in cold biomes)
     */
    public record CropProfile(String cropId, float greenhouseBonus, Set<String> naturalSeasons, boolean climateSensitive) {}

    private CropSpecialization() {}
}
