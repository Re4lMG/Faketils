package com.faketils.events;

import com.faketils.mixin.PlayerInventoryAccessor;
import com.faketils.pathing.Pathfinder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.UpdatePlayerAbilitiesC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WalkingHandler {

    public static List<Vec3d> path = new ArrayList<>();
    private static int index = 0;
    private static State state = State.NONE;

    private static Vec3d targetPos;
    private static Entity targetEntity;
    private static float yModifier = 0f;
    private static boolean follow = false;
    private static boolean smooth = false;
    private static boolean useAOTV = false;
    private static boolean sprinting = false;

    private static long lastTpTime = 0;
    private static boolean tped = true;

    private static long stuckBreakTime = 0;
    private static long stuckCheckTime = 0;
    private static Vec3d lastPosCheck = Vec3d.ZERO;
    private static int ticksAtLastPos = 0;

    private static float neededYaw = Float.MIN_VALUE;

    private static long loweringRaisingTime = 0;
    private static long flyDelayTime = 0;
    private static long aotvDelayTime = 0;
    private static long stuckEscapeEndTime = 0;
    private static boolean isStuckEscaping = false;

    private static int tickCounter = 0;

    private static final double STOPPING_THRESHOLD = 0.75;
    private static final int MAX_NODES = 24000;
    private static final long ESCAPE_DURATION = 800;

    public enum State {
        NONE, CALCULATING, PATHING, DECELERATING, FAILED
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    public static void walkTo(Vec3d goal, boolean followEntity, boolean smoothPath, boolean aotvEnabled) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        useAOTV = aotvEnabled;
        smooth = smoothPath;
        follow = followEntity;
        targetPos = goal;
        targetEntity = null;
        yModifier = 0f;

        if (mc.player.getEntityPos().distanceTo(goal) < 1.0) {
            stop();
            return;
        }

        state = State.CALCULATING;

        BlockPos start = mc.player.getBlockPos();
        BlockPos end = BlockPos.ofFloored(goal.getX(), goal.getY(), goal.getZ());

        List<BlockPos> raw = Pathfinder.findPath(start, end, mc.world, MAX_NODES);
        if (raw == null || raw.isEmpty()) {
            state = State.FAILED;
            return;
        }

        List<Vec3d> vecPath = new ArrayList<>();
        for (BlockPos bp : raw) {
            vecPath.add(new Vec3d(bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5));
        }

        if (smooth) {
            vecPath = smoothPath(vecPath);
        }

        path = vecPath;
        index = 0;
        state = State.PATHING;
        tped = true;
    }

    public static void walkToEntity(Entity entity, boolean smoothPath, boolean aotvEnabled, float yMod) {
        targetEntity = entity;
        yModifier = yMod;
        walkTo(entity.getEntityPos().add(0, yMod, 0), true, smoothPath, aotvEnabled);
    }

    public static void setSprinting(boolean enabled) {
        sprinting = enabled;
    }

    public static void stop() {
        state = State.NONE;
        path.clear();
        index = 0;
        targetPos = null;
        targetEntity = null;
        follow = false;
        neededYaw = Float.MIN_VALUE;
        isStuckEscaping = false;
        stuckEscapeEndTime = 0;
        stopMovement();
        RotationHandler.reset();
    }

    public static boolean isRunning() {
        return state == State.PATHING || state == State.CALCULATING || state == State.DECELERATING || isStuckEscaping;
    }

    private static List<Vec3d> smoothPath(List<Vec3d> originalPath) {
        if (originalPath.size() < 3) return new ArrayList<>(originalPath);

        List<Vec3d> smoothed = new ArrayList<>();
        smoothed.add(originalPath.get(0));

        int lowerIndex = 0;
        while (lowerIndex < originalPath.size() - 2) {
            Vec3d start = originalPath.get(lowerIndex);
            Vec3d lastValid = originalPath.get(lowerIndex + 1);

            for (int upperIndex = lowerIndex + 2; upperIndex < originalPath.size(); upperIndex++) {
                Vec3d end = originalPath.get(upperIndex);
                if (traversable(start, end)) {
                    lastValid = end;
                }
            }
            smoothed.add(lastValid);
            lowerIndex = originalPath.indexOf(lastValid);
        }
        smoothed.add(originalPath.get(originalPath.size() - 1));
        return smoothed;
    }

    private static boolean traversable(Vec3d from, Vec3d to) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return false;

        Vec3d[] offsets = {
                new Vec3d(0.05, 0.1, 0.05), new Vec3d(0.05, 0.1, 0.95),
                new Vec3d(0.95, 0.1, 0.05), new Vec3d(0.95, 0.1, 0.95),
                new Vec3d(0.05, 1.9, 0.05), new Vec3d(0.05, 1.9, 0.95),
                new Vec3d(0.95, 1.9, 0.05), new Vec3d(0.95, 1.9, 0.95)
        };

        for (Vec3d offset : offsets) {
            Vec3d startPos = from.add(offset);
            Vec3d endPos = to.add(offset);

            BlockHitResult hit = mc.world.raycast(new RaycastContext(
                    startPos, endPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    mc.player
            ));

            if (hit.getType() == HitResult.Type.BLOCK) {
                return false;
            }
        }
        return true;
    }

    private static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || path.isEmpty() || state == State.NONE) return;

        tickCounter = (tickCounter + 1) % 12;

        if (follow && targetEntity != null && tickCounter == 0) {
            walkToEntity(targetEntity, smooth, useAOTV, yModifier);
            return;
        }

        long now = System.currentTimeMillis();

        if (isStuckEscaping) {
            if (now > stuckEscapeEndTime) {
                isStuckEscaping = false;
                stopMovement();
            } else {
                handleEscapeMovement(player);
                return;
            }
        }

        if (state == State.DECELERATING) {
            if (Math.abs(player.getVelocity().x) <= 0.05 && Math.abs(player.getVelocity().z) <= 0.05) {
                stop();
            }
            return;
        }

        if (checkForStuck(player.getEntityPos())) {
            handleStuck();
            return;
        }

        Vec3d current = player.getEntityPos();
        Vec3d next = getNextPoint();

        if (targetEntity != null) {
            Vec3d targetPosUpdated = targetEntity.getEntityPos().add(0, yModifier, 0);
            double vel = targetEntity.getVelocity().horizontalLength();
            if (vel > 0.1) {
                targetPosUpdated = targetPosUpdated.add(targetEntity.getVelocity().multiply(1.3));
            }
            if (current.distanceTo(targetPosUpdated) < 1.0 && vel < 0.15) {
                state = State.DECELERATING;
                stopMovement();
                return;
            }
        }

        if (willArriveAtDestinationAfterStopping(next)) {
            state = State.DECELERATING;
            stopMovement();
            return;
        }

        if (useAOTV && tped && now > aotvDelayTime && current.distanceTo(next) > 12 && isFrontClean(player)) {
            int aotvSlot = getAOTVSlot();
            PlayerInventoryAccessor inv = (PlayerInventoryAccessor) MinecraftClient.getInstance().player.getInventory();
            if (aotvSlot != -1 && aotvSlot != inv.getSelectedSlot()) {
                player.getInventory().setSelectedSlot(aotvSlot);
                aotvDelayTime = now + 150;
            } else if (aotvSlot != -1) {
                mc.interactionManager.interactItem(player, Hand.MAIN_HAND);
                tped = false;
                lastTpTime = now;
            }
        }

        Vec3d delta = next.subtract(current);
        float yaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalLength()));
        RotationHandler.setTarget(yaw, pitch);

        neededYaw = yaw;

        double dist = delta.length();
        mc.options.forwardKey.setPressed(dist > 1.5);

        VerticalDirection vert = shouldChangeHeight(current, next);
        if (player.getAbilities().allowFlying) {
            if (fly(next, current)) return;

            if (vert == VerticalDirection.HIGHER) {
                mc.options.jumpKey.setPressed(true);
                loweringRaisingTime = now + 750;
            } else if (vert == VerticalDirection.LOWER) {
                mc.options.sneakKey.setPressed(true);
                loweringRaisingTime = now + 750;
            } else if (now > loweringRaisingTime && delta.y > 0.5) {
                mc.options.jumpKey.setPressed(true);
            } else if (now > loweringRaisingTime && delta.y < -0.5) {
                mc.options.sneakKey.setPressed(true);
            }
        } else {
            if (shouldJump(next, current)) {
                player.jump();
            }
        }

        if (sprinting) {
            mc.options.sprintKey.setPressed(true);
        }

        if (dist < 0.8) {
            index++;
        }
    }

    private static boolean checkForStuck(Vec3d pos) {
        long now = System.currentTimeMillis();
        if (now < stuckCheckTime) return false;

        double diff = pos.squaredDistanceTo(lastPosCheck);
        if (diff < 2.25) {
            ticksAtLastPos++;
            if (ticksAtLastPos > 15) {
                ticksAtLastPos = 0;
                return true;
            }
        } else {
            ticksAtLastPos = 0;
            lastPosCheck = pos;
        }

        stuckCheckTime = now + 100;
        return false;
    }

    private static void handleStuck() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        Vec3d current = player.getEntityPos();

        float escapeYaw = 0f;
        for (float testYaw = 0; testYaw < 360; testYaw += 20) {
            Vec3d escapeDir = current.add(
                    Math.cos(Math.toRadians(testYaw)) * 2,
                    0,
                    Math.sin(Math.toRadians(testYaw)) * 2
            );
            if (traversable(current.add(0, 0.1, 0), escapeDir.add(0, 0.1, 0)) &&
                    traversable(current.add(0, 0.9, 0), escapeDir.add(0, 0.9, 0)) &&
                    traversable(current.add(0, 1.1, 0), escapeDir.add(0, 1.1, 0)) &&
                    traversable(current.add(0, 1.9, 0), escapeDir.add(0, 1.9, 0))) {
                escapeYaw = testYaw;
                break;
            }
        }

        neededYaw = escapeYaw;
        isStuckEscaping = true;
        stuckEscapeEndTime = System.currentTimeMillis() + ESCAPE_DURATION;

        mc.options.forwardKey.setPressed(true);
    }

    private static void handleEscapeMovement(ClientPlayerEntity player) {
        MinecraftClient.getInstance().options.forwardKey.setPressed(true);
    }

    private static boolean willArriveAtDestinationAfterStopping(Vec3d target) {
        Vec3d predicted = predictStoppingPosition();
        return predicted.distanceTo(target) < STOPPING_THRESHOLD;
    }

    private static Vec3d predictStoppingPosition() {
        MinecraftClient mc = MinecraftClient.getInstance();
        Vec3d pos = mc.player.getEntityPos();
        Vec3d vel = mc.player.getVelocity();

        for (int i = 0; i < 30; i++) {
            vel = vel.multiply(0.91);
            pos = pos.add(vel);
            if (vel.horizontalLength() < 0.01) break;
        }
        return pos;
    }

    private static Vec3d getNextPoint() {
        if (path.isEmpty()) return MinecraftClient.getInstance().player.getEntityPos();

        Vec3d current = MinecraftClient.getInstance().player.getEntityPos();
        return path.stream()
                .min(Comparator.comparingDouble(v -> v.distanceTo(current)))
                .map(closest -> {
                    int idx = path.indexOf(closest);
                    return idx + 1 < path.size() ? path.get(idx + 1) : path.get(path.size() - 1);
                })
                .orElse(path.get(path.size() - 1));
    }

    private static boolean shouldJump(Vec3d next, Vec3d current) {
        double dy = next.y - current.y;
        return dy > 0.6 && MinecraftClient.getInstance().player.isOnGround();
    }

    private enum VerticalDirection { HIGHER, LOWER, NONE }

    private static VerticalDirection shouldChangeHeight(Vec3d current, Vec3d next) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return VerticalDirection.NONE;

        float yaw = neededYaw;
        Vec3d dir = new Vec3d(
                -Math.sin(Math.toRadians(yaw)),
                0,
                Math.cos(Math.toRadians(yaw))
        ).normalize();

        Vec3d dirLeft = new Vec3d(
                -Math.sin(Math.toRadians(yaw - 20)),
                0,
                Math.cos(Math.toRadians(yaw - 20))
        ).normalize();

        Vec3d dirRight = new Vec3d(
                -Math.sin(Math.toRadians(yaw + 20)),
                0,
                Math.cos(Math.toRadians(yaw + 20))
        ).normalize();

        if (rayHitsBlock(current.add(dir.multiply(0.75)).add(0, mc.player.getHeight() + 0.1, 0), dir) ||
                rayHitsBlock(current.add(dirLeft.multiply(0.75)).add(0, mc.player.getHeight() + 0.1, 0), dirLeft) ||
                rayHitsBlock(current.add(dirRight.multiply(0.75)).add(0, mc.player.getHeight() + 0.1, 0), dirRight)) {
            return VerticalDirection.LOWER;
        }

        if (rayHitsBlock(current.add(dir.multiply(0.75)).add(0, -0.1, 0), dir) ||
                rayHitsBlock(current.add(dirLeft.multiply(0.75)).add(0, -0.1, 0), dirLeft) ||
                rayHitsBlock(current.add(dirRight.multiply(0.75)).add(0, -0.1, 0), dirRight)) {
            return VerticalDirection.HIGHER;
        }

        return VerticalDirection.NONE;
    }

    private static boolean rayHitsBlock(Vec3d from, Vec3d direction) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Vec3d to = from.add(direction.multiply(1.0));
        BlockHitResult hit = mc.world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return hit.getType() == HitResult.Type.BLOCK;
    }

    private static boolean fly(Vec3d next, Vec3d current) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        long now = System.currentTimeMillis();

        if (player.getVelocity().y < -0.0784) {
            if (now > flyDelayTime) {
                if (!player.getAbilities().flying) {
                    player.getAbilities().flying = true;
                    player.networkHandler.sendPacket(new UpdatePlayerAbilitiesC2SPacket(player.getAbilities()));
                }
                flyDelayTime = now + 180 + (long) (Math.random() * 180);
            }
        }

        if (player.isOnGround() && next.y - current.y > 0.5) {
            player.jump();
            flyDelayTime = now + 180 + (long) (Math.random() * 180);
            return true;
        }

        return false;
    }

    private static boolean isFrontClean(ClientPlayerEntity player) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Vec3d direction = player.getRotationVec(1f);
        Vec3d end = player.getEyePos().add(direction.multiply(12));
        BlockHitResult hit = mc.world.raycast(new RaycastContext(
                player.getEyePos(), end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
        ));
        return hit.getType() != HitResult.Type.BLOCK;
    }

    private static int getAOTVSlot() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        for (int i = 0; i < 9; i++) {
            String name = player.getInventory().getStack(i).getName().getString().toLowerCase();
            if (name.contains("aspect of the void") || name.contains("aspect of the end")) {
                return i;
            }
        }
        return -1;
    }

    private static boolean hasJustTped() {
        return System.currentTimeMillis() - lastTpTime < 550;
    }

    private static void stopMovement() {
        var o = MinecraftClient.getInstance().options;
        o.forwardKey.setPressed(false);
        o.backKey.setPressed(false);
        o.leftKey.setPressed(false);
        o.rightKey.setPressed(false);
        o.jumpKey.setPressed(false);
        o.sneakKey.setPressed(false);
        o.sprintKey.setPressed(false);
    }
}