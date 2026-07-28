package com.terravera.common.power;

import net.minecraft.core.Direction;

/**
 * A heavy cast junction block where up to six conductors meet - the hub of a wiring run. Unlike plain wire it ties
 * together every face, so it is used for vertical drops, climbing a wall, and crossing runs at a single point.
 * Its bus bar is rated for 400 W.
 */
public class WireIntersectionBlock extends WireBlock {
    public WireIntersectionBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int capacity() {
        return 400;
    }

    @Override
    public boolean allowsVerticalRuns() {
        return true;
    }

    @Override
    public boolean canHaveConnection(Direction side) {
        return true;
    }
}
