package de.f4ked.datacenterextended.power.model;

import de.f4ked.datacenterextended.power.GeneratorStatus;

/**
 * Photovoltaics: output follows the sun's elevation (Minecraft time 0 = sunrise, 6000 = noon, 12000 = sunset) and
 * drops under clouds – real panels make roughly a third of their power under overcast skies.
 */
public final class SolarModel {
    private SolarModel() {
    }

    /**
     * @param timeOfDay  day time 0..23999
     * @param rain       rain level 0..1
     * @param thunder    thunder level 0..1
     * @param seesSky    nothing above the panel
     * @param hasDaylight the dimension has a sun (false in the Nether / End)
     */
    public static Output output(long timeOfDay, float rain, float thunder, boolean seesSky, boolean hasDaylight) {
        if (!hasDaylight || !seesSky) return Output.off(GeneratorStatus.NO_SKY);
        long t = Math.floorMod(timeOfDay, 24000L);
        double elevation = t < 12000 ? Math.sin(Math.PI * t / 12000.0) : 0;
        if (elevation <= 0.02) return Output.off(GeneratorStatus.NIGHT);
        double weather = 1.0 - 0.65 * rain - 0.20 * thunder;
        double factor = Output.clamp(elevation * weather);
        return new Output(factor, rain > 0.2F ? GeneratorStatus.WEATHER : GeneratorStatus.RUNNING);
    }
}
