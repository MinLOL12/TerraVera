package com.terravera.common.power;

/**
 * Insulated low-voltage cable. The insulation lets runs pass close together without shorting, and the heavier
 * gauge carries 200 W. {@link PowerNetwork} follows only the connection properties this block declares, so a run
 * must physically touch terminals and other wire face-to-face - it never bridges air.
 */
public class CopperWireBlock extends WireBlock {
    public CopperWireBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int capacity() {
        return 200;
    }
}
