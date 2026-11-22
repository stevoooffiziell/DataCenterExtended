package de.f4ked.datacenterextended.power;

import static de.f4ked.datacenterextended.power.GeneratorFamily.*;

/**
 * Every generator block. Peak output in FE/t (with the default 0.1 FE per watt, 1 FE/t ≈ 10 W): a Pentium 4 server
 * needs ~20 FE/t, a modern GPU server several hundred.
 */
public enum GeneratorType {
    POLYCRYSTALLINE_SOLAR("polycrystalline_solar_panel", SOLAR, 1, 8),
    MONOCRYSTALLINE_SOLAR("monocrystalline_solar_panel", SOLAR, 2, 20),
    PERC_SOLAR("perc_solar_panel", SOLAR, 3, 40),
    PEROVSKITE_SOLAR("perovskite_solar_panel", SOLAR, 4, 80),
    SMALL_WIND("small_wind_turbine", WIND, 1, 24),
    MEDIUM_WIND("medium_wind_turbine", WIND, 2, 80),
    LARGE_WIND("large_wind_turbine", WIND, 3, 240),
    WATER_WHEEL("water_wheel", WATER, 1, 16),
    MICRO_HYDRO("micro_hydro_turbine", WATER, 2, 60),
    HYDROELECTRIC("hydroelectric_turbine", WATER, 3, 192),
    COMBUSTION_GENERATOR("combustion_generator", COMBUSTION, 1, 40),
    GAS_TURBINE("gas_turbine", COMBUSTION, 2, 120),
    GEOTHERMAL_PLANT("geothermal_plant", GEOTHERMAL, 1, 36),
    BINARY_CYCLE_PLANT("binary_cycle_plant", GEOTHERMAL, 2, 90),
    SMALL_MODULAR_REACTOR("small_modular_reactor", NUCLEAR, 1, 2000);

    /** Fuel rod slots of the reactor. */
    public static final int REACTOR_RODS = 4;

    private final String id;
    private final GeneratorFamily family;
    private final int tier;
    private final int peak;

    GeneratorType(String id, GeneratorFamily family, int tier, int peak) {
        this.id = id;
        this.family = family;
        this.tier = tier;
        this.peak = peak;
    }

    public String id() {
        return id;
    }

    public GeneratorFamily family() {
        return family;
    }

    public int tier() {
        return tier;
    }

    /** Peak output in FE/t before the config multiplier. */
    public int peak() {
        return peak;
    }

    /** Internal buffer: 30 seconds of peak output, at least 10k FE. */
    public int bufferCapacity() {
        return Math.max(10_000, peak * 600);
    }

    /** FE/t pushed into neighbours. */
    public int pushRate() {
        return Math.max(256, peak * 4);
    }

    /** Free radius (blocks) the rotor needs around the nacelle, in the plane in front of it. */
    public int rotorRadius() {
        return family == WIND ? tier : 0;
    }

    /** Water column for full output (turbines). */
    public int maxHead() {
        return this == MICRO_HYDRO ? 30 : 32;
    }

    /** Adjacent lava sources for full output (geothermal: all six sides). */
    public double lavaForFull() {
        return 6.0;
    }

    /** FE per unit of fuel compared with the basic combustion generator. */
    public double fuelEfficiency() {
        return this == GAS_TURBINE ? 2.0 : 1.0;
    }

    /** Item slots: fuel for combustion, rods for the reactor. */
    public int slotCount() {
        return switch (family) {
            case COMBUSTION -> 1;
            case NUCLEAR -> REACTOR_RODS;
            default -> 0;
        };
    }

    public boolean slab() {
        return family == SOLAR;
    }

    public static GeneratorType byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : POLYCRYSTALLINE_SOLAR;
    }
}
