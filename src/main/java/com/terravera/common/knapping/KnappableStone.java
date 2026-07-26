/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.knapping;

import javax.annotation.Nullable;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import net.dries007.tfc.util.data.DataManager;

import com.terravera.TerraVera;

/**
 * Declares that a given item is a lump of stone you can sit down and knap, and what kind of stone it is.
 * <p>
 * Lives in {@code data/<namespace>/terravera/knappable_stone/<name>.json}. TerraVera ships one per TerraFirmaCraft
 * rock category plus obsidian; adding a new stone to the system is a single JSON file.
 *
 * @param ingredient which items count
 * @param material   the material key, used for the resulting head's component, the tooltip, and to pick the TFC tier
 *                   of the finished tool. Conventionally matches a TFC rock category, e.g. {@code igneous_intrusive}.
 * @param consume    how many items are consumed per knapping session
 * @param brittle    brittle stone (obsidian, chert) takes a finer edge but tolerates less abuse. Applies a penalty to
 *                   base requirements and a bonus to tip requirements.
 */
public record KnappableStone(Ingredient ingredient, String material, int consume, boolean brittle)
{
    public static final Codec<KnappableStone> CODEC = RecordCodecBuilder.create(i -> i.group(
        Ingredient.CODEC.fieldOf("ingredient").forGetter(KnappableStone::ingredient),
        Codec.STRING.fieldOf("material").forGetter(KnappableStone::material),
        Codec.INT.optionalFieldOf("consume", 2).forGetter(KnappableStone::consume),
        Codec.BOOL.optionalFieldOf("brittle", false).forGetter(KnappableStone::brittle)
    ).apply(i, KnappableStone::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, KnappableStone> STREAM_CODEC = StreamCodec.composite(
        Ingredient.CONTENTS_STREAM_CODEC, KnappableStone::ingredient,
        ByteBufCodecs.stringUtf8(64), KnappableStone::material,
        ByteBufCodecs.VAR_INT, KnappableStone::consume,
        ByteBufCodecs.BOOL, KnappableStone::brittle,
        KnappableStone::new
    );

    public static final DataManager<KnappableStone> MANAGER = new DataManager<>(
        ResourceLocation.fromNamespaceAndPath(TerraVera.MOD_ID, "knappable_stone"), CODEC, STREAM_CODEC);

    @Nullable
    public static KnappableStone get(ItemStack stack)
    {
        if (stack.isEmpty()) return null;
        for (KnappableStone stone : MANAGER.getValues())
        {
            if (stone.ingredient.test(stack)) return stone;
        }
        return null;
    }

    public boolean matches(ItemStack stack)
    {
        return stack.getCount() >= consume && ingredient.test(stack);
    }
}
