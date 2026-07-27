/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.smithing;

import java.util.Locale;
import net.minecraft.network.chat.Component;

/**
 * The individual actions a player can repeat on a hot tool. These are not instant recipes; each strike changes the
 * state of the metal a little and restores only part of the tool's usable edge or body.
 */
public enum SmithingOperation
{
    DRAWING("drawing", 0.08f, 7, 2, 0, -1, 0, 1, 1,
        "lengthens the hot metal, thinning it as the edge is drawn out"),
    UPSETTING("upsetting", 0.10f, 9, -1, 0, 2, 0, 0, 1,
        "shortens and thickens the end, putting mass back behind a mushroomed or worn face"),
    FLATTENING("flattening", 0.09f, 8, 0, 2, -1, 0, 1, 1,
        "spreads high spots wider and flatter while sharpening broad edges"),
    STRAIGHTENING("straightening", 0.06f, 10, 0, 0, 0, 0, 0, -2,
        "takes bends out with light corrective blows"),
    BENDING("bending", 0.05f, 4, 0, 0, 0, 2, 0, 1,
        "sets or corrects a curve; useful, but it adds stress if overdone"),
    CONTROLLED_STRIKE("controlled_strike", 0.07f, 12, 0, 0, 0, 0, 1, -1,
        "lands a measured hammer blow that closes cracks and trues the working shape"),
    FORGE_WELD("forge_weld", 0.00f, 14, 0, 0, 0, 0, 0, -2,
        "joins separate hot metal with flux; this is the only operation that consumes flux");

    private static final SmithingOperation[] VALUES = values();

    private final String id;
    private final float massLoss;
    private final int repairPercent;
    private final int lengthChange;
    private final int widthChange;
    private final int thicknessChange;
    private final int bendChange;
    private final int edgeChange;
    private final int strainChange;
    private final String description;

    SmithingOperation(String id, float massLoss, int repairPercent, int lengthChange, int widthChange,
        int thicknessChange, int bendChange, int edgeChange, int strainChange, String description)
    {
        this.id = id;
        this.massLoss = massLoss;
        this.repairPercent = repairPercent;
        this.lengthChange = lengthChange;
        this.widthChange = widthChange;
        this.thicknessChange = thicknessChange;
        this.bendChange = bendChange;
        this.edgeChange = edgeChange;
        this.strainChange = strainChange;
        this.description = description;
    }

    public String id()
    {
        return id;
    }

    public float massLoss()
    {
        return massLoss;
    }

    public int repairPercent()
    {
        return repairPercent;
    }

    public int lengthChange()
    {
        return lengthChange;
    }

    public int widthChange()
    {
        return widthChange;
    }

    public int thicknessChange()
    {
        return thicknessChange;
    }

    public int bendChange()
    {
        return bendChange;
    }

    public int edgeChange()
    {
        return edgeChange;
    }

    public int strainChange()
    {
        return strainChange;
    }

    public String description()
    {
        return description;
    }

    public Component displayName()
    {
        return Component.translatable("terravera.smithing.operation." + id);
    }

    public SmithingOperation next()
    {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public static SmithingOperation byId(String id)
    {
        final String normalized = id == null ? "" : id.toLowerCase(Locale.ROOT);
        for (SmithingOperation operation : VALUES)
        {
            if (operation.id.equals(normalized)) return operation;
        }
        return CONTROLLED_STRIKE;
    }
}
