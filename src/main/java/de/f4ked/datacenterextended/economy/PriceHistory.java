package de.f4ked.datacenterextended.economy;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Real-world reference prices (pure, no Minecraft classes, unit tested). The tables hold <b>approximate</b> USD prices
 * at the start of each month, rounded and compiled from memory, so they follow the real ups and downs (2013 rally,
 * 2017 bubble, 2021 peak, 2022 crash, ...) without claiming exchange-grade accuracy.
 * <ul>
 *     <li>Between two points the price is interpolated on a log scale.</li>
 *     <li>Before a coin's launch there is no price ({@link #launched}).</li>
 *     <li>After the last point the price drifts by a yearly rate (config {@code futureDrift}).</li>
 * </ul>
 */
public final class PriceHistory {
    /** Real launch dates (genesis block / first listing). */
    private static final Map<Coin, LocalDate> LAUNCH = new EnumMap<>(Map.of(
            Coin.BITCOIN, LocalDate.of(2009, 1, 3),
            Coin.DOGECOIN, LocalDate.of(2013, 12, 6),
            Coin.ETHEREUM, LocalDate.of(2015, 7, 30)));

    /** The Merge: Ethereum switched from mining to proof of stake. */
    public static final LocalDate THE_MERGE = LocalDate.of(2022, 9, 15);

    /** Real Bitcoin halvings; later ones are projected every 4 years. */
    private static final List<LocalDate> BTC_HALVINGS = List.of(
            LocalDate.of(2012, 11, 28), LocalDate.of(2016, 7, 9), LocalDate.of(2020, 5, 11), LocalDate.of(2024, 4, 20));

    private static final String BTC = """
            2009-01:0.0008 2009-10:0.0008 2010-03:0.003 2010-05:0.004 2010-07:0.07 2010-08:0.07 2010-09:0.06
            2010-10:0.10 2010-11:0.25 2010-12:0.25 2011-01:0.30 2011-02:0.90 2011-03:0.90 2011-04:0.80 2011-05:3.5
            2011-06:9 2011-07:15 2011-08:13 2011-09:8 2011-10:5 2011-11:3.2 2011-12:3.0 2012-01:5.0 2012-02:5.5
            2012-03:4.9 2012-04:4.9 2012-05:5.0 2012-06:5.3 2012-07:6.7 2012-08:9.3 2012-09:10.2 2012-10:12.4
            2012-11:11.2 2012-12:12.6 2013-01:13.5 2013-02:20 2013-03:34 2013-04:93 2013-05:139 2013-06:129
            2013-07:97 2013-08:106 2013-09:135 2013-10:133 2013-11:200 2013-12:1000 2014-01:770 2014-02:830
            2014-03:560 2014-04:450 2014-05:450 2014-06:630 2014-07:640 2014-08:580 2014-09:480 2014-10:390
            2014-11:340 2014-12:380 2015-01:315 2015-02:225 2015-03:255 2015-04:245 2015-05:235 2015-06:230
            2015-07:260 2015-08:285 2015-09:230 2015-10:237 2015-11:315 2015-12:377 2016-01:430 2016-02:370
            2016-03:435 2016-04:415 2016-05:450 2016-06:530 2016-07:670 2016-08:625 2016-09:575 2016-10:610
            2016-11:700 2016-12:745 2017-01:970 2017-02:965 2017-03:1190 2017-04:1080 2017-05:1420 2017-06:2400
            2017-07:2480 2017-08:2870 2017-09:4700 2017-10:4400 2017-11:6450 2017-12:10000 2018-01:13600
            2018-02:10200 2018-03:10400 2018-04:6900 2018-05:9200 2018-06:7500 2018-07:6400 2018-08:7700
            2018-09:7000 2018-10:6600 2018-11:6350 2018-12:4000 2019-01:3700 2019-02:3450 2019-03:3800
            2019-04:4100 2019-05:5350 2019-06:8550 2019-07:10750 2019-08:10100 2019-09:9600 2019-10:8300
            2019-11:9200 2019-12:7550 2020-01:7200 2020-02:9350 2020-03:8550 2020-04:6650 2020-05:8650
            2020-06:9450 2020-07:9150 2020-08:11350 2020-09:11650 2020-10:10800 2020-11:13800 2020-12:19700
            2021-01:29400 2021-02:33100 2021-03:45200 2021-04:58800 2021-05:57700 2021-06:37300 2021-07:35000
            2021-08:41500 2021-09:47100 2021-10:43800 2021-11:61300 2021-12:57000 2022-01:46300 2022-02:38500
            2022-03:44400 2022-04:45500 2022-05:37700 2022-06:31800 2022-07:19300 2022-08:23300 2022-09:20000
            2022-10:19400 2022-11:20500 2022-12:17200 2023-01:16600 2023-02:23100 2023-03:23600 2023-04:28500
            2023-05:29300 2023-06:27200 2023-07:30500 2023-08:29200 2023-09:25900 2023-10:27000 2023-11:34700
            2023-12:37700 2024-01:42300 2024-02:42600 2024-03:61200 2024-04:71300 2024-05:60600 2024-06:67500
            2024-07:62700 2024-08:64600 2024-09:58900 2024-10:63300 2024-11:70200 2024-12:96400 2025-01:94400
            2025-02:102400 2025-03:84300 2025-04:82500 2025-05:94200 2025-06:104600 2025-07:107100
            2025-08:115800 2025-09:108200 2025-10:114000
            """;

