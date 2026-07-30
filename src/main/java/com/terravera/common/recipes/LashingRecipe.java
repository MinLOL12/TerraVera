/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.recipes;

import java.util.ArrayList;
import java.util.List;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import com.terravera.common.TerraVeraDataComponents;
import com.terravera.common.TerraVeraDataComponents.BindingBonus;
import com.terravera.common.TerraVeraDataComponents.DamageBonus;
import com.terravera.common.component.Cordage;
import com.terravera.common.component.Adhesive;
import com.terravera.common.component.KnappedHead;
import com.terravera.common.items.HeadItem;
import com.terravera.config.TerraVeraConfig;

/**
 * The recipe that turns {@code head + haft + cordage} into a finished stone tool.
 * <p>
 * This is a shapeless custom recipe rather than a JSON shaped recipe for two reasons. First, the output depends on the
 * <em>components</em> of the inputs - which stone the head is, how well it was knapped, what the cordage is twisted
 * from, and how long the cordage is - and there is no way to express that in a vanilla recipe. Second, one recipe 
 * instance covers every (stone x head kind) combination, which is what stops the mod from needing dozens of near-identical 
 * recipe files.
 * <p>
 * Cordage requirements: Either 2 normal cordage OR 2 heavy cordage. The length of each cordage piece affects:
 * <ul>
 *   <li><b>Durability</b>: Longer cordage creates a tighter binding, reducing wear on the tool head</li>
 *   <li><b>Speed</b>: Better bindings reduce wobble, allowing for faster tool use</li>
 *   <li><b>Damage</b>: Secure bindings transfer more force from the haft to the head</li>
 * </ul>
 *
 * @param headKind      which working end this recipe hafts, e.g. {@code wedge}
 * @param haft          what the head is mounted on, usually a stick
 * @param cordageNormal ingredient for normal cordage
 * @param cordageHeavy  ingredient for heavy cordage
 * @param cordageCount  how many lengths of cordage the lashing takes (typically 2)
 * @param results       candidate results, one per material; the first whose material matches the head's is used
 */
public record LashingRecipe(
    String headKind,
    Ingredient haft,
    Ingredient cordageNormal,
    Ingredient cordageHeavy,
    int cordageCount,
    List<MaterialResult> results
) implements net.minecraft.world.item.crafting.CraftingRecipe
{
    public static final MapCodec<LashingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        com.mojang.serialization.Codec.STRING.fieldOf("head_kind").forGetter(LashingRecipe::headKind),
        Ingredient.CODEC.fieldOf("haft").forGetter(LashingRecipe::haft),
        Ingredient.CODEC.fieldOf("cordage_normal").forGetter(LashingRecipe::cordageNormal),
        Ingredient.CODEC.fieldOf("cordage_heavy").forGetter(LashingRecipe::cordageHeavy),
        com.mojang.serialization.Codec.INT.optionalFieldOf("cordage_count", 2).forGetter(LashingRecipe::cordageCount),
        MaterialResult.CODEC.listOf().fieldOf("results").forGetter(LashingRecipe::results)
    ).apply(i, LashingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, LashingRecipe> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(64), LashingRecipe::headKind,
        Ingredient.CONTENTS_STREAM_CODEC, LashingRecipe::haft,
        Ingredient.CONTENTS_STREAM_CODEC, LashingRecipe::cordageNormal,
        Ingredient.CONTENTS_STREAM_CODEC, LashingRecipe::cordageHeavy,
        ByteBufCodecs.VAR_INT, LashingRecipe::cordageCount,
        MaterialResult.STREAM_CODEC.apply(ByteBufCodecs.list()), LashingRecipe::results,
        LashingRecipe::new
    );

    /**
     * @param material the head material this result applies to, e.g. {@code igneous_intrusive}
     * @param result   the tool produced. Usually a TerraFirmaCraft stone tool of the matching tier.
     */
    public record MaterialResult(String material, ItemStack result)
    {
        public static final com.mojang.serialization.Codec<MaterialResult> CODEC = RecordCodecBuilder.create(i -> i.group(
            com.mojang.serialization.Codec.STRING.fieldOf("material").forGetter(MaterialResult::material),
            ItemStack.CODEC.fieldOf("result").forGetter(MaterialResult::result)
        ).apply(i, MaterialResult::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, MaterialResult> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), MaterialResult::material,
            ItemStack.STREAM_CODEC, MaterialResult::result,
            MaterialResult::new
        );
    }

    @Override
    public boolean matches(CraftingInput input, Level level)
    {
        return find(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries)
    {
        final Match match = find(input);
        if (match == null) return ItemStack.EMPTY;

        final ItemStack result = match.result.copy();

        // Calculate combined cordage binding quality
        float totalBindingQuality = 0f;
        int cordageUsed = 0;
        for (Cordage cordage : match.cordages)
        {
            totalBindingQuality += cordage.bindingQuality();
            cordageUsed++;
        }
        float avgBindingQuality = cordageUsed > 0 ? totalBindingQuality / cordageUsed : 1f;

        // Calculate combined cordage strength
        float totalCordageStrength = 0f;
        for (Cordage cordage : match.cordages)
        {
            totalCordageStrength += cordage.strength();
        }
        float avgCordageStrength = cordageUsed > 0 ? totalCordageStrength / cordageUsed : 0.5f;

        // Durability is affected by knapping quality, cordage strength, and cordage binding quality
        if (TerraVeraConfig.SERVER.scaleDurabilityByCraftsmanship.get())
        {
            final int baseDurability = result.getMaxDamage();
            if (baseDurability > 0)
            {
                // Weights: 40% head quality, 30% cordage strength, 30% binding quality from length
                final float craftsmanship = 0.4f * match.head.quality() + 0.3f * avgCordageStrength + 0.3f * avgBindingQuality;
                final double min = TerraVeraConfig.SERVER.minimumDurabilityMultiplier.get();
                final double max = TerraVeraConfig.SERVER.maximumDurabilityMultiplier.get();
                final double multiplier = min + (max - min) * craftsmanship;
                result.set(DataComponents.MAX_DAMAGE, Math.max(1, (int) Math.round(baseDurability * multiplier)));
            }
        }

        // Apply speed modifier from binding quality
        if (TerraVeraConfig.SERVER.applyBindingBonusToSpeed.get())
        {
            // Speed bonus: better binding = less wobble = faster tool
            // Scale: avgBindingQuality ranges from 0.5 to 1.5
            // Map to speed modifier: 0.9 to 1.1 (10% variation)
            float speedModifier = 0.9f + (avgBindingQuality - 0.5f) * 0.2f;
            speedModifier = Math.max(0.9f, Math.min(1.1f, speedModifier));
            result.set(TerraVeraDataComponents.BINDING_SPEED_BONUS.get(), new BindingBonus(speedModifier, avgBindingQuality));
        }

        // Apply damage modifier from binding quality
        if (TerraVeraConfig.SERVER.applyBindingBonusToDamage.get())
        {
            // Damage bonus: better binding = better force transfer = more damage
            // Scale: avgBindingQuality ranges from 0.5 to 1.5
            // Map to damage bonus: 0.85 to 1.15 (15% variation)
            float damageModifier = 0.85f + (avgBindingQuality - 0.5f) * 0.3f;
            damageModifier = Math.max(0.85f, Math.min(1.15f, damageModifier));
            result.set(TerraVeraDataComponents.BINDING_DAMAGE_BONUS.get(), new DamageBonus(damageModifier, avgBindingQuality));
        }

        // Remember what it was lashed with, so that a future re-lashing recipe (and the tooltip) can see it.
        // Use the average cordage properties for storage
        Cordage avgCordage = new Cordage(
            avgCordageStrength,
            match.cordages.isEmpty() ? "mixed" : match.cordages.get(0).source(),
            (int)(match.cordages.stream().mapToInt(Cordage::lengthMM).average().orElse(300))
        );
        result.set(TerraVeraDataComponents.CORDAGE.get(), avgCordage);
        // A glued haft is a distinct joint: retain its wet resistance/flexibility for tooltips and future wear logic.
        if (match.adhesive != null) result.set(TerraVeraDataComponents.ADHESIVE.get(), match.adhesive);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height)
    {
        // One head, one haft, and every length of cordage need their own crafting slot.
        return width * height >= 2 + cordageCount;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries)
    {
        return results.isEmpty() ? ItemStack.EMPTY : results.getFirst().result();
    }

    @Override
    public NonNullList<Ingredient> getIngredients()
    {
        final NonNullList<Ingredient> list = NonNullList.create();
        list.add(haft);
        for (int i = 0; i < cordageCount; i++) 
        {
            // Add both possibilities - the recipe system will handle matching
            list.add(cordageNormal);
            list.add(cordageHeavy);
        }
        return list;
    }

    /**
     * The output depends on the components of the inputs, not just their identities, so the vanilla recipe book
     * cannot meaningfully preview it.
     */
    @Override
    public boolean isSpecial()
    {
        return true;
    }

    @Override
    public CraftingBookCategory category()
    {
        return CraftingBookCategory.EQUIPMENT;
    }

    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return TerraVeraRecipes.LASHING.get();
    }

    /**
     * Walks the crafting grid looking for exactly one head of the right kind, one haft, and the required cordage.
     * Shapeless: it does not matter where in the grid the pieces are.
     * Requires either 2 normal cordage OR 2 heavy cordage.
     */
    private Match find(CraftingInput input)
    {
        KnappedHead head = null;
        List<Cordage> cordages = new ArrayList<>();
        int normalCordageSlots = 0;
        int heavyCordageSlots = 0;
        Adhesive adhesive = null;
        int hafts = 0, other = 0;

        for (int i = 0; i < input.size(); i++)
        {
            final ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            final KnappedHead candidate = stack.get(TerraVeraDataComponents.KNAPPED_HEAD.get());
            if (candidate != null && stack.getItem() instanceof HeadItem item && item.kindPath().equals(headKind))
            {
                if (head != null) return null; // Two heads - no
                head = candidate;
            }
            else if (stack.has(TerraVeraDataComponents.ADHESIVE.get()))
            {
                // One batch is enough to bed a stone head. Glue is accepted only as a complete alternative to a tie.
                if (adhesive != null) return null;
                adhesive = stack.get(TerraVeraDataComponents.ADHESIVE.get());
                // Reuse the binding-quality calculation while retaining the actual glue component on the result.
                cordages.add(new Cordage(adhesive.strength(), "glued", 100 + Math.round(500f * (0.5f + adhesive.flexibility()))));
            }
            else if (this.cordageHeavy.test(stack))
            {
                // Crafting consumes one item per occupied slot. Count slots rather than stack sizes so a stack of
                // cordage cannot satisfy a two-cord lashing while only one length is actually consumed.
                heavyCordageSlots++;
                cordages.add(stack.getOrDefault(TerraVeraDataComponents.CORDAGE.get(), Cordage.DEFAULT));
            }
            else if (this.cordageNormal.test(stack))
            {
                normalCordageSlots++;
                cordages.add(stack.getOrDefault(TerraVeraDataComponents.CORDAGE.get(), Cordage.DEFAULT));
            }
            else if (haft.test(stack))
            {
                hafts += stack.getCount();
            }
            else
            {
                other++;
            }
        }

        if (head == null || hafts != 1 || other > 0) return null;
        
        // Check cordage requirements: the occupied cordage slots must be exactly the required number of one type.
        // This both prevents mixing types and guarantees every required length is consumed by the crafting grid.
        if (TerraVeraConfig.SERVER.requireCordageForHafting.get())
        {
            final boolean hasCorrectNormal = normalCordageSlots == cordageCount && heavyCordageSlots == 0 && adhesive == null;
            final boolean hasCorrectHeavy = heavyCordageSlots == cordageCount && normalCordageSlots == 0 && adhesive == null;
            final boolean hasGlueJoint = adhesive != null && normalCordageSlots == 0 && heavyCordageSlots == 0;

            if (!hasCorrectNormal && !hasCorrectHeavy && !hasGlueJoint) return null;
        }

        for (MaterialResult result : results)
        {
            if (result.material().equals(head.material()))
            {
                return new Match(head, cordages, adhesive, result.result());
            }
        }
        return null;
    }

    private record Match(KnappedHead head, List<Cordage> cordages, Adhesive adhesive, ItemStack result) {}
}
