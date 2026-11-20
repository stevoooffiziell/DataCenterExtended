package de.f4ked.datacenterextended.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyTest {

    @Test
    void formatsCents() {
        assertEquals("$1,234.56", Money.format(123456));
        assertEquals("$0.05", Money.format(5));
        assertEquals("-$2.00", Money.format(-200));
    }

    @Test
    void roundsDollarsToCents() {
        assertEquals(1999, Money.toCents(19.99));
        assertEquals(25000, Money.toCents(250));
    }

    @Test
    void formatsPricesBelowOneDollarWithMoreDecimals() {
        assertEquals("$0.2531", Money.formatPrice(0.25314));
        assertEquals("$1,512.40", Money.formatPrice(1512.4));
    }
}
