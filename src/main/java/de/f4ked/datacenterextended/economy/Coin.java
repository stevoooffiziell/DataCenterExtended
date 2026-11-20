package de.f4ked.datacenterextended.economy;

import de.f4ked.datacenterextended.Config;

import java.util.Locale;

/** Mineable crypto currencies. Market settings live in {@link Config#COINS} (same order). */
public enum Coin {
    BITCOIN("BTC", 0xFFF7931A, 6),
    DOGECOIN("DOGE", 0xFFC2A633, 2),
    ETHEREUM("ETH", 0xFF8C8CFF, 5);

    private static final Coin[] VALUES = values();

    private final String symbol;
    private final int color;
    private final int decimals;

    Coin(String symbol, int color, int decimals) {
        this.symbol = symbol;
        this.color = color;
        this.decimals = decimals;
    }

    public String symbol() {
        return symbol;
    }

    /** ARGB colour for charts and labels. */
    public int color() {
        return color;
    }

    /** Decimals shown for amounts. */
    public int decimals() {
        return decimals;
    }

    public String translationKey() {
        return "coin.datacenterextended." + name().toLowerCase(Locale.ROOT);
    }

    public Config.CoinSettings settings() {
        return Config.COINS[ordinal()];
    }

    public double basePrice() {
        return Config.safe(settings().basePrice(), 1.0);
    }

    public double hashesPerCoin() {
        return Config.safe(settings().hashesPerCoin(), 1.0E10);
    }

    public static Coin byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : BITCOIN;
    }

    public static int count() {
        return VALUES.length;
    }
}
