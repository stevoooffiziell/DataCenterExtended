package de.f4ked.datacenterextended.registry;

import de.f4ked.datacenterextended.Datacenterextended;
import de.f4ked.datacenterextended.power.GeneratorType;
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

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ENERGY = CREATIVE_MODE_TABS.register("energy",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.datacenterextended.energy"))
                    .icon(() -> ModItems.GENERATORS.get(GeneratorType.MONOCRYSTALLINE_SOLAR).get().getDefaultInstance())
                    .withTabsBefore(MAIN.getId())
                    .displayItems((params, output) -> ModItems.ENERGY.forEach(item -> output.accept(item.get())))
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MATERIALS = CREATIVE_MODE_TABS.register("materials",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.datacenterextended.materials"))
                    .icon(() -> ModItems.MATERIAL_ITEMS.get("aluminium_ingot").get().getDefaultInstance())
                    .withTabsBefore(ENERGY.getId())
                    .displayItems((params, output) -> ModItems.MATERIALS.forEach(item -> output.accept(item.get())))
                    .build());

    private ModCreativeTabs() {
    }
}
