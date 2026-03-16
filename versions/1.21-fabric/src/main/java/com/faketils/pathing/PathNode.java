package com.faketils.pathing;

import net.minecraft.util.math.BlockPos;

public class PathNode {

    public final BlockPos pos;
    public PathNode parent;

    public double g;
    public double h;

    public PathNode(BlockPos pos) {
        this.pos = pos;
    }

    public double f() {
        return g + h;
    }
}