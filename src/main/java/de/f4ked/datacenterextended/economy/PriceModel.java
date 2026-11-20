package de.f4ked.datacenterextended.economy;

/**
 * Mean-reverting random walk in log space (discrete Ornstein-Uhlenbeck process). Pure and unit tested.
 * <pre>ln p' = ln p + reversion * (ln base - ln p) + volatility * gaussian</pre>
 * The price can never become zero or negative and is always pulled back towards {@code base}.
 */
public final class PriceModel {
    private PriceModel() {
    }

    public static double next(double price, double base, double volatility, double reversion, double gaussian) {
        double logPrice = Math.log(Math.max(price, 1e-12));
        double logBase = Math.log(Math.max(base, 1e-12));
        double next = logPrice + reversion * (logBase - logPrice) + volatility * gaussian;
        // keep within a sane band (1/1000x .. 1000x of the base) so a datapack typo can't break the economy
        next = Math.max(logBase - 6.9, Math.min(logBase + 6.9, next));
        return Math.exp(next);
    }
}
