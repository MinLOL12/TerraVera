/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-12
 */

package com.terravera.common.farming;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import com.terravera.common.blocks.TerraVeraBlocks;
import com.terravera.common.quality.SoilCondition;
import com.terravera.common.greenhouse.GreenhouseBlock;
import com.terravera.common.greenhouse.GreenhouseBlockEntity;
import com.terravera.config.TerraVeraConfig;

/**
 * Handles all farming event interactions: soil preparation, crop disease ticking, harvest bonuses from
 * well-worked ground, and greenhouse tray planting.
 * <p>
 * Notably it does <em>not</em> handle seeds or crop blocks. TerraFirmaCraft owns those; TerraVera's job here is
 * the soil under the crop and the glass over it.
 * <p>
 * This is the event-driven side of the farming system. The data is stored in block entities
 * ({@link PreparedFarmlandBlockEntity}, {@link GreenhouseBlockEntity}) but the triggers come from block
 * breaks, crop growth ticks, and right-click interactions.
 */
public final class FarmingEventHandler
{
    private static final TagKey<Block> CROP_DISEASE_HOSTS = TagKey.create(
        net.minecraft.core.registries.Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath("terravera", "crop_disease_hosts"));
    private static final TagKey<Block> PREPARABLE_SOIL = TagKey.create(
        net.minecraft.core.registries.Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath("terravera", "preparable_soil"));
    /** Seeds TerraVera recognises. Populated from TFC's own seed tag rather than from items of TerraVera's own. */
    private static final TagKey<Item> TERRAVERA_SEEDS = TagKey.create(
        net.minecraft.core.registries.Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath("terravera", "seeds"));

    public static void init()
    {
        NeoForge.EVENT_BUS.addListener(FarmingEventHandler::onCropHarvest);
        NeoForge.EVENT_BUS.addListener(FarmingEventHandler::onSeedPlanting);
        NeoForge.EVENT_BUS.addListener(FarmingEventHandler::onSoilPreparation);
        NeoForge.EVENT_BUS.addListener(FarmingEventHandler::onLevelTick);
    }

    /**
     * When a crop is harvested off prepared soil, well-worked ground pays back a little extra produce.
     * <p>
     * The old version of this handler dropped a TerraVera "select seed" carrying its own genetic quality data. That
     * has been removed: TerraFirmaCraft already has seeds, crop blocks, and its own growth rules, and running a
     * parallel seed line meant a second crop block rendered with vanilla wheat models sitting in TFC fields, which
     * is where the visual glitching came from. Soil preparation now expresses itself the honest way - as more of
     * whatever crop the player was actually growing.
     */
    @SubscribeEvent
    public static void onCropHarvest(BlockEvent.BreakEvent event)
    {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!TerraVeraConfig.SERVER.enableFarming.get()) return;

        BlockState state = event.getState();
        if (!isCrop(state)) return;

        Player player = event.getPlayer();
        if (player == null || player.isCreative()) return;

        RandomSource random = level.getRandom();
        BlockPos pos = event.getPos();

        // Soil quality from the prepared farmland the crop is standing on.
        SoilCondition soil = SoilCondition.UNPREPARED;
        if (level.getBlockEntity(pos.below()) instanceof PreparedFarmlandBlockEntity be)
        {
            soil = be.soilCondition();
        }

        float greenhouseBonus = greenhouseBonusNear(level, pos);
        float quality = Math.min(1.0f, Math.max(0.0f,
            soil.overallQuality() * 0.7f + greenhouseBonus * 0.3f));

