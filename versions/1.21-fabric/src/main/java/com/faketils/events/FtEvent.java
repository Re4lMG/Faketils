package com.faketils.events;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class FtEvent {

    @Nullable
    public final DrawContext context;

    protected FtEvent(@Nullable DrawContext context) {
        this.context = context;
    }

    public static class WorldRender extends FtEvent {
        public final Matrix4f positionMatrix;
        public final Matrix4f modelViewMatrix;
        public final Matrix4f projectionMatrix;
        public final float tickDelta;
        public final Camera camera;

        public WorldRender(
                Matrix4f positionMatrix,
                Matrix4f modelViewMatrix,
                Matrix4f projectionMatrix,
                float tickDelta,
                Camera camera
        ) {
            super(null);
            this.positionMatrix = positionMatrix;
            this.modelViewMatrix = modelViewMatrix;
            this.projectionMatrix = projectionMatrix;
            this.tickDelta = tickDelta;
            this.camera = camera;
        }
    }

    public static class HudRender extends FtEvent {
        public final float tickDelta;

        public HudRender(DrawContext context, float tickDelta) {
            super(context);
            this.tickDelta = tickDelta;
        }
    }
}