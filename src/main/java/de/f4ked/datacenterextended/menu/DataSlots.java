package de.f4ked.datacenterextended.menu;

import net.minecraft.world.inventory.ContainerData;

/** ContainerData values are synced as 16-bit shorts; longs are split across 4 consecutive slots. */
public final class DataSlots {
    private DataSlots() {
    }

    /** The 16-bit chunk of {@code value} for slot {@code index}, where the long starts at {@code base}. */
    public static int chunk(long value, int index, int base) {
        return (int) ((value >>> (16 * (index - base))) & 0xFFFF);
    }

    public static long readLong(ContainerData data, int base) {
        long value = 0;
        for (int i = 0; i < 4; i++) {
            value |= ((long) (data.get(base + i) & 0xFFFF)) << (16 * i);
        }
        return value;
    }

    /** Reads a non-negative 15-bit int (survives the short sign extension). */
    public static int readSmall(ContainerData data, int index) {
        return data.get(index) & 0x7FFF;
    }
}
