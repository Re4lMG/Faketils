package com.faketils.pathing;

import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class AStarPathfinder {

    private final ClientWorld world;
    private final boolean flyMode;

    public AStarPathfinder(ClientWorld world, boolean flyMode) {
        this.world = world;
        this.flyMode = flyMode;
    }

    public List<Vec3d> find(Vec3d startVec, Vec3d goalVec) {
        BlockPos start = BlockPos.ofFloored(startVec);
        BlockPos goal = BlockPos.ofFloored(goalVec);

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(Node::f));
        Map<BlockPos, Node> nodes = new HashMap<>();
        Set<BlockPos> closed = new HashSet<>();

        Node startNode = new Node(start);
        startNode.g = 0;
        startNode.h = heuristic(start, goal);

        open.add(startNode);
        nodes.put(start, startNode);

        while (!open.isEmpty()) {
            Node current = open.poll();

            if (current.pos.equals(goal)) {
                return buildPath(current);
            }

            closed.add(current.pos);

            for (BlockPos next : getNeighbors(current.pos)) {
                if (closed.contains(next) || !isValidPosition(next)) continue;

                double cost = calculateCost(current.pos, next);
                Node node = nodes.computeIfAbsent(next, Node::new);
                double newG = current.g + cost;

                if (node.parent == null || newG < node.g) {
                    node.parent = current;
                    node.g = newG;
                    node.h = heuristic(next, goal);
                    open.remove(node);
                    open.add(node);
                }
            }
        }
        return List.of();
    }

    private List<BlockPos> getNeighbors(BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;

                // 1. Normal horizontal move (walk/traverse)
                BlockPos horiz = pos.add(dx, 0, dz);
                if (isValidPosition(horiz)) {
                    neighbors.add(horiz);
                }

                // 2. Ascend 1 block (Baritone-style jump)
                BlockPos up = pos.add(dx, 1, dz);
                if (canAscend(pos, dx, dz)) {
                    neighbors.add(up);
                }

                // 3. Safe fall (1-3 blocks)
                for (int dy = -1; dy >= -3; dy--) {
                    BlockPos fall = pos.add(dx, dy, dz);
                    if (isValidPosition(fall)) {
                        neighbors.add(fall);
                        break; // land on first valid spot
                    }
                }
            }
        }

        if (flyMode) {
            // Extra pure vertical for flying
            if (isValidPosition(pos.up())) neighbors.add(pos.up());
            if (isValidPosition(pos.down())) neighbors.add(pos.down());
        }

        return neighbors;
    }

    private boolean isValidPosition(BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        if (flyMode) {
            return state.isAir(); // pure flying (you can tweak to allow water if wanted)
        }

        // WALK MODE - Baritone-style ground validation
        BlockPos floor = pos.down();
        BlockState floorState = world.getBlockState(floor);
        boolean hasFloor = floorState.isSolidBlock(world, floor) ||
                floorState.getBlock() instanceof StairsBlock ||
                floorState.getBlock() instanceof SlabBlock;

        BlockState feet = state;
        BlockState head = world.getBlockState(pos.up());

        return hasFloor && feet.isAir() && head.isAir();
    }

    private boolean canAscend(BlockPos from, int dx, int dz) {
        BlockPos target = from.add(dx, 1, dz);
        if (!isValidPosition(target)) return false;

        // Clearance over the step (Baritone-style)
        BlockPos front = from.add(dx, 0, dz);
        return world.getBlockState(front.up()).isAir();
    }

    private double calculateCost(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();

        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dy > 0) return dist * 1.6;           // JUMP
        if (dy < 0) return dist * 1.1;           // FALL
        boolean diagonal = Math.abs(dx) + Math.abs(dz) > 1;
        return diagonal ? 1.4 : 1.0;             // WALK / DIAGONAL
    }

    private double heuristic(BlockPos a, BlockPos b) {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        int dz = Math.abs(a.getZ() - b.getZ());
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private List<Vec3d> buildPath(Node node) {
        List<Vec3d> path = new ArrayList<>();
        while (node != null) {
            path.add(Vec3d.ofCenter(node.pos));
            node = node.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private static class Node {
        final BlockPos pos;
        Node parent = null;
        double g = Double.MAX_VALUE;
        double h = 0;

        Node(BlockPos pos) { this.pos = pos; }

        double f() { return g + h; }
    }
}