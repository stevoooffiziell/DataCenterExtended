package de.f4ked.datacenterextended.heat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeatModelTest {

    @Test
    void targetIsThrottleTemperatureWhenHeatEqualsCooling() {
        assertEquals(70.0, HeatModel.targetTemperature(20, 70, 1000, 1000), 1e-9);
        assertEquals(20.0, HeatModel.targetTemperature(20, 70, 0, 1000), 1e-9);
        assertEquals(45.0, HeatModel.targetTemperature(20, 70, 500, 1000), 1e-9);
    }

    @Test
    void withoutCoolingTheRackHitsTheCap() {
        assertEquals(HeatModel.MAX_TEMPERATURE, HeatModel.targetTemperature(20, 70, 100, 0), 1e-9);
        assertEquals(HeatModel.MAX_TEMPERATURE, HeatModel.targetTemperature(20, 70, 1_000_000, 10), 1e-9);
    }

    @Test
    void temperatureApproachesTheTarget() {
        double t = 20;
        for (int i = 0; i < 2000; i++) t = HeatModel.step(t, 80);
        assertEquals(80, t, 1e-9);
        double half = 20;
        for (int i = 0; i < 69; i++) half = HeatModel.step(half, 80);
        assertTrue(half > 45 && half < 55, "about half way after ~70 ticks, was " + half);
    }

    @Test
    void speedDropsLinearlyBetweenThrottleAndCritical() {
        assertEquals(1.0, HeatModel.speedFactor(60, 70, 95, 0.25), 1e-9);
        assertEquals(1.0, HeatModel.speedFactor(70, 70, 95, 0.25), 1e-9);
        assertEquals(0.625, HeatModel.speedFactor(82.5, 70, 95, 0.25), 1e-9);
        assertEquals(0.25, HeatModel.speedFactor(120, 70, 95, 0.25), 1e-9);
        assertEquals(0.5, HeatModel.overheat(82.5, 70, 95), 1e-9);
    }

    @Test
    void biomesSetTheAmbientTemperature() {
        assertEquals(22.0, HeatModel.ambientFromBiome(0.8F), 1e-5);
        assertEquals(40.0, HeatModel.ambientFromBiome(2.0F), 1e-5);
        assertEquals(10.0, HeatModel.ambientFromBiome(0.0F), 1e-5);
    }
}
