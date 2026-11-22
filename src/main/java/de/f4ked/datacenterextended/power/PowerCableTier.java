package de.f4ked.datacenterextended.power;

/** Power cables: FE/t a network can move (limited by its weakest cable). */
public enum PowerCableTier {
    COPPER("copper_power_cable", 256),
    ALUMINIUM("aluminium_power_cable", 2048),
    SUPERCONDUCTING("superconducting_cable", 16384);

    private final String id;
    private final int transfer;

    PowerCableTier(String id, int transfer) {
        this.id = id;
        this.transfer = transfer;
    }

    public String id() {
        return id;
    }

    public int transfer() {
        return transfer;
    }

    public String translationKey() {
        return "block.datacenterextended." + id;
    }
}
