package de.f4ked.datacenterextended.hardware;

import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.registry.ModDataMaps;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static de.f4ked.datacenterextended.hardware.ServerLayout.*;

/**
 * Effective stats of one server item, computed from its parts. This is the only place where component stats are
 * summed up; racks, the workbench screen and tooltips all use it.
 *
 * @param computeBps        effective compute in bytes/s (memory factor and global multiplier applied)
 * @param hashesPerSecond   effective hashrate in H/s (global multiplier applied)
 * @param watts             total power draw of all active parts
 * @param psuCapacity       watts the PSU delivers (0 = no PSU)
 * @param psuEfficiency     0..1
 * @param storageGb         total storage capacity of active drives
 * @param memoryGb          usable RAM in GB (capped at the motherboard's maximum)
 * @param recommendedMemory RAM in GB the installed CPUs want
 * @param memorySpeed       MT/s the memory runs at (slowest module)
 * @param memoryModules     number of active memory modules
 * @param memoryFactor      compute multiplier from RAM (see {@link MemoryModel})
 * @param problem           {@link ServerProblem#NONE} if the server can boot
 * @param worstCondition    lowest condition (0..1) of all parts, see {@link Wear}
 * @param asic              this is an ASIC miner (a rack bay item of its own, not a server)
 */
public record ServerSpec(double computeBps, double hashesPerSecond, long watts, long psuCapacity,
                         double psuEfficiency, double storageGb, double memoryGb, double recommendedMemory,
                         int memorySpeed, int memoryModules, double memoryFactor, ServerProblem problem,
                         double worstCondition, boolean asic) {

    public static final ServerSpec EMPTY = new ServerSpec(0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, ServerProblem.NO_MOTHERBOARD, 1, false);

    public static ServerSpec of(ItemStack server) {
        if (server.isEmpty()) return EMPTY;
        HardwareStats unit = ModDataMaps.hardware(server);
        if (unit != null && unit.type() == HardwareType.ASIC) return asic(server, unit);
        return of(readParts(server));
    }

    /** An ASIC miner: hashrate and power come from its own stats, it has a built-in power supply. */
    public static ServerSpec asic(ItemStack stack, HardwareStats stats) {
        double hashes = stats.hashesPerSecond() * Wear.performance(stack) * Config.safe(Config.HASHRATE_MULTIPLIER, 1.0);
        ServerProblem problem = Wear.isBroken(stack) ? ServerProblem.BROKEN_PART : ServerProblem.NONE;
        return new ServerSpec(0, hashes, stats.power(), stats.power(), stats.efficiency(), 0, 0, 0, 0, 0, 1, problem,
                Wear.condition(stack), true);
    }

    public static ServerSpec of(List<ItemStack> parts) {
        HardwareStats board = statsIn(parts, SLOT_MOTHERBOARD, HardwareType.MOTHERBOARD);
        HardwareStats psu = statsIn(parts, SLOT_PSU, HardwareType.PSU);
        ItemStack boardStack = parts.get(SLOT_MOTHERBOARD);

        double compute = 0, hashes = 0, storage = 0, memory = 0, recommended = 0;
        int speed = Integer.MAX_VALUE, modules = 0, cpus = 0;
        boolean badCpu = false, badMemory = false;
        boolean broken = Wear.isBroken(boardStack) || Wear.isBroken(parts.get(SLOT_PSU));
        long draw = 0;
        if (board != null) {
            draw += board.power();
            for (int i = 0; i < SIZE; i++) {
                if (i == SLOT_MOTHERBOARD || i == SLOT_PSU || !isSlotEnabled(i, boardStack)) continue;
                HardwareType type = slotType(i);
                HardwareStats part = statsIn(parts, i, type);
                if (part == null) continue;
                if (!isCompatible(i, parts.get(i), boardStack)) {
                    if (type == HardwareType.CPU) badCpu = true;
                    else badMemory = true;
                    continue;
                }
                if (Wear.isBroken(parts.get(i))) {
                    broken = true;
                    continue;
                }
                draw += part.power();
                switch (type) {
                    case CPU -> {
                        cpus++;
                        compute += part.computeBytesPerSecond() * Wear.performance(parts.get(i));
                        recommended += part.recommendedMemory();
                    }
                    case GPU -> hashes += part.hashesPerSecond() * Wear.performance(parts.get(i));
                    case STORAGE -> storage += part.storage();
                    case MEMORY -> {
                        modules++;
                        memory += part.memory();
                        if (part.speed() > 0) speed = Math.min(speed, part.speed());
                    }
                    default -> {
                    }
                }
            }
            if (board.maxMemory() > 0) memory = Math.min(memory, board.maxMemory());
        }
        if (speed == Integer.MAX_VALUE) speed = 0;

        double memFactor = MemoryModel.factor(memory, recommended, speed, modules,
                Config.safe(Config.MEMORY_STARVED_FACTOR, 0.25), Config.safe(Config.MEMORY_SPEED_BONUS, 0.03),
                Config.safe(Config.MEMORY_CHANNEL_BONUS, 0.10));
        double effCompute = compute * memFactor * Config.safe(Config.COMPUTE_MULTIPLIER, 1.0);
        double cpuMining = effCompute / 1024.0 * 1000.0 * Config.safe(Config.CPU_MINING_FACTOR, 0.0);
        double effHashes = (hashes + cpuMining) * Config.safe(Config.HASHRATE_MULTIPLIER, 1.0);
        long capacity = psu != null ? psu.power() : 0;

        ServerProblem problem;
        if (board == null) problem = ServerProblem.NO_MOTHERBOARD;
        else if (psu == null) problem = ServerProblem.NO_PSU;
        else if (broken) problem = ServerProblem.BROKEN_PART;
        else if (badCpu) problem = ServerProblem.INCOMPATIBLE_CPU;
        else if (badMemory) problem = ServerProblem.INCOMPATIBLE_MEMORY;
        else if (cpus == 0) problem = ServerProblem.NO_CPU;
        else if (modules == 0 && Config.safe(Config.REQUIRE_MEMORY, true)) problem = ServerProblem.NO_MEMORY;
        else if (draw > capacity && Config.safe(Config.ENFORCE_PSU_CAPACITY, true)) problem = ServerProblem.PSU_OVERLOADED;
        else problem = ServerProblem.NONE;

        return new ServerSpec(effCompute, effHashes, draw, capacity, psu != null ? psu.efficiency() : 1.0, storage,
                memory, recommended, speed, modules, memFactor, problem, Wear.worstCondition(parts), false);
    }

    public boolean boots() {
        return problem == ServerProblem.NONE;
    }

    /** FE per tick this server needs while running. */
    public long energyPerTick() {
        return energyPerTick(watts, psuEfficiency);
    }

    /** FE per tick for a draw in watts behind a PSU of the given efficiency. */
    public static long energyPerTick(long watts, double psuEfficiency) {
        if (watts <= 0) return 0;
        return (long) Math.ceil(watts * Config.safe(Config.FE_PER_WATT, 0.1) / Math.max(0.01, psuEfficiency));
    }
}
