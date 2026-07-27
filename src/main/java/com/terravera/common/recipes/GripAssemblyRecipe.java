/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.recipes;

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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import com.terravera.common.TerraVeraDataComponents;
import com.terravera.common.component.ToolGrip;

/**
 * Wraps a grip around an existing, cordage-hafted wooden handle.
 * <p>
 * The recipe deliberately copies the tool stack. This preserves its wear, knapping, lashing, heat, and other data -
 * a grip is a refit, not a free replacement tool. Requiring an existing {@code CORDAGE} component also makes the
 * progression explicit: first make a stick handle and bind the head; only then is there a handle worth improving.
 */
public record GripAssemblyRecipe(String material, Ingredient grip, Ingredient binding)
    implements net.minecraft.world.item.crafting.CraftingRecipe
{
    public static final MapCodec<GripAssemblyRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        com.mojang.serialization.Codec.STRING.fieldOf("material").forGetter(GripAssemblyRecipe::material),
        Ingredient.CODEC.fieldOf("grip").forGetter(GripAssemblyRecipe::grip),
        Ingredient.CODEC.fieldOf("binding").forGetter(GripAssemblyRecipe::binding)
    ).apply(i, GripAssemblyRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GripAssemblyRecipe> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(24), GripAssemblyRecipe::material,
        Ingredient.CONTENTS_STREAM_CODEC, GripAssemblyRecipe::grip,
        Ingredient.CONTENTS_STREAM_CODEC, GripAssemblyRecipe::binding,
        GripAssemblyRecipe::new
    );

    @Override
    public boolean matches(CraftingInput input, Level level)
    {
        return findTool(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries)
    {
        final ItemStack tool = findTool(input);
        if (tool == null) return ItemStack.EMPTY;
        final ItemStack result = tool.copyWithCount(1);
        result.set(TerraVeraDataComponents.TOOL_GRIP.get(), "rubber".equals(material) ? ToolGrip.rubber() : ToolGrip.leather());
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height)
    {
        return width * height >= 3;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries)
    {
        return ItemStack.EMPTY; // The exact tool, including its data, is only known from the crafting input.
    }

    @Override
    public NonNullList<Ingredient> getIngredients()
    {
        final NonNullList<Ingredient> result = NonNullList.create();
        result.add(grip);
        result.add(binding);
        return result;
    }

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
        return TerraVeraRecipes.GRIP_ASSEMBLY.get();
    }

    private ItemStack findTool(CraftingInput input)
    {
        ItemStack tool = null;
        int gripCount = 0;
        int bindingCount = 0;

        for (int i = 0; i < input.size(); i++)
        {
            final ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (grip.test(stack))
            {
                gripCount++;
            }
            else if (binding.test(stack))
            {
                bindingCount++;
            }
            else if (stack.isDamageableItem()
                && stack.has(TerraVeraDataComponents.CORDAGE.get())
                && !stack.has(TerraVeraDataComponents.TOOL_GRIP.get()))
            {
                if (tool != null) return null;
                tool = stack;
            }
            else
            {
                return null;
            }
        }
        return tool != null && gripCount == 1 && bindingCount == 1 ? tool : null;
    }
}
