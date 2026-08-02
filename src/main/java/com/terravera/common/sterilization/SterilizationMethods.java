/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.sterilization;

import java.util.List;
import java.util.Locale;

/**
 * The complete catalogue of water sterilization methods in TerraVera - twenty-two of them, every one real.
 * <p>
 * The methods are the actual techniques taught by field guides and emergency-response manuals, and they map onto
 * TerraFirmaCraft's own progression: the TFC firepit pot for the heat methods, TFC's pottery and firing for the
 * ceramic filter, TFC's barrels and powders for the chemical methods. The one documented-only entry (freezing) exists
 * so the catalogue is honest about what the real world can do, and so a future machine has a home waiting for it.
 * <p>
 * Each method's description and materials are in the lang file under {@code terravera.sterilization.<id>.*}; this
 * class is the source of truth for the ids and the progression order.
 */
public final class SterilizationMethods
{
    public static final SterilizationMethod BOILING = new SterilizationMethod(
        "boiling", SterilizationCategory.HEAT, 0,
        "terravera.sterilization.boiling.name", "terravera.sterilization.boiling.desc", "terravera.sterilization.boiling.materials");

    public static final SterilizationMethod ROLLING_BOIL = new SterilizationMethod(
        "rolling_boil", SterilizationCategory.HEAT, 0,
        "terravera.sterilization.rolling_boil.name", "terravera.sterilization.rolling_boil.desc", "terravera.sterilization.rolling_boil.materials");

    public static final SterilizationMethod PASTEURIZATION = new SterilizationMethod(
        "pasteurization", SterilizationCategory.HEAT, 0,
        "terravera.sterilization.pasteurization.name", "terravera.sterilization.pasteurization.desc", "terravera.sterilization.pasteurization.materials");

    public static final SterilizationMethod STONE_BOILING = new SterilizationMethod(
        "stone_boiling", SterilizationCategory.HEAT, 1,
        "terravera.sterilization.stone_boiling.name", "terravera.sterilization.stone_boiling.desc", "terravera.sterilization.stone_boiling.materials");

    public static final SterilizationMethod SODIS = new SterilizationMethod(
        "sodis", SterilizationCategory.SOLAR, 1,
        "terravera.sterilization.sodis.name", "terravera.sterilization.sodis.desc", "terravera.sterilization.sodis.materials");

    public static final SterilizationMethod SOLAR_STILL = new SterilizationMethod(
        "solar_still", SterilizationCategory.SOLAR, 1,
        "terravera.sterilization.solar_still.name", "terravera.sterilization.solar_still.desc", "terravera.sterilization.solar_still.materials");

    public static final SterilizationMethod DISTILLATION = new SterilizationMethod(
        "distillation", SterilizationCategory.DISTILLATION, 3,
        "terravera.sterilization.distillation.name", "terravera.sterilization.distillation.desc", "terravera.sterilization.distillation.materials");

    public static final SterilizationMethod CLOTH_FILTER = new SterilizationMethod(
        "cloth_filter", SterilizationCategory.FILTRATION, 0,
        "terravera.sterilization.cloth_filter.name", "terravera.sterilization.cloth_filter.desc", "terravera.sterilization.cloth_filter.materials");

    public static final SterilizationMethod CHARCOAL_FILTER = new SterilizationMethod(
        "charcoal_filter", SterilizationCategory.FILTRATION, 1,
        "terravera.sterilization.charcoal_filter.name", "terravera.sterilization.charcoal_filter.desc", "terravera.sterilization.charcoal_filter.materials");

    public static final SterilizationMethod BIO_SAND = new SterilizationMethod(
        "bio_sand", SterilizationCategory.FILTRATION, 2,
        "terravera.sterilization.bio_sand.name", "terravera.sterilization.bio_sand.desc", "terravera.sterilization.bio_sand.materials");

    public static final SterilizationMethod CERAMIC_FILTER = new SterilizationMethod(
        "ceramic_filter", SterilizationCategory.FILTRATION, 3,
        "terravera.sterilization.ceramic_filter.name", "terravera.sterilization.ceramic_filter.desc", "terravera.sterilization.ceramic_filter.materials");

