package de.f4ked.datacenterextended.time;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Calendar maths (pure, no Minecraft classes, unit tested). Calendar time is a {@code double} "epoch day": days since
 * 1970-01-01, with the time of day as the fraction (0.5 = noon).
 */
public final class Calendar {
    private Calendar() {
    }

    public static double epochDay(LocalDate date) {
        return date.toEpochDay();
    }

    public static LocalDate date(double epochDay) {
        return LocalDate.ofEpochDay((long) Math.floor(epochDay));
    }

    /** Calendar time after one tick at the given day length. */
    public static double advance(double epochDay, int ticksPerDay) {
        return epochDay + 1.0 / Math.max(1, ticksPerDay);
    }

    /** Hour of the day, 0..23. */
    public static int hour(double epochDay) {
        double fraction = epochDay - Math.floor(epochDay);
        return Math.min(23, (int) (fraction * 24));
    }

    /** Whole calendar hours since 1970 (statistics buckets). */
    public static long epochHour(double epochDay) {
        return (long) Math.floor(epochDay * 24);
    }

    /** Years between two dates as a fraction (365.25-day years). */
    public static double yearsBetween(double fromEpochDay, double toEpochDay) {
        return (toEpochDay - fromEpochDay) / 365.25;
    }

    /** "12 Mar 2017" (month names in English, numbers are locale independent). */
    public static String format(LocalDate date) {
        return date.getDayOfMonth() + " " + date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + date.getYear();
    }

    /** "Mar 2017". */
    public static String formatMonth(LocalDate date) {
        return date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + date.getYear();
    }

    public static String format(double epochDay) {
        return format(date(epochDay));
    }
}
