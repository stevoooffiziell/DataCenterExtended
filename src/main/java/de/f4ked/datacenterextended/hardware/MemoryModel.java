package de.f4ked.datacenterextended.hardware;

/**
 * How RAM affects compute (pure, no Minecraft classes, unit tested).
 * <ul>
 *     <li><b>Capacity:</b> below the CPUs' recommended RAM, compute scales linearly from {@code starvedFactor} to 1.</li>
 *     <li><b>Speed:</b> {@code +speedBonusPer1000} per 1000 MT/s; all modules run at the slowest module's speed.</li>
 *     <li><b>Channels:</b> {@code +channelBonus} with 2 or more modules.</li>
 * </ul>
 */
public final class MemoryModel {
    private MemoryModel() {
    }

    public static double factor(double installedGb, double recommendedGb, int speedMts, int modules,
                                double starvedFactor, double speedBonusPer1000, double channelBonus) {
        if (modules <= 0 || installedGb <= 0) return starvedFactor;
        double coverage = recommendedGb <= 0 ? 1.0 : Math.min(1.0, installedGb / recommendedGb);
        double capacity = starvedFactor + (1.0 - starvedFactor) * coverage;
        double speed = 1.0 + speedBonusPer1000 * Math.max(0, speedMts) / 1000.0;
        double channels = modules >= 2 ? 1.0 + channelBonus : 1.0;
        return capacity * speed * channels;
    }
}
