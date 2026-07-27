/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.blocks;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.terravera.TerraVera;

public final class TerraVeraBlocks
{
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, TerraVera.MOD_ID);

    /**
     * A heavy, flat iron striking surface that can be set down beside a charcoal forge. It is intentionally weaker
     * than a true anvil: it can maintain and correct worn tools, but it does not replace TFC's anvil progression.
     */
    public static final DeferredHolder<Block, WorkplateBlock> WORKPLATE = BLOCKS.register("workplate",
        () -> new WorkplateBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .requiresCorrectToolForDrops()
            .strength(4.5f, 6.0f)
            .sound(SoundType.METAL)
            .noOcclusion()));

    /** Compact laid-stone footing. It distributes a column's load into soil or rock beneath it. */
    public static final DeferredHolder<Block, Block> RUBBLE_FOUNDATION = BLOCKS.register("rubble_foundation",
        () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .requiresCorrectToolForDrops()
            .strength(3.0f, 9.0f)
            .sound(SoundType.STONE)));

    /** A pegged timber post or lintel. Flexible and forgiving, but not suitable for long spans of masonry. */
    public static final DeferredHolder<Block, SupportBeamBlock> WOODEN_SUPPORT_BEAM = BLOCKS.register("wooden_support_beam",
        () -> new SupportBeamBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.5f, 4.0f)
            .sound(SoundType.WOOD)
            .noOcclusion()));

    /** Forge-welded wrought-iron I-section. It carries much heavier masonry and roof spans than timber. */
    public static final DeferredHolder<Block, SupportBeamBlock> WROUGHT_IRON_SUPPORT_BEAM = BLOCKS.register("wrought_iron_support_beam",
        () -> new SupportBeamBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .requiresCorrectToolForDrops()
            .strength(5.0f, 12.0f)
            .sound(SoundType.METAL)
            .noOcclusion()));

    private TerraVeraBlocks() {}
}
