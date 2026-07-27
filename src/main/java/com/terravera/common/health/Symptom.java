/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.health;

import java.util.Locale;
import javax.annotation.Nullable;

/**
 * What an illness actually does to you once it becomes symptomatic.
 * <p>
 * These are deliberately mundane. Real infectious disease does not, for the most part, kill you outright - it makes you
 * tired, it makes you thirsty, it stops you getting anything out of the food you eat, and it does that for days on end
 * while you still have to run a farm. That is the design target: a disease should be an economic problem before it is a
 * combat problem.
 * <p>
 * This enum is intentionally free of any Minecraft types so that the progression logic around it can be unit tested.
 * The mapping onto actual mob effects lives in {@link SymptomEffects}.
 */
public enum Symptom
{
    /**
     * Malaise. Drains stamina/food faster and slows work. The most common symptom in the mod, because it is the most
     * common symptom in life.
     */
    FATIGUE,
    /**
     * A raised core temperature: burns through water, makes you weak, and at high amplifier does periodic harm.
     */
    FEVER,
    /**
     * Shivering. Slows movement.
     */
    CHILLS,
    /**
     * Your gut stops taking up what you eat. Nutrition (and therefore max health, via TFC's nutrition-health link)
     * decays even though you are eating normally. The signature symptom of the parasites.
     */
    MALABSORPTION,
    /**
     * Compensatory hunger: a tapeworm eats before you do, so you are hungry again far sooner.
     */
    INCREASED_HUNGER,
    /**
     * Fluid loss. The thing that actually kills people in a cholera outbreak.
     */
    DEHYDRATION,
    /**
     * Queasiness. Blurred vision, and an occasional chance of losing a meal outright.
     */
    NAUSEA,
    /**
     * Muscle pain and stiffness. Reduces the damage you deal and how fast you swing.
     */
    MUSCLE_PAIN,
    /**
     * Respiratory involvement - coughing, breathlessness. Reduces stamina and slows you when you exert yourself.
     */
    COUGH,
    /**
     * Rigid, spasming muscles. Tetanus. Severe, and the only symptom that is outright dangerous on its own.
     */
    SPASMS;

    private final String id = name().toLowerCase(Locale.ROOT);

    public String id()
    {
        return id;
    }

    /** The translation key used for this symptom in tooltips and the health screen. */
    public String translationKey()
    {
        return "terravera.symptom." + id;
    }

    @Nullable
    public static Symptom byId(String id)
    {
        for (Symptom symptom : values())
        {
            if (symptom.id.equals(id)) return symptom;
        }
        return null;
    }
}
