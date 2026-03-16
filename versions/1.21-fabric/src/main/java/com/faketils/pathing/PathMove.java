package com.faketils.pathing;

import net.minecraft.util.math.BlockPos;

public class PathMove {

    public final BlockPos pos;
    public final double cost;

    public PathMove(BlockPos pos, double cost) {
        this.pos = pos;
        this.cost = cost;
    }
}