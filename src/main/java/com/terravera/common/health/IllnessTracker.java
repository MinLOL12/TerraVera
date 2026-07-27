/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.health;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.dries007.tfc.common.player.IPlayerInfo;
import net.dries007.tfc.util.calendar.Calendars;

import com.terravera.TerraVera;
import com.terravera.common.TerraVeraAttachments;
import com.terravera.common.health.effect.TerraVeraEffects;
import com.terravera.common.skill.SkillSystem;
import com.terravera.common.skill.SkillType;
import com.terravera.config.TerraVeraConfig;

/**
 * The engine of the disease system: exposure, incubation, symptoms, recovery, and treatment.
 * <p>
 * Everything funnels through here so that there is exactly one place where a player becomes ill and exactly one place
 * where illness state advances. The tick loop is deliberately cheap - it only does real work once a second, and only
 * for players who are actually carrying something.
 * <p>
 * The single most important behaviour in this class is that {@link #expose} never applies an effect. It creates an
 * {@link Infection} stamped with the current calendar tick and then walks away. Symptoms appear later, in
 * {@link #tick}, when the incubation period has elapsed. Nothing in TerraVera makes you ill the instant you drink.
 */
public final class IllnessTracker
{
    /** How often the tracker does real work, in ticks. Once a second is plenty for a system measured in days. */
    private static final int TICK_INTERVAL = 20;

    // ----- Exposure -----------------------------------------------------------------------------------------

    /**
     * Rolls for infection from a single exposure event.
     * <p>
     * {@code exposureStrength} scales the base chance and is where the interesting design lives - it is the
     * contamination of the water you drank, or how rotten the food was, or how filthy you were when you took the
     * wound. A clean exposure is a small number and usually does nothing.
     *
     * @param player           the player being exposed
     * @param vector           how they were exposed
     * @param exposureStrength a multiplier on the base chance, typically in {@code [0, 2]}
     * @return the illness contracted, or {@code null} if the player got away with it
     */
    @Nullable
    public static Illness expose(Player player, TransmissionVector vector, float exposureStrength)
    {
        if (player.level().isClientSide() || player.isCreative() || player.isSpectator()) return null;
        if (!TerraVeraConfig.SERVER.enableDisease.get()) return null;
        if (exposureStrength <= 0f) return null;

        final List<Illness> candidates = Illness.byVector(vector);
        if (candidates.isEmpty()) return null;

        final long now = Calendars.get(player.level()).getTicks();
        final PlayerHealth health = get(player);
        final RandomSource random = player.getRandom();
        final float globalScale = TerraVeraConfig.SERVER.diseaseChanceMultiplier.get().floatValue();

        // Hygiene matters for anything you put in your mouth or let into a wound, but not for the quality of the
        // water itself - clean hands do not clean a swamp.
        final float hygieneScale = vector == TransmissionVector.WATER ? 1f : health.hygieneRiskMultiplier();

        // Being poorly nourished makes you easier to infect. This is the loop that makes disease compound: you get
        // ill, you absorb less, you get more ill.
        final float resistance = resistanceMultiplier(player);

        for (Illness illness : candidates)
        {
            final ResourceLocation id = Illness.idOf(illness);
            if (id == null || !health.isSusceptibleTo(id, now)) continue;

            final float chance = illness.baseChance() * exposureStrength * globalScale * hygieneScale * resistance;
            if (random.nextFloat() < chance)
            {
                // Severity of the eventual illness is worse if you were run down when you caught it.
                final float severityScale = Mth.clamp(0.7f + (resistance - 1f) * 0.5f, 0.4f, 1.4f);
                set(player, get(player).withInfection(Infection.contract(id, now, severityScale)));
                TerraVera.LOGGER.debug("{} was exposed to {} via {}", player.getGameProfile().getName(), id, vector);
                return illness;
            }
        }
        return null;
    }

    /**
     * How much more (or less) likely this player is to catch something, from their nutrition. TFC already ties max
     * health to nutrition; this ties infection resistance to it as well, which makes eating a varied diet a defence
     * against disease rather than only a defence against having a small health bar.
     */
    private static float resistanceMultiplier(Player player)
    {
        final float nutrition = IPlayerInfo.get(player).nutrition().getAverageNutrition();
        // Well-fed: 0.7x risk. Starving: 1.5x risk.
        return Mth.clampedMap(nutrition, 0.1f, 0.85f, 1.5f, 0.7f);
    }

