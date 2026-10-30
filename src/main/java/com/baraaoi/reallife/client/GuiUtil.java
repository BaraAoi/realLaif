package com.baraaoi.reallife.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;

/** Общие помощники отрисовки окон мода. */
public final class GuiUtil {
    private GuiUtil() {}

    public static void panel(MatrixStack ms, int x, int y, int w, int h, int border, int bg) {
        AbstractGui.fill(ms, x, y, x + w, y + h, border);
        AbstractGui.fill(ms, x + 2, y + 2, x + w - 2, y + h - 2, bg);
    }

    public static void slot(MatrixStack ms, int x, int y) {
        AbstractGui.fill(ms, x - 1, y - 1, x + 17, y + 17, 0xFF373737);
        AbstractGui.fill(ms, x, y, x + 17, y + 17, 0xFFFFFFFF);
        AbstractGui.fill(ms, x, y, x + 16, y + 16, 0xFF8B8B8B);
    }

    public static void inventory(MatrixStack ms, int x, int y) {
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) slot(ms, x + c * 18, y + r * 18);
        for (int c = 0; c < 9; c++) slot(ms, x + c * 18, y + 58);
    }

    public static void small(FontRenderer font, MatrixStack ms, String s, int x, int y) {
        ms.push();
        ms.translate(x, y, 0);
        ms.scale(0.75F, 0.75F, 1.0F);
        font.drawString(ms, s, 0, 0, 0xFFFFFF);
        ms.pop();
    }
}
