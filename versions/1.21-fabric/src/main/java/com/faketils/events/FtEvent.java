package com.faketils.events;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.Nullable;

public class FtEvent {

    @Nullable
    public final DrawContext context;

    protected FtEvent(@Nullable DrawContext context) {
        this.context = context;
    }

    public static class WorldRender extends FtEvent {
        public final WorldRenderContext worldContext;

        public WorldRender(WorldRenderContext context) {
            super(null);
            this.worldContext = context;
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