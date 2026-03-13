package com.faketils.events;

import com.faketils.features.Farming;
import com.faketils.mixin.PlayerInventoryAccessor;
import com.faketils.utils.Utils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdatePlayerAbilitiesC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class FlyHandler {

    private static Vec3d targetPos = null;
    private static boolean active = false;

    private static final float VERTICAL_TOLERANCE    = 0.6f;
    private static final float APPROACH_DISTANCE     = 10.0f;
    private static final float SLOW_STOP_DISTANCE    = 1.2f;
    private static final float AOTV_MIN_DISTANCE     = 24.0f;
    private static final float AOTV_MAX_DISTANCE     = 100.0f;
    private static final int   AOTV_COOLDOWN_TICKS   = 20;
    private static final int   SLOT_SWITCH_DELAY     = 2 + new Random().nextInt(3);

    private static final float STRAFE_CHANCE_FAR     = 0.015f;
    private static final float STRAFE_CHANCE_NEAR    = 0.08f;
    private static final int   STRAFE_TICKS          = 4 + new Random().nextInt(5);
    private static final int   JUMP_BOOST_DELAY      = 180;

    private static final Random RANDOM = new Random();

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "AOTV-SwitchBack-Delay");
        t.setDaemon(true);
        return t;
    });

    private static int strafeTimer      = 0;
    private static boolean strafeLeft   = false;
    private static long lastJumpTime    = 0;
    private static boolean wasFallingFast = false;

    private static int originalSlot     = -1;
    private static int aotvSlot         = -1;
    private static int slotSwitchTimer  = 0;
    private static int aotvCooldown     = 0;
    private static boolean awaitingRightClick = false;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> onTick(client));
    }

    public static void setTarget(Vec3d pos) {
        if (MinecraftClient.getInstance().player == null) return;
        PlayerInventoryAccessor inv = (PlayerInventoryAccessor) MinecraftClient.getInstance().player.getInventory();
        originalSlot = inv.getSelectedSlot();
        targetPos = pos;
        active = true;
        strafeTimer = 0;
        //resetKeys();
        //resetAotvState();
    }

    public static void stop() {
        active = false;
        targetPos = null;
        resetKeys();
        resetAotvState();
        //setFlying(false);
    }

    private static void resetAotvState() {
        originalSlot = -1;
        aotvSlot = -1;
        slotSwitchTimer = 0;
        awaitingRightClick = false;
    }

    private static void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || !active || targetPos == null) return;

        Vec3d pos = player.getEntityPos();
        Vec3d delta = targetPos.subtract(pos);
        Vec3d delta2 = targetPos.add(0, 4.5, 0).subtract(pos);
        double horizDist = delta2.horizontalLength();
        double fullDist = delta2.length();

        PlayerInventoryAccessor inv = (PlayerInventoryAccessor) player.getInventory();

        if (aotvCooldown > 0) aotvCooldown--;
        if (slotSwitchTimer > 0) {
            slotSwitchTimer--;
            if (slotSwitchTimer == 0 && awaitingRightClick) {
                doAotvRightClick();
                awaitingRightClick = false;
//                new Thread(() -> {
//                    try { Thread.sleep(80); } catch (InterruptedException ignored) {}
//                    ClientPlayerEntity p = MinecraftClient.getInstance().player;
//                    if (p != null && originalSlot != -1 && inv.getSelectedSlot() != originalSlot) {
//                        p.getInventory().setSelectedSlot(originalSlot);
//                    }
//                }).start();
            }
        }

        if (fullDist < APPROACH_DISTANCE && Farming.vacuumSlot != -1) {
            if (inv.getSelectedSlot() != Farming.vacuumSlot) {
                player.getInventory().setSelectedSlot(Farming.vacuumSlot);
            }
        }

        if (player.isOnGround()) {
            player.jump();
        }

        boolean fallingFast = player.getVelocity().y < -0.1;
         //&& !wasFallingFast
        if (fallingFast) {
            if (System.currentTimeMillis() - lastJumpTime > JUMP_BOOST_DELAY) {
                setFlying(true);
            }
        }
        //
        wasFallingFast = fallingFast;
        if (!fallingFast && player.getAbilities().flying) {
            //player.jump();
            //new Thread(() -> {
            //    try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                //setFlying(true);
            //}).start();
        }

        float targetYaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90f;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizDist));

        float yawDiff = Math.abs(wrapDegrees(player.getYaw() - targetYaw));
        float pitchDiff = Math.abs(player.getPitch() - targetPitch);

        if (yawDiff < 10f && pitchDiff < 10f) {
            RotationHandler.reset();
        } else {
            RotationHandler.setTarget(targetYaw, targetPitch);
        }

        if (fullDist > AOTV_MIN_DISTANCE && fullDist < AOTV_MAX_DISTANCE
                && aotvCooldown <= 0 && slotSwitchTimer == 0 && !player.isSneaking() && yawDiff <= 40f
                && pitchDiff <= 40f) {

            aotvSlot = findAotvSlot(player);
            if (aotvSlot != -1 && isFrontClear(player, 12.0)) {
                player.getInventory().setSelectedSlot(aotvSlot);
                slotSwitchTimer = SLOT_SWITCH_DELAY;
                awaitingRightClick = true;
                aotvCooldown = AOTV_COOLDOWN_TICKS + RANDOM.nextInt(15);
            }
        }

        boolean wantUp = delta2.y > VERTICAL_TOLERANCE;
        boolean wantDown = delta2.y < -VERTICAL_TOLERANCE;

        boolean blockedUp   = isDirectionBlocked(player, 0.0f, +1.2f);
        boolean blockedDown = isDirectionBlocked(player, 0.0f, -0.8f);

        if (blockedUp)   wantUp   = false;
        if (blockedDown) wantDown = false;

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

        jump.setPressed(wantUp || (player.isOnGround() && wantUp && fullDist > 2.0));
        sneak.setPressed(wantDown);

        if (player.isOnGround() && wantUp && delta2.y > 0.6) {
            player.jump();
            lastJumpTime = System.currentTimeMillis();
        }

        if (fullDist < APPROACH_DISTANCE) {
            //if (RANDOM.nextFloat() < 0.25f) forward.setPressed(false);
            resetKeys();
        }

        if (fullDist < 0.9 && Math.abs(delta2.y) < 0.4) {
            //stop();
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
        Vec3d look = player.getRotationVector().multiply(distance);
        Vec3d end = start.add(look);

        RaycastContext ctx = new RaycastContext(
                start, end,
                ShapeType.COLLIDER,
                FluidHandling.NONE,
                player
        );
        BlockHitResult hit = player.getEntityWorld().raycast(ctx);
        return hit.getType() != HitResult.Type.BLOCK || hit.getBlockPos().getY() < player.getY() - 1;
    }

    private static void setFlying(boolean enable) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;

        if (player == null || mc.getNetworkHandler() == null) return;

        PlayerAbilities abilities = player.getAbilities();

        abilities.flying = enable;

        mc.getNetworkHandler().sendPacket(
                new UpdatePlayerAbilitiesC2SPacket(abilities)
        );
    }

    private static boolean isDirectionBlocked(ClientPlayerEntity player, float yawOffset, float yOffset) {
        Vec3d look = player.getRotationVector().rotateY((float) Math.toRadians(yawOffset));
        Vec3d start = player.getEyePos();
        Vec3d end = start.add(look.multiply(1.2)).add(0, yOffset, 0);

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