package de.f4ked.datacenterextended.network;

import de.f4ked.datacenterextended.Datacenterextended;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jspecify.annotations.Nullable;

/**
 * Something that takes part in a compute network. Exposed as a block capability so other mods can add producers or
 * consumers: a block whose capability is non-null on a face is connected by an adjacent data cable.
 * <p>
 * Every tick the network asks each node for its {@link #supply()} (spare compute it can give away) and
 * {@link #demand()} (compute it could still use), splits the transfer and reports back via {@link #applyAllocation}.
 */
public interface ComputeNode {
    BlockCapability<ComputeNode, @Nullable Direction> CAPABILITY =
            BlockCapability.createSided(Datacenterextended.id("compute_node"), ComputeNode.class);

    /** Spare compute in bytes/s this node offers to the network. */
    long supply();

    /** Compute in bytes/s this node would still like to receive. */
    long demand();

    /** Identity used to de-duplicate nodes reached through several blocks (e.g. merged racks). */
    default Object nodeKey() {
        return this;
    }

    /**
     * Result of this tick's allocation.
     *
     * @param received bytes/s this node receives
     * @param exported bytes/s taken from this node's supply
     */
    void applyAllocation(long received, long exported, NetworkInfo info, long gameTime);
}
