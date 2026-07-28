package com.terravera.common.power;

/**
 * A bare single copper conductor, strung horizontally from terminal to terminal. Cheap and easy to make, but thin:
 * it only carries 100 W before it starts to heat, and it cannot be run vertically - for drops and climbs, use the
 * insulated cable or a wire intersection as a junction.
 */
public class SingleWireBlock extends WireBlock {
    public SingleWireBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int capacity() {
        return 100;
    }
}
