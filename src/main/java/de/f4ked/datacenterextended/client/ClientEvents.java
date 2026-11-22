package de.f4ked.datacenterextended.client;

import de.f4ked.datacenterextended.Datacenterextended;
import de.f4ked.datacenterextended.hardware.DataUnits;
import de.f4ked.datacenterextended.hardware.HardwareStats;
import de.f4ked.datacenterextended.hardware.HardwareType;
import de.f4ked.datacenterextended.hardware.ServerLayout;
import de.f4ked.datacenterextended.hardware.ServerSpec;
import de.f4ked.datacenterextended.hardware.Wear;
import de.f4ked.datacenterextended.economy.ShopPricing;
import de.f4ked.datacenterextended.item.PrebuiltServerItem;
import de.f4ked.datacenterextended.time.Calendar;
import de.f4ked.datacenterextended.heat.CoolingStats;
import net.minecraft.network.chat.MutableComponent;
import de.f4ked.datacenterextended.registry.ModDataMaps;
import de.f4ked.datacenterextended.registry.ModItems;
import de.f4ked.datacenterextended.registry.ModMenus;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import de.f4ked.datacenterextended.registry.ModBlockEntities;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.power.BatteryBlock;
import de.f4ked.datacenterextended.power.GeneratorBlock;
import de.f4ked.datacenterextended.power.GeneratorType;
import de.f4ked.datacenterextended.power.GridConnectionBlock;
import de.f4ked.datacenterextended.power.PowerCableBlock;
import de.f4ked.datacenterextended.registry.ModDataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

import java.util.List;

