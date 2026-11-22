package de.f4ked.datacenterextended.power.model;

/**
 * Feed-in tariff of the grid connection. The economy prices energy per kWh per real-time hour (like the electricity
 * bill), the export price is a small fraction of the bill price and rises slowly with the calendar year, so selling
 * energy only becomes interesting late in the game.
 */
public final class GridModel {
    public static final int BASE_YEAR = 2010;
    private static final double TICKS_PER_HOUR = 72000.0;

    private GridModel() {
    }

    /** Export price multiplier for a calendar year (fractional), never below 1. */
    public static double growth(double year, double yearlyGrowth) {
        return Math.pow(1.0 + yearlyGrowth, Math.max(0, year - BASE_YEAR));
    }

    /** Cents paid for exporting {@code watts} for one real-time hour. */
    public static double centsPerHour(double watts, double pricePerKWh, double growth) {
        return watts / 1000.0 * pricePerKWh * growth * 100.0;
    }

    /** Cents paid for {@code fe} exported FE (one FE/t = 1 / fePerWatt watts for one tick). */
    public static double centsForFe(double fe, double fePerWatt, double pricePerKWh, double growth) {
        double wattTicks = fe / Math.max(1.0E-9, fePerWatt);
        return centsPerHour(wattTicks / TICKS_PER_HOUR, pricePerKWh, growth);
    }
}
