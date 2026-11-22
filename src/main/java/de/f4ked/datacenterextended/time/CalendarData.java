package de.f4ked.datacenterextended.time;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.Datacenterextended;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.time.LocalDate;

/**
 * The world's calendar date, saved with the overworld. It starts at the configured start date and advances one
 * {@code 1 / ticksPerDay} per server tick (independent of {@code /time set}).
 */
public final class CalendarData extends SavedData {
    private static final long UNSET = Long.MIN_VALUE;

    private static final Codec<CalendarData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.optionalFieldOf("day", UNSET).forGetter(c -> c.day),
            Codec.INT.optionalFieldOf("tick", 0).forGetter(c -> c.tickOfDay)
    ).apply(i, CalendarData::new));

    public static final SavedDataType<CalendarData> TYPE =
            new SavedDataType<>(Datacenterextended.id("calendar"), CalendarData::new, CODEC);

    /** Whole days since 1970 plus ticks into the current day (exact, no rounding drift). */
    private long day;
    private int tickOfDay;

    public CalendarData() {
        this(UNSET, 0);
    }

    private CalendarData(long day, int tickOfDay) {
        this.day = day;
        this.tickOfDay = tickOfDay;
    }

    public static CalendarData get(MinecraftServer server) {
        CalendarData data = server.overworld().getDataStorage().computeIfAbsent(TYPE);
        if (data.day == UNSET) {
            data.day = Config.startDate().toEpochDay();
            data.tickOfDay = 0;
            data.setDirty();
        }
        return data;
    }

    /** Shortcut: the current calendar time of the server. */
    public static double now(MinecraftServer server) {
        return get(server).epochDay();
    }

    private static int ticksPerDay() {
        return Math.max(1, Config.safe(Config.TICKS_PER_DAY, 2400));
    }

    /** Calendar time: days since 1970 with the time of day as fraction. */
    public double epochDay() {
        return day + Math.min(1.0 - 1e-9, (double) tickOfDay / ticksPerDay());
    }

    public LocalDate today() {
        return LocalDate.ofEpochDay(day);
    }

    public void tick() {
        if (++tickOfDay >= ticksPerDay()) {
            tickOfDay = 0;
            day++;
        }
        setDirty();
    }

    /** Jumps to the start of the given date. */
    public void set(LocalDate date) {
        day = date.toEpochDay();
        tickOfDay = 0;
        setDirty();
    }
}
