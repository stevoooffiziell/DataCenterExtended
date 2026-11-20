package de.f4ked.datacenterextended.registry;

import de.f4ked.datacenterextended.Datacenterextended;
import de.f4ked.datacenterextended.block.RackType;
import de.f4ked.datacenterextended.network.CableTier;
import de.f4ked.datacenterextended.network.NetworkTesterItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All items. Hardware items are plain items – their stats come from the {@code hardware} data map, so they can be
 * rebalanced by datapacks and other mods' items can be turned into hardware as well.
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Datacenterextended.MODID);

    private static final List<DeferredItem<? extends Item>> TAB_ORDER = new ArrayList<>();
    public static final List<DeferredItem<? extends Item>> ALL = Collections.unmodifiableList(TAB_ORDER);

    // ------------------------------------------------------------------ blocks
    public static final DeferredItem<BlockItem> SERVER_WORKBENCH = add(ITEMS.registerSimpleBlockItem("server_workbench", ModBlocks.SERVER_WORKBENCH));
    public static final Map<RackType, DeferredItem<BlockItem>> RACKS = new EnumMap<>(RackType.class);
    public static final Map<CableTier, DeferredItem<BlockItem>> CABLES = new EnumMap<>(CableTier.class);

    static {
        for (RackType type : RackType.values()) {
            RACKS.put(type, add(ITEMS.registerSimpleBlockItem(type.id(), ModBlocks.RACKS.get(type))));
        }
        for (CableTier tier : CableTier.values()) {
            CABLES.put(tier, add(ITEMS.registerSimpleBlockItem(tier.id(), ModBlocks.CABLES.get(tier))));
        }
    }

    public static final DeferredItem<BlockItem> HARDWARE_SHOP = add(ITEMS.registerSimpleBlockItem("hardware_shop", ModBlocks.HARDWARE_SHOP));

    // ------------------------------------------------------------------ server + tools
    /** A 1U server; its parts live in the server_parts data component (assembled at the Server Workbench). */
    public static final DeferredItem<Item> SERVER = add(ITEMS.registerSimpleItem("server", p -> p.stacksTo(1)));
    public static final DeferredItem<NetworkTesterItem> NETWORK_TESTER = add(ITEMS.registerItem("network_tester", NetworkTesterItem::new, p -> p.stacksTo(1)));

    // ------------------------------------------------------------------ crafting materials
    public static final DeferredItem<Item> SILICON = simple("silicon", Rarity.COMMON, 64);
    public static final DeferredItem<Item> CIRCUIT_BOARD = simple("circuit_board", Rarity.COMMON, 64);
    /** Intermediate for hardware recipes (replaces the old credit cost). */
    public static final DeferredItem<Item> PROCESSOR_DIE = simple("processor_die", Rarity.UNCOMMON, 64);

    // ------------------------------------------------------------------ hardware (stats: data_maps/item/hardware.json)
    /** All hardware items by id, in creative tab order. */
    public static final Map<String, DeferredItem<Item>> HARDWARE = new LinkedHashMap<>();

    static {
        // motherboards (socket + memory type are in the hardware data map)
        hardware("office_motherboard", Rarity.COMMON);
        hardware("am3_motherboard", Rarity.COMMON);
        hardware("z77_motherboard", Rarity.COMMON);
        hardware("workstation_motherboard", Rarity.UNCOMMON);
        hardware("gaming_motherboard", Rarity.UNCOMMON);
        hardware("mining_motherboard", Rarity.UNCOMMON);
        hardware("x570_motherboard", Rarity.UNCOMMON);
        hardware("epyc_sp3_motherboard", Rarity.RARE);
        hardware("z790_motherboard", Rarity.RARE);
        hardware("x670e_motherboard", Rarity.RARE);
        hardware("server_motherboard", Rarity.EPIC);
        hardware("xeon_quad_motherboard", Rarity.EPIC);
        // CPUs
        hardware("pentium_4", Rarity.COMMON);
        hardware("core_2_quad_q6600", Rarity.COMMON);
        hardware("amd_fx_4300", Rarity.COMMON);
        hardware("amd_fx_8350", Rarity.COMMON);
        hardware("core_i5_2500k", Rarity.COMMON);
        hardware("core_i7_3770k", Rarity.COMMON);
        hardware("xeon_e5_2690", Rarity.UNCOMMON);
        hardware("core_i7_8700k", Rarity.UNCOMMON);
        hardware("core_i9_9900k", Rarity.UNCOMMON);
        hardware("ryzen_5_3600", Rarity.UNCOMMON);
        hardware("ryzen_7_5800x3d", Rarity.UNCOMMON);
        hardware("epyc_7742", Rarity.RARE);
        hardware("core_i5_13600k", Rarity.RARE);
        hardware("core_i9_13900k", Rarity.RARE);
        hardware("ryzen_7_9800x3d", Rarity.RARE);
        hardware("ryzen_9_7950x", Rarity.RARE);
        hardware("epyc_9654", Rarity.EPIC);
        hardware("xeon_platinum_8480", Rarity.EPIC);
        // GPUs
        hardware("geforce_gtx_460", Rarity.COMMON);
        hardware("radeon_hd_7970", Rarity.COMMON);
        hardware("geforce_gtx_970", Rarity.COMMON);
        hardware("radeon_rx_580", Rarity.UNCOMMON);
        hardware("geforce_gtx_1080_ti", Rarity.UNCOMMON);
        hardware("geforce_rtx_2080_ti", Rarity.UNCOMMON);
        hardware("radeon_rx_6800_xt", Rarity.RARE);
        hardware("geforce_rtx_3090", Rarity.RARE);
        hardware("radeon_rx_7900_xtx", Rarity.RARE);
        hardware("geforce_rtx_4090", Rarity.EPIC);
        hardware("geforce_rtx_5090", Rarity.EPIC);
        hardware("nvidia_h100", Rarity.EPIC);
        // memory
        hardware("ddr2_1gb", Rarity.COMMON);
        hardware("ddr2_2gb", Rarity.COMMON);
        hardware("ddr3_4gb", Rarity.COMMON);
        hardware("ddr3_8gb", Rarity.COMMON);
        hardware("ddr3_32gb_ecc", Rarity.UNCOMMON);
        hardware("ddr4_8gb", Rarity.UNCOMMON);
        hardware("ddr4_16gb", Rarity.UNCOMMON);
        hardware("ddr4_64gb_ecc", Rarity.RARE);
        hardware("ddr5_16gb", Rarity.RARE);
        hardware("ddr5_32gb", Rarity.RARE);
        hardware("ddr5_128gb_ecc", Rarity.EPIC);
        // PSUs
        hardware("psu_300w", Rarity.COMMON);
        hardware("psu_550w", Rarity.COMMON);
        hardware("psu_850w", Rarity.UNCOMMON);
        hardware("psu_1200w", Rarity.UNCOMMON);
        hardware("psu_2000w", Rarity.RARE);
        hardware("psu_3200w", Rarity.EPIC);
        // storage
        hardware("hdd_500gb", Rarity.COMMON);
        hardware("ssd_1tb", Rarity.UNCOMMON);
        hardware("nvme_2tb", Rarity.RARE);
        hardware("enterprise_ssd_8tb", Rarity.EPIC);
    }

    private ModItems() {
    }

    private static DeferredItem<Item> simple(String name, Rarity rarity, int stackSize) {
        return add(ITEMS.registerSimpleItem(name, p -> p.rarity(rarity).stacksTo(stackSize)));
    }

    private static void hardware(String name, Rarity rarity) {
        HARDWARE.put(name, simple(name, rarity, 16));
    }

    /** A hardware item by id, e.g. {@code hardware("epyc_9654")}. */
    public static Item hardware(String name) {
        DeferredItem<Item> item = HARDWARE.get(name);
        if (item == null) throw new IllegalArgumentException("Unknown hardware item " + name);
        return item.get();
    }

    private static <T extends Item> DeferredItem<T> add(DeferredItem<T> item) {
        TAB_ORDER.add(item);
        return item;
    }
}
