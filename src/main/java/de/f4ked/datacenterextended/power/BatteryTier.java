package de.f4ked.datacenterextended.power;

/** Battery banks: capacity and charge/discharge rate (FE/t). */
public enum BatteryTier {
    LEAD_ACID("lead_acid_battery", 500_000, 256),
    LITHIUM_ION("lithium_ion_battery", 4_000_000, 1024),
    LFP("lfp_battery_bank", 20_000_000, 4096);

    private final String id;
    private final int capacity;
    private final int rate;

    BatteryTier(String id, int capacity, int rate) {
        this.id = id;
        this.capacity = capacity;
        this.rate = rate;
    }

    public String id() {
        return id;
    }

    public int capacity() {
        return capacity;
    }

    public int rate() {
        return rate;
    }
}
