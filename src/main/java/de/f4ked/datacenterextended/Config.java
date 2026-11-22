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

    public static final ModConfigSpec.BooleanValue GIVE_HANDBOOK = BUILDER
            .comment("If true, players get the Data Center Handbook (the guide book) when they join a world for the first time.")
            .define("giveHandbook", true);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- energy
    static {
        BUILDER.comment("Forge Energy (FE) integration").push("energy");
    }

    public static final ModConfigSpec.BooleanValue REQUIRE_ENERGY = BUILDER
            .comment("If true, racks need FE (from generators/cables of other mods) to run their servers.",
                    "If false, power is only a PSU-capacity mechanic and racks run for free.")
            .define("requireEnergy", true);

    public static final ModConfigSpec.DoubleValue FE_PER_WATT = BUILDER
            .comment("FE per tick for each watt a server draws, divided by the PSU efficiency.",
                    "Default 0.1: a 500 W server with an 80+ Gold PSU (87%) uses ~58 FE/t.")
            .defineInRange("fePerWattPerTick", 0.1, 0.0, 1000.0);

    public static final ModConfigSpec.IntValue ENERGY_CAPACITY = BUILDER
            .comment("FE buffer of each rack block. A merged rack of 3 blocks holds 3x this amount.")
            .defineInRange("energyCapacity", 200_000, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue MAX_ENERGY_INPUT = BUILDER
            .comment("Maximum FE per tick each rack block accepts.")
            .defineInRange("maxEnergyInput", 20_000, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue ELECTRICITY_BILLS = BUILDER
            .comment("If true, racks don't use FE. Instead, the rack's owner pays an electricity bill in dollars from their wallet.",
                    "When the wallet can't cover the bill, the rack shuts down until the debt is paid. Overrides requireEnergy.")
            .define("electricityBills", false);

    public static final ModConfigSpec.DoubleValue PRICE_PER_KWH = BUILDER
            .comment("Dollars per kWh (wall power: server watts / PSU efficiency + cooling) per real-time hour.",
                    "The mod's economy is scaled up: default $40/kWh = a Pentium 4 server costs ~$6/h and earns ~$24/h.")
            .defineInRange("pricePerKWh", 40.0, 0.0, 1.0E9);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- cooling
    static {
        BUILDER.comment("Heat and cooling. Every watt a server draws becomes heat. Cooling items (rack fans, water cooling) in the",
                "rack's cooling slots remove it. A rack that runs hotter than throttleTemperature slows its servers down.").push("cooling");
    }

    public static final ModConfigSpec.BooleanValue HEAT_ENABLED = BUILDER
            .comment("If false, racks never heat up and cooling items do nothing.")
            .define("enabled", true);

    public static final ModConfigSpec.IntValue PASSIVE_COOLING = BUILDER
            .comment("Watts of heat every rack block removes without any cooling item (airflow through the case).")
            .defineInRange("passiveCoolingPerSegment", 300, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue THROTTLE_TEMPERATURE = BUILDER
            .comment("°C above which servers throttle. A rack reaches this temperature when the heat equals its cooling.")
            .defineInRange("throttleTemperature", 70.0, 1.0, 1000.0);

    public static final ModConfigSpec.DoubleValue CRITICAL_TEMPERATURE = BUILDER
            .comment("°C at which servers run at minSpeed. Between throttleTemperature and this, speed drops linearly.")
            .defineInRange("criticalTemperature", 95.0, 1.0, 1000.0);

    public static final ModConfigSpec.DoubleValue MIN_SPEED = BUILDER
            .comment("Speed multiplier of a rack at (or above) criticalTemperature.")
            .defineInRange("minSpeed", 0.25, 0.0, 1.0);

    public static final ModConfigSpec.BooleanValue BIOME_TEMPERATURE = BUILDER
            .comment("If true, the biome sets the ambient temperature (a desert or the Nether is hot, a snowy biome cools the rack).")
            .define("biomeAmbient", true);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- wear
    static {
        BUILDER.comment("Hardware wear. Parts of running servers slowly lose condition. Worn CPUs/GPUs/ASICs are slower,",
                "a part at 0% is broken and stops the server. Repair or sell parts in the Service tab of the wallet screen.").push("wear");
    }

    public static final ModConfigSpec.BooleanValue WEAR_ENABLED = BUILDER
            .comment("If false, parts never wear out (existing wear stays).")
            .define("enabled", true);

    public static final ModConfigSpec.DoubleValue WEAR_PER_HOUR = BUILDER
            .comment("Condition lost per real-time hour of running (0.03 = 3%/h, a part lasts ~33 hours).")
            .defineInRange("lossPerHour", 0.03, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue HEAT_WEAR_MULTIPLIER = BUILDER
            .comment("Wear multiplier at the critical temperature (scales linearly from 1.0 at the throttle temperature).")
            .defineInRange("heatMultiplier", 3.0, 1.0, 100.0);

    public static final ModConfigSpec.DoubleValue WORN_MIN_PERFORMANCE = BUILDER
            .comment("Performance of a CPU/GPU/ASIC at 1% condition. Scales linearly up to 1.0 at 100%.")
            .defineInRange("minPerformance", 0.7, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue REPAIR_COST = BUILDER
            .comment("Repair cost as a fraction of the shop price, per 100% of lost condition.")
            .defineInRange("repairCostFactor", 0.5, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue RESALE_FACTOR = BUILDER
            .comment("Second-hand price as a fraction of the shop price, multiplied by the condition.")
            .defineInRange("resaleFactor", 0.4, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue WARNING_THRESHOLD = BUILDER
            .comment("The rack's owner gets a warning when a part drops below this condition (and when it breaks). 0 = only when it breaks.")
            .defineInRange("warningThreshold", 0.10, 0.0, 1.0);

    public static final ModConfigSpec.BooleanValue WEAR_TOASTS = BUILDER
            .comment("If true, wear warnings also pop up as a toast (in addition to the chat message).")
            .define("toasts", true);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- memory
    static {
        BUILDER.comment("Memory (RAM). Every CPU recommends an amount of RAM; less RAM slows the server's compute down.").push("memory");
    }

    public static final ModConfigSpec.BooleanValue REQUIRE_MEMORY = BUILDER
            .comment("If true, a server needs at least one memory module to boot.")
            .define("requireMemory", true);

    public static final ModConfigSpec.DoubleValue MEMORY_STARVED_FACTOR = BUILDER
            .comment("Compute multiplier of a server with (almost) no RAM. Scales linearly up to 1.0 at the recommended amount.")
            .defineInRange("starvedFactor", 0.25, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue MEMORY_SPEED_BONUS = BUILDER
            .comment("Compute bonus per 1000 MT/s of memory speed. All modules run at the speed of the slowest one.",
                    "Default 0.03: DDR2-800 = +2.4%, DDR5-6000 = +18%.")
            .defineInRange("speedBonusPer1000MTs", 0.03, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue MEMORY_CHANNEL_BONUS = BUILDER
            .comment("Compute bonus when 2 or more memory modules are installed (dual/multi channel).")
            .defineInRange("multiChannelBonus", 0.10, 0.0, 10.0);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- mining
    static {
        BUILDER.comment("Crypto mining (uses GPU hashrate). Coin difficulty and prices are in the [market] section.").push("mining");
    }

    public static final ModConfigSpec.DoubleValue CPU_MINING_FACTOR = BUILDER
            .comment("Fraction of CPU compute that also counts as hashrate while mining. 1 KB/s => factor * 1 KH/s. 0 disables CPU mining.")
            .defineInRange("cpuMiningFactor", 0.0, 0.0, 1000.0);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- hosting
    static {
        BUILDER.comment("Hosting. Customers sign up for Shared / VPS / Dedicated plans while the rack has spare compute and pay",
                "their monthly fee per hour. When the rack can't deliver the compute it sold (overloaded), customers cancel.").push("hosting");
    }

    public static final ModConfigSpec.IntValue CUSTOMER_INTERVAL = BUILDER
            .comment("Ticks between customer checks (sign-ups and cancellations).")
            .defineInRange("customerIntervalTicks", 200, 20, 72_000);

    public static final ModConfigSpec.DoubleValue SIGNUP_CHANCE = BUILDER
            .comment("Chance per check that a new customer signs up (if a plan fits into the spare compute).",
                    "Multiplied by the satisfaction squared. A rack without customers always gets its first one.")
            .defineInRange("signupChance", 0.5, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue OVERLOAD_CANCEL_CHANCE = BUILDER
            .comment("Chance per check and customer to cancel while overloaded, times the share of compute that is missing.")
            .defineInRange("overloadCancelChance", 0.5, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue CHURN_CHANCE = BUILDER
            .comment("Chance per check and customer to cancel for no reason (keeps the customer base moving).")
            .defineInRange("churnChance", 0.002, 0.0, 1.0);

    /** Per-plan settings, indexed like {@link de.f4ked.datacenterextended.hosting.HostingPlan}. */
    public static final PlanSettings[] PLANS = {
            plan("shared", 2, 12.0, 6),
            plan("vps", 16, 110.0, 3),
            plan("dedicated", 128, 900.0, 1),
    };

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

    // ---------------------------------------------------------------- economy
    static {
        BUILDER.comment("Wallets. Every player has a wallet with dollars and crypto coins; racks pay into their owner's wallet.").push("economy");
    }

    public static final ModConfigSpec.DoubleValue STARTING_BALANCE = BUILDER
            .comment("Dollars a new player starts with (enough for a first server from the shop).")
            .defineInRange("startingBalance", 250.0, 0.0, 1.0E12);

    public static final ModConfigSpec.DoubleValue SELL_FEE = BUILDER
            .comment("Fraction of the value kept as exchange fee when selling coins.")
            .defineInRange("sellFee", 0.02, 0.0, 1.0);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- calendar
    static {
        BUILDER.comment("In-game calendar. Crypto prices, halvings and hardware release dates follow real history along this date.").push("calendar");
    }

    public static final ModConfigSpec.ConfigValue<String> START_DATE = BUILDER
            .comment("Date (YYYY-MM-DD) a new world starts at. Use /datacenter date set to jump in an existing world.")
            .define("startDate", "2010-07-01", Config::isValidDate);

    public static final ModConfigSpec.IntValue TICKS_PER_DAY = BUILDER
            .comment("Ticks per calendar day. 2400 = 2 real minutes (one real hour is about a month). 24000 = one Minecraft day.")
            .defineInRange("ticksPerDay", 2400, 20, 24_000_000);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- market
    static {
        BUILDER.comment("Crypto market. Prices follow approximate real monthly prices for the calendar date, with a little random noise.",
                "After the end of the built-in history (2025-10) prices drift by futureDrift per year.").push("market");
    }

    public static final ModConfigSpec.IntValue MARKET_UPDATE_TICKS = BUILDER
            .comment("Ticks between price updates (1200 = one minute). The chart keeps the last 288 updates.")
            .defineInRange("updateTicks", 1200, 20, 1_728_000);

    /** Per-coin settings, indexed like {@link de.f4ked.datacenterextended.economy.Coin}. */
    public static final CoinSettings[] COINS = {
            coin("bitcoin", 1500.0, 0.02, 0.10, 2.7E11, 0.15, 1.0),
            coin("dogecoin", 0.25, 0.05, 0.10, 3.6E7, 0.0, 1.0),
            coin("ethereum", 120.0, 0.03, 0.10, 2.0E10, 0.10, 0.1),
    };

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------- shop
    static {
        BUILDER.comment("Hardware shop. Prices come from the datacenterextended:shop_price data map.").push("shop");
    }

    public static final ModConfigSpec.BooleanValue REMOTE_SHOP = BUILDER
            .comment("If true, the shop can also be used from the wallet screen (keybind) anywhere, not only at a Hardware Shop block.")
            .define("remoteShop", true);

    public static final ModConfigSpec.DoubleValue SHOP_PRICE_MULTIPLIER = BUILDER
            .comment("Multiplier for all shop prices.")
            .defineInRange("priceMultiplier", 1.0, 0.0, 1.0E6);

    public static final ModConfigSpec.BooleanValue RELEASE_DATES = BUILDER
            .comment("If true, hardware can only be bought after its real release date (datacenterextended:release_date data map).")
            .define("releaseDates", true);

    public static final ModConfigSpec.DoubleValue YEARLY_PRICE_DROP = BUILDER
            .comment("Shop prices drop by this fraction per year after an item's release (0.15 = -15%/year).")
            .defineInRange("yearlyPriceDrop", 0.15, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue MIN_PRICE_FACTOR = BUILDER
            .comment("Old hardware never gets cheaper than this fraction of its launch price.")
            .defineInRange("minPriceFactor", 0.3, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue PREBUILT_MARKUP = BUILDER
            .comment("Prebuilt servers cost the sum of their parts times this (assembly and warranty).")
            .defineInRange("prebuiltMarkup", 1.35, 1.0, 100.0);

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

    // ---------------------------------------------------------------- power generation
    static {
        BUILDER.comment("Generators, batteries, power cables and the grid connection.").push("power");
    }

    public static final ModConfigSpec.DoubleValue SOLAR_OUTPUT = BUILDER
            .comment("Output multiplier of solar panels.").defineInRange("solarOutput", 1.0, 0.0, 1000.0);
    public static final ModConfigSpec.DoubleValue WIND_OUTPUT = BUILDER
            .comment("Output multiplier of wind turbines.").defineInRange("windOutput", 1.0, 0.0, 1000.0);
    public static final ModConfigSpec.DoubleValue WATER_OUTPUT = BUILDER
            .comment("Output multiplier of water wheels and hydro turbines.").defineInRange("waterOutput", 1.0, 0.0, 1000.0);
    public static final ModConfigSpec.DoubleValue COMBUSTION_OUTPUT = BUILDER
            .comment("Output multiplier of combustion generators and gas turbines.").defineInRange("combustionOutput", 1.0, 0.0, 1000.0);
    public static final ModConfigSpec.DoubleValue GEOTHERMAL_OUTPUT = BUILDER
            .comment("Output multiplier of geothermal plants.").defineInRange("geothermalOutput", 1.0, 0.0, 1000.0);
    public static final ModConfigSpec.DoubleValue NUCLEAR_OUTPUT = BUILDER
            .comment("Output multiplier of the small modular reactor.").defineInRange("nuclearOutput", 1.0, 0.0, 1000.0);
    public static final ModConfigSpec.DoubleValue CABLE_TRANSFER = BUILDER
            .comment("Multiplier of the FE/t power cables can move.").defineInRange("cableTransferMultiplier", 1.0, 0.01, 1000.0);
    public static final ModConfigSpec.IntValue MAX_POWER_CABLES = BUILDER
            .comment("Maximum power cables scanned per network. Larger networks are cut off (protects servers).")
            .defineInRange("maxCablesPerNetwork", 4096, 1, 1_000_000);
    public static final ModConfigSpec.DoubleValue FUEL_ROD_DAYS = BUILDER
            .comment("Calendar days a fuel rod lasts at full reactor power.").defineInRange("fuelRodDays", 7.0, 0.01, 10000.0);
    public static final ModConfigSpec.BooleanValue SELF_CONSUMPTION = BUILDER
            .comment("Electricity bill mode: racks use FE from their buffer first and only the rest goes on the bill.")
            .define("billSelfConsumption", true);
    public static final ModConfigSpec.DoubleValue GRID_EXPORT_PRICE = BUILDER
            .comment("Dollars per kWh (per real-time hour, like pricePerKWh) the grid pays for exported energy in 2010.",
                    "Keep it well below pricePerKWh: running servers should always earn more than selling the same energy.")
            .defineInRange("gridExportPricePerKWh", 3.0, 0.0, 1.0E9);
    public static final ModConfigSpec.DoubleValue GRID_YEARLY_GROWTH = BUILDER
            .comment("Yearly growth of the export price after 2010 (0.065 = x2.6 by 2025).")
            .defineInRange("gridYearlyGrowth", 0.065, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue GRID_STANDING_CHARGE = BUILDER
            .comment("Dollars per calendar day a grid connection costs. Taken from that day's export earnings only.")
            .defineInRange("gridStandingCharge", 0.01, 0.0, 1.0E9);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    /** Output multiplier of a generator family. */
    public static double outputMultiplier(de.f4ked.datacenterextended.power.GeneratorFamily family) {
        return switch (family) {
            case SOLAR -> safe(SOLAR_OUTPUT, 1.0);
            case WIND -> safe(WIND_OUTPUT, 1.0);
            case WATER -> safe(WATER_OUTPUT, 1.0);
            case COMBUSTION -> safe(COMBUSTION_OUTPUT, 1.0);
            case GEOTHERMAL -> safe(GEOTHERMAL_OUTPUT, 1.0);
            case NUCLEAR -> safe(NUCLEAR_OUTPUT, 1.0);
        };
    }

    /** Market settings of one coin. */
    public record CoinSettings(ModConfigSpec.DoubleValue basePrice, ModConfigSpec.DoubleValue volatility,
                               ModConfigSpec.DoubleValue reversion, ModConfigSpec.DoubleValue hashesPerCoin,
                               ModConfigSpec.DoubleValue difficultyExponent, ModConfigSpec.DoubleValue futureDrift,
                               ModConfigSpec.DoubleValue postMergeReward) {
    }

    /** Settings of one hosting plan. */
    public record PlanSettings(ModConfigSpec.DoubleValue compute, ModConfigSpec.DoubleValue pricePerHour,
                               ModConfigSpec.IntValue weight) {
    }

    private static CoinSettings coin(String name, double price, double volatility, double reversion, double hashes,
                                     double drift, double postMerge) {
        BUILDER.push(name);
        CoinSettings settings = new CoinSettings(
                BUILDER.comment("Reference price for the difficulty: at this price, mining one coin takes hashesPerCoin hashes.")
                        .defineInRange("basePrice", price, 0.0001, 1.0E12),
                BUILDER.comment("Standard deviation of the random price noise per update (0.02 = about 2%).").defineInRange("volatility", volatility, 0.0, 1.0),
                BUILDER.comment("How strongly the noise is pulled back to the real price per update (0..1).").defineInRange("reversion", reversion, 0.0, 1.0),
                BUILDER.comment("Hashes needed to mine one coin at basePrice (difficulty).").defineInRange("hashesPerCoin", hashes, 1.0, Double.MAX_VALUE),
                BUILDER.comment("Difficulty follows the price: hashesPerCoin * (price / basePrice) ^ this. 1 = income per hash never changes,",
                        "0 = fixed difficulty (income follows the price 1:1). 0.85 keeps mining playable from 2010 to today.")
                        .defineInRange("difficultyExponent", 0.85, 0.0, 2.0),
                BUILDER.comment("Yearly price change after the end of the built-in price history.").defineInRange("futureDrift", drift, -0.9, 10.0),
                BUILDER.comment("Reward multiplier after The Merge (2022-09-15, Ethereum only; 1 = no change).").defineInRange("postMergeReward", postMerge, 0.0, 1.0));
        BUILDER.pop();
        return settings;
    }

    private static PlanSettings plan(String name, double computeKb, double pricePerHour, int weight) {
        BUILDER.push(name);
        PlanSettings settings = new PlanSettings(
                BUILDER.comment("Compute (KB/s) the customer uses.").defineInRange("compute", computeKb, 0.001, 1.0E12),
                BUILDER.comment("Dollars the customer pays per hour while fully served.").defineInRange("pricePerHour", pricePerHour, 0.0, 1.0E12),
                BUILDER.comment("How often this plan is picked compared to the others (0 = never).").defineInRange("weight", weight, 0, 1000));
        BUILDER.pop();
        return settings;
    }

    /** Whether racks need FE (off in electricity bill mode). */
    public static boolean usesFe() {
        return safe(REQUIRE_ENERGY, true) && !safe(ELECTRICITY_BILLS, false);
    }

    private Config() {
    }

    private static boolean isValidDate(Object obj) {
        if (!(obj instanceof String s)) return false;
        try {
            java.time.LocalDate.parse(s);
            return true;
        } catch (java.time.format.DateTimeParseException e) {
            return false;
        }
    }

    /** The configured start date (falls back to 2010-07-01). */
    public static java.time.LocalDate startDate() {
        try {
            return java.time.LocalDate.parse(START_DATE.get());
        } catch (RuntimeException e) {
            return java.time.LocalDate.of(2010, 7, 1);
        }
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
