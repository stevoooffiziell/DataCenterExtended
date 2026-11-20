package de.f4ked.datacenterextended;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Global balancing knobs. Per-item values (component stats, file sizes) are data-driven via data maps:
 * <ul>
 *     <li>{@code data/<namespace>/data_maps/item/hardware.json}</li>
 *     <li>{@code data/<namespace>/data_maps/item/file_size.json}</li>
 * </ul>
 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ---------------------------------------------------------------- general
    static {
        BUILDER.comment("General server behaviour").push("general");
    }

    public static final ModConfigSpec.DoubleValue COMPUTE_MULTIPLIER = BUILDER
            .comment("Global multiplier applied to the compute power (KB/s) of every CPU.")
            .defineInRange("computeMultiplier", 1.0, 0.0, 1_000_000.0);

    public static final ModConfigSpec.DoubleValue HASHRATE_MULTIPLIER = BUILDER
            .comment("Global multiplier applied to the hashrate (MH/s) of every GPU.")
            .defineInRange("hashrateMultiplier", 1.0, 0.0, 1_000_000.0);

    public static final ModConfigSpec.BooleanValue ENFORCE_PSU_CAPACITY = BUILDER
            .comment("If true, a server does not boot when its components draw more watts than its PSU provides.")
            .define("enforcePsuCapacity", true);

    public static final ModConfigSpec.IntValue MAX_OUTPUTS_PER_TICK = BUILDER
            .comment("Upper bound of items a single rack can produce per tick (protects against absurd setups).")
            .defineInRange("maxOutputsPerTick", 64, 1, 4096);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- energy
    static {
        BUILDER.comment("Forge Energy (FE) integration").push("energy");
    }

    public static final ModConfigSpec.BooleanValue REQUIRE_ENERGY = BUILDER
            .comment("If true, racks need FE from cables/generators of other mods to run.",
                    "If false, the PSU alone is enough (power is purely a component-limit mechanic).")
            .define("requireEnergy", false);

    public static final ModConfigSpec.DoubleValue FE_PER_WATT = BUILDER
            .comment("FE consumed per tick for each watt of component draw (before PSU efficiency).")
            .defineInRange("fePerWattPerTick", 0.1, 0.0, 1000.0);

    public static final ModConfigSpec.IntValue ENERGY_CAPACITY = BUILDER
            .comment("Internal FE buffer of a rack.")
            .defineInRange("energyCapacity", 200_000, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue MAX_ENERGY_INPUT = BUILDER
            .comment("Maximum FE per tick a rack accepts.")
            .defineInRange("maxEnergyInput", 20_000, 1, Integer.MAX_VALUE);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- mining
    static {
        BUILDER.comment("Bitcoin mining (uses GPU hashrate)").push("mining");
    }

    public static final ModConfigSpec.DoubleValue HASHES_PER_BITCOIN = BUILDER
            .comment("Hashes required to mine one Bitcoin item. Default: a GTX 460 (1 MH/s) needs 10 minutes.")
            .defineInRange("hashesPerBitcoin", 6.0E8, 1.0, Double.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue CPU_MINING_FACTOR = BUILDER
            .comment("Fraction of CPU compute that also counts as hashrate while mining. 1 KB/s => factor * 1 KH/s. 0 disables CPU mining.")
            .defineInRange("cpuMiningFactor", 0.0, 0.0, 1000.0);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- hosting
    static {
        BUILDER.comment("Hosting (uses CPU compute, pays credits)").push("hosting");
    }

    public static final ModConfigSpec.LongValue BYTES_PER_CREDIT = BUILDER
            .comment("Bytes of compute that must be served to earn one credit. Default 256 KB => an FX-4300 earns ~1 credit / 25 s.")
            .defineInRange("bytesPerCredit", 262_144L, 1L, Long.MAX_VALUE);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- fabrication
    static {
        BUILDER.comment("Fabrication (uses CPU compute to 'download' items by their file size)").push("fabrication");
    }

    public static final ModConfigSpec.DoubleValue FABRICATION_SPEED = BUILDER
            .comment("Multiplier for fabrication speed.")
            .defineInRange("speedMultiplier", 1.0, 0.0, 1_000_000.0);

    public static final ModConfigSpec.BooleanValue ALLOW_UNLISTED_ITEMS = BUILDER
            .comment("If true, items without an entry in the file_size data map can be fabricated using defaultFileSize.")
            .define("allowUnlistedItems", false);

    public static final ModConfigSpec.LongValue DEFAULT_FILE_SIZE = BUILDER
            .comment("File size in bytes for unlisted items (only used when allowUnlistedItems = true). Default 16 MB.")
            .defineInRange("defaultFileSize", 16L * 1024 * 1024, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> FABRICATION_BLACKLIST = BUILDER
            .comment("Item ids that can never be fabricated, regardless of data maps. The item tag",
                    "'#datacenterextended:not_fabricable' works as well.")
            .defineListAllowEmpty("blacklist", List.of("minecraft:bedrock", "minecraft:command_block", "minecraft:barrier"),
                    () -> "minecraft:air", Config::isValidItemId);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- trading
    static {
        BUILDER.comment("Trading terminal").push("trading");
    }

    public static final ModConfigSpec.IntValue CREDITS_PER_BITCOIN = BUILDER
            .comment("Credits paid by the trading terminal for one Bitcoin.")
            .defineInRange("creditsPerBitcoin", 64, 0, 1_000_000);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- network
    static {
        BUILDER.comment("Compute network (data cables between hosting / fabricating racks).",
                "A network's bandwidth is the capacity of its slowest cable. Values in bytes per second.").push("network");
    }

    public static final ModConfigSpec.LongValue COAX_CAPACITY = BUILDER
            .comment("Coax cable (10 Mbit/s). Default 1.25 MB/s.")
            .defineInRange("coaxCapacity", 1_310_720L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.LongValue CAT5E_CAPACITY = BUILDER
            .comment("Cat5e cable (1 Gbit/s). Default 125 MB/s.")
            .defineInRange("cat5eCapacity", 131_072_000L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.LongValue CAT6A_CAPACITY = BUILDER
            .comment("Cat6a cable (10 Gbit/s). Default 1.25 GB/s.")
            .defineInRange("cat6aCapacity", 1_342_177_280L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.LongValue FIBER_CAPACITY = BUILDER
            .comment("Fiber cable (100 Gbit/s). Default 12.5 GB/s.")
            .defineInRange("fiberCapacity", 13_421_772_800L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.IntValue MAX_CABLES_PER_NETWORK = BUILDER
            .comment("Maximum cables scanned per network. Larger networks are cut off (protects servers).")
            .defineInRange("maxCablesPerNetwork", 4096, 1, 1_000_000);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- racks
    static {
        BUILDER.comment("Server racks. A rack segment is one block (4 servers); up to 3 identical racks stack into one rack.").push("racks");
    }

    public static final ModConfigSpec.LongValue FABRICATING_INTAKE_PER_SEGMENT = BUILDER
            .comment("Maximum compute (bytes/s) a fabricating rack segment can use. Surplus is shared over the network.")
            .defineInRange("fabricatingIntakePerSegment", 32L * 1024 * 1024, 0L, Long.MAX_VALUE);

    public static final ModConfigSpec.LongValue HOSTING_CAPACITY_PER_SEGMENT = BUILDER
            .comment("Maximum compute (bytes/s) a hosting rack segment can sell to customers. Surplus is shared over the network.")
            .defineInRange("hostingCapacityPerSegment", 16L * 1024 * 1024, 0L, Long.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue STORAGE_BONUS_PER_TB = BUILDER
            .comment("Hosting income bonus per TB of storage installed in the rack's servers (0.05 = +5% per TB).")
            .defineInRange("storageBonusPerTB", 0.05, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue MAX_STORAGE_BONUS = BUILDER
            .comment("Upper limit of the hosting storage bonus (0.25 = +25%).")
            .defineInRange("maxStorageBonus", 0.25, 0.0, 100.0);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    private static boolean isValidItemId(Object obj) {
        return obj instanceof String s && Identifier.tryParse(s) != null;
    }

    /** Config values throw before the config is loaded (e.g. early client screens); fall back to the default then. */
    public static double safe(ModConfigSpec.DoubleValue value, double fallback) {
        try {
            return value.get();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }

    public static long safe(ModConfigSpec.LongValue value, long fallback) {
        try {
            return value.get();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }

    public static int safe(ModConfigSpec.IntValue value, int fallback) {
        try {
            return value.get();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }

    public static boolean safe(ModConfigSpec.BooleanValue value, boolean fallback) {
        try {
            return value.get();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }

    public static boolean isBlacklisted(Item item) {
        String key = BuiltInRegistries.ITEM.getKey(item).toString();
        return FABRICATION_BLACKLIST.get().contains(key);
    }
}
