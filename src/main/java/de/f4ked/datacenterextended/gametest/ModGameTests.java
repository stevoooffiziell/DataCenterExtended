package de.f4ked.datacenterextended.gametest;

import de.f4ked.datacenterextended.power.BatteryBlockEntity;
import de.f4ked.datacenterextended.power.BatteryTier;
import de.f4ked.datacenterextended.power.GeneratorBlockEntity;
import de.f4ked.datacenterextended.power.GeneratorStatus;
import de.f4ked.datacenterextended.power.GeneratorType;
import de.f4ked.datacenterextended.power.GridConnectionBlockEntity;
import de.f4ked.datacenterextended.power.GridTier;
import de.f4ked.datacenterextended.power.PowerBlock;
import de.f4ked.datacenterextended.power.PowerCableTier;
import de.f4ked.datacenterextended.power.PowerNetworkManager;
import de.f4ked.datacenterextended.power.model.SolarModel;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.StartupConfig;
import de.f4ked.datacenterextended.economy.Coin;
import de.f4ked.datacenterextended.economy.EconomyService;
import de.f4ked.datacenterextended.economy.MarketData;
import de.f4ked.datacenterextended.economy.WalletData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import de.f4ked.datacenterextended.Datacenterextended;
import de.f4ked.datacenterextended.block.RackBlockEntity;
import de.f4ked.datacenterextended.block.RackPart;
import de.f4ked.datacenterextended.block.RackStatus;
import de.f4ked.datacenterextended.block.RackType;
import de.f4ked.datacenterextended.block.RackBlock;
import de.f4ked.datacenterextended.hardware.ServerLayout;
import de.f4ked.datacenterextended.hardware.ServerProblem;
import de.f4ked.datacenterextended.hardware.ServerSpec;
import de.f4ked.datacenterextended.hardware.Wear;
import de.f4ked.datacenterextended.economy.ItemValue;
import de.f4ked.datacenterextended.hosting.HostingPlan;
import de.f4ked.datacenterextended.network.CableTier;
import de.f4ked.datacenterextended.network.NetworkInfo;
import de.f4ked.datacenterextended.registry.ModBlocks;
import de.f4ked.datacenterextended.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

import java.time.LocalDate;
import java.util.List;
import de.f4ked.datacenterextended.block.RackComparatorMode;
import de.f4ked.datacenterextended.block.ServerMonitorBlockEntity;
import de.f4ked.datacenterextended.economy.ShopPricing;
import de.f4ked.datacenterextended.hardware.Prebuilt;
import de.f4ked.datacenterextended.time.CalendarData;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * In-world tests, run by {@code ./gradlew runGameTestServer}. Test instances live in
 * {@code data/datacenterextended/test_instance/}. They only run when the namespace is enabled for game tests.
 */
