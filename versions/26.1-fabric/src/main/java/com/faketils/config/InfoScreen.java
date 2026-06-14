package com.faketils.config;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.ChatFormatting;

public class InfoScreen extends Screen {

    private static final Identifier ICON = Identifier.fromNamespaceAndPath("faketils", "banner.png");

    private final Screen parent;
    private final Screen configScreen;

    public InfoScreen(Screen parent, Screen configScreen) {
        super(Component.literal("Faketils Info"));
        this.parent = parent;
        this.configScreen = configScreen;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Open Config"), btn ->
                        minecraft.setScreen(configScreen))
                .bounds(width / 2 - 75, height - 40, 150, 20)
                .build());
    }

    private void drawCentered(GuiGraphicsExtractor graphics, Component text, int y) {
        graphics.centeredText(font, text, width / 2, y, 0xFFFFFFFF);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        int iconWidth = 280;
        int iconHeight = 80;
        int cx = width / 2;

        graphics.blit(RenderPipelines.GUI_TEXTURED, ICON,
                cx - iconWidth / 2, height / 4 - iconHeight - 8,
                0, 0, iconWidth, iconHeight, iconWidth, iconHeight);

        int y = height / 4;
        int gap = 12;

        drawCentered(graphics, Component.literal("A Hypixel SkyBlock mod.").withStyle(ChatFormatting.GRAY), y);
        y += gap * 2;
        drawCentered(graphics, Component.literal("Red options: ").withStyle(ChatFormatting.RED)
                .append(Component.literal("Spectate bannable").withStyle(ChatFormatting.WHITE)), y);
        y += gap;
        drawCentered(graphics, Component.literal("Yellow options: ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("Macro checkable").withStyle(ChatFormatting.WHITE)), y);
        y += gap;
        drawCentered(graphics, Component.literal("Others are fine").withStyle(ChatFormatting.GRAY), y);
        y += gap * 2;
        drawCentered(graphics, Component.literal("Keybinds: F8 toggle, P pause, Backspace reset fails").withStyle(ChatFormatting.GRAY), y);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}