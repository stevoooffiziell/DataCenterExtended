package de.f4ked.datacenterextended.time;

import de.f4ked.datacenterextended.economy.ShopPricing;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalendarTest {

    @Test
    void advancesOneDayPerTicksPerDay() {
        double day = Calendar.epochDay(LocalDate.of(2010, 7, 1));
        for (int i = 0; i < 2400; i++) day = Calendar.advance(day, 2400);
        assertEquals(Calendar.epochDay(LocalDate.of(2010, 7, 2)), day, 1e-6);
    }

    @Test
    void hoursAndFormatting() {
        double noon = Calendar.epochDay(LocalDate.of(2017, 3, 12)) + 0.5;
        assertEquals(12, Calendar.hour(noon));
        assertEquals("12 Mar 2017", Calendar.format(noon));
        assertEquals("Mar 2017", Calendar.formatMonth(LocalDate.of(2017, 3, 12)));
    }

    @Test
    void shopPricesDropWithAgeDownToTheFloor() {
        assertEquals(1.0, ShopPricing.ageFactor(-1, 0.15, 0.3), 1e-9);
        assertEquals(0.85, ShopPricing.ageFactor(1, 0.15, 0.3), 1e-9);
        assertEquals(0.3, ShopPricing.ageFactor(20, 0.15, 0.3), 1e-9);
    }
}
