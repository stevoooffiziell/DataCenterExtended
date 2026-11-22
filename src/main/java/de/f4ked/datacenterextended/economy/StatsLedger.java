package de.f4ked.datacenterextended.economy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Income and expenses of one player over calendar time (pure, no Minecraft classes, unit tested). Amounts are cents
 * in hourly buckets (last {@value #HOURS} calendar hours) and daily buckets (last {@value #DAYS} calendar days).
 */
public final class StatsLedger {
    public static final int HOURS = 48;
    public static final int DAYS = 400;

    /** What an amount was for. Coin sales are a conversion (the coins were already counted when mined). */
    public enum Category {
        HOSTING(true), MINING(true), SALES(true), BILLS(false), SHOP(false), GRID(true);

        private final boolean income;

        Category(boolean income) {
            this.income = income;
        }

        public boolean income() {
            return income;
        }

        /** Counted in the profit (income minus bills and shop spending). Coin sales are not. */
        public boolean countsForProfit() {
            return this != SALES;
        }
    }

    /** Time ranges of the statistics screen. */
    public enum Scale {
        DAY(24), WEEK(7), MONTH(30), YEAR(12);

        private final int buckets;

        Scale(int buckets) {
            this.buckets = buckets;
        }

        public int buckets() {
            return buckets;
        }

        public static Scale byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : DAY;
        }
    }

    /**
     * Buckets of one scale, oldest first: {@code values[bucket][category]}.
     *
     * @param firstKey epoch hour (DAY) or epoch day of the first bucket (WEEK, MONTH; YEAR = first day of the month)
     */
    public record Series(Scale scale, long firstKey, long[][] values) {
    }

    public static final int CATEGORIES = Category.values().length;

    private final TreeMap<Long, long[]> hourly = new TreeMap<>();
    private final TreeMap<Long, long[]> daily = new TreeMap<>();

    public void record(Category category, long cents, double epochDay) {
        if (cents == 0) return;
        long hour = (long) Math.floor(epochDay * 24);
        long day = (long) Math.floor(epochDay);
        hourly.computeIfAbsent(hour, k -> new long[CATEGORIES])[category.ordinal()] += cents;
        daily.computeIfAbsent(day, k -> new long[CATEGORIES])[category.ordinal()] += cents;
        hourly.headMap(hour - HOURS, true).clear();
        daily.headMap(day - DAYS, true).clear();
    }

    public Series series(Scale scale, double epochDay) {
        long[][] values = new long[scale.buckets()][CATEGORIES];
        long hour = (long) Math.floor(epochDay * 24);
        long day = (long) Math.floor(epochDay);
        return switch (scale) {
            case DAY -> {
                long first = hour - 23;
                for (int i = 0; i < 24; i++) copy(hourly.get(first + i), values[i]);
                yield new Series(scale, first, values);
            }
            case WEEK, MONTH -> {
                long first = day - scale.buckets() + 1;
                for (int i = 0; i < scale.buckets(); i++) copy(daily.get(first + i), values[i]);
                yield new Series(scale, first, values);
            }
            case YEAR -> {
                LocalDate thisMonth = LocalDate.ofEpochDay(day).withDayOfMonth(1);
                LocalDate firstMonth = thisMonth.minusMonths(11);
                for (int i = 0; i < 12; i++) {
                    long from = firstMonth.plusMonths(i).toEpochDay(), to = firstMonth.plusMonths(i + 1).toEpochDay();
                    for (Map.Entry<Long, long[]> e : daily.subMap(from, true, to, false).entrySet()) add(e.getValue(), values[i]);
                }
                yield new Series(scale, firstMonth.toEpochDay(), values);
            }
        };
    }

    private static void copy(long[] from, long[] to) {
        if (from != null) System.arraycopy(from, 0, to, 0, Math.min(from.length, to.length));
    }

    private static void add(long[] from, long[] to) {
        for (int i = 0; i < Math.min(from.length, to.length); i++) to[i] += from[i];
    }

    // ------------------------------------------------------------------ persistence ([key, v0, v1, ...] rows)

    public List<List<Long>> hourlyRows() {
        return rows(hourly);
    }

    public List<List<Long>> dailyRows() {
        return rows(daily);
    }

    public static StatsLedger of(List<List<Long>> hourlyRows, List<List<Long>> dailyRows) {
        StatsLedger ledger = new StatsLedger();
        load(hourlyRows, ledger.hourly);
        load(dailyRows, ledger.daily);
        return ledger;
    }

    private static List<List<Long>> rows(TreeMap<Long, long[]> map) {
        List<List<Long>> rows = new ArrayList<>(map.size());
        for (Map.Entry<Long, long[]> e : map.entrySet()) {
            List<Long> row = new ArrayList<>(CATEGORIES + 1);
            row.add(e.getKey());
            for (long v : e.getValue()) row.add(v);
            rows.add(row);
        }
        return rows;
    }

    private static void load(List<List<Long>> rows, TreeMap<Long, long[]> map) {
        for (List<Long> row : rows) {
            if (row.isEmpty()) continue;
            long[] values = new long[CATEGORIES];
            for (int i = 1; i < row.size() && i <= CATEGORIES; i++) values[i - 1] = row.get(i);
            map.put(row.get(0), values);
        }
    }
}
