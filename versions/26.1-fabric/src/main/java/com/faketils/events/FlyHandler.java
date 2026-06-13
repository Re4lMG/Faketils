package com.faketils.events;

import com.faketils.features.Farming;
import com.faketils.mixin.PlayerInventoryAccessor;
import com.faketils.pathing.Pathfinder;
import com.faketils.utils.Utils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FlyHandler {

    private static Vec3 targetPos = null;
    private static boolean active = false;
    private static boolean decelerating = false;

    private static volatile boolean pathfindingInProgress = false;

    private static final float  VERTICAL_TOLERANCE  = 0.6f;
    private static final float  APPROACH_DISTANCE   = 8.0f;
    private static final float  SLOW_STOP_DISTANCE  = 8.0f;
    private static final float  AOTV_MIN_DISTANCE   = 14.0f;
    private static final float  AOTV_MAX_DISTANCE   = 1000.0f;
    private static final int    AOTV_COOLDOWN_TICKS = 4;
    private static final int    SLOT_SWITCH_DELAY   = 2 + new Random().nextInt(3);
    private static final double STOPPING_THRESHOLD  = 0.75;
    private static final float  ANGLE_THRESHOLD     = 20.0f;

    private static int     frontClearTicks = 0;
    private static boolean lastFrontClear  = false;

    private static final int   JUMP_BOOST_DELAY   = 180;

    private static final float TARGET_Y_OFFSET = 4.5f;

    private static final double MOVE_KEY_THRESHOLD = 0.35;

    private static final Random RANDOM = new Random();

    private static long    lastJumpTime = 0;

    public static List<Vec3> path;
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
    private static final long PATH_REQUEST_COOLDOWN_MS = 500;

    private static long    stuckCheckTime     = 0;
    private static Vec3    lastPosCheck       = Vec3.ZERO;
    private static int     ticksAtLastPos     = 0;
    private static long    stuckEscapeEndTime = 0;
    private static boolean isStuckEscaping   = false;
    private static float   escapeYaw         = 0f;

    private enum VerticalDirection { HIGHER, LOWER, NONE }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(FlyHandler::onTick);
    }

    public static void setTarget(Vec3 pos) {
        if (Minecraft.getInstance().player == null) return;
        PlayerInventoryAccessor inv = (PlayerInventoryAccessor) Minecraft.getInstance().player.getInventory();
        originalSlot = inv.getSelectedSlot();
        targetPos = pos;
        active = true;
        decelerating = false;
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

    public static void setPath(List<Vec3> newPath) {
        path = newPath;
        pathIndex = Math.min(1, path.size() - 1);
        active = true;
        decelerating = false;
    }

    public static void flyTo(Vec3 goal) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (pathfindingInProgress) return;

        targetPos = goal;
        active = true;
        decelerating = false;

        BlockPos start = mc.player.blockPosition();
        BlockPos end   = BlockPos.containing(goal);

        pathfindingInProgress = true;
        new Thread(() -> {
            List<BlockPos> raw = Pathfinder.findPath(start, end, mc.level, 20000);
            mc.execute(() -> {
                pathfindingInProgress = false;
                if (raw == null || raw.isEmpty()) return;

                List<Vec3> vec3Path = new ArrayList<>();
                for (BlockPos bp : raw) {
                    vec3Path.add(new Vec3(bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5));
                }
                clearPath();
                setPath(smoothPath(vec3Path));
            });
        }, "pathfinder").start();
    }

    private static void onTick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !active || targetPos == null) return;

        Vec3 pos   = player.position();
        Vec3 delta = targetPos.subtract(pos);
        double horizDist = delta.horizontalDistance();
        double fullDist  = delta.length();

        if (decelerating) {
            if (Math.abs(player.getDeltaMovement().x) <= 0.05 && Math.abs(player.getDeltaMovement().z) <= 0.05) {
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
            if (now - lastPathRequestTime > PATH_REQUEST_COOLDOWN_MS) {
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
                inv.setSelectedSlot(Farming.vacuumSlot);
            }
            resetKeys();
        }

        boolean fallingFast = player.getDeltaMovement().y < -0.1;
        if (fallingFast && System.currentTimeMillis() - lastJumpTime > JUMP_BOOST_DELAY) {
            setFlying(true);
        }
    }

    private static void tickPath(Minecraft client, LocalPlayer player,
                                 Vec3 pos, Vec3 delta, double fullDist) {

        if (path == null || path.isEmpty() || pathIndex >= path.size()) return;

        boolean frontClear = isFrontClear(player, 2.0);

        if (frontClear == lastFrontClear) {
            frontClearTicks++;
        } else {
            frontClearTicks = 0;
            lastFrontClear = frontClear;
        }
        boolean stableFrontClear = frontClearTicks > 3 ? lastFrontClear : !lastFrontClear;

        Vec3 rawNode    = path.get(pathIndex);
        Vec3 moveTarget = stableFrontClear ? rawNode.add(0, 4, 0) : rawNode;
        Vec3 node       = moveTarget.add(0, 0.05, 0);

        Vec3 deltaPath    = node.subtract(pos);
        Vec3 deltaPathRaw = moveTarget.subtract(pos);
        Vec3 deltaLook    = rawNode.subtract(pos);

        float fullDistPath    = (float) deltaPath.length();
        float fullDistPathRaw = (float) deltaPathRaw.length();

        float targetYawPath   = (float) Math.toDegrees(Math.atan2(deltaLook.z, deltaLook.x)) - 90f;
        float targetPitchPath = (float) -Math.toDegrees(Math.atan2(deltaLook.y, deltaLook.horizontalDistance()));

        RotationHandler.setTarget(targetYawPath, targetPitchPath);

        float   yawDiff      = Math.abs(wrapDegrees(player.getYRot() - targetYawPath));
        float   pitchDiff    = Math.abs(player.getXRot() - targetPitchPath);
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

        if (facingTarget && player.onGround() && wantUp) {
            player.jumpFromGround();
            lastJumpTime = System.currentTimeMillis();
        }

        KeyMapping jump  = client.options.keyJump;
        KeyMapping sneak = client.options.keyShift;

        boolean shouldMove = facingTarget && fullDist > SLOW_STOP_DISTANCE;
        float relAngle = wrapDegrees(player.getYRot() - targetYawPath);
        setMovementKeysForAngle(client, relAngle, shouldMove, fullDist);
        player.setSprinting(shouldMove && Math.abs(relAngle) < 50f);

        if (shouldMove) {
            jump.setDown(wantUp);
            sneak.setDown(wantDown);
        } else {
            jump.setDown(false);
            sneak.setDown(false);
        }

        PlayerInventoryAccessor inv = (PlayerInventoryAccessor) player.getInventory();

        if (facingTarget && fullDistPath > AOTV_MIN_DISTANCE && fullDistPath < AOTV_MAX_DISTANCE
                && aotvCooldown <= 0 && slotSwitchTimer == 0) {
            aotvSlot = findAotvSlot(player);
            if (aotvSlot != -1 && isFrontClear(player, 12.0)) {
                inv.setSelectedSlot(aotvSlot);
                slotSwitchTimer = SLOT_SWITCH_DELAY;
                awaitingRightClick = true;
                aotvCooldown = AOTV_COOLDOWN_TICKS + RANDOM.nextInt(15);
            }
        }

        double rawGroundDist = rawNode.subtract(pos).length();
        if (rawGroundDist <= 1.2) {
            int pathSize = path.size();
            pathIndex++;
            clearPath();
            if (pathIndex >= pathSize) {
                stop();
                return;
            }
        }
    }

    private static void tickDirect(Minecraft client, LocalPlayer player,
                                   Vec3 pos, Vec3 delta, double horizDist, double fullDist) {
        float targetYaw   = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90f;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizDist));

        float yawDiff   = Math.abs(wrapDegrees(player.getYRot() - targetYaw));
        float pitchDiff = Math.abs(player.getXRot() - targetPitch);
        boolean facingTarget = yawDiff < ANGLE_THRESHOLD && pitchDiff < ANGLE_THRESHOLD;

        RotationHandler.setTarget(targetYaw, targetPitch);

        if (willArriveAtDestinationAfterStopping(player, targetPos)) {
            decelerating = true;
            stop();
            return;
        }

        PlayerInventoryAccessor inv = (PlayerInventoryAccessor) player.getInventory();

        if (facingTarget && fullDist > AOTV_MIN_DISTANCE && fullDist < AOTV_MAX_DISTANCE
                && aotvCooldown <= 0 && slotSwitchTimer == 0) {
            aotvSlot = findAotvSlot(player);
            if (aotvSlot != -1 && isFrontClear(player, 12.0)) {
                inv.setSelectedSlot(aotvSlot);
                slotSwitchTimer = SLOT_SWITCH_DELAY;
                awaitingRightClick = true;
                aotvCooldown = AOTV_COOLDOWN_TICKS + RANDOM.nextInt(15);
            }
        }

        Vec3 eDelta = targetPos.add(0, TARGET_Y_OFFSET, 0).subtract(pos);

        VerticalDirection vert = shouldChangeHeight(pos, targetPos.add(0, TARGET_Y_OFFSET, 0), targetYaw);
        boolean wantUp   = eDelta.y > VERTICAL_TOLERANCE || vert == VerticalDirection.HIGHER;
        boolean wantDown = eDelta.y < -VERTICAL_TOLERANCE || vert == VerticalDirection.LOWER;

        if (isDirectionBlocked(player, 0.0f, +1.2f)) wantUp   = false;
        if (isDirectionBlocked(player, 0.0f, -0.01f)) wantDown = false;

        KeyMapping jump  = client.options.keyJump;
        KeyMapping sneak = client.options.keyShift;

        boolean shouldMove = facingTarget && fullDist > SLOW_STOP_DISTANCE;
        float relAngle = wrapDegrees(player.getYRot() - targetYaw);
        setMovementKeysForAngle(client, relAngle, shouldMove, fullDist);
        player.setSprinting(shouldMove && Math.abs(relAngle) < 50f);

        if (shouldMove) {
            jump.setDown(wantUp);
            sneak.setDown(wantDown);
        } else {
            jump.setDown(false);
            sneak.setDown(false);
        }

        if (player.onGround() && wantUp && shouldMove) {
            player.jumpFromGround();
            lastJumpTime = System.currentTimeMillis();
        }
    }

    private static void setMovementKeysForAngle(Minecraft client, float relAngle, boolean shouldMove, double distanceToTarget) {
        KeyMapping forward = client.options.keyUp;
        KeyMapping back    = client.options.keyDown;
        KeyMapping left    = client.options.keyLeft;
        KeyMapping right   = client.options.keyRight;

        if (distanceToTarget <= 2) {
            forward.setDown(false);
            back.setDown(true);
            left.setDown(false);
            right.setDown(false);
            return;
        }

        if (!shouldMove) {
            forward.setDown(false);
            back.setDown(false);
            left.setDown(false);
            right.setDown(false);
            return;
        }

        double rad = Math.toRadians(relAngle);
        double fwd = Math.cos(rad);
        double str = Math.sin(rad);

        forward.setDown(fwd >  MOVE_KEY_THRESHOLD);
        back.setDown(   fwd < -MOVE_KEY_THRESHOLD);
        left.setDown(   str >  MOVE_KEY_THRESHOLD);
        right.setDown(  str < -MOVE_KEY_THRESHOLD);
    }

    private static List<Vec3> smoothPath(List<Vec3> originalPath) {
        if (originalPath.size() < 3) return new ArrayList<>(originalPath);

        List<Vec3> smoothed = new ArrayList<>();
        smoothed.add(originalPath.get(0));

        int lowerIndex = 0;
        while (lowerIndex < originalPath.size() - 2) {
            Vec3 start     = originalPath.get(lowerIndex);
            Vec3 lastValid = originalPath.get(lowerIndex + 1);

            for (int upperIndex = lowerIndex + 2; upperIndex < originalPath.size(); upperIndex++) {
                Vec3 end = originalPath.get(upperIndex);
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

    private static boolean traversable(Vec3 from, Vec3 to) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;

        Vec3[] offsets = {
                new Vec3(0.05, 0.1, 0.05), new Vec3(0.05, 0.1, 0.95),
                new Vec3(0.95, 0.1, 0.05), new Vec3(0.95, 0.1, 0.95),
                new Vec3(0.05, 1.9, 0.05), new Vec3(0.05, 1.9, 0.95),
                new Vec3(0.95, 1.9, 0.05), new Vec3(0.95, 1.9, 0.95)
        };

        for (Vec3 offset : offsets) {
            BlockHitResult hit = mc.level.clip(new ClipContext(
                    from.add(offset), to.add(offset),
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    mc.player
            ));
            if (hit.getType() == HitResult.Type.BLOCK) return false;
        }
        return true;
    }

    private static boolean willArriveAtDestinationAfterStopping(LocalPlayer player, Vec3 target) {
        return predictStoppingPosition(player).distanceTo(target) < STOPPING_THRESHOLD;
    }

    private static Vec3 predictStoppingPosition(LocalPlayer player) {
        Vec3 pos = player.position();
        Vec3 vel = player.getDeltaMovement();
        for (int i = 0; i < 30; i++) {
            vel = vel.scale(0.91);
            pos = pos.add(vel);
            if (vel.horizontalDistance() < 0.01) break;
        }
        return pos;
    }

    private static boolean checkForStuck(Vec3 pos) {
        long now = System.currentTimeMillis();
        if (now < stuckCheckTime) return false;

        double diff = pos.distanceToSqr(lastPosCheck);
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

    private static void handleStuck(LocalPlayer player) {
        isStuckEscaping = false;
        ticksAtLastPos = 0;
        resetKeys();

        if (targetPos != null) {
            flyTo(targetPos);
        }
    }

    private static void handleEscapeMovement(LocalPlayer player) {
        RotationHandler.setTarget(escapeYaw, 0f);
        Minecraft.getInstance().options.keyUp.setDown(true);
    }

    private static VerticalDirection shouldChangeHeight(Vec3 current, Vec3 next, float yaw) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return VerticalDirection.NONE;

        Vec3 dir      = new Vec3(-Math.sin(Math.toRadians(yaw)),      0, Math.cos(Math.toRadians(yaw))).normalize();
        Vec3 dirLeft  = new Vec3(-Math.sin(Math.toRadians(yaw - 20)), 0, Math.cos(Math.toRadians(yaw - 20))).normalize();
        Vec3 dirRight = new Vec3(-Math.sin(Math.toRadians(yaw + 20)), 0, Math.cos(Math.toRadians(yaw + 20))).normalize();

        double h = mc.player.getBbHeight();

        if (rayHitsBlock(current.add(dir.scale(0.75)).add(0, h + 0.1, 0), dir) ||
                rayHitsBlock(current.add(dirLeft.scale(0.75)).add(0, h + 0.1, 0), dirLeft) ||
                rayHitsBlock(current.add(dirRight.scale(0.75)).add(0, h + 0.1, 0), dirRight)) {
            return VerticalDirection.LOWER;
        }

        if (rayHitsBlock(current.add(dir.scale(0.75)).add(0, -0.1, 0), dir) ||
                rayHitsBlock(current.add(dirLeft.scale(0.75)).add(0, -0.1, 0), dirLeft) ||
                rayHitsBlock(current.add(dirRight.scale(0.75)).add(0, -0.1, 0), dirRight)) {
            return VerticalDirection.HIGHER;
        }

        return VerticalDirection.NONE;
    }

    private static boolean rayHitsBlock(Vec3 from, Vec3 direction) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        Vec3 to = from.add(direction.scale(1.0));
        BlockHitResult hit = mc.level.clip(new ClipContext(
                from, to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mc.player
        ));
        return hit.getType() == HitResult.Type.BLOCK;
    }

    public static int findAotvSlot(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                String name = stack.getHoverName().getString().toLowerCase();
                if (name.contains("aspect of the void") || name.contains("aspect of the end")) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static void doAotvRightClick() {
        Minecraft mc = Minecraft.getInstance();
        Utils.simulateUseItem(mc.gameMode);
    }

    private static float wrapDegrees(float degrees) {
        degrees = degrees % 360.0F;
        if (degrees >= 180.0F) degrees -= 360.0F;
        if (degrees < -180.0F) degrees += 360.0F;
        return degrees;
    }

    private static boolean isFrontClear(LocalPlayer player, double distance) {
        float yaw = player.getYRot();

        float[] yawOffsets = { 0f, -30f, 30f, -60f, 60f };
        double[] yOffsets = { 0.1, 0.9, 1.7 };

        for (int i = 0; i < yawOffsets.length; i++) {
            float checkYaw = yaw + yawOffsets[i];
            double checkDist = i < 3 ? distance : distance * 0.5;
            Vec3 dir = new Vec3(
                    -Math.sin(Math.toRadians(checkYaw)),
                    0,
                    Math.cos(Math.toRadians(checkYaw))
            ).normalize();

            for (double yOff : yOffsets) {
                Vec3 start = player.position().add(0, yOff, 0);
                Vec3 end   = start.add(dir.scale(checkDist));
                ClipContext ctx = new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
                BlockHitResult hit = player.level().clip(ctx);
                if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().getY() >= player.getY() - 1) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void setFlying(boolean enable) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.getConnection() == null) return;
        Abilities abilities = player.getAbilities();
        abilities.flying = enable;
        mc.getConnection().send(new ServerboundPlayerAbilitiesPacket(abilities));
    }

    private static boolean isDirectionBlocked(LocalPlayer player, float yawOffset, float yOffset) {
        Vec3 look  = player.getLookAngle();
        Vec3 start = player.getEyePosition();
        Vec3 end   = start.add(look.scale(1.2)).add(0, yOffset, 0);
        ClipContext ctx = new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        HitResult hit = player.level().clip(ctx);
        return hit != null && hit.getType() == HitResult.Type.BLOCK;
    }

    private static void resetKeys() {
        var opts = Minecraft.getInstance().options;
        opts.keyUp.setDown(false);
        opts.keyDown.setDown(false);
        opts.keyLeft.setDown(false);
        opts.keyRight.setDown(false);
        opts.keyJump.setDown(false);
        opts.keyShift.setDown(false);
    }

    public static boolean isFlyingActive() {
        return active;
    }
}