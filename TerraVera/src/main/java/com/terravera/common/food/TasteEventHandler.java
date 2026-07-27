package com.terravera.common.food;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "terravera")
public class TasteEventHandler {

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack stack = event.getItem();
        if (stack.isEmpty() || !stack.getItem().isEdible()) return;

        // Get the original saturation value from the food item
        float baseSaturation = stack.getItem().getFoodProperties().getSaturationModifier();

        // Apply taste system
        float newSaturation = TasteSystem.onFoodEaten(player, stack, baseSaturation);

        // Override saturation if the system changed the value
        // Note: In a real TFC integration you would use the TFC food capability instead
        if (newSaturation != baseSaturation) {
            // This is a simplified example - real implementation would modify the TFC FoodData
            // For now we log the change (you can hook into TFC's nutrition system here)
            System.out.println("[TasteSystem] " + stack.getItem() + " saturation modified: " +
                    baseSaturation + " -> " + newSaturation);
        }
    }
}
