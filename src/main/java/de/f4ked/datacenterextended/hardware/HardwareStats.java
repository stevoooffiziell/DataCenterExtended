package de.f4ked.datacenterextended.hardware;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Locale;

/**
 * Stats of a single hardware component, attached to items through the {@code datacenterextended:hardware} data map.
 * Because it is a data map, ANY item (including items from other mods) can become a component.
 *
 * <pre>{@code
 * "minecraft:amethyst_shard": { "type": "cpu", "socket": "am4", "compute": 12.5, "power": 40, "recommended_memory": 8 }
 * }</pre>
 *
 * @param type              which server slot the item fits into
 * @param compute           CPUs: compute power in KB/s (used by hosting and fabrication)
 * @param hashrate          GPUs: hashrate in MH/s (used by mining)
 * @param power             power draw in watts; for PSUs: the maximum wattage it delivers
 * @param cpuSockets        motherboards: number of CPU sockets (max 4)
 * @param gpuSlots          motherboards: usable GPU slots (max 6)
 * @param storageSlots      motherboards: usable storage bays (max 4)
 * @param memorySlots       motherboards: memory slots (max 8)
 * @param efficiency        PSUs: 0..1, FE drawn = watts * fePerWatt / efficiency
 * @param storage           storage drives: capacity in GB
 * @param socket            CPUs and motherboards: socket id (e.g. "am4"). Empty = fits every socket
 * @param memoryType        memory and motherboards: memory type id (e.g. "ddr4"). Empty = fits every type
 * @param maxMemory         motherboards: maximum total RAM in GB (0 = unlimited)
 * @param memory            memory modules: capacity in GB
 * @param speed             memory modules: speed in MT/s
 * @param recommendedMemory CPUs: RAM in GB the CPU needs to reach full speed
 */
public record HardwareStats(HardwareType type, double compute, double hashrate, int power,
                            int cpuSockets, int gpuSlots, int storageSlots, int memorySlots,
                            double efficiency, double storage, String socket, String memoryType,
                            double maxMemory, double memory, int speed, double recommendedMemory) {

    public static final int MAX_CPU_SOCKETS = 4;
    public static final int MAX_GPU_SLOTS = 6;
    public static final int MAX_STORAGE_SLOTS = 4;
    public static final int MAX_MEMORY_SLOTS = 8;

    private static final Codec<String> ID = Codec.STRING.xmap(s -> s.toLowerCase(Locale.ROOT), s -> s);

    public static final Codec<HardwareStats> CODEC = RecordCodecBuilder.create(i -> i.group(
            HardwareType.CODEC.fieldOf("type").forGetter(HardwareStats::type),
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("compute", 0.0).forGetter(HardwareStats::compute),
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("hashrate", 0.0).forGetter(HardwareStats::hashrate),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("power", 0).forGetter(HardwareStats::power),
            Codec.intRange(0, MAX_CPU_SOCKETS).optionalFieldOf("cpu_sockets", 1).forGetter(HardwareStats::cpuSockets),
            Codec.intRange(0, MAX_GPU_SLOTS).optionalFieldOf("gpu_slots", 0).forGetter(HardwareStats::gpuSlots),
            Codec.intRange(0, MAX_STORAGE_SLOTS).optionalFieldOf("storage_slots", 2).forGetter(HardwareStats::storageSlots),
            Codec.intRange(0, MAX_MEMORY_SLOTS).optionalFieldOf("memory_slots", 2).forGetter(HardwareStats::memorySlots),
            Codec.doubleRange(0.01, 1.0).optionalFieldOf("efficiency", 1.0).forGetter(HardwareStats::efficiency),
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("storage", 0.0).forGetter(HardwareStats::storage),
            ID.optionalFieldOf("socket", "").forGetter(HardwareStats::socket),
            ID.optionalFieldOf("memory_type", "").forGetter(HardwareStats::memoryType),
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("max_memory", 0.0).forGetter(HardwareStats::maxMemory),
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("memory", 0.0).forGetter(HardwareStats::memory),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("speed", 0).forGetter(HardwareStats::speed),
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("recommended_memory", 0.0).forGetter(HardwareStats::recommendedMemory)
    ).apply(i, HardwareStats::new));

    /** Compute in bytes per second. */
    public double computeBytesPerSecond() {
        return compute * DataUnits.KB;
    }

    /** Hashrate in hashes per second. */
    public double hashesPerSecond() {
        return hashrate * 1_000_000.0;
    }

    /** Whether this CPU fits the given motherboard's socket. An empty socket on either side is a wildcard. */
    public boolean fitsSocketOf(HardwareStats board) {
        return socket.isEmpty() || board.socket.isEmpty() || socket.equals(board.socket);
    }

    /** Whether this memory module is the memory type the given motherboard takes. Empty = wildcard. */
    public boolean fitsMemoryOf(HardwareStats board) {
        return memoryType.isEmpty() || board.memoryType.isEmpty() || memoryType.equals(board.memoryType);
    }
}
