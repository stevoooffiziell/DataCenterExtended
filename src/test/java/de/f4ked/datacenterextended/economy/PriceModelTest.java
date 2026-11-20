package de.f4ked.datacenterextended.economy;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceModelTest {

    @Test
    void withoutNoisePricesRevertTowardsTheBase() {
        double price = 3000;
        for (int i = 0; i < 200; i++) price = PriceModel.next(price, 1500, 0.05, 0.05, 0.0);
        assertEquals(1500, price, 1.0);
    }

    @Test
    void neverNegativeAndStaysInBand() {
        Random random = new Random(42);
        double price = 0.25;
        for (int i = 0; i < 100_000; i++) {
            price = PriceModel.next(price, 0.25, 0.5, 0.0, random.nextGaussian() * 10);
            assertTrue(price > 0);
            assertTrue(price <= 0.25 * 1000 * 1.001 && price >= 0.25 / 1000 * 0.999);
        }
    }

    @Test
    void deterministicForTheSameNoise() {
        assertEquals(PriceModel.next(100, 120, 0.04, 0.02, 1.3), PriceModel.next(100, 120, 0.04, 0.02, 1.3));
    }

    @Test
    void longRunAverageIsNearTheBase() {
        Random random = new Random(7);
        double price = 120, logSum = 0;
        int n = 200_000;
        for (int i = 0; i < n; i++) {
            price = PriceModel.next(price, 120, 0.04, 0.02, random.nextGaussian());
            logSum += Math.log(price);
        }
        assertEquals(Math.log(120), logSum / n, 0.05);
    }
}
