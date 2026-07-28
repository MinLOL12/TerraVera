/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.health;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.dries007.tfc.common.component.food.IFood;
import net.dries007.tfc.common.fluids.FluidHelpers;
import net.dries007.tfc.util.calendar.Calendars;

import com.terravera.TerraVera;
import com.terravera.config.TerraVeraConfig;

/**
 * Where the disease system meets the rest of the game.
 * <p>
 * Every route of transmission in the mod is a listener in this class, and each one is small enough to read on its own:
 * <ul>
 *     <li><strong>Drinking</strong> - {@link #onDrinkFromWorld} evaluates the water block you are drinking from and
 *     rolls against its contamination. {@link #onFinishDrinking} does the same for a jug or bottle, reading the
 *     treatment record stamped on it when it was filled.</li>
 *     <li><strong>Eating</strong> - {@link #onFinishEating} handles rotten food, raw meat, and the dirty-hands
 *     penalty, and applies the malabsorption symptom at the point where nutrition is actually granted.</li>
 *     <li><strong>Wounds</strong> - {@link #onDamage} rolls for wound infection and tetanus when you take a wound
 *     while filthy, or from something that has been in the ground.</li>
 *     <li><strong>Contact</strong> - {@link #onPlayerTick} spreads respiratory illness between players who spend time
 *     near each other.</li>
 *     <li><strong>Filth</strong> - block breaking and standing in muck soil the player, feeding the hygiene system.</li>
 * </ul>
 */
public final class HealthEventHandler
{
    /** How often, in ticks, we check for contagion and passive grime. Cheap by construction. */
    private static final int SLOW_TICK = 100;

    public static void init()
    {
        NeoForge.EVENT_BUS.register(HealthEventHandler.class);
        TerraVera.LOGGER.info("TerraVera disease, water, and sanitation system registered");
    }

    // ----- Water --------------------------------------------------------------------------------------------

    /**
     * Drinking straight from a water block. This fires at LOW priority so that TFC's own drink handling - which runs
     * at LOWEST - still owns the interaction; we only observe it and roll for exposure.
     * <p>
     * We do not cancel or replace the drink. Refusing to let the player drink from a pond would be a worse game than
     * letting them drink from it and then dealing with the consequences three days later.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDrinkFromWorld(PlayerInteractEvent.RightClickBlock event)
    {
        final Player player = event.getEntity();
        final Level level = event.getLevel();
        if (level.isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!player.getMainHandItem().isEmpty()) return;

        // Use the block the player actually clicked first.  Re-raytracing with SOURCE_ONLY is subtly wrong here:
        // it can miss a perfectly drinkable flowing TFC source when the client/server eye position differs by a
        // fraction of a block (and it also misses water clicked through a non-source fluid face).
        final BlockPos clickedPos = event.getPos();
        final BlockHitResult hit = getWaterHit(level, player, clickedPos);
        if (hit == null) return;

        final BlockPos pos = hit.getBlockPos();
        final BlockState state = level.getBlockState(pos);
        final WaterSource source = WaterSource.evaluate(level, pos, state);
        if (source == null || source.salty()) return;

        // Restore stamina
        com.terravera.common.temperature.TemperatureSystem.addStamina(player, 40f);

        if (!TerraVeraConfig.SERVER.enableWaterContamination.get()) return;

        drinkUntreated(player, source.contamination(), source.quality());
    }

    /**
     * Drinking from a container. The container knows where its water came from and what has been done to it, so this
     * is where boiling and filtration actually pay off.
     */
    @SubscribeEvent
    public static void onFinishDrinking(LivingEntityUseItemEvent.Finish event)
    {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;

        final ItemStack stack = event.getItem();
        if (stack.isEmpty() || stack.has(DataComponents.FOOD)) return; // food is handled separately
        if (FluidHelpers.getContainedFluid(stack).isEmpty() && !stack.has(
            com.terravera.common.TerraVeraDataComponents.WATER_TREATMENT.get())) return;

        // Restore stamina when drinking fluids from container
        com.terravera.common.temperature.TemperatureSystem.addStamina(player, 50f);

        if (!TerraVeraConfig.SERVER.enableWaterContamination.get()) return;

        final WaterTreatment treatment = WaterTreatment.get(stack);
        final float contamination = treatment.effectiveContamination();
        if (contamination <= 0f) return;

        drinkUntreated(player, contamination, WaterQuality.fromContamination(contamination));
    }

