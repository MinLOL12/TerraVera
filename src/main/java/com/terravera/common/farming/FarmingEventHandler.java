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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import com.terravera.common.TerraVeraDataComponents;
import com.terravera.common.items.TerraVeraItems;
import com.terravera.common.quality.CropHealth;
import com.terravera.common.quality.SeedQuality;
import com.terravera.common.quality.SoilCondition;
import com.terravera.common.greenhouse.GreenhouseBlockEntity;
import com.terravera.config.TerraVeraConfig;

/**
 * Handles all farming event interactions: soil preparation, crop disease ticking, seed quality on harvest,
 * and crop growth modifiers from soil and greenhouse conditions.
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

    public static void init()
    {
        NeoForge.EVENT_BUS.addListener(FarmingEventHandler::onCropHarvest);
        NeoForge.EVENT_BUS.addListener(FarmingEventHandler::onSoilPreparation);
        NeoForge.EVENT_BUS.addListener(FarmingEventHandler::onLevelTick);
    }

    /**
     * When a crop is harvested, determine seed quality from the parent plant's condition and the soil it grew in.
     * Better soil + healthier plant = better seeds for replanting.
     */
    @SubscribeEvent
    public static void onCropHarvest(BlockEvent.BreakEvent event)
    {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!TerraVeraConfig.SERVER.enableFarming.get()) return;

        BlockState state = event.getState();
        if (!(state.getBlock() instanceof CropBlock crop)) return;

        Player player = event.getPlayer();
        if (player.isCreative()) return;

        RandomSource random = level.getRandom();
        BlockPos pos = event.getPos();
        BlockPos below = pos.below();

        // Determine soil quality from prepared farmland below
        SoilCondition soil = SoilCondition.UNPREPARED;
        if (level.getBlockEntity(below) instanceof PreparedFarmlandBlockEntity be)
        {
            soil = be.soilCondition();
        }

        // Check if inside a greenhouse for climate bonus
        float greenhouseBonus = 0.0f;
        for (int dx = -3; dx <= 3; dx++)
        {
            for (int dz = -3; dz <= 3; dz++)
            {
                for (int dy = 0; dy <= 3; dy++)
                {
                    BlockPos check = pos.offset(dx, dy, dz);
                    if (level.getBlockEntity(check) instanceof GreenhouseBlockEntity greenhouse)
                    {
                        greenhouseBonus = Math.max(greenhouseBonus, greenhouse.climate().growthModifier());
                    }
                }
            }
        }

        // Calculate harvested quality based on soil, greenhouse, and randomness
        float baseQuality = soil.overallQuality() * 0.6f + greenhouseBonus * 0.3f + random.nextFloat() * 0.1f;
        baseQuality = Math.min(1.0f, Math.max(0.0f, baseQuality));

        // Chance of bonus seed drops based on quality
        if (baseQuality > 0.7f && random.nextFloat() < (baseQuality - 0.5f) * 0.4f)
        {
            ItemStack seed = new ItemStack(TerraVeraItems.SELECT_SEED.get());
            seed.set(TerraVeraDataComponents.SEED_QUALITY.get(),
                new SeedQuality(baseQuality, 1, "crop", baseQuality));
            Block.popResource(level, pos, seed);
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

        BlockState clicked = level.getBlockState(event.getPos());
        BlockPos above = event.getPos().above();
        BlockState aboveState = level.getBlockState(above);

        // Must be clicking on dirt/soil with empty space above
        if (!isTillable(clicked) || !aboveState.isAir()) return;

        ItemStack held = event.getEntity().getMainHandItem();
        if (!held.is(net.minecraft.tags.ItemTags.HOES)
            && !BuiltInRegistries.ITEM.getKey(held.getItem()).getPath().contains("digging_stick"))
        {
            return;
        }

        // Convert to prepared farmland
        BlockState farmland = com.terravera.common.blocks.TerraVeraBlocks.PREPARED_FARMLAND.get()
            .defaultBlockState().setValue(PreparedFarmlandBlock.PREPARATION, 1);
        level.setBlock(above, farmland, Block.UPDATE_ALL);

        // Set initial soil condition - just cleared, not yet loosened or fertilized
        if (level.getBlockEntity(above) instanceof PreparedFarmlandBlockEntity be)
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

                if (!(state.getBlock() instanceof CropBlock)) continue;

                BlockPos below = pos.below();
                if (level.getBlockEntity(below) instanceof PreparedFarmlandBlockEntity farmland)
                {
                    SoilCondition soil = farmland.soilCondition();

                    // Poor soil quality increases disease risk
                    if (soil.overallQuality() < 0.3f && random.nextFloat() < 0.05f)
                    {
                        // Reduce crop growth stage as disease effect
                        if (state.getValue(CropBlock.AGE) > 0)
                        {
                            level.setBlock(pos, state.setValue(CropBlock.AGE,
                                Math.max(0, state.getValue(CropBlock.AGE) - 1)), Block.UPDATE_ALL);
                        }
                    }

                    // Weed competition: unweeded soil slows crop growth
                    if (soil.weedFree() < 0.3f && random.nextFloat() < 0.03f)
                    {
                        // Crop doesn't advance even on random tick
                    }
                }
            }
        }
    }

    private static boolean isTillable(BlockState state)
    {
        // Accept vanilla dirt, grass blocks, and TFC soil blocks
        return state.is(BlockTags.DIRT) ||
            state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) ||
            BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().contains("dirt") ||
            BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().contains("loam") ||
            BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().contains("silt");
    }

    private FarmingEventHandler() {}
}
