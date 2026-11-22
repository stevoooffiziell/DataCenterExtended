package de.f4ked.datacenterextended.power.model;

/**
 * Heat of the small modular reactor (0 = cold, 1 = limit). Power heats the core, coolant removes heat in proportion
 * to the temperature (Newton cooling). Above {@link #THROTTLE} the control rods move in and reduce the power to 25 %
 * at the limit; at {@link #SCRAM} the reactor shuts down until it has cooled and is restarted.
 */
public final class ReactorModel {
    public static final double THROTTLE = 0.8;
    public static final double SCRAM = 0.98;
    public static final double RESTART_BELOW = 0.3;
    /** Heating per tick at full power: from cold to the limit in one minute without cooling. */
    static final double HEAT_PER_TICK = 1.0 / 1200.0;
    /** Cooling per tick per coolant unit at heat 1: six water sides hold full power at ~60 % heat. */
    static final double COOL_PER_UNIT = HEAT_PER_TICK / 3.6;
    /** Passive cooling through the casing. */
    static final double AMBIENT_UNITS = 0.25;

    private ReactorModel() {
    }

    /** Power fraction the control rods allow at this heat. */
    public static double throttle(double heat) {
        if (heat <= THROTTLE) return 1.0;
        return Math.max(0.25, 1.0 - 0.75 * (heat - THROTTLE) / (1.0 - THROTTLE));
    }

    /** Heat after one tick. */
    public static double step(double heat, double powerFraction, double coolantUnits) {
        double next = heat + powerFraction * HEAT_PER_TICK - (coolantUnits + AMBIENT_UNITS) * COOL_PER_UNIT * heat;
        return Math.max(0, Math.min(1, next));
    }
}
