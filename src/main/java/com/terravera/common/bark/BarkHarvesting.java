/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 */

package com.terravera.common.bark;

import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import net.dries007.tfc.common.TFCTags;

import com.terravera.TerraVera;
import com.terravera.common.component.BarkProperties;
import com.terravera.common.items.TerraVeraItems;

/**
 * Knife harvesting for bark, including persistent pressure on each living tree.
 *
 * <p>Two sheets inside one recovery window are sustainable. A third strip thins the canopy; further attempts yield
 * nothing and may ring-bark the trunk. This makes moving between trees materially better than repeatedly clicking the
 * same log, while a harvested tree recovers after seven game days.</p>
 */
public final class BarkHarvesting
{
    private static final TagKey<Block> HARVESTABLE = TagKey.create(Registries.BLOCK, TerraVera.identifier("bark_harvestable"));
    private static final TagKey<Item> CUTTING_TOOLS = TagKey.create(Registries.ITEM, TerraVera.identifier("bark_cutting_tools"));
    private static final long RECOVERY_TICKS = 7L * 24000L;

    public static void init()
    {
        NeoForge.EVENT_BUS.addListener(BarkHarvesting::onStripBark);
    }

    private static void onStripBark(PlayerInteractEvent.RightClickBlock event)
    {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        final Player player = event.getEntity();
        final ItemStack tool = event.getItemStack();
        if (!isCuttingTool(tool)) return;

        final Level level = event.getLevel();
        final BlockPos clicked = event.getPos();
        final BlockState state = level.getBlockState(clicked);
        if (!state.is(BlockTags.LOGS) && !state.is(TFCTags.Blocks.LOGS_THAT_LOG) && !state.is(HARVESTABLE)) return;

        final BlockPos root = findRoot(level, clicked, state.getBlock());
        if (!hasLivingCanopy(level, root)) return; // Placed log piles are lumber, not infinitely renewable trees.

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
        if (!(level instanceof ServerLevel server)) return;

        final BarkSpecies species = classify(state.getBlock());
        final TreeHarvestData data = TreeHarvestData.get(server);
        final TreeHarvestData.Harvest before = data.get(root.asLong(), level.getGameTime(), RECOVERY_TICKS);

        if (!player.isCreative())
        {
            tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }

        // The fourth and later cut crosses from harvesting into ring-barking. There is no useful yield left.
        if (before.strips() >= 3)
        {
            data.record(root.asLong(), level.getGameTime(), species.id(), RECOVERY_TICKS);
            if (level.getRandom().nextFloat() < 0.5f)
            {
                killTree(server, root, state.getBlock());
                data.remove(root.asLong());
                player.displayClientMessage(Component.translatable("terravera.bark.tree_killed")
                    .withStyle(ChatFormatting.RED), true);
            }
            else
            {
                damageCanopy(server, root, 5);
                player.displayClientMessage(Component.translatable("terravera.bark.overstripped")
                    .withStyle(ChatFormatting.DARK_RED), true);
            }
            return;
        }

        final TreeHarvestData.Harvest after = data.record(root.asLong(), level.getGameTime(), species.id(), RECOVERY_TICKS);
        int count = species.properties().thicknessMm() >= 3.25f ? 2 : 1;
        if (after.strips() == 3)
        {
            count = 1;
            damageCanopy(server, root, 3);
            player.displayClientMessage(Component.translatable("terravera.bark.tree_damaged")
                .withStyle(ChatFormatting.GOLD), true);
        }
        else
        {
            player.displayClientMessage(Component.translatable("terravera.bark.harvested", after.strips(), 2)
                .withStyle(ChatFormatting.GRAY), true);
        }

        final ItemStack bark = new ItemStack(species.item().get(), count);
        // Be explicit so harvested bark always begins at species-correct field moisture, even if a pack changes defaults.
        bark.set(com.terravera.common.TerraVeraDataComponents.BARK_PROPERTIES.get(), species.properties());
        Block.popResource(level, clicked, bark);
    }

