package de.f4ked.datacenterextended.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** A flat dark button with an accent border on hover; the label is fitted to the width. Can act as a selected tab. */
public class FlatButton extends Button {
    private boolean selected;
    private int labelColor = Theme.TEXT;

    protected FlatButton(Builder builder) {
        super(builder);
    }

    public static FlatButton of(Component label, OnPress onPress, int x, int y, int w, int h) {
        return new FlatButton(Button.builder(label, onPress).bounds(x, y, w, h));
    }

    public FlatButton selected(boolean selected) {
        this.selected = selected;
        return this;
    }

    public FlatButton labelColor(int color) {
        this.labelColor = color;
        return this;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean hover = active && isHoveredOrFocused();
        int border = selected ? Theme.ACCENT : hover ? Theme.ACCENT_DARK : Theme.BORDER;
        int fill = !active ? Theme.BG : selected || hover ? Theme.PANEL_HOVER : Theme.PANEL_LIGHT;
        g.fill(x, y, x + w, y + h, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
        if (selected) g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, Theme.ACCENT);
        var font = Minecraft.getInstance().font;
        int color = !active ? Theme.DIM : selected ? Theme.ACCENT : labelColor;
        Ui.fitCentered(g, font, getMessage(), x + w / 2, y + (h - 8) / 2, w - 6, color);
    }
}
