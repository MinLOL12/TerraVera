/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.health;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import net.dries007.tfc.common.blockentities.IHeatable;
import net.dries007.tfc.common.blockentities.PotBlockEntity;
import net.dries007.tfc.common.fluids.FluidHelpers;

import com.terravera.TerraVera;
import com.terravera.config.TerraVeraConfig;

/**
 * Boiling, pasteurizing, and filtering water - the first ways out of the disease system, and the ones that use the
 * devices TerraFirmaCraft already has.
 * <p>
 * The design goal here was that water treatment should use the devices TerraFirmaCraft already has, at the point in
 * progression where the player already has them, rather than introducing a parallel "water purifier" tech tree.
 *
 * <h4>Boiling</h4>
 * Hold a filled container against a pot that is at or above boiling and the water in it is marked boiled. That is it.
 * The pot is one of the first things a TFC player builds - clay, a firepit, and a knapped mould - so the complete
 * answer to waterborne disease is available on day one to a player who thinks of it. What it costs is fuel and time,
 * every single time, and that recurring cost is the actual gameplay: you boil what you are about to carry, not the
 * whole lake.
 *
 * <h4>Pasteurization</h4>
 * The same TFC pot, held hot but below the boil, marks water pasteurized instead. Real pasteurization (63-99C,
 * sustained) kills most bacteria and viruses but leaves the hardy protozoan cysts - Giardia and Cryptosporidium are
 * famously resistant to mere heat. Boiling remains strictly better, which keeps the fuel cost meaningful.
 *
 * <h4>Filtration</h4>
 * A sand-and-charcoal filter is crafted (see the recipes) and used on a filled container. It removes the protozoa -
 * Giardia and Cryptosporidium, the two illnesses that most reliably ruin a long journey - and most of the load, but
 * leaves the bacteria. It is portable, reusable, and does not need fire, so it is the field answer where boiling is the
 * camp answer. Crucially it is <em>not</em> a substitute for boiling against cholera and typhoid, which keeps both
 * tiers relevant.
 */
public final class BoilingHandler
{
    /** Water boils at 100C; TFC's pot recipes use 300 as their "hot enough" threshold, so we sit comfortably above. */
    private static final float BOILING_TEMPERATURE = 100f;
    /** Pasteurization starts at 63C - the low end of the real "VAT" pasteurization band. Below this, nothing happens. */
    private static final float PASTEURIZATION_TEMPERATURE = 63f;

    public static void init()
    {
        NeoForge.EVENT_BUS.register(BoilingHandler.class);
    }

    /**
     * Right-clicking a hot pot with a filled water container boils the water in it.
     * <p>
     * This runs before TFC's own pot interaction would try to transfer fluid, and only when the player is holding
     * something that already contains water, so it never interferes with normal cooking.
     */
    @SubscribeEvent
    public static void onUsePot(PlayerInteractEvent.RightClickBlock event)
    {
        if (!TerraVeraConfig.SERVER.enableWaterContamination.get()) return;

        final Level level = event.getLevel();
        if (level.isClientSide()) return;

        final ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        final WaterTreatment current = WaterTreatment.get(stack);
        if (current == null || current.treatment().rank() >= WaterTreatment.Treatment.BOILED.rank()) return;
        if (FluidHelpers.getContainedFluid(stack).isEmpty()) return;

        final BlockPos pos = event.getPos();
        final BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof IHeatable heatable)) return;

        // A pot has to actually have water in it and be hot; a cold pot is just a pot.
        if (heatable.getTemperature() < PASTEURIZATION_TEMPERATURE)
        {
            event.getEntity().displayClientMessage(
                Component.translatable("terravera.water.pot_not_hot").withStyle(ChatFormatting.GRAY), true);
            return;
        }

        if (entity instanceof PotBlockEntity || heatable.getTemperature() >= PASTEURIZATION_TEMPERATURE)
        {
            // Hot-but-not-boiling pasteurizes; a rolling boil is the only thing that kills the cysts as well.
            final WaterTreatment.Treatment result = heatable.getTemperature() >= BOILING_TEMPERATURE
                ? WaterTreatment.Treatment.BOILED : WaterTreatment.Treatment.PASTEURIZED;
            WaterTreatment.set(stack, new WaterTreatment(result, current.sourceContamination()));
            level.playSound(null, pos,
                result == WaterTreatment.Treatment.BOILED ? SoundEvents.BREWING_STAND_BREW : SoundEvents.BUBBLE_COLUMN_BUBBLE_POP,
                SoundSource.BLOCKS, 0.6f, result == WaterTreatment.Treatment.BOILED ? 1.4f : 1.1f);
            event.getEntity().displayClientMessage(
                Component.translatable(result == WaterTreatment.Treatment.BOILED
                    ? "terravera.water.boiled" : "terravera.water.pasteurized").withStyle(ChatFormatting.AQUA), true);
            event.setCanceled(true);
            TerraVera.LOGGER.debug("Treated water for {} ({})", event.getEntity().getGameProfile().getName(), result.id());
        }
    }

    /**
     * Applies a filter to a container of water. Called from the filter item itself.
     *
     * @return {@code true} if the water was actually improved
     */
    public static boolean filter(Player player, ItemStack container)
    {
        final WaterTreatment current = WaterTreatment.get(container);
        if (current.treatment() != WaterTreatment.Treatment.UNTREATED) return false;
        if (FluidHelpers.getContainedFluid(container).isEmpty()) return false;

        WaterTreatment.set(container, current.filtered());
        player.displayClientMessage(
            Component.translatable("terravera.water.filtered").withStyle(ChatFormatting.GREEN), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 0.5f, 1.0f);
        return true;
    }

    private BoilingHandler() {}
}