    public static final SterilizationMethod SETTLING = new SterilizationMethod(
        "settling", SterilizationCategory.SETTLING, 0,
        "terravera.sterilization.settling.name", "terravera.sterilization.settling.desc", "terravera.sterilization.settling.materials");

    public static final SterilizationMethod FLOCCULATION = new SterilizationMethod(
        "flocculation", SterilizationCategory.SETTLING, 1,
        "terravera.sterilization.flocculation.name", "terravera.sterilization.flocculation.desc", "terravera.sterilization.flocculation.materials");

    public static final SterilizationMethod MORINGA = new SterilizationMethod(
        "moringa", SterilizationCategory.SETTLING, 1,
        "terravera.sterilization.moringa.name", "terravera.sterilization.moringa.desc", "terravera.sterilization.moringa.materials");

    public static final SterilizationMethod CHLORINATION = new SterilizationMethod(
        "chlorination", SterilizationCategory.CHEMICAL, 3,
        "terravera.sterilization.chlorination.name", "terravera.sterilization.chlorination.desc", "terravera.sterilization.chlorination.materials");

    public static final SterilizationMethod IODINATION = new SterilizationMethod(
        "iodination", SterilizationCategory.CHEMICAL, 2,
        "terravera.sterilization.iodination.name", "terravera.sterilization.iodination.desc", "terravera.sterilization.iodination.materials");

    public static final SterilizationMethod PERMANGANATE = new SterilizationMethod(
        "permanganate", SterilizationCategory.CHEMICAL, 2,
        "terravera.sterilization.permanganate.name", "terravera.sterilization.permanganate.desc", "terravera.sterilization.permanganate.materials");

    public static final SterilizationMethod ACIDIFICATION = new SterilizationMethod(
        "acidification", SterilizationCategory.CHEMICAL, 0,
        "terravera.sterilization.acidification.name", "terravera.sterilization.acidification.desc", "terravera.sterilization.acidification.materials");

    public static final SterilizationMethod SILVER = new SterilizationMethod(
        "silver", SterilizationCategory.CHEMICAL, 4,
        "terravera.sterilization.silver.name", "terravera.sterilization.silver.desc", "terravera.sterilization.silver.materials");

    public static final SterilizationMethod UV = new SterilizationMethod(
        "uv", SterilizationCategory.PHYSICAL, 4,
        "terravera.sterilization.uv.name", "terravera.sterilization.uv.desc", "terravera.sterilization.uv.materials");

    public static final SterilizationMethod REVERSE_OSMOSIS = new SterilizationMethod(
        "reverse_osmosis", SterilizationCategory.PHYSICAL, 5,
        "terravera.sterilization.reverse_osmosis.name", "terravera.sterilization.reverse_osmosis.desc", "terravera.sterilization.reverse_osmosis.materials");

    public static final SterilizationMethod FREEZING = new SterilizationMethod(
        "freezing", SterilizationCategory.PHYSICAL, 0,
        "terravera.sterilization.freezing.name", "terravera.sterilization.freezing.desc", "terravera.sterilization.freezing.materials",
        false);

    /** All catalogue entries, in the order a player should meet them. */
    public static final List<SterilizationMethod> ALL = List.of(
        BOILING, ROLLING_BOIL, PASTEURIZATION, STONE_BOILING,
        SODIS, SOLAR_STILL,
        DISTILLATION,
        CLOTH_FILTER, CHARCOAL_FILTER, BIO_SAND, CERAMIC_FILTER,
        SETTLING, FLOCCULATION, MORINGA,
        CHLORINATION, IODINATION, PERMANGANATE, ACIDIFICATION, SILVER,
        UV, REVERSE_OSMOSIS, FREEZING);

    public static SterilizationMethod byId(String id)
    {
        final String wanted = id.toLowerCase(Locale.ROOT);
        for (SterilizationMethod method : ALL)
        {
            if (method.id().equals(wanted)) return method;
        }
        return BOILING;
    }

    private SterilizationMethods() {}
}