        // Only genuinely good ground gives a bonus, and it is a chance rather than a guarantee, so a well-kept bed
        // is noticeably but not overwhelmingly better than tilled dirt.
        if (quality > 0.6f && random.nextFloat() < (quality - 0.5f) * 0.8f)
        {
            for (ItemStack drop : Block.getDrops(state, level, pos, null, player, player.getMainHandItem()))
            {
                // Duplicate the produce, not the seed: a fertile field yields more grain, not more seed stock.
                if (drop.isEmpty() || isSeedLike(drop)) continue;
                Block.popResource(level, pos, drop.copyWithCount(1));
                break;
            }
        }
    }

    /** The best greenhouse growth modifier within reach of this position, or 0 if there is no greenhouse. */
    private static float greenhouseBonusNear(ServerLevel level, BlockPos pos)
    {
        float best = 0.0f;
        for (int dx = -3; dx <= 3; dx++)
        {
            for (int dz = -3; dz <= 3; dz++)
            {
                for (int dy = 0; dy <= 3; dy++)
                {
                    if (level.getBlockEntity(pos.offset(dx, dy, dz)) instanceof GreenhouseBlockEntity greenhouse)
                    {
                        best = Math.max(best, greenhouse.climate().growthModifier());
                    }
                }
            }
        }
        return best;
    }

    /**
     * Greenhouse blocks accept seeds into their trays.
     * <p>
     * This handler no longer places a crop block of its own. Previously any seed-like item clicked onto prepared
     * soil was replaced with TerraVera's generic crop, which overrode TFC's own planting and rendered a vanilla
     * wheat model regardless of what had been sown. Prepared farmland is now simply farmland as far as planting is
     * concerned, and TFC (or vanilla) handles the seed it knows about.
     */
    @SubscribeEvent
    public static void onSeedPlanting(PlayerInteractEvent.RightClickBlock event)
    {
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!TerraVeraConfig.SERVER.enableFarming.get()) return;

        ItemStack held = event.getEntity().getMainHandItem();
        if (!isSeedLike(held)) return;

        BlockPos clickedPos = event.getPos();
        BlockState clicked = level.getBlockState(clickedPos);
        Player player = event.getEntity();

        if (clicked.getBlock() instanceof GreenhouseBlock
            && level.getBlockEntity(clickedPos) instanceof GreenhouseBlockEntity greenhouse)
        {
            if (greenhouse.tryPlantSeed(held, player))
            {
                if (!player.isCreative()) held.shrink(1);
            }
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
            event.setCanceled(true);
        }
    }

    /**
     * Right-clicking dirt with a stone-tipped digging stick or hoe starts soil preparation.
     * This converts vanilla dirt/TFC soil into prepared farmland with initial low quality.
     */
    @SubscribeEvent
    public static void onSoilPreparation(PlayerInteractEvent.RightClickBlock event)
    {
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!TerraVeraConfig.SERVER.enableFarming.get()) return;

        BlockPos soilPos = event.getPos();
        BlockState clicked = level.getBlockState(soilPos);
        BlockPos above = soilPos.above();
        BlockState aboveState = level.getBlockState(above);

        // Must be clicking on dirt/soil with empty space above
        if (!isTillable(clicked) || !aboveState.isAir()) return;

        ItemStack held = event.getEntity().getMainHandItem();
        if (!held.is(net.minecraft.tags.ItemTags.HOES)
            && !BuiltInRegistries.ITEM.getKey(held.getItem()).getPath().contains("digging_stick"))
        {
            return;
        }

        // Convert the clicked soil itself to prepared farmland. The old code placed the new block in the air above,
        // which created a floating soil block and left the original dirt untouched.
        BlockState farmland = TerraVeraBlocks.PREPARED_FARMLAND.get()
            .defaultBlockState().setValue(PreparedFarmlandBlock.PREPARATION, 1);
        level.setBlock(soilPos, farmland, Block.UPDATE_ALL);

        // Set initial soil condition - just cleared, not yet loosened or fertilized
        if (level.getBlockEntity(soilPos) instanceof PreparedFarmlandBlockEntity be)
        {
            be.setSoilCondition(new SoilCondition(0.1f, 0.0f, 0.1f, 0.0f, 0.3f));
        }

        // Damage the tool
        if (!event.getEntity().isCreative())
        {
            held.hurtAndBreak(1, event.getEntity(), net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        }

        event.getEntity().displayClientMessage(
            net.minecraft.network.chat.Component.translatable("terravera.soil.prepared"),
            true);

        event.setCancellationResult(net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide()));
        event.setCanceled(true);
    }

    /**
     * Tick crop diseases and greenhouse crops. Runs on the level tick event, checking every 200 ticks (10 seconds)
     * for efficiency.
     */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event)
    {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!TerraVeraConfig.SERVER.enableFarming.get()) return;
        if (level.getGameTime() % 200 != 0) return;

        // We use a random sampling approach rather than iterating every block, which would be expensive.
        // Each chunk has a chance of getting a disease check pass.
        level.getAllEntities().forEach(entity -> {
            if (entity instanceof Player player && !player.isSpectator())
            {
                checkNearbyCrops(level, player.blockPosition());
            }
        });
    }

    /**
     * Check crops near a player for disease progression. Limited to a small area to keep performance reasonable.
     */
    private static void checkNearbyCrops(ServerLevel level, BlockPos center)
    {
        RandomSource random = level.getRandom();

        for (int dx = -8; dx <= 8; dx++)
        {
            for (int dz = -8; dz <= 8; dz++)
            {
                BlockPos pos = center.offset(dx, 0, dz);
                BlockState state = level.getBlockState(pos);

                if (!isCrop(state)) continue;

                BlockPos below = pos.below();
                if (level.getBlockEntity(below) instanceof PreparedFarmlandBlockEntity farmland)
                {
                    SoilCondition soil = farmland.soilCondition();

                    // Poor soil quality increases disease risk
                    // Poor ground sets a crop back a stage. Guarded on the vanilla AGE property because TFC
                    // crops use their own growth properties and must not be poked through the wrong one.
                    if (soil.overallQuality() < 0.3f && random.nextFloat() < 0.05f
                        && state.hasProperty(CropBlock.AGE) && state.getValue(CropBlock.AGE) > 0)
                    {
                        level.setBlock(pos, state.setValue(CropBlock.AGE,
                            Math.max(0, state.getValue(CropBlock.AGE) - 1)), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    /**
     * Whether a block is a crop. TFC's crops do not extend vanilla {@link CropBlock}, so matching only on that
     * class silently skipped every TFC field - which made the whole soil system look like it did nothing.
     */
    private static boolean isCrop(BlockState state)
    {
        if (state.getBlock() instanceof CropBlock) return true;
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return path.startsWith("crop/") || path.contains("_crop") || path.endsWith("_plant");
    }

    private static boolean isTillable(BlockState state)
    {
        // Accept vanilla dirt, grass blocks, the datapack-configurable TerraVera preparable soil tag, and TFC soil blocks.
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return state.is(PREPARABLE_SOIL) ||
            state.is(BlockTags.DIRT) ||
            state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) ||
            path.contains("dirt") ||
            path.contains("loam") ||
            path.contains("silt") ||
            path.contains("soil");
    }

    private static boolean isSeedLike(ItemStack stack)
    {
        if (stack.isEmpty()) return false;
        if (stack.is(TERRAVERA_SEEDS)) return true;
        if (stack.is(Items.WHEAT_SEEDS) || stack.is(Items.BEETROOT_SEEDS) || stack.is(Items.MELON_SEEDS)
            || stack.is(Items.PUMPKIN_SEEDS) || stack.is(Items.TORCHFLOWER_SEEDS) || stack.is(Items.PITCHER_POD))
        {
            return true;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return !path.contains("seed_tray") && (path.endsWith("_seed") || path.endsWith("_seeds")
            || path.contains("/seed") || path.contains("seeds/"));
    }

    private FarmingEventHandler() {}
}
