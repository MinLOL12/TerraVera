/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.sterilization;

import java.util.List;

/**
 * The branches of the treatment tree, for grouping the catalogue.
 */
public enum SterilizationCategory
{
    HEAT,
    SOLAR,
    DISTILLATION,
    FILTRATION,
    SETTLING,
    CHEMICAL,
    PHYSICAL;

    public String translationKey()
    {
        return "terravera.sterilization.category." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
