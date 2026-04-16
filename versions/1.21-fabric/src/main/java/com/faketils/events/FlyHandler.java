package com.faketils.events;

import com.faketils.features.Farming;
import com.faketils.mixin.PlayerInventoryAccessor;
import com.faketils.pathing.Pathfinder;
import com.faketils.utils.Utils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdatePlayerAbilitiesC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FlyHandler {

    private static Vec3d targetPos = null;
    private static boolean active = false;
    private static boolean decelerating = false;

    private static Vec3d lastFlyToTarget = null;
    private static final double RETARGET_THRESHOLD_SQ = 2.0;
    private static volatile boolean pathfindingInProgress = false;

    private static final float  VERTICAL_TOLERANCE  = 0.6f;
    private static final float  APPROACH_DISTANCE   = 8.0f;
    private static final float  SLOW_STOP_DISTANCE  = 8.0f;
    private static final float  AOTV_MIN_DISTANCE   = 20.0f;
    private static final float  AOTV_MAX_DISTANCE   = 240.0f;
    private static final int    AOTV_COOLDOWN_TICKS = 7;
    private static final int    SLOT_SWITCH_DELAY   = 2 + new Random().nextInt(3);
    private static final double STOPPING_THRESHOLD  = 0.75;
    private static final long   ESCAPE_DURATION     = 800;
    private static final float  ANGLE_THRESHOLD     = 20.0f;

    private static int     frontClearTicks = 0;
    private static boolean lastFrontClear  = false;

    private static final float STRAFE_CHANCE_FAR  = 0.015f;
    private static final float STRAFE_CHANCE_NEAR = 0.08f;
    private static final int   STRAFE_TICKS       = 4 + new Random().nextInt(5);
    private static final int   JUMP_BOOST_DELAY   = 180;

    private static final float TARGET_Y_OFFSET = 4.5f;

    private static final double MOVE_KEY_THRESHOLD = 0.35;

    private static final Random RANDOM = new Random();

    private static int     strafeTimer  = 0;
    private static boolean strafeLeft   = false;
    private static long    lastJumpTime = 0;

    public static List<Vec3d> path;
    private static int pathIndex = 0;

    private static int     originalSlot      = -1;
    private static int     aotvSlot          = -1;
    private static int     slotSwitchTimer   = 0;
    private static int     aotvCooldown      = 0;
    private static boolean awaitingRightClick = false;
    private static boolean usingPath = false;
    private static long pathModeEndTime = 0;
    private static final long PATH_MODE_DURATION_MS = 3000;
    private static long lastPathRequestTime = 0;
    private static final long PATH_REQUEST_COOLDOWN_MS = 3000;

    private static long    stuckCheckTime     = 0;
    private static Vec3d   lastPosCheck       = Vec3d.ZERO;
    private static int     ticksAtLastPos     = 0;
    private static long    stuckEscapeEndTime = 0;
    private static boolean isStuckEscaping   = false;
    private static float   escapeYaw         = 0f;

    private enum VerticalDirection { HIGHER, LOWER, NONE }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(FlyHandler::onTick);
    }

    public static void setTarget(Vec3d pos) {
        if (MinecraftClient.getInstance().player == null) return;
        PlayerInventoryAccessor inv = (PlayerInventoryAccessor) MinecraftClient.getInstance().player.getInventory();
        originalSlot = inv.getSelectedSlot();
        targetPos = pos;
        active = true;
        decelerating = false;
        strafeTimer = 0;
    }

    public static void stop() {
        active = false;
        decelerating = false;
        targetPos = null;
        isStuckEscaping = false;
        stuckEscapeEndTime = 0;
        ticksAtLastPos = 0;
        usingPath = false;
        pathModeEndTime = 0;
        lastPathRequestTime = 0;
        resetKeys();
        resetAotvState();
        lastFlyToTarget = null;
        pathfindingInProgress = false;
        clearPath();
        RotationHandler.reset();
    }

    public static void clearPath() {
        path = null;
        pathIndex = 0;
        frontClearTicks = 0;
        lastFrontClear = false;
    }

    private static void resetAotvState() {
        originalSlot = -1;
        aotvSlot = -1;
        slotSwitchTimer = 0;
        awaitingRightClick = false;
    }

    public static void setPath(List<Vec3d> newPath) {
        path = newPath;
        pathIndex = Math.min(1, path.size() - 1);
        active = true;
        decelerating = false;
    }

    public static void flyTo(Vec3d goal) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        if (pathfindingInProgress) return;

        lastFlyToTarget = goal;
        targetPos = goal;
        active = true;
        decelerating = false;

        BlockPos start = mc.player.getBlockPos();
        BlockPos end   = BlockPos.ofFloored(goal);

        pathfindingInProgress = true;
        new Thread(() -> {
            List<BlockPos> raw = Pathfinder.findPath(start, end, mc.world, 20000);
            mc.execute(() -> {
                pathfindingInProgress = false;
                if (raw == null || raw.isEmpty()) return;

                List<Vec3d> vec3dPath = new ArrayList<>();
                for (BlockPos bp : raw) {
                    vec3dPath.add(new Vec3d(bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5));
                }
                clearPath();
                setPath(smoothPath(vec3dPath));
            });
        }, "pathfinder").start();
    }

    private static void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || !active || targetPos == null) return;

        Vec3d pos   = player.getEntityPos();
        Vec3d delta = targetPos.subtract(pos);
        double horizDist = delta.horizontalLength();
        double fullDist  = delta.length();

        if (decelerating) {
            if (Math.abs(player.getVelocity().x) <= 0.05 && Math.abs(player.getVelocity().z) <= 0.05) {
                stop();
            }
            return;
        }

        if (isStuckEscaping) {
            long now = System.currentTimeMillis();
            if (now > stuckEscapeEndTime) {
                isStuckEscaping = false;
                resetKeys();
            } else {
                handleEscapeMovement(player);
                return;
            }
        }

        if (checkForStuck(pos)) {
            handleStuck(player);
            return;
        }

        long now = System.currentTimeMillis();

        if (usingPath && now > pathModeEndTime) {
            usingPath = false;
        }

        if (!usingPath) {
            if (!isFrontClear(player, 4.0)) {
                usingPath = true;
                pathModeEndTime = now + PATH_MODE_DURATION_MS;
            }
        }

        if (!usingPath) {
            if (path != null && !path.isEmpty()) clearPath();
            tickDirect(client, player, pos, delta, horizDist, fullDist);
        } else {
            if ((path == null || path.isEmpty())
                    && now - lastPathRequestTime > PATH_REQUEST_COOLDOWN_MS) {
                lastPathRequestTime = now;
                flyTo(targetPos);
            }
            if (path != null && !path.isEmpty()) {
                tickPath(client, player, pos, delta, fullDist);
            } else {
                tickDirect(client, player, pos, delta, horizDist, fullDist);
            }
        }

        PlayerInventoryAccessor inv = (PlayerInventoryAccessor) player.getInventory();

        if (aotvCooldown > 0) aotvCooldown--;
        if (slotSwitchTimer > 0) {
            slotSwitchTimer--;
            if (slotSwitchTimer == 0 && awaitingRightClick) {
                doAotvRightClick();
                awaitingRightClick = false;
            }
        }

        if (fullDist < APPROACH_DISTANCE && Farming.vacuumSlot != -1) {
            if (inv.getSelectedSlot() != Farming.vacuumSlot) {
                player.getInventory().setSelectedSlot(Farming.vacuumSlot);
            }
            resetKeys();
        }

        boolean fallingFast = player.getVelocity().y < -0.1;
        if (fallingFast && System.currentTimeMillis() - lastJumpTime > JUMP_BOOST_DELAY) {
            setFlying(true);
        }
    }

    private static void tickPath(MinecraftClient client, ClientPlayerEntity player,
                                 Vec3d pos, Vec3d delta, double fullDist) {

        if (path == null || path.isEmpty() || pathIndex >= path.size()) return;

        boolean frontClear = isFrontClear(player, 2.0);

        if (frontClear == lastFrontClear) {
            frontClearTicks++;
        } else {
            frontClearTicks = 0;
            lastFrontClear = frontClear;
        }
        boolean stableFrontClear = frontClearTicks > 3 ? lastFrontClear : !lastFrontClear;

        Vec3d rawNode    = path.get(pathIndex);
        Vec3d moveTarget = stableFrontClear ? rawNode.add(0, 4, 0) : rawNode;
        Vec3d node       = moveTarget.add(0, 0.05, 0);

        Vec3d deltaPath    = node.subtract(pos);
        Vec3d deltaPathRaw = moveTarget.subtract(pos);
        Vec3d deltaLook    = rawNode.subtract(pos);

        float fullDistPath    = (float) deltaPath.length();
        float fullDistPathRaw = (float) deltaPathRaw.length();

        float targetYawPath   = (float) Math.toDegrees(Math.atan2(deltaLook.z, deltaLook.x)) - 90f;
        float targetPitchPath = (float) -Math.toDegrees(Math.atan2(deltaLook.y, deltaLook.horizontalLength()));

        RotationHandler.setTarget(targetYawPath, targetPitchPath);

        float   yawDiff      = Math.abs(wrapDegrees(player.getYaw() - targetYawPath));
        float   pitchDiff    = Math.abs(player.getPitch() - targetPitchPath);
        boolean facingTarget = yawDiff < ANGLE_THRESHOLD && pitchDiff < ANGLE_THRESHOLD;

        boolean isLastNode = pathIndex >= path.size() - 1;
        if (isLastNode && willArriveAtDestinationAfterStopping(player, moveTarget)) {
            decelerating = true;
            stop();
            return;
        }

        VerticalDirection vert = shouldChangeHeight(pos, node, targetYawPath);
        boolean wantUp   = deltaPath.y > VERTICAL_TOLERANCE || vert == VerticalDirection.HIGHER;
        boolean wantDown = deltaPath.y < -VERTICAL_TOLERANCE || vert == VerticalDirection.LOWER;

        if (isDirectionBlocked(player, 0.0f, +1.2f)) wantUp = false;

        if (facingTarget && player.isOnGround() && wantUp) {
            player.jump();
            lastJumpTime = System.currentTimeMillis();
        }

        KeyBinding jump  = client.options.jumpKey;
        KeyBinding sneak = client.options.sneakKey;

        boolean shouldMove = facingTarget && fullDist > SLOW_STOP_DISTANCE;
        float relAngle = wrapDegrees(player.getYaw() - targetYawPath);
        setMovementKeysForAngle(client, relAngle, shouldMove);
        player.setSprinting(shouldMove && Math.abs(relAngle) < 50f);

        if (facingTarget) {
            jump.setPressed(wantUp);
            sneak.setPressed(wantDown);
        } else {
            jump.setPressed(false);
            sneak.setPressed(false);
        }

        if (facingTarget && fullDistPath > AOTV_MIN_DISTANCE && fullDistPath < AOTV_MAX_DISTANCE
                && aotvCooldown <= 0 && slotSwitchTimer == 0) {
            aotvSlot = findAotvSlot(player);
            if (aotvSlot != -1 && isFrontClear(player, 12.0)) {
                player.getInventory().setSelectedSlot(aotvSlot);
                slotSwitchTimer = SLOT_SWITCH_DELAY;
                awaitingRightClick = true;
                aotvCooldown = AOTV_COOLDOWN_TICKS + RANDOM.nextInt(15);
            }
        }

        double rawGroundDist = rawNode.subtract(pos).length();
        if (rawGroundDist <= 1.2) {
            pathIndex++;
            if (pathIndex >= path.size()) {
                path = null;
                clearPath();
                stop();
                return;
            }
        }
    }

    private static void tickDirect(MinecraftClient client, ClientPlayerEntity player,
                                   Vec3d pos, Vec3d delta, double horizDist, double fullDist) {
        float targetYaw   = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90f;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizDist));

        float yawDiff   = Math.abs(wrapDegrees(player.getYaw() - targetYaw));
        float pitchDiff = Math.abs(player.getPitch() - targetPitch);
        boolean facingTarget = yawDiff < ANGLE_THRESHOLD && pitchDiff < ANGLE_THRESHOLD;

        RotationHandler.setTarget(targetYaw, targetPitch);

        if (willArriveAtDestinationAfterStopping(player, targetPos)) {
            decelerating = true;
            stop();
            return;
        }

        if (facingTarget && fullDist > AOTV_MIN_DISTANCE && fullDist < AOTV_MAX_DISTANCE
                && aotvCooldown <= 0 && slotSwitchTimer == 0) {
            aotvSlot = findAotvSlot(player);
            if (aotvSlot != -1 && isFrontClear(player, 12.0)) {
                player.getInventory().setSelectedSlot(aotvSlot);
                slotSwitchTimer = SLOT_SWITCH_DELAY;
                awaitingRightClick = true;
                aotvCooldown = AOTV_COOLDOWN_TICKS + RANDOM.nextInt(15);
            }
        }

        Vec3d eDelta = targetPos.add(0, TARGET_Y_OFFSET, 0).subtract(pos);

        VerticalDirection vert = shouldChangeHeight(pos, targetPos.add(0, TARGET_Y_OFFSET, 0), targetYaw);
        boolean wantUp   = eDelta.y > VERTICAL_TOLERANCE || vert == VerticalDirection.HIGHER;
        boolean wantDown = eDelta.y < -VERTICAL_TOLERANCE || vert == VerticalDirection.LOWER;

        if (isDirectionBlocked(player, 0.0f, +1.2f)) wantUp   = false;
        if (isDirectionBlocked(player, 0.0f, -0.01f)) wantDown = false;

        KeyBinding jump  = client.options.jumpKey;
        KeyBinding sneak = client.options.sneakKey;

        boolean shouldMove = facingTarget && fullDist > SLOW_STOP_DISTANCE;
        float relAngle = wrapDegrees(player.getYaw() - targetYaw);
        setMovementKeysForAngle(client, relAngle, shouldMove);
        player.setSprinting(shouldMove && Math.abs(relAngle) < 50f);

        if (facingTarget) {
            jump.setPressed(wantUp);
            sneak.setPressed(wantDown);
        } else {
            jump.setPressed(false);
            sneak.setPressed(false);
        }

        if (facingTarget && player.isOnGround() && wantUp) {
            player.jump();
            lastJumpTime = System.currentTimeMillis();
        }
    }

    private static void setMovementKeysForAngle(MinecraftClient client, float relAngle, boolean shouldMove) {
        KeyBinding forward = client.options.forwardKey;
        KeyBinding back    = client.options.backKey;
        KeyBinding left    = client.options.leftKey;
        KeyBinding right   = client.options.rightKey;

        if (!shouldMove) {
            forward.setPressed(false);
            back.setPressed(false);
            left.setPressed(false);
            right.setPressed(false);
            return;
        }

        double rad = Math.toRadians(relAngle);
        double fwd = Math.cos(rad);
        double str = Math.sin(rad);

        forward.setPressed(fwd >  MOVE_KEY_THRESHOLD);
        back.setPressed(   fwd < -MOVE_KEY_THRESHOLD);
        left.setPressed(   str >  MOVE_KEY_THRESHOLD);
        right.setPressed(  str < -MOVE_KEY_THRESHOLD);
    }

    private static List<Vec3d> smoothPath(List<Vec3d> originalPath) {
        if (originalPath.size() < 3) return new ArrayList<>(originalPath);

        List<Vec3d> smoothed = new ArrayList<>();
        smoothed.add(originalPath.get(0));

        int lowerIndex = 0;
        while (lowerIndex < originalPath.size() - 2) {
            Vec3d start     = originalPath.get(lowerIndex);
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
            BlockHitResult hit = mc.world.raycast(new RaycastContext(
                    from.add(offset), to.add(offset),
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    mc.player
            ));
            if (hit.getType() == HitResult.Type.BLOCK) return false;
        }
        return true;
    }

    private static boolean willArriveAtDestinationAfterStopping(ClientPlayerEntity player, Vec3d target) {
        return predictStoppingPosition(player).distanceTo(target) < STOPPING_THRESHOLD;
    }

    private static Vec3d predictStoppingPosition(ClientPlayerEntity player) {
        Vec3d pos = player.getEntityPos();
        Vec3d vel = player.getVelocity();
        for (int i = 0; i < 30; i++) {
            vel = vel.multiply(0.91);
            pos = pos.add(vel);
            if (vel.horizontalLength() < 0.01) break;
        }
        return pos;
    }

    private static boolean checkForStuck(Vec3d pos) {
        long now = System.currentTimeMillis();
        if (now < stuckCheckTime) return false;

        double diff = pos.squaredDistanceTo(lastPosCheck);
        if (diff < 2.25) {
            if (++ticksAtLastPos > 15) {
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

    private static void handleStuck(ClientPlayerEntity player) {
        isStuckEscaping = false;
        ticksAtLastPos = 0;
        resetKeys();

        if (targetPos != null) {
            flyTo(targetPos);
        }
    }

    private static void handleEscapeMovement(ClientPlayerEntity player) {
        RotationHandler.setTarget(escapeYaw, 0f);
        MinecraftClient.getInstance().options.forwardKey.setPressed(true);
    }

    private static VerticalDirection shouldChangeHeight(Vec3d current, Vec3d next, float yaw) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return VerticalDirection.NONE;

        Vec3d dir      = new Vec3d(-Math.sin(Math.toRadians(yaw)),      0, Math.cos(Math.toRadians(yaw))).normalize();
        Vec3d dirLeft  = new Vec3d(-Math.sin(Math.toRadians(yaw - 20)), 0, Math.cos(Math.toRadians(yaw - 20))).normalize();
        Vec3d dirRight = new Vec3d(-Math.sin(Math.toRadians(yaw + 20)), 0, Math.cos(Math.toRadians(yaw + 20))).normalize();

        double h = mc.player.getHeight();

        if (rayHitsBlock(current.add(dir.multiply(0.75)).add(0, h + 0.1, 0), dir) ||
                rayHitsBlock(current.add(dirLeft.multiply(0.75)).add(0, h + 0.1, 0), dirLeft) ||
                rayHitsBlock(current.add(dirRight.multiply(0.75)).add(0, h + 0.1, 0), dirRight)) {
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
        if (mc.world == null) return false;
        Vec3d to = from.add(direction.multiply(1.0));
        BlockHitResult hit = mc.world.raycast(new RaycastContext(
                from, to,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        return hit.getType() == HitResult.Type.BLOCK;
    }

    private static int findAotvSlot(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                String name = stack.getName().getString().toLowerCase();
                if (name.contains("aspect of the void") || name.contains("aspect of the end")) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static void doAotvRightClick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        Utils.simulateUseItem(mc.interactionManager);
    }

    private static float wrapDegrees(float degrees) {
        degrees = degrees % 360.0F;
        if (degrees >= 180.0F) degrees -= 360.0F;
        if (degrees < -180.0F) degrees += 360.0F;
        return degrees;
    }

    private static boolean isFrontClear(ClientPlayerEntity player, double distance) {
        float yaw = player.getYaw();

        float[] yawOffsets = { 0f, -30f, 30f, -60f, 60f };
        double[] yOffsets = { 0.1, 0.9, 1.7 };

        for (int i = 0; i < yawOffsets.length; i++) {
            float checkYaw = yaw + yawOffsets[i];
            double checkDist = i < 3 ? distance : distance * 0.5;
            Vec3d dir = new Vec3d(
                    -Math.sin(Math.toRadians(checkYaw)),
                    0,
                    Math.cos(Math.toRadians(checkYaw))
            ).normalize();

            for (double yOff : yOffsets) {
                Vec3d start = player.getEntityPos().add(0, yOff, 0);
                Vec3d end   = start.add(dir.multiply(checkDist));
                RaycastContext ctx = new RaycastContext(start, end, ShapeType.COLLIDER, FluidHandling.NONE, player);
                BlockHitResult hit = player.getEntityWorld().raycast(ctx);
                if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().getY() >= player.getY() - 1) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void setFlying(boolean enable) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.getNetworkHandler() == null) return;
        PlayerAbilities abilities = player.getAbilities();
        abilities.flying = enable;
        mc.getNetworkHandler().sendPacket(new UpdatePlayerAbilitiesC2SPacket(abilities));
    }

    private static boolean isDirectionBlocked(ClientPlayerEntity player, float yawOffset, float yOffset) {
        Vec3d look  = player.getRotationVector().rotateY((float) Math.toRadians(yawOffset));
        Vec3d start = player.getEyePos();
        Vec3d end   = start.add(look.multiply(1.2)).add(0, yOffset, 0);
        RaycastContext ctx = new RaycastContext(start, end, ShapeType.COLLIDER, FluidHandling.NONE, player);
        HitResult hit = player.getEntityWorld().raycast(ctx);
        return hit != null && hit.getType() == HitResult.Type.BLOCK;
    }

    private static void resetKeys() {
        var opts = MinecraftClient.getInstance().options;
        opts.forwardKey.setPressed(false);
        opts.backKey.setPressed(false);
        opts.leftKey.setPressed(false);
        opts.rightKey.setPressed(false);
        opts.jumpKey.setPressed(false);
        opts.sneakKey.setPressed(false);
    }

    public static boolean isFlyingActive() {
        return active;
    }
}