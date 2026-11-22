package de.f4ked.datacenterextended.client.ui;

/** Colours of the dark "server room" GUI theme (ARGB). */
public final class Theme {
    public static final int BG = 0xFF12161C;
    public static final int PANEL = 0xFF1B222B;
    public static final int PANEL_LIGHT = 0xFF232C37;
    public static final int PANEL_HOVER = 0xFF2A3542;
    public static final int BORDER = 0xFF2E3A47;
    public static final int BORDER_LIGHT = 0xFF3B4A5A;
    public static final int INSET = 0xFF0B1016;
    public static final int ACCENT = 0xFF39C5E0;
    public static final int ACCENT_DARK = 0xFF1E6F80;
    public static final int OK = 0xFF4ADE80;
    public static final int WARN = 0xFFFBBF24;
    public static final int ERR = 0xFFF87171;
    public static final int TEXT = 0xFFE5EDF5;
    public static final int MUTED = 0xFF8A99A8;
    public static final int DIM = 0xFF5A6876;
    public static final int ENERGY = 0xFFEF4444;
    public static final int LOCKED = 0xC012161C;

    private Theme() {
    }

    /** Status colour: error, warning or ok. */
    public static int status(boolean ok, boolean warning) {
        return !ok ? ERR : warning ? WARN : OK;
    }

    /** Green above 60 %, yellow above 25 %, red below. */
    public static int condition(double condition) {
        return condition > 0.6 ? OK : condition > 0.25 ? WARN : ERR;
    }
}
