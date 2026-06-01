package com.faketils.events;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.Random;

public class RotationHandler {
    private static float targetYaw = 0;
    private static float targetPitch = 0;
    public static boolean active = false;

    private static final float DEGREES_PER_SECOND = 720f;
    private static final float FULL_SPEED_DISTANCE = 90.0f;
    private static final float JITTER_AMPLITUDE = 0.0f;
    private static final float CURVE_INTENSITY = 0.15f;
    private static final float CURVE_FREQUENCY = 0.15f;

    private static final Random RANDOM = new Random();
    private static float time = 0;
    private static long lastTickMs = -1;

    public static void init() {
        FtEventBus.onEvent(FtEvent.WorldRender.class, event -> tick());
    }

    public static void setTarget(float yaw, float pitch) {
        targetYaw = yaw;
        targetPitch = pitch;
        active = true;
        time = 0;
        lastTickMs = -1;
    }

    public static void reset() {
        active = false;
        lastTickMs = -1;
    }

    public static void tick() {
        if (!active) return;

        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return;

        long now = System.currentTimeMillis();
        if (lastTickMs == -1) {
            lastTickMs = now;
            return;
        }

        float dtSeconds = Math.min((now - lastTickMs) / 1000f, 0.05f);
        lastTickMs = now;

        float currentYaw   = player.getYRot();
        float currentPitch = player.getXRot();

        float yawDiff   = normalizeAngle(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        float maxDiff = Math.max(Math.abs(yawDiff), Math.abs(pitchDiff));
        if (maxDiff < 0.1f) {
            player.setYRot(targetYaw);
            player.setXRot(targetPitch);
            active = false;
            return;
        }

        float speed = DEGREES_PER_SECOND * dtSeconds;
        speed *= Math.min(1f, maxDiff / FULL_SPEED_DISTANCE);
        speed *= (0.9f + 0.2f * RANDOM.nextFloat());

        float yawMove   = yawDiff   * (speed / maxDiff);
        float pitchMove = pitchDiff * (speed / maxDiff);

        time += dtSeconds;
        float curveFactor = CURVE_INTENSITY * (float) Math.sin(time * CURVE_FREQUENCY);
        float perpYaw   = -pitchMove * curveFactor;
        float perpPitch =  yawMove   * curveFactor;

        yawMove   += perpYaw;
        pitchMove += perpPitch;

        yawMove   += (RANDOM.nextFloat() - 0.5f) * JITTER_AMPLITUDE * dtSeconds;
        pitchMove += (RANDOM.nextFloat() - 0.5f) * JITTER_AMPLITUDE * dtSeconds;

        player.setYRot(currentYaw + yawMove);
        player.setXRot(Math.max(-90, Math.min(90, currentPitch + pitchMove)));

        if (Math.abs(yawDiff) < 0.1f && Math.abs(pitchDiff) < 0.1f) {
            player.setYRot(targetYaw);
            player.setXRot(targetPitch);
            active = false;
        }
    }

    private static float normalizeAngle(float angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }
}