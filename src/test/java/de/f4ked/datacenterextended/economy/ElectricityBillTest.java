package de.f4ked.datacenterextended.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElectricityBillTest {

    @Test
    void priceIsWattsTimesPricePerKwh() {
        assertEquals(4000.0, ElectricityBill.centsPerHour(1000, 40.0), 1e-9); // 1 kW at $40/kWh = $40/h
        assertEquals(0.0, ElectricityBill.centsPerHour(-5, 40.0), 1e-9);
    }

    @Test
    void paysWholeCentsAndKeepsTheRest() {
        ElectricityBill bill = new ElectricityBill();
        bill.accrue(2.7);
        assertEquals(2, bill.settle(100));
        assertEquals(0.7, bill.pending(), 1e-9);
        assertFalse(bill.unpaid());
    }

    @Test
    void emptyWalletShutsDownUntilTheDebtIsPaid() {
        ElectricityBill bill = new ElectricityBill();
        bill.accrue(50);
        assertEquals(20, bill.settle(20), "pays what the wallet has");
        assertTrue(bill.unpaid());
        assertEquals(30, bill.pending(), 1e-9);
        assertEquals(0, bill.settle(29), "partial payments don't restart the rack");
        assertTrue(bill.unpaid());
        assertEquals(30, bill.settle(30));
        assertFalse(bill.unpaid());
        assertEquals(0, bill.pending(), 1e-9);
    }
}
