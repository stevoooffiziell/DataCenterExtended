package de.f4ked.datacenterextended.power.model;

import de.f4ked.datacenterextended.power.GeneratorStatus;

/**
 * Water power. A water wheel is driven by flowing water at its sides; turbines use the "head", the height of the
 * water column above them (hydro power is proportional to flow × head).
 */
public final class HydroModel {
    private HydroModel() {
    }

    /** @param flowingSides horizontal sides with flowing water (0..4) */
    public static Output wheel(int flowingSides) {
        if (flowingSides <= 0) return Output.off(GeneratorStatus.NO_WATER);
        return new Output(Output.clamp(flowingSides / 4.0), GeneratorStatus.RUNNING);
    }

    /** @param head water blocks stacked directly above the turbine, {@code maxHead} gives full output */
    public static Output turbine(int head, int maxHead) {
        if (head <= 0) return Output.off(GeneratorStatus.NO_WATER);
        return new Output(Output.clamp(head / (double) maxHead), GeneratorStatus.RUNNING);
    }

    /** Geothermal: heat from adjacent lava sources ({@code sourcesForFull} give full output). */
    public static Output geothermal(int lavaSources, double sourcesForFull, double dimensionBonus) {
        if (lavaSources <= 0) return Output.off(GeneratorStatus.NO_LAVA);
        return new Output(Output.clamp(lavaSources * dimensionBonus / sourcesForFull), GeneratorStatus.RUNNING);
    }
}
