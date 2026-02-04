package com.faketils.utils;

import com.faketils.events.FtEvent;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class RenderUtils {

    public static void renderWaypointMarker(
            Vec3d waypointPos,
            Vec3d cameraPos,
            int color,
            String waypointName,
            FtEvent.WorldRender event
    ) {
        float red   = ((color >> 16) & 0xFF) / 255f;
        float green = ((color >>  8) & 0xFF) / 255f;
        float blue  =  (color        & 0xFF) / 255f;
        float alpha = 0.5f;

        Vec3d rel = waypointPos.subtract(cameraPos);

        Matrix4f baseMatrix = new Matrix4f(event.positionMatrix)
                .translate((float) rel.x, (float) rel.y, (float) rel.z);


        Tessellator tessellator = Tessellator.getInstance();

        float cubeAlpha = 0.8f;
        float half = 0.5f;

        BufferBuilder cube = tessellator.begin(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_COLOR
        );

        addCube(cube, baseMatrix, half, red, green, blue, cubeAlpha);
        RenderLayer.getDebugQuads().draw(cube.end());

        Matrix4f beamMatrix = new Matrix4f(baseMatrix)
                .translate(0f, 0.5f, 0f)
                .scale(0.2f, 50f, 0.2f);

        BufferBuilder beam = tessellator.begin(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_COLOR
        );

        addCube(beam, beamMatrix, 0.5f, red, green, blue, alpha);
        RenderLayer.getDebugQuads().draw(beam.end());

        if (waypointName != null && !waypointName.isEmpty()) {
            renderWaypointName(
                    waypointPos,
                    cameraPos,
                    waypointName,
                    color,
                    event
            );
        }
    }

    private static void renderWaypointName(
            Vec3d waypointPos,
            Vec3d cameraPos,
            String name,
            int color,
            FtEvent.WorldRender event
    ) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;

        double distance = waypointPos.distanceTo(cameraPos);

        Vec3d rel = waypointPos.subtract(cameraPos).add(0, 2.0, 0);

        float scale = (float) Math.max(0.02, Math.min(0.1, distance * 0.02));

        Quaternionf rotation = new Quaternionf()
                .rotateY((float) Math.toRadians(180f + event.camera.getYaw()))
                .rotateX((float) Math.toRadians(-event.camera.getPitch()));

        Matrix4f textMatrix = new Matrix4f(event.positionMatrix)
                .translate((float) rel.x, (float) rel.y, (float) rel.z)
                .rotate(rotation)
                .scale(-scale, -scale, scale);

        Text text = Text.literal(name);
        float x = -tr.getWidth(text) / 2f;

        VertexConsumerProvider.Immediate provider =
                mc.getBufferBuilders().getEntityVertexConsumers();

        tr.draw(
                text,
                x,
                0,
                color,
                true,
                textMatrix,
                provider,
                TextRenderer.TextLayerType.SEE_THROUGH,
                0,
                15728880
        );

        provider.draw();
    }

    public static void renderLine(
            Vec3d start,
            Vec3d end,
            int color,
            float width,
            FtEvent.WorldRender event
    ) {
        float red   = ((color >> 16) & 0xFF) / 255f;
        float green = ((color >>  8) & 0xFF) / 255f;
        float blue  =  (color        & 0xFF) / 255f;
        float alpha = ((color >> 24) & 0xFF) / 255f;

        Vec3d cam = event.camera.getPos();

        Vec3d startRel = start.subtract(cam);
        Vec3d endRel = end.subtract(cam);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(
                VertexFormat.DrawMode.LINES,
                VertexFormats.POSITION_COLOR
        );

        Matrix4f startMatrix = new Matrix4f(event.positionMatrix)
                .translate((float) startRel.x, (float) startRel.y, (float) startRel.z);

        Matrix4f endMatrix = new Matrix4f(event.positionMatrix)
                .translate((float) endRel.x, (float) endRel.y, (float) endRel.z);


        buffer.vertex(startMatrix, (float) start.x, (float) start.y, (float) start.z)
                .color(red, green, blue, alpha);
        buffer.vertex(endMatrix, (float) end.x, (float) end.y, (float) end.z)
                .color(red, green, blue, alpha);

        RenderSystem.lineWidth(width);
        RenderLayer.getLines().draw(buffer.end());
    }

    public static void renderLineToEntity(
            Entity entity,
            int color,
            float width,
            FtEvent.WorldRender event
    ) {
        if (entity == null || !entity.isAlive()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        Vec3d eye = mc.player.getEyePos();
        Vec3d target = entity.getLerpedPos(event.tickDelta);

        renderLine(eye, target, color, width, event);
    }

    private static void addCube(
            BufferBuilder buffer,
            Matrix4f m,
            float h,
            float r,
            float g,
            float b,
            float a
    ) {
        // Top
        quad(buffer, m, -h,1,-h,  h,1,-h,  h,1,h, -h,1,h, r,g,b,a);
        // Bottom
        quad(buffer, m, -h,0,h,  h,0,h,  h,0,-h, -h,0,-h, r,g,b,a);
        // North
        quad(buffer, m, -h,0,-h,  h,0,-h,  h,1,-h, -h,1,-h, r,g,b,a);
        // South
        quad(buffer, m, -h,1,h,  h,1,h,  h,0,h, -h,0,h, r,g,b,a);
        // West
        quad(buffer, m, -h,0,h, -h,0,-h, -h,1,-h, -h,1,h, r,g,b,a);
        // East
        quad(buffer, m, h,1,h,  h,1,-h,  h,0,-h,  h,0,h, r,g,b,a);
    }

    private static void quad(
            BufferBuilder b,
            Matrix4f m,
            float x1,float y1,float z1,
            float x2,float y2,float z2,
            float x3,float y3,float z3,
            float x4,float y4,float z4,
            float r,float g,float bl,float a
    ) {
        b.vertex(m,x1,y1,z1).color(r,g,bl,a);
        b.vertex(m,x2,y2,z2).color(r,g,bl,a);
        b.vertex(m,x3,y3,z3).color(r,g,bl,a);
        b.vertex(m,x4,y4,z4).color(r,g,bl,a);
    }
}