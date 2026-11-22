package de.f4ked.datacenterextended.hosting;

import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.hardware.DataUnits;

import java.util.Locale;

/** Plans hosting customers sign up for. Settings live in {@link Config#PLANS} (same order). */
public enum HostingPlan {
    SHARED("S"),
    VPS("V"),
    DEDICATED("D");

    private static final HostingPlan[] VALUES = values();
    private final String shortName;

    HostingPlan(String shortName) {
        this.shortName = shortName;
    }

    public static HostingPlan byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : SHARED;
    }

    public static int count() {
        return VALUES.length;
    }

    /** One letter for compact displays. */
    public String shortName() {
        return shortName;
    }

    public String translationKey() {
        return "plan.datacenterextended." + name().toLowerCase(Locale.ROOT);
    }

    /** Compute the customer uses, in bytes per second. */
    public double computeBps() {
        return Config.safe(Config.PLANS[ordinal()].compute(), 1.0) * DataUnits.KB;
    }

    /** Dollars per hour while fully served. */
    public double pricePerHour() {
        return Config.safe(Config.PLANS[ordinal()].pricePerHour(), 0.0);
    }

    public int weight() {
        return Config.safe(Config.PLANS[ordinal()].weight(), 1);
    }

    /** The current config as parameters for {@link CustomerBase}. */
    public static CustomerBase.Params params() {
        double[] compute = new double[VALUES.length], price = new double[VALUES.length];
        int[] weight = new int[VALUES.length];
        for (HostingPlan plan : VALUES) {
            compute[plan.ordinal()] = plan.computeBps();
            price[plan.ordinal()] = plan.pricePerHour();
            weight[plan.ordinal()] = plan.weight();
        }
        return new CustomerBase.Params(compute, price, weight, Config.safe(Config.SIGNUP_CHANCE, 0.5),
                Config.safe(Config.OVERLOAD_CANCEL_CHANCE, 0.5), Config.safe(Config.CHURN_CHANCE, 0.002));
    }
}
