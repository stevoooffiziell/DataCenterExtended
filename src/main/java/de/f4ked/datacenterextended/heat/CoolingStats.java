package de.f4ked.datacenterextended.heat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A cooling item for the rack's cooling slots, attached through the {@code datacenterextended:cooling} data map.
 *
 * <pre>{@code
 * "datacenterextended:rack_fan": { "capacity": 800, "power": 15 }
 * }</pre>
 *
 * @param capacity watts of heat the item removes
 * @param power    watts the item draws itself (FE or electricity bill)
 */
public record CoolingStats(int capacity, int power) {
    public static final Codec<CoolingStats> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("capacity").forGetter(CoolingStats::capacity),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("power", 0).forGetter(CoolingStats::power)
    ).apply(i, CoolingStats::new));
}
