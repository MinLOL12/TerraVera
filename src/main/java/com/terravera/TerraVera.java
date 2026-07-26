/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

import com.terravera.client.TerraVeraClient;
import com.terravera.common.TerraVeraCreativeTab;
import com.terravera.common.TerraVeraDataComponents;
import com.terravera.common.TerraVeraDataManagers;
import com.terravera.common.container.TerraVeraContainers;
import com.terravera.common.items.TerraVeraItems;
import com.terravera.common.recipes.TerraVeraRecipes;
import com.terravera.config.TerraVeraConfig;

/**
 * TerraVera - "true earth".
 * <p>
 * An addon for TerraFirmaCraft that reworks the very start of the game around two ideas:
 * <ol>
 *     <li><strong>Cordage before tools.</strong> A stone head lashed to a stick with nothing is a rock on a stick. You
 *     gather plant fibre from grasses and herbs, ret it in water, and twist it into cordage before you can haft
 *     anything. See {@link com.terravera.common.recipes.LashingRecipe}.</li>
 *     <li><strong>Function, not silhouette.</strong> Knapping no longer asks you to reproduce a picture. It asks
 *     whether the stone in your hand has a sturdy base and a strong tip. See
 *     {@link com.terravera.common.knapping.KnapAnalysis}.</li>
 * </ol>
 * <p>
 * Note: Knapping uses a custom {@link com.terravera.client.ShapingScreen} that reuses TFC's native knapping GUI
 * components (buttons, textures, sounds) but overlays TerraVera's function-based analysis of the shape being worked.
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
        TerraVeraContainers.CONTAINERS.register(bus);

        bus.addListener(TerraVeraCreativeTab::onBuildTabContents);

        TerraVeraEventHandler.init();

        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            TerraVeraClient.init(bus);
        }
    }
}
