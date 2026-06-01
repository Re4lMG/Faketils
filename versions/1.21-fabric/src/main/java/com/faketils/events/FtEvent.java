package com.faketils.events;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Camera;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class FtEvent {

    @Nullable
    public final GuiGraphicsExtractor guiGraphics;

    protected FtEvent(@Nullable GuiGraphicsExtractor guiGraphics) {
        this.guiGraphics = guiGraphics;
    }

    public static class WorldRender extends FtEvent {
        public final Matrix4f positionMatrix;
        public final Matrix4f modelViewMatrix;
        public final GpuBufferSlice projectionMatrix;
        public final float tickDelta;
        public final Camera camera;

        public WorldRender(
                Matrix4f positionMatrix,
                Matrix4f modelViewMatrix,
                GpuBufferSlice projectionMatrix,
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

        public HudRender(GuiGraphicsExtractor guiGraphics, float tickDelta) {
            super(guiGraphics);
            this.tickDelta = tickDelta;
        }
    }
}