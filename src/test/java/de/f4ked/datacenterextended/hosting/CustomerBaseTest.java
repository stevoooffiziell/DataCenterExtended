package de.f4ked.datacenterextended.hosting;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerBaseTest {
    private static final CustomerBase.Params PARAMS = new CustomerBase.Params(
            new double[]{2048, 16384, 131072}, new double[]{12, 110, 900}, new int[]{6, 3, 1}, 0.5, 0.5, 0.0);

    @Test
    void firstCustomerAlwaysComesAndOnlyFittingPlansAreChosen() {
        CustomerBase base = new CustomerBase(3);
        CustomerBase.Result result = base.check(1.0, 4096, PARAMS, new Random(1)::nextDouble);
        assertEquals(HostingPlan.SHARED.ordinal(), result.signedUp(), "only the shared plan fits into 4 KB/s");
        assertEquals(1, base.total());
    }

    @Test
    void nothingSignsUpWithoutSpareCompute() {
        CustomerBase base = new CustomerBase(3);
        CustomerBase.Result result = base.check(1.0, 1000, PARAMS, () -> 0.0);
        assertEquals(-1, result.signedUp());
        assertEquals(0, base.total());
    }

    @Test
    void overloadedRacksLoseCustomersAndSatisfaction() {
        CustomerBase base = new CustomerBase(3);
        base.setCount(2, 100);
        Random random = new Random(7);
        int cancelled = base.check(0.0, 0, PARAMS, random::nextDouble).cancelled();
        assertTrue(cancelled > 30 && cancelled < 70, "about half cancel at 0% served, cancelled " + cancelled);
        assertTrue(base.satisfaction() < 1.0);
        assertEquals(100 - cancelled, base.count(2));
    }

    @Test
    void happyCustomersStay() {
        CustomerBase base = new CustomerBase(3);
        base.setCount(0, 50);
        for (int i = 0; i < 20; i++) base.check(1.0, 0, PARAMS, new Random(i)::nextDouble);
        assertEquals(50, base.count(0));
        assertEquals(1.0, base.satisfaction(), 1e-9);
    }

    @Test
    void contractedComputeAndRevenueAddUp() {
        CustomerBase base = new CustomerBase(3);
        base.setCount(0, 2);
        base.setCount(2, 1);
        assertEquals(2 * 2048 + 131072, base.contractedBps(PARAMS.computeBps()), 1e-9);
        assertEquals(2 * 12 + 900, base.revenuePerHour(PARAMS.pricePerHour()), 1e-9);
    }

    @Test
    void pickPlanRespectsWeights() {
        int[] picks = new int[3];
        Random random = new Random(3);
        for (int i = 0; i < 10_000; i++) picks[CustomerBase.pickPlan(1e9, PARAMS, random.nextDouble())]++;
        assertTrue(picks[0] > picks[1] && picks[1] > picks[2], "weights 6:3:1 expected, got " + java.util.Arrays.toString(picks));
    }
}
