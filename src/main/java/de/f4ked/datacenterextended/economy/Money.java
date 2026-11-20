package de.f4ked.datacenterextended.economy;

import java.util.Locale;

/** Money is stored as whole cents (long) to avoid rounding drift. */
public final class Money {
    private Money() {
    }

    public static long toCents(double dollars) {
        return Math.round(dollars * 100.0);
    }

    /** 123456 -> "$1,234.56", negative values get a leading minus. */
    public static String format(long cents) {
        String sign = cents < 0 ? "-" : "";
        long abs = Math.abs(cents);
        return String.format(Locale.ROOT, "%s$%,d.%02d", sign, abs / 100, abs % 100);
    }

    /** A price that may be below one cent, e.g. "$0.2531" or "$1,512.40". */
    public static String formatPrice(double dollars) {
        if (dollars < 1.0) return String.format(Locale.ROOT, "$%.4f", dollars);
        return String.format(Locale.ROOT, "$%,.2f", dollars);
    }

    /** "0.000421 BTC". */
    public static String formatCoin(double amount, Coin coin) {
        return String.format(Locale.ROOT, "%,." + coin.decimals() + "f %s", amount, coin.symbol());
    }
}
