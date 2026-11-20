package de.f4ked.datacenterextended.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Texture-free vanilla-style GUI primitives shared by all screens of the mod. */
public final class GuiDraw {
    public static final int PANEL = 0xFFC6C6C6;
    public static final int LIGHT = 0xFFFFFFFF;
    public static final int SHADOW = 0xFF555555;
    public static final int SLOT_DARK = 0xFF373737;
    public static final int SLOT_FILL = 0xFF8B8B8B;
    public static final int SCREEN_BG = 0xFF101820;
    public static final int TEXT_OK = 0xFF55FF7F;
    public static final int TEXT_INFO = 0xFFB0E0FF;
    public static final int TEXT_WARN = 0xFFFFC857;
    public static final int TEXT_ERR = 0xFFFF5555;
    public static final int LOCKED = 0x90601010;

    private GuiDraw() {
    }

    public static void window(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
        g.fill(x + 1, y + 1, x + w - 2, y + 3, LIGHT);
        g.fill(x + 1, y + 1, x + 3, y + h - 2, LIGHT);
        g.fill(x + 2, y + h - 3, x + w - 1, y + h - 1, SHADOW);
        g.fill(x + w - 3, y + 2, x + w - 1, y + h - 1, SHADOW);
    }

    /** Draws an 18x18 slot frame; (x, y) is the slot's item position (like {@code Slot.x/y}). */
    public static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, SLOT_DARK);
        g.fill(x, y, x + 17, y + 17, LIGHT);
        g.fill(x, y, x + 16, y + 16, SLOT_FILL);
    }

    public static void lockedOverlay(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 16, y + 16, LOCKED);
    }

    /** Dark "monitor" panel with a bevel. */
    public static void monitor(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, SLOT_DARK);
        g.fill(x, y, x + w + 1, y + h + 1, LIGHT);
        g.fill(x, y, x + w, y + h, SCREEN_BG);
    }

    public static void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, double fraction, int color) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, SLOT_DARK);
        g.fill(x, y, x + w, y + h, 0xFF202020);
        int filled = (int) Math.round(Math.max(0, Math.min(1, fraction)) * w);
        if (filled > 0) {
            g.fill(x, y, x + filled, y + h, color);
        }
    }
}
