package de.f4ked.datacenterextended.registry;

import de.f4ked.datacenterextended.Datacenterextended;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Datacenterextended.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.datacenterextended"))
                    .icon(() -> ModItems.SERVER.get().getDefaultInstance())
                    .displayItems((params, output) -> ModItems.ALL.forEach(item -> output.accept(item.get())))
                    .build());

    private ModCreativeTabs() {
    }
}
