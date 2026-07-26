/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

import com.terravera.common.TerraVeraCreativeTab;
import com.terravera.common.TerraVeraDataComponents;
import com.terravera.common.TerraVeraDataManagers;
import com.terravera.common.items.TerraVeraItems;
import com.terravera.common.recipes.TerraVeraRecipes;
import com.terravera.config.TerraVeraConfig;

/**
 * TerraVera - "true earth".
 * <p>
 * An addon for TerraFirmaCraft that adds the step missing from the start of the game: <strong>cordage before
 * tools</strong>. A stone head tied to a stick with nothing is a rock on a stick. You gather plant fibre from grasses
 * and herbs, ret it in water, and twist it into cordage before you can haft anything.
 * <p>
 * Knapping itself is left entirely to TerraFirmaCraft - you knap heads in TFC's own knapping screen, from TFC's own
 * recipes. TerraVera only changes what it takes to turn one of those heads into a finished tool. See
 * {@link com.terravera.common.recipes.LashingRecipe}.
 */
@Mod(TerraVera.MOD_ID)
public final class TerraVera
{
    public static final String MOD_ID = "terravera";
    public static final String MOD_NAME = "TerraVera";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation identifier(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public TerraVera(ModContainer mod, IEventBus bus)
    {
        LOGGER.info("Initializing TerraVera - a TerraFirmaCraft addon");

        mod.registerConfig(ModConfig.Type.SERVER, TerraVeraConfig.SERVER.spec());

        TerraVeraItems.ITEMS.register(bus);
        TerraVeraCreativeTab.CREATIVE_TABS.register(bus);
        TerraVeraRecipes.RECIPE_TYPES.register(bus);
        TerraVeraRecipes.RECIPE_SERIALIZERS.register(bus);
        TerraVeraDataComponents.COMPONENTS.register(bus);
        TerraVeraDataManagers.MANAGERS.register(bus);

        bus.addListener(TerraVeraCreativeTab::onBuildTabContents);

        TerraVeraEventHandler.init();
    }
}
