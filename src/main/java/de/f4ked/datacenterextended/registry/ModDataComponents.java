package de.f4ked.datacenterextended.registry;

import de.f4ked.datacenterextended.Datacenterextended;
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

    private ModDataComponents() {
    }
}