    /**
     * Records where a container's water came from, at the moment it is filled. Without this, treatment state has
     * nothing to attach to.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFillContainer(PlayerInteractEvent.RightClickBlock event)
    {
        if (!TerraVeraConfig.SERVER.enableWaterContamination.get()) return;

        final Level level = event.getLevel();
        if (level.isClientSide()) return;

        final ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        // Only care about things that can actually hold a fluid.
        if (stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM) == null) return;

        final BlockPos pos = event.getPos();
        final BlockState state = level.getBlockState(pos);
        final WaterSource source = WaterSource.evaluate(level, pos, state);
        if (source == null || source.salty()) return;

        // The fill itself is TFC's business; we run after it and stamp whatever ended up in the player's hand.
        final ItemStack result = event.getEntity().getItemInHand(event.getHand());
        if (!result.isEmpty() && !FluidHelpers.getContainedFluid(result).isEmpty())
        {
            WaterTreatment.fillFrom(result, source);
        }
    }

    /**
     * The shared "you just drank something questionable" path. Warns the player if the water was visibly bad, then
     * rolls for exposure. The warning is deliberately after the fact for a container and before the fact for a source
     * block, because in the first case they had a chance to look at the tooltip and in the second they did not.
     */
    private static void drinkUntreated(Player player, float contamination, WaterQuality quality)
    {
        if (contamination <= 0.001f) return;

        if (TerraVeraConfig.SERVER.warnBeforeDrinkingUnsafeWater.get() && quality.isRisky())
        {
            player.displayClientMessage(Component.translatable("terravera.water.risky",
                    Component.translatable(quality.translationKey()))
                .withStyle(style -> style.withColor(quality.color())), true);
        }

        IllnessTracker.expose(player, TransmissionVector.WATER, contamination);
    }

    @Nullable
    private static BlockHitResult getWaterHit(Level level, Player player, BlockPos clickedPos)
    {
        // RightClickBlock's position is authoritative on the server.  This is important for TFC fluids because the
        // interaction is often dispatched for a fluid face rather than the exact block selected by a client POV ray.
        if (!level.getFluidState(clickedPos).isEmpty())
        {
            return new BlockHitResult(Vec3.atCenterOf(clickedPos), Direction.UP, clickedPos, false);
        }

        // Keep the raytrace as a fallback for interactions routed through an adjacent block (for example a fluid face
        // with a container in hand).
        final BlockHitResult hit = net.minecraft.world.item.Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        return hit.getType() == HitResult.Type.BLOCK && !level.getFluidState(hit.getBlockPos()).isEmpty() ? hit : null;
    }

    // ----- Food ---------------------------------------------------------------------------------------------

    /**
     * Eating. Three separate things happen here, in order:
     * <ol>
     *     <li>rotten food and raw meat roll for their respective illnesses;</li>
     *     <li>filthy hands add a foodborne exposure on top, which is the mechanism that makes hygiene matter;</li>
     *     <li>if the player currently has malabsorption, the nutrition they just gained is partly taken back.</li>
     * </ol>
     */
    @SubscribeEvent
    public static void onFinishEating(LivingEntityUseItemEvent.Finish event)
    {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;

        final ItemStack stack = event.getItem();
        if (stack.isEmpty() || !stack.has(DataComponents.FOOD)) return;

        if (TerraVeraConfig.SERVER.enableFoodborneIllness.get())
        {
            float exposure = 0f;

            // Rotten food. TFC already applies hunger and poison for this; we add the possibility of something that
            // does not clear up in ninety seconds.
            final IFood food = FoodCapability.get(stack);
            if (food != null && food.isRotten())
            {
                exposure += 1.1f;
            }

            // Raw meat and fish. This is the tapeworm and trichinosis route, and it is why cooking exists.
            if (stack.is(TerraVeraHealthTags.Items.RISKY_RAW_MEAT)
                || stack.is(TFCTags.Items.RAW_MEATS)
                || stack.is(TFCTags.Items.RAW_FISH))
            {
                IllnessTracker.expose(player, TransmissionVector.UNDERCOOKED_MEAT, 1.0f);
                Hygiene.soil(player, Hygiene.SOIL_RAW_MEAT);
            }

            // Eating with filthy hands. Small on its own, but it is the difference between a clean camp and a
            // dysentery outbreak over a long game.
            final float hygiene = IllnessTracker.get(player).hygiene();
            if (hygiene < 0.5f)
            {
                exposure += Mth.clampedMap(hygiene, 0f, 0.5f, 0.7f, 0f);
            }

            if (exposure > 0f)
            {
                IllnessTracker.expose(player, TransmissionVector.FOOD, exposure);
            }
        }

        applyMalabsorption(player, stack);
    }

