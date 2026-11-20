package de.f4ked.datacenterextended.registry;

import de.f4ked.datacenterextended.Datacenterextended;
import de.f4ked.datacenterextended.block.RackBlockEntity;
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

    private ModBlockEntities() {
    }
}
