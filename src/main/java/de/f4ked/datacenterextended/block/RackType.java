package de.f4ked.datacenterextended.block;

import java.util.Locale;

public enum RackType {
    /** GPU hashrate of its own servers -> coins in the owner's wallet. Not connected to the compute network. */
    MINING(false, 0),
    /** Compute -> dollars in the owner's wallet; storage in the servers gives a bonus. */
    HOSTING(true, 0),
    /** Compute -> copies of the target item, by file size. */
    FABRICATING(true, 3);

    private static final RackType[] VALUES = values();
    private final boolean networked;
    private final int outputSlots;

    RackType(boolean networked, int outputSlots) {
        this.networked = networked;
        this.outputSlots = outputSlots;
    }

    /** Whether this rack joins compute networks (and connects to data cables). */
    public boolean networked() {
        return networked;
    }

    public int outputSlots() {
        return outputSlots;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT) + "_rack";
    }

    public static RackType byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : MINING;
    }
}
