package de.f4ked.datacenterextended.network;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ComputeAllocatorTest {

    @Test
    void noBandwidthMeansNoTransfer() {
        var r = ComputeAllocator.allocate(new long[]{100, 0}, new long[]{0, 100}, 0);
        assertEquals(0, r.transferred());
        assertArrayEquals(new long[]{0, 0}, r.received());
        assertArrayEquals(new long[]{0, 0}, r.exported());
        assertTrue(r.bandwidthLimited());
    }

    @Test
    void bandwidthCapsTheTransfer() {
        var r = ComputeAllocator.allocate(new long[]{1000, 0}, new long[]{0, 1000}, 250);
        assertEquals(250, r.transferred());
        assertEquals(250, r.received()[1]);
        assertEquals(250, r.exported()[0]);
        assertTrue(r.bandwidthLimited());
    }

    @Test
    void supplyLimitsWhenBandwidthIsPlenty() {
        var r = ComputeAllocator.allocate(new long[]{300, 0, 0}, new long[]{0, 500, 500}, Long.MAX_VALUE);
        assertEquals(300, r.transferred());
        assertEquals(150, r.received()[1]);
        assertEquals(150, r.received()[2]);
        assertFalse(r.bandwidthLimited());
    }

    @Test
    void smallDemandsAreFilledCompletelyAndTheRestIsSharedFairly() {
        // 100 available; demands 10, 80, 80 -> 10 + 45 + 45
        var r = ComputeAllocator.allocate(new long[]{100, 0, 0, 0}, new long[]{0, 10, 80, 80}, 1_000);
        assertEquals(100, r.transferred());
        assertEquals(10, r.received()[1]);
        assertEquals(45, r.received()[2]);
        assertEquals(45, r.received()[3]);
    }

    @Test
    void moreSupplyThanDemandTakesProportionallyFromSuppliers() {
        // demand 90, suppliers offer 200 and 100 -> they give 60 and 30
        var r = ComputeAllocator.allocate(new long[]{200, 100, 0}, new long[]{0, 0, 90}, 1_000);
        assertEquals(90, r.transferred());
        assertEquals(90, r.received()[2]);
        assertEquals(60, r.exported()[0]);
        assertEquals(30, r.exported()[1]);
    }

    @Test
    void roundingNeverLosesOrInventsCompute() {
        var r = ComputeAllocator.allocate(new long[]{7, 7, 7, 0, 0, 0}, new long[]{0, 0, 0, 5, 5, 5}, 13);
        assertEquals(13, r.transferred());
        assertEquals(13, Arrays.stream(r.received()).sum());
        assertEquals(13, Arrays.stream(r.exported()).sum());
        for (int i = 0; i < 3; i++) assertTrue(r.exported()[i] <= 7);
        for (int i = 3; i < 6; i++) assertTrue(r.received()[i] <= 5);
    }

    @Test
    void emptyNetworkIsFine() {
        var r = ComputeAllocator.allocate(new long[0], new long[0], 100);
        assertEquals(0, r.transferred());
    }
}
