package com.faketils.utils;

import com.faketils.Faketils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.*;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.*;

public class Utils {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final RenderLayer FILLED_LAYER = RenderLayer.getDebugFilledBox();
    private static final RenderLayer OUTLINE_LAYER = RenderLayer.getLines();

    public static String stripColorCodes(String s) {
        return s == null ? "" : Formatting.strip(s);
    }

    public static String cleanSB(String s) {
        if (s == null) return "";

        String stripped = Formatting.strip(s);

        return stripped
                .replaceAll("[^\\p{Print}]", "")
                .trim();
    }

    public static void log(String message) {
        if (!Faketils.config.debug) return;
        if (mc.player != null) {
            mc.player.sendMessage(
                    Text.literal("§7[§bFaketils§7] §f" + message),
                    false
            );
        }
    }

    public static List<String> getSidebarLines() {
        List<String> lines = new ArrayList<>();

        if (mc.world == null) return lines;

        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective objective =
                scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);

        if (objective == null) return lines;

        List<Text> textLines = new ArrayList<>();

        for (ScoreboardEntry entry : scoreboard.getScoreboardEntries(objective)) {
            String name = entry.name().toString();
            Team team = scoreboard.getTeam(name);
            if (team == null) continue;

            Text line = Text.empty()
                    .append(team.getPrefix())
                    .append(team.getSuffix());

            if (!line.getString().trim().isEmpty()) {
                textLines.add(line);
            }
        }

        Collections.reverse(textLines);

        for (Text t : textLines) {
            lines.add(t.getString());
        }

