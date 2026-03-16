package com.faketils.events;

import com.faketils.pathing.AStarPathfinder;
import com.faketils.pathing.PathSmoother;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class WalkingHandler {

    public static List<Vec3d> path;
    private static int index = 0;
    private static boolean active = false;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    public static void setPath(List<Vec3d> newPath) {
        path = newPath;
        index = 0;
        active = true;
    }

    public static void stop() {
        active = false;
        resetKeys();
    }

    //toUse
    public static void walkTo(Vec3d goal) {

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null || mc.world == null)
            return;

        AStarPathfinder pathfinder = new AStarPathfinder(mc.world, false);

        List<Vec3d> rawPath = pathfinder.find(
                mc.player.getEntityPos(),
                goal
        );

        if (rawPath.isEmpty())
            return;

        List<Vec3d> smoothPath =
                PathSmoother.smooth(mc.world, rawPath);

        setPath(smoothPath);
    }

    private static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (!active || player == null || path == null || index >= path.size()) return;

        Vec3d target = path.get(index);
        Vec3d pos = player.getEntityPos();
        Vec3d delta = target.subtract(pos);
        double dist = delta.length();

        float yaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalLength()));

        RotationHandler.setTarget(yaw, pitch);

        mc.options.forwardKey.setPressed(dist > 0.8);
        mc.options.jumpKey.setPressed(shouldJump(mc.player));

        if (dist < 0.8) {
            index++;
        }
    }

    private static boolean shouldJump(ClientPlayerEntity player) {

        BlockPos front = player.getBlockPos().offset(player.getHorizontalFacing());

        return player.getEntityWorld()
                .getBlockState(front)
                .isSolidBlock(player.getEntityWorld(), front);
    }

    private static void resetKeys() {

        var o = MinecraftClient.getInstance().options;

        o.forwardKey.setPressed(false);
        o.backKey.setPressed(false);
        o.leftKey.setPressed(false);
        o.rightKey.setPressed(false);
        o.jumpKey.setPressed(false);
    }
}