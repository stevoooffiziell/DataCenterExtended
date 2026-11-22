package de.f4ked.datacenterextended.client;

import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.client.ui.FlatButton;
import de.f4ked.datacenterextended.client.ui.Theme;
import de.f4ked.datacenterextended.client.ui.Ui;
import de.f4ked.datacenterextended.economy.Money;
import de.f4ked.datacenterextended.menu.PowerMenu;
import de.f4ked.datacenterextended.power.BatteryTier;
import de.f4ked.datacenterextended.power.GeneratorFamily;
import de.f4ked.datacenterextended.power.GeneratorStatus;
import de.f4ked.datacenterextended.power.GeneratorType;
import de.f4ked.datacenterextended.power.GridTier;
import de.f4ked.datacenterextended.power.model.ReactorModel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

import static de.f4ked.datacenterextended.menu.PowerMenu.*;
import static de.f4ked.datacenterextended.power.PowerBlockEntity.*;

/** GUI of generators, battery banks and grid connections in the dark server-room style. */
public class PowerScreen extends AbstractContainerScreen<PowerMenu> {
    private static final int CARD_X = 8, CARD_Y = 20, CARD_W = IMAGE_WIDTH - 16, CARD_H = 62;
    private @Nullable FlatButton restartButton;

    public PowerScreen(PowerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        if (menu.nuclear()) {
            restartButton = addRenderableWidget(FlatButton.of(Component.translatable("gui.datacenterextended.power.restart"),
                    b -> {
                        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                    }, leftPos + IMAGE_WIDTH - 8 - 48, topPos + SLOT_Y, 48, 16).labelColor(Theme.WARN));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (restartButton != null) {
            restartButton.visible = menu.status() == GeneratorStatus.SCRAM;
            restartButton.active = menu.heat() < ReactorModel.RESTART_BELOW;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        Ui.window(g, leftPos, topPos, imageWidth, imageHeight);
        Ui.inset(g, leftPos + CARD_X, topPos + CARD_Y, CARD_W, CARD_H);
        for (Slot slot : menu.slots) Ui.slot(g, leftPos + slot.x, topPos + slot.y);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int chipLeft = statusChip(g);
        Ui.fit(g, font, title, 6, 4, chipLeft - 10, Theme.TEXT);
        Ui.fit(g, font, playerInventoryTitle, INVENTORY_X, INVENTORY_Y - 11, 100, Theme.MUTED);
        int x = CARD_X + 5, w = CARD_W - 10, y = CARD_Y + 4;
        switch (menu.kind()) {
            case KIND_BATTERY -> battery(g, x, y, w);
            case KIND_GRID -> grid(g, x, y, w);
            default -> generator(g, x, y, w);
        }
    }

    /** Draws the status pill in the header; returns its left edge. */
    private int statusChip(GuiGraphicsExtractor g) {
        Component text;
        int color;
        if (menu.kind() == KIND_GENERATOR) {
            GeneratorStatus status = menu.status();
            text = Component.translatable(status.translationKey());
            color = !status.producing() ? Theme.ERR : status.warning() ? Theme.WARN : Theme.OK;
        } else if (menu.kind() == KIND_GRID && menu.rawStatus() == 1) {
            text = Component.translatable("status.datacenterextended.no_owner_short");
            color = Theme.ERR;
        } else if (menu.kind() == KIND_BATTERY) {
            double flow = menu.netFlow();
            text = Component.translatable(flow > 0.5 ? "gui.datacenterextended.power.charging"
                    : flow < -0.5 ? "gui.datacenterextended.power.discharging" : "gui.datacenterextended.power.idle");
            color = flow > 0.5 ? Theme.OK : flow < -0.5 ? Theme.WARN : Theme.MUTED;
        } else {
            boolean exporting = menu.output() > 0.05;
            text = Component.translatable(exporting ? "gui.datacenterextended.power.exporting" : "gui.datacenterextended.power.idle");
            color = exporting ? Theme.OK : Theme.MUTED;
        }
        return Ui.chip(g, font, text, IMAGE_WIDTH - 6, 3, color, 90);
    }

    private void generator(GuiGraphicsExtractor g, int x, int y, int w) {
        GeneratorType type = menu.generatorType();
        double output = menu.output(), peak = Math.max(0.0001, menu.peak());
        Ui.row(g, font, Component.translatable("gui.datacenterextended.power.output"),
                Component.literal(fe(output) + " / " + fe(peak) + " FE/t"), x, y, w, Theme.TEXT);
        Ui.bar(g, x, y + 10, w, 4, output / peak, Theme.ACCENT);
        Ui.row(g, font, Component.translatable("gui.datacenterextended.power.equivalent"),
                Component.literal(watts(output) + " · " + Math.round(100 * output / peak) + " %"), x, y + 17, w, Theme.MUTED);
        energyRows(g, x, y + 29, w);
        if (type == null) return;
        int ly = y + 47;
        if (type.family() == GeneratorFamily.NUCLEAR) {
            double heat = menu.heat();
            int color = heat >= ReactorModel.THROTTLE ? Theme.ERR : heat >= 0.6 ? Theme.WARN : Theme.OK;
            Ui.row(g, font, Component.translatable("gui.datacenterextended.power.core_heat"),
                    Component.literal(Math.round(heat * 100) + " %"), x, ly, w / 2 - 4, color);
            Ui.bar(g, x + w / 2 + 2, ly + 2, w / 2 - 2, 4, heat, color);
        } else if (type.family() == GeneratorFamily.COMBUSTION) {
            Ui.row(g, font, Component.translatable("gui.datacenterextended.power.burn_time"),
                    Component.literal(Math.round(menu.burn() * 100) + " %"), x, ly, w / 2 - 4, Theme.WARN);
            Ui.bar(g, x + w / 2 + 2, ly + 2, w / 2 - 2, 4, menu.burn(), Theme.WARN);
        } else {
            Ui.fit(g, font, Component.translatable(type.family().translationKey())
                    .append(" · ").append(Component.translatable("gui.datacenterextended.power.tier", type.tier()))
                    .append(" · ").append(Component.translatable("gui.datacenterextended.power.hint." + type.family().name().toLowerCase(Locale.ROOT))),
                    x, ly, w, Theme.DIM);
        }
    }

    private void battery(GuiGraphicsExtractor g, int x, int y, int w) {
        energyRows(g, x, y, w);
        double flow = menu.netFlow();
        int rate = BatteryTier.values()[Math.min(menu.typeOrdinal(), BatteryTier.values().length - 1)].rate();
        Ui.row(g, font, Component.translatable("gui.datacenterextended.power.flow"),
                Component.literal((flow >= 0 ? "+" : "") + fe(flow) + " FE/t"), x, y + 20, w, flow > 0.5 ? Theme.OK : flow < -0.5 ? Theme.WARN : Theme.MUTED);
        Component time;
        if (flow > 0.5) {
            time = Component.translatable("gui.datacenterextended.power.full_in", duration((menu.capacity() - menu.stored()) / flow));
        } else if (flow < -0.5) {
            time = Component.translatable("gui.datacenterextended.power.empty_in", duration(menu.stored() / -flow));
        } else {
            time = Component.translatable("gui.datacenterextended.power.idle");
        }
        Ui.row(g, font, Component.translatable("gui.datacenterextended.power.time"), time, x, y + 31, w, Theme.TEXT);
        Ui.row(g, font, Component.translatable("gui.datacenterextended.power.max_rate"),
                Component.literal(fe(rate) + " FE/t"), x, y + 42, w, Theme.MUTED);
    }

    private void grid(GuiGraphicsExtractor g, int x, int y, int w) {
        double export = menu.output(), max = Math.max(1, menu.peak());
        Ui.row(g, font, Component.translatable("gui.datacenterextended.power.export"),
                Component.literal(fe(export) + " / " + fe(max) + " FE/t"), x, y, w, Theme.TEXT);
        Ui.bar(g, x, y + 10, w, 4, export / max, Theme.OK);
        double price = menu.priceCentsPerKWh();
        Ui.row(g, font, Component.translatable("gui.datacenterextended.power.tariff"),
                Component.translatable("gui.datacenterextended.power.per_kwh", Money.format(Math.round(price))), x, y + 17, w, Theme.TEXT);
        double wattsNow = export / Math.max(1.0E-9, Config.safe(Config.FE_PER_WATT, 0.1));
        long perHour = Math.round(wattsNow / 1000.0 * price);
        Ui.row(g, font, Component.translatable("gui.datacenterextended.power.income"),
                Component.translatable("gui.datacenterextended.power.per_hour", Money.format(perHour)), x, y + 28, w, Theme.OK);
        Ui.row(g, font, Component.translatable("gui.datacenterextended.power.earned"),
                Component.literal(Money.format(menu.earned())), x, y + 39, w, Theme.ACCENT);
        GridTier tier = GridTier.values()[Math.min(menu.typeOrdinal(), GridTier.values().length - 1)];
        Ui.fit(g, font, Component.translatable("gui.datacenterextended.power.grid_hint", watts(tier.rate())), x, y + 50, w, Theme.DIM);
    }

    private void energyRows(GuiGraphicsExtractor g, int x, int y, int w) {
        long stored = menu.stored(), capacity = Math.max(1, menu.capacity());
        Ui.row(g, font, Component.translatable("gui.datacenterextended.power.buffer"),
                Component.literal(String.format(Locale.ROOT, "%,d / %,d FE", stored, capacity)), x, y, w, Theme.TEXT);
        Ui.bar(g, x, y + 10, w, 4, stored / (double) capacity, Theme.ENERGY);
    }

    static String fe(double value) {
        double abs = Math.abs(value);
        return abs >= 100 ? String.format(Locale.ROOT, "%,d", Math.round(value)) : String.format(Locale.ROOT, "%.1f", value);
    }

    /** FE/t as electrical power. */
    static String watts(double fePerTick) {
        double w = fePerTick / Math.max(1.0E-9, Config.safe(Config.FE_PER_WATT, 0.1));
        if (w >= 1_000_000) return String.format(Locale.ROOT, "%.1f MW", w / 1_000_000);
        if (w >= 1_000) return String.format(Locale.ROOT, "%.1f kW", w / 1_000);
        return Math.round(w) + " W";
    }

    private static String duration(double ticks) {
        long seconds = Math.max(0, Math.round(ticks / 20));
        if (seconds >= 3600) return (seconds / 3600) + " h " + (seconds % 3600 / 60) + " m";
        if (seconds >= 60) return (seconds / 60) + " m " + (seconds % 60) + " s";
        return seconds + " s";
    }
}
