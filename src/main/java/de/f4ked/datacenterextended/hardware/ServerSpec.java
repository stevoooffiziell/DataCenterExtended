package de.f4ked.datacenterextended.hardware;

import de.f4ked.datacenterextended.Config;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static de.f4ked.datacenterextended.hardware.ServerLayout.*;

/**
 * Effective stats of one server item, computed from its parts. This is the only place where component stats are
 * summed up – racks, the workbench screen and tooltips all use it.
 *
 * @param computeBps      effective compute in bytes/s (global multiplier applied)
 * @param hashesPerSecond effective hashrate in H/s (global multiplier applied)
 * @param watts           total power draw of all active parts
 * @param psuCapacity     watts the PSU delivers (0 = no PSU)
 * @param psuEfficiency   0..1
 * @param storageGb       total storage capacity of active drives
 * @param problem         {@link ServerProblem#NONE} if the server can boot
 */
public record ServerSpec(double computeBps, double hashesPerSecond, long watts, long psuCapacity,
                         double psuEfficiency, double storageGb, ServerProblem problem) {

    public static final ServerSpec EMPTY = new ServerSpec(0, 0, 0, 0, 1, 0, ServerProblem.NO_MOTHERBOARD);

    public static ServerSpec of(ItemStack server) {
        return server.isEmpty() ? EMPTY : of(readParts(server));
    }

    public static ServerSpec of(List<ItemStack> parts) {
        HardwareStats board = statsIn(parts, SLOT_MOTHERBOARD, HardwareType.MOTHERBOARD);
        HardwareStats psu = statsIn(parts, SLOT_PSU, HardwareType.PSU);

        double compute = 0, hashes = 0, storage = 0;
        long draw = 0;
        if (board != null) {
            draw += board.power();
            for (int i = 0; i < SIZE; i++) {
                if (i == SLOT_MOTHERBOARD || i == SLOT_PSU || !isSlotEnabled(i, parts.get(SLOT_MOTHERBOARD))) continue;
                HardwareStats part = statsIn(parts, i, slotType(i));
                if (part == null) continue;
                compute += part.computeBytesPerSecond();
                hashes += part.hashesPerSecond();
                storage += part.storage();
                draw += part.power();
            }
        }
        double cpuMining = compute / 1024.0 * 1000.0 * Config.safe(Config.CPU_MINING_FACTOR, 0.0);
        double effCompute = compute * Config.safe(Config.COMPUTE_MULTIPLIER, 1.0);
        double effHashes = (hashes + cpuMining) * Config.safe(Config.HASHRATE_MULTIPLIER, 1.0);
        long capacity = psu != null ? psu.power() : 0;

        ServerProblem problem;
        if (board == null) problem = ServerProblem.NO_MOTHERBOARD;
        else if (psu == null) problem = ServerProblem.NO_PSU;
        else if (draw > capacity && Config.safe(Config.ENFORCE_PSU_CAPACITY, true)) problem = ServerProblem.PSU_OVERLOADED;
        else problem = ServerProblem.NONE;

        return new ServerSpec(effCompute, effHashes, draw, capacity, psu != null ? psu.efficiency() : 1.0, storage, problem);
    }

    public boolean boots() {
        return problem == ServerProblem.NONE;
    }

    /** FE per tick this server needs while running. */
    public long energyPerTick() {
        return (long) Math.ceil(watts * Config.safe(Config.FE_PER_WATT, 0.1) / psuEfficiency);
    }
}
