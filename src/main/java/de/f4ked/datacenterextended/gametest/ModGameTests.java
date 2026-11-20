package de.f4ked.datacenterextended.gametest;

import de.f4ked.datacenterextended.Datacenterextended;
import de.f4ked.datacenterextended.block.RackBlockEntity;
import de.f4ked.datacenterextended.block.RackPart;
import de.f4ked.datacenterextended.block.RackStatus;
import de.f4ked.datacenterextended.block.RackType;
import de.f4ked.datacenterextended.block.RackBlock;
import de.f4ked.datacenterextended.hardware.ServerLayout;
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
            source.setItem(i, server(ModItems.SERVER_MOTHERBOARD.get(), ModItems.PSU_3200W.get(),
                    new Item[]{ModItems.EPYC_9654.get(), ModItems.EPYC_9654.get(), ModItems.EPYC_9654.get(), ModItems.EPYC_9654.get()},
                    new Item[0], new Item[0]));
        }

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

        top.setItem(0, server(ModItems.OFFICE_MOTHERBOARD.get(), ModItems.PSU_300W.get(),
                new Item[0], new Item[]{ModItems.GEFORCE_GTX_460.get()}, new Item[0]));

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
        rack.setItem(0, server(ModItems.OFFICE_MOTHERBOARD.get(), ModItems.PSU_300W.get(),
                new Item[]{ModItems.PENTIUM_4.get()}, new Item[0], new Item[]{ModItems.ENTERPRISE_SSD_8TB.get()}));

        helper.succeedWhen(() -> {
            helper.assertTrue(rack.status() == RackStatus.RUNNING, "hosting rack should run, was " + rack.status());
            helper.assertTrue(Math.abs(rack.storageBonus() - 0.25) < 1e-9, "8 TB should hit the 25% cap, was " + rack.storageBonus());
        });
    }

    private static RackPart part(GameTestHelper helper, int y) {
        return helper.getBlockState(new BlockPos(0, y, 0)).getValue(RackBlock.PART);
    }

    private static ItemStack server(Item board, Item psu, Item[] cpus, Item[] gpus, Item[] drives) {
        NonNullList<ItemStack> parts = NonNullList.withSize(ServerLayout.SIZE, ItemStack.EMPTY);
        parts.set(ServerLayout.SLOT_MOTHERBOARD, new ItemStack(board));
        parts.set(ServerLayout.SLOT_PSU, new ItemStack(psu));
        for (int i = 0; i < cpus.length; i++) parts.set(ServerLayout.SLOT_CPU_START + i, new ItemStack(cpus[i]));
        for (int i = 0; i < gpus.length; i++) parts.set(ServerLayout.SLOT_GPU_START + i, new ItemStack(gpus[i]));
        for (int i = 0; i < drives.length; i++) parts.set(ServerLayout.SLOT_STORAGE_START + i, new ItemStack(drives[i]));
        ItemStack server = new ItemStack(ModItems.SERVER.get());
        ServerLayout.writeParts(server, parts);
        return server;
    }
}
