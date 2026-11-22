package de.f4ked.datacenterextended.client;

import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.block.RackBlockEntity;
import de.f4ked.datacenterextended.block.RackStatus;
import de.f4ked.datacenterextended.block.RackType;
import de.f4ked.datacenterextended.client.ui.FlatButton;
import de.f4ked.datacenterextended.client.ui.Theme;
import de.f4ked.datacenterextended.client.ui.Ui;
import de.f4ked.datacenterextended.economy.Coin;
import de.f4ked.datacenterextended.economy.Money;
import de.f4ked.datacenterextended.hardware.DataUnits;
import de.f4ked.datacenterextended.hardware.ServerProblem;
import de.f4ked.datacenterextended.hosting.HostingPlan;
import de.f4ked.datacenterextended.menu.RackMenu;
import de.f4ked.datacenterextended.time.Calendar;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static de.f4ked.datacenterextended.menu.RackMenu.*;

/**
 * Rack GUI in the dark theme: header (name, date, status chip), bays with LEDs, cooling slots, FE and temperature
 * gauges, an income / production card and an info screen whose lines are always fitted to their width.
 */
public class RackScreen extends AbstractContainerScreen<RackMenu> {
    private static final int INFO_X = 8, INFO_Y = 92, INFO_W = 188, INFO_H = 56;
    private static final int CARD_Y = 22, CARD_BOTTOM = 86;

    private @Nullable FlatButton coinButton;
    private @Nullable FlatButton comparatorButton;

