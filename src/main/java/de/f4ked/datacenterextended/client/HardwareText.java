package de.f4ked.datacenterextended.client;

import de.f4ked.datacenterextended.hardware.DataUnits;
import de.f4ked.datacenterextended.hardware.HardwareStats;
import de.f4ked.datacenterextended.hardware.ServerSpec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

/** Display names for sockets, memory types and memory amounts. */
public final class HardwareText {
    private HardwareText() {
    }

    /** "LGA1151", "AM3+", ...; unknown sockets (other mods) fall back to the upper-case id. */
    public static Component socket(String id) {
        return Component.translatableWithFallback("socket.datacenterextended." + id, id.toUpperCase(Locale.ROOT));
    }

    public static String memoryType(String id) {
        return id.toUpperCase(Locale.ROOT);
    }

    public static String gb(double gb) {
        return DataUnits.formatBytes(gb * DataUnits.GB);
    }

    /** Socket / memory requirements of a motherboard, e.g. "LGA1151 · DDR4 · max 128 GB". */
    public static MutableComponent boardRequirements(HardwareStats board) {
        Component socketText = board.socket().isEmpty() ? Component.literal("*") : socket(board.socket());
        String memory = board.memoryType().isEmpty() ? "*" : memoryType(board.memoryType());
        String max = board.maxMemory() > 0 ? gb(board.maxMemory()) : "∞";
        return Component.translatable("tooltip.datacenterextended.board", socketText, memory, max);
    }

    /** "RAM 16 GB / 32 GB · DDR4-3200 · ×0.85". */
    public static MutableComponent memoryLine(ServerSpec spec, String memoryTypeId) {
        String type = memoryTypeId.isEmpty() ? "" : memoryType(memoryTypeId) + "-";
        String speed = spec.memorySpeed() > 0 ? type + spec.memorySpeed() : "-";
        return Component.translatable("gui.datacenterextended.memory", gb(spec.memoryGb()), gb(spec.recommendedMemory()), speed,
                String.format(Locale.ROOT, "%.2f", spec.memoryFactor()));
    }
}
