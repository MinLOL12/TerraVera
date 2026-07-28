/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.butchery;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Specialized butchering knife with tiers from wrought iron to red steel.
 * <p>
 * Designed specifically for working animal carcasses hanging on a Carcass Rack. Compared to general-purpose knives,
 * a Butcher's Knife cuts cleaner and faster, reducing waste and yielding more meat and organs from the carcass.
 * As you use it on a hanging carcass, the anatomical layers and pixels of the animal wear off realistically and drop loot.
 */
public class ButchersKnifeItem extends Item
{
    public enum Tier
    {
        WROUGHT_IRON("wrought_iron", 600, 0.80f, 6.0f),
        STEEL("steel", 1200, 0.92f, 7.5f),
        BLACK_STEEL("black_steel", 2000, 0.96f, 8.5f),
        BLUE_STEEL("blue_steel", 3000, 1.00f, 9.5f),
        RED_STEEL("red_steel", 3000, 1.00f, 10.0f);

        private final String id;
        private final int durability;
        private final float keenness;
        private final float attackDamage;

        Tier(String id, int durability, float keenness, float attackDamage)
        {
            this.id = id;
            this.durability = durability;
            this.keenness = keenness;
            this.attackDamage = attackDamage;
        }

        public String id() { return id; }
        public int durability() { return durability; }
        public float keenness() { return keenness; }
        public float attackDamage() { return attackDamage; }
    }

    private final Tier tier;

    public ButchersKnifeItem(Tier tier, Properties properties)
    {
        super(properties.durability(tier.durability()));
        this.tier = tier;
    }

    public Tier tier() { return tier; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
    {
        tooltip.add(Component.translatable("terravera.tooltip.butchers_knife.tier",
            Component.translatable("terravera.metal." + tier.id())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("terravera.tooltip.butchers_knife.usage")
            .withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("terravera.tooltip.butchers_knife.wear")
            .withStyle(ChatFormatting.DARK_AQUA));
    }
}
