package de.f4ked.datacenterextended.network;

import org.jspecify.annotations.Nullable;

/**
 * Snapshot of one compute network for a tick.
 *
 * @param bottleneck       slowest cable tier (defines the bandwidth), null for a network without cables
 * @param bandwidthLimited true if the bandwidth, not supply or demand, limited the transfer
 * @param truncated        true if the network exceeded {@code maxCablesPerNetwork} and was cut off
 */
public record NetworkInfo(int cables, int nodes, @Nullable CableTier bottleneck, long bandwidth,
                          long totalSupply, long totalDemand, long transferred, boolean bandwidthLimited, boolean truncated) {
}
