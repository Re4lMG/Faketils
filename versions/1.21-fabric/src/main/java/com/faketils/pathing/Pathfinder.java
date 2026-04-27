package com.faketils.pathing;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;

import java.util.*;

public class Pathfinder {

    private static final int[][] NEIGHBORS = {
            { 1, 0,  0}, {-1, 0,  0}, {0, 0,  1}, {0, 0, -1},
            { 1, 0,  1}, { 1, 0, -1}, {-1, 0,  1}, {-1, 0, -1},
            {0,  1, 0}, {0, -1, 0},
            { 1,  1, 0}, {-1,  1, 0}, {0,  1,  1}, {0,  1, -1},
            { 1, -1, 0}, {-1, -1, 0}, {0, -1,  1}, {0, -1, -1},
    };

    public static List<BlockPos> findPath(BlockPos start, BlockPos end, World world, int maxNodes) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<BlockPos, Node> allNodes = new HashMap<>();
        Set<BlockPos> closedSet = new HashSet<>();

        Node startNode = new Node(start, 0, heuristic(start, end));
        openSet.add(startNode);
        allNodes.put(start, startNode);

        int count = 0;
        while (!openSet.isEmpty() && count++ < maxNodes) {
            Node current = openSet.poll();

            if (closedSet.contains(current.pos)) continue;
            closedSet.add(current.pos);

            if (current.pos.isWithinDistance(end, 1.5)) {
                List<BlockPos> raw = reconstructPath(current);
                return smoothPath(raw, world);
            }

            for (int[] offset : NEIGHBORS) {
                BlockPos neighbour = current.pos.add(offset[0], offset[1], offset[2]);
                if (closedSet.contains(neighbour)) continue;
                if (!isPassable(neighbour, world)) continue;

                double moveCost = (offset[0] != 0 && offset[2] != 0) ? 1.414 : 1.0;
                double tentativeG = current.gScore + moveCost;

                Node neighbourNode = allNodes.get(neighbour);
                if (neighbourNode == null) {
                    neighbourNode = new Node(neighbour, Double.POSITIVE_INFINITY, heuristic(neighbour, end));
                    allNodes.put(neighbour, neighbourNode);
                }

                if (tentativeG < neighbourNode.gScore) {
                    neighbourNode.parent = current;
                    neighbourNode.gScore = tentativeG;
                    neighbourNode.fScore = tentativeG + neighbourNode.hScore;
                    openSet.add(neighbourNode);
                }
            }
        }
        return null;
    }

    private static boolean isPassable(BlockPos pos, World world) {
        BlockState feet  = world.getBlockState(pos);
        BlockState head  = world.getBlockState(pos.up());
        return feet.getCollisionShape(world, pos).isEmpty()
                && head.getCollisionShape(world, pos.up()).isEmpty();
    }

    private static List<BlockPos> smoothPath(List<BlockPos> path, World world) {
        if (path.size() < 3) return path;

        List<BlockPos> result = new ArrayList<>();
        result.add(path.get(0));

        int i = 0;
        while (i < path.size() - 1) {
            int j = path.size() - 1;
            while (j > i + 1 && !hasLineOfSight(path.get(i), path.get(j), world)) {
                j--;
            }
            result.add(path.get(j));
            i = j;
        }
        return result;
    }

    private static boolean hasLineOfSight(BlockPos from, BlockPos to, World world) {
        double fx = from.getX() + 0.5, fy = from.getY(), fz = from.getZ() + 0.5;
        double tx = to.getX()   + 0.5, ty = to.getY(),   tz = to.getZ()   + 0.5;

        double dx = tx - fx, dy = ty - fy, dz = tz - fz;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length == 0) return true;

        dx /= length; dy /= length; dz /= length;

        double[] xOff = {-0.3, 0.3};
        double[] zOff = {-0.3, 0.3};
        double[] yOff = { 0.1, 1.7};  // feet, head

        int steps = (int) Math.ceil(length);
        for (double xo : xOff) {
            for (double zo : zOff) {
                for (double yo : yOff) {
                    for (int s = 1; s <= steps; s++) {
                        double t = (double) s / steps * length;
                        BlockPos check = BlockPos.ofFloored(
                                fx + dx * t + xo,
                                fy + dy * t + yo,
                                fz + dz * t + zo
                        );
                        BlockState state = world.getBlockState(check);
                        if (!state.getCollisionShape(world, check).isEmpty()) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        return Math.sqrt(a.getSquaredDistance(b));
    }

    private static List<BlockPos> reconstructPath(Node node) {
        List<BlockPos> path = new ArrayList<>();
        Node current = node;
        while (current != null) {
            path.add(current.pos);
            current = current.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private static class Node {
        BlockPos pos;
        double gScore, hScore, fScore;
        Node parent;

        Node(BlockPos pos, double gScore, double hScore) {
            this.pos    = pos;
            this.gScore = gScore;
            this.hScore = hScore;
            this.fScore = gScore + hScore;
        }
    }
}