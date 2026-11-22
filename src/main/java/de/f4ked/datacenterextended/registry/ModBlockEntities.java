package de.f4ked.datacenterextended.registry;

import de.f4ked.datacenterextended.Datacenterextended;
import de.f4ked.datacenterextended.block.RackBlockEntity;
import de.f4ked.datacenterextended.block.ServerMonitorBlockEntity;
import de.f4ked.datacenterextended.power.BatteryBlockEntity;
import de.f4ked.datacenterextended.power.GeneratorBlockEntity;
import de.f4ked.datacenterextended.power.GridConnectionBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Datacenterextended.MODID);

    /** Shared by the mining, hosting and fabricating rack blocks. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RackBlockEntity>> RACK =
            BLOCK_ENTITY_TYPES.register("rack", () -> new BlockEntityType<>(RackBlockEntity::new,
                    ModBlocks.RACKS.values().stream().map(b -> (Block) b.get()).toArray(Block[]::new)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ServerMonitorBlockEntity>> SERVER_MONITOR =
            BLOCK_ENTITY_TYPES.register("server_monitor", () -> new BlockEntityType<>(ServerMonitorBlockEntity::new, ModBlocks.SERVER_MONITOR.get()));

    /** Shared by all generator blocks (the type comes from the block). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeneratorBlockEntity>> GENERATOR =
            BLOCK_ENTITY_TYPES.register("generator", () -> new BlockEntityType<>(GeneratorBlockEntity::new,
                    ModBlocks.GENERATORS.values().stream().map(b -> (Block) b.get()).toArray(Block[]::new)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryBlockEntity>> BATTERY =
            BLOCK_ENTITY_TYPES.register("battery", () -> new BlockEntityType<>(BatteryBlockEntity::new,
                    ModBlocks.BATTERIES.values().stream().map(b -> (Block) b.get()).toArray(Block[]::new)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GridConnectionBlockEntity>> GRID_CONNECTION =
            BLOCK_ENTITY_TYPES.register("grid_connection", () -> new BlockEntityType<>(GridConnectionBlockEntity::new,
                    ModBlocks.GRID_CONNECTIONS.values().stream().map(b -> (Block) b.get()).toArray(Block[]::new)));

    private ModBlockEntities() {
    }
}
