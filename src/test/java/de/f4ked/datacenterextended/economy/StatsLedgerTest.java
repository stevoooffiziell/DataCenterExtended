package de.f4ked.datacenterextended.economy;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatsLedgerTest {
    private static final double DAY = LocalDate.of(2020, 3, 15).toEpochDay();

    @Test
    void hourlyBucketsForTheDayView() {
        StatsLedger ledger = new StatsLedger();
        ledger.record(StatsLedger.Category.HOSTING, 100, DAY + 10.5 / 24);
        ledger.record(StatsLedger.Category.HOSTING, 50, DAY + 10.9 / 24);
        ledger.record(StatsLedger.Category.BILLS, 30, DAY + 11.2 / 24);
        StatsLedger.Series s = ledger.series(StatsLedger.Scale.DAY, DAY + 11.5 / 24);
        assertEquals(24, s.values().length);
        assertEquals(150, s.values()[22][StatsLedger.Category.HOSTING.ordinal()]);
        assertEquals(30, s.values()[23][StatsLedger.Category.BILLS.ordinal()]);
    }

    @Test
    void dailyAndMonthlyViews() {
        StatsLedger ledger = new StatsLedger();
        ledger.record(StatsLedger.Category.MINING, 1000, DAY - 2);
        ledger.record(StatsLedger.Category.MINING, 500, DAY);
        ledger.record(StatsLedger.Category.SHOP, 200, DAY - 40);
        StatsLedger.Series week = ledger.series(StatsLedger.Scale.WEEK, DAY + 0.5);
        assertEquals(1000, week.values()[4][StatsLedger.Category.MINING.ordinal()]);
        assertEquals(500, week.values()[6][StatsLedger.Category.MINING.ordinal()]);
        StatsLedger.Series year = ledger.series(StatsLedger.Scale.YEAR, DAY + 0.5);
        assertEquals(1500, year.values()[11][StatsLedger.Category.MINING.ordinal()], "March 2020");
        assertEquals(200, year.values()[10][StatsLedger.Category.SHOP.ordinal()], "40 days earlier = early February");
    }

    @Test
    void oldBucketsArePrunedAndRowsRoundTrip() {
        StatsLedger ledger = new StatsLedger();
        ledger.record(StatsLedger.Category.HOSTING, 7, DAY - 500);
        ledger.record(StatsLedger.Category.HOSTING, 9, DAY);
        StatsLedger copy = StatsLedger.of(ledger.hourlyRows(), ledger.dailyRows());
        assertEquals(1, copy.dailyRows().size(), "500 days ago is pruned");
        assertEquals(9, copy.series(StatsLedger.Scale.WEEK, DAY).values()[6][0]);
    }
}
