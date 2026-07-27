/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.recipes;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.terravera.TerraVera;

public final class TerraVeraRecipes
{
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(Registries.RECIPE_TYPE, TerraVera.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, TerraVera.MOD_ID);

    /** Hafting a knapped head onto a stick with cordage. Runs in the normal crafting grid. */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<LashingRecipe>> LASHING =
        register("lashing", LashingRecipe.CODEC, LashingRecipe.STREAM_CODEC);

    /** Twisting fibre into cordage, which carries the fibre's strength into the resulting cord. */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TwistingRecipe>> TWISTING =
        register("twisting", TwistingRecipe.CODEC, TwistingRecipe.STREAM_CODEC);

    /** Fitting a leather or rubber wrap to an existing cordage-hafted wooden handle. */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GripAssemblyRecipe>> GRIP_ASSEMBLY =
        register("grip_assembly", GripAssemblyRecipe.CODEC, GripAssemblyRecipe.STREAM_CODEC);

    private static <R extends Recipe<?>> DeferredHolder<RecipeSerializer<?>, RecipeSerializer<R>> register(
        String name, MapCodec<R> codec, StreamCodec<RegistryFriendlyByteBuf, R> streamCodec)
    {
        return RECIPE_SERIALIZERS.register(name, () -> new RecipeSerializer<R>()
        {
            @Override
            public MapCodec<R> codec()
            {
                return codec;
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec()
            {
                return streamCodec;
            }
        });
    }

    private TerraVeraRecipes() {}
}
