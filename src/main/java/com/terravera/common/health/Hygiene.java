/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.health;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.dries007.tfc.util.calendar.Calendars;

import com.terravera.config.TerraVeraConfig;

/**
 * Getting dirty, and getting clean again.
 * <p>
 * Hygiene is the low-tech, always-available defence in the disease system, and the one that most directly rewards
 * changing how you play rather than what you have built. It is a single {@code [0, 1]} value that:
 * <ul>
 *     <li>drops when you butcher an animal, work in mud, handle waste, or simply go a long time without washing;</li>
 *     <li>rises when you wash - with plain water at first, better with ash or lye, best with soap;</li>
 *     <li>multiplies the infection chance of every food-, wound-, and sanitation-borne illness in the mod.</li>
 * </ul>
 * There is no threshold and no binary "clean" state, so there is never a point at which washing stops being worth it,
 * and washing before you eat is a genuinely good habit rather than a ritual the game demands.
 */
public final class Hygiene
{
    /** Minimum ticks between washes, so washing cannot be spammed to hold hygiene pinned at 1. */
    private static final int WASH_COOLDOWN = 200;

    // --- How much various activities cost you ---------------------------------------------------------------

    /** Butchering an animal. Blood and gut contents; the classic route for a foodborne infection. */
    public static final float SOIL_BUTCHERING = 0.22f;
    /** Working in mud, muck, or waste. */
    public static final float SOIL_FILTH = 0.12f;
    /** Handling raw meat or fish with your hands. */
    public static final float SOIL_RAW_MEAT = 0.06f;
    /** General passive grime, applied slowly over time. */
    public static final float SOIL_PASSIVE = 0.02f;

    // --- How much washing gets back -------------------------------------------------------------------------

    /** Rinsing in whatever water is to hand. Better than nothing; not much better. */
    public static final float WASH_WATER = 0.30f;
    /** Washing with ash or lye - genuinely alkaline, genuinely effective. */
    public static final float WASH_ASH = 0.55f;
    /** Soap. The single largest hygiene improvement available. */
    public static final float WASH_SOAP = 0.90f;

    /**
     * Applies grime to the player.
     *
     * @param amount how filthy the activity is, typically one of the {@code SOIL_*} constants
     */
    public static void soil(Player player, float amount)
    {
        if (player.level().isClientSide() || player.isCreative()) return;
        if (!TerraVeraConfig.SERVER.enableHygiene.get()) return;

        final PlayerHealth health = IllnessTracker.get(player);
        IllnessTracker.set(player, health.soiled(amount * TerraVeraConfig.SERVER.hygieneDecayMultiplier.get().floatValue()));
    }

    /**
     * Washes the player, if enough time has passed since the last wash.
     *
     * @param amount how good the wash is, typically one of the {@code WASH_*} constants
     * @return {@code true} if the player actually got cleaner, so the caller can consume soap / play a sound
     */
    public static boolean wash(Player player, float amount)
    {
        if (player.level().isClientSide() || !TerraVeraConfig.SERVER.enableHygiene.get()) return false;

        final long now = Calendars.get(player.level()).getTicks();
        final PlayerHealth health = IllnessTracker.get(player);
        if (now - health.lastWashTick() < WASH_COOLDOWN) return false;
        if (health.hygiene() >= 0.995f) return false;

        IllnessTracker.set(player, health.washed(amount, now));
        player.displayClientMessage(Component.translatable("terravera.hygiene.washed").withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    /**
     * @return how good a wash {@code stack} provides, or {@code 0} if it is not a hygiene item. Soap beats ash beats
     * an empty hand in water.
     */
    public static float washQuality(ItemStack stack)
    {
        if (stack.isEmpty()) return WASH_WATER;
        if (stack.is(TerraVeraHealthTags.Items.SOAP)) return WASH_SOAP;
        if (stack.is(TerraVeraHealthTags.Items.HYGIENE_ITEMS)) return WASH_ASH;
        return 0f;
    }

    /** A human-readable band for the tooltip / health screen. */
    public static String descriptorKey(float hygiene)
    {
        if (hygiene >= 0.85f) return "terravera.hygiene.clean";
        if (hygiene >= 0.6f) return "terravera.hygiene.passable";
        if (hygiene >= 0.35f) return "terravera.hygiene.grubby";
        if (hygiene >= 0.15f) return "terravera.hygiene.dirty";
        return "terravera.hygiene.filthy";
    }

    public static ChatFormatting descriptorColor(float hygiene)
    {
        if (hygiene >= 0.85f) return ChatFormatting.AQUA;
        if (hygiene >= 0.6f) return ChatFormatting.GREEN;
        if (hygiene >= 0.35f) return ChatFormatting.YELLOW;
        if (hygiene >= 0.15f) return ChatFormatting.GOLD;
        return ChatFormatting.RED;
    }

    private Hygiene() {}
}
