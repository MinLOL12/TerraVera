/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.health;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockEvent;

import net.dries007.tfc.common.TFCTags;

import com.terravera.common.items.TerraVeraItems;
import com.terravera.config.TerraVeraConfig;

/**
 * Where the herbal remedies come from.
 * <p>
 * The three tier-one remedies are gathered from the landscape rather than crafted, which puts them on exactly the same
 * footing as the fibre the mod already asks you to gather: a thing you pick up as you travel, in small quantities, from
 * plants that grow where they should. That matters for pacing - a player who has been ill once starts noticing
 * medicinal plants on the way past, which is the behaviour the system wants to teach.
 *
 * <ul>
 *     <li><strong>Bitter herbs</strong> come from the same broadleaf herbs that yield herb fibre. Common, weak.</li>
 *     <li><strong>Wormwood</strong> comes from dry-climate shrubs - sagebrush and its relatives. Uncommon, and the
 *     only pre-modern anthelmintic in the game.</li>
 *     <li><strong>Willow bark</strong> is stripped from willow logs with a knife. It requires a blade, in the same
 *     spirit as bast fibre requiring one.</li>
 * </ul>
 */
public final class HerbGathering
{
    public static void init()
    {
        NeoForge.EVENT_BUS.register(HerbGathering.class);
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event)
    {
        if (!TerraVeraConfig.SERVER.enableDisease.get()) return;
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) return;

        final Player player = event.getPlayer();
        if (player.isCreative()) return;

        final BlockState state = event.getState();
        final BlockPos pos = event.getPos();
        final RandomSource random = level.getRandom();

        final ItemStack tool = player.getMainHandItem();
        final boolean bladed = tool.is(TFCTags.Items.TOOLS_KNIFE)
            || tool.is(Tags.Items.TOOLS_SHEAR)
            || tool.is(TFCTags.Items.TOOLS_SCYTHE);

        // Bitter herbs: the broadleaf herbs that also give herb-grade fibre.
        if (state.is(HerbTags.BITTER_HERB_SOURCE) && random.nextFloat() < (bladed ? 0.35f : 0.15f))
        {
            Block.popResource(level, pos, new ItemStack(TerraVeraItems.BITTER_HERBS.get()));
        }

        // Wormwood: dry shrubs. Rarer, and worth going out of your way for once you have a tapeworm.
        if (state.is(HerbTags.WORMWOOD_SOURCE) && random.nextFloat() < (bladed ? 0.30f : 0.10f))
        {
            Block.popResource(level, pos, new ItemStack(TerraVeraItems.WORMWOOD.get()));
        }

        // Willow bark: needs a blade, same bootstrap logic as bast fibre.
        if (bladed && state.is(HerbTags.WILLOW_BARK_SOURCE) && random.nextFloat() < 0.45f)
        {
            Block.popResource(level, pos, new ItemStack(TerraVeraItems.WILLOW_BARK.get(), 1 + random.nextInt(2)));
        }
    }

    /** Block tags for the three gatherable remedies. Kept here rather than in the main tag class for locality. */
    public static final class HerbTags
    {
        public static final net.minecraft.tags.TagKey<Block> BITTER_HERB_SOURCE = tag("herbs/bitter");
        public static final net.minecraft.tags.TagKey<Block> WORMWOOD_SOURCE = tag("herbs/wormwood");
        public static final net.minecraft.tags.TagKey<Block> WILLOW_BARK_SOURCE = tag("herbs/willow_bark");

        private static net.minecraft.tags.TagKey<Block> tag(String name)
        {
            return net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.terravera.TerraVera.MOD_ID, name));
        }

        private HerbTags() {}
    }

    private HerbGathering() {}
}
