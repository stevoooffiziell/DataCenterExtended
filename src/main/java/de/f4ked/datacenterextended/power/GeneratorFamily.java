package de.f4ked.datacenterextended.power;

import java.util.Locale;

public enum GeneratorFamily {
    SOLAR, WIND, WATER, COMBUSTION, GEOTHERMAL, NUCLEAR;

    public String translationKey() {
        return "generator.datacenterextended.family." + name().toLowerCase(Locale.ROOT);
    }
}
