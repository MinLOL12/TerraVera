package com.terravera.common.items;

import com.terravera.common.TerraVeraDataComponents;
import com.terravera.common.component.CulinaryQuality;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The extended griddle catalogue. Each of the fourteen batter methods can be finished eight ways, yielding 112
 * individual cooked foods. The intermediate is intentionally separate so JSON recipes can require a hot surface.
 */
public final class GriddleFoods
{
    public record Entry(String base, String topping, String leavening, int restMinutes, float lightness, int taste) {}
    public static final Map<String, DeferredHolder<Item, Item>> BATTERS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Item, Item>> FOODS = new LinkedHashMap<>();
    public static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    private static final String[] BASES = {"unleavened", "yeast", "sourdough", "soda", "powder", "whipped_white", "fermented", "buckwheat", "oat", "rye", "corn", "chestnut", "barley", "spelt"};
    private static final String[] TOPPINGS = {"plain", "butter", "honey", "maple", "berry", "preserve", "cream", "nut_spice"};

    public static void register(DeferredRegister.Items items)
    {
        if (!ENTRIES.isEmpty()) return;
        for (int b = 0; b < BASES.length; b++)
        {
            String base = BASES[b];
            String leavening = switch (base)
            {
                case "yeast" -> "yeast";
                case "sourdough" -> "sourdough_starter";
                case "soda" -> "baking_soda";
                case "powder" -> "baking_powder";
                case "whipped_white" -> "whipped_egg_white";
                case "fermented" -> "fermented_batter";
                default -> "none";
            };
            int rest = switch (leavening) { case "yeast" -> 90; case "sourdough_starter" -> 480; case "fermented_batter" -> 240; case "whipped_egg_white" -> 10; default -> 20; };
            float lightness = switch (leavening) { case "yeast" -> .82f; case "sourdough_starter" -> .78f; case "baking_powder" -> .72f; case "baking_soda" -> .63f; case "whipped_egg_white" -> .86f; case "fermented_batter" -> .70f; default -> .34f; };
            for (int t = 0; t < TOPPINGS.length; t++)
            {
                final int toppingIndex = t;
                String id = base + "_griddle_" + TOPPINGS[toppingIndex];
                // Some plain, unleavened foods are deliberately merely serviceable; long-fermented sweet meals shine.
                int taste = Math.max(12, Math.min(96, 26 + b * 2 + new int[] {-14, 5, 20, 23, 18, 16, 24, 19}[toppingIndex] + Math.round(lightness * 12)));
                Entry entry = new Entry(base, TOPPINGS[toppingIndex], leavening, rest, lightness, taste);
                ENTRIES.put(id, entry);
                BATTERS.put(id, items.register(id + "_batter", () -> new Item(new Item.Properties().stacksTo(16))));
                FOODS.put(id, items.register(id, () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(5 + Math.max(0, toppingIndex - 1)).saturationModifier(.42f + lightness * .42f).build())
                    .component(TerraVeraDataComponents.CULINARY_QUALITY.get(), new CulinaryQuality(leavening, rest, lightness, taste)))));
            }
        }
    }

    private GriddleFoods() {}
}
