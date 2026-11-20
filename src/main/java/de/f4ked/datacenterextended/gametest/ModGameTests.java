package de.f4ked.datacenterextended.gametest;

import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.Datacenterextended;
import de.f4ked.datacenterextended.block.RackBlockEntity;
import de.f4ked.datacenterextended.block.RackPart;
import de.f4ked.datacenterextended.block.RackStatus;
import de.f4ked.datacenterextended.block.RackType;
import de.f4ked.datacenterextended.block.RackBlock;
import de.f4ked.datacenterextended.hardware.ServerLayout;
import de.f4ked.datacenterextended.hardware.ServerProblem;
import de.f4ked.datacenterextended.hardware.ServerSpec;
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
    }

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

        helper.succeedWhen(() -> {
            helper.assertTrue(bottom.status() == RackStatus.RUNNING, "controller should run, was " + bottom.status());
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

        helper.succeedWhen(() -> {
            helper.assertTrue(rack.status() == RackStatus.RUNNING, "hosting rack should run, was " + rack.status());
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

        helper.runAfterDelay(5, () -> {
            helper.assertTrue(rack.status() == RackStatus.NO_ENERGY, "empty buffer should stop the rack, was " + rack.status());
            ((SimpleEnergyHandler) rack.getEnergyHandler()).set(10_000);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getTick() > 7, "waiting for the energy check");
            helper.assertTrue(rack.status() == RackStatus.RUNNING, "powered rack should run, was " + rack.status());
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
