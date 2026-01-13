package com.faketils.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class RenderUtils {

    public static void renderWaypointMarker(
            MatrixStack matrices,
            Vec3d waypointPos,
            Vec3d playerPos,
            int color,
            String waypointName
    ) {
        double distance = playerPos.distanceTo(waypointPos);
        matrices.push();

        matrices.translate(
                waypointPos.x + 0.5 - playerPos.x,
                waypointPos.y - playerPos.y,
                waypointPos.z + 0.5 - playerPos.z
        );

        float red   = ((color >> 16) & 0xFF) / 255f;
        float green = ((color >>  8) & 0xFF) / 255f;
        float blue  =  (color        & 0xFF) / 255f;
        float alpha = 0.5f;

        float beamWidth  = 0.2f;
        float beamHeight = 50f;
        float baseSize   = 0.5f;

        Tessellator tessellator = Tessellator.getInstance();

        float cubeAlpha = 0.8f;
        BufferBuilder cubeBuffer = tessellator.begin(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_COLOR
        );
        Matrix4f cubeMatrix = matrices.peek().getPositionMatrix();

        float half = 0.5f;

        // Top
        cubeBuffer.vertex(cubeMatrix, -half, 1, -half).color(red, green, blue, cubeAlpha);
        cubeBuffer.vertex(cubeMatrix,  half, 1, -half).color(red, green, blue, cubeAlpha);
        cubeBuffer.vertex(cubeMatrix,  half, 1,  half).color(red, green, blue, cubeAlpha);
        cubeBuffer.vertex(cubeMatrix, -half, 1,  half).color(red, green, blue, cubeAlpha);
        // Bottom
        cubeBuffer.vertex(cubeMatrix, -half, 0,  half).color(red, green, blue, cubeAlpha);
        cubeBuffer.vertex(cubeMatrix,  half, 0,  half).color(red, green, blue, cubeAlpha);
        cubeBuffer.vertex(cubeMatrix,  half, 0, -half).color(red, green, blue, cubeAlpha);
        cubeBuffer.vertex(cubeMatrix, -half, 0, -half).color(red, green, blue, cubeAlpha);
        // North
        cubeBuffer.vertex(cubeMatrix, -half, 0, -half).color(red, green, blue, cubeAlpha);
        cubeBuffer.vertex(cubeMatrix,  half, 0, -half).color(red, green, blue, cubeAlpha);
        cubeBuffer.vertex(cubeMatrix,  half, 1, -half).color(red, green, blue, cubeAlpha);
        cubeBuffer.vertex(cubeMatrix, -half, 1, -half).color(red, green, blue, cubeAlpha);
        // South
        cubeBuffer.vertex(cubeMatrix, -half, 1,  half).color(red, green, blue, cubeAlpha);
        cubeBuffer.vertex(cubeMatrix,  half, 1,  half).color(red, green, blue, cubeAlpha);
        cubeBuffer.vertex(cubeMatrix,  half, 0,  half).color(red, green, blue, cubeAlpha);
        cubeBuffer.vertex(cubeMatrix, -half, 0,  half).color(red, green, blue, cubeAlpha);
        // West
        cubeBuffer.vertex(cubeMatrix, -half, 0,  half).color(red, green, blue, cubeAlpha);
        cubeBuffer.vertex(cubeMatrix, -half, 0, -half).color(red, green, blue, cubeAlpha);
        cubeBuffer.vertex(cubeMatrix, -half, 1, -half).color(red, green, blue, cubeAlpha);
        cubeBuffer.vertex(cubeMatrix, -half, 1,  half).color(red, green, blue, cubeAlpha);
        // East
        cubeBuffer.vertex(cubeMatrix, half, 1, half).color(red, green, blue, cubeAlpha);
        cubeBuffer.vertex(cubeMatrix, half, 1, -half).color(red, green, blue, cubeAlpha);
        cubeBuffer.vertex(cubeMatrix, half, 0, -half).color(red, green, blue, cubeAlpha);
        cubeBuffer.vertex(cubeMatrix, half, 0, half).color(red, green, blue, cubeAlpha);

        RenderLayer.getDebugQuads().draw(cubeBuffer.end());

        matrices.push();
        matrices.translate(0, 0.5, 0);
        matrices.scale(beamWidth, beamHeight, beamWidth);

        BufferBuilder beamBuffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float size = 0.5f;

        // Top
        beamBuffer.vertex(matrix, -size, 1.0f, -size).color(red, green, blue, alpha);
        beamBuffer.vertex(matrix,  size, 1.0f, -size).color(red, green, blue, alpha);
        beamBuffer.vertex(matrix,  size, 1.0f,  size).color(red, green, blue, alpha);
        beamBuffer.vertex(matrix, -size, 1.0f,  size).color(red, green, blue, alpha);
        // Bottom
        beamBuffer.vertex(matrix, -size, 0.0f,  size).color(red, green, blue, alpha);
        beamBuffer.vertex(matrix,  size, 0.0f,  size).color(red, green, blue, alpha);
        beamBuffer.vertex(matrix,  size, 0.0f, -size).color(red, green, blue, alpha);
        beamBuffer.vertex(matrix, -size, 0.0f, -size).color(red, green, blue, alpha);
        // North
        beamBuffer.vertex(matrix, -size, 0.0f, -size).color(red, green, blue, alpha);
        beamBuffer.vertex(matrix,  size, 0.0f, -size).color(red, green, blue, alpha);
        beamBuffer.vertex(matrix,  size, 1.0f, -size).color(red, green, blue, alpha);
        beamBuffer.vertex(matrix, -size, 1.0f, -size).color(red, green, blue, alpha);
        // South
        beamBuffer.vertex(matrix, -size, 1.0f,  size).color(red, green, blue, alpha);
        beamBuffer.vertex(matrix,  size, 1.0f,  size).color(red, green, blue, alpha);
        beamBuffer.vertex(matrix,  size, 0.0f,  size).color(red, green, blue, alpha);
        beamBuffer.vertex(matrix, -size, 0.0f,  size).color(red, green, blue, alpha);
        // West
        beamBuffer.vertex(matrix, -size, 0.0f,  size).color(red, green, blue, alpha);
        beamBuffer.vertex(matrix, -size, 0.0f, -size).color(red, green, blue, alpha);
        beamBuffer.vertex(matrix, -size, 1.0f, -size).color(red, green, blue, alpha);
        beamBuffer.vertex(matrix, -size, 1.0f,  size).color(red, green, blue, alpha);
        // East
        beamBuffer.vertex(matrix, size, 1.0f, size).color(red, green, blue, alpha);
        beamBuffer.vertex(matrix, size, 1.0f, -size).color(red, green, blue, alpha);
        beamBuffer.vertex(matrix, size, 0.0f, -size).color(red, green, blue, alpha);
        beamBuffer.vertex(matrix, size, 0.0f, size).color(red, green, blue, alpha);

        RenderLayer.getDebugQuads().draw(beamBuffer.end());
        matrices.pop();

        if (waypointName != null && !waypointName.isEmpty()) {
            matrices.push();
            matrices.translate(
                    0.5,
                    1.5,
                    0.5
            );
            renderWaypointName(matrices, waypointPos, playerPos, waypointName, color, distance);
            matrices.pop();
        }

        matrices.pop();
    }

    public static void renderWaypointMarker(
            MatrixStack matrices,
            Vec3d waypointPos,
            Vec3d playerPos,
            String waypointName
    ) {
        renderWaypointMarker(matrices, waypointPos, playerPos, 0xFF0000, waypointName);
    }

    public static void renderWaypointMarker(
            MatrixStack matrices,
            Vec3d waypointPos,
            Vec3d playerPos
    ) {
        renderWaypointMarker(matrices, waypointPos, playerPos, 0xFF0000, "");
    }

    public static void renderWaypointMarker(
            MatrixStack matrices,
            Vec3d waypointPos,
            Vec3d playerPos,
            int color
    ) {
        renderWaypointMarker(matrices, waypointPos, playerPos, color, "");
    }

    private static void renderWaypointName(
            MatrixStack matrices,
            Vec3d waypointPos,
            Vec3d playerPos,
            String waypointName,
            int color,
            double distance
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;

        matrices.push();

        matrices.translate(
                waypointPos.x - playerPos.x,
                waypointPos.y - playerPos.y + 2.0,
                waypointPos.z - playerPos.z
        );

        net.minecraft.client.render.Camera camera = client.gameRenderer.getCamera();

        Vec3d dir = new Vec3d(
                camera.getPos().x - waypointPos.x,
                camera.getPos().y - waypointPos.y,
                camera.getPos().z - waypointPos.z
        ).normalize();

        float yaw = (float) Math.toDegrees(Math.atan2(dir.x, dir.z));
        float pitch = (float) Math.toDegrees(Math.asin(dir.y));

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));

        float baseScale = 0.1f;
        float minScale = 0.1f;
        float maxScale = 1f;

        double scaleFactor = Math.sqrt(distance / 3.0);
        scaleFactor = Math.max(1.0, Math.min(8.0, scaleFactor));

        float dynamicScale = (float) (baseScale * scaleFactor);
        dynamicScale = Math.max(minScale, Math.min(maxScale, dynamicScale));

        matrices.scale(-dynamicScale, -dynamicScale, dynamicScale);

        Text text = Text.literal(waypointName);
        int textWidth = textRenderer.getWidth(text);

        VertexConsumerProvider.Immediate provider =
                client.getBufferBuilders().getEntityVertexConsumers();

        textRenderer.draw(
                text,
                -textWidth / 2f,
                0f,
                color,
                true,
                matrices.peek().getPositionMatrix(),
                provider,
                TextRenderer.TextLayerType.SEE_THROUGH,
                0,
                15728880
        );

        provider.draw();
        matrices.pop();
    }

    private static void renderWaypointName(
            MatrixStack matrices,
            Vec3d waypointPos,
            Vec3d playerPos,
            String waypointName,
            int color
    ) {
        double distance = playerPos.distanceTo(waypointPos);
        renderWaypointName(matrices, waypointPos, playerPos, waypointName, color, distance);
    }

    public static void renderLineToEntity(
            MatrixStack matrices,
            Entity targetEntity,
            int color,
            float lineWidth,
            float partialTicks
    ) {
        if (targetEntity == null || !targetEntity.isAlive()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        Vec3d eyePos = client.player.getLerpedPos(partialTicks)
                .add(0, client.player.getEyeHeight(client.player.getPose()), 0);

        Vec3d targetPos = targetEntity.getLerpedPos(partialTicks);

        renderLine(matrices, eyePos, targetPos, color, lineWidth);
    }

    public static void renderLine(
            MatrixStack matrices,
            Vec3d start,
            Vec3d end,
            int color,
            float lineWidth
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.gameRenderer == null || client.gameRenderer.getCamera() == null) return;

        Vec3d camPos = client.gameRenderer.getCamera().getPos();

        matrices.push();

        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        float red   = ((color >> 16) & 0xFF) / 255f;
        float green = ((color >>  8) & 0xFF) / 255f;
        float blue  =  (color        & 0xFF) / 255f;
        float alpha = ((color >> 24) & 0xFF) / 255f;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        buffer.vertex(matrix, (float)start.x, (float)start.y, (float)start.z).color(red, green, blue, alpha);
        buffer.vertex(matrix, (float)end.x,   (float)end.y,   (float)end.z).color(red, green, blue, alpha);

        RenderSystem.lineWidth(lineWidth);

        RenderLayer.getLines().draw(buffer.end());

        matrices.pop();
    }

    public static void renderLineToPos(
            MatrixStack matrices,
            Vec3d targetPos,
            int color,
            float lineWidth,
            float partialTicks
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Vec3d eyePos = client.player.getLerpedPos(partialTicks).add(0, client.player.getEyeHeight(client.player.getPose()), 0);

        renderLine(matrices, eyePos, targetPos, color, lineWidth);
    }
}