package de.f4ked.datacenterextended.network;

import de.f4ked.datacenterextended.Config;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Locale;
import java.util.function.Supplier;

/** Data cable tiers; capacities (bytes/s) come from the config. */
public enum CableTier {
    COAX(() -> Config.COAX_CAPACITY, 1_310_720L),
    CAT5E(() -> Config.CAT5E_CAPACITY, 131_072_000L),
    CAT6A(() -> Config.CAT6A_CAPACITY, 1_342_177_280L),
    FIBER(() -> Config.FIBER_CAPACITY, 13_421_772_800L);

    private final Supplier<ModConfigSpec.LongValue> config;
    private final long fallback;

    CableTier(Supplier<ModConfigSpec.LongValue> config, long fallback) {
        this.config = config;
        this.fallback = fallback;
    }

    public long capacity() {
        return Config.safe(config.get(), fallback);
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT) + "_cable";
    }

    public String translationKey() {
        return "block.datacenterextended." + id();
    }
}
