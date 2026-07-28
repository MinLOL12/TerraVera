/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.butchery;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * What the blade in the player's hand is actually capable of.
 * <p>
 * Two separate things matter and they are deliberately kept apart. <em>Keenness</em> is the material: a knapped
 * flake will open an animal but it tears as much as it cuts, while a steel knife parts the seams. <em>Edge</em> is
 * the condition of that particular tool: a steel knife worn down to its last few points of durability is worse than
 * a fresh copper one. TFC's tool tiers map straight onto keenness, so a player's butchering improves as their
 * metalworking does without any separate progression.
 */
public record ButcheryTool(float keenness, float edge, boolean isBlade, boolean isSaw)
{
    /** Bare hands. You can pull a hide half off a rabbit and that is about it. */
    public static final ButcheryTool BARE_HANDS = new ButcheryTool(0.10f, 1.0f, false, false);

    private static final TagKey<Item> KNIVES = TagKey.create(
        net.minecraft.core.registries.Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath("tfc", "knives"));

    /** How cleanly this tool cuts, combining what it is made of with how worn it is. */
    public float cutQuality()
    {
        return Math.max(0f, Math.min(1f, keenness * (0.55f + 0.45f * edge)));
    }

    /**
     * How long the work takes relative to a sharp steel knife. A dull edge does not just waste meat, it makes you
     * saw at the carcass, which is the other half of why players should keep their knives in order.
     */
    public float workTimeMultiplier()
    {
        return 1f / Math.max(0.15f, cutQuality());
    }

    public static ButcheryTool of(ItemStack stack)
    {
        if (stack.isEmpty()) return BARE_HANDS;

        final String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        final boolean saw = path.contains("saw") || path.contains("axe") || path.contains("cleaver");
        final boolean blade = stack.is(KNIVES)
            || stack.is(ItemTags.SWORDS)
            || path.contains("knife")
            || path.contains("scalpel")
            || path.contains("shears")
            || saw;

        if (!blade) return BARE_HANDS;

        final float keenness = keennessOf(path);
        final float edge = edgeOf(stack);
        return new ButcheryTool(keenness, edge, true, saw);
    }

    /**
     * Material keenness, read from the item id so that it works for TFC metals, TerraVera's own knapped blades, and
     * any addon that follows the same naming, without a hard compile-time dependency on any of their registries.
     */
    private static float keennessOf(String path)
    {
        if (path.contains("stone") || path.contains("flint") || path.contains("knapped")
            || path.contains("igneous") || path.contains("sedimentary") || path.contains("metamorphic"))
        {
            return 0.35f;
        }
        if (path.contains("obsidian")) return 0.60f;
        if (path.contains("copper")) return 0.50f;
        if (path.contains("bronze")) return 0.65f;
        if (path.contains("wrought_iron") || path.contains("iron")) return 0.80f;
        if (path.contains("steel")) return 0.92f;
        if (path.contains("red_steel") || path.contains("blue_steel")) return 1.00f;
        if (path.contains("bone")) return 0.30f;
        return 0.55f;
    }

    /** Remaining edge, from durability. A tool at half wear has lost noticeably more than half its bite. */
    private static float edgeOf(ItemStack stack)
    {
        if (!stack.isDamageableItem() || stack.getMaxDamage() <= 0) return 1f;
        final float remaining = 1f - (float) stack.getDamageValue() / stack.getMaxDamage();
        // Squared: a knife does not go blunt linearly, it holds its edge and then loses it.
        return Math.max(0.05f, remaining * remaining);
    }

    /** Whether this tool is good enough to attempt the given stage at all. */
    public boolean canPerform(ButcheryStage stage)
    {
        if (!stage.needsBlade()) return true;
        // Bleeding and skinning can be forced with a poor edge; drawing and breaking down cannot.
        return isBlade && (cutQuality() >= 0.2f || stage == ButcheryStage.BLED || stage == ButcheryStage.SKINNED);
    }
}
