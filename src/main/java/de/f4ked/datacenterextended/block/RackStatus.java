package de.f4ked.datacenterextended.block;

import java.util.Locale;

public enum RackStatus {
    RUNNING(true),
    /** Servers run, but all their compute currently goes to other racks over the network. */
    EXPORTING(true),
    NO_SERVERS(false),
    NO_ENERGY(false),
    NO_TARGET(false),
    NOT_FABRICABLE(false),
    OUTPUT_FULL(false),
    /** Mining / hosting rack placed without a player (e.g. /setblock): nobody to pay. */
    NO_OWNER(false),
    IDLE(false);

    private static final RackStatus[] VALUES = values();
    private final boolean ok;

    RackStatus(boolean ok) {
        this.ok = ok;
    }

    public boolean ok() {
        return ok;
    }

    public static RackStatus byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : NO_SERVERS;
    }

    public String translationKey() {
        return "status.datacenterextended." + name().toLowerCase(Locale.ROOT);
    }
}
