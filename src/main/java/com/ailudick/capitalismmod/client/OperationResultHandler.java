package com.ailudick.capitalismmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class OperationResultHandler {
    private OperationResultHandler() {
    }

    public static void show(boolean success, String message) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen previous = minecraft.screen;
        minecraft.setScreen(new ResultScreen(previous, success, message));
    }

    private static final class ResultScreen extends Screen {
        private final Screen previous;
        private final boolean success;
        private final String message;

        private ResultScreen(Screen previous, boolean success, String message) {
            super(Component.literal(success ? "业务完成" : "业务失败"));
            this.previous = previous;
            this.success = success;
            this.message = message;
        }

        @Override
        protected void init() {
            addRenderableWidget(Button.builder(Component.literal("确定"), button -> minecraft.setScreen(previous))
                    .bounds(width / 2 - 50, height / 2 + 24, 100, 20).build());
        }

        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xFF0B1018);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xFF0B1018);
            int left = width / 2 - 150;
            int top = height / 2 - 60;
            int right = width / 2 + 150;
            int bottom = height / 2 + 60;
            graphics.fill(left, top, right, bottom, 0xFF34445D);
            graphics.fill(left, top, right, top + 2, 0xFF586B86);
            graphics.fill(left, bottom - 2, right, bottom, 0xFF111722);
            graphics.fill(left, top, left + 2, bottom, 0xFF586B86);
            graphics.fill(right - 2, top, right, bottom, 0xFF111722);
            super.render(graphics, mouseX, mouseY, partialTick);
            int color = success ? 0x55FF55 : 0xFF5555;
            graphics.drawCenteredString(font, title, width / 2, height / 2 - 28, color);
            graphics.drawCenteredString(font, Component.literal(message), width / 2, height / 2 - 8, 0xFFFFFF);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
