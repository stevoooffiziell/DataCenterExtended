package de.f4ked.datacenterextended.network;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.IntStream;

/**
 * Splits compute inside one network. Pure function, no Minecraft dependencies (unit tested).
 * <ul>
 *     <li>transfer = min(total supply, total demand, bandwidth)</li>
 *     <li>receivers are served max-min fair ("water-filling"): small demands are filled completely, the rest is
 *     shared equally among the larger ones</li>
 *     <li>suppliers give in proportion to what they offer</li>
 * </ul>
 */
public final class ComputeAllocator {

    public record Result(long[] received, long[] exported, long transferred, boolean bandwidthLimited) {
    }

    private ComputeAllocator() {
    }

    public static Result allocate(long[] supply, long[] demand, long bandwidth) {
        int n = supply.length;
        if (demand.length != n) throw new IllegalArgumentException("supply and demand must have the same length");

        long totalSupply = sum(supply);
        long totalDemand = sum(demand);
        long possible = Math.min(totalSupply, totalDemand);
        long transfer = Math.max(0, Math.min(possible, bandwidth));

        long[] received = new long[n];
        long[] exported = new long[n];
        if (transfer > 0) {
            fillFair(demand, transfer, received);
            shareProportionally(supply, totalSupply, transfer, exported);
        }
        return new Result(received, exported, transfer, bandwidth < possible);
    }

    /** Max-min fair distribution of {@code amount} over {@code demand}. */
    private static void fillFair(long[] demand, long amount, long[] out) {
        Integer[] order = IntStream.range(0, demand.length).boxed().toArray(Integer[]::new);
        Arrays.sort(order, Comparator.comparingLong(i -> Math.max(0, demand[i])));
        long remaining = amount;
        for (int k = 0; k < order.length; k++) {
            int i = order[k];
            long share = remaining / (order.length - k);
            long give = Math.min(Math.max(0, demand[i]), share);
            if (k == order.length - 1) give = Math.min(Math.max(0, demand[i]), remaining);
            out[i] = give;
            remaining -= give;
        }
    }

    /** Takes {@code amount} from the suppliers proportionally to their supply. */
    private static void shareProportionally(long[] supply, long totalSupply, long amount, long[] out) {
        long assigned = 0;
        for (int i = 0; i < supply.length; i++) {
            if (supply[i] <= 0) continue;
            out[i] = (long) Math.floor((double) amount * supply[i] / totalSupply);
            out[i] = Math.min(out[i], supply[i]);
            assigned += out[i];
        }
        // hand out the rounding remainder to whoever still has spare supply
        for (int i = 0; i < supply.length && assigned < amount; i++) {
            long spare = supply[i] - out[i];
            if (spare <= 0) continue;
            long add = Math.min(spare, amount - assigned);
            out[i] += add;
            assigned += add;
        }
    }

    private static long sum(long[] values) {
        long total = 0;
        for (long v : values) {
            if (v > 0) total = saturatedAdd(total, v);
        }
        return total;
    }

    private static long saturatedAdd(long a, long b) {
        long r = a + b;
        return r < 0 ? Long.MAX_VALUE : r;
    }
}
