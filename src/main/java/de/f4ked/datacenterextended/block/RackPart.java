package de.f4ked.datacenterextended.block;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** Position of a rack block inside its merged rack (visual only – logic always resolves the stack). */
public enum RackPart implements StringRepresentable {
    SINGLE,
    BOTTOM,
    MIDDLE,
    TOP;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
