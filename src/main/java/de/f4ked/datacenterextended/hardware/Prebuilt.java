package de.f4ked.datacenterextended.hardware;

import de.f4ked.datacenterextended.registry.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Prebuilt servers sold in the shop: complete, bootable configurations of their era. They cost more than the parts
 * (config {@code shop.prebuiltMarkup}) and become available when their newest part is released.
 */
public enum Prebuilt {
    OFFICE_2004(RackKind.HOSTING, "office_motherboard", "psu_300w",
            List.of("pentium_4"), List.of(), List.of("hdd_500gb"), List.of("ddr2_1gb", "ddr2_1gb")),
    HOME_2012(RackKind.HOSTING, "z77_motherboard", "psu_550w",
            List.of("core_i5_2500k"), List.of(), List.of("hdd_500gb", "hdd_500gb"), List.of("ddr3_8gb", "ddr3_8gb")),
    MINING_2017(RackKind.MINING, "mining_motherboard", "psu_2000w",
            List.of("core_i7_8700k"),
            List.of("geforce_gtx_1080_ti", "geforce_gtx_1080_ti", "geforce_gtx_1080_ti", "geforce_gtx_1080_ti",
                    "geforce_gtx_1080_ti", "geforce_gtx_1080_ti"),
            List.of("hdd_500gb"), List.of("ddr4_8gb")),
    EPYC_2019(RackKind.HOSTING, "epyc_sp3_motherboard", "psu_1200w",
            List.of("epyc_7742", "epyc_7742"), List.of(), List.of("ssd_1tb", "ssd_1tb", "ssd_1tb", "ssd_1tb"),
            List.of("ddr4_64gb_ecc", "ddr4_64gb_ecc", "ddr4_64gb_ecc", "ddr4_64gb_ecc",
                    "ddr4_64gb_ecc", "ddr4_64gb_ecc", "ddr4_64gb_ecc", "ddr4_64gb_ecc")),
    GAMING_2019(RackKind.MINING, "gaming_motherboard", "psu_850w",
            List.of("core_i9_9900k"), List.of("geforce_rtx_2080_ti"), List.of("nvme_2tb"), List.of("ddr4_16gb", "ddr4_16gb")),
    WORKSTATION_2020(RackKind.HOSTING, "x570_motherboard", "psu_850w",
            List.of("ryzen_9_5950x"), List.of("geforce_rtx_3080"), List.of("nvme_2tb"),
            List.of("ddr4_32gb", "ddr4_32gb", "ddr4_32gb", "ddr4_32gb")),
    AI_2023(RackKind.MINING, "server_motherboard", "psu_3200w",
            List.of("epyc_9654", "epyc_9654"), List.of("nvidia_h100", "nvidia_h100", "nvidia_h100"),
            List.of("enterprise_ssd_8tb", "enterprise_ssd_8tb"),
            List.of("ddr5_128gb_ecc", "ddr5_128gb_ecc", "ddr5_128gb_ecc", "ddr5_128gb_ecc",
                    "ddr5_128gb_ecc", "ddr5_128gb_ecc", "ddr5_128gb_ecc", "ddr5_128gb_ecc"));

    /** What the configuration is meant for (shown in the tooltip; it works in any rack). */
    public enum RackKind { HOSTING, MINING }

    private final RackKind kind;
    private final String board, psu;
    private final List<String> cpus, gpus, drives, memory;

    Prebuilt(RackKind kind, String board, String psu, List<String> cpus, List<String> gpus, List<String> drives, List<String> memory) {
        this.kind = kind;
        this.board = board;
        this.psu = psu;
        this.cpus = cpus;
        this.gpus = gpus;
        this.drives = drives;
        this.memory = memory;
    }

    /** Item id, e.g. {@code prebuilt_office_2004}. */
    public String id() {
        return "prebuilt_" + name().toLowerCase(Locale.ROOT);
    }

    public RackKind kind() {
        return kind;
    }

    /** All part items, one entry per installed part. */
    public List<Item> parts() {
        List<Item> parts = new ArrayList<>();
        parts.add(ModItems.hardware(board));
        parts.add(ModItems.hardware(psu));
        for (List<String> group : List.of(cpus, gpus, drives, memory)) {
            for (String id : group) parts.add(ModItems.hardware(id));
        }
        return parts;
    }

    /** A server item with all parts installed. */
    public ItemStack assemble() {
        NonNullList<ItemStack> parts = NonNullList.withSize(ServerLayout.SIZE, ItemStack.EMPTY);
        parts.set(ServerLayout.SLOT_MOTHERBOARD, new ItemStack(ModItems.hardware(board)));
        parts.set(ServerLayout.SLOT_PSU, new ItemStack(ModItems.hardware(psu)));
        fill(parts, ServerLayout.SLOT_CPU_START, cpus);
        fill(parts, ServerLayout.SLOT_GPU_START, gpus);
        fill(parts, ServerLayout.SLOT_STORAGE_START, drives);
        fill(parts, ServerLayout.SLOT_MEMORY_START, memory);
        ItemStack server = new ItemStack(ModItems.SERVER.get());
        ServerLayout.writeParts(server, parts);
        return server;
    }

    private static void fill(NonNullList<ItemStack> parts, int start, List<String> ids) {
        for (int i = 0; i < ids.size(); i++) parts.set(start + i, new ItemStack(ModItems.hardware(ids.get(i))));
    }
}