public final class ModGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, Datacenterextended.MODID);

    static {
        TEST_FUNCTIONS.register("network_sharing", () -> ModGameTests::networkSharing);
        TEST_FUNCTIONS.register("rack_merging", () -> ModGameTests::rackMerging);
        TEST_FUNCTIONS.register("hosting_storage_bonus", () -> ModGameTests::hostingStorageBonus);
        TEST_FUNCTIONS.register("rack_energy", () -> ModGameTests::rackEnergy);
        TEST_FUNCTIONS.register("memory_and_sockets", () -> ModGameTests::memoryAndSockets);
        TEST_FUNCTIONS.register("wallet_payout", () -> ModGameTests::walletPayout);
        TEST_FUNCTIONS.register("shop_and_sell", () -> ModGameTests::shopAndSell);
        TEST_FUNCTIONS.register("part_recipes", () -> ModGameTests::partRecipes);
        TEST_FUNCTIONS.register("heat_throttle", () -> ModGameTests::heatThrottle);
        TEST_FUNCTIONS.register("asic_mining", () -> ModGameTests::asicMining);
        TEST_FUNCTIONS.register("calendar_history", () -> ModGameTests::calendarHistory);
        TEST_FUNCTIONS.register("prebuilt_servers", () -> ModGameTests::prebuiltServers);
        TEST_FUNCTIONS.register("comparator_output", () -> ModGameTests::comparatorOutput);
        TEST_FUNCTIONS.register("release_lock", () -> ModGameTests::releaseLock);
        TEST_FUNCTIONS.register("wear_warning", () -> ModGameTests::wearWarning);
        TEST_FUNCTIONS.register("server_monitor", () -> ModGameTests::serverMonitor);
        TEST_FUNCTIONS.register("wear_and_service", () -> ModGameTests::wearAndService);
        TEST_FUNCTIONS.register("hosting_customers", () -> ModGameTests::hostingCustomers);
        TEST_FUNCTIONS.register("solar_daytime", () -> ModGameTests::solarDaytime);
        TEST_FUNCTIONS.register("cable_feeds_rack", () -> ModGameTests::cableFeedsRack);
        TEST_FUNCTIONS.register("battery_stores_and_releases", () -> ModGameTests::batteryStoresAndReleases);
        TEST_FUNCTIONS.register("combustion_burns_coal", () -> ModGameTests::combustionBurnsCoal);
        TEST_FUNCTIONS.register("water_wheel_flowing", () -> ModGameTests::waterWheelFlowing);
        TEST_FUNCTIONS.register("geothermal_lava", () -> ModGameTests::geothermalLava);
        TEST_FUNCTIONS.register("reactor_scram_without_cooling", () -> ModGameTests::reactorScram);
        TEST_FUNCTIONS.register("grid_export_pays_owner", () -> ModGameTests::gridExportPaysOwner);
    }

    private static final LocalDate TEST_DATE = LocalDate.of(2021, 6, 1);
    private static final UUID TEST_OWNER = UUID.fromString("00000000-0000-0000-0000-00000000d0c0");

    private ModGameTests() {
    }

    /** An idle fabricating rack shares its compute over a coax cable with a fabricating rack that has a target. */
    private static void networkSharing(GameTestHelper helper) {
        BlockPos consumer = new BlockPos(0, 1, 0), cable = new BlockPos(1, 1, 0), producer = new BlockPos(2, 1, 0);
        helper.setBlock(consumer, ModBlocks.RACKS.get(RackType.FABRICATING).get());
        helper.setBlock(cable, ModBlocks.CABLES.get(CableTier.COAX).get());
        helper.setBlock(producer, ModBlocks.RACKS.get(RackType.FABRICATING).get());

        RackBlockEntity target = helper.getBlockEntity(consumer, RackBlockEntity.class);
        RackBlockEntity source = helper.getBlockEntity(producer, RackBlockEntity.class);
        target.setItem(RackBlockEntity.SLOT_TARGET, new ItemStack(Items.COBBLESTONE));
        for (int i = 0; i < RackBlockEntity.SERVERS; i++) {
            source.setItem(i, server("server_motherboard", "psu_3200w", items("epyc_9654", "epyc_9654"), items(), items(),
                    items("ddr5_128gb_ecc", "ddr5_128gb_ecc")));
        }
        fillEnergy(target);
        fillEnergy(source);

        helper.succeedWhen(() -> {
            NetworkInfo info = target.networkInfo();
            helper.assertTrue(info != null, "consumer rack is not on a network");
            helper.assertTrue(info.bottleneck() == CableTier.COAX, "bottleneck should be coax");
            helper.assertTrue(info.bandwidthLimited(), "32 MB/s over coax must be bandwidth limited");
            helper.assertTrue(info.transferred() == CableTier.COAX.capacity(), "transfer should equal the coax capacity");
            helper.assertTrue(source.exported() > 0, "producer rack should export compute");
            helper.assertTrue(source.status() == RackStatus.EXPORTING, "producer should report EXPORTING, was " + source.status());
            helper.assertTrue(target.getItem(RackBlockEntity.SLOT_OUTPUT_START).is(Items.COBBLESTONE),
                    "consumer should have fabricated cobblestone with network compute");
        });
    }

    /** 4 stacked racks -> one merged rack of 3 + a single rack; servers in an upper segment count for the controller. */
    private static void rackMerging(GameTestHelper helper) {
        for (int y = 1; y <= 4; y++) {
            helper.setBlock(new BlockPos(0, y, 0), ModBlocks.RACKS.get(RackType.MINING).get());
        }
        helper.assertTrue(part(helper, 1) == RackPart.BOTTOM, "y1 should be BOTTOM, was " + part(helper, 1));
        helper.assertTrue(part(helper, 2) == RackPart.MIDDLE, "y2 should be MIDDLE, was " + part(helper, 2));
        helper.assertTrue(part(helper, 3) == RackPart.TOP, "y3 should be TOP, was " + part(helper, 3));
        helper.assertTrue(part(helper, 4) == RackPart.SINGLE, "y4 should be SINGLE, was " + part(helper, 4));

        RackBlockEntity bottom = helper.getBlockEntity(new BlockPos(0, 1, 0), RackBlockEntity.class);
        RackBlockEntity top = helper.getBlockEntity(new BlockPos(0, 3, 0), RackBlockEntity.class);
        helper.assertTrue(bottom.isController() && !top.isController(), "bottom block must be the controller");
        helper.assertTrue(top.segments().size() == 3, "merged rack should have 3 segments");
        helper.assertTrue(top.computeNode() == null, "mining racks must not join compute networks");

        top.setItem(0, server("office_motherboard", "psu_300w", items("pentium_4"), items("geforce_gtx_460"), items(),
                items("ddr2_1gb")));
        fillEnergy(bottom);
        bottom.setOwner(TEST_OWNER, "test");

        helper.succeedWhen(() -> {
            helper.assertTrue(bottom.status().ok(), "controller should run, was " + bottom.status());
            helper.assertTrue(bottom.localRate() >= 1_000_000, "GTX 460 server should give >= 1 MH/s, was " + bottom.localRate());
        });
    }

    /** 8 TB of storage in a hosting server gives the (capped) storage bonus. */
    private static void hostingStorageBonus(GameTestHelper helper) {
        BlockPos pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, ModBlocks.RACKS.get(RackType.HOSTING).get());
        RackBlockEntity rack = helper.getBlockEntity(pos, RackBlockEntity.class);
        rack.setItem(0, server("office_motherboard", "psu_300w", items("pentium_4"), items(), items("enterprise_ssd_8tb"),
                items("ddr2_1gb")));
        fillEnergy(rack);
        rack.setOwner(TEST_OWNER, "test");
        rack.setCustomers(HostingPlan.SHARED, 1);

        helper.succeedWhen(() -> {
            helper.assertTrue(rack.status().ok(), "hosting rack should run, was " + rack.status());
            helper.assertTrue(Math.abs(rack.storageBonus() - 0.25) < 1e-9, "8 TB should hit the 25% cap, was " + rack.storageBonus());
        });
    }

    /** Without FE the rack stops; with FE it runs and drains the buffer by the servers' FE/t. */
    private static void rackEnergy(GameTestHelper helper) {
        // in-memory only (not saved); the other tests fill their buffers, so they pass either way
        Config.REQUIRE_ENERGY.set(true);
        BlockPos pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, ModBlocks.RACKS.get(RackType.HOSTING).get());
        RackBlockEntity rack = helper.getBlockEntity(pos, RackBlockEntity.class);
        ItemStack server = server("gaming_motherboard", "psu_550w", items("core_i9_9900k"), items(), items(), items("ddr4_16gb", "ddr4_16gb"));
        long fePerTick = ServerSpec.of(server).energyPerTick();
        rack.setItem(0, server);
        rack.setOwner(TEST_OWNER, "test");
        rack.setCustomers(HostingPlan.SHARED, 1);

        helper.runAfterDelay(5, () -> {
            helper.assertTrue(rack.status() == RackStatus.NO_ENERGY, "empty buffer should stop the rack, was " + rack.status());
            ((SimpleEnergyHandler) rack.getEnergyHandler()).set(10_000);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getTick() > 7, "waiting for the energy check");
            helper.assertTrue(rack.status().ok(), "powered rack should run, was " + rack.status());
            helper.assertTrue(rack.energyPerTick() == fePerTick, "rack should use " + fePerTick + " FE/t, used " + rack.energyPerTick());
            long stored = rack.getEnergyHandler().getAmountAsLong();
            helper.assertTrue(stored < 10_000 && stored > 0, "buffer should drain, is " + stored);
        });
    }

    /** Wrong socket / no RAM stop a server; RAM below the recommendation slows it down. */
    private static void memoryAndSockets(GameTestHelper helper) {
        ServerSpec wrongSocket = ServerSpec.of(server("x570_motherboard", "psu_550w", items("core_i9_9900k"), items(), items(), items("ddr4_8gb")));
        helper.assertTrue(wrongSocket.problem() == ServerProblem.INCOMPATIBLE_CPU, "LGA1151 CPU on AM4 board, was " + wrongSocket.problem());
        ServerSpec wrongRam = ServerSpec.of(server("x570_motherboard", "psu_550w", items("ryzen_5_3600"), items(), items(), items("ddr5_16gb")));
        helper.assertTrue(wrongRam.problem() == ServerProblem.INCOMPATIBLE_MEMORY, "DDR5 on DDR4 board, was " + wrongRam.problem());
        ServerSpec noRam = ServerSpec.of(server("x570_motherboard", "psu_550w", items("ryzen_5_3600"), items(), items(), items()));
        helper.assertTrue(noRam.problem() == ServerProblem.NO_MEMORY, "no RAM, was " + noRam.problem());

        ServerSpec little = ServerSpec.of(server("x570_motherboard", "psu_550w", items("ryzen_5_3600"), items(), items(), items("ddr4_8gb")));
        ServerSpec plenty = ServerSpec.of(server("x570_motherboard", "psu_550w", items("ryzen_5_3600"), items(), items(), items("ddr4_16gb", "ddr4_16gb")));
        helper.assertTrue(little.boots() && plenty.boots(), "both servers should boot");
        helper.assertTrue(little.memoryFactor() < 1.0, "8 GB of the recommended 16 GB should slow down, factor " + little.memoryFactor());
        helper.assertTrue(plenty.computeBps() > little.computeBps() * 1.4, "dual channel 32 GB should be much faster");
        helper.succeed();
    }

    /** Hosting pays dollars, mining DOGE pays coins into the owner's wallet; a rack without owner reports NO_OWNER. */
    private static void walletPayout(GameTestHelper helper) {
        useTestDate(helper);
        UUID owner = UUID.randomUUID();
        WalletData wallets = WalletData.get(helper.getLevel().getServer());
        long startCents = wallets.wallet(owner).cents();

        BlockPos hostingPos = new BlockPos(0, 1, 0), miningPos = new BlockPos(2, 1, 0), orphanPos = new BlockPos(4, 1, 0);
        helper.setBlock(hostingPos, ModBlocks.RACKS.get(RackType.HOSTING).get());
        helper.setBlock(miningPos, ModBlocks.RACKS.get(RackType.MINING).get());
        helper.setBlock(orphanPos, ModBlocks.RACKS.get(RackType.HOSTING).get());
        RackBlockEntity hosting = helper.getBlockEntity(hostingPos, RackBlockEntity.class);
        RackBlockEntity mining = helper.getBlockEntity(miningPos, RackBlockEntity.class);
        RackBlockEntity orphan = helper.getBlockEntity(orphanPos, RackBlockEntity.class);
        ItemStack cpuServer = server("server_motherboard", "psu_3200w", items("epyc_9654", "epyc_9654"), items(), items(),
                items("ddr5_128gb_ecc", "ddr5_128gb_ecc"));
        hosting.setItem(0, cpuServer.copy());
        orphan.setItem(0, cpuServer.copy());
        mining.setItem(0, server("mining_motherboard", "psu_3200w", items("core_i7_8700k"),
                items("geforce_rtx_4090", "geforce_rtx_4090", "geforce_rtx_4090"), items(), items("ddr4_16gb")));
        for (RackBlockEntity rack : List.of(hosting, mining, orphan)) fillEnergy(rack);
        hosting.setOwner(owner, "test");
        hosting.setCustomers(HostingPlan.DEDICATED, 1);
        hosting.setItem(RackBlockEntity.SLOT_COOLING, new ItemStack(ModItems.WATER_COOLING_KIT.get()));
        mining.setItem(RackBlockEntity.SLOT_COOLING, new ItemStack(ModItems.WATER_COOLING_KIT.get()));
        mining.setOwner(owner, "test");
        mining.setCoin(Coin.DOGECOIN);

        helper.succeedWhen(() -> {
            helper.assertTrue(orphan.status() == RackStatus.NO_OWNER, "rack without owner should report NO_OWNER, was " + orphan.status());
            helper.assertTrue(hosting.status().ok() && mining.status().ok(),
                    "racks should run: " + hosting.status() + " / " + mining.status());
            helper.assertTrue(wallets.wallet(owner).cents() > startCents, "hosting should pay dollars");
            helper.assertTrue(wallets.wallet(owner).coins(Coin.DOGECOIN) > 0, "mining should pay DOGE");
            helper.assertTrue(wallets.wallet(owner).coins(Coin.BITCOIN) == 0, "no BTC when mining DOGE");
        });
    }

    /** Buying takes the price and hands over the item; selling pays price * amount minus the fee. */
    private static void shopAndSell(GameTestHelper helper) {
        useTestDate(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        WalletData wallets = WalletData.get(helper.getLevel().getServer());
        UUID id = player.getUUID();
        wallets.wallet(id);
        long price = EconomyService.priceCents(ModItems.hardware("pentium_4"), CalendarData.now(helper.getLevel().getServer()));
        helper.assertTrue(price > 0, "pentium_4 should be sold in the shop");

        wallets.withdraw(id, wallets.wallet(id).cents());
        helper.assertTrue(EconomyService.buy(player, ModItems.hardware("pentium_4"), 1) == EconomyService.BuyResult.TOO_EXPENSIVE,
                "buying with an empty wallet must fail");
        wallets.deposit(id, price * 2);
        helper.assertTrue(EconomyService.buy(player, ModItems.hardware("pentium_4"), 2) == EconomyService.BuyResult.OK, "buy should succeed");
        helper.assertTrue(wallets.wallet(id).cents() == 0, "balance should be 0 after buying, is " + wallets.wallet(id).cents());
        helper.assertTrue(player.getInventory().countItem(ModItems.hardware("pentium_4")) == 2, "player should own 2 Pentium 4");
        helper.assertTrue(EconomyService.buy(player, ModItems.SERVER_WORKBENCH.get(), 1) != EconomyService.BuyResult.NOT_FOR_SALE,
                "the workbench is sold in the shop");

        MarketData.get(helper.getLevel().getServer()).setPrice(Coin.ETHEREUM, 100.0);
        wallets.depositCoins(id, Coin.ETHEREUM, 2.0);
        helper.assertTrue(EconomyService.sell(player, Coin.ETHEREUM, 3.0) > 0, "selling more than held sells everything held");
        long expected = EconomyService.saleValue(Coin.ETHEREUM, 2.0, 100.0);
        helper.assertTrue(wallets.wallet(id).cents() == expected, "expected " + expected + " cents, got " + wallets.wallet(id).cents());
        helper.assertTrue(wallets.wallet(id).coins(Coin.ETHEREUM) == 0, "all ETH should be sold");
        helper.assertTrue(EconomyService.sell(player, Coin.ETHEREUM, 1.0) == -1, "selling without coins must fail");
        helper.succeed();
    }

    /** Hardware recipes exist (enablePartRecipes defaults to true) and carry the condition. */
    private static void partRecipes(GameTestHelper helper) {
        var recipes = helper.getLevel().getServer().getRecipeManager();
        var key = ResourceKey.create(Registries.RECIPE, Datacenterextended.id("pentium_4"));
        helper.assertTrue(recipes.byKey(key).isPresent() == StartupConfig.ENABLE_PART_RECIPES.get(),
                "pentium_4 recipe presence should follow enablePartRecipes");
        helper.succeed();
    }

    /** A hot rack throttles its servers; a water-cooled rack with a light load stays at full speed. */
    private static void heatThrottle(GameTestHelper helper) {
        BlockPos hotPos = new BlockPos(0, 1, 0), coolPos = new BlockPos(2, 1, 0);
        helper.setBlock(hotPos, ModBlocks.RACKS.get(RackType.HOSTING).get());
        helper.setBlock(coolPos, ModBlocks.RACKS.get(RackType.HOSTING).get());
        RackBlockEntity hot = helper.getBlockEntity(hotPos, RackBlockEntity.class);
        RackBlockEntity cool = helper.getBlockEntity(coolPos, RackBlockEntity.class);
        ItemStack heavy = server("server_motherboard", "psu_3200w", items("epyc_9654", "epyc_9654"), items(), items(),
                items("ddr5_128gb_ecc", "ddr5_128gb_ecc"));
        for (int i = 0; i < RackBlockEntity.SERVERS; i++) hot.setItem(i, heavy.copy());
        cool.setItem(0, server("office_motherboard", "psu_300w", items("pentium_4"), items(), items(), items("ddr2_1gb")));
        cool.setItem(RackBlockEntity.SLOT_COOLING, new ItemStack(ModItems.WATER_COOLING_KIT.get()));
        for (RackBlockEntity rack : List.of(hot, cool)) {
            fillEnergy(rack);
            rack.setOwner(TEST_OWNER, "test");
            rack.setCustomers(HostingPlan.SHARED, 1);
        }
        hot.setTemperature(Config.CRITICAL_TEMPERATURE.get());

        helper.succeedWhen(() -> {
            helper.assertTrue(hot.speedFactor() < 1.0, "a rack at the critical temperature must throttle, speed " + hot.speedFactor());
            helper.assertTrue(hot.status() == RackStatus.OVERHEATING, "hot rack should report OVERHEATING, was " + hot.status());
            helper.assertTrue(hot.heatLoad() > hot.coolingCapacity(), "4 EPYC servers produce more heat than passive cooling removes");
            helper.assertTrue(cool.coolingCapacity() == Config.PASSIVE_COOLING.get() + 12_000L,
                    "passive + water cooling expected, was " + cool.coolingCapacity());
            helper.assertTrue(cool.speedFactor() == 1.0 && cool.temperature() < Config.THROTTLE_TEMPERATURE.get(),
                    "water cooled rack should stay cool, " + cool.temperature() + " C");
        });
    }

    /** An ASIC mines Bitcoin in a bay of its own, but refuses other coins. */
    private static void asicMining(GameTestHelper helper) {
        useTestDate(helper);
        BlockPos btcPos = new BlockPos(0, 1, 0), dogePos = new BlockPos(2, 1, 0);
        helper.setBlock(btcPos, ModBlocks.RACKS.get(RackType.MINING).get());
        helper.setBlock(dogePos, ModBlocks.RACKS.get(RackType.MINING).get());
        RackBlockEntity btc = helper.getBlockEntity(btcPos, RackBlockEntity.class);
        RackBlockEntity doge = helper.getBlockEntity(dogePos, RackBlockEntity.class);
        helper.assertTrue(btc.canPlaceItem(0, new ItemStack(ModItems.hardware("antminer_s9"))), "ASICs go into rack bays");
        btc.setItem(0, new ItemStack(ModItems.hardware("antminer_s9")));
        doge.setItem(0, new ItemStack(ModItems.hardware("antminer_s9")));
        for (RackBlockEntity rack : List.of(btc, doge)) {
            fillEnergy(rack);
            rack.setOwner(TEST_OWNER, "test");
            rack.setItem(RackBlockEntity.SLOT_COOLING, new ItemStack(ModItems.HIGH_FLOW_FAN.get()));
        }
        doge.setCoin(Coin.DOGECOIN);

        helper.succeedWhen(() -> {
            helper.assertTrue(btc.status().ok(), "ASIC should mine BTC, was " + btc.status());
            helper.assertTrue(btc.localRate() >= 100_000_000, "Antminer S9 should give >= 100 MH/s, was " + btc.localRate());
            helper.assertTrue(doge.status() == RackStatus.NO_SERVERS, "ASIC must not mine DOGE, was " + doge.status());
        });
    }

    /** Real dates: launches, halvings, The Merge, reference prices; the market follows the calendar. */
    private static void calendarHistory(GameTestHelper helper) {
        useTestDate(helper);
        double d2012 = LocalDate.of(2012, 6, 1).toEpochDay(), d2013 = LocalDate.of(2013, 1, 1).toEpochDay();
        double d2025 = LocalDate.of(2025, 1, 1).toEpochDay(), d2023 = LocalDate.of(2023, 1, 1).toEpochDay();
        helper.assertTrue(!MarketData.launched(Coin.DOGECOIN, d2012), "Dogecoin did not exist in 2012");
        helper.assertTrue(MarketData.halvings(Coin.BITCOIN, d2013) == 1, "one halving by 2013");
        helper.assertTrue(MarketData.halvings(Coin.BITCOIN, d2025) == 4, "four halvings by 2025");
        helper.assertTrue(MarketData.rewardMultiplier(Coin.BITCOIN, d2025) == 1.0 / 16, "BTC reward 1/16 in 2025");
        helper.assertTrue(Math.abs(MarketData.rewardMultiplier(Coin.ETHEREUM, d2023) - Coin.ETHEREUM.postMergeReward()) < 1e-9,
                "ETH reward is reduced after The Merge");
        double btc2017 = MarketData.referencePrice(Coin.BITCOIN, LocalDate.of(2017, 12, 1).toEpochDay());
        helper.assertTrue(btc2017 > 5_000 && btc2017 < 20_000, "BTC was around $10,000 in Dec 2017, got " + btc2017);
        var server = helper.getLevel().getServer();
        MarketData market = MarketData.get(server);
        market.refresh(CalendarData.now(server));
        double ref = MarketData.referencePrice(Coin.BITCOIN, CalendarData.now(server));
        double price = market.price(Coin.BITCOIN);
        helper.assertTrue(price > ref * 0.2 && price < ref * 5, "market price should follow the real price, " + price + " vs " + ref);
        helper.succeed();
    }

    /** Every prebuilt server boots, and the shop hands out an assembled server for it. */
    private static void prebuiltServers(GameTestHelper helper) {
        for (Prebuilt prebuilt : Prebuilt.values()) {
            ServerSpec spec = ServerSpec.of(prebuilt.assemble());
            helper.assertTrue(spec.boots(), prebuilt + " should boot, problem " + spec.problem());
            double rate = prebuilt.kind() == Prebuilt.RackKind.MINING ? spec.hashesPerSecond() : spec.computeBps();
            helper.assertTrue(rate > 0, prebuilt + " should be useful in its rack type");
            long parts = 0;
            double late = LocalDate.of(2030, 1, 1).toEpochDay();
            for (Item part : prebuilt.parts()) parts += ShopPricing.priceCents(part, late);
            long price = ShopPricing.priceCents(ModItems.PREBUILT.get(prebuilt).get(), late);
            helper.assertTrue(price > parts, prebuilt + " must cost more than its parts (" + price + " vs " + parts + ")");
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        WalletData wallets = WalletData.get(helper.getLevel().getServer());
        wallets.deposit(player.getUUID(), 10_000_000L);
        useTestDate(helper);
        helper.assertTrue(EconomyService.buy(player, ModItems.PREBUILT.get(Prebuilt.OFFICE_2004).get(), 1) == EconomyService.BuyResult.OK,
                "buying a prebuilt should work");
        helper.assertTrue(player.getInventory().countItem(ModItems.SERVER.get()) == 1, "the prebuilt arrives as an assembled server");
        helper.succeed();
    }

    /** A comparator reads 15 from a rack at the critical temperature in temperature mode, 0 from an empty rack. */
    private static void comparatorOutput(GameTestHelper helper) {
        BlockPos hotPos = new BlockPos(0, 1, 0), emptyPos = new BlockPos(2, 1, 0);
        helper.setBlock(hotPos, ModBlocks.RACKS.get(RackType.HOSTING).get());
        helper.setBlock(emptyPos, ModBlocks.RACKS.get(RackType.HOSTING).get());
        RackBlockEntity hot = helper.getBlockEntity(hotPos, RackBlockEntity.class);
        RackBlockEntity empty = helper.getBlockEntity(emptyPos, RackBlockEntity.class);
        ItemStack heavy = server("server_motherboard", "psu_3200w", items("epyc_9654", "epyc_9654"), items(), items(),
                items("ddr5_128gb_ecc", "ddr5_128gb_ecc"));
        for (int i = 0; i < RackBlockEntity.SERVERS; i++) hot.setItem(i, heavy.copy());
        fillEnergy(hot);
        hot.setOwner(TEST_OWNER, "test");
        empty.setOwner(TEST_OWNER, "test");
        hot.cycleComparatorMode(); // STATUS -> TEMPERATURE
        hot.setTemperature(Config.CRITICAL_TEMPERATURE.get() + 20);
        helper.succeedWhen(() -> {
            helper.assertTrue(hot.comparatorMode() == RackComparatorMode.TEMPERATURE, "mode should be TEMPERATURE");
            helper.assertTrue(hot.comparatorSignal() == 15, "critical temperature should give 15, got " + hot.comparatorSignal());
            helper.assertTrue(empty.comparatorSignal() == 0, "an empty rack gives 0, got " + empty.comparatorSignal());
        });
    }

    /** Hardware can't be bought before its release date and gets cheaper with age. */
    private static void releaseLock(GameTestHelper helper) {
        useTestDate(helper); // 2021-06-01
        Item rtx4090 = ModItems.hardware("geforce_rtx_4090"), gtx460 = ModItems.hardware("geforce_gtx_460");
        double today = CalendarData.now(helper.getLevel().getServer());
        helper.assertTrue(!ShopPricing.released(rtx4090, today), "the RTX 4090 came out in 2022");
        helper.assertTrue(ShopPricing.released(gtx460, today), "the GTX 460 came out in 2010");
        double launch = ShopPricing.releaseDate(gtx460).toEpochDay();
        helper.assertTrue(ShopPricing.priceCents(gtx460, today) < ShopPricing.priceCents(gtx460, launch), "old hardware is cheaper");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        WalletData.get(helper.getLevel().getServer()).deposit(player.getUUID(), 100_000_000L);
        helper.assertTrue(EconomyService.buy(player, rtx4090, 1) == EconomyService.BuyResult.NOT_RELEASED, "unreleased parts can't be bought");
        helper.succeed();
    }

    /** Crossing the warning threshold and breaking produce a wear event. */
    private static void wearWarning(GameTestHelper helper) {
        ItemStack gpu = new ItemStack(ModItems.hardware("geforce_gtx_460"));
        Wear.set(gpu, 0.85F);
        helper.assertTrue(Wear.wear(gpu, 0.01, 0.10) == null, "no warning above the threshold");
        Wear.WearEvent low = Wear.wear(gpu, 0.06, 0.10);
        helper.assertTrue(low != null && !low.broke(), "crossing 10% should warn");
        helper.assertTrue(Wear.wear(gpu, 0.01, 0.10) == null, "the warning comes only once");
        Wear.WearEvent broke = Wear.wear(gpu, 0.5, 0.10);
        helper.assertTrue(broke != null && broke.broke(), "breaking should warn");
        helper.succeed();
    }

    /** A monitor next to a rack copies its state. */
    private static void serverMonitor(GameTestHelper helper) {
        BlockPos rackPos = new BlockPos(0, 1, 0), monitorPos = new BlockPos(1, 1, 0);
        helper.setBlock(rackPos, ModBlocks.RACKS.get(RackType.HOSTING).get());
        helper.setBlock(monitorPos, ModBlocks.SERVER_MONITOR.get());
        RackBlockEntity rack = helper.getBlockEntity(rackPos, RackBlockEntity.class);
        rack.setItem(0, server("office_motherboard", "psu_300w", items("pentium_4"), items(), items(), items("ddr2_1gb")));
        fillEnergy(rack);
        rack.setOwner(TEST_OWNER, "test");
        rack.setCustomers(HostingPlan.SHARED, 2);
        ServerMonitorBlockEntity monitor = helper.getBlockEntity(monitorPos, ServerMonitorBlockEntity.class);
        helper.succeedWhen(() -> {
            ServerMonitorBlockEntity.Snapshot s = monitor.snapshot();
            helper.assertTrue(s.connected(), "monitor should find the rack");
            helper.assertTrue(s.type() == RackType.HOSTING.ordinal(), "monitor should show a hosting rack");
            helper.assertTrue(s.customers()[HostingPlan.SHARED.ordinal()] == 2, "monitor should show 2 shared customers");
        });
    }

    /** Wear slows a server down and finally breaks it; the Service tab repairs and buys parts. */
    private static void wearAndService(GameTestHelper helper) {
        useTestDate(helper);
        ItemStack fresh = server("x570_motherboard", "psu_550w", items("ryzen_5_3600"), items(), items(), items("ddr4_16gb", "ddr4_16gb"));
        ItemStack worn = fresh.copy();
        Wear.wearServer(worn, 0.5);
        ServerSpec freshSpec = ServerSpec.of(fresh), wornSpec = ServerSpec.of(worn);
        helper.assertTrue(wornSpec.boots(), "a half worn server still boots");
        helper.assertTrue(wornSpec.computeBps() < freshSpec.computeBps(), "worn CPUs are slower");
        helper.assertTrue(Math.abs(wornSpec.worstCondition() - 0.5) < 1e-6, "condition should be 50%, was " + wornSpec.worstCondition());
        Wear.wearServer(worn, 0.6);
        helper.assertTrue(ServerSpec.of(worn).problem() == ServerProblem.BROKEN_PART, "0% condition breaks the server");

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        WalletData wallets = WalletData.get(helper.getLevel().getServer());
        UUID id = player.getUUID();
        wallets.withdraw(id, wallets.wallet(id).cents());
        ItemStack cpu = new ItemStack(ModItems.hardware("ryzen_5_3600"));
        Wear.set(cpu, 0.5F);
        double today = CalendarData.now(helper.getLevel().getServer());
        long repairCost = ItemValue.repairCents(cpu, today);
        helper.assertTrue(repairCost > 0, "a worn CPU has a repair cost");
        player.getInventory().setItem(0, cpu);
        wallets.deposit(id, repairCost);
        helper.assertTrue(EconomyService.service(player, 0, cpu.getItem(), true) == EconomyService.ServiceResult.OK, "repair should work");
        helper.assertTrue(Wear.get(player.getInventory().getItem(0)) == 0, "repaired CPU should be like new");
        helper.assertTrue(wallets.wallet(id).cents() == 0, "repair should cost " + repairCost);

        long resale = ItemValue.resaleCents(player.getInventory().getItem(0), today);
        helper.assertTrue(EconomyService.service(player, 0, cpu.getItem(), false) == EconomyService.ServiceResult.OK, "selling should work");
        helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "sold CPU leaves the inventory");
        helper.assertTrue(wallets.wallet(id).cents() == resale && resale > 0, "resale should pay " + resale);
        helper.succeed();
    }

    /** Customers of an overloaded rack cancel; an idle rack gets its first customer. */
    private static void hostingCustomers(GameTestHelper helper) {
        BlockPos overloadedPos = new BlockPos(0, 1, 0), freshPos = new BlockPos(2, 1, 0);
        helper.setBlock(overloadedPos, ModBlocks.RACKS.get(RackType.HOSTING).get());
        helper.setBlock(freshPos, ModBlocks.RACKS.get(RackType.HOSTING).get());
        RackBlockEntity overloaded = helper.getBlockEntity(overloadedPos, RackBlockEntity.class);
        RackBlockEntity fresh = helper.getBlockEntity(freshPos, RackBlockEntity.class);
        ItemStack small = server("office_motherboard", "psu_300w", items("pentium_4"), items(), items(), items("ddr2_1gb"));
        overloaded.setItem(0, small.copy());
        fresh.setItem(0, small.copy());
        for (RackBlockEntity rack : List.of(overloaded, fresh)) {
            fillEnergy(rack);
            rack.setOwner(TEST_OWNER, "test");
        }
        // a Pentium 4 (about 4 KB/s) cannot serve 20 dedicated customers (128 KB/s each)
        overloaded.setCustomers(HostingPlan.DEDICATED, 20);

        helper.runAfterDelay(2, () -> helper.assertTrue(overloaded.status() == RackStatus.OVERLOADED,
                "rack should be OVERLOADED, was " + overloaded.status()));
        helper.succeedWhen(() -> {
            helper.assertTrue(overloaded.customers().count(HostingPlan.DEDICATED.ordinal()) < 20, "overloaded customers should cancel");
            helper.assertTrue(fresh.customers().total() >= 1, "an idle rack should get its first customer");
            helper.assertTrue(fresh.customers().count(HostingPlan.DEDICATED.ordinal()) == 0, "a dedicated plan does not fit a Pentium 4");
        });
    }

    /** Puts the calendar at 2021-06-01 (all coins launched, before The Merge). Idempotent, so parallel tests agree. */
    // ================================================================== power generation

    /** A panel under open sky produces exactly when the sun is up; a covered panel reports NO_SKY. */
    private static void solarDaytime(GameTestHelper helper) {
        BlockPos open = new BlockPos(0, 1, 0), covered = new BlockPos(2, 1, 0);
        helper.setBlock(open, ModBlocks.GENERATORS.get(GeneratorType.MONOCRYSTALLINE_SOLAR).get());
        helper.setBlock(covered, ModBlocks.GENERATORS.get(GeneratorType.MONOCRYSTALLINE_SOLAR).get());
        helper.setBlock(covered.above(), Blocks.STONE);
        GeneratorBlockEntity sun = helper.getBlockEntity(open, GeneratorBlockEntity.class);
        GeneratorBlockEntity shade = helper.getBlockEntity(covered, GeneratorBlockEntity.class);
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getTick() > 2, "waiting for the first tick");
            helper.assertTrue(shade.status() == GeneratorStatus.NO_SKY, "covered panel should report NO_SKY, was " + shade.status());
            helper.assertTrue(shade.output() == 0, "covered panel must not produce");
            ServerLevel level = helper.getLevel();
            boolean skyVisible = level.canSeeSky(helper.absolutePos(open).above());
            double expected = SolarModel.output(level.getDefaultClockTime(), level.getRainLevel(1.0F), level.getThunderLevel(1.0F),
                    skyVisible, level.dimensionType().hasSkyLight() && !level.dimensionType().hasFixedTime()).factor();
            helper.assertTrue((sun.output() > 0) == (expected > 0), "open panel output " + sun.output() + " vs model " + expected);
        });
    }

    /** Combustion generator -> 3 copper power cables -> hosting rack: the rack's buffer fills over the network. */
    private static void cableFeedsRack(GameTestHelper helper) {
        BlockPos gen = new BlockPos(0, 1, 0), rack = new BlockPos(4, 1, 0);
        helper.setBlock(gen, ModBlocks.GENERATORS.get(GeneratorType.COMBUSTION_GENERATOR).get());
        for (int x = 1; x <= 3; x++) helper.setBlock(new BlockPos(x, 1, 0), ModBlocks.POWER_CABLES.get(PowerCableTier.COPPER).get());
        helper.setBlock(rack, ModBlocks.RACKS.get(RackType.HOSTING).get());
        GeneratorBlockEntity generator = helper.getBlockEntity(gen, GeneratorBlockEntity.class);
        generator.setItem(0, new ItemStack(Items.COAL, 4));
        RackBlockEntity target = helper.getBlockEntity(rack, RackBlockEntity.class);
        helper.succeedWhen(() -> {
            helper.assertTrue(target.getEnergyHandler().getAmountAsLong() > 0, "rack should receive FE over the cables");
            PowerNetworkManager.PowerInfo info = PowerNetworkManager.get(helper.getLevel()).describe(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 0)));
            helper.assertTrue(info != null && info.generators() == 1 && info.consumers() == 1,
                    "network should have 1 generator and 1 consumer: " + info);
            helper.assertTrue(info.bottleneck() == PowerCableTier.COPPER, "bottleneck should be copper");
        });
    }

    /** A charged battery pushes FE out of its front into a rack; a generator next to another battery charges it. */
    private static void batteryStoresAndReleases(GameTestHelper helper) {
        BlockPos battery = new BlockPos(0, 1, 0), rack = new BlockPos(1, 1, 0);
        helper.setBlock(battery, ModBlocks.BATTERIES.get(BatteryTier.LEAD_ACID).get().defaultBlockState().setValue(PowerBlock.FACING, Direction.EAST));
        helper.setBlock(rack, ModBlocks.RACKS.get(RackType.HOSTING).get());
        BatteryBlockEntity full = helper.getBlockEntity(battery, BatteryBlockEntity.class);
        ((SimpleEnergyHandler) full.energy()).set(100_000);
        RackBlockEntity target = helper.getBlockEntity(rack, RackBlockEntity.class);

        BlockPos gen = new BlockPos(0, 1, 2), empty = new BlockPos(1, 1, 2);
        helper.setBlock(gen, ModBlocks.GENERATORS.get(GeneratorType.COMBUSTION_GENERATOR).get());
        helper.setBlock(empty, ModBlocks.BATTERIES.get(BatteryTier.LEAD_ACID).get().defaultBlockState().setValue(PowerBlock.FACING, Direction.EAST));
        helper.getBlockEntity(gen, GeneratorBlockEntity.class).setItem(0, new ItemStack(Items.COAL, 4));
        BatteryBlockEntity charging = helper.getBlockEntity(empty, BatteryBlockEntity.class);
        helper.succeedWhen(() -> {
            helper.assertTrue(target.getEnergyHandler().getAmountAsLong() > 0, "battery should discharge into the rack");
            helper.assertTrue(full.energy().getAmountAsLong() < 100_000, "battery should lose charge");
            helper.assertTrue(charging.energy().getAmountAsLong() > 0, "generator should charge the second battery");
        });
    }

    /** One coal = 1600 burn ticks; the generator runs at 40 FE/t while burning. */
    private static void combustionBurnsCoal(GameTestHelper helper) {
        BlockPos pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, ModBlocks.GENERATORS.get(GeneratorType.COMBUSTION_GENERATOR).get());
        GeneratorBlockEntity gen = helper.getBlockEntity(pos, GeneratorBlockEntity.class);
        gen.setItem(0, new ItemStack(Items.COAL));
        helper.succeedWhen(() -> {
            helper.assertTrue(gen.getItem(0).isEmpty(), "coal should be burnt");
            helper.assertTrue(gen.status() == GeneratorStatus.RUNNING, "should be running, was " + gen.status());
            helper.assertTrue(Math.abs(gen.output() - gen.peak()) < 1.0E-6, "should produce peak output, was " + gen.output());
            helper.assertTrue(gen.energy().getAmountAsLong() > 0, "buffer should fill");
        });
    }

    /** Flowing water at the side of a water wheel drives it. */
    private static void waterWheelFlowing(GameTestHelper helper) {
        BlockPos wheel = new BlockPos(1, 1, 1);
        helper.setBlock(wheel, ModBlocks.GENERATORS.get(GeneratorType.WATER_WHEEL).get());
        helper.setBlock(new BlockPos(0, 0, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(0, 2, 1), Blocks.WATER);
        // falling (non-source) water beside the wheel, fed by the source above
        helper.setBlock(new BlockPos(0, 1, 1), Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, 8));
        GeneratorBlockEntity gen = helper.getBlockEntity(wheel, GeneratorBlockEntity.class);
        helper.succeedWhen(() -> {
            helper.assertTrue(gen.output() > 0, "flowing water should drive the wheel, status " + gen.status());
            helper.assertTrue(gen.status() == GeneratorStatus.RUNNING, "should be running, was " + gen.status());
        });
    }

    /** Two adjacent lava sources give 2/6 of the geothermal plant's peak output. */
    private static void geothermalLava(GameTestHelper helper) {
        BlockPos plant = new BlockPos(1, 1, 1);
        helper.setBlock(plant, ModBlocks.GENERATORS.get(GeneratorType.GEOTHERMAL_PLANT).get());
        helper.setBlock(new BlockPos(0, 1, 1), Blocks.LAVA);
        helper.setBlock(new BlockPos(2, 1, 1), Blocks.LAVA);
        GeneratorBlockEntity gen = helper.getBlockEntity(plant, GeneratorBlockEntity.class);
        helper.succeedWhen(() -> {
            double expected = gen.peak() * 2.0 / 6.0;
            helper.assertTrue(Math.abs(gen.output() - expected) < 1.0E-6, "expected " + expected + " FE/t, was " + gen.output() + " (" + gen.status() + ")");
        });
    }

    /** A reactor at the heat limit without cooling SCRAMs and stops producing; it can't restart while hot. */
    private static void reactorScram(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.GENERATORS.get(GeneratorType.SMALL_MODULAR_REACTOR).get());
        GeneratorBlockEntity reactor = helper.getBlockEntity(pos, GeneratorBlockEntity.class);
        for (int i = 0; i < GeneratorType.REACTOR_RODS; i++) reactor.setItem(i, new ItemStack(ModItems.FUEL_ROD.get()));
        reactor.setHeat(0.979);
        helper.succeedWhen(() -> {
            helper.assertTrue(reactor.scrammed(), "reactor should SCRAM, heat " + reactor.heat());
            helper.assertTrue(reactor.status() == GeneratorStatus.SCRAM, "status should be SCRAM, was " + reactor.status());
            helper.assertTrue(reactor.output() == 0, "a scrammed reactor must not produce");
            reactor.handleButton(0);
            helper.assertTrue(reactor.scrammed(), "restart must be refused while the core is hot");
        });
    }

    /** A generator feeding a grid connection pays its owner (export price raised so the test needn't wait an hour). */
    private static void gridExportPaysOwner(GameTestHelper helper) {
        useTestDate(helper);
        Config.GRID_EXPORT_PRICE.set(1_000_000.0); // in-memory only, like rack_energy
        UUID owner = UUID.randomUUID();
        WalletData wallets = WalletData.get(helper.getLevel().getServer());
        long start = wallets.wallet(owner).cents();
        BlockPos gen = new BlockPos(0, 1, 0), grid = new BlockPos(1, 1, 0);
        helper.setBlock(gen, ModBlocks.GENERATORS.get(GeneratorType.COMBUSTION_GENERATOR).get());
        helper.setBlock(grid, ModBlocks.GRID_CONNECTIONS.get(GridTier.RESIDENTIAL).get());
        helper.getBlockEntity(gen, GeneratorBlockEntity.class).setItem(0, new ItemStack(Items.COAL, 4));
        GridConnectionBlockEntity connection = helper.getBlockEntity(grid, GridConnectionBlockEntity.class);
        connection.setOwner(owner, "test");
        helper.succeedWhen(() -> {
            helper.assertTrue(connection.exportRate() > 0, "grid connection should receive FE");
            helper.assertTrue(wallets.wallet(owner).cents() > start, "owner should be paid for the export");
            helper.assertTrue(connection.earned() > 0, "earned total should count the payout");
        });
    }

    private static void useTestDate(GameTestHelper helper) {
        CalendarData calendar = CalendarData.get(helper.getLevel().getServer());
        if (calendar.today().isBefore(TEST_DATE) || calendar.today().isAfter(TEST_DATE.plusDays(30))) calendar.set(TEST_DATE);
        MarketData.get(helper.getLevel().getServer()).refresh(calendar.epochDay());
    }

    private static void fillEnergy(RackBlockEntity rack) {
        SimpleEnergyHandler handler = (SimpleEnergyHandler) rack.getEnergyHandler();
        handler.set((int) handler.getCapacityAsLong());
    }

    private static RackPart part(GameTestHelper helper, int y) {
        return helper.getBlockState(new BlockPos(0, y, 0)).getValue(RackBlock.PART);
    }

    private static Item[] items(String... ids) {
        Item[] result = new Item[ids.length];
        for (int i = 0; i < ids.length; i++) result[i] = ModItems.hardware(ids[i]);
        return result;
    }

    private static ItemStack server(String board, String psu, Item[] cpus, Item[] gpus, Item[] drives, Item[] memory) {
        NonNullList<ItemStack> parts = NonNullList.withSize(ServerLayout.SIZE, ItemStack.EMPTY);
        parts.set(ServerLayout.SLOT_MOTHERBOARD, new ItemStack(ModItems.hardware(board)));
        parts.set(ServerLayout.SLOT_PSU, new ItemStack(ModItems.hardware(psu)));
        for (int i = 0; i < memory.length; i++) parts.set(ServerLayout.SLOT_MEMORY_START + i, new ItemStack(memory[i]));
        for (int i = 0; i < cpus.length; i++) parts.set(ServerLayout.SLOT_CPU_START + i, new ItemStack(cpus[i]));
        for (int i = 0; i < gpus.length; i++) parts.set(ServerLayout.SLOT_GPU_START + i, new ItemStack(gpus[i]));
        for (int i = 0; i < drives.length; i++) parts.set(ServerLayout.SLOT_STORAGE_START + i, new ItemStack(drives[i]));
        ItemStack server = new ItemStack(ModItems.SERVER.get());
        ServerLayout.writeParts(server, parts);
        return server;
    }
}
