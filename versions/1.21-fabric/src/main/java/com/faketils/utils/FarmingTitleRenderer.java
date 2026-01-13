package com.faketils.utils;

import com.faketils.features.Farming;
import com.faketils.events.FtEvent;
import com.faketils.events.FtEventBus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class FarmingTitleRenderer {

    public static void init() {
        FtEventBus.onEvent(FtEvent.HudRender.class, hud -> {
            renderTitle(hud.context);
        });
    }

    private static void renderTitle(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !Farming.isActive) return;

        String fail = Farming.getCurrentFail();
        if (fail == null || fail.isEmpty()) return;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        float scale = 3.0f;
        int x = sw / 2;
        int y = sh / 5;

        long time = System.currentTimeMillis();
        float pulse = 0.9f + 0.1f * (float)Math.sin(time / 200.0);

        int alpha = (int)(255 * pulse);
        int color = (alpha << 24) | 0x00FFAAAA;

        var matrices = ctx.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);

        ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal("FAIL: " + fail), 0, 0, color);

        matrices.popMatrix();
    }
}