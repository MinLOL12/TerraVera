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
import net.minecraft.core.component.DataComponents;
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
import com.terravera.config.TerraVeraConfig;

/**
 * The recipe that turns {@code head + haft + cordage} into a finished stone tool.
 * <p>
 * The head is a normal TerraFirmaCraft stone tool head, knapped in TFC's own knapping screen from a loose rock. All
 * TerraVera changes is that you cannot simply put it on a stick - it has to be lashed on with cordage.
 * <p>
 * This is a custom recipe rather than a JSON shaped recipe because the output depends on the <em>components</em> of
 * the inputs: how strong the cordage is decides how much durability the finished tool has, and that cannot be
 * expressed in a vanilla recipe.
 *
 * @param head         the tool head this recipe hafts, e.g. {@code tfc:stone/axe_head/igneous_intrusive}
 * @param haft         what the head is mounted on, usually a stick
 * @param cordage      the required lashing
 * @param cordageCount how many lengths of cordage the lashing takes
 * @param result       the finished tool
 */
public record LashingRecipe(
    Ingredient head,
    Ingredient haft,
    Ingredient cordage,
    int cordageCount,
    ItemStack result
) implements CraftingRecipe
{
    public static final MapCodec<LashingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Ingredient.CODEC.fieldOf("head").forGetter(LashingRecipe::head),
        Ingredient.CODEC.fieldOf("haft").forGetter(LashingRecipe::haft),
        Ingredient.CODEC.fieldOf("cordage").forGetter(LashingRecipe::cordage),
        Codec.INT.optionalFieldOf("cordage_count", 1).forGetter(LashingRecipe::cordageCount),
        ItemStack.CODEC.fieldOf("result").forGetter(LashingRecipe::result)
    ).apply(i, LashingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, LashingRecipe> STREAM_CODEC = StreamCodec.composite(
        Ingredient.CONTENTS_STREAM_CODEC, LashingRecipe::head,
        Ingredient.CONTENTS_STREAM_CODEC, LashingRecipe::haft,
        Ingredient.CONTENTS_STREAM_CODEC, LashingRecipe::cordage,
        ByteBufCodecs.VAR_INT, LashingRecipe::cordageCount,
        ItemStack.STREAM_CODEC, LashingRecipe::result,
        LashingRecipe::new
    );

    @Override
    public boolean matches(CraftingInput input, Level level)
    {
        return find(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries)
    {
        final Cordage cordage = find(input);
        if (cordage == null) return ItemStack.EMPTY;

        final ItemStack output = result.copy();

        // Durability scales with the one thing the player actually controlled - how good the cordage holding the head
        // on is. An axe lashed with dry grass twine comes apart long before one lashed with bast.
        if (TerraVeraConfig.SERVER.scaleDurabilityByCordage.get())
        {
            final int baseDurability = output.getMaxDamage();
            if (baseDurability > 0)
            {
                final double min = TerraVeraConfig.SERVER.minimumDurabilityMultiplier.get();
                final double max = TerraVeraConfig.SERVER.maximumDurabilityMultiplier.get();
                final double multiplier = min + (max - min) * cordage.strength();
                output.set(DataComponents.MAX_DAMAGE, Math.max(1, (int) Math.round(baseDurability * multiplier)));
            }
        }

        // Remember what it was lashed with, so the tooltip (and any future re-lashing) can see it.
        output.set(TerraVeraDataComponents.CORDAGE.get(), cordage);
        return output;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height)
    {
        return width * height >= 2 + cordageCount;
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
        list.add(head);
        list.add(haft);
        for (int i = 0; i < cordageCount; i++) list.add(cordage);
        return list;
    }

    /** The output depends on the components of the inputs, so the recipe book cannot meaningfully preview it. */
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
     * Walks the crafting grid looking for exactly one head, one haft, and the required cordage. Shapeless: it does
     * not matter where in the grid the pieces are.
     *
     * @return the cordage used, or {@code null} if the grid does not match
     */
    private Cordage find(CraftingInput input)
    {
        Cordage found = null;
        int heads = 0, hafts = 0, cords = 0, other = 0;

        for (int i = 0; i < input.size(); i++)
        {
            final ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            if (head.test(stack))
            {
                heads += stack.getCount();
            }
            else if (cordage.test(stack))
            {
                cords += stack.getCount();
                if (found == null) found = stack.getOrDefault(TerraVeraDataComponents.CORDAGE.get(), Cordage.DEFAULT);
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

        if (heads != 1 || hafts != 1 || other > 0) return null;
        if (TerraVeraConfig.SERVER.requireCordageForHafting.get())
        {
            if (cords != cordageCount) return null;
        }
        else if (cords != 0 && cords != cordageCount)
        {
            return null;
        }
        return found != null ? found : Cordage.DEFAULT;
    }
}
