package com.ailudick.capitalismmod.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Shared visual style for all GUI screens.
 */
public final class GuiStyles {
    public static final int BG_TOP = 0xFF2B2B36;     // gradient top
    public static final int BG_BOTTOM = 0xFF17171F;  // gradient bottom
    public static final int ACCENT = 0xFFF2C14E;     // gold accent (titles/highlights)
    public static final int TEXT = 0xFFECECF2;       // primary text
    public static final int TEXT_DIM = 0xFF9B9BAC;   // secondary text

    private GuiStyles() {
    }

    /** Draws the modern gradient background. */
    public static void drawBackground(GuiGraphics graphics, int left, int top, int width, int height) {
        graphics.fillGradient(left, top, left + width, top + height, BG_TOP, BG_BOTTOM);
    }
}
