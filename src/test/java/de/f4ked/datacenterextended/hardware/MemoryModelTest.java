package de.f4ked.datacenterextended.hardware;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemoryModelTest {
    private static final double STARVED = 0.25, SPEED = 0.03, CHANNEL = 0.10;

    private static double factor(double installed, double recommended, int speed, int modules) {
        return MemoryModel.factor(installed, recommended, speed, modules, STARVED, SPEED, CHANNEL);
    }

    @Test
    void noMemoryIsStarved() {
        assertEquals(STARVED, factor(0, 16, 0, 0), 1e-9);
    }

    @Test
    void halfTheRecommendedRamScalesLinearly() {
        // 0.25 + 0.75 * 0.5 = 0.625, no speed, single module
        assertEquals(0.625, factor(8, 16, 0, 1), 1e-9);
    }

    @Test
    void moreThanRecommendedDoesNotHelpFurther() {
        assertEquals(factor(16, 16, 0, 1), factor(64, 16, 0, 1), 1e-9);
    }

    @Test
    void speedAndChannelsMultiply() {
        // full capacity * (1 + 0.03 * 3.2) * 1.1
        assertEquals(1.096 * 1.1, factor(32, 16, 3200, 2), 1e-9);
    }

    @Test
    void cpuWithoutRecommendationRunsAtFullCapacity() {
        assertEquals(1.0, factor(1, 0, 0, 1), 1e-9);
    }
}
