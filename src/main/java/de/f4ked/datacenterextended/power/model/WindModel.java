package de.f4ked.datacenterextended.power.model;

import de.f4ked.datacenterextended.power.GeneratorStatus;

/**
 * Wind turbines: wind speed grows with height above the ground (here: above sea level), storms add wind, and the
 * rotor needs free air around it. Gusts vary the output a little over time.
 */
public final class WindModel {
    /** Blocks above sea level for full output. */
    public static final int FULL_HEIGHT = 96;

    private WindModel() {
    }

    /**
     * @param y            height of the nacelle
     * @param seaLevel     the level's sea level
     * @param freeFraction share of the rotor area that is free air (0..1)
     * @param seed         per-turbine gust offset
     */
    public static Output output(int y, int seaLevel, double freeFraction, float rain, float thunder, long gameTime, long seed) {
        if (y < seaLevel) return Output.off(GeneratorStatus.TOO_LOW);
        if (freeFraction < 0.5) return Output.off(GeneratorStatus.BLOCKED);
        double height = 0.1 + 0.9 * Output.clamp((y - seaLevel) / (double) FULL_HEIGHT);
        double weather = 1.0 + 0.25 * rain + 0.25 * thunder;
        double gust = 0.85 + 0.15 * Math.sin(gameTime / 400.0 + Math.floorMod(seed, 628) / 100.0);
        double factor = Output.clamp(height * weather * gust) * freeFraction * freeFraction;
        GeneratorStatus status = freeFraction < 0.999 ? GeneratorStatus.BLOCKED
                : y < seaLevel + 16 ? GeneratorStatus.TOO_LOW : GeneratorStatus.RUNNING;
        return new Output(factor, status);
    }
}