    private static final String ETH = """
            2015-08:2.8 2015-09:1.35 2015-10:0.73 2015-11:1.0 2015-12:0.87 2016-01:0.93 2016-02:2.2 2016-03:5.6
            2016-04:11.5 2016-05:8.8 2016-06:14 2016-07:12.5 2016-08:11.5 2016-09:12.2 2016-10:13.1 2016-11:11.0
            2016-12:8.2 2017-01:8.2 2017-02:10.8 2017-03:16 2017-04:50 2017-05:80 2017-06:230 2017-07:290
            2017-08:210 2017-09:390 2017-10:300 2017-11:305 2017-12:465 2018-01:750 2018-02:1150 2018-03:860
            2018-04:400 2018-05:670 2018-06:580 2018-07:455 2018-08:430 2018-09:280 2018-10:230 2018-11:200
            2018-12:115 2019-01:140 2019-02:106 2019-03:136 2019-04:141 2019-05:162 2019-06:268 2019-07:293
            2019-08:218 2019-09:172 2019-10:180 2019-11:182 2019-12:151 2020-01:130 2020-02:180 2020-03:220
            2020-04:135 2020-05:207 2020-06:230 2020-07:225 2020-08:345 2020-09:435 2020-10:360 2020-11:385
            2020-12:615 2021-01:735 2021-02:1310 2021-03:1420 2021-04:1920 2021-05:2770 2021-06:2700
            2021-07:2270 2021-08:2540 2021-09:3430 2021-10:3000 2021-11:4290 2021-12:4630 2022-01:3680
            2022-02:2680 2022-03:2920 2022-04:3280 2022-05:2730 2022-06:1820 2022-07:1070 2022-08:1680
            2022-09:1550 2022-10:1330 2022-11:1570 2022-12:1290 2023-01:1200 2023-02:1590 2023-03:1600
            2023-04:1820 2023-05:1870 2023-06:1870 2023-07:1930 2023-08:1860 2023-09:1650 2023-10:1670
            2023-11:1810 2023-12:2050 2024-01:2280 2024-02:2280 2024-03:3340 2024-04:3610 2024-05:3010
            2024-06:3780 2024-07:3430 2024-08:3230 2024-09:2510 2024-10:2600 2024-11:2510 2024-12:3700
            2025-01:3350 2025-02:3300 2025-03:2200 2025-04:1820 2025-05:1800 2025-06:2530 2025-07:2490
            2025-08:3700 2025-09:4370 2025-10:4140
            """;

    private static final String DOGE = """
            2013-12:0.0005 2014-01:0.0015 2014-02:0.0014 2014-03:0.0005 2014-04:0.0005 2014-05:0.00045
            2014-06:0.0004 2014-07:0.00025 2014-08:0.00025 2014-09:0.0002 2014-10:0.0002 2014-11:0.0002
            2014-12:0.00025 2015-01:0.00018 2015-04:0.00013 2015-07:0.00015 2015-10:0.00012 2015-12:0.00015
            2016-01:0.00025 2016-04:0.00025 2016-07:0.00024 2016-10:0.00022 2016-12:0.00022 2017-01:0.00022
            2017-03:0.0003 2017-05:0.001 2017-06:0.003 2017-07:0.0018 2017-08:0.0017 2017-09:0.0012
            2017-10:0.001 2017-11:0.0011 2017-12:0.0023 2018-01:0.0083 2018-02:0.0047 2018-03:0.0038
            2018-04:0.0026 2018-05:0.0055 2018-06:0.0034 2018-07:0.0025 2018-08:0.0026 2018-09:0.0054
            2018-10:0.0045 2018-11:0.0045 2018-12:0.0023 2019-01:0.0024 2019-03:0.002 2019-04:0.0026
            2019-05:0.0023 2019-06:0.0032 2019-07:0.003 2019-09:0.0025 2019-12:0.002 2020-01:0.002
            2020-03:0.0019 2020-05:0.0026 2020-07:0.0026 2020-09:0.0028 2020-11:0.0026 2020-12:0.0035
            2021-01:0.0057 2021-02:0.035 2021-03:0.054 2021-04:0.061 2021-05:0.39 2021-06:0.33 2021-07:0.25
            2021-08:0.20 2021-09:0.28 2021-10:0.21 2021-11:0.29 2021-12:0.21 2022-01:0.17 2022-02:0.14
            2022-03:0.12 2022-04:0.14 2022-05:0.13 2022-06:0.08 2022-07:0.066 2022-08:0.069 2022-09:0.063
            2022-10:0.06 2022-11:0.12 2022-12:0.10 2023-01:0.07 2023-02:0.09 2023-03:0.08 2023-04:0.077
            2023-05:0.072 2023-06:0.068 2023-07:0.067 2023-08:0.08 2023-09:0.064 2023-10:0.063 2023-11:0.069
            2023-12:0.085 2024-01:0.09 2024-02:0.081 2024-03:0.17 2024-04:0.20 2024-05:0.13 2024-06:0.16
            2024-07:0.12 2024-08:0.12 2024-09:0.10 2024-10:0.11 2024-11:0.16 2024-12:0.40 2025-01:0.33
            2025-02:0.33 2025-03:0.20 2025-04:0.17 2025-05:0.18 2025-06:0.19 2025-07:0.16 2025-08:0.23
            2025-09:0.22 2025-10:0.25
            """;

