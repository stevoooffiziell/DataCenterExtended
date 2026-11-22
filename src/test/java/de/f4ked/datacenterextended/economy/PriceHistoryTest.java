package de.f4ked.datacenterextended.economy;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceHistoryTest {
    private static double day(int y, int m, int d) {
        return LocalDate.of(y, m, d).toEpochDay();
    }

    @Test
    void knownPointsAreReturnedExactly() {
        assertEquals(10000, PriceHistory.referencePrice(Coin.BITCOIN, day(2017, 12, 1), 0), 1e-6);
        assertEquals(0.39, PriceHistory.referencePrice(Coin.DOGECOIN, day(2021, 5, 1), 0), 1e-9);
    }

    @Test
    void betweenPointsTheLogPriceIsInterpolated() {
        double mid = PriceHistory.referencePrice(Coin.BITCOIN, day(2017, 12, 16), 0);
        assertTrue(mid > 10000 && mid < 13600, "between Dec and Jan values, was " + mid);
    }

    @Test
    void noPriceBeforeLaunch() {
        assertEquals(0, PriceHistory.referencePrice(Coin.DOGECOIN, day(2013, 1, 1), 0));
        assertFalse(PriceHistory.launched(Coin.ETHEREUM, day(2015, 7, 1)));
        assertTrue(PriceHistory.launched(Coin.ETHEREUM, day(2015, 8, 1)));
    }

    @Test
    void afterTheTableThePriceDrifts() {
        double last = PriceHistory.lastKnownDay(Coin.BITCOIN);
        double atEnd = PriceHistory.referencePrice(Coin.BITCOIN, last, 0.10);
        double yearLater = PriceHistory.referencePrice(Coin.BITCOIN, last + 365.25, 0.10);
        assertEquals(atEnd * 1.10, yearLater, atEnd * 1e-6);
    }

    @Test
    void bitcoinHalvingsFollowRealDates() {
        assertEquals(0, PriceHistory.halvings(Coin.BITCOIN, day(2012, 11, 27)));
        assertEquals(1, PriceHistory.halvings(Coin.BITCOIN, day(2012, 11, 28)));
        assertEquals(4, PriceHistory.halvings(Coin.BITCOIN, day(2025, 1, 1)));
        assertEquals(5, PriceHistory.halvings(Coin.BITCOIN, day(2028, 5, 1)), "projected 4 years after 2024");
        assertEquals(LocalDate.of(2016, 7, 9), PriceHistory.nextHalving(Coin.BITCOIN, day(2014, 1, 1)));
        assertNull(PriceHistory.nextHalving(Coin.DOGECOIN, day(2014, 1, 1)));
        assertEquals(0, PriceHistory.halvings(Coin.ETHEREUM, day(2025, 1, 1)));
    }

    @Test
    void theMerge() {
        assertFalse(PriceHistory.afterMerge(day(2022, 9, 14)));
        assertTrue(PriceHistory.afterMerge(day(2022, 9, 15)));
    }
}
