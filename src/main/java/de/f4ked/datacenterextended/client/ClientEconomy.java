package de.f4ked.datacenterextended.client;

import de.f4ked.datacenterextended.economy.Coin;

import java.util.ArrayList;
import java.util.List;

/** The local player's wallet and the market, as last synced by the server. */
public final class ClientEconomy {
    private static long cents;
    private static final double[] COINS = new double[Coin.count()];
    private static final double[] PRICES = new double[Coin.count()];
    private static final List<List<Double>> HISTORY = new ArrayList<>();
    private static final int[] HALVINGS = new int[Coin.count()];
    private static final long[] NEXT_HALVING = new long[Coin.count()];
    private static long marketSyncedAt;
    private static double syncedEpochDay = de.f4ked.datacenterextended.time.Calendar.epochDay(java.time.LocalDate.of(2010, 7, 1));
    private static int ticksPerDay = 2400;

    private ClientEconomy() {
    }

    static void setWallet(long newCents, List<Double> coins) {
        cents = newCents;
        for (int i = 0; i < COINS.length; i++) COINS[i] = i < coins.size() ? coins.get(i) : 0;
    }

    static void setMarket(List<Double> prices, List<List<Double>> history) {
        for (int i = 0; i < PRICES.length; i++) PRICES[i] = i < prices.size() ? prices.get(i) : 0;
        HISTORY.clear();
        for (List<Double> h : history) HISTORY.add(List.copyOf(h));
    }

    static void setHalvings(List<Integer> halvings, List<Long> nextHalving, double epochDay, int dayTicks) {
        for (int i = 0; i < HALVINGS.length; i++) {
            HALVINGS[i] = i < halvings.size() ? halvings.get(i) : 0;
            NEXT_HALVING[i] = i < nextHalving.size() ? nextHalving.get(i) : -1;
        }
        marketSyncedAt = System.currentTimeMillis();
        syncedEpochDay = epochDay;
        ticksPerDay = Math.max(1, dayTicks);
    }

    /** Calendar time, advanced locally since the last sync (20 ticks per real second). */
    public static double epochDay() {
        double ticks = (System.currentTimeMillis() - marketSyncedAt) / 50.0;
        return syncedEpochDay + Math.min(ticks, 1200) / ticksPerDay;
    }

    public static java.time.LocalDate today() {
        return de.f4ked.datacenterextended.time.Calendar.date(epochDay());
    }

    /** Real seconds per calendar day. */
    public static double secondsPerDay() {
        return ticksPerDay / 20.0;
    }

    public static int halvings(Coin coin) {
        return HALVINGS[coin.ordinal()];
    }

    /** Date of the coin's next halving, or null if there is none. */
    public static java.time.LocalDate nextHalving(Coin coin) {
        long day = NEXT_HALVING[coin.ordinal()];
        return day < 0 ? null : java.time.LocalDate.ofEpochDay(day);
    }

    /** Real seconds until the next halving, or -1. */
    public static double secondsToNextHalving(Coin coin) {
        long day = NEXT_HALVING[coin.ordinal()];
        if (day < 0) return -1;
        return Math.max(0, (day - epochDay()) * secondsPerDay());
    }

    public static long cents() {
        return cents;
    }

    public static double coins(Coin coin) {
        return COINS[coin.ordinal()];
    }

    public static double price(Coin coin) {
        return PRICES[coin.ordinal()];
    }

    /** Price history, oldest first (may be empty right after a world is created). */
    public static List<Double> history(Coin coin) {
        return coin.ordinal() < HISTORY.size() ? HISTORY.get(coin.ordinal()) : List.of();
    }

    /** Change against the previous market update, e.g. 0.031 = +3.1%. */
    public static double change(Coin coin) {
        List<Double> h = history(coin);
        if (h.size() < 2) return 0;
        double before = h.get(h.size() - 2);
        return before > 0 ? h.getLast() / before - 1.0 : 0;
    }
}