    /** Parsed points per coin: epoch days and log prices, sorted. */
    private record Series(double[] days, double[] logPrices) {
    }

    private static final Map<Coin, Series> SERIES = new EnumMap<>(Coin.class);

    static {
        SERIES.put(Coin.BITCOIN, parse(BTC));
        SERIES.put(Coin.ETHEREUM, parse(ETH));
        SERIES.put(Coin.DOGECOIN, parse(DOGE));
    }

    private PriceHistory() {
    }

    static Series parse(String table) {
        List<double[]> points = new ArrayList<>();
        for (String token : table.trim().split("\\s+")) {
            String[] parts = token.split(":");
            YearMonth month = YearMonth.parse(parts[0]);
            points.add(new double[]{month.atDay(1).toEpochDay(), Math.log(Double.parseDouble(parts[1]))});
        }
        double[] days = new double[points.size()], logs = new double[points.size()];
        for (int i = 0; i < points.size(); i++) {
            days[i] = points.get(i)[0];
            logs[i] = points.get(i)[1];
            if (i > 0 && days[i] <= days[i - 1]) throw new IllegalStateException("price table not sorted at " + i);
        }
        return new Series(days, logs);
    }

    public static LocalDate launchDate(Coin coin) {
        return LAUNCH.get(coin);
    }

    public static boolean launched(Coin coin, double epochDay) {
        return epochDay >= LAUNCH.get(coin).toEpochDay();
    }

    /** Epoch day of the last built-in price of a coin. */
    public static double lastKnownDay(Coin coin) {
        double[] days = SERIES.get(coin).days();
        return days[days.length - 1];
    }

    /**
     * Reference price in dollars at the given calendar time, or 0 before the coin's launch.
     *
     * @param yearlyDrift price change per year after the last built-in point (0.15 = +15 %/year)
     */
    public static double referencePrice(Coin coin, double epochDay, double yearlyDrift) {
        if (!launched(coin, epochDay)) return 0;
        Series s = SERIES.get(coin);
        double[] days = s.days(), logs = s.logPrices();
        int last = days.length - 1;
        if (epochDay <= days[0]) return Math.exp(logs[0]);
        if (epochDay >= days[last]) {
            double years = (epochDay - days[last]) / 365.25;
            return Math.exp(logs[last]) * Math.pow(1.0 + yearlyDrift, years);
        }
        int lo = 0, hi = last;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (days[mid] <= epochDay) lo = mid;
            else hi = mid;
        }
        double t = (epochDay - days[lo]) / (days[hi] - days[lo]);
        return Math.exp(logs[lo] + (logs[hi] - logs[lo]) * t);
    }

    /** Number of Bitcoin halvings up to the date (real ones, then every 4 years). Other coins: 0. */
    public static int halvings(Coin coin, double epochDay) {
        if (coin != Coin.BITCOIN) return 0;
        int count = 0;
        for (LocalDate date : BTC_HALVINGS) {
            if (epochDay >= date.toEpochDay()) count++;
        }
        LocalDate next = BTC_HALVINGS.getLast().plusYears(4);
        while (epochDay >= next.toEpochDay() && count < 32) {
            count++;
            next = next.plusYears(4);
        }
        return count;
    }

    /** Date of the next halving after the given time, or null if the coin has none. */
    public static LocalDate nextHalving(Coin coin, double epochDay) {
        if (coin != Coin.BITCOIN) return null;
        for (LocalDate date : BTC_HALVINGS) {
            if (date.toEpochDay() > epochDay) return date;
        }
        LocalDate next = BTC_HALVINGS.getLast().plusYears(4);
        while (next.toEpochDay() <= epochDay) next = next.plusYears(4);
        return next;
    }

    /** Whether the date is after Ethereum's switch to proof of stake. */
    public static boolean afterMerge(double epochDay) {
        return epochDay >= THE_MERGE.toEpochDay();
    }
}
