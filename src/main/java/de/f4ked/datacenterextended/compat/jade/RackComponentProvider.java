package de.f4ked.datacenterextended.compat.jade;

import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.block.RackStatus;
import de.f4ked.datacenterextended.block.RackType;
import de.f4ked.datacenterextended.economy.Coin;
import de.f4ked.datacenterextended.economy.Money;
import de.f4ked.datacenterextended.hardware.DataUnits;
import de.f4ked.datacenterextended.hosting.HostingPlan;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

/** Client side of the Jade integration: turns the data from {@link RackDataProvider} into tooltip lines. */
public enum RackComponentProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public Identifier getUid() {
        return RackDataProvider.UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("status")) return;
        RackStatus status = RackStatus.byId(data.getIntOr("status", 0));
        RackType type = RackType.byId(data.getIntOr("type", 0));
        ChatFormatting statusColor = !status.ok() ? ChatFormatting.RED : status.warning() ? ChatFormatting.YELLOW : ChatFormatting.GREEN;
        tooltip.add(Component.translatable(status.translationKey()).withStyle(statusColor));

        double income = data.getDoubleOr("income", 0);
        double rate = data.getDoubleOr("rate", 0);
        switch (type) {
            case MINING -> {
                Coin coin = Coin.byId(data.getIntOr("coin", 0));
                tooltip.add(Component.translatable("jade.datacenterextended.income", Money.formatCoin(income, coin)).withColor(coin.color()));
                tooltip.add(gray("gui.datacenterextended.rack.hashrate", DataUnits.formatHashrate(rate)));
            }
            case HOSTING -> {
                tooltip.add(Component.translatable("jade.datacenterextended.income", Money.format(Math.round(income)))
                        .withStyle(ChatFormatting.GREEN));
                StringBuilder plans = new StringBuilder();
                for (HostingPlan plan : HostingPlan.values()) {
                    if (!plans.isEmpty()) plans.append(" ");
                    plans.append(plan.shortName()).append(data.getIntOr("plan" + plan.ordinal(), 0));
                }
                tooltip.add(gray("jade.datacenterextended.customers", plans.toString(),
                        Math.round(data.getDoubleOr("satisfaction", 1) * 100)));
            }
            case FABRICATING -> tooltip.add(gray("jade.datacenterextended.compute", DataUnits.formatBytesPerSecond(rate)));
        }

        if (Config.safe(Config.ELECTRICITY_BILLS, false)) {
            tooltip.add(gray("jade.datacenterextended.bill", Money.format(Math.round(data.getDoubleOr("bill", 0)))));
        } else if (Config.usesFe()) {
            tooltip.add(gray("jade.datacenterextended.energy", DataUnits.formatNumber(data.getLongOr("stored", 0)),
                    DataUnits.formatNumber(data.getLongOr("capacity", 0)), DataUnits.formatNumber(data.getLongOr("fe", 0))));
        }

        if (Config.safe(Config.HEAT_ENABLED, true)) {
            double speed = data.getDoubleOr("speed", 1);
            MutableComponent heat = Component.translatable("jade.datacenterextended.temperature",
                    String.format(Locale.ROOT, "%.1f", data.getDoubleOr("temperature", 20)));
            if (speed < 0.999) heat.append(Component.translatable("gui.datacenterextended.rack.throttled", Math.round(speed * 100)));
            tooltip.add(heat.withStyle(speed < 0.999 ? ChatFormatting.RED : ChatFormatting.GRAY));
        }

        tooltip.add(Component.literal(de.f4ked.datacenterextended.time.Calendar.format(
                de.f4ked.datacenterextended.client.ClientEconomy.today())).withStyle(ChatFormatting.DARK_GRAY));
        String owner = data.getStringOr("owner", "");
        if (accessor.showDetails() && !owner.isEmpty()) {
            tooltip.add(gray("jade.datacenterextended.owner", owner));
        }
    }

    private static Component gray(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.GRAY);
    }
}
