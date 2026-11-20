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
    public static final int SLOT_GPU_START = SLOT_CPU_START + HardwareStats.MAX_CPU_SOCKETS;        // 6
    public static final int SLOT_STORAGE_START = SLOT_GPU_START + HardwareStats.MAX_GPU_SLOTS;      // 12
    public static final int SIZE = SLOT_STORAGE_START + HardwareStats.MAX_STORAGE_SLOTS;            // 16

    private ServerLayout() {
    }

    public static HardwareType slotType(int slot) {
        if (slot == SLOT_MOTHERBOARD) return HardwareType.MOTHERBOARD;
        if (slot == SLOT_PSU) return HardwareType.PSU;
        if (slot < SLOT_GPU_START) return HardwareType.CPU;
        if (slot < SLOT_STORAGE_START) return HardwareType.GPU;
        return HardwareType.STORAGE;
    }

    /** Whether the item is the right kind of part for the slot (ignores motherboard limits). */
    public static boolean isValidForSlot(int slot, ItemStack stack) {
        HardwareStats stats = ModDataMaps.hardware(stack);
        return stats != null && slot >= 0 && slot < SIZE && stats.type() == slotType(slot);
    }

    /** Whether a CPU/GPU/storage slot is usable with the given motherboard. MB and PSU slots are always usable. */
    public static boolean isSlotEnabled(int slot, ItemStack motherboard) {
        if (slot == SLOT_MOTHERBOARD || slot == SLOT_PSU) return true;
        HardwareStats board = ModDataMaps.hardware(motherboard);
        if (board == null || board.type() != HardwareType.MOTHERBOARD) return false;
        if (slot < SLOT_GPU_START) return slot - SLOT_CPU_START < board.cpuSockets();
        if (slot < SLOT_STORAGE_START) return slot - SLOT_GPU_START < board.gpuSlots();
        return slot - SLOT_STORAGE_START < board.storageSlots();
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
}
