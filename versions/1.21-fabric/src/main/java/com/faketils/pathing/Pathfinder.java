package com.faketils.pathing;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;

import java.util.*;

public class Pathfinder {
    public static List<BlockPos> findPath(BlockPos start, BlockPos end, World world, int maxNodes) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<BlockPos, Node> allNodes = new HashMap<>();
        Set<BlockPos> closedSet = new HashSet<>();

        Node startNode = new Node(start, 0, getDistance(start, end));
        openSet.add(startNode);
        allNodes.put(start, startNode);

        int count = 0;
        while (!openSet.isEmpty() && count++ < maxNodes) {
            Node current = openSet.poll();

            if (closedSet.contains(current.pos)) continue;
            closedSet.add(current.pos);

            if (getManhattan(current.pos, end) < 2) {
                return reconstructPath(current);
            }

            for (int[] offset : NEIGHBORS) {
                BlockPos neighbor = current.pos.add(offset[0], offset[1], offset[2]);
                if (closedSet.contains(neighbor)) continue;
                if (!isPassable(neighbor, world)) continue;

                double moveCost = (offset[0] != 0 && offset[2] != 0) ? 1.414 : 1.0;
                double tentativeG = current.gScore + moveCost;

                Node neighborNode = allNodes.get(neighbor);
                if (neighborNode == null) {
                    neighborNode = new Node(neighbor, Double.POSITIVE_INFINITY, getDistance(neighbor, end));
                    allNodes.put(neighbor, neighborNode);
                }

                if (tentativeG < neighborNode.gScore) {
                    neighborNode.parent = current;
                    neighborNode.gScore = tentativeG;
                    neighborNode.fScore = tentativeG + neighborNode.hScore;
                    openSet.add(neighborNode);
                }
            }
        }
        return null;
    }

    private static int getManhattan(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX())
                + Math.abs(a.getY() - b.getY())
                + Math.abs(a.getZ() - b.getZ());
    }

    private static double getDistance(BlockPos a, BlockPos b) {
        return Math.sqrt(a.getSquaredDistance(b));
    }

    private static List<BlockPos> getNeighbors(BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();
        neighbors.add(pos.north());
        neighbors.add(pos.south());
        neighbors.add(pos.east());
        neighbors.add(pos.west());
        neighbors.add(pos.up());
        neighbors.add(pos.down());
        return neighbors;
    }

    private static final int[][] NEIGHBORS = {
            {1,0,0},{-1,0,0},{0,0,1},{0,0,-1},
            {1,0,1},{1,0,-1},{-1,0,1},{-1,0,-1},
            {0,1,0},{0,-1,0},
            {1,1,0},{-1,1,0},{0,1,1},{0,1,-1},
            {1,-1,0},{-1,-1,0},{0,-1,1},{0,-1,-1}
    };

    private static boolean isPassable(BlockPos pos, World world) {
        BlockState state = world.getBlockState(pos);
        BlockState above = world.getBlockState(pos.up());
        BlockState below = world.getBlockState(pos.down());

        boolean bodyFree = state.getCollisionShape(world, pos).isEmpty()
                && above.getCollisionShape(world, pos.up()).isEmpty();
        boolean hasGround = !below.getCollisionShape(world, pos.down()).isEmpty();

        return bodyFree && hasGround;
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
        double gScore;
        double hScore;
        double fScore;
        Node parent;

        Node(BlockPos pos, double gScore, double hScore) {
            this.pos = pos;
            this.gScore = gScore;
            this.hScore = hScore;
            this.fScore = gScore + hScore;
        }
    }
}