    /**
     * Takes back part of the nutrition just gained, if the player has a malabsorptive illness.
     * <p>
     * This is the symptom the request specifically asked for and it is the most interesting one mechanically, because
     * TFC ties max health to nutrition. A tapeworm does not hurt you; it quietly shrinks your health bar over a week
     * while you eat exactly as much as you always did.
     */
    private static void applyMalabsorption(Player player, ItemStack stack)
    {
        if (!player.hasEffect(com.terravera.common.health.effect.TerraVeraEffects.MALABSORPTION)) return;

        final var instance = player.getEffect(com.terravera.common.health.effect.TerraVeraEffects.MALABSORPTION);
        if (instance == null) return;

        final int amplifier = instance.getAmplifier();
        // 25% of the meal is lost at amplifier 0, up to 55% at amplifier 2.
        final float lost = Mth.clamp(0.25f + amplifier * 0.15f, 0f, 0.6f);

        final var foodData = player.getFoodData();
        final int hunger = foodData.getFoodLevel();
        final float saturation = foodData.getSaturationLevel();

        final int hungerLost = Math.round(hunger * lost * 0.25f);
        if (hungerLost > 0)
        {
            foodData.setFoodLevel(Math.max(0, hunger - hungerLost));
        }
        foodData.setSaturation(Math.max(0f, saturation * (1f - lost)));

        if (player.getRandom().nextFloat() < 0.3f)
        {
            player.displayClientMessage(Component.translatable("terravera.symptom.malabsorption.feedback")
                .withStyle(ChatFormatting.GRAY), true);
        }
    }

    // ----- Wounds -------------------------------------------------------------------------------------------

    /**
     * Wound infection and tetanus.
     * <p>
     * Tetanus is soil-borne, so the roll is gated on being hurt by something plausibly dirty - a fall onto the ground,
     * a thorn bush, a cactus, or any wound taken while filthy. It is a critical, lethal illness with a long incubation
     * period, which makes it the single most alarming thing in the mod and the strongest argument for washing.
     */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event)
    {
        if (!TerraVeraConfig.SERVER.enableWoundInfection.get()) return;
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;
        if (event.getNewDamage() < 2f) return; // scratches do not get infected

        final var source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_ARMOR) && source.is(DamageTypeTags.IS_FALL)) return;
        if (source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypeTags.IS_FIRE)) return;

        final PlayerHealth health = IllnessTracker.get(player);

        // A dirty wound is a function of how filthy you were and how deep the wound was.
        float exposure = Mth.clampedMap(health.hygiene(), 0f, 1f, 1.0f, 0.15f)
            * Mth.clampedMap(event.getNewDamage(), 2f, 12f, 0.5f, 1.4f);

        // Standing in filth when it happened makes it much worse - this is the soil contact tetanus needs.
        if (isStandingInFilth(player))
        {
            exposure *= 1.8f;
        }

        IllnessTracker.expose(player, TransmissionVector.WOUND, exposure);
        Hygiene.soil(player, Hygiene.SOIL_FILTH * 0.5f);
    }

    private static boolean isStandingInFilth(Player player)
    {
        final BlockState below = player.level().getBlockState(player.blockPosition().below());
        return below.is(TFCTags.Blocks.MUD)
            || below.is(TerraVeraHealthTags.Blocks.SOILS_PLAYER)
            || below.is(TerraVeraHealthTags.Blocks.FOULS_WATER);
    }

    // ----- Tick: progression, contagion, and grime ----------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event)
    {
        final Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        IllnessTracker.tick(player);

        if (player.tickCount % SLOW_TICK != 0) return;

        tickPassiveHygiene(player);
        tickContagion(player);
    }

    /**
     * Grime accumulates from simply living, and faster while standing in muck. Nothing dramatic - the point is that
     * hygiene is a resource you have to keep topping up, not a one-off purchase.
     */
    private static void tickPassiveHygiene(Player player)
    {
        if (!TerraVeraConfig.SERVER.enableHygiene.get()) return;

        float grime = Hygiene.SOIL_PASSIVE * 0.05f;
        if (isStandingInFilth(player)) grime += Hygiene.SOIL_FILTH * 0.1f;
        if (player.isSprinting()) grime *= 1.5f;

        // Being in clean water rinses you off slowly, which is a nice, discoverable interaction.
        if (player.isInWater() && !isStandingInFilth(player))
        {
            final PlayerHealth health = IllnessTracker.get(player);
            IllnessTracker.set(player, health.withHygiene(health.hygiene() + 0.01f));
            return;
        }

        Hygiene.soil(player, grime);
    }

    /**
     * Respiratory illness spreading by proximity. This is the only vector that does not require the player to do
     * anything at all, which is exactly right for a cold.
     */
    private static void tickContagion(Player player)
    {
        if (!TerraVeraConfig.SERVER.enableContagion.get()) return;

        final long now = Calendars.get(player.level()).getTicks();
        final List<Illness> shedding = IllnessTracker.shedding(player, now);
        if (shedding.isEmpty()) return;

        final double range = TerraVeraConfig.SERVER.contagionRange.get();
        final List<Player> nearby = player.level().getEntitiesOfClass(Player.class,
            player.getBoundingBox().inflate(range),
            other -> other != player && other.isAlive());

        for (Player other : nearby)
        {
            final PlayerHealth otherHealth = IllnessTracker.get(other);
            for (Illness illness : shedding)
            {
                final ResourceLocation id = Illness.idOf(illness);
                if (id == null || !otherHealth.isSusceptibleTo(id, now)) continue;

                // Closer is worse, and the roll only happens once every five seconds, so a passing encounter is
                // usually survivable and sharing a small hut for a week usually is not.
                final double distance = player.distanceTo(other);
                final float proximity = (float) Mth.clampedMap(distance, 0.5, range, 1.2, 0.15);
                IllnessTracker.expose(other, TransmissionVector.CONTACT, proximity);
            }
        }
    }

    // ----- Filth from work ----------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onBlockBroken(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event)
    {
        if (!TerraVeraConfig.SERVER.enableHygiene.get()) return;
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) return;

        final BlockState state = event.getState();
        if (state.is(TerraVeraHealthTags.Blocks.SOILS_PLAYER) || state.is(TFCTags.Blocks.MUD))
        {
            Hygiene.soil(event.getPlayer(), Hygiene.SOIL_FILTH * 0.35f);
        }
    }

    /** Butchering. TFC drops raw meat from animals; getting it is a filthy job. */
    @SubscribeEvent
    public static void onAttackEntity(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event)
    {
        if (!TerraVeraConfig.SERVER.enableHygiene.get()) return;

        final Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(event.getTarget() instanceof net.minecraft.world.entity.LivingEntity target)) return;
        // Only the killing blow makes a mess.
        if (target.getHealth() > player.getAttackStrengthScale(0.5f) * 8f) return;

        Hygiene.soil(player, Hygiene.SOIL_BUTCHERING * 0.4f);
    }

    // ----- Washing and treatment interactions ---------------------------------------------------------------

    /**
     * Washing your hands. Crouch and right-click clean water, or a washing station, with soap or an empty hand.
     * <p>
     * Deliberately a crouch interaction so that it never competes with drinking.
     */
    @SubscribeEvent
    public static void onWash(PlayerInteractEvent.RightClickBlock event)
    {
        if (!TerraVeraConfig.SERVER.enableHygiene.get()) return;

        final Player player = event.getEntity();
        final Level level = event.getLevel();
        if (level.isClientSide() || !player.isShiftKeyDown()) return;

        final BlockPos pos = event.getPos();
        final BlockState state = level.getBlockState(pos);
        final boolean isStation = state.is(TerraVeraHealthTags.Blocks.WASHING_STATION);
        final boolean isWater = !level.getFluidState(pos).isEmpty();
        if (!isStation && !isWater) return;

        // You cannot get clean in a swamp.
        if (isWater)
        {
            final WaterSource source = WaterSource.evaluate(level, pos, state);
            if (source != null && source.contamination() > 0.5f)
            {
                player.displayClientMessage(Component.translatable("terravera.hygiene.too_dirty_to_wash")
                    .withStyle(ChatFormatting.RED), true);
                return;
            }
        }

        final ItemStack held = event.getItemStack();
        final float quality = Hygiene.washQuality(held);
        if (quality <= 0f) return;

        if (Hygiene.wash(player, quality))
        {
            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 0.5f, 1.2f);
            if (!player.isCreative() && held.is(TerraVeraHealthTags.Items.SOAP))
            {
                held.shrink(1);
            }
            event.setCanceled(true);
        }
    }

    /**
     * Using a remedy. Any item tagged as a remedy is consumed on right-click if the player is ill.
     * <p>
     * Remedies that are also food (garlic, honey) are handled through eating instead - see
     * {@link #onRemedyEaten(LivingEntityUseItemEvent.Finish)} - so the player does not have to choose between eating
     * them and using them.
     */
    @SubscribeEvent
    public static void onUseRemedy(PlayerInteractEvent.RightClickItem event)
    {
        final Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        final ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || stack.has(DataComponents.FOOD)) return;
        if (!Remedy.isRemedy(stack)) return;

        if (IllnessTracker.treat(player, stack))
        {
            if (!player.isCreative()) stack.shrink(1);
            event.setCanceled(true);
        }
    }

    /** Remedies that happen to be food take effect when eaten. */
    @SubscribeEvent
    public static void onRemedyEaten(LivingEntityUseItemEvent.Finish event)
    {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;

        final ItemStack stack = event.getItem();
        if (stack.isEmpty() || !Remedy.isRemedy(stack)) return;

        IllnessTracker.treat(player, stack);
    }

    // ----- Death and respawn --------------------------------------------------------------------------------

    /**
     * Death clears the acute infections you were carrying but keeps your acquired immunity, via
     * {@link PlayerHealth#onDeath()}. The attachment itself is marked {@code copyOnDeath}, so this only has to apply
     * the clearing rule.
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event)
    {
        if (!event.isWasDeath()) return;

        final PlayerHealth old = IllnessTracker.get(event.getOriginal());
        IllnessTracker.set(event.getEntity(), old.onDeath());
    }

    // ----- Tooltips -----------------------------------------------------------------------------------------

    /**
     * Shows water quality and treatment state on containers, and what a remedy treats on remedies. The water tooltip
     * is the main teaching surface of the whole system - it is where a player learns that the jug they filled at the
     * pond is not the same as the jug they boiled.
     */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event)
    {
        final ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        if (TerraVeraConfig.SERVER.showWaterTooltip.get()
            && stack.has(com.terravera.common.TerraVeraDataComponents.WATER_TREATMENT.get()))
        {
            final WaterTreatment treatment = WaterTreatment.get(stack);
            event.getToolTip().add(treatment.describe());
            event.getToolTip().add(treatment.describeTreatment());
        }

        if (Remedy.isRemedy(stack))
        {
            event.getToolTip().add(Component.translatable("terravera.tooltip.remedy").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    private HealthEventHandler() {}
}
