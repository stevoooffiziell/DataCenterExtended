package de.f4ked.datacenterextended.network;

import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.block.DataCableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
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
 * Builds and runs the compute networks of one level.
 * <p>
 * Networks are discovered by walking the data cables next to every registered node (racks register themselves while
 * loaded). Any change to cables, racks or loaded chunks marks the manager dirty and the networks are rebuilt lazily at
 * the end of the next level tick. Each network's bandwidth is the capacity of its slowest cable.
 */
public final class ComputeNetworkManager {
    private static final Map<ServerLevel, ComputeNetworkManager> MANAGERS = new WeakHashMap<>();

    private final Set<BlockPos> nodePositions = new HashSet<>();
    private final List<Network> networks = new ArrayList<>();
    private boolean dirty = true;

    private ComputeNetworkManager() {
    }

    public static ComputeNetworkManager get(ServerLevel level) {
        return MANAGERS.computeIfAbsent(level, l -> new ComputeNetworkManager());
    }

    public static void markDirty(ServerLevel level) {
        get(level).dirty = true;
    }

    /** Registers a block position whose {@link ComputeNode} capability should be considered (racks, add-on blocks). */
    public static void registerNode(ServerLevel level, BlockPos pos) {
        ComputeNetworkManager manager = get(level);
        manager.nodePositions.add(pos.immutable());
        manager.dirty = true;
    }

    public static void unregisterNode(ServerLevel level, BlockPos pos) {
        ComputeNetworkManager manager = get(level);
        manager.nodePositions.remove(pos);
        manager.dirty = true;
    }

    // ================================================================== ticking

    public void tick(ServerLevel level) {
        if (dirty) {
            rebuild(level);
            dirty = false;
        }
        long gameTime = level.getGameTime();
        for (Network net : networks) {
            int n = net.nodes.size();
            long[] supply = new long[n];
            long[] demand = new long[n];
            for (int i = 0; i < n; i++) {
                supply[i] = Math.max(0, net.nodes.get(i).supply());
                demand[i] = Math.max(0, net.nodes.get(i).demand());
            }
            long bandwidth = net.bottleneck == null ? 0 : net.bottleneck.capacity();
            ComputeAllocator.Result result = ComputeAllocator.allocate(supply, demand, bandwidth);
            NetworkInfo info = new NetworkInfo(net.cables.size(), n, net.bottleneck, bandwidth,
                    sum(supply), sum(demand), result.transferred(), result.bandwidthLimited(), net.truncated);
            net.lastInfo = info;
            for (int i = 0; i < n; i++) {
                net.nodes.get(i).applyAllocation(result.received()[i], result.exported()[i], info, gameTime);
            }
        }
    }

    private void rebuild(ServerLevel level) {
        networks.clear();
        Set<BlockPos> visited = new HashSet<>();
        int maxCables = Config.safe(Config.MAX_CABLES_PER_NETWORK, 4096);
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

    /** Breadth-first walk over connected cables, collecting the nodes attached to them. */
    private static Network walk(ServerLevel level, BlockPos start, Set<BlockPos> visited, int maxCables) {
        Network net = new Network();
        Map<Object, ComputeNode> nodes = new LinkedHashMap<>();
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
                    ComputeNode node = level.getCapability(ComputeNode.CAPABILITY, next, dir.getOpposite());
                    if (node != null) {
                        nodes.putIfAbsent(node.nodeKey(), node);
                        net.nodePositions.add(next.immutable());
                    }
                }
            }
        }
        net.nodes.addAll(nodes.values());
        return net;
    }

    private static void addCable(Network net, BlockState state, BlockPos pos) {
        net.cables.add(pos.immutable());
        CableTier tier = ((DataCableBlock) state.getBlock()).tier();
        if (net.bottleneck == null || tier.capacity() < net.bottleneck.capacity()) {
            net.bottleneck = tier;
        }
    }

    private static boolean isCable(BlockState state) {
        return state.getBlock() instanceof DataCableBlock;
    }

    private static long sum(long[] values) {
        long total = 0;
        for (long v : values) total += v;
        return total;
    }

    // ================================================================== queries

    /** The info of the network containing the given cable or node position, or null. */
    public @Nullable NetworkInfo describe(ServerLevel level, BlockPos pos) {
        if (dirty) {
            rebuild(level);
            dirty = false;
        }
        for (Network net : networks) {
            if (net.cables.contains(pos) || net.nodePositions.contains(pos)) {
                return net.lastInfo != null ? net.lastInfo
                        : new NetworkInfo(net.cables.size(), net.nodes.size(), net.bottleneck,
                        net.bottleneck == null ? 0 : net.bottleneck.capacity(), 0, 0, 0, false, net.truncated);
            }
        }
        return null;
    }

    private static final class Network {
        final List<ComputeNode> nodes = new ArrayList<>();
        final Set<BlockPos> cables = new HashSet<>();
        final Set<BlockPos> nodePositions = new HashSet<>();
        @Nullable CableTier bottleneck;
        boolean truncated;
        @Nullable NetworkInfo lastInfo;
    }
}
