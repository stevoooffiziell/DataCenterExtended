package de.f4ked.datacenterextended.client;

import de.f4ked.datacenterextended.Datacenterextended;
import de.f4ked.datacenterextended.hardware.DataUnits;
import de.f4ked.datacenterextended.hardware.HardwareStats;
import de.f4ked.datacenterextended.hardware.HardwareType;
import de.f4ked.datacenterextended.hardware.ServerLayout;
import de.f4ked.datacenterextended.hardware.ServerSpec;
import de.f4ked.datacenterextended.registry.ModDataMaps;
import de.f4ked.datacenterextended.registry.ModItems;
import de.f4ked.datacenterextended.registry.ModMenus;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@EventBusSubscriber(modid = Datacenterextended.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.SERVER_WORKBENCH.get(), ServerWorkbenchScreen::new);
        event.register(ModMenus.RACK.get(), RackScreen::new);
    }

    /** Adds server summaries, hardware stats and file sizes to tooltips – for ANY item with data map entries. */
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();

        if (stack.is(ModItems.SERVER.get())) {
            addServerTooltip(stack, tooltip);
            return;
        }

        HardwareStats hw = ModDataMaps.hardware(stack);
        if (hw != null) {
            tooltip.add(Component.translatable(hw.type().translationKey()).withStyle(ChatFormatting.GOLD));
            switch (hw.type()) {
                case MOTHERBOARD -> tooltip.add(stat("tooltip.datacenterextended.slots", hw.cpuSockets(), hw.gpuSlots(), hw.storageSlots()));
                case PSU -> {
                    tooltip.add(stat("tooltip.datacenterextended.capacity", hw.power()));
                    tooltip.add(stat("tooltip.datacenterextended.efficiency", Math.round(hw.efficiency() * 100)));
                }
                default -> {
                }
            }
            if (hw.compute() > 0) {
                tooltip.add(stat("tooltip.datacenterextended.compute", DataUnits.formatBytesPerSecond(hw.computeBytesPerSecond())));
            }
            if (hw.hashrate() > 0) {
                tooltip.add(stat("tooltip.datacenterextended.hashrate", DataUnits.formatHashrate(hw.hashesPerSecond())));
            }
            if (hw.storage() > 0) {
                tooltip.add(stat("tooltip.datacenterextended.storage", DataUnits.formatBytes(hw.storage() * DataUnits.GB)));
            }
            if (hw.type() != HardwareType.PSU && hw.power() > 0) {
                tooltip.add(stat("tooltip.datacenterextended.power", hw.power()));
            }
        }

        long size = ModDataMaps.listedFileSize(stack);
        if (size > 0) {
            tooltip.add(stat("tooltip.datacenterextended.file_size", DataUnits.formatBytes(size)));
        }
    }

    private static void addServerTooltip(ItemStack server, List<Component> tooltip) {
        List<ItemStack> parts = ServerLayout.readParts(server);
        if (parts.stream().allMatch(ItemStack::isEmpty)) {
            tooltip.add(stat("tooltip.datacenterextended.server.empty"));
            return;
        }
        ServerSpec spec = ServerSpec.of(parts);
        if (spec.boots()) {
            tooltip.add(Component.translatable("gui.datacenterextended.workbench.boots").withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable(spec.problem().translationKey()).withStyle(ChatFormatting.RED));
        }
        tooltip.add(stat("tooltip.datacenterextended.compute", DataUnits.formatBytesPerSecond(spec.computeBps())));
        tooltip.add(stat("tooltip.datacenterextended.hashrate", DataUnits.formatHashrate(spec.hashesPerSecond())));
        tooltip.add(stat("gui.datacenterextended.power", spec.watts(), spec.psuCapacity()));
        if (spec.storageGb() > 0) {
            tooltip.add(stat("tooltip.datacenterextended.storage", DataUnits.formatBytes(spec.storageGb() * DataUnits.GB)));
        }
        for (ItemStack part : parts) {
            if (!part.isEmpty()) {
                tooltip.add(Component.literal(" • ").append(part.getHoverName()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    private static Component stat(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.GRAY);
    }
}
