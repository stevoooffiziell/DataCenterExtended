package de.f4ked.datacenterextended.power;

import de.f4ked.datacenterextended.power.model.GridModel;
import de.f4ked.datacenterextended.power.model.HydroModel;
import de.f4ked.datacenterextended.power.model.Output;
import de.f4ked.datacenterextended.power.model.ReactorModel;
import de.f4ked.datacenterextended.power.model.SolarModel;
import de.f4ked.datacenterextended.power.model.WindModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerModelTest {

    @Test
    void solarPeaksAtNoonAndStopsAtNight() {
        Output noon = SolarModel.output(6000, 0, 0, true, true);
        assertEquals(1.0, noon.factor(), 1e-9);
        assertEquals(GeneratorStatus.RUNNING, noon.status());
        assertTrue(SolarModel.output(2000, 0, 0, true, true).factor() < noon.factor());
        assertEquals(GeneratorStatus.NIGHT, SolarModel.output(18000, 0, 0, true, true).status());
        assertEquals(0, SolarModel.output(18000, 0, 0, true, true).factor());
        assertEquals(GeneratorStatus.NO_SKY, SolarModel.output(6000, 0, 0, false, true).status());
        assertEquals(GeneratorStatus.NO_SKY, SolarModel.output(6000, 0, 0, true, false).status());
    }

    @Test
    void cloudsReduceSolarToAThird() {
        Output rain = SolarModel.output(6000, 1, 0, true, true);
        assertEquals(0.35, rain.factor(), 1e-9);
        assertEquals(GeneratorStatus.WEATHER, rain.status());
        assertEquals(0.15, SolarModel.output(6000, 1, 1, true, true).factor(), 1e-9);
    }

    @Test
    void windGrowsWithHeightAndNeedsFreeAir() {
        double low = WindModel.output(70, 63, 1, 0, 0, 0, 0).factor();
        double high = WindModel.output(63 + WindModel.FULL_HEIGHT, 63, 1, 0, 0, 0, 0).factor();
        assertTrue(high > low * 3, "high " + high + " vs low " + low);
        assertEquals(GeneratorStatus.TOO_LOW, WindModel.output(40, 63, 1, 0, 0, 0, 0).status());
        assertEquals(0, WindModel.output(40, 63, 1, 0, 0, 0, 0).factor());
        assertEquals(GeneratorStatus.BLOCKED, WindModel.output(150, 63, 0.3, 0, 0, 0, 0).status());
        assertEquals(0, WindModel.output(150, 63, 0.3, 0, 0, 0, 0).factor());
        double partly = WindModel.output(150, 63, 0.8, 0, 0, 0, 0).factor();
        double free = WindModel.output(150, 63, 1.0, 0, 0, 0, 0).factor();
        assertEquals(free * 0.64, partly, 1e-9);
    }

    @Test
    void hydroAndGeothermalScaleWithTheirSource() {
        assertEquals(GeneratorStatus.NO_WATER, HydroModel.wheel(0).status());
        assertEquals(0.5, HydroModel.wheel(2).factor(), 1e-9);
        assertEquals(1.0, HydroModel.wheel(4).factor(), 1e-9);
        assertEquals(0.5, HydroModel.turbine(16, 32).factor(), 1e-9);
        assertEquals(1.0, HydroModel.turbine(40, 32).factor(), 1e-9);
        assertEquals(GeneratorStatus.NO_LAVA, HydroModel.geothermal(0, 6, 1).status());
        assertEquals(0.5, HydroModel.geothermal(3, 6, 1).factor(), 1e-9);
        assertEquals(0.75, HydroModel.geothermal(3, 6, 1.5).factor(), 1e-9);
    }

    @Test
    void reactorWithSixWaterSidesHoldsFullPower() {
        double heat = 0;
        for (int i = 0; i < 100_000; i++) heat = ReactorModel.step(heat, ReactorModel.throttle(heat), 6);
        assertTrue(heat < ReactorModel.THROTTLE, "equilibrium heat " + heat);
        assertEquals(1.0, ReactorModel.throttle(heat), 1e-9);
    }

    @Test
    void reactorWithoutCoolingReachesTheScramLimit() {
        double heat = 0;
        int ticks = 0;
        while (heat < ReactorModel.SCRAM && ticks < 100_000) {
            heat = ReactorModel.step(heat, ReactorModel.throttle(heat), 0);
            ticks++;
        }
        assertTrue(heat >= ReactorModel.SCRAM, "should overheat, heat " + heat);
        assertTrue(ReactorModel.throttle(0.9) < 1.0 && ReactorModel.throttle(1.0) >= 0.25);
    }

    @Test
    void gridExportIsFarLessWorthThanRunningServers() {
        // default economy: a Pentium 4 server (~150 W at the wall) earns about $24 per hour hosting
        double serverCentsPerHour = 2400;
        double exportCentsPerHour = GridModel.centsPerHour(150, 3.0, GridModel.growth(2010, 0.065));
        assertTrue(exportCentsPerHour < serverCentsPerHour * 0.05, "export pays " + exportCentsPerHour + " cents/h");
        // even with 2025 prices it stays a side income
        double late = GridModel.centsPerHour(150, 3.0, GridModel.growth(2025, 0.065));
        assertTrue(late < serverCentsPerHour * 0.1, "2025 export pays " + late + " cents/h");
        assertTrue(late > exportCentsPerHour * 2, "export price should grow over the years");
    }

    @Test
    void feConversionMatchesWatts() {
        // 1 FE/t at 0.1 FE per watt = 10 W; over 72000 ticks (one hour) that is 10 Wh
        double cents = GridModel.centsForFe(72_000, 0.1, 100.0, 1.0);
        assertEquals(GridModel.centsPerHour(10, 100.0, 1.0), cents, 1e-9);
    }
}
