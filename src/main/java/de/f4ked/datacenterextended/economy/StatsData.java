package de.f4ked.datacenterextended.economy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.f4ked.datacenterextended.Datacenterextended;
import de.f4ked.datacenterextended.time.CalendarData;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Income / expense statistics of every player ({@link StatsLedger}), saved with the overworld. */
public final class StatsData extends SavedData {
    private static final Codec<List<List<Long>>> ROWS = Codec.LONG.listOf().listOf();
    private static final Codec<StatsLedger> LEDGER = RecordCodecBuilder.create(i -> i.group(
            ROWS.optionalFieldOf("hourly", List.of()).forGetter(StatsLedger::hourlyRows),
            ROWS.optionalFieldOf("daily", List.of()).forGetter(StatsLedger::dailyRows)
    ).apply(i, StatsLedger::of));
    private static final Codec<StatsData> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, LEDGER)
            .xmap(StatsData::new, data -> data.ledgers);

    public static final SavedDataType<StatsData> TYPE =
            new SavedDataType<>(Datacenterextended.id("stats"), StatsData::new, CODEC);

    private final Map<UUID, StatsLedger> ledgers;

    public StatsData() {
        this(Map.of());
    }

    private StatsData(Map<UUID, StatsLedger> ledgers) {
        this.ledgers = new HashMap<>(ledgers);
    }

    public static StatsData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** Records an amount (cents) for the player at the current calendar time. */
    public static void record(MinecraftServer server, @Nullable UUID player, StatsLedger.Category category, long cents) {
        if (player == null || cents == 0) return;
        StatsData data = get(server);
        data.ledgers.computeIfAbsent(player, id -> new StatsLedger()).record(category, cents, CalendarData.now(server));
        data.setDirty();
    }

    public StatsLedger.Series series(UUID player, StatsLedger.Scale scale, double epochDay) {
        StatsLedger ledger = ledgers.get(player);
        return (ledger != null ? ledger : new StatsLedger()).series(scale, epochDay);
    }
}
