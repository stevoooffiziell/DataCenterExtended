package de.f4ked.datacenterextended.compat.jade;

import de.f4ked.datacenterextended.economy.Money;
import de.f4ked.datacenterextended.power.GeneratorStatus;
import de.f4ked.datacenterextended.power.PowerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

/** Client side of the Jade integration for generators, batteries and grid connections. */
public enum PowerComponentProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public Identifier getUid() {
        return PowerDataProvider.UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("kind")) return;
        int kind = data.getIntOr("kind", 0);
        if (kind == PowerBlockEntity.KIND_GENERATOR) {
            GeneratorStatus status = GeneratorStatus.byId(data.getIntOr("status", 0));
            ChatFormatting color = !status.producing() ? ChatFormatting.RED : status.warning() ? ChatFormatting.YELLOW : ChatFormatting.GREEN;
            tooltip.add(Component.translatable(status.translationKey()).withStyle(color));
            tooltip.add(Component.translatable("jade.datacenterextended.generator.output",
                    fe(data.getDoubleOr("output", 0)), fe(data.getDoubleOr("peak", 0))).withStyle(ChatFormatting.AQUA));
            if (data.getBooleanOr("nuclear", false)) {
                tooltip.add(gray("gui.datacenterextended.power.core_heat").append(": " + Math.round(data.getDoubleOr("heat", 0) * 100) + " %"));
            }
        } else if (kind == PowerBlockEntity.KIND_BATTERY) {
            double flow = data.getDoubleOr("flow", 0);
            tooltip.add(Component.translatable("jade.datacenterextended.battery.flow", (flow >= 0 ? "+" : "") + fe(flow))
                    .withStyle(flow > 0.5 ? ChatFormatting.GREEN : flow < -0.5 ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("jade.datacenterextended.grid.export",
                    fe(data.getDoubleOr("output", 0)), fe(data.getDoubleOr("peak", 0))).withStyle(ChatFormatting.GREEN));
            tooltip.add(gray("gui.datacenterextended.power.earned").append(": " + Money.format(data.getLongOr("earned", 0))));
        }
        tooltip.add(Component.translatable("gui.datacenterextended.energy.stored",
                String.format(Locale.ROOT, "%,d", data.getLongOr("stored", 0)),
                String.format(Locale.ROOT, "%,d", data.getLongOr("capacity", 0))).withStyle(ChatFormatting.RED));
        String owner = data.getStringOr("owner", "");
        if (!owner.isEmpty()) tooltip.add(Component.translatable("jade.datacenterextended.owner", owner).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static net.minecraft.network.chat.MutableComponent gray(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.GRAY);
    }

    private static String fe(double value) {
        return Math.abs(value) >= 100 ? String.format(Locale.ROOT, "%,d", Math.round(value)) : String.format(Locale.ROOT, "%.1f", value);
    }
}
