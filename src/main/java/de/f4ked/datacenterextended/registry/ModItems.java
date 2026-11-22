package de.f4ked.datacenterextended.registry;

import de.f4ked.datacenterextended.Datacenterextended;
import de.f4ked.datacenterextended.block.RackType;
import de.f4ked.datacenterextended.network.CableTier;
import de.f4ked.datacenterextended.network.NetworkTesterItem;
import de.f4ked.datacenterextended.hardware.Prebuilt;
import de.f4ked.datacenterextended.item.HandbookItem;
import de.f4ked.datacenterextended.item.PrebuiltServerItem;
import de.f4ked.datacenterextended.power.BatteryTier;
import de.f4ked.datacenterextended.power.GeneratorType;
import de.f4ked.datacenterextended.power.GridTier;
import de.f4ked.datacenterextended.power.PowerCableTier;
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
    private static final List<DeferredItem<? extends Item>> MATERIAL_ORDER = new ArrayList<>();
    /** Raw materials, refined materials and components (creative tab "Materials"). */
    public static final List<DeferredItem<? extends Item>> MATERIALS = Collections.unmodifiableList(MATERIAL_ORDER);
    private static final List<DeferredItem<? extends Item>> ENERGY_ORDER = new ArrayList<>();
    /** Generators, batteries, power cables, grid connections (creative tab "Energy"). */
    public static final List<DeferredItem<? extends Item>> ENERGY = Collections.unmodifiableList(ENERGY_ORDER);

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
    public static final DeferredItem<BlockItem> SERVER_MONITOR = add(ITEMS.registerSimpleBlockItem("server_monitor", ModBlocks.SERVER_MONITOR));

    // ------------------------------------------------------------------ server + tools
    /** A 1U server; its parts live in the server_parts data component (assembled at the Server Workbench). */
    public static final DeferredItem<Item> SERVER = add(ITEMS.registerSimpleItem("server", p -> p.stacksTo(1)));
    public static final DeferredItem<NetworkTesterItem> NETWORK_TESTER = add(ITEMS.registerItem("network_tester", NetworkTesterItem::new, p -> p.stacksTo(1)));
    /** The guide book (opens a book screen with the progression guide). */
    public static final DeferredItem<HandbookItem> HANDBOOK = add(ITEMS.registerItem("handbook", HandbookItem::new, p -> p.stacksTo(1)));

    // ------------------------------------------------------------------ cooling (stats: data_maps/item/cooling.json)
    public static final DeferredItem<Item> RACK_FAN = simple("rack_fan", Rarity.COMMON, 16);
    public static final DeferredItem<Item> HIGH_FLOW_FAN = simple("high_flow_fan", Rarity.UNCOMMON, 16);
    public static final DeferredItem<Item> WATER_COOLING_KIT = simple("water_cooling_kit", Rarity.RARE, 16);

    /** Prebuilt servers (bought assembled; the box item unpacks into a server). */
    public static final Map<Prebuilt, DeferredItem<PrebuiltServerItem>> PREBUILT = new EnumMap<>(Prebuilt.class);

    static {
        for (Prebuilt prebuilt : Prebuilt.values()) {
            PREBUILT.put(prebuilt, add(ITEMS.registerItem(prebuilt.id(), p -> new PrebuiltServerItem(prebuilt, p), p -> p.stacksTo(1).rarity(Rarity.UNCOMMON))));
        }
    }

    // ------------------------------------------------------------------ materials & components (own creative tab)
    /** Material items by id (ore blocks excluded), in creative tab order. */
    public static final Map<String, DeferredItem<Item>> MATERIAL_ITEMS = new LinkedHashMap<>();

    static {
        ModBlocks.RESOURCE_BLOCKS.forEach((id, block) -> MATERIAL_ORDER.add(ITEMS.registerSimpleBlockItem(id, block)));
        for (String id : new String[]{"raw_bauxite", "raw_spodumene", "raw_uraninite", "aluminium_ingot", "aluminium_nugget",
                "steel_blend", "steel_ingot", "lithium_carbonate", "yellowcake"}) {
            material(id, Rarity.COMMON, 64);
        }
        material("enriched_uranium", Rarity.RARE, 64);
    }

    public static final DeferredItem<Item> SILICON = material("silicon", Rarity.COMMON, 64);

    static {
        for (String id : new String[]{"silicon_wafer", "n_type_wafer", "p_type_wafer", "solar_cell", "tempered_glass"}) {
            material(id, Rarity.COMMON, 64);
        }
    }

    public static final DeferredItem<Item> CIRCUIT_BOARD = material("circuit_board", Rarity.COMMON, 64);
    /** Intermediate for hardware recipes (replaces the old credit cost). */
    public static final DeferredItem<Item> PROCESSOR_DIE = material("processor_die", Rarity.UNCOMMON, 64);

    static {
        for (String id : new String[]{"copper_wire", "copper_coil", "permanent_magnet", "capacitor", "inverter", "charge_controller",
                "steel_plate", "steel_rod", "aluminium_sheet", "steel_gear", "bearing", "gearbox", "alternator",
                "fibreglass", "carbon_fibre", "fibreglass_blade", "carbon_fibre_blade", "water_wheel_paddle",
                "lead_acid_cell", "li_ion_cell", "lfp_cell", "liquid_nitrogen_cell", "control_rod", "reactor_casing"}) {
            material(id, Rarity.COMMON, 64);
        }
    }

    /** Rotors are crafting components and are also drawn spinning on the wind turbines. */
    public static final DeferredItem<Item> FIBREGLASS_ROTOR = material("fibreglass_rotor", Rarity.COMMON, 16);
    public static final DeferredItem<Item> CARBON_FIBRE_ROTOR = material("carbon_fibre_rotor", Rarity.UNCOMMON, 16);
    public static final DeferredItem<Item> FUEL_ROD = material("fuel_rod", Rarity.RARE, 16);
    public static final DeferredItem<Item> SPENT_FUEL_ROD = material("spent_fuel_rod", Rarity.UNCOMMON, 16);

    // ------------------------------------------------------------------ energy (own creative tab)
    public static final Map<GeneratorType, DeferredItem<BlockItem>> GENERATORS = new EnumMap<>(GeneratorType.class);
    public static final Map<BatteryTier, DeferredItem<BlockItem>> BATTERIES = new EnumMap<>(BatteryTier.class);
    public static final Map<PowerCableTier, DeferredItem<BlockItem>> POWER_CABLES = new EnumMap<>(PowerCableTier.class);
    public static final Map<GridTier, DeferredItem<BlockItem>> GRID_CONNECTIONS = new EnumMap<>(GridTier.class);

    static {
        for (GeneratorType type : GeneratorType.values()) {
            GENERATORS.put(type, energy(ITEMS.registerSimpleBlockItem(type.id(), ModBlocks.GENERATORS.get(type))));
        }
        for (BatteryTier tier : BatteryTier.values()) {
            BATTERIES.put(tier, energy(ITEMS.registerSimpleBlockItem(tier.id(), ModBlocks.BATTERIES.get(tier), p -> p.stacksTo(1))));
        }
        for (PowerCableTier tier : PowerCableTier.values()) {
            POWER_CABLES.put(tier, energy(ITEMS.registerSimpleBlockItem(tier.id(), ModBlocks.POWER_CABLES.get(tier))));
        }
        for (GridTier tier : GridTier.values()) {
            GRID_CONNECTIONS.put(tier, energy(ITEMS.registerSimpleBlockItem(tier.id(), ModBlocks.GRID_CONNECTIONS.get(tier))));
        }
    }

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
        hardware("am2_motherboard", Rarity.COMMON);
        hardware("x58_motherboard", Rarity.COMMON);
        hardware("z87_motherboard", Rarity.UNCOMMON);
        hardware("z170_motherboard", Rarity.UNCOMMON);
        hardware("x370_motherboard", Rarity.UNCOMMON);
        hardware("trx40_motherboard", Rarity.RARE);
        hardware("z690_motherboard", Rarity.RARE);
        hardware("wrx90_motherboard", Rarity.EPIC);
        hardware("z890_motherboard", Rarity.EPIC);
        hardware("x870e_motherboard", Rarity.EPIC);
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
        hardware("athlon_64_x2_4800", Rarity.COMMON);
        hardware("core_2_duo_e8400", Rarity.COMMON);
        hardware("core_i7_920", Rarity.COMMON);
        hardware("phenom_ii_x6_1100t", Rarity.COMMON);
        hardware("core_i7_4770k", Rarity.UNCOMMON);
        hardware("core_i7_6700k", Rarity.UNCOMMON);
        hardware("ryzen_7_1800x", Rarity.UNCOMMON);
        hardware("ryzen_9_3950x", Rarity.RARE);
        hardware("threadripper_3990x", Rarity.RARE);
        hardware("ryzen_9_5950x", Rarity.RARE);
        hardware("core_i9_12900k", Rarity.RARE);
        hardware("core_i9_14900k", Rarity.RARE);
        hardware("ryzen_9_9950x", Rarity.EPIC);
        hardware("core_ultra_9_285k", Rarity.EPIC);
        hardware("threadripper_pro_7995wx", Rarity.EPIC);
        hardware("epyc_9965", Rarity.EPIC);
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
        hardware("geforce_8800_gtx", Rarity.COMMON);
        hardware("radeon_hd_5870", Rarity.COMMON);
        hardware("geforce_gtx_580", Rarity.COMMON);
        hardware("geforce_gtx_680", Rarity.COMMON);
        hardware("radeon_r9_290x", Rarity.UNCOMMON);
        hardware("geforce_gtx_980_ti", Rarity.UNCOMMON);
        hardware("radeon_rx_vega_64", Rarity.UNCOMMON);
        hardware("geforce_rtx_3060_ti", Rarity.UNCOMMON);
        hardware("geforce_rtx_3080", Rarity.RARE);
        hardware("radeon_rx_6900_xt", Rarity.RARE);
        hardware("geforce_rtx_4070", Rarity.RARE);
        hardware("radeon_rx_9070_xt", Rarity.RARE);
        hardware("nvidia_a100", Rarity.EPIC);
        hardware("nvidia_h200", Rarity.EPIC);
        hardware("nvidia_b200", Rarity.EPIC);
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
        hardware("ddr2_4gb", Rarity.COMMON);
        hardware("ddr3_16gb", Rarity.UNCOMMON);
        hardware("ddr4_32gb", Rarity.RARE);
        hardware("ddr4_128gb_ecc", Rarity.EPIC);
        hardware("ddr5_48gb", Rarity.RARE);
        hardware("ddr5_32gb_8000", Rarity.EPIC);
        hardware("ddr5_256gb_ecc", Rarity.EPIC);
        // PSUs
        hardware("psu_300w", Rarity.COMMON);
        hardware("psu_550w", Rarity.COMMON);
        hardware("psu_850w", Rarity.UNCOMMON);
        hardware("psu_1200w", Rarity.UNCOMMON);
        hardware("psu_2000w", Rarity.RARE);
        hardware("psu_3200w", Rarity.EPIC);
        hardware("psu_400w", Rarity.COMMON);
        hardware("psu_650w", Rarity.COMMON);
        hardware("psu_1000w", Rarity.UNCOMMON);
        hardware("psu_1600w", Rarity.RARE);
        // storage
        hardware("hdd_500gb", Rarity.COMMON);
        hardware("ssd_1tb", Rarity.UNCOMMON);
        hardware("nvme_2tb", Rarity.RARE);
        hardware("enterprise_ssd_8tb", Rarity.EPIC);
        // ASIC miners (Bitcoin only, go straight into a rack bay)
        hardware("antminer_s9", Rarity.UNCOMMON);
        hardware("antminer_s19_pro", Rarity.RARE);
        hardware("antminer_s21", Rarity.EPIC);
    }

    private ModItems() {
    }

    private static DeferredItem<Item> simple(String name, Rarity rarity, int stackSize) {
        return add(ITEMS.registerSimpleItem(name, p -> p.rarity(rarity).stacksTo(stackSize)));
    }

    private static DeferredItem<Item> material(String name, Rarity rarity, int stackSize) {
        DeferredItem<Item> item = ITEMS.registerSimpleItem(name, p -> p.rarity(rarity).stacksTo(stackSize));
        MATERIAL_ORDER.add(item);
        MATERIAL_ITEMS.put(name, item);
        return item;
    }

    private static <T extends Item> DeferredItem<T> energy(DeferredItem<T> item) {
        ENERGY_ORDER.add(item);
        return item;
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
