package de.f4ked.datacenterextended.power.model;

import de.f4ked.datacenterextended.power.GeneratorStatus;

/** Result of a generator model: fraction of the peak output (0..1) and the reason shown to the player. */
public record Output(double factor, GeneratorStatus status) {
    public static Output off(GeneratorStatus status) {
        return new Output(0, status);
    }

    static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
