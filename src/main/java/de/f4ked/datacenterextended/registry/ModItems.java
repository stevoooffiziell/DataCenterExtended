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

    public static final DeferredItem<BlockItem> TRADING_TERMINAL = add(ITEMS.registerSimpleBlockItem("trading_terminal", ModBlocks.TRADING_TERMINAL));

    // ------------------------------------------------------------------ server + tools
    /** A 1U server; its parts live in the server_parts data component (assembled at the Server Workbench). */
    public static final DeferredItem<Item> SERVER = add(ITEMS.registerSimpleItem("server", p -> p.stacksTo(1)));
    public static final DeferredItem<NetworkTesterItem> NETWORK_TESTER = add(ITEMS.registerItem("network_tester", NetworkTesterItem::new, p -> p.stacksTo(1)));

    // ------------------------------------------------------------------ currency
    public static final DeferredItem<Item> CREDIT = simple("credit", Rarity.COMMON, 64);
    public static final DeferredItem<Item> CREDIT_BUNDLE = simple("credit_bundle", Rarity.UNCOMMON, 64);
    public static final DeferredItem<Item> CREDIT_CRATE = simple("credit_crate", Rarity.RARE, 64);
    public static final DeferredItem<Item> BITCOIN = simple("bitcoin", Rarity.UNCOMMON, 64);

    // ------------------------------------------------------------------ crafting materials
    public static final DeferredItem<Item> SILICON = simple("silicon", Rarity.COMMON, 64);
    public static final DeferredItem<Item> CIRCUIT_BOARD = simple("circuit_board", Rarity.COMMON, 64);

    // ------------------------------------------------------------------ motherboards
    public static final DeferredItem<Item> OFFICE_MOTHERBOARD = hardware("office_motherboard", Rarity.COMMON);
    public static final DeferredItem<Item> GAMING_MOTHERBOARD = hardware("gaming_motherboard", Rarity.COMMON);
    public static final DeferredItem<Item> WORKSTATION_MOTHERBOARD = hardware("workstation_motherboard", Rarity.UNCOMMON);
    public static final DeferredItem<Item> MINING_MOTHERBOARD = hardware("mining_motherboard", Rarity.UNCOMMON);
    public static final DeferredItem<Item> SERVER_MOTHERBOARD = hardware("server_motherboard", Rarity.RARE);

    // ------------------------------------------------------------------ CPUs
    public static final DeferredItem<Item> PENTIUM_4 = hardware("pentium_4", Rarity.COMMON);
    public static final DeferredItem<Item> AMD_FX_4300 = hardware("amd_fx_4300", Rarity.COMMON);
    public static final DeferredItem<Item> CORE_I5_2500K = hardware("core_i5_2500k", Rarity.COMMON);
    public static final DeferredItem<Item> RYZEN_5_3600 = hardware("ryzen_5_3600", Rarity.UNCOMMON);
    public static final DeferredItem<Item> CORE_I9_9900K = hardware("core_i9_9900k", Rarity.UNCOMMON);
    public static final DeferredItem<Item> RYZEN_9_7950X = hardware("ryzen_9_7950x", Rarity.RARE);
    public static final DeferredItem<Item> EPYC_9654 = hardware("epyc_9654", Rarity.EPIC);

    // ------------------------------------------------------------------ GPUs
    public static final DeferredItem<Item> GEFORCE_GTX_460 = hardware("geforce_gtx_460", Rarity.COMMON);
    public static final DeferredItem<Item> RADEON_HD_7970 = hardware("radeon_hd_7970", Rarity.COMMON);
    public static final DeferredItem<Item> RADEON_RX_580 = hardware("radeon_rx_580", Rarity.UNCOMMON);
    public static final DeferredItem<Item> GEFORCE_GTX_1080_TI = hardware("geforce_gtx_1080_ti", Rarity.UNCOMMON);
    public static final DeferredItem<Item> GEFORCE_RTX_3090 = hardware("geforce_rtx_3090", Rarity.RARE);
    public static final DeferredItem<Item> GEFORCE_RTX_4090 = hardware("geforce_rtx_4090", Rarity.EPIC);

    // ------------------------------------------------------------------ PSUs
    public static final DeferredItem<Item> PSU_300W = hardware("psu_300w", Rarity.COMMON);
    public static final DeferredItem<Item> PSU_550W = hardware("psu_550w", Rarity.COMMON);
    public static final DeferredItem<Item> PSU_850W = hardware("psu_850w", Rarity.UNCOMMON);
    public static final DeferredItem<Item> PSU_1200W = hardware("psu_1200w", Rarity.UNCOMMON);
    public static final DeferredItem<Item> PSU_2000W = hardware("psu_2000w", Rarity.RARE);
    public static final DeferredItem<Item> PSU_3200W = hardware("psu_3200w", Rarity.EPIC);

    // ------------------------------------------------------------------ storage
    public static final DeferredItem<Item> HDD_500GB = hardware("hdd_500gb", Rarity.COMMON);
    public static final DeferredItem<Item> SSD_1TB = hardware("ssd_1tb", Rarity.UNCOMMON);
    public static final DeferredItem<Item> NVME_2TB = hardware("nvme_2tb", Rarity.RARE);
    public static final DeferredItem<Item> ENTERPRISE_SSD_8TB = hardware("enterprise_ssd_8tb", Rarity.EPIC);

    private ModItems() {
    }

    private static DeferredItem<Item> simple(String name, Rarity rarity, int stackSize) {
        return add(ITEMS.registerSimpleItem(name, p -> p.rarity(rarity).stacksTo(stackSize)));
    }

    private static DeferredItem<Item> hardware(String name, Rarity rarity) {
        return simple(name, rarity, 16);
    }

    private static <T extends Item> DeferredItem<T> add(DeferredItem<T> item) {
        TAB_ORDER.add(item);
        return item;
    }
}
