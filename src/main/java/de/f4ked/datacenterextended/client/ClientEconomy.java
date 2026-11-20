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
