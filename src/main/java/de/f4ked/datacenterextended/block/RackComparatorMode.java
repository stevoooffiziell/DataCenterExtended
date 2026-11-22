package de.f4ked.datacenterextended.block;

import java.util.Locale;

/** What a comparator next to a rack reads. */
public enum RackComparatorMode {
    /** 15 = running, 8 = running with a warning (overheating, no customers), 0 = stopped. */
    STATUS,
    /** 0 at ambient temperature up to 15 at the critical temperature. */
    TEMPERATURE,
    /** FE fill of all segments (electricity bill mode: 15 = paid, 0 = unpaid). */
    ENERGY;

    private static final RackComparatorMode[] VALUES = values();

    public static RackComparatorMode byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : STATUS;
    }

    public String translationKey() {
        return "comparator.datacenterextended." + name().toLowerCase(Locale.ROOT);
    }
}
