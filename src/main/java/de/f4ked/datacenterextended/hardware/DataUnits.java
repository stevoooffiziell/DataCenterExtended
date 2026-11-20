package de.f4ked.datacenterextended.hardware;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Binary data units (1 KB = 1024 B) plus hashrate formatting. */
public final class DataUnits {
    public static final long KB = 1024L;
    public static final long MB = KB * 1024L;
    public static final long GB = MB * 1024L;
    public static final long TB = GB * 1024L;

    private static final String[] BYTE_UNITS = {"B", "KB", "MB", "GB", "TB", "PB", "EB"};
    private static final String[] HASH_UNITS = {"H/s", "KH/s", "MH/s", "GH/s", "TH/s", "PH/s", "EH/s"};
    private static final Pattern SIZE = Pattern.compile("^\\s*([0-9]+(?:\\.[0-9]+)?)\\s*([KMGTP]?I?B?)\\s*$", Pattern.CASE_INSENSITIVE);

    private DataUnits() {
    }

    public static long parseBytes(String text) {
        Matcher m = SIZE.matcher(text);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid file size '" + text + "', expected e.g. '512 KB', '1MB' or '2.5 GB'");
        }
        double value = Double.parseDouble(m.group(1));
        String unit = m.group(2).toUpperCase(Locale.ROOT);
        long multiplier = switch (unit.isEmpty() ? 'B' : unit.charAt(0)) {
            case 'K' -> KB;
            case 'M' -> MB;
            case 'G' -> GB;
            case 'T' -> TB;
            case 'P' -> TB * 1024L;
            default -> 1L;
        };
        return Math.round(value * multiplier);
    }

    public static String formatBytes(double bytes) {
        return format(bytes, 1024.0, BYTE_UNITS);
    }

    public static String formatBytesPerSecond(double bytesPerSecond) {
        return formatBytes(bytesPerSecond) + "/s";
    }

    public static String formatHashrate(double hashesPerSecond) {
        return format(hashesPerSecond, 1000.0, HASH_UNITS);
    }

    private static String format(double value, double step, String[] units) {
        int unit = 0;
        while (value >= step && unit < units.length - 1) {
            value /= step;
            unit++;
        }
        if (unit == 0 || value >= 100 || value == Math.floor(value)) {
            return String.format(Locale.ROOT, "%.0f %s", value, units[unit]);
        }
        return String.format(Locale.ROOT, value >= 10 ? "%.1f %s" : "%.2f %s", value, units[unit]);
    }

    /** Human readable duration from seconds, e.g. "1h 4m", "37s". */
    /** 123456 -> "123,456". */
    public static String formatNumber(long value) {
        return String.format(java.util.Locale.ROOT, "%,d", value);
    }

    public static String formatDuration(double seconds) {
        if (Double.isInfinite(seconds) || Double.isNaN(seconds)) return "∞";
        long s = (long) Math.ceil(seconds);
        if (s < 60) return s + "s";
        if (s < 3600) return (s / 60) + "m " + (s % 60) + "s";
        if (s < 86400) return (s / 3600) + "h " + ((s % 3600) / 60) + "m";
        return (s / 86400) + "d " + ((s % 86400) / 3600) + "h";
    }
}
