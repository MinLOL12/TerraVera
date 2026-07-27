/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.temperature;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import com.terravera.common.TerraVeraDataComponents;

/**
 * A wearable garment.
 * <p>
 * It is an {@link ArmorItem} so that it goes in the armour slots, renders on the player, and can be equipped by
 * right-clicking, exactly like every other piece of clothing in Minecraft. What TerraVera adds is that the item knows
 * what it is <em>made of</em>, which is what the body temperature system reads.
 * <p>
 * Physical protection is deliberately near-nil on most of these. A linen shirt is not armour, and pretending it is
 * would quietly make the clothing system the best armour progression in the game. The reason to wear a fur parka is
 * that you will otherwise freeze, not that it stops arrows.
 */
public class ClothingItem extends ArmorItem
{
    private final ClothingMaterial material;
    private final GarmentSlot garmentSlot;

    public ClothingItem(Holder<ArmorMaterial> armorMaterial, ClothingMaterial material, GarmentSlot slot, Properties properties)
    {
        super(armorMaterial, slot.type(), properties);
        this.material = material;
        this.garmentSlot = slot;
    }

    public ClothingMaterial material()
    {
        return material;
    }

    public GarmentSlot garmentSlot()
    {
        return garmentSlot;
    }

    /** Insulation this garment contributes to the whole body, i.e. material value scaled by how much it covers. */
    public float insulation()
    {
        return material.insulation() * garmentSlot.coverage();
    }

    /** How much of the wind this garment keeps off, weighted by coverage. */
    public float windProofing()
    {
        return material.windProof() * garmentSlot.coverage();
    }

    /** How freely sweat escapes through it, weighted by coverage. */
    public float breathability()
    {
        return material.breathability() * garmentSlot.coverage();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
    {
        super.appendHoverText(stack, context, tooltip, flag);

        tooltip.add(Component.translatable("terravera.tooltip.clothing.material",
            Component.translatable(material.translationKey())).withStyle(ChatFormatting.GRAY));

        // Described in words rather than numbers, to match the rest of the mod: the player learns that wool is warm
        // by wearing it in winter, not by comparing two decimals.
        tooltip.add(Component.translatable(warmthKey()).withStyle(warmthColour()));

        if (material.windProof() >= 0.7f)
        {
            tooltip.add(Component.translatable("terravera.tooltip.clothing.windproof").withStyle(ChatFormatting.AQUA));
        }
        if (material.breathability() >= 0.85f)
        {
            tooltip.add(Component.translatable("terravera.tooltip.clothing.breathable").withStyle(ChatFormatting.GREEN));
        }
        if (material.wetPenalty() <= 0.5f)
        {
            tooltip.add(Component.translatable("terravera.tooltip.clothing.warm_when_wet").withStyle(ChatFormatting.AQUA));
        }

        final Wetness wetness = stack.get(TerraVeraDataComponents.GARMENT_WETNESS.get());
        if (wetness != null && wetness.wetness() > 0.05f)
        {
            tooltip.add(Component.translatable(wetness.descriptorKey()).withStyle(ChatFormatting.BLUE));
        }
    }

    private String warmthKey()
    {
        final float warmth = material.insulation();
        if (warmth >= 0.75f) return "terravera.tooltip.clothing.warmth.extreme";
        if (warmth >= 0.5f) return "terravera.tooltip.clothing.warmth.warm";
        if (warmth >= 0.3f) return "terravera.tooltip.clothing.warmth.moderate";
        if (warmth >= 0.18f) return "terravera.tooltip.clothing.warmth.light";
        return "terravera.tooltip.clothing.warmth.negligible";
    }

    private ChatFormatting warmthColour()
    {
        final float warmth = material.insulation();
        if (warmth >= 0.75f) return ChatFormatting.RED;
        if (warmth >= 0.5f) return ChatFormatting.GOLD;
        if (warmth >= 0.3f) return ChatFormatting.YELLOW;
        return ChatFormatting.GRAY;
    }
}
