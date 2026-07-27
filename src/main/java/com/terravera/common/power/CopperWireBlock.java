package com.terravera.common.power;

import net.minecraft.world.level.block.Block;

/** Insulated low-voltage cable. PowerNetwork follows adjacent wire blocks and never bridges air. */
public class CopperWireBlock extends Block {
    public CopperWireBlock(Properties properties) { super(properties); }
}
