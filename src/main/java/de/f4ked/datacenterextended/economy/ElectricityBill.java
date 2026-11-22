package de.f4ked.datacenterextended.economy;

/**
 * The electricity bill of one rack (pure, no Minecraft classes, unit tested). Costs accrue every tick in cents and are
 * settled against the owner's wallet regularly. A wallet that can't cover the bill pays what it has and the rack is
 * {@link #unpaid()} (shut down) until the wallet can pay the rest in one go.
 */
public final class ElectricityBill {
    private double pending;
    private boolean unpaid;

    /** Cents per hour for a wall draw in watts at a price in dollars per kWh. */
    public static double centsPerHour(double wallWatts, double pricePerKWh) {
        return Math.max(0, wallWatts) / 1000.0 * pricePerKWh * 100.0;
    }

    public void accrue(double cents) {
        if (cents > 0) pending += cents;
    }

    /**
     * Settles the bill against a wallet.
     *
     * @param balance cents in the wallet
     * @return cents to take from the wallet (never more than {@code balance})
     */
    public long settle(long balance) {
        if (unpaid) {
            long owed = (long) Math.ceil(pending);
            if (balance >= owed) {
                pending = 0;
                unpaid = false;
                return owed;
            }
            return 0;
        }
        long owed = (long) Math.floor(pending);
        if (owed <= 0) return 0;
        long paid = Math.min(owed, Math.max(0, balance));
        pending -= paid;
        if (paid < owed) unpaid = true;
        return paid;
    }

    public void reset() {
        pending = 0;
        unpaid = false;
    }

    /** Cents not yet paid. */
    public double pending() {
        return pending;
    }

    public boolean unpaid() {
        return unpaid;
    }

    public void load(double pending, boolean unpaid) {
        this.pending = Math.max(0, pending);
        this.unpaid = unpaid;
    }
}