    // ----- Per-tick progression -----------------------------------------------------------------------------

    /**
     * Advances every infection the player is carrying by one tick: promotes incubating infections to symptomatic when
     * their time comes, refreshes symptom effects, and clears infections that have run their course.
     */
    public static void tick(Player player)
    {
        if (player.level().isClientSide()) return;
        if (player.tickCount % TICK_INTERVAL != 0) return;

        PlayerHealth health = get(player);
        if (health.isHealthy())
        {
            // Even a healthy player accumulates grime, and lapsed immunities should be tidied up occasionally.
            if (player.tickCount % 1200 == 0)
            {
                final PlayerHealth pruned = health.prunedImmunities(Calendars.get(player.level()).getTicks());
                if (pruned != health) set(player, pruned);
            }
            return;
        }

        final long now = Calendars.get(player.level()).getTicks();
        final List<Infection> surviving = new ArrayList<>(health.infections().size());
        boolean changed = false;

        for (Infection infection : health.infections())
        {
            final Illness illness = Illness.get(infection.illnessId());
            if (illness == null)
            {
                // The illness was removed from the datapack under us. Drop the infection rather than crash.
                changed = true;
                continue;
            }

            switch (infection.stage(illness, now))
            {
                case INCUBATING ->
                {
                    // Nothing happens. This is the whole point.
                    surviving.add(infection);
                }
                case SYMPTOMATIC ->
                {
                    Infection current = infection;
                    if (!current.acknowledged())
                    {
                        announceOnset(player, illness);
                        current = current.acknowledge();
                        changed = true;
                    }
                    applySymptoms(player, illness, current, now);
                    surviving.add(current);
                }
                case RESOLVED ->
                {
                    recover(player, illness, infection, now);
                    changed = true;
                    if (illness.immunityTicks() > 0)
                    {
                        health = health.withImmunity(infection.illnessId(), now + illness.immunityTicks());
                    }
                }
            }
        }

        if (changed || surviving.size() != health.infections().size())
        {
            set(player, health.withInfections(surviving));
        }
    }

    /**
     * Applies (or refreshes) the mob effects for one symptomatic illness. Effects are given a short duration and
     * re-applied every tick interval, so they exactly track the infection and vanish the moment it resolves.
     */
    private static void applySymptoms(Player player, Illness illness, Infection infection, long now)
    {
        final float intensity = infection.intensity(illness, now);
        if (intensity <= 0.05f) return;

        // Amplifier comes from how severe the disease is and how far into it we are.
        final int amplifier = Mth.clamp(illness.severity().amplifierBonus() + (intensity > 0.9f ? 1 : 0), 0, 3);
        final int duration = TICK_INTERVAL * 4; // comfortably longer than the refresh interval

        for (Symptom symptom : illness.symptoms())
        {
            player.addEffect(new MobEffectInstance(
                TerraVeraEffects.holder(symptom), duration, amplifier, false, false, true));
        }

        // A lethal illness at full intensity, left entirely untreated, will eventually kill. This only bites at the
        // peak of a critical illness the player has done nothing about.
        if (illness.lethal() && intensity >= 1.0f
            && illness.severity() == Illness.Severity.CRITICAL
            && player.getRandom().nextFloat() < 0.02f
            && player.getHealth() > 1f)
        {
            player.hurt(player.damageSources().magic(), 1.0f);
        }
    }

    private static void announceOnset(Player player, Illness illness)
    {
        final ResourceLocation id = Illness.idOf(illness);
        if (id == null) return;
        player.displayClientMessage(Component.translatable("terravera.illness.onset",
                Component.translatable(nameKey(id)).withStyle(style -> style.withColor(illness.severity().color())))
            .withStyle(ChatFormatting.ITALIC), false);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.4f, 0.8f);
    }

    private static void recover(Player player, Illness illness, Infection infection, long now)
    {
        player.displayClientMessage(Component.translatable("terravera.illness.recovered",
                Component.translatable(nameKey(infection.illnessId())))
            .withStyle(ChatFormatting.GREEN), false);
    }

    // ----- Treatment ----------------------------------------------------------------------------------------

    /**
     * Applies {@code stack} as a remedy to whatever the player is currently carrying.
     *
     * @return {@code true} if the item did something, and should therefore be consumed
     */
    public static boolean treat(Player player, ItemStack stack)
    {
        if (player.level().isClientSide()) return false;

        PlayerHealth health = get(player);
        if (health.isHealthy()) return false;

        final long now = Calendars.get(player.level()).getTicks();
        final List<Infection> next = new ArrayList<>(health.infections().size());
        boolean treated = false;

        for (Infection infection : health.infections())
        {
            final Illness illness = Illness.get(infection.illnessId());
            if (illness == null)
            {
                continue;
            }

            final Remedy remedy = Remedy.find(stack, infection.illnessId(), illness);
            if (remedy == null)
            {
                next.add(infection);
                continue;
            }

            treated = true;
            if (remedy.cures())
            {
                // Cured outright. Clear the symptoms immediately rather than waiting for the next tick.
                clearEffects(player, illness);
                if (illness.immunityTicks() > 0)
                {
                    health = health.withImmunity(infection.illnessId(), now + illness.immunityTicks());
                }
                player.displayClientMessage(Component.translatable("terravera.illness.cured",
                    Component.translatable(nameKey(infection.illnessId()))).withStyle(ChatFormatting.GREEN), true);
            }
            else
            {
                // Diagnosis and dosing improve gradually with experience, but a novice's correctly matched remedy
                // still works. This makes medicine learned knowledge, not a hard level gate on treatment.
                final float knowledge = SkillSystem.proficiency(player, SkillType.MEDICINE);
                final int shortened = Math.round(remedy.shortenTicks() * (1f + knowledge * 0.20f));
                final float relief = Math.min(0.95f, remedy.severityReduction() * (1f + knowledge * 0.15f));
                next.add(infection.treated(shortened, relief));
                player.displayClientMessage(Component.translatable("terravera.illness.treated",
                    Component.translatable(nameKey(infection.illnessId()))).withStyle(ChatFormatting.YELLOW), true);
            }
        }

        if (treated)
        {
            set(player, health.withInfections(next));
            SkillSystem.award(player, SkillType.MEDICINE, 1.25f);
        }
        return treated;
    }

    private static void clearEffects(Player player, Illness illness)
    {
        for (Symptom symptom : illness.symptoms())
        {
            player.removeEffect(TerraVeraEffects.holder(symptom));
        }
    }

    // ----- Contagion ----------------------------------------------------------------------------------------

    /**
     * @return every contagious illness this player is currently shedding. A player is infectious during the
     * symptomatic phase and, for respiratory illness, for the tail end of incubation - which is exactly why colds
     * spread through a base before anyone knows anyone is ill.
     */
    public static List<Illness> shedding(Player player, long now)
    {
        final PlayerHealth health = get(player);
        if (health.isHealthy()) return List.of();

        final List<Illness> result = new ArrayList<>(2);
        for (Infection infection : health.infections())
        {
            final Illness illness = Illness.get(infection.illnessId());
            if (illness == null || !illness.contagious()) continue;

            final Infection.Stage stage = infection.stage(illness, now);
            if (stage == Infection.Stage.SYMPTOMATIC)
            {
                result.add(illness);
            }
            else if (stage == Infection.Stage.INCUBATING)
            {
                // Shedding starts in the last quarter of incubation.
                final long elapsed = now - infection.contractedTick() + infection.treatmentTicks();
                if (elapsed > illness.incubationTicks() * 0.75f) result.add(illness);
            }
        }
        return result;
    }

    // ----- Accessors ----------------------------------------------------------------------------------------

    public static PlayerHealth get(Player player)
    {
        return player.getData(TerraVeraAttachments.PLAYER_HEALTH);
    }

    public static void set(Player player, PlayerHealth health)
    {
        player.setData(TerraVeraAttachments.PLAYER_HEALTH, health);
    }

    /** The translation key for an illness' display name, derived from its id. */
    public static String nameKey(ResourceLocation id)
    {
        return "terravera.illness." + id.getNamespace() + "." + id.getPath();
    }

    private IllnessTracker() {}
}