@EventBusSubscriber(modid = Datacenterextended.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SERVER_MONITOR.get(), ServerMonitorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.GENERATOR.get(), WindTurbineRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.SERVER_WORKBENCH.get(), ServerWorkbenchScreen::new);
        event.register(ModMenus.RACK.get(), RackScreen::new);
        event.register(ModMenus.POWER.get(), PowerScreen::new);
    }

    /** Adds server summaries, hardware stats and file sizes to tooltips – for ANY item with data map entries. */
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();

        if (stack.getItem() instanceof PrebuiltServerItem prebuilt) {
            tooltip.add(Component.translatable("tooltip.datacenterextended.prebuilt").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.datacenterextended.prebuilt." + prebuilt.prebuilt().kind().name().toLowerCase(java.util.Locale.ROOT))
                    .withStyle(ChatFormatting.GRAY));
            addReleaseDate(stack, tooltip);
            addServerTooltip(prebuilt.prebuilt().assemble(), tooltip);
            return;
        }
        if (stack.is(ModItems.SERVER.get())) {
            addServerTooltip(stack, tooltip);
            return;
        }
        addReleaseDate(stack, tooltip);
        addPowerTooltip(stack, tooltip);

        CoolingStats cooling = ModDataMaps.cooling(stack);
        if (cooling != null) {
            tooltip.add(Component.translatable("tooltip.datacenterextended.cooling").withStyle(ChatFormatting.GOLD));
            tooltip.add(stat("tooltip.datacenterextended.cooling_capacity", DataUnits.formatWatts(cooling.capacity())));
            if (cooling.power() > 0) tooltip.add(stat("tooltip.datacenterextended.power", cooling.power()));
        }

        HardwareStats hw = ModDataMaps.hardware(stack);
        if (hw != null) {
            tooltip.add(Component.translatable(hw.type().translationKey()).withStyle(ChatFormatting.GOLD));
            addCondition(stack, tooltip);
            switch (hw.type()) {
                case MOTHERBOARD -> {
                    tooltip.add(HardwareText.boardRequirements(hw).withStyle(ChatFormatting.GRAY));
                    tooltip.add(stat("tooltip.datacenterextended.slots", hw.cpuSockets(), hw.gpuSlots(), hw.memorySlots(), hw.storageSlots()));
                }
                case CPU -> {
                    if (!hw.socket().isEmpty()) tooltip.add(stat("tooltip.datacenterextended.socket", HardwareText.socket(hw.socket())));
                    if (hw.recommendedMemory() > 0) {
                        tooltip.add(stat("tooltip.datacenterextended.recommended_memory", HardwareText.gb(hw.recommendedMemory())));
                    }
                }
                case MEMORY -> tooltip.add(stat("tooltip.datacenterextended.memory_module", HardwareText.gb(hw.memory()),
                        hw.memoryType().isEmpty() ? "RAM" : HardwareText.memoryType(hw.memoryType()), hw.speed()));
                case PSU -> {
                    tooltip.add(stat("tooltip.datacenterextended.capacity", hw.power()));
                    tooltip.add(stat("tooltip.datacenterextended.efficiency", Math.round(hw.efficiency() * 100)));
                }
                case ASIC -> tooltip.add(Component.translatable("tooltip.datacenterextended.asic").withStyle(ChatFormatting.YELLOW));
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
            if (hw.type() == HardwareType.ASIC) {
                tooltip.add(stat("tooltip.datacenterextended.psu_fe", DataUnits.formatNumber(ServerSpec.energyPerTick(hw.power(), hw.efficiency()))));
            }
            if (hw.type() == HardwareType.PSU) {
                tooltip.add(stat("tooltip.datacenterextended.psu_fe", DataUnits.formatNumber(ServerSpec.energyPerTick(hw.power(), hw.efficiency()))));
            }
        }

        long size = ModDataMaps.listedFileSize(stack);
        if (size > 0) {
            tooltip.add(stat("tooltip.datacenterextended.file_size", DataUnits.formatBytes(size)));
        }
    }

    /** Output, storage and transfer stats of generators, batteries, power cables and grid connections. */
    private static void addPowerTooltip(ItemStack stack, List<Component> tooltip) {
        if (stack.is(ModItems.FUEL_ROD.get())) {
            float used = stack.getOrDefault(ModDataComponents.WEAR.get(), 0.0F);
            tooltip.add(stat("tooltip.datacenterextended.fuel_rod", Math.round((1 - used) * 100)));
            return;
        }
        if (!(stack.getItem() instanceof BlockItem blockItem)) return;
        Block block = blockItem.getBlock();
        if (block instanceof GeneratorBlock generator) {
            GeneratorType type = generator.type();
            tooltip.add(Component.translatable(type.family().translationKey()).append(" · ")
                    .append(Component.translatable("gui.datacenterextended.power.tier", type.tier())).withStyle(ChatFormatting.GOLD));
            double peak = type.peak() * Config.outputMultiplier(type.family());
            tooltip.add(stat("tooltip.datacenterextended.generator.peak", PowerScreen.fe(peak), PowerScreen.watts(peak)));
            tooltip.add(Component.translatable("tooltip.datacenterextended.generator." + type.id()).withStyle(ChatFormatting.DARK_GRAY));
        } else if (block instanceof BatteryBlock battery) {
            int stored = stack.getOrDefault(ModDataComponents.STORED_ENERGY.get(), 0);
            tooltip.add(stat("tooltip.datacenterextended.battery.stored",
                    String.format(java.util.Locale.ROOT, "%,d", stored), String.format(java.util.Locale.ROOT, "%,d", battery.tier().capacity())));
            tooltip.add(stat("tooltip.datacenterextended.battery.rate", String.format(java.util.Locale.ROOT, "%,d", battery.tier().rate())));
        } else if (block instanceof PowerCableBlock cable) {
            tooltip.add(stat("tooltip.datacenterextended.power_cable", String.format(java.util.Locale.ROOT, "%,d",
                    Math.round(cable.tier().transfer() * Config.safe(Config.CABLE_TRANSFER, 1.0)))));
        } else if (block instanceof GridConnectionBlock grid) {
            tooltip.add(stat("tooltip.datacenterextended.grid", PowerScreen.watts(grid.tier().rate())));
            tooltip.add(Component.translatable("tooltip.datacenterextended.grid.hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static void addServerTooltip(ItemStack server, List<Component> tooltip) {
        List<ItemStack> parts = ServerLayout.readParts(server);
        if (parts.stream().allMatch(ItemStack::isEmpty)) {
            tooltip.add(stat("tooltip.datacenterextended.server.empty"));
            return;
        }
        ServerSpec spec = ServerSpec.of(parts);
        if (spec.worstCondition() < 1.0) {
            tooltip.add(Component.translatable("tooltip.datacenterextended.worst_condition", Math.round(spec.worstCondition() * 100))
                    .withStyle(conditionStyle(spec.worstCondition())));
        }
        if (spec.boots()) {
            tooltip.add(Component.translatable("gui.datacenterextended.workbench.boots").withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable(spec.problem().translationKey()).withStyle(ChatFormatting.RED));
        }
        tooltip.add(stat("tooltip.datacenterextended.compute", DataUnits.formatBytesPerSecond(spec.computeBps())));
        tooltip.add(stat("tooltip.datacenterextended.hashrate", DataUnits.formatHashrate(spec.hashesPerSecond())));
        HardwareStats board = ServerLayout.statsIn(parts, ServerLayout.SLOT_MOTHERBOARD, HardwareType.MOTHERBOARD);
        tooltip.add(HardwareText.memoryLine(spec, board != null ? board.memoryType() : "").withStyle(ChatFormatting.GRAY));
        tooltip.add(stat("gui.datacenterextended.power_fe", spec.watts(), spec.psuCapacity(), spec.energyPerTick()));
        if (spec.storageGb() > 0) {
            tooltip.add(stat("tooltip.datacenterextended.storage", DataUnits.formatBytes(spec.storageGb() * DataUnits.GB)));
        }
        for (ItemStack part : parts) {
            if (!part.isEmpty()) {
                MutableComponent line = Component.literal(" • ").append(part.getHoverName());
                if (Wear.condition(part) < 1.0) line.append(" (" + Math.round(Wear.condition(part) * 100) + "%)");
                tooltip.add(line.withStyle(Wear.isBroken(part) ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
            }
        }
    }

    /** "Condition 73%" for worn parts (new parts show nothing). */
    private static void addCondition(ItemStack stack, List<Component> tooltip) {
        double condition = Wear.condition(stack);
        if (condition >= 1.0) return;
        String key = Wear.isBroken(stack) ? "tooltip.datacenterextended.broken" : "tooltip.datacenterextended.condition";
        tooltip.add(Component.translatable(key, Math.round(condition * 100)).withStyle(conditionStyle(condition)));
    }

    private static ChatFormatting conditionStyle(double condition) {
        if (condition > 0.6) return ChatFormatting.GREEN;
        if (condition > 0.25) return ChatFormatting.YELLOW;
        return ChatFormatting.RED;
    }

    private static void addReleaseDate(ItemStack stack, List<Component> tooltip) {
        java.time.LocalDate date = ShopPricing.releaseDate(stack.getItem());
        if (date == null) return;
        boolean released = ShopPricing.released(stack.getItem(), ClientEconomy.epochDay());
        tooltip.add(Component.translatable("tooltip.datacenterextended.released", Calendar.format(date))
                .withStyle(released ? ChatFormatting.DARK_GRAY : ChatFormatting.YELLOW));
    }

    private static Component stat(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.GRAY);
    }
}
