package de.f4ked.datacenterextended.power;

/** Grid connections: maximum export in FE/t (5 kW, 50 kW, 500 kW at the default 0.1 FE per watt). */
public enum GridTier {
    RESIDENTIAL("residential_grid_connection", 50),
    COMMERCIAL("commercial_grid_connection", 500),
    UTILITY("utility_grid_connection", 5000);

    private final String id;
    private final int rate;

    GridTier(String id, int rate) {
        this.id = id;
        this.rate = rate;
    }

    public String id() {
        return id;
    }

    public int rate() {
        return rate;
    }
}
