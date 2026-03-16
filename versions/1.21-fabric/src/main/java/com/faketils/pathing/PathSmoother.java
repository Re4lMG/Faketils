package com.faketils.pathing;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.*;

public class PathSmoother {

    public static List<Vec3d> smooth(ClientWorld world, List<Vec3d> path) {

        if (path.size() < 3) return path;

        List<Vec3d> result = new ArrayList<>();

        int i = 0;
        result.add(path.get(0));

        while (i < path.size() - 1) {

            int j = path.size() - 1;

            while (j > i + 1) {

                if (hasLOS(world, path.get(i), path.get(j)))
                    break;

                j--;
            }

            result.add(path.get(j));
            i = j;
        }

        return result;
    }

    private static boolean hasLOS(ClientWorld world, Vec3d a, Vec3d b) {
        HitResult hit = world.raycast(new RaycastContext(
                a,
                b,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                MinecraftClient.getInstance().player
        ));

        return hit.getType() == HitResult.Type.MISS;
    }
}