package de.f4ked.datacenterextended.hardware;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataUnitsTest {

    @Test
    void parsesCommonNotations() {
        assertEquals(1024, DataUnits.parseBytes("1 KB"));
        assertEquals(1024 * 1024, DataUnits.parseBytes("1MB"));
        assertEquals(1024 * 1024, DataUnits.parseBytes("1 MiB"));
        assertEquals(512 * 1024, DataUnits.parseBytes("512kb"));
        assertEquals((long) (2.5 * 1024 * 1024 * 1024), DataUnits.parseBytes("2.5 GB"));
        assertEquals(42, DataUnits.parseBytes("42"));
        assertEquals(42, DataUnits.parseBytes("42 B"));
    }

    @Test
    void rejectsGarbage() {
        assertThrows(IllegalArgumentException.class, () -> DataUnits.parseBytes("lots"));
        assertThrows(IllegalArgumentException.class, () -> DataUnits.parseBytes("1 XB"));
    }

    @Test
    void formatsWithBinaryUnits() {
        assertEquals("1 MB", DataUnits.formatBytes(1024 * 1024));
        assertEquals("10 KB/s", DataUnits.formatBytesPerSecond(10 * 1024));
        assertEquals("1 MH/s", DataUnits.formatHashrate(1_000_000));
        assertEquals("2.50 MH/s", DataUnits.formatHashrate(2_500_000));
    }
}