    private static boolean isCuttingTool(ItemStack stack)
    {
        return stack.is(CUTTING_TOOLS) || stack.is(TFCTags.Items.TOOLS_KNIFE)
            || stack.is(net.neoforged.neoforge.common.Tags.Items.TOOLS_SHEAR);
    }

    private static BlockPos findRoot(Level level, BlockPos start, Block trunk)
    {
        BlockPos root = start;
        for (int i = 0; i < 24; i++)
        {
            final BlockPos below = root.below();
            if (level.getBlockState(below).getBlock() != trunk) break;
            root = below;
        }
        return root;
    }

    private static boolean hasLivingCanopy(Level level, BlockPos root)
    {
        for (BlockPos pos : BlockPos.betweenClosed(root.offset(-4, 1, -4), root.offset(4, 24, 4)))
        {
            if (level.getBlockState(pos).is(BlockTags.LEAVES)) return true;
        }
        return false;
    }

    private static void damageCanopy(ServerLevel level, BlockPos root, int leavesToRemove)
    {
        int removed = 0;
        for (BlockPos pos : BlockPos.randomBetweenClosed(level.getRandom(), 96,
            root.getX() - 4, root.getY() + 2, root.getZ() - 4,
            root.getX() + 4, root.getY() + 24, root.getZ() + 4))
        {
            if (level.getBlockState(pos).is(BlockTags.LEAVES))
            {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                if (++removed >= leavesToRemove) return;
            }
        }
    }

    private static void killTree(ServerLevel level, BlockPos root, Block trunk)
    {
        // Removing the basal stem kills the tree and lets normal leaf-distance/decay rules take the canopy from here.
        for (int y = 0; y < 4; y++)
        {
            final BlockPos pos = root.above(y);
            if (level.getBlockState(pos).getBlock() != trunk) break;
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        damageCanopy(level, root, 8);
    }

    /** Uses registry paths so TFC species and vanilla species work without a hard dependency on either block roster. */
    static BarkSpecies classify(Block block)
    {
        final ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        final String path = key == null ? "" : key.getPath().toLowerCase(Locale.ROOT);
        if (path.contains("hemlock")) return BarkSpecies.HEMLOCK;
        if (path.contains("willow")) return BarkSpecies.WILLOW;
        if (path.contains("birch")) return BarkSpecies.BIRCH;
        if (path.contains("oak")) return BarkSpecies.OAK;
        if (path.contains("cedar") || path.contains("linden") || path.contains("elm")
            || path.contains("juniper") || path.contains("kapok") || path.contains("mangrove"))
        {
            return BarkSpecies.BAST;
        }
        return BarkSpecies.MIXED;
    }

    enum BarkSpecies
    {
        OAK("oak", TerraVeraItems.OAK_BARK_PROPERTIES, TerraVeraItems.OAK_BARK),
        HEMLOCK("hemlock", TerraVeraItems.HEMLOCK_BARK_PROPERTIES, TerraVeraItems.HEMLOCK_BARK),
        WILLOW("willow", TerraVeraItems.WILLOW_BARK_PROPERTIES, TerraVeraItems.WILLOW_BARK),
        BIRCH("birch", TerraVeraItems.BIRCH_BARK_PROPERTIES, TerraVeraItems.BIRCH_BARK),
        BAST("bast", TerraVeraItems.BAST_BARK_PROPERTIES, TerraVeraItems.BAST_BARK),
        MIXED("mixed", TerraVeraItems.MIXED_BARK_PROPERTIES, TerraVeraItems.BARK);

        private final String id;
        private final BarkProperties properties;
        private final Supplier<? extends Item> item;

        BarkSpecies(String id, BarkProperties properties, Supplier<? extends Item> item)
        {
            this.id = id;
            this.properties = properties;
            this.item = item;
        }

        String id() { return id; }
        BarkProperties properties() { return properties; }
        Supplier<? extends Item> item() { return item; }
    }

    private BarkHarvesting() {}
}
