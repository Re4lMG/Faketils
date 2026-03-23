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

    private static final float VERTICAL_TOLERANCE  = 0.6f;
    private static final float APPROACH_DISTANCE   = 8.0f;
    private static final float SLOW_STOP_DISTANCE  = 1.2f;
    private static final float AOTV_MIN_DISTANCE   = 24.0f;
    private static final float AOTV_MAX_DISTANCE   = 100.0f;
    private static final int   AOTV_COOLDOWN_TICKS = 7;
    private static final int   SLOT_SWITCH_DELAY   = 2 + new Random().nextInt(3);

    private static int frontClearTicks = 0;
    private static boolean lastFrontClear = false;

    private static final float STRAFE_CHANCE_FAR  = 0.015f;
    private static final float STRAFE_CHANCE_NEAR = 0.08f;
    private static final int   STRAFE_TICKS       = 4 + new Random().nextInt(5);
    private static final int   JUMP_BOOST_DELAY   = 180;

    private static final float TARGET_Y_OFFSET = 4.5f;

    private static final Random RANDOM = new Random();

    private static int strafeTimer    = 0;
    private static boolean strafeLeft = false;
    private static long lastJumpTime  = 0;

    public static List<Vec3d> path;
    private static int pathIndex = 0;

    private static int originalSlot          = -1;
    private static int aotvSlot              = -1;
    private static int slotSwitchTimer       = 0;
    private static int aotvCooldown          = 0;
    private static boolean awaitingRightClick = false;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(FlyHandler::onTick);
    }

    public static void setTarget(Vec3d pos) {
        if (MinecraftClient.getInstance().player == null) return;
        PlayerInventoryAccessor inv = (PlayerInventoryAccessor) MinecraftClient.getInstance().player.getInventory();
        originalSlot = inv.getSelectedSlot();
        targetPos = pos;
        active = true;
        strafeTimer = 0;
    }

    public static void stop() {
        active = false;
        targetPos = null;
        resetKeys();
        resetAotvState();
        clearPath();
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
        pathIndex = 1;
        active = true;
    }

    public static void flyTo(Vec3d goal) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        clearPath();

        BlockPos start = mc.player.getBlockPos();
        BlockPos end   = BlockPos.ofFloored(goal);

        List<BlockPos> raw = Pathfinder.findPath(start, end, mc.world, 20000);

        if (raw == null || raw.isEmpty()) {
            targetPos = goal;
            active = true;
            return;
        }

        List<Vec3d> vec3dPath = new ArrayList<>();
        for (BlockPos bp : raw) {
            vec3dPath.add(new Vec3d(bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5));
        }

        targetPos = goal;
        setPath(vec3dPath);
    }

    private static void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || !active || targetPos == null) return;

        Vec3d pos   = player.getEntityPos();
        Vec3d delta = targetPos.subtract(pos);
        double horizDist = delta.horizontalLength();
        double fullDist  = delta.length();

        if (path != null && !path.isEmpty()) {
            tickPath(client, player, pos, delta, fullDist);
        } else {
            tickDirect(client, player, pos, delta, horizDist, fullDist);
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
        }

        if (fullDist < APPROACH_DISTANCE) {
            stop();
            if (Farming.killingPests) {
                Farming.handleKilling();
            }
        }

        boolean fallingFast = player.getVelocity().y < -0.1;
        if (fallingFast && System.currentTimeMillis() - lastJumpTime > JUMP_BOOST_DELAY) {
            setFlying(true);
        }
    }

    private static void tickPath(MinecraftClient client, ClientPlayerEntity player,
                                 Vec3d pos, Vec3d delta, double fullDist) {
        Vec3d node       = path.get(pathIndex).add(0, 0.05, 0);
        Vec3d nodeRaw    = path.get(pathIndex);
        Vec3d deltaPath  = node.subtract(pos);
        Vec3d deltaPathRaw = nodeRaw.subtract(pos);
        float fullDistPath    = (float) deltaPath.length();
        float fullDistPathRaw = (float) deltaPathRaw.length();

        float targetYawPath;
        float targetPitchPath;

        boolean frontClear = isFrontClear(player, 4.0);
        if (frontClear == lastFrontClear) {
            frontClearTicks++;
            } else {
                frontClearTicks = 0;
                lastFrontClear = frontClear;
            }
            boolean useDirectLook = frontClearTicks > 3 ? lastFrontClear : !lastFrontClear;

            if (useDirectLook) {
            targetYawPath   = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90f;
            targetPitchPath = (float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalLength()));
        } else {
            targetYawPath   = (float) Math.toDegrees(Math.atan2(deltaPathRaw.z, deltaPathRaw.x)) - 90f;
            targetPitchPath = (float) -Math.toDegrees(Math.atan2(deltaPathRaw.y, deltaPathRaw.horizontalLength()));
        }

        RotationHandler.setTarget(targetYawPath, targetPitchPath);

        float yawDiff    = Math.abs(wrapDegrees(player.getYaw() - targetYawPath));
        boolean facingTarget = yawDiff < 35f;

        boolean wantUp   = deltaPath.y > VERTICAL_TOLERANCE;
        boolean wantDown = deltaPath.y < -VERTICAL_TOLERANCE;

        if (isDirectionBlocked(player, 0.0f, +1.2f)) wantUp   = false;
        if (isDirectionBlocked(player, 0.0f, -0.01f)) wantDown = false;

        if (player.isOnGround() && wantUp) {
            player.jump();
            lastJumpTime = System.currentTimeMillis();
        }

        KeyBinding forward = client.options.forwardKey;
        KeyBinding jump    = client.options.jumpKey;
        KeyBinding sneak   = client.options.sneakKey;

        forward.setPressed(facingTarget && fullDist > SLOW_STOP_DISTANCE);
        player.setSprinting(facingTarget);
        jump.setPressed(wantUp);
        sneak.setPressed(wantDown);

        if (fullDistPath > AOTV_MIN_DISTANCE && fullDistPath < AOTV_MAX_DISTANCE
                && aotvCooldown <= 0 && slotSwitchTimer == 0 && !player.isSneaking()
                && yawDiff <= 20f) {
            aotvSlot = findAotvSlot(player);
            if (aotvSlot != -1 && isFrontClear(player, 12.0)) {
                player.getInventory().setSelectedSlot(aotvSlot);
                slotSwitchTimer = SLOT_SWITCH_DELAY;
                awaitingRightClick = true;
                aotvCooldown = AOTV_COOLDOWN_TICKS + RANDOM.nextInt(15);
            }
        }

        if (fullDistPathRaw <= 0.5) {
            pathIndex++;
            if (pathIndex >= path.size()) {
                path = null;
            }
        }
    }

    private static void tickDirect(MinecraftClient client, ClientPlayerEntity player,
                                   Vec3d pos, Vec3d delta, double horizDist, double fullDist) {
        float targetYaw   = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90f;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizDist));

        float yawDiff   = Math.abs(wrapDegrees(player.getYaw() - targetYaw));
        float pitchDiff = Math.abs(player.getPitch() - targetPitch);

        RotationHandler.setTarget(targetYaw, targetPitch);

        if (fullDist > AOTV_MIN_DISTANCE && fullDist < AOTV_MAX_DISTANCE
                && aotvCooldown <= 0 && slotSwitchTimer == 0 && !player.isSneaking()
                && yawDiff <= 20f && pitchDiff <= 20f) {
            aotvSlot = findAotvSlot(player);
            if (aotvSlot != -1 && isFrontClear(player, 12.0)) {
                player.getInventory().setSelectedSlot(aotvSlot);
                slotSwitchTimer = SLOT_SWITCH_DELAY;
                awaitingRightClick = true;
                aotvCooldown = AOTV_COOLDOWN_TICKS + RANDOM.nextInt(15);
            }
        }

        Vec3d eDelta  = targetPos.add(0, TARGET_Y_OFFSET, 0).subtract(pos);
        boolean wantUp   = eDelta.y > VERTICAL_TOLERANCE;
        boolean wantDown = eDelta.y < -VERTICAL_TOLERANCE;

        if (isDirectionBlocked(player, 0.0f, +1.2f)) wantUp   = false;
        if (isDirectionBlocked(player, 0.0f, -0.01f)) wantDown = false;

        KeyBinding forward = client.options.forwardKey;
        KeyBinding jump    = client.options.jumpKey;
        KeyBinding sneak   = client.options.sneakKey;
        KeyBinding left    = client.options.leftKey;
        KeyBinding right   = client.options.rightKey;

        if (strafeTimer > 0) {
            strafeTimer--;
            left.setPressed(strafeLeft);
            right.setPressed(!strafeLeft);
        } else {
            left.setPressed(false);
            right.setPressed(false);
            float chance = (fullDist > APPROACH_DISTANCE * 1.5) ? STRAFE_CHANCE_FAR : STRAFE_CHANCE_NEAR;
            if (RANDOM.nextFloat() < chance) {
                strafeLeft = RANDOM.nextBoolean();
                strafeTimer = STRAFE_TICKS;
            }
        }

        forward.setPressed(fullDist > SLOW_STOP_DISTANCE);
        player.setSprinting(true);
        jump.setPressed(wantUp);
        sneak.setPressed(wantDown);

        if (player.isOnGround() && wantUp) {
            player.jump();
            lastJumpTime = System.currentTimeMillis();
        }
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
        Vec3d start = player.getEyePos();
        Vec3d end   = start.add(player.getRotationVector().multiply(distance));

        RaycastContext ctx = new RaycastContext(start, end, ShapeType.COLLIDER, FluidHandling.NONE, player);
        BlockHitResult hit = player.getEntityWorld().raycast(ctx);
        return hit.getType() != HitResult.Type.BLOCK || hit.getBlockPos().getY() < player.getY() - 1;
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