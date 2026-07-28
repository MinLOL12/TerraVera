/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.butchery;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * A size and build class for a carcass, resolved from whatever entity was killed.
 * <p>
 * Rather than a table of every animal in TFC and every animal every other addon might add, carcasses are sorted
 * into a handful of classes by carcass weight and body type. A cow and a bison butcher the same way and give the
 * same list of parts in different quantities; a pig gives far more fat than a deer of the same weight, which is
 * exactly why lard and venison are different things. Anything unrecognised falls back on its entity size.
 *
 * @param id            registry-safe identifier used in save data and translation keys
 * @param carcassMass   dressed weight in kilograms, the scale everything else is derived from
 * @param fatness       how much of that mass is recoverable fat and suet, 0..1
 * @param hidePieces    how many usable hide pieces a clean skinning produces
 * @param woolBearing   whether the animal carries fleece rather than a furred or bare hide
 */
public record CarcassSpecies(String id, float carcassMass, float fatness, int hidePieces, boolean woolBearing)
{
    public static final CarcassSpecies SMALL_GAME = new CarcassSpecies("small_game", 2.0f, 0.05f, 1, false);
    public static final CarcassSpecies FOWL = new CarcassSpecies("fowl", 1.5f, 0.10f, 0, false);
    public static final CarcassSpecies SHEEP = new CarcassSpecies("sheep", 25.0f, 0.18f, 1, true);
    public static final CarcassSpecies GOAT = new CarcassSpecies("goat", 22.0f, 0.10f, 1, false);
    public static final CarcassSpecies PIG = new CarcassSpecies("pig", 60.0f, 0.30f, 2, false);
    public static final CarcassSpecies DEER = new CarcassSpecies("deer", 45.0f, 0.06f, 2, false);
    public static final CarcassSpecies CATTLE = new CarcassSpecies("cattle", 250.0f, 0.16f, 4, false);
    public static final CarcassSpecies LARGE_GAME = new CarcassSpecies("large_game", 200.0f, 0.14f, 4, false);
    public static final CarcassSpecies PREDATOR = new CarcassSpecies("predator", 40.0f, 0.08f, 2, false);

    /**
     * Classify an entity type by its registry id. This is string matching on purpose: it works for TFC, for vanilla,
     * and for any animal addon at all, and it degrades to a sensible guess rather than crashing on the unknown.
     */
    public static CarcassSpecies fromEntityId(ResourceLocation entityId, float boundingBoxVolume)
    {
        final String path = entityId.getPath().toLowerCase(Locale.ROOT);

        if (path.contains("chicken") || path.contains("duck") || path.contains("quail")
            || path.contains("grouse") || path.contains("turkey") || path.contains("pheasant")
            || path.contains("parrot") || path.contains("penguin")) return FOWL;

        if (path.contains("rabbit") || path.contains("hare") || path.contains("fox")
            || path.contains("rat") || path.contains("squirrel") || path.contains("cat")
            || path.contains("chipmunk") || path.contains("marmot")) return SMALL_GAME;

        if (path.contains("sheep") || path.contains("alpaca") || path.contains("llama")
            || path.contains("musk_ox")) return SHEEP;

        if (path.contains("goat")) return GOAT;
        if (path.contains("pig") || path.contains("hog") || path.contains("boar")) return PIG;

        if (path.contains("deer") || path.contains("caribou") || path.contains("gazelle")
            || path.contains("antelope") || path.contains("moose") || path.contains("elk")) return DEER;

        if (path.contains("cow") || path.contains("cattle") || path.contains("yak")
            || path.contains("zebu") || path.contains("ox")) return CATTLE;

        if (path.contains("bison") || path.contains("horse") || path.contains("donkey")
            || path.contains("mule") || path.contains("camel")) return LARGE_GAME;

        if (path.contains("bear") || path.contains("wolf") || path.contains("lion")
            || path.contains("tiger") || path.contains("cougar") || path.contains("hyena")
            || path.contains("panther") || path.contains("sabertooth")) return PREDATOR;

        // Unknown animal: size it from its hitbox so that a modded moose is not butchered like a rabbit.
        if (boundingBoxVolume >= 4.0f) return LARGE_GAME;
        if (boundingBoxVolume >= 1.2f) return DEER;
        if (boundingBoxVolume >= 0.4f) return GOAT;
        return SMALL_GAME;
    }

    /** Number of primal cuts this build yields. Small animals are simply not big enough to break down. */
    public int primalCount()
    {
        if (carcassMass < 5f) return 1;
        if (carcassMass < 30f) return 2;
        if (carcassMass < 80f) return 4;
        return 6;
    }

    public Component displayName()
    {
        return Component.translatable("terravera.carcass.species." + id);
    }

    public static CarcassSpecies byId(String id)
    {
        return switch (id)
        {
            case "fowl" -> FOWL;
            case "sheep" -> SHEEP;
            case "goat" -> GOAT;
            case "pig" -> PIG;
            case "deer" -> DEER;
            case "cattle" -> CATTLE;
            case "large_game" -> LARGE_GAME;
            case "predator" -> PREDATOR;
            default -> SMALL_GAME;
        };
    }
}
