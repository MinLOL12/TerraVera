/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import com.terravera.common.TerraVeraDataComponents;
import com.terravera.common.component.Cordage;

/**
 * Twists loose fibre into cordage. The interesting part is that the strength of the input fibre is carried through
 * into the output, so cordage made from nettle bast is meaningfully better than cordage made from dry grass, without
 * needing a separate item for each.
 * <p>
 * Cordage also carries a length value that affects how well it can secure tool heads. The length is based on the
 * recipe configuration and the quality of the fibre used.
 *
 * @param fibre         the fibre consumed
 * @param count         how many fibres per cord
 * @param result        the cord produced
 * @param baseStrength  strength floor for this recipe, before the fibre's own strength is mixed in
 * @param baseLengthMM  the base length of the cordage in millimeters
 */
public record TwistingRecipe(
    Ingredient fibre,
    int count,
    ItemStack result,
    float baseStrength,
    int baseLengthMM
) implements CraftingRecipe
{
    public static final MapCodec<TwistingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Ingredient.CODEC.fieldOf("fibre").forGetter(TwistingRecipe::fibre),
        Codec.INT.optionalFieldOf("count", 3).forGetter(TwistingRecipe::count),
        ItemStack.CODEC.fieldOf("result").forGetter(TwistingRecipe::result),
        Codec.FLOAT.optionalFieldOf("base_strength", 0.4f).forGetter(TwistingRecipe::baseStrength),
        Codec.INT.optionalFieldOf("base_length_mm", 350).forGetter(TwistingRecipe::baseLengthMM)
    ).apply(i, TwistingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TwistingRecipe> STREAM_CODEC = StreamCodec.composite(
        Ingredient.CONTENTS_STREAM_CODEC, TwistingRecipe::fibre,
        ByteBufCodecs.VAR_INT, TwistingRecipe::count,
        ItemStack.STREAM_CODEC, TwistingRecipe::result,
        ByteBufCodecs.FLOAT, TwistingRecipe::baseStrength,
        ByteBufCodecs.VAR_INT, TwistingRecipe::baseLengthMM,
        TwistingRecipe::new
    );

    @Override
    public boolean matches(CraftingInput input, Level level)
    {
        int found = 0;
        for (int i = 0; i < input.size(); i++)
        {
            final ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (!fibre.test(stack)) return false;
            found += stack.getCount();
        }
        return found == count;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries)
    {
        // Cordage inherits the strength of the best fibre that went into it, floored by the recipe's own base.
        float strength = baseStrength;
        String source = "mixed";
        int totalLength = 0;
        int fibreCount = 0;
        
        for (int i = 0; i < input.size(); i++)
        {
            final ItemStack stack = input.getItem(i);
            if (stack.isEmpty() || !fibre.test(stack)) continue;
            
            final Cordage carried = stack.get(TerraVeraDataComponents.CORDAGE.get());
            if (carried != null)
            {
                if (carried.strength() > strength)
                {
                    strength = carried.strength();
                    source = carried.source();
                }
                // Longer input fibre = potentially longer cordage
                totalLength += carried.lengthMM();
                fibreCount++;
            }
        }
        
        // Calculate final length based on base and quality of fibre
        // Better fibre (higher strength) can result in slightly longer cordage
        float qualityBonus = (strength - baseStrength) * 100; // Up to +60mm bonus for excellent fibre
        int finalLength = Math.min(800, Math.max(200, (int)(baseLengthMM + qualityBonus)));

        final ItemStack out = result.copy();
        out.set(TerraVeraDataComponents.CORDAGE.get(), new Cordage(Math.min(1f, strength), source, finalLength));
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height)
    {
        return width * height >= count;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries)
    {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients()
    {
        final NonNullList<Ingredient> list = NonNullList.create();
        for (int i = 0; i < count; i++) list.add(fibre);
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
        return CraftingBookCategory.MISC;
    }

    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return TerraVeraRecipes.TWISTING.get();
    }
}
