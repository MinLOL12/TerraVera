/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.health.effect;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.terravera.TerraVera;
import com.terravera.common.health.Symptom;

/**
 * One registered mob effect per {@link Symptom}.
 * <p>
 * Symptoms are surfaced as ordinary status effects on purpose. The player already knows how to read that UI, it shows
 * them how long they have left, and it means an illness is legible at a glance without a bespoke HUD. What is
 * <em>not</em> ordinary is where they come from: nothing in this mod hands out a symptom directly. They are applied
 * only by {@link com.terravera.common.health.IllnessTracker} while an infection is in its symptomatic phase, and they
 * are refreshed every tick from that infection, so they cannot be milked, cured with milk, or outlasted.
 * <p>
 * Attribute-driven symptoms have their modifiers declared here. Symptoms that need to reach into TerraFirmaCraft's
 * hunger and thirst are implemented in {@link SymptomEffect}.
 */
public final class TerraVeraEffects
{
    public static final DeferredRegister<MobEffect> EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, TerraVera.MOD_ID);

    /** Malaise: everything is harder and slower. */
    public static final DeferredHolder<MobEffect, MobEffect> FATIGUE = register("fatigue", Symptom.FATIGUE, 0x7A6A4F,
        effect -> effect
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, TerraVera.identifier("effect.fatigue.move"), -0.10D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.ATTACK_DAMAGE, TerraVera.identifier("effect.fatigue.damage"), -0.15D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** Raised core temperature. Burns water and calories, and at high amplifier does harm. */
    public static final DeferredHolder<MobEffect, MobEffect> FEVER = register("fever", Symptom.FEVER, 0xC9502E,
        effect -> effect
            .addAttributeModifier(Attributes.ATTACK_DAMAGE, TerraVera.identifier("effect.fever.damage"), -0.20D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** Shivering. Slows you down. */
    public static final DeferredHolder<MobEffect, MobEffect> CHILLS = register("chills", Symptom.CHILLS, 0x8FA8C4,
        effect -> effect
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, TerraVera.identifier("effect.chills.move"), -0.15D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** Your gut has stopped taking up what you eat. */
    public static final DeferredHolder<MobEffect, MobEffect> MALABSORPTION = register("malabsorption", Symptom.MALABSORPTION, 0x9E8455,
        effect -> effect);

    /** Compensatory hunger - something is eating before you do. */
    public static final DeferredHolder<MobEffect, MobEffect> INCREASED_HUNGER = register("increased_hunger", Symptom.INCREASED_HUNGER, 0xB08A3E,
        effect -> effect);

    /** Fluid loss. */
    public static final DeferredHolder<MobEffect, MobEffect> DEHYDRATION = register("dehydration", Symptom.DEHYDRATION, 0xD4C08A,
        effect -> effect
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, TerraVera.identifier("effect.dehydration.move"), -0.12D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** Queasiness. */
    public static final DeferredHolder<MobEffect, MobEffect> NAUSEA = register("nausea", Symptom.NAUSEA, 0x6F8B4A,
        effect -> effect);

    /** Aching, stiff muscles. */
    public static final DeferredHolder<MobEffect, MobEffect> MUSCLE_PAIN = register("muscle_pain", Symptom.MUSCLE_PAIN, 0x8B5E5E,
        effect -> effect
            .addAttributeModifier(Attributes.ATTACK_DAMAGE, TerraVera.identifier("effect.muscle_pain.damage"), -0.25D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.ATTACK_SPEED, TerraVera.identifier("effect.muscle_pain.speed"), -0.15D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** Coughing and breathlessness. */
    public static final DeferredHolder<MobEffect, MobEffect> COUGH = register("cough", Symptom.COUGH, 0xA5A5A5,
        effect -> effect
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, TerraVera.identifier("effect.cough.move"), -0.06D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** Tetanic spasms. The most dangerous symptom in the mod. */
    public static final DeferredHolder<MobEffect, MobEffect> SPASMS = register("spasms", Symptom.SPASMS, 0x6B3B6B,
        effect -> effect
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, TerraVera.identifier("effect.spasms.move"), -0.30D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.ATTACK_SPEED, TerraVera.identifier("effect.spasms.attack_speed"), -0.35D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    private static final Map<Symptom, DeferredHolder<MobEffect, MobEffect>> BY_SYMPTOM = new EnumMap<>(Symptom.class);

    static
    {
        BY_SYMPTOM.put(Symptom.FATIGUE, FATIGUE);
        BY_SYMPTOM.put(Symptom.FEVER, FEVER);
        BY_SYMPTOM.put(Symptom.CHILLS, CHILLS);
        BY_SYMPTOM.put(Symptom.MALABSORPTION, MALABSORPTION);
        BY_SYMPTOM.put(Symptom.INCREASED_HUNGER, INCREASED_HUNGER);
        BY_SYMPTOM.put(Symptom.DEHYDRATION, DEHYDRATION);
        BY_SYMPTOM.put(Symptom.NAUSEA, NAUSEA);
        BY_SYMPTOM.put(Symptom.MUSCLE_PAIN, MUSCLE_PAIN);
        BY_SYMPTOM.put(Symptom.COUGH, COUGH);
        BY_SYMPTOM.put(Symptom.SPASMS, SPASMS);
    }

    /** @return the registered effect holder for a symptom. Every symptom has one. */
    public static Holder<MobEffect> holder(Symptom symptom)
    {
        final DeferredHolder<MobEffect, MobEffect> holder = BY_SYMPTOM.get(symptom);
        if (holder == null) throw new IllegalStateException("No effect registered for symptom " + symptom);
        return holder;
    }

    public static Supplier<MobEffect> effect(Symptom symptom)
    {
        return BY_SYMPTOM.get(symptom);
    }

    private static DeferredHolder<MobEffect, MobEffect> register(
        String name, Symptom symptom, int color, java.util.function.UnaryOperator<MobEffect> configure)
    {
        return EFFECTS.register(name, () -> configure.apply(new SymptomEffect(MobEffectCategory.HARMFUL, color, symptom)));
    }

    private TerraVeraEffects() {}
}
