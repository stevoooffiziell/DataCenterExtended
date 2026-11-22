package de.f4ked.datacenterextended.hosting;

import java.util.Arrays;
import java.util.function.DoubleSupplier;

/**
 * The customers of one hosting rack: a count per plan plus a satisfaction score (pure, no Minecraft classes, unit
 * tested). Customers are identical within a plan, so counts are enough.
 * <ul>
 *     <li><b>Sign-ups:</b> on every check, a new customer signs up with {@code signupChance × satisfaction²} if a plan
 *     fits into the spare compute (weighted pick among the plans that fit). The first customer always comes.</li>
 *     <li><b>Cancellations:</b> every customer cancels with {@code churn + overloadCancel × missing share}, where the
 *     missing share is the part of the sold compute the rack couldn't deliver.</li>
 *     <li><b>Satisfaction</b> follows the delivered share (moving average), so a rack that was overloaded recently
 *     attracts fewer customers for a while.</li>
 * </ul>
 */
public final class CustomerBase {
    private static final double SATISFACTION_SMOOTHING = 0.25;

    private final int[] counts;
    private double satisfaction = 1.0;

    /** Plan parameters, indexed like {@link HostingPlan}. */
    public record Params(double[] computeBps, double[] pricePerHour, int[] weight,
                         double signupChance, double overloadCancelChance, double churnChance) {
    }

    /** What happened during a check. {@code signedUp} is a plan index or -1. */
    public record Result(int signedUp, int cancelled) {
    }

    public CustomerBase(int plans) {
        counts = new int[plans];
    }

    public int count(int plan) {
        return counts[plan];
    }

    public void setCount(int plan, int count) {
        counts[plan] = Math.max(0, count);
    }

    public int total() {
        return Arrays.stream(counts).sum();
    }

    public double satisfaction() {
        return satisfaction;
    }

    public void setSatisfaction(double satisfaction) {
        this.satisfaction = Math.max(0, Math.min(1, satisfaction));
    }

    /** Compute in B/s sold to all customers. */
    public double contractedBps(double[] computeBps) {
        double total = 0;
        for (int i = 0; i < counts.length; i++) total += counts[i] * computeBps[i];
        return total;
    }

    /** Dollars per hour all customers pay while fully served. */
    public double revenuePerHour(double[] pricePerHour) {
        double total = 0;
        for (int i = 0; i < counts.length; i++) total += counts[i] * pricePerHour[i];
        return total;
    }

    /**
     * One customer check.
     *
     * @param served share (0..1) of the contracted compute delivered right now (1 without customers)
     * @param spare  compute in B/s available beyond the contracted compute
     * @param random uniform numbers in [0, 1)
     */
    public Result check(double served, double spare, Params params, DoubleSupplier random) {
        served = Math.max(0, Math.min(1, served));
        satisfaction += (served - satisfaction) * SATISFACTION_SMOOTHING;

        int cancelled = 0;
        double cancelChance = params.churnChance() + params.overloadCancelChance() * (1.0 - served);
        if (cancelChance > 0) {
            for (int plan = 0; plan < counts.length; plan++) {
                int keep = 0;
                for (int c = 0; c < counts[plan]; c++) {
                    if (random.getAsDouble() >= cancelChance) keep++;
                }
                cancelled += counts[plan] - keep;
                counts[plan] = keep;
            }
        }

        int signedUp = -1;
        // nobody signs up while existing customers aren't fully served
        if (served >= 0.999) {
            double chance = total() == 0 ? 1.0 : params.signupChance() * satisfaction * satisfaction;
            if (random.getAsDouble() < chance) {
                signedUp = pickPlan(spare, params, random.getAsDouble());
                if (signedUp >= 0) counts[signedUp]++;
            }
        }
        return new Result(signedUp, cancelled);
    }

    /** Weighted pick among the plans whose compute fits into {@code spare}; -1 if none fits. */
    static int pickPlan(double spare, Params params, double roll) {
        int totalWeight = 0;
        for (int i = 0; i < params.weight().length; i++) {
            if (params.computeBps()[i] <= spare) totalWeight += Math.max(0, params.weight()[i]);
        }
        if (totalWeight <= 0) return -1;
        double pick = roll * totalWeight;
        for (int i = 0; i < params.weight().length; i++) {
            if (params.computeBps()[i] > spare) continue;
            pick -= Math.max(0, params.weight()[i]);
            if (pick < 0) return i;
        }
        return -1;
    }
}
