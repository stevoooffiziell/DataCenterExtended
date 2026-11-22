package de.f4ked.datacenterextended.registry;

import com.mojang.serialization.Codec;
import de.f4ked.datacenterextended.Datacenterextended;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Datacenterextended.MODID);

    /** The parts installed in a server item (layout: {@link de.f4ked.datacenterextended.hardware.ServerLayout}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> SERVER_PARTS =
            DATA_COMPONENTS.registerComponentType("server_parts",
                    b -> b.persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC));

    /** Wear of a hardware part: 0 = new, 1 = broken (see {@link de.f4ked.datacenterextended.hardware.Wear}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> WEAR =
            DATA_COMPONENTS.registerComponentType("wear",
                    b -> b.persistent(Codec.floatRange(0.0F, 1.0F)).networkSynchronized(ByteBufCodecs.FLOAT));

    /** FE stored in a battery bank item (kept when the block is broken). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STORED_ENERGY =
            DATA_COMPONENTS.registerComponentType("stored_energy",
                    b -> b.persistent(Codec.intRange(0, Integer.MAX_VALUE)).networkSynchronized(ByteBufCodecs.VAR_INT));

    private ModDataComponents() {
    }
}
