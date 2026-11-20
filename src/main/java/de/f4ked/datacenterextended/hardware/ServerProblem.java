package de.f4ked.datacenterextended.hardware;

import java.util.Locale;

/** Why a server cannot run. {@link #NONE} = it runs. */
public enum ServerProblem {
    NONE,
    NO_MOTHERBOARD,
    NO_PSU,
    PSU_OVERLOADED,
    /** Runs, but has no compute – useless in hosting / fabricating racks. */
    NO_COMPUTE,
    /** Runs, but has no hashrate – useless in mining racks. */
    NO_HASHRATE,
    NO_ENERGY;

    private static final ServerProblem[] VALUES = values();

    public static ServerProblem byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
    }

    public String translationKey() {
        return "problem.datacenterextended." + name().toLowerCase(Locale.ROOT);
    }
}
