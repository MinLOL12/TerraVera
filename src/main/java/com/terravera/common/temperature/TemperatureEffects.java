/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.temperature;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import com.terravera.common.health.Symptom;
import com.terravera.common.health.effect.TerraVeraEffects;

/**
 * Turns a thermal band into things the player can feel.
 * <p>
 * The escalation is deliberately slow and legible, and mirrors what actually happens to a cooling or overheating
 * human being. Discomfort first, then loss of fine control, then loss of coordination, and only at the extremes any
 * direct harm:
 *
 * <table>
 *     <tr><th>Band</th><th>What the player experiences</th></tr>
 *     <tr><td>Slightly chilled</td><td>Nothing mechanical at all. A message, and that is it.</td></tr>
 *     <tr><td>Hands stiff</td><td>Slower mining and attack speed. Fiddly work suffers first, as it does in life.</td></tr>
 *     <tr><td>Struggling to stay warm</td><td>Slower movement, weaker blows, real impairment.</td></tr>
 *     <tr><td>Hypothermia</td><td>Severe impairment and slow harm. Only after minutes of ignored warnings.</td></tr>
 *     <tr><td>Sweating</td><td>Nothing mechanical. You are just thirsty.</td></tr>
 *     <tr><td>Overheated</td><td>Fatigue, weaker blows.</td></tr>
 *     <tr><td>Heat exhaustion</td><td>Severe fatigue and slowness.</td></tr>
 *     <tr><td>Heat stroke</td><td>Blurred vision, severe impairment, slow harm.</td></tr>
 * </table>
 *
 * Effects are given short durations and refreshed every interval, so they track the body's state exactly and clear
 * the moment the player warms up or cools down. Nothing here can be waited out or drunk away.
 */
public final class TemperatureEffects
{
    /** Slightly longer than the tick interval, so the effect never flickers between refreshes. */
    private static final int DURATION = TemperatureSystem.TICK_INTERVAL * 3;

    private TemperatureEffects() {}

    public static void apply(Player player, ThermalModel.Band band)
    {
        switch (band)
        {
            case MILD_COLD, MILD_HEAT, COMFORTABLE ->
            {
                // Nothing. Being a bit chilly or a bit warm is a message, not a debuff. This band exists precisely so
                // the player gets told before anything is taken away from them.
            }
            case MODERATE_COLD ->
            {
                // Stiff hands: fine motor work goes first. Mining and swing speed, not movement.
                add(player, MobEffects.DIG_SLOWDOWN, 0);
                add(player, TerraVeraEffects.holder(Symptom.CHILLS), 0);
            }
            case SEVERE_COLD ->
            {
                add(player, MobEffects.DIG_SLOWDOWN, 1);
                add(player, MobEffects.MOVEMENT_SLOWDOWN, 0);
                add(player, MobEffects.WEAKNESS, 0);
                add(player, TerraVeraEffects.holder(Symptom.CHILLS), 1);
            }
            case HYPOTHERMIA ->
            {
                add(player, MobEffects.DIG_SLOWDOWN, 2);
                add(player, MobEffects.MOVEMENT_SLOWDOWN, 1);
                add(player, MobEffects.WEAKNESS, 1);
                add(player, MobEffects.CONFUSION, 0);
                add(player, TerraVeraEffects.holder(Symptom.CHILLS), 2);
                harm(player);
            }
            case MODERATE_HEAT ->
            {
                add(player, TerraVeraEffects.holder(Symptom.FATIGUE), 0);
                add(player, MobEffects.WEAKNESS, 0);
            }
            case SEVERE_HEAT ->
            {
                add(player, TerraVeraEffects.holder(Symptom.FATIGUE), 1);
                add(player, MobEffects.WEAKNESS, 1);
                add(player, MobEffects.MOVEMENT_SLOWDOWN, 0);
            }
            case HEAT_STROKE ->
            {
                add(player, TerraVeraEffects.holder(Symptom.FATIGUE), 2);
                add(player, MobEffects.WEAKNESS, 1);
                add(player, MobEffects.MOVEMENT_SLOWDOWN, 1);
                add(player, MobEffects.CONFUSION, 0);
                harm(player);
            }
        }
    }

    private static void add(Player player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier)
    {
        // Ambient and invisible: this is the player's own body, not something that was done to them, so it does not
        // deserve a screen full of potion swirls.
        player.addEffect(new MobEffectInstance(effect, DURATION, amplifier, true, false, true));
    }

    /**
     * The only place temperature deals damage, and it is slow, capped, and cannot kill outright from full health in
     * one step. A player at this point has had several minutes of escalating symptoms telling them to act.
     */
    private static void harm(Player player)
    {
        if (!com.terravera.config.TerraVeraConfig.SERVER.enableTemperatureDamage.get()) return;
        if (player.tickCount % 100 != 0) return;
        if (player.getHealth() <= 2f) return;
        player.hurt(player.damageSources().generic(), 1f);
    }
}
