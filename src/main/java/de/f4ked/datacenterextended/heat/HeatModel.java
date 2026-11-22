package de.f4ked.datacenterextended.heat;

/**
 * Rack temperature (pure, no Minecraft classes, unit tested).
 * <ul>
 *     <li>The <b>target temperature</b> grows linearly with the heat load: ambient with no load, the throttle temperature
 *     when the load equals the cooling capacity, and higher beyond that.</li>
 *     <li>The rack's temperature moves towards the target a little every tick (thermal mass).</li>
 *     <li>Above the throttle temperature the speed drops linearly to {@code minSpeed} at the critical temperature.</li>
 * </ul>
 */
public final class HeatModel {
    /** Share of the gap to the target temperature closed per tick (~5 seconds to get 63% of the way). */
    public static final double RESPONSE_PER_TICK = 0.01;
    /** Hard cap so a rack without any cooling doesn't show absurd numbers. */
    public static final double MAX_TEMPERATURE = 150.0;

    private HeatModel() {
    }

    public static double targetTemperature(double ambient, double throttleTemp, double heatWatts, double coolingWatts) {
        if (heatWatts <= 0) return ambient;
        if (coolingWatts <= 0) return MAX_TEMPERATURE;
        double target = ambient + (throttleTemp - ambient) * heatWatts / coolingWatts;
        return Math.min(MAX_TEMPERATURE, target);
    }

    /** The temperature after one tick. */
    public static double step(double current, double target) {
        double next = current + (target - current) * RESPONSE_PER_TICK;
        return Math.abs(next - target) < 0.01 ? target : next;
    }

    /** Speed multiplier (1.0 = full speed) at the given temperature. */
    public static double speedFactor(double temperature, double throttleTemp, double criticalTemp, double minSpeed) {
        if (temperature <= throttleTemp) return 1.0;
        if (criticalTemp <= throttleTemp || temperature >= criticalTemp) return minSpeed;
        double t = (temperature - throttleTemp) / (criticalTemp - throttleTemp);
        return 1.0 - t * (1.0 - minSpeed);
    }

    /** 0 below the throttle temperature, 1 at (or above) the critical temperature. */
    public static double overheat(double temperature, double throttleTemp, double criticalTemp) {
        if (temperature <= throttleTemp) return 0;
        if (criticalTemp <= throttleTemp) return 1;
        return Math.min(1.0, (temperature - throttleTemp) / (criticalTemp - throttleTemp));
    }

    /** Ambient °C from a biome's base temperature (plains 0.8 = 22 °C, desert 2.0 = 40 °C, snowy plains 0.0 = 10 °C). */
    public static double ambientFromBiome(float biomeTemperature) {
        return Math.max(-10.0, Math.min(50.0, 10.0 + 15.0 * biomeTemperature));
    }
}
