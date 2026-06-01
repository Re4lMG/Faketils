package com.faketils.utils;

import com.faketils.features.Farming;
import com.faketils.events.FtEvent;
import com.faketils.events.FtEventBus;
import org.joml.Matrix3x2fStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class FarmingTitleRenderer {

    public static void init() {
        FtEventBus.onEvent(FtEvent.HudRender.class, hud -> {
            renderTitle(hud.guiGraphics);
        });
    }

    private static void renderTitle(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !Farming.isActive) return;

        String fail = Farming.getCurrentFail();
        if (fail == null || fail.isEmpty()) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        float scale = 9.0f;
        int x = sw / 2;
        int y = sh / 5;

        long time = System.currentTimeMillis();
        float pulse = 0.9f + 0.1f * (float)Math.sin(time / 200.0);

        int alpha = (int)(255 * pulse);
        int color = (alpha << 24) | 0x00FFAAAA;

        Component textComponent = Component.literal("FAIL: " + fail);
        int textWidth = mc.font.width(textComponent);

        Matrix3x2fStack poseStack = graphics.pose();
        poseStack.pushMatrix();
        poseStack.translate(x, y);
        poseStack.scale(scale, scale);

        graphics.textWithBackdrop(
                mc.font,
                textComponent,
                -textWidth / 2,
                0,
                textWidth,
                color
        );

        poseStack.popMatrix();
    }
}