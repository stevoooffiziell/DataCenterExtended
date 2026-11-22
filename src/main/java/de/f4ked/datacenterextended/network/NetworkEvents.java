package de.f4ked.datacenterextended.network;

import de.f4ked.datacenterextended.Datacenterextended;
import de.f4ked.datacenterextended.power.PowerNetworkManager;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Datacenterextended.MODID)
public final class NetworkEvents {
    private NetworkEvents() {
    }

    /** Runs after all block entities ticked, so racks have published this tick's supply and demand. */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ComputeNetworkManager.get(level).tick(level);
            PowerNetworkManager.get(level).tick(level);
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ComputeNetworkManager.markDirty(level);
            PowerNetworkManager.markDirty(level);
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ComputeNetworkManager.markDirty(level);
            PowerNetworkManager.markDirty(level);
        }
    }
}
