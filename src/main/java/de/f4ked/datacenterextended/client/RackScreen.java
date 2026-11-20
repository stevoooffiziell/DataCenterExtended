package de.f4ked.datacenterextended.client;

import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.block.RackBlockEntity;
import de.f4ked.datacenterextended.block.RackStatus;
import de.f4ked.datacenterextended.block.RackType;
import de.f4ked.datacenterextended.hardware.DataUnits;
import de.f4ked.datacenterextended.hardware.ServerProblem;
import de.f4ked.datacenterextended.menu.RackMenu;
import de.f4ked.datacenterextended.economy.Coin;
import de.f4ked.datacenterextended.economy.Money;
import net.minecraft.client.gui.components.Button;
import org.jspecify.annotations.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static de.f4ked.datacenterextended.menu.RackMenu.*;

public class RackScreen extends AbstractContainerScreen<RackMenu> {
    private static final int INFO_X = 8, INFO_Y = 86, INFO_W = 160, INFO_H = 46;
    private static final int LED_RUNNING = 0xFF3CE07A, LED_PROBLEM = 0xFFE04040, LED_EMPTY = 0xFF2A2A2A;

    private static final int INCOME_X = 98, INCOME_W = 70;
    private @Nullable Button coinButton;

    public RackScreen(RackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        this.inventoryLabelY = INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        if (menu.type() == RackType.MINING) {
            coinButton = addRenderableWidget(Button.builder(coinLabel(), b -> {
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, RackBlockEntity.BUTTON_NEXT_COIN);
                }
            }).bounds(leftPos + INCOME_X, topPos + 17, INCOME_W, 16).build());
        }
    }

    private Component coinLabel() {
        Coin coin = menu.coin();
        return Component.literal("◀ " + coin.symbol() + " ▶").withColor(coin.color());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (coinButton != null) coinButton.setMessage(coinLabel());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        GuiDraw.window(g, x, y, imageWidth, imageHeight);

        for (Slot slot : menu.slots) {
            GuiDraw.slot(g, x + slot.x, y + slot.y);
        }
        // status LED bar below every server bay
        for (int i = 0; i < menu.serverSlotCount(); i++) {
            Slot slot = menu.slots.get(i);
            int code = menu.serverCode(i);
            int color = code == RackBlockEntity.CODE_EMPTY ? LED_EMPTY : code == RackBlockEntity.CODE_RUNNING ? LED_RUNNING : LED_PROBLEM;
            g.fill(x + slot.x, y + slot.y + 18, x + slot.x + 16, y + slot.y + 20, color);
        }

        if (energyRequired()) {
            long cap = menu.energyCapacity();
            GuiDraw.verticalBar(g, x + ENERGY_X, y + ENERGY_Y, ENERGY_W, ENERGY_H, cap > 0 ? (double) menu.energy() / cap : 0, GuiDraw.ENERGY);
        }
        GuiDraw.monitor(g, x + INFO_X, y + INFO_Y, INFO_W, INFO_H);
        if (menu.type() == RackType.FABRICATING) {
            long goal = menu.goal();
            double fraction = goal > 0 ? (double) menu.progress() / goal : 0;
            GuiDraw.bar(g, x + BAR_X, y + BAR_Y, BAR_W, BAR_H, fraction, menu.status() == RackStatus.RUNNING ? 0xFF3CB371 : 0xFF8B6914);
        } else {
            int top = menu.type() == RackType.MINING ? 38 : 18;
            GuiDraw.monitor(g, x + INCOME_X, y + top, INCOME_W, 70 - top + 4);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        RackType type = menu.type();
        RackStatus status = menu.status();
        int tx = INFO_X + 4, ty = INFO_Y + 2;
        boolean mining = type == RackType.MINING;

        // 1: status
        g.text(font, Component.translatable(status.translationKey()), tx, ty, status.ok() ? GuiDraw.TEXT_OK : GuiDraw.TEXT_ERR, false);

        // 2: performance
        Component perf = mining
                ? Component.translatable("gui.datacenterextended.rack.hashrate", DataUnits.formatHashrate(menu.localRate()))
                : Component.translatable("gui.datacenterextended.rack.compute",
                DataUnits.formatBytesPerSecond(menu.localRate()), DataUnits.formatBytesPerSecond(menu.effectiveRate()));
        g.text(font, perf, tx, ty + 9, GuiDraw.TEXT_INFO, false);

        // 3: network
        Component net;
        int netColor = GuiDraw.TEXT_INFO;
        if (mining) {
            net = Component.translatable("gui.datacenterextended.rack.standalone");
        } else if (!menu.networked()) {
            net = Component.translatable("gui.datacenterextended.rack.no_network");
            netColor = GuiDraw.TEXT_WARN;
        } else {
            net = Component.translatable("gui.datacenterextended.rack.network",
                    DataUnits.formatBytesPerSecond(menu.received()), DataUnits.formatBytesPerSecond(menu.exported()),
                    DataUnits.formatBytesPerSecond(menu.bandwidth()));
        }
        g.text(font, net, tx, ty + 18, netColor, false);

        // 4: power (+ FE)
        Component power = energyRequired()
                ? Component.translatable("gui.datacenterextended.rack.power_fe", menu.watts(), DataUnits.formatNumber(menu.fePerTick()))
                : Component.translatable("gui.datacenterextended.rack.power", menu.watts());
        g.text(font, power, tx, ty + 27, status == RackStatus.NO_ENERGY ? GuiDraw.TEXT_ERR : GuiDraw.TEXT_INFO, false);

        // income panel (mining / hosting)
        if (type != RackType.FABRICATING) {
            extractIncome(g, type);
        }

        // 5: progress / ETA (fabricating) or storage bonus (hosting)
        if (type == RackType.HOSTING) {
            g.text(font, Component.translatable("gui.datacenterextended.rack.storage_bonus", Math.round(menu.storageBonus() * 100)),
                    tx, ty + 36, GuiDraw.TEXT_INFO, false);
        }
        long goal = menu.goal();
        if (goal > 0 && type == RackType.FABRICATING) {
            double speed = switch (type) {
                case HOSTING -> 1.0 + menu.storageBonus();
                case FABRICATING -> Config.safe(Config.FABRICATION_SPEED, 1.0);
                case MINING -> 1.0;
            };
            double rate = menu.effectiveRate() * speed;
            String eta = rate > 0 ? DataUnits.formatDuration((goal - menu.progress()) / rate) : "∞";
            Component line = switch (type) {
                case MINING -> Component.translatable("gui.datacenterextended.next_bitcoin", eta);
                case HOSTING -> Component.translatable("gui.datacenterextended.next_credit_bonus", eta, Math.round(menu.storageBonus() * 100));
                case FABRICATING -> Component.translatable("gui.datacenterextended.fabricating",
                        DataUnits.formatBytes(menu.progress()), DataUnits.formatBytes(goal), eta);
            };
            g.text(font, line, tx, ty + 36, GuiDraw.TEXT_INFO, false);
        }
    }

    private void extractIncome(GuiGraphicsExtractor g, RackType type) {
        int px = INCOME_X + 3;
        int py = (type == RackType.MINING ? 38 : 18) + 3;
        double perHour = menu.incomePerHour();
        if (type == RackType.MINING) {
            Coin coin = menu.coin();
            double dollars = perHour * ClientEconomy.price(coin);
            g.text(font, Component.translatable("gui.datacenterextended.income.per_hour"), px, py, GuiDraw.TEXT_INFO, false);
            g.text(font, Money.formatCoin(perHour, coin), px, py + 10, coin.color(), false);
            g.text(font, "≈ " + Money.format(Math.round(dollars * 100)), px, py + 20, GuiDraw.TEXT_OK, false);
        } else {
            g.text(font, Component.translatable("gui.datacenterextended.income.per_hour"), px, py, GuiDraw.TEXT_INFO, false);
            g.text(font, Money.format(Math.round(perHour)), px, py + 12, GuiDraw.TEXT_OK, false);
            g.text(font, Component.translatable("gui.datacenterextended.income.per_day", Money.format(Math.round(perHour * 0.3333))),
                    px, py + 24, GuiDraw.TEXT_INFO, false);
            g.text(font, Component.translatable("gui.datacenterextended.income.wallet"), px, py + 40, 0xFF7F8FA0, false);
        }
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> tooltip = super.getTooltipFromContainerItem(stack);
        Slot slot = hoveredSlot;
        if (slot != null && slot.index < menu.serverSlotCount()) {
            tooltip.add(serverState(menu.serverCode(slot.index)));
        }
        return tooltip;
    }

    private static Component serverState(int code) {
        if (code == RackBlockEntity.CODE_RUNNING) {
            return Component.translatable("gui.datacenterextended.server.running").withStyle(ChatFormatting.GREEN);
        }
        return Component.translatable(ServerProblem.byId(code - 2).translationKey()).withStyle(ChatFormatting.RED);
    }

    private static boolean energyRequired() {
        return Config.safe(Config.REQUIRE_ENERGY, true);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        int gx = leftPos + ENERGY_X, gy = topPos + ENERGY_Y;
        if (energyRequired() && mouseX >= gx - 1 && mouseX <= gx + ENERGY_W && mouseY >= gy - 1 && mouseY <= gy + ENERGY_H) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("gui.datacenterextended.energy.stored",
                            DataUnits.formatNumber(menu.energy()), DataUnits.formatNumber(menu.energyCapacity())),
                    Component.translatable("gui.datacenterextended.energy.usage", DataUnits.formatNumber(menu.fePerTick()))
                            .withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            return;
        }
        Slot slot = hoveredSlot;
        if (slot == null || slot.hasItem() || !menu.getCarried().isEmpty() || slot.index >= menu.slots.size() - 36) return;
        String key;
        if (slot.index < menu.serverSlotCount()) key = "server_bay";
        else if (menu.type() == RackType.FABRICATING && slot.index == menu.serverSlotCount()) key = "target";
        else key = "output";
        g.setTooltipForNextFrame(font, Component.translatable("gui.datacenterextended.slot." + key), mouseX, mouseY);
    }
}
