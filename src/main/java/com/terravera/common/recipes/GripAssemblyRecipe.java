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
 * Wraps a grip around a tool's handle.
 * <p>
 * The recipe deliberately copies the tool stack. This preserves its wear, knapping, lashing, heat, and other data -
 * a grip is a refit, not a free replacement tool.
 * <p>
 * <strong>What counts as a tool.</strong> Originally this recipe only accepted stacks carrying TerraVera's own
 * {@code CORDAGE} component, i.e. a head the player had personally lashed. In practice that meant grips could not be
 * fitted to <em>anything</em> a normal game produces: every metal tool, every TFC-crafted stone tool, and every tool
 * that had been repaired or traded lacks that component, so the recipe silently never matched. A handle wrap is a
 * physical thing you tie around a haft; it does not care how the haft was made. Any damageable tool now qualifies,
 * and the lashed-handle requirement survives as an opt-in flag ({@code require_lashed_handle}) for packs that want
 * the stricter progression.
 * <p>
 * Refitting is also allowed: a tool already wearing a leather wrap can be upgraded to rubber. Only re-fitting the
 * <em>same</em> material is rejected, because that would be a no-op that quietly eats a grip.
 */
public record GripAssemblyRecipe(String material, Ingredient grip, Ingredient binding, boolean requireLashedHandle)
    implements net.minecraft.world.item.crafting.CraftingRecipe
{
    public static final MapCodec<GripAssemblyRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        com.mojang.serialization.Codec.STRING.fieldOf("material").forGetter(GripAssemblyRecipe::material),
        Ingredient.CODEC.fieldOf("grip").forGetter(GripAssemblyRecipe::grip),
        Ingredient.CODEC.fieldOf("binding").forGetter(GripAssemblyRecipe::binding),
        com.mojang.serialization.Codec.BOOL.optionalFieldOf("require_lashed_handle", false)
            .forGetter(GripAssemblyRecipe::requireLashedHandle)
    ).apply(i, GripAssemblyRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GripAssemblyRecipe> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(24), GripAssemblyRecipe::material,
        Ingredient.CONTENTS_STREAM_CODEC, GripAssemblyRecipe::grip,
        Ingredient.CONTENTS_STREAM_CODEC, GripAssemblyRecipe::binding,
        ByteBufCodecs.BOOL, GripAssemblyRecipe::requireLashedHandle,
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
            else if (acceptsGrip(stack))
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

    /**
     * @return whether a wrap can be tied onto this stack. Any single damageable tool with a haft qualifies; the only
     * refusals are non-tools, stacks of more than one, and a tool that already wears this exact grip material.
     */
    private boolean acceptsGrip(ItemStack stack)
    {
        if (stack.getCount() != 1 || !stack.isDamageableItem()) return false;
        if (requireLashedHandle && !stack.has(TerraVeraDataComponents.CORDAGE.get())) return false;

        final com.terravera.common.component.ToolGrip existing = stack.get(TerraVeraDataComponents.TOOL_GRIP.get());
        return existing == null || !existing.material().equals(material);
    }
}
