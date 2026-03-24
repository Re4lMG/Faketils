package com.faketils.pathing;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;

import java.util.*;

public class Pathfinder {
    public static List<BlockPos> findPath(BlockPos start, BlockPos end, World world, int maxNodes) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<BlockPos, Node> allNodes = new HashMap<>();

        Node startNode = new Node(start, 0, getDistance(start, end));
        openSet.add(startNode);
        allNodes.put(start, startNode);

        int count = 0;
        while (!openSet.isEmpty() && count++ < maxNodes) {
            Node current = openSet.poll();

            if (current.pos.equals(end) || getManhattan(current.pos, end) < 2) {
                return reconstructPath(current);
            }

            for (BlockPos neighbor : getNeighbors(current.pos)) {
                if (!isPassable(neighbor, world)) continue;

                double tentativeGScore = current.gScore + getDistance(current.pos, neighbor);
                Node neighborNode = allNodes.getOrDefault(neighbor, new Node(neighbor, Double.POSITIVE_INFINITY, getDistance(neighbor, end)));

                if (tentativeGScore < neighborNode.gScore) {
                    neighborNode.parent = current;
                    neighborNode.gScore = tentativeGScore;
                    neighborNode.fScore = tentativeGScore + neighborNode.hScore;
                    if (!openSet.contains(neighborNode)) {
                        openSet.add(neighborNode);
                        allNodes.put(neighbor, neighborNode);
                    }
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

    private static boolean isPassable(BlockPos pos, World world) {
        BlockState state = world.getBlockState(pos);
        BlockState above = world.getBlockState(pos.up());
        return state.getCollisionShape(world, pos).isEmpty() &&
                above.getCollisionShape(world, pos.up()).isEmpty();
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