package de.f4ked.datacenterextended.hardware;

import de.f4ked.datacenterextended.registry.ModDataComponents;
import de.f4ked.datacenterextended.registry.ModDataMaps;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Slot layout of a server item's parts (stored in the {@code datacenterextended:server_parts} component) and the rules
 * for which part fits where.
 */
public final class ServerLayout {
    public static final int SLOT_MOTHERBOARD = 0;
    public static final int SLOT_PSU = 1;
    public static final int SLOT_CPU_START = 2;
    public static final int SLOT_GPU_START = SLOT_CPU_START + HardwareStats.MAX_CPU_SOCKETS;          // 6
    public static final int SLOT_STORAGE_START = SLOT_GPU_START + HardwareStats.MAX_GPU_SLOTS;        // 12
    public static final int SLOT_MEMORY_START = SLOT_STORAGE_START + HardwareStats.MAX_STORAGE_SLOTS; // 16
    public static final int SIZE = SLOT_MEMORY_START + HardwareStats.MAX_MEMORY_SLOTS;                // 24

    private ServerLayout() {
    }

    public static HardwareType slotType(int slot) {
        if (slot == SLOT_MOTHERBOARD) return HardwareType.MOTHERBOARD;
        if (slot == SLOT_PSU) return HardwareType.PSU;
        if (slot < SLOT_GPU_START) return HardwareType.CPU;
        if (slot < SLOT_STORAGE_START) return HardwareType.GPU;
        if (slot < SLOT_MEMORY_START) return HardwareType.STORAGE;
        return HardwareType.MEMORY;
    }

    /** Whether the item is the right kind of part for the slot (ignores motherboard limits). */
    public static boolean isValidForSlot(int slot, ItemStack stack) {
        HardwareStats stats = ModDataMaps.hardware(stack);
        return stats != null && slot >= 0 && slot < SIZE && stats.type() == slotType(slot);
    }

    /** Whether a CPU/GPU/storage/memory slot is usable with the given motherboard. MB and PSU slots are always usable. */
    public static boolean isSlotEnabled(int slot, ItemStack motherboard) {
        if (slot == SLOT_MOTHERBOARD || slot == SLOT_PSU) return true;
        HardwareStats board = board(motherboard);
        if (board == null) return false;
        if (slot < SLOT_GPU_START) return slot - SLOT_CPU_START < board.cpuSockets();
        if (slot < SLOT_STORAGE_START) return slot - SLOT_GPU_START < board.gpuSlots();
        if (slot < SLOT_MEMORY_START) return slot - SLOT_STORAGE_START < board.storageSlots();
        return slot - SLOT_MEMORY_START < board.memorySlots();
    }

    /** Socket / memory type check against the motherboard. Parts other than CPUs and memory are always compatible. */
    public static boolean isCompatible(int slot, ItemStack part, ItemStack motherboard) {
        HardwareStats stats = ModDataMaps.hardware(part);
        HardwareStats board = board(motherboard);
        if (stats == null || board == null) return true;
        return switch (stats.type()) {
            case CPU -> stats.fitsSocketOf(board);
            case MEMORY -> stats.fitsMemoryOf(board);
            default -> true;
        };
    }

    /** Total RAM in GB of the memory modules in the given parts, optionally skipping one slot. */
    public static double installedMemory(List<ItemStack> parts, int skipSlot) {
        double total = 0;
        for (int i = SLOT_MEMORY_START; i < SIZE; i++) {
            if (i == skipSlot) continue;
            HardwareStats stats = statsIn(parts, i, HardwareType.MEMORY);
            if (stats != null) total += stats.memory();
        }
        return total;
    }

    /** Whether inserting {@code module} into {@code slot} stays within the motherboard's maximum RAM. */
    public static boolean fitsMemoryLimit(List<ItemStack> parts, int slot, ItemStack module) {
        HardwareStats board = statsIn(parts, SLOT_MOTHERBOARD, HardwareType.MOTHERBOARD);
        HardwareStats stats = ModDataMaps.hardware(module);
        if (board == null || stats == null || board.maxMemory() <= 0) return true;
        return installedMemory(parts, slot) + stats.memory() <= board.maxMemory() + 1e-9;
    }

    public static NonNullList<ItemStack> readParts(ItemStack server) {
        NonNullList<ItemStack> parts = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ItemContainerContents contents = server.get(ModDataComponents.SERVER_PARTS.get());
        if (contents != null) {
            contents.copyInto(parts);
        }
        return parts;
    }

    public static void writeParts(ItemStack server, List<ItemStack> parts) {
        boolean empty = parts.stream().allMatch(ItemStack::isEmpty);
        if (empty) {
            server.remove(ModDataComponents.SERVER_PARTS.get());
        } else {
            server.set(ModDataComponents.SERVER_PARTS.get(), ItemContainerContents.fromItems(parts));
        }
    }

    public static @Nullable HardwareStats statsIn(List<ItemStack> parts, int slot, HardwareType type) {
        HardwareStats stats = ModDataMaps.hardware(parts.get(slot));
        return stats != null && stats.type() == type ? stats : null;
    }

    private static @Nullable HardwareStats board(ItemStack motherboard) {
        HardwareStats board = ModDataMaps.hardware(motherboard);
        return board != null && board.type() == HardwareType.MOTHERBOARD ? board : null;
    }
}
