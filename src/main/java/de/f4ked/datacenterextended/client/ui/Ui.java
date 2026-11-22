package de.f4ked.datacenterextended.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Texture-free drawing helpers of the dark theme. All text that has to stay inside a box goes through
 * {@link #fit}: it is scaled down (to at most {@value #MIN_SCALE}) and then cut with an ellipsis.
 */
public final class Ui {
    public static final float MIN_SCALE = 0.7F;
    public static final int HEADER_H = 16;

    private Ui() {
    }

    // ------------------------------------------------------------------ frames

    /** Window background with a 1 px border and an accent line under the header. */
    public static void window(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, Theme.BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, Theme.BG);
        g.fill(x + 1, y + 1, x + w - 1, y + HEADER_H, Theme.PANEL);
        g.fill(x + 1, y + HEADER_H, x + w - 1, y + HEADER_H + 1, Theme.ACCENT_DARK);
    }

    /** A raised card. */
    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, Theme.BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, Theme.PANEL);
    }

    /** A sunken "screen" area. */
    public static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, Theme.BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, Theme.INSET);
    }

    /** An 18x18 slot frame; (x, y) is the item position like {@code Slot.x/y}. */
    public static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, Theme.BORDER_LIGHT);
        g.fill(x, y, x + 16, y + 16, Theme.INSET);
    }

    /** Vertical gauge filling from the bottom, with quarter tick marks. */
    public static void gauge(GuiGraphicsExtractor g, int x, int y, int w, int h, double fraction, int color) {
        inset(g, x - 1, y - 1, w + 2, h + 2);
        int filled = (int) Math.round(clamp(fraction) * h);
        if (filled > 0) g.fill(x, y + h - filled, x + w, y + h, color);
        for (int i = 1; i < 4; i++) {
            int ty = y + h * i / 4;
            g.fill(x, ty, x + 2, ty + 1, Theme.BORDER_LIGHT);
        }
    }

    /** Horizontal progress bar. */
    public static void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, double fraction, int color) {
        inset(g, x - 1, y - 1, w + 2, h + 2);
        int filled = (int) Math.round(clamp(fraction) * w);
        if (filled > 0) g.fill(x, y, x + filled, y + h, color);
    }

    /** A 3x3 status LED with a dim glow. */
    public static void led(GuiGraphicsExtractor g, int x, int y, int color) {
        g.fill(x - 1, y - 1, x + 4, y + 4, (color & 0x00FFFFFF) | 0x40000000);
        g.fill(x, y, x + 3, y + 3, color);
    }

    /** A small pill with text, right-aligned at {@code right}. Returns its left edge. */
    public static int chip(GuiGraphicsExtractor g, Font font, Component text, int right, int y, int color, int maxW) {
        int w = Math.min(maxW, font.width(text) + 8);
        int x = right - w;
        int bg = (color & 0x00FFFFFF) | 0x33000000;
        g.fill(x + 1, y, x + w - 1, y + 11, bg);
        g.fill(x, y + 1, x + 1, y + 10, bg);
        g.fill(x + w - 1, y + 1, x + w, y + 10, bg);
        fit(g, font, text, x + 4, y + 2, w - 8, color);
        return x;
    }

    // ------------------------------------------------------------------ text

    /** Draws text inside {@code maxW}: scaled down if needed, then cut with an ellipsis. */
    public static void fit(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int maxW, int color) {
        if (maxW <= 0) return;
        int width = font.width(text);
        if (width <= maxW) {
            g.text(font, text, x, y, color, false);
            return;
        }
        float scale = Math.max(MIN_SCALE, (float) maxW / width);
        Component shown = text;
        if (width * scale > maxW) {
            int room = (int) (maxW / scale) - font.width("…");
            shown = Component.literal(font.plainSubstrByWidth(text.getString(), Math.max(0, room)) + "…").withStyle(text.getStyle());
        }
        g.pose().pushMatrix();
        g.pose().translate(x, y + (1 - scale) * 4);
        g.pose().scale(scale, scale);
        g.text(font, shown, 0, 0, color, false);
        g.pose().popMatrix();
    }

    public static void fit(GuiGraphicsExtractor g, Font font, String text, int x, int y, int maxW, int color) {
        fit(g, font, Component.literal(text), x, y, maxW, color);
    }

    /** Right-aligned fitted text ending at {@code right}. */
    public static void fitRight(GuiGraphicsExtractor g, Font font, Component text, int right, int y, int maxW, int color) {
        int w = Math.min(maxW, font.width(text));
        fit(g, font, text, right - w, y, maxW, color);
    }

    public static void fitRight(GuiGraphicsExtractor g, Font font, String text, int right, int y, int maxW, int color) {
        fitRight(g, font, Component.literal(text), right, y, maxW, color);
    }

    /** Centred fitted text. */
    public static void fitCentered(GuiGraphicsExtractor g, Font font, Component text, int centerX, int y, int maxW, int color) {
        int w = Math.min(maxW, font.width(text));
        fit(g, font, text, centerX - w / 2, y, maxW, color);
    }

    /** A row "label ... value": muted label on the left, value right-aligned; both share {@code w}. */
    public static void row(GuiGraphicsExtractor g, Font font, Component label, Component value, int x, int y, int w, int valueColor) {
        int valueW = Math.min(font.width(value), w * 2 / 3);
        fitRight(g, font, value, x + w, y, w * 2 / 3, valueColor);
        fit(g, font, label, x, y, w - valueW - 6, Theme.MUTED);
    }

    public static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
