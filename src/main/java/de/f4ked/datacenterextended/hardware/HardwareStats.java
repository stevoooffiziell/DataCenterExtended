package de.f4ked.datacenterextended.hardware;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Stats of a single hardware component, attached to items through the {@code datacenterextended:hardware} data map.
 * Because it is a data map, ANY item (including items from other mods) can become a component.
 *
 * <pre>{@code
 * "minecraft:amethyst_shard": { "type": "cpu", "compute": 12.5, "power": 40 }
 * }</pre>
 *
 * @param type         which server slot the item fits into
 * @param compute      compute power in KB/s (used by hosting and fabrication)
 * @param hashrate     hashrate in MH/s (used by mining)
 * @param power        power draw in watts – for PSUs: the maximum wattage it delivers
 * @param cpuSockets   motherboards only: usable CPU slots (max 4)
 * @param gpuSlots     motherboards only: usable GPU slots (max 6)
 * @param storageSlots motherboards only: usable storage bays (max 4)
 * @param efficiency   PSUs only: 0..1, FE drawn = watts * fePerWatt / efficiency
 * @param storage      storage drives only: capacity in GB
 */
public record HardwareStats(HardwareType type, double compute, double hashrate, int power,
                            int cpuSockets, int gpuSlots, int storageSlots, double efficiency, double storage) {

    public static final int MAX_CPU_SOCKETS = 4;
    public static final int MAX_GPU_SLOTS = 6;
    public static final int MAX_STORAGE_SLOTS = 4;

    public static final Codec<HardwareStats> CODEC = RecordCodecBuilder.create(i -> i.group(
            HardwareType.CODEC.fieldOf("type").forGetter(HardwareStats::type),
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("compute", 0.0).forGetter(HardwareStats::compute),
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("hashrate", 0.0).forGetter(HardwareStats::hashrate),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("power", 0).forGetter(HardwareStats::power),
            Codec.intRange(0, MAX_CPU_SOCKETS).optionalFieldOf("cpu_sockets", 0).forGetter(HardwareStats::cpuSockets),
            Codec.intRange(0, MAX_GPU_SLOTS).optionalFieldOf("gpu_slots", 0).forGetter(HardwareStats::gpuSlots),
            Codec.intRange(0, MAX_STORAGE_SLOTS).optionalFieldOf("storage_slots", 2).forGetter(HardwareStats::storageSlots),
            Codec.doubleRange(0.01, 1.0).optionalFieldOf("efficiency", 1.0).forGetter(HardwareStats::efficiency),
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("storage", 0.0).forGetter(HardwareStats::storage)
    ).apply(i, HardwareStats::new));

    /** Compute in bytes per second. */
    public double computeBytesPerSecond() {
        return compute * DataUnits.KB;
    }

    /** Hashrate in hashes per second. */
    public double hashesPerSecond() {
        return hashrate * 1_000_000.0;
    }
}
