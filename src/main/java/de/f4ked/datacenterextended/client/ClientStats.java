package de.f4ked.datacenterextended.client;

import de.f4ked.datacenterextended.economy.StatsLedger;
import de.f4ked.datacenterextended.network.ModPayloads;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** The local player's statistics as last sent by the server, per scale. */
public final class ClientStats {
    private static final Map<StatsLedger.Scale, StatsLedger.Series> SERIES = new EnumMap<>(StatsLedger.Scale.class);

    private ClientStats() {
    }

    static void set(ModPayloads.StatsSync payload) {
        StatsLedger.Scale scale = StatsLedger.Scale.byId(payload.scale());
        int categories = StatsLedger.CATEGORIES;
        long[][] values = new long[scale.buckets()][categories];
        List<Long> flat = payload.values();
        for (int b = 0; b < scale.buckets(); b++) {
            for (int c = 0; c < categories; c++) {
                int i = b * categories + c;
                values[b][c] = i < flat.size() ? flat.get(i) : 0;
            }
        }
        SERIES.put(scale, new StatsLedger.Series(scale, payload.firstKey(), values));
    }

    public static StatsLedger.@Nullable Series get(StatsLedger.Scale scale) {
        return SERIES.get(scale);
    }
}
