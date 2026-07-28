/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.butchery;

import net.minecraft.network.chat.Component;

/**
 * How long a carcass has been dead, and what that has done to it.
 * <p>
 * The bands are not arbitrary flavour. A freshly killed animal still holds usable blood and intact organs; a few
 * hours later the blood has settled and the offal is the first thing to turn; after a day the muscle is still good
 * (this is hanging, and it genuinely improves the meat) but the offal is gone; past that, bacterial spoilage takes
 * the meat too. Hide and bone survive far longer than anything edible, which is why the last band still gives you
 * something rather than nothing.
 */
public enum Freshness
{
    /** Just killed. Everything is recoverable, including blood and the delicate organs. */
    FRESH("fresh", 1.00f, 1.00f, 1.00f),
    /** Cooled and set. The best state to actually work in: the meat firms up and cuts cleanly. */
    COOL("cool", 0.98f, 0.85f, 1.00f),
    /** Hung. Muscle is improving, offal is going, blood is no longer worth collecting. */
    AGING("aging", 0.92f, 0.35f, 1.00f),
    /** Turning. Meat is still salvageable if you cook or salt it now. Organs are lost. */
    SPOILING("spoiling", 0.55f, 0.00f, 0.95f),
    /** Gone. Only the hide, the bone, and the sinew are worth the knife work. */
    ROTTEN("rotten", 0.00f, 0.00f, 0.80f);

    /** In-game hours a carcass spends in each band at a neutral ~15 C. */
    private static final float[] HOURS_AT_NEUTRAL = { 2f, 8f, 24f, 48f };

    private final String id;
    private final float meatYield;
    private final float organYield;
    private final float durableYield;

    Freshness(String id, float meatYield, float organYield, float durableYield)
    {
        this.id = id;
        this.meatYield = meatYield;
        this.organYield = organYield;
        this.durableYield = durableYield;
    }

    public String id() { return id; }

    /** Multiplier on recovered meat cuts, fat, and suet. */
    public float meatYield() { return meatYield; }

    /** Multiplier on recovered organs and blood - the first things to be lost. */
    public float organYield() { return organYield; }

    /** Multiplier on hide, bone, sinew, and tendon, which outlast the edible parts by a long way. */
    public float durableYield() { return durableYield; }

    public boolean edible() { return meatYield > 0f; }

    public Component displayName()
    {
        return Component.translatable("terravera.freshness." + id);
    }

    /**
     * Resolve the band from elapsed in-game hours and the ambient temperature the carcass has been sitting in.
     * <p>
     * Temperature is the whole point of the mechanic: TFC winters let you leave a deer hanging for days, and a hot
     * summer afternoon gives you until roughly the evening. The scaling is a crude Q10-style rule - spoilage roughly
     * doubles in rate for every 10 C above the reference - clamped so that a freezing carcass still eventually turns
     * and a desert one does not vanish instantly.
     */
    public static Freshness of(float hoursDead, float ambientC)
    {
        final float effectiveHours = hoursDead * spoilageRate(ambientC);
        float cumulative = 0f;
        final Freshness[] bands = values();
        for (int i = 0; i < HOURS_AT_NEUTRAL.length; i++)
        {
            cumulative += HOURS_AT_NEUTRAL[i];
            if (effectiveHours < cumulative) return bands[i];
        }
        return ROTTEN;
    }

    /** How much faster than the 15 C reference a carcass spoils at the given temperature. */
    public static float spoilageRate(float ambientC)
    {
        final float rate = (float) Math.pow(2.0, (ambientC - 15.0f) / 10.0f);
        return Math.max(0.12f, Math.min(8.0f, rate));
    }

    public static Freshness byId(String id)
    {
        for (Freshness freshness : values())
        {
            if (freshness.id.equals(id)) return freshness;
        }
        return FRESH;
    }
}
