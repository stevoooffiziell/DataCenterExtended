package de.f4ked.datacenterextended.hardware;

import java.util.Locale;

/** Why a server cannot run. {@link #NONE} = it runs. */
public enum ServerProblem {
    NONE,
    NO_MOTHERBOARD,
    NO_PSU,
    NO_CPU,
    /** A CPU does not fit the motherboard's socket (e.g. after swapping the motherboard). */
    INCOMPATIBLE_CPU,
    /** A memory module is the wrong type for the motherboard. */
    INCOMPATIBLE_MEMORY,
    NO_MEMORY,
    PSU_OVERLOADED,
    /** Runs, but has no compute: useless in hosting / fabricating racks. */
    NO_COMPUTE,
    /** Runs, but has no hashrate: useless in mining racks. */
    NO_HASHRATE,
    NO_ENERGY,
    /** A part has 0% condition (repair it in the Service tab). */
    BROKEN_PART,
    /** ASIC miners only mine Bitcoin. */
    ASIC_WRONG_COIN,
    /** The owner could not pay the electricity bill. */
    UNPAID_BILL;

    private static final ServerProblem[] VALUES = values();

    public static ServerProblem byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
    }

    public String translationKey() {
        return "problem.datacenterextended." + name().toLowerCase(Locale.ROOT);
    }
}