    public RackScreen(RackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        if (menu.type() == RackType.MINING) {
            coinButton = addRenderableWidget(FlatButton.of(coinLabel(), b -> pressButton(RackBlockEntity.BUTTON_NEXT_COIN),
                    leftPos + RIGHT_X, topPos + CARD_Y - 1, RIGHT_W, 14));
        }
        comparatorButton = addRenderableWidget(FlatButton.of(comparatorLabel(), b -> pressButton(RackBlockEntity.BUTTON_COMPARATOR),
                leftPos + imageWidth - 8 - 90, topPos + INVENTORY_Y - 14, 90, 12));
        comparatorButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("gui.datacenterextended.comparator.tooltip")));
    }

    private void pressButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private Component coinLabel() {
        Coin coin = menu.coin();
        return Component.literal("◀ " + coin.symbol() + " ▶").withColor(coin.color());
    }

    private Component comparatorLabel() {
        return Component.translatable("gui.datacenterextended.comparator.button",
                Component.translatable(menu.comparatorMode().translationKey()), menu.comparatorSignal());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (coinButton != null) coinButton.setMessage(coinLabel());
        if (comparatorButton != null) comparatorButton.setMessage(comparatorLabel());
    }

    // ================================================================== background

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        Ui.window(g, x, y, imageWidth, imageHeight);
        Ui.panel(g, x + 4, y + CARD_Y - 4, ENERGY_X - 10, CARD_BOTTOM - CARD_Y + 7);

        for (Slot slot : menu.slots) Ui.slot(g, x + slot.x, y + slot.y);

        // status LED bar below every bay
        for (int i = 0; i < menu.serverSlotCount(); i++) {
            Slot slot = menu.slots.get(i);
            int code = menu.serverCode(i);
            int color = code == RackBlockEntity.CODE_EMPTY ? Theme.BORDER : code == RackBlockEntity.CODE_RUNNING ? Theme.OK : Theme.ERR;
            g.fill(x + slot.x + 1, y + slot.y + 18, x + slot.x + 15, y + slot.y + 20, color);
        }
        for (int i = menu.serverSlotCount(); i < menu.coolingSlotEnd(); i++) {
            Slot slot = menu.slots.get(i);
            if (!slot.hasItem()) g.text(font, "❄", x + slot.x + 4, y + slot.y + 4, Theme.DIM, false);
        }

        if (Config.usesFe()) {
            long cap = menu.energyCapacity();
            Ui.gauge(g, x + ENERGY_X, y + ENERGY_Y, ENERGY_W, ENERGY_H, cap > 0 ? (double) menu.energy() / cap : 0, Theme.ENERGY);
        }
        if (heatEnabled()) {
            double temp = menu.temperature();
            double max = Config.safe(Config.CRITICAL_TEMPERATURE, 95.0) + 5;
            Ui.gauge(g, x + TEMP_X, y + ENERGY_Y, ENERGY_W, ENERGY_H, Math.max(0, temp) / max, temperatureColor(temp));
            int markY = y + ENERGY_Y + ENERGY_H - (int) Math.round(Config.safe(Config.THROTTLE_TEMPERATURE, 70.0) / max * ENERGY_H);
            g.fill(x + TEMP_X - 2, markY, x + TEMP_X + ENERGY_W + 2, markY + 1, Theme.WARN);
        }

        if (menu.type() == RackType.FABRICATING) {
            long goal = menu.goal();
            double fraction = goal > 0 ? (double) menu.progress() / goal : 0;
            Ui.bar(g, x + BAR_X, y + BAR_Y, BAR_W, BAR_H, fraction, menu.status().ok() ? Theme.OK : Theme.WARN);
        } else {
            int top = menu.type() == RackType.MINING ? CARD_Y + 16 : CARD_Y - 1;
            Ui.inset(g, x + RIGHT_X - 1, y + top, RIGHT_W + 2, CARD_BOTTOM - top + 1);
        }
        Ui.inset(g, x + INFO_X, y + INFO_Y, INFO_W, INFO_H);
    }

    private static int temperatureColor(double temp) {
        double throttle = Config.safe(Config.THROTTLE_TEMPERATURE, 70.0);
        if (temp >= throttle) return Theme.ERR;
        if (temp >= throttle * 0.75) return Theme.WARN;
        return Theme.ACCENT;
    }

    // ================================================================== labels

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // header: title, date, status chip
        RackStatus status = menu.status();
        int statusColor = Theme.status(status.ok(), status.warning());
        Component date = Component.literal(Calendar.format(ClientEconomy.today()));
        int dateW = font.width(date);
        int chipLeft = Ui.chip(g, font, Component.translatable(status.ok() ? (status.warning() ? "gui.datacenterextended.chip.warning"
                : "gui.datacenterextended.chip.online") : "gui.datacenterextended.chip.offline"), imageWidth - 5, 3, statusColor, 60);
        Ui.fit(g, font, Component.literal(date.getString()), chipLeft - dateW - 6, 4, dateW, Theme.MUTED);
        Ui.fit(g, font, title, 6, 4, chipLeft - dateW - 14, Theme.TEXT);
        Ui.fit(g, font, playerInventoryTitle, INVENTORY_X, INVENTORY_Y - 11, 80, Theme.MUTED);

        extractInfo(g, status, statusColor);
        if (menu.type() != RackType.FABRICATING) extractIncome(g, menu.type());
    }

    private void extractInfo(GuiGraphicsExtractor g, RackStatus status, int statusColor) {
        RackType type = menu.type();
        boolean mining = type == RackType.MINING;
        int tx = INFO_X + 4, ty = INFO_Y + 3, w = INFO_W - 8;

        Ui.fit(g, font, Component.translatable(status.translationKey()), tx, ty, w, statusColor);

        Component perf = mining
                ? Component.translatable("gui.datacenterextended.rack.hashrate", DataUnits.formatHashrate(menu.localRate()))
                : Component.translatable("gui.datacenterextended.rack.compute",
                DataUnits.formatBytesPerSecond(menu.localRate()), DataUnits.formatBytesPerSecond(menu.effectiveRate()));
        Ui.fit(g, font, perf, tx, ty + 9, w, Theme.TEXT);

        Component net;
        int netColor = Theme.TEXT;
        if (mining) {
            net = Component.translatable("gui.datacenterextended.rack.standalone");
            netColor = Theme.MUTED;
        } else if (!menu.networked()) {
            net = Component.translatable("gui.datacenterextended.rack.no_network");
            netColor = Theme.WARN;
        } else {
            net = Component.translatable("gui.datacenterextended.rack.network",
                    DataUnits.formatBytesPerSecond(menu.received()), DataUnits.formatBytesPerSecond(menu.exported()),
                    DataUnits.formatBytesPerSecond(menu.bandwidth()));
        }
        Ui.fit(g, font, net, tx, ty + 18, w, netColor);

        Component power;
        if (Config.safe(Config.ELECTRICITY_BILLS, false)) {
            power = menu.selfPowered() > 0.001
                    ? Component.translatable("gui.datacenterextended.rack.power_bill_self", Money.format(menu.billPerHour()),
                    Math.round(menu.selfPowered() * 100))
                    : Component.translatable("gui.datacenterextended.rack.power_bill", menu.watts(), Money.format(menu.billPerHour()));
        } else if (Config.usesFe()) {
            power = Component.translatable("gui.datacenterextended.rack.power_fe", menu.watts(), DataUnits.formatNumber(menu.fePerTick()));
        } else {
            power = Component.translatable("gui.datacenterextended.rack.power", menu.watts());
        }
        boolean powerProblem = status == RackStatus.NO_ENERGY || status == RackStatus.NO_FUNDS;
        Ui.fit(g, font, power, tx, ty + 27, w, powerProblem ? Theme.ERR : Theme.TEXT);

        switch (type) {
            case HOSTING -> Ui.fit(g, font, Component.translatable("gui.datacenterextended.rack.hosting_line",
                    DataUnits.formatBytesPerSecond(menu.contracted()), Math.round(menu.storageBonus() * 100)), tx, ty + 36, w, Theme.TEXT);
            case MINING -> {
                Coin coin = menu.coin();
                java.time.LocalDate next = ClientEconomy.nextHalving(coin);
                Component reward = Component.translatable("gui.datacenterextended.rack.reward",
                        "1/" + (1L << Math.min(62, ClientEconomy.halvings(coin))));
                if (next != null) reward = reward.copy().append(" · ").append(Component.translatable(
                        "gui.datacenterextended.rack.next_halving", Calendar.formatMonth(next)));
                Ui.fit(g, font, reward, tx, ty + 36, w, Theme.MUTED);
            }
            case FABRICATING -> {
                long goal = menu.goal();
                if (goal > 0) {
                    double rate = menu.effectiveRate() * Config.safe(Config.FABRICATION_SPEED, 1.0);
                    String eta = rate > 0 ? DataUnits.formatDuration((goal - menu.progress()) / rate) : "∞";
                    Ui.fit(g, font, Component.translatable("gui.datacenterextended.fabricating",
                            DataUnits.formatBytes(menu.progress()), DataUnits.formatBytes(goal), eta), tx, ty + 36, w, Theme.TEXT);
                }
            }
        }

        if (heatEnabled()) {
            double speed = menu.speedFactor();
            Component heat = Component.translatable("gui.datacenterextended.rack.heat",
                    String.format(Locale.ROOT, "%.1f", menu.temperature()),
                    DataUnits.formatWatts(menu.heatLoad()), DataUnits.formatWatts(menu.coolingCapacity()));
            if (speed < 0.999) heat = heat.copy().append(Component.translatable("gui.datacenterextended.rack.throttled", Math.round(speed * 100)));
            int color = speed < 0.999 ? Theme.ERR : menu.heatLoad() > menu.coolingCapacity() ? Theme.WARN : Theme.TEXT;
            Ui.fit(g, font, heat, tx, ty + 45, w, color);
        }
    }

    private void extractIncome(GuiGraphicsExtractor g, RackType type) {
        int px = RIGHT_X + 3, w = RIGHT_W - 5;
        int py = (type == RackType.MINING ? CARD_Y + 19 : CARD_Y + 2);
        double perHour = menu.incomePerHour();
        Ui.fit(g, font, Component.translatable("gui.datacenterextended.income.per_hour"), px, py, w, Theme.MUTED);
        if (type == RackType.MINING) {
            Coin coin = menu.coin();
            double dollars = perHour * ClientEconomy.price(coin);
            Ui.fit(g, font, Money.formatCoin(perHour, coin), px, py + 11, w, coin.color());
            Ui.fit(g, font, "≈ " + Money.format(Math.round(dollars * 100)), px, py + 22, w, Theme.OK);
            Ui.fit(g, font, Money.formatPrice(ClientEconomy.price(coin)) + " / " + coin.symbol(), px, py + 33, w, Theme.DIM);
        } else {
            Ui.fit(g, font, Money.format(Math.round(perHour)), px, py + 11, w, Theme.OK);
            StringBuilder plans = new StringBuilder();
            for (HostingPlan plan : HostingPlan.values()) {
                if (!plans.isEmpty()) plans.append("  ");
                plans.append(plan.shortName()).append(menu.customers(plan));
            }
            Ui.fit(g, font, plans.toString(), px, py + 24, w, Theme.TEXT);
            double satisfaction = menu.satisfaction();
            Ui.fit(g, font, Component.translatable("gui.datacenterextended.hosting.satisfaction", Math.round(satisfaction * 100)),
                    px, py + 35, w, satisfaction > 0.9 ? Theme.OK : satisfaction > 0.6 ? Theme.WARN : Theme.ERR);
            Ui.fit(g, font, Component.translatable("gui.datacenterextended.income.wallet"), px, py + 50, w, Theme.DIM);
        }
    }

    // ================================================================== tooltips

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> tooltip = super.getTooltipFromContainerItem(stack);
        Slot slot = hoveredSlot;
        if (slot != null && slot.index < menu.serverSlotCount()) tooltip.add(serverState(menu.serverCode(slot.index)));
        return tooltip;
    }

    private static Component serverState(int code) {
        if (code == RackBlockEntity.CODE_RUNNING) {
            return Component.translatable("gui.datacenterextended.server.running").withStyle(ChatFormatting.GREEN);
        }
        return Component.translatable(ServerProblem.byId(code - 2).translationKey()).withStyle(ChatFormatting.RED);
    }

    private static boolean heatEnabled() {
        return Config.safe(Config.HEAT_ENABLED, true);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        if (Config.usesFe() && Ui.inside(mouseX, mouseY, leftPos + ENERGY_X - 1, topPos + ENERGY_Y - 1, ENERGY_W + 2, ENERGY_H + 2)) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("gui.datacenterextended.energy.stored",
                            DataUnits.formatNumber(menu.energy()), DataUnits.formatNumber(menu.energyCapacity())),
                    Component.translatable("gui.datacenterextended.energy.usage", DataUnits.formatNumber(menu.fePerTick()))
                            .withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            return;
        }
        if (heatEnabled() && Ui.inside(mouseX, mouseY, leftPos + TEMP_X - 1, topPos + ENERGY_Y - 1, ENERGY_W + 2, ENERGY_H + 2)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.datacenterextended.heat.temperature", String.format(Locale.ROOT, "%.1f", menu.temperature())));
            lines.add(Component.translatable("gui.datacenterextended.heat.load", DataUnits.formatWatts(menu.heatLoad()),
                    DataUnits.formatWatts(menu.coolingCapacity())).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("gui.datacenterextended.heat.throttle",
                    Math.round(Config.safe(Config.THROTTLE_TEMPERATURE, 70.0))).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("gui.datacenterextended.heat.speed", Math.round(menu.speedFactor() * 100))
                    .withStyle(menu.speedFactor() < 0.999 ? ChatFormatting.RED : ChatFormatting.GREEN));
            g.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
            return;
        }
        Slot slot = hoveredSlot;
        if (slot == null || slot.hasItem() || !menu.getCarried().isEmpty() || slot.index >= menu.slots.size() - 36) return;
        String key;
        if (slot.index < menu.serverSlotCount()) key = "server_bay";
        else if (slot.index < menu.coolingSlotEnd()) key = "cooling";
        else if (menu.type() == RackType.FABRICATING && slot.index == menu.coolingSlotEnd()) key = "target";
        else key = "output";
        g.setTooltipForNextFrame(font, Component.translatable("gui.datacenterextended.slot." + key), mouseX, mouseY);
    }
}
