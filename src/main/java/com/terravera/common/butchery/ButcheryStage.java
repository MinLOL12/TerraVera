/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.butchery;

import net.minecraft.network.chat.Component;

/**
 * The order a carcass is actually taken apart in.
 * <p>
 * The sequence is fixed because it is fixed in reality, and each step forecloses the one before it: you cannot skin
 * an animal you have already quartered, and if you separate the primals before drawing the guts you have opened the
 * stomach into the meat. Every stage is a separate interaction with its own tool requirement and its own products,
 * so a player who only wants meat can stop after {@link #PRIMALS} and leave the rest - at the cost of the fat,
 * bone, and sinew that the later systems need.
 */
public enum ButcheryStage
{
    /** Nothing done yet. The carcass has to be hung or laid out before a knife is any use. */
    INTACT("intact", 0f, false),
    /** Bled out and hung. Yields blood if the animal is still fresh. */
    BLED("bled", 0.6f, true),
    /** Hide off in one piece, if the knife work was clean. Yields hide. */
    SKINNED("skinned", 1.2f, true),
    /** Drawn: the offal is out before it can taint the muscle. Yields organs and stomach. */
    EVISCERATED("eviscerated", 1.6f, true),
    /** Broken into primal cuts along the natural seams. Yields shoulder, ribs, loin, and leg. */
    PRIMALS("primals", 2.0f, true),
    /** Everything else off the frame: bones, marrow bones, fat, suet, sinew, tendon. */
    RENDERED("rendered", 1.4f, true),
    /** Nothing left worth cutting. */
    STRIPPED("stripped", 0f, false);

    private final String id;
    private final float difficulty;
    private final boolean needsBlade;

    ButcheryStage(String id, float difficulty, boolean needsBlade)
    {
        this.id = id;
        this.difficulty = difficulty;
        this.needsBlade = needsBlade;
    }

    public String id() { return id; }

    /** Roughly how much can go wrong here. Skinning and drawing punish a bad knife more than bleeding does. */
    public float difficulty() { return difficulty; }

    public boolean needsBlade() { return needsBlade; }

    /** The stage that follows this one. {@link #STRIPPED} is terminal. */
    public ButcheryStage next()
    {
        return this == STRIPPED ? STRIPPED : values()[ordinal() + 1];
    }

    public boolean complete()
    {
        return this == STRIPPED;
    }

    public Component displayName()
    {
        return Component.translatable("terravera.butchery.stage." + id);
    }

    public static ButcheryStage byId(String id)
    {
        for (ButcheryStage stage : values())
        {
            if (stage.id.equals(id)) return stage;
        }
        return INTACT;
    }
}
