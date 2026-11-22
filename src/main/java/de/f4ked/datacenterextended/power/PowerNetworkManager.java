package de.f4ked.datacenterextended.power;

import de.f4ked.datacenterextended.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Builds and runs the power networks of one level (same approach as the compute networks: walk the cables next to
 * every registered generator, battery and grid connection; rebuild lazily when something changed).
 * <p>
 * Every tick, after all block entities ticked, each network moves FE in priority order, limited by its weakest
 * cable: generators → consumers, batteries → consumers, generators → batteries, generators → grid connections.
 * Consumers are all other blocks that accept FE (racks, other mods' machines).
 */
public final class PowerNetworkManager {
    private static final Map<ServerLevel, PowerNetworkManager> MANAGERS = new WeakHashMap<>();

    private final Set<BlockPos> nodePositions = new HashSet<>();
    private final List<Network> networks = new ArrayList<>();
    private boolean dirty = true;

    /** Snapshot of a network for the multimeter and Jade. FE values are per tick. */
    public record PowerInfo(int cables, int generators, int batteries, int grids, int consumers,
                            @Nullable PowerCableTier bottleneck, long transferLimit, double production,
                            long toConsumers, long toBatteries, long toGrid, long fromBatteries,
                            long stored, long capacity, boolean truncated) {
        public boolean limited() {
            return transferLimit > 0 && toConsumers + toBatteries + toGrid >= transferLimit;
        }
    }

    private PowerNetworkManager() {
    }

    public static PowerNetworkManager get(ServerLevel level) {
        return MANAGERS.computeIfAbsent(level, l -> new PowerNetworkManager());
    }

    public static void markDirty(ServerLevel level) {
        get(level).dirty = true;
    }

    public static void registerNode(ServerLevel level, BlockPos pos) {
        PowerNetworkManager manager = get(level);
        manager.nodePositions.add(pos.immutable());
        manager.dirty = true;
    }

    public static void unregisterNode(ServerLevel level, BlockPos pos) {
        PowerNetworkManager manager = get(level);
        manager.nodePositions.remove(pos);
        manager.dirty = true;
    }

    // ================================================================== ticking

    public void tick(ServerLevel level) {
        if (dirty) {
            rebuild(level);
            dirty = false;
        }
        double multiplier = Config.safe(Config.CABLE_TRANSFER, 1.0);
        for (Network net : networks) {
            long limit = net.bottleneck == null ? 0 : (long) Math.max(1, net.bottleneck.transfer() * multiplier);
            List<EnergyHandler> gens = resolve(net.generators);
            List<EnergyHandler> batteries = resolve(net.batteries);
            List<EnergyHandler> grids = resolve(net.grids);
            List<EnergyHandler> consumers = resolve(net.consumers);
            int rotation = (int) (level.getGameTime() % 64);

            long toConsumers = distribute(gens, consumers, limit, rotation);
            long fromBatteries = distribute(batteries, consumers, limit - toConsumers, rotation);
            long toBatteries = distribute(gens, batteries, limit - toConsumers - fromBatteries, rotation);
            long toGrid = distribute(gens, grids, limit - toConsumers - fromBatteries - toBatteries, rotation);

            double production = 0;
            for (BlockPos pos : net.generators.keySet()) {
                if (level.getBlockEntity(pos) instanceof GeneratorBlockEntity gen) production += gen.output();
            }
            long stored = 0, capacity = 0;
            for (EnergyHandler battery : batteries) {
                stored += battery.getAmountAsLong();
                capacity += battery.getCapacityAsLong();
            }
            net.lastInfo = new PowerInfo(net.cables.size(), net.generators.size(), net.batteries.size(), net.grids.size(),
                    net.consumers.size(), net.bottleneck, limit, production, toConsumers + fromBatteries, toBatteries, toGrid,
                    fromBatteries, stored, capacity, net.truncated);
        }
    }

    private static List<EnergyHandler> resolve(Map<BlockPos, BlockCapabilityCache<EnergyHandler, Direction>> caches) {
        List<EnergyHandler> handlers = new ArrayList<>(caches.size());
        for (BlockCapabilityCache<EnergyHandler, Direction> cache : caches.values()) {
            EnergyHandler handler = cache.getCapability();
            if (handler != null) handlers.add(handler);
        }
        return handlers;
    }

    /**
     * Moves up to {@code limit} FE from the sources to the targets, sharing it evenly between the targets (whatever a
     * target doesn't take is offered to the next ones).
     */
    static long distribute(List<EnergyHandler> sources, List<EnergyHandler> targets, long limit, int rotation) {
        if (limit <= 0 || sources.isEmpty() || targets.isEmpty()) return 0;
        long moved = 0;
        int n = targets.size();
        for (int k = 0; k < n && moved < limit; k++) {
            EnergyHandler target = targets.get((k + rotation) % n);
            long share = Math.max(1, (limit - moved) / (n - k));
            long got = 0;
            for (EnergyHandler source : sources) {
                if (got >= share) break;
                if (source == target) continue;
                got += EnergyHandlerUtil.move(source, target, (int) Math.min(Integer.MAX_VALUE, share - got), null);
            }
            moved += got;
        }
        return moved;
    }

    private void rebuild(ServerLevel level) {
        networks.clear();
        Set<BlockPos> visited = new HashSet<>();
        int maxCables = Config.safe(Config.MAX_POWER_CABLES, 4096);
        nodePositions.removeIf(p -> !level.isLoaded(p));
        for (BlockPos nodePos : nodePositions) {
            for (Direction dir : Direction.values()) {
                BlockPos start = nodePos.relative(dir);
                if (!visited.contains(start) && level.isLoaded(start) && isCable(level.getBlockState(start))) {
                    networks.add(walk(level, start, visited, maxCables));
                }
            }
        }
    }

    private static Network walk(ServerLevel level, BlockPos start, Set<BlockPos> visited, int maxCables) {
        Network net = new Network();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        addCable(net, level.getBlockState(start), start);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (!level.isLoaded(next)) continue;
                BlockState state = level.getBlockState(next);
                if (isCable(state)) {
                    if (visited.contains(next)) continue;
                    if (net.cables.size() >= maxCables) {
                        net.truncated = true;
                        continue;
                    }
                    visited.add(next);
                    addCable(net, state, next);
                    queue.add(next);
                } else {
                    addEndpoint(level, net, next.immutable(), dir.getOpposite());
                }
            }
        }
        return net;
    }

    private static void addEndpoint(ServerLevel level, Network net, BlockPos pos, Direction side) {
        if (net.contains(pos) || level.getCapability(Capabilities.Energy.BLOCK, pos, side) == null) return;
        BlockCapabilityCache<EnergyHandler, Direction> cache = BlockCapabilityCache.create(Capabilities.Energy.BLOCK, level, pos, side);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof GeneratorBlockEntity) net.generators.put(pos, cache);
        else if (be instanceof BatteryBlockEntity) net.batteries.put(pos, cache);
        else if (be instanceof GridConnectionBlockEntity) net.grids.put(pos, cache);
        else net.consumers.put(pos, cache);
    }

    private static void addCable(Network net, BlockState state, BlockPos pos) {
        net.cables.add(pos.immutable());
        PowerCableTier tier = ((PowerCableBlock) state.getBlock()).tier();
        if (net.bottleneck == null || tier.transfer() < net.bottleneck.transfer()) net.bottleneck = tier;
    }

    private static boolean isCable(BlockState state) {
        return state.getBlock() instanceof PowerCableBlock;
    }

    // ================================================================== queries

    /** The network containing the given cable or endpoint, or null. */
    public @Nullable PowerInfo describe(ServerLevel level, BlockPos pos) {
        if (dirty) {
            rebuild(level);
            dirty = false;
        }
        for (Network net : networks) {
            if (net.cables.contains(pos) || net.contains(pos)) {
                return net.lastInfo != null ? net.lastInfo
                        : new PowerInfo(net.cables.size(), net.generators.size(), net.batteries.size(), net.grids.size(),
                        net.consumers.size(), net.bottleneck, 0, 0, 0, 0, 0, 0, 0, 0, net.truncated);
            }
        }
        return null;
    }

    private static final class Network {
        final Set<BlockPos> cables = new HashSet<>();
        final Map<BlockPos, BlockCapabilityCache<EnergyHandler, Direction>> generators = new LinkedHashMap<>();
        final Map<BlockPos, BlockCapabilityCache<EnergyHandler, Direction>> batteries = new LinkedHashMap<>();
        final Map<BlockPos, BlockCapabilityCache<EnergyHandler, Direction>> grids = new LinkedHashMap<>();
        final Map<BlockPos, BlockCapabilityCache<EnergyHandler, Direction>> consumers = new LinkedHashMap<>();
        @Nullable PowerCableTier bottleneck;
        boolean truncated;
        @Nullable PowerInfo lastInfo;

        boolean contains(BlockPos pos) {
            return generators.containsKey(pos) || batteries.containsKey(pos) || grids.containsKey(pos) || consumers.containsKey(pos);
        }
    }
}
