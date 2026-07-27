package com.terravera.common.food;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.bus.api.SubscribeEvent;

import com.terravera.TerraVera;

/**
 * Handles food eating events and applies the taste system to modify effective saturation gained.
 * This actually modifies the player's FoodData (hunger + saturation) instead of just printing.
 */
public final class TasteEventHandler {

    public static void init() {
        NeoForge.EVENT_BUS.register(TasteEventHandler.class);
        TerraVera.LOGGER.info("TasteSystem event handler registered");
    }

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return; // server only

        ItemStack stack = event.getItem();
        if (stack.isEmpty() || !stack.has(DataComponents.FOOD)) return;

        FoodProperties props = stack.get(DataComponents.FOOD);
        if (props == null) return;

        int nutrition = props.nutrition();
        float satModifier = props.saturation();

        // Vanilla formula for added saturation from this food: nutrition * satMod * 2
        float baseSaturation = Math.max(0, nutrition * satModifier * 2.0f);

        // Compute what the taste system wants the effective saturation gain to be
        float effectiveSaturation = TasteSystem.onFoodEaten(player, stack, baseSaturation);

        FoodData foodData = player.getFoodData();
        float currentSat = foodData.getSaturationLevel();
        int currentFood = foodData.getFoodLevel();

        // Apply delta to bring the *gain* to the taste-adjusted value
        float deltaSat = effectiveSaturation - baseSaturation;
        if (Math.abs(deltaSat) > 0.001f) {
            float newSat = Math.max(0.0f, Math.min(currentSat + deltaSat, (float) currentFood));
            foodData.setSaturation(newSat);
        }

        // For very bad tasting food, reduce some of the hunger restored (makes "disgusting" food less filling)
        int baseTaste = TasteSystem.getBaseTaste(stack);
        if (baseTaste <= TasteSystem.TASTE_BAD && nutrition > 1) {
            // Scale penalty: worse taste = more reduction (up to ~35% of nutrition)
            float penaltyFactor = Math.min(0.35f, (TasteSystem.TASTE_BAD - baseTaste) / 150.0f);
            int hungerReduction = Math.max(0, (int) (nutrition * penaltyFactor));
            if (hungerReduction > 0) {
                int newHunger = Math.max(currentFood - hungerReduction, currentFood - nutrition + 1); // never remove more than was granted
                foodData.setFoodLevel(newHunger);
            }
        }

        // Give the player some feedback when taste has a noticeable impact
        if (Math.abs(deltaSat) > 1.5f || baseTaste <= TasteSystem.TASTE_BAD) {
            String msg = getTasteFeedback(baseTaste, deltaSat);
            if (!msg.isEmpty()) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(msg), true); // action bar
            }
        }
    }

    private static String getTasteFeedback(int taste, float satDelta) {
        if (taste <= TasteSystem.TASTE_DISGUSTING + 10) return "§cThat tasted awful...";
        if (taste <= TasteSystem.TASTE_BAD) return "§7Ugh, that was pretty bad.";
        if (taste >= TasteSystem.TASTE_EXCEPTIONAL - 5) return "§6Absolutely delicious!";
        if (taste >= TasteSystem.TASTE_DELICIOUS) return "§aReally good!";
        if (taste >= TasteSystem.TASTE_GOOD) return "§aTasty.";
        if (satDelta < -2.0f) return "§7Not very satisfying...";
        return "";
    }

    private TasteEventHandler() {}
}
