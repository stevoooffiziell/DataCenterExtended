package de.f4ked.datacenterextended.block;

import java.util.Locale;

public enum RackStatus {
    RUNNING(true, false),
    /** Servers run, but all their compute currently goes to other racks over the network. */
    EXPORTING(true, false),
    /** Hosting rack online, but no customer has signed up yet. */
    NO_CUSTOMERS(true, true),
    /** Running, but too hot: the servers are throttled. */
    OVERHEATING(true, true),
    /** Hosting rack sold more compute than it delivers: customers will cancel. */
    OVERLOADED(false, false),
    NO_SERVERS(false, false),
    NO_ENERGY(false, false),
    /** Electricity bill mode: the owner's wallet couldn't pay the bill. */
    NO_FUNDS(false, false),
    NO_TARGET(false, false),
    NOT_FABRICABLE(false, false),
    OUTPUT_FULL(false, false),
    /** Mining rack set to a coin that doesn't exist yet at the calendar date. */
    COIN_NOT_LAUNCHED(false, false),
    /** Mining / hosting rack placed without a player (e.g. /setblock): nobody to pay. */
    NO_OWNER(false, false),
    IDLE(false, false);

    private static final RackStatus[] VALUES = values();
    private final boolean ok;
    private final boolean warning;

    RackStatus(boolean ok, boolean warning) {
        this.ok = ok;
        this.warning = warning;
    }

    public boolean ok() {
        return ok;
    }

    /** Works, but needs attention (shown in yellow). */
    public boolean warning() {
        return warning;
    }

    public static RackStatus byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : NO_SERVERS;
    }

    public String translationKey() {
        return "status.datacenterextended." + name().toLowerCase(Locale.ROOT);
    }
}
