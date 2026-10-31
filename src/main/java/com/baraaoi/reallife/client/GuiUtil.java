package com.baraaoi.reallife.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;

/** Общие помощники отрисовки окон мода. */
public final class GuiUtil {
    private GuiUtil() {}

    public static void panel(GuiGraphicsExtractor ms, int x, int y, int w, int h, int border, int bg) {
        ms.fill(x, y, x + w, y + h, border);
        ms.fill(x + 2, y + 2, x + w - 2, y + h - 2, bg);
    }

    public static void slot(GuiGraphicsExtractor ms, int x, int y) {
        ms.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
        ms.fill(x, y, x + 17, y + 17, 0xFFFFFFFF);
        ms.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
    }

    public static void inventory(GuiGraphicsExtractor ms, int x, int y) {
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) slot(ms, x + c * 18, y + r * 18);
        for (int c = 0; c < 9; c++) slot(ms, x + c * 18, y + 58);
    }

    /** Текст без тени; цвет без альфы (0xRRGGBB) делается непрозрачным. */
    public static void text(GuiGraphicsExtractor ms, Font font, String s, int x, int y, int color) {
        if ((color & 0xFF000000) == 0) color |= 0xFF000000;
        ms.text(font, s, x, y, color, false);
    }

    public static void small(Font font, GuiGraphicsExtractor ms, String s, int x, int y) {
        ms.pose().pushMatrix();
        ms.pose().translate((float) (x), (float) (y));
        ms.pose().scale(0.75F, 0.75F);
        GuiUtil.text(ms, font, s, 0, 0, 0xFFFFFF);
        ms.pose().popMatrix();
    }
}