        return lines;
    }

    public static boolean isInSkyblock() {
        if (mc.world == null || mc.player == null) return false;
        if (mc.isInSingleplayer()) return false;

        ScoreboardObjective obj = mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);

        if (obj == null) return false;

        String objectiveName = stripColorCodes(obj.getDisplayName().getString()).toLowerCase(Locale.ROOT);
        return objectiveName.contains("skyblock");
    }

    public static class FtVec {
        public final double x;
        public final double y;
        public final double z;

        public FtVec(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Vec3d toVec3d() {
            return new Vec3d(x, y, z);
        }

        public FtVec add(double x, double y, double z) {
            return new FtVec(this.x + x, this.y + y, this.z + z);
        }

        public Vec3d center() {
            return new Vec3d(x + 0.5, y + 0.5, z + 0.5);
        }
    }

    private static void withLineWidth(float lineWidth, Runnable action) {
        float oldLineWidth = GL11.glGetFloat(GL11.GL_LINE_WIDTH);
        GL11.glLineWidth(lineWidth);
        try {
            action.run();
        } finally {
            GL11.glLineWidth(oldLineWidth);
        }
    }

    public static void drawFilledBox(WorldRenderContext context,
            FtVec pos,
            double width,
            double height,
            double depth,
            float[] colorComponents,
            float alpha,
            boolean throughWalls
    ) {
        MatrixStack matrices = context.matrixStack();
        matrices.push();

        Camera camera = context.camera();
        Vec3d cameraPos = camera.getPos();
        matrices.translate(pos.x + 0.5 - cameraPos.x, pos.y - cameraPos.y, pos.z + 0.5 - cameraPos.z);

        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) {
            matrices.pop();
            return;
        }

        RenderLayer layer = throughWalls ? OUTLINE_LAYER : FILLED_LAYER;
        VertexConsumer buffer = consumers.getBuffer(layer);

        double halfWidth = width / 2.0;
        double halfDepth = depth / 2.0;

        float minX = (float) -halfWidth;
        float maxX = (float) halfWidth;
        float minY = 0.0f;
        float maxY = (float) height;
        float minZ = (float) -halfDepth;
        float maxZ = (float) halfDepth;

        float r = colorComponents[0];
        float g = colorComponents[1];
        float b = colorComponents[2];

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        if (throughWalls) {
            // Bottom square
            buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, alpha).normal(0f, 1f, 0f);
            buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, alpha).normal(0f, 1f, 0f);

            buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, alpha).normal(0f, 1f, 0f);
            buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, alpha).normal(0f, 1f, 0f);

            buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, alpha).normal(0f, 1f, 0f);
            buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, alpha).normal(0f, 1f, 0f);

            buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, alpha).normal(0f, 1f, 0f);
            buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, alpha).normal(0f, 1f, 0f);

            // Top square
            buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, alpha).normal(0f, 1f, 0f);
            buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, alpha).normal(0f, 1f, 0f);

            buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, alpha).normal(0f, 1f, 0f);
            buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, alpha).normal(0f, 1f, 0f);

            buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, alpha).normal(0f, 1f, 0f);
            buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, alpha).normal(0f, 1f, 0f);

            buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, alpha).normal(0f, 1f, 0f);
            buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, alpha).normal(0f, 1f, 0f);

            // Vertical edges
            buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, alpha).normal(0f, 1f, 0f);
            buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, alpha).normal(0f, 1f, 0f);

            buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, alpha).normal(0f, 1f, 0f);
            buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, alpha).normal(0f, 1f, 0f);

            buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, alpha).normal(0f, 1f, 0f);
            buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, alpha).normal(0f, 1f, 0f);

            buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, alpha).normal(0f, 1f, 0f);
            buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, alpha).normal(0f, 1f, 0f);
        } else {
            // Bottom face
            buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, alpha);
            buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, alpha);
            buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, alpha);
            buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, alpha);

            // Top face
            buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, alpha);
            buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, alpha);
            buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, alpha);
            buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, alpha);

            // North (minZ)
            buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, alpha);
            buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, alpha);
            buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, alpha);
            buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, alpha);

            // South (maxZ)
            buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, alpha);
            buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, alpha);
            buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, alpha);
            buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, alpha);

            // West (minX)
            buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, alpha);
            buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, alpha);
            buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, alpha);
            buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, alpha);

            // East (maxX)
            buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, alpha);
            buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, alpha);
            buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, alpha);
            buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, alpha);
        }

        matrices.pop();
    }

    public static void drawString(
            WorldRenderContext context,
            BlockPos pos,
            double yOffset,
            String text,
            int color,
            boolean shadow,
            double scale,
            boolean throughWalls
    ) {
        MatrixStack matrices = context.matrixStack();
        matrices.push();

        var camera = context.camera();
        var cameraPos = camera.getPos();
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer textRenderer = mc.textRenderer;

        // Calculate position in world
        Camera c = mc.gameRenderer.getCamera();
        double viewerX = c.getPos().x;
        double viewerY = c.getPos().y;
        double viewerZ = c.getPos().z;

        // Calculate vector from viewer to waypoint
        double x = pos.getX() + 0.5 - viewerX;
        double y = pos.getY() + 0.5 - viewerY;
        double z = pos.getZ() + 0.5 - viewerZ;

        double distSq = x * x + y * y + z * z;
        double dist = Math.sqrt(distSq);

        // Scale position if too far (like 1.8 code does)
        if (distSq > 144) { // 12 blocks squared
            double scaleFactor = 12.0 / dist;
            x *= scaleFactor;
            y *= scaleFactor;
            z *= scaleFactor;
        }

        // Translate to position
        matrices.translate(x, y + yOffset, z);

        // Apply camera rotations (like 1.8 code)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

        // Apply the additional 2.0 scale from 1.8 renderWaypointText
        matrices.scale(2.0f, 2.0f, 2.0f);

        // Base scale (like 1.8 nametag scale)
        float f = 1.6f;
        float f1 = 0.016666668f * f;
        matrices.scale(-f1, -f1, f1);

        // Center the text
        float xOffset = -textRenderer.getWidth(text) / 2f;

        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null || !(consumers instanceof VertexConsumerProvider.Immediate)) {
            matrices.pop();
            return;
        }

        VertexConsumerProvider.Immediate immediate = (VertexConsumerProvider.Immediate) consumers;

        // Create DrawContext with Immediate
        DrawContext drawContext = new DrawContext(mc, immediate);

        Text literalText = Text.literal(text);

        // Draw the text
        drawContext.drawText(
                textRenderer,
                literalText,
                (int) xOffset,
                0,
                color,
                shadow
        );

        // IMPORTANT: DrawContext doesn't automatically flush the buffer
        immediate.draw();

        matrices.pop();
    }

    public static void drawLineFromCursor(
            WorldRenderContext context,
            FtVec target,
            float[] color,
            float lineWidth,
            boolean throughWalls,
            float alpha
    ) {
        MatrixStack matrices = context.matrixStack();
        matrices.push();

        Camera camera = context.camera();
        Vec3d cameraPos = camera.getPos();

        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) {
            matrices.pop();
            return;
        }

        Vec3d startPos = cameraPos.add(Vec3d.fromPolar(camera.getPitch(), camera.getYaw()));
        Vec3d endPos = target.center().add(0.0, 0.5, 0.0);

        VertexConsumer buffer = consumers.getBuffer(OUTLINE_LAYER);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        withLineWidth(lineWidth, () -> {
            buffer.vertex(matrix, (float) startPos.x, (float) startPos.y, (float) startPos.z)
                    .color(color[0], color[1], color[2], alpha)
                    .normal(0f, 1f, 0f);

            buffer.vertex(matrix, (float) endPos.x, (float) endPos.y, (float) endPos.z)
                    .color(color[0], color[1], color[2], alpha)
                    .normal(0f, 1f, 0f);
        });

        matrices.pop();
    }

    public static void drawLineFromCursor(
            WorldRenderContext context,
            FtVec target,
            float[] color,
            float lineWidth,
            boolean throughWalls
    ) {
        drawLineFromCursor(context, target, color, lineWidth, throughWalls, 0.5f);
    }
}