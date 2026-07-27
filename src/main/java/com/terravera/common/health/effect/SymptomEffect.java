/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.health.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.dries007.tfc.common.player.IPlayerInfo;
import net.dries007.tfc.common.player.PlayerInfo;

import com.terravera.common.health.Symptom;

/**
 * The per-tick behaviour of a symptom that TFC's own systems have to be reached into for.
 * <p>
 * Most symptoms are plain attribute modifiers and are registered as such in {@link TerraVeraEffects}. The ones handled
 * here are the ones that have to touch TerraFirmaCraft's hunger, thirst, and nutrition model directly, because those
 * are exactly the systems TFC replaced and where the interesting gameplay lives:
 * <ul>
 *     <li><strong>Fever</strong> burns water, the way a real fever does.</li>
 *     <li><strong>Dehydration</strong> strips thirst hard - the actual cause of death in cholera.</li>
 *     <li><strong>Increased hunger</strong> makes you burn food faster, because something else is eating it.</li>
 *     <li><strong>Malabsorption</strong> is handled at the point of eating (see the event handler), because that is
 *     where nutrition is granted; here it just adds a small ongoing exhaustion cost.</li>
 * </ul>
 */
public class SymptomEffect extends MobEffect
{
    private final Symptom symptom;

    public SymptomEffect(MobEffectCategory category, int color, Symptom symptom)
    {
        super(category, color);
        this.symptom = symptom;
    }

    public Symptom symptom()
    {
        return symptom;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier)
    {
        if (!(entity instanceof Player player) || player.level().isClientSide())
        {
            return true;
        }

        final IPlayerInfo info = IPlayerInfo.get(player);
        final int level = amplifier + 1;

        switch (symptom)
        {
            case FEVER ->
            {
                // A fever costs you water and calories. At high amplifier it also does slow direct harm.
                info.addThirst(-0.35f * level);
                player.causeFoodExhaustion(PlayerInfo.PASSIVE_EXHAUSTION_PER_TICK * 20 * 0.6f * level);
                if (amplifier >= 2 && player.getHealth() > 4f)
                {
                    player.hurt(player.damageSources().magic(), 0.5f);
                }
            }
            case DEHYDRATION ->
            {
                // Fluid loss, fast. This is what makes cholera an emergency rather than an inconvenience.
                info.addThirst(-1.1f * level);
            }
            case INCREASED_HUNGER ->
            {
                // Something in you is eating first.
                player.causeFoodExhaustion(PlayerInfo.PASSIVE_EXHAUSTION_PER_TICK * 20 * 1.2f * level);
            }
            case MALABSORPTION ->
            {
                // The absorption penalty itself is applied when food is eaten. This is the background cost of
                // running on a deficit.
                player.causeFoodExhaustion(PlayerInfo.PASSIVE_EXHAUSTION_PER_TICK * 20 * 0.45f * level);
            }
            case COUGH ->
            {
                // Respiratory effort. Cheap, but constant.
                player.causeFoodExhaustion(PlayerInfo.PASSIVE_EXHAUSTION_PER_TICK * 20 * 0.3f * level);
            }
            case SPASMS ->
            {
                // Tetanus. Rigid, painful, and genuinely dangerous.
                player.causeFoodExhaustion(PlayerInfo.PASSIVE_EXHAUSTION_PER_TICK * 20 * 0.8f * level);
                if (player.getHealth() > 2f)
                {
                    player.hurt(player.damageSources().magic(), 0.5f * level);
                }
            }
            default ->
            {
                // FATIGUE, CHILLS, NAUSEA and MUSCLE_PAIN are pure attribute modifiers, applied by the effect
                // registration itself.
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier)
    {
        // Once every two seconds at amplifier 0, faster as it gets worse. Matches the cadence TFC uses for thirst.
        final int period = Math.max(10, 40 >> amplifier);
        return duration % period == 0;
    }
}
