package com.terravera.common.power;

import net.minecraft.core.Direction;

/**
 * Any machine that participates in TerraVera's low-voltage grid.
 * <p>
 * Wires only attach to a machine through its declared terminal faces, so a turbine's spinning blades or the top of a
 * cabinet are not secretly live - you connect where the terminals are, exactly like real wiring.
 */
public interface PowerMachine {
    /**
     * Whether a wire pressed against the given face of this machine makes an electrical connection.
     *
     * @param side the face of the machine the wire is touching (e.g. {@code Direction.NORTH} for a wire north of it)
     */
    boolean canConnectWires(Direction side);
}
