package com.faketils.events;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Random;

public class RotationHandler {
    private static float targetYaw = 0;
    private static float targetPitch = 0;
    private static boolean active = false;

    private static final float MAX_SPEED = 2.5f;
    private static final float FULL_SPEED_DISTANCE = 90.0f;
    private static final float JITTER_AMPLITUDE = 0.15f;
    private static final float CURVE_INTENSITY = 0.15f;
    private static final float CURVE_FREQUENCY = 0.15f;

    private static final Random RANDOM = new Random();
    private static float time = 0;

    public static void init() {
        FtEventBus.onEvent(FtEvent.WorldRender.class, event -> tick(event.tickDelta));
    }

    public static void setTarget(float yaw, float pitch) {
        targetYaw = yaw;
        targetPitch = pitch;
        active = true;
        time = 0;
    }

    public static void reset() {
        active = false;
    }

    public static void tick(float partialTicks) {
        if (!active) return;

        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null) return;

        float currentYaw   = player.getYaw();
        float currentPitch = player.getPitch();

        float yawDiff   = normalizeAngle(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        float maxDiff = Math.max(Math.abs(yawDiff), Math.abs(pitchDiff));
        if (maxDiff < 0.001f) {
            player.setYaw(targetYaw);
            player.setPitch(targetPitch);
            active = false;
            return;
        }

        float speed = Math.min(MAX_SPEED, MAX_SPEED * (maxDiff / FULL_SPEED_DISTANCE));
        speed *= partialTicks;
        speed *= (0.9f + 0.2f * RANDOM.nextFloat());

        float yawMove   = yawDiff   * (speed / maxDiff);
        float pitchMove = pitchDiff * (speed / maxDiff);

        time += partialTicks;
        float curveFactor = CURVE_INTENSITY * (float) Math.sin(time * CURVE_FREQUENCY);
        float perpYaw   = -pitchMove * curveFactor;
        float perpPitch =  yawMove   * curveFactor;

        yawMove   += perpYaw;
        pitchMove += perpPitch;

        yawMove   += (RANDOM.nextFloat() - 0.5f) * JITTER_AMPLITUDE * partialTicks;
        pitchMove += (RANDOM.nextFloat() - 0.5f) * JITTER_AMPLITUDE * partialTicks;

        float newYaw   = currentYaw   + yawMove;
        float newPitch = currentPitch + pitchMove;

        newPitch = Math.max(-90, Math.min(90, newPitch));

        player.setYaw(newYaw);
        player.setPitch(newPitch);

        if (Math.abs(yawDiff) < 0.1f && Math.abs(pitchDiff) < 0.1f) {
            player.setYaw(targetYaw);
            player.setPitch(targetPitch);
            active = false;
        }
    }

    private static float normalizeAngle(float angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }
}