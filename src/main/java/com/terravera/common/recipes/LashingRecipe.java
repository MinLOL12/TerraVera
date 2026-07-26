/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.recipes;

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
import com.terravera.common.component.Cordage;
import com.terravera.common.component.KnappedHead;
import com.terravera.common.items.HeadItem;
import com.terravera.config.TerraVeraConfig;

/**
 * The recipe that turns {@code head + haft + cordage} into a finished stone tool.
 * <p>
 * This is a shapeless custom recipe rather than a JSON shaped recipe for two reasons. First, the output depends on the
 * <em>components</em> of the inputs - which stone the head is, how well it was knapped, what the cordage is twisted
 * from - and there is no way to express that in a vanilla recipe. Second, one recipe instance covers every
 * (stone x head kind) combination, which is what stops the mod from needing dozens of near-identical recipe files.
 *
 * @param headKind which working end this recipe hafts, e.g. {@code wedge}
 * @param haft     what the head is mounted on, usually a stick
 * @param cordage  the required lashing
 * @param cordageCount how many lengths of cordage the lashing takes
 * @param results  candidate results, one per material; the first whose material matches the head's is used
 */
public record LashingRecipe(
    String headKind,
    Ingredient haft,
    Ingredient cordage,
    int cordageCount,
    List<MaterialResult> results
) implements net.minecraft.world.item.crafting.CraftingRecipe
{
    public static final MapCodec<LashingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        com.mojang.serialization.Codec.STRING.fieldOf("head_kind").forGetter(LashingRecipe::headKind),
        Ingredient.CODEC.fieldOf("haft").forGetter(LashingRecipe::haft),
        Ingredient.CODEC.fieldOf("cordage").forGetter(LashingRecipe::cordage),
        com.mojang.serialization.Codec.INT.optionalFieldOf("cordage_count", 1).forGetter(LashingRecipe::cordageCount),
        MaterialResult.CODEC.listOf().fieldOf("results").forGetter(LashingRecipe::results)
    ).apply(i, LashingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, LashingRecipe> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(64), LashingRecipe::headKind,
        Ingredient.CONTENTS_STREAM_CODEC, LashingRecipe::haft,
        Ingredient.CONTENTS_STREAM_CODEC, LashingRecipe::cordage,
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

        // Durability is the product of the two things the player actually controlled: how well they knapped the head,
        // and how good the cordage holding it on is. A masterful head lashed with grass twine is still a bad axe.
        if (TerraVeraConfig.SERVER.scaleDurabilityByCraftsmanship.get())
        {
            final int baseDurability = result.getMaxDamage();
            if (baseDurability > 0)
            {
                final float craftsmanship = 0.6f * match.head.quality() + 0.4f * match.cordage.strength();
                final double min = TerraVeraConfig.SERVER.minimumDurabilityMultiplier.get();
                final double max = TerraVeraConfig.SERVER.maximumDurabilityMultiplier.get();
                final double multiplier = min + (max - min) * craftsmanship;
                result.set(DataComponents.MAX_DAMAGE, Math.max(1, (int) Math.round(baseDurability * multiplier)));
            }
        }

        // Remember what it was lashed with, so that a future re-lashing recipe (and the tooltip) can see it.
        result.set(TerraVeraDataComponents.CORDAGE.get(), match.cordage);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height)
    {
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
        for (int i = 0; i < cordageCount; i++) list.add(cordage);
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
     */
    private Match find(CraftingInput input)
    {
        KnappedHead head = null;
        Cordage cordage = null;
        int hafts = 0, cords = 0, other = 0;

        for (int i = 0; i < input.size(); i++)
        {
            final ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            final KnappedHead candidate = stack.get(TerraVeraDataComponents.KNAPPED_HEAD.get());
            if (candidate != null && stack.getItem() instanceof HeadItem item && item.kindPath().equals(headKind))
            {
                if (head != null) return null; // Two heads, one haft - no
                head = candidate;
            }
            else if (this.cordage.test(stack))
            {
                cords += stack.getCount();
                if (cordage == null) cordage = stack.getOrDefault(TerraVeraDataComponents.CORDAGE.get(), Cordage.DEFAULT);
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
        if (TerraVeraConfig.SERVER.requireCordageForHafting.get() && cords != cordageCount) return null;
        if (cordage == null) cordage = Cordage.DEFAULT;

        for (MaterialResult result : results)
        {
            if (result.material().equals(head.material()))
            {
                return new Match(head, cordage, result.result());
            }
        }
        return null;
    }

    private record Match(KnappedHead head, Cordage cordage, ItemStack result) {}
}
