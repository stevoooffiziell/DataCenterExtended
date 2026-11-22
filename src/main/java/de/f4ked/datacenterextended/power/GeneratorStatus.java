package de.f4ked.datacenterextended.power;

import java.util.Locale;

/** Why a generator produces what it produces (shown in its GUI, Jade and the multimeter). */
public enum GeneratorStatus {
    RUNNING, WEATHER, THROTTLED, BUFFER_FULL,
    NIGHT, NO_SKY, TOO_LOW, BLOCKED, NO_WATER, NO_FUEL, NO_LAVA, NO_RODS, SCRAM;

    public String translationKey() {
        return "status.datacenterextended.generator." + name().toLowerCase(Locale.ROOT);
    }

    /** Producing at all (possibly reduced). */
    public boolean producing() {
        return ordinal() <= BUFFER_FULL.ordinal();
    }

    /** Producing, but less than it could. */
    public boolean warning() {
        return this == WEATHER || this == THROTTLED || this == BUFFER_FULL || this == TOO_LOW || this == BLOCKED;
    }

    public static GeneratorStatus byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : RUNNING;
    }
}
