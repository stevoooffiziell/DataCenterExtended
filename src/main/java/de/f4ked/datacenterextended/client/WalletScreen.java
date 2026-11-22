package de.f4ked.datacenterextended.client;

import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.client.ui.FlatButton;
import de.f4ked.datacenterextended.client.ui.Theme;
import de.f4ked.datacenterextended.client.ui.Ui;
import de.f4ked.datacenterextended.economy.Coin;
import de.f4ked.datacenterextended.economy.ItemValue;
import de.f4ked.datacenterextended.economy.Money;
import de.f4ked.datacenterextended.economy.PriceHistory;
import de.f4ked.datacenterextended.economy.ShopPricing;
import de.f4ked.datacenterextended.economy.StatsLedger;
import de.f4ked.datacenterextended.hardware.DataUnits;
import de.f4ked.datacenterextended.hardware.HardwareStats;
import de.f4ked.datacenterextended.hardware.HardwareType;
import de.f4ked.datacenterextended.item.PrebuiltServerItem;
import de.f4ked.datacenterextended.network.ModPayloads;
import de.f4ked.datacenterextended.registry.ModDataMaps;
import de.f4ked.datacenterextended.registry.ModItems;
import de.f4ked.datacenterextended.time.Calendar;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Wallet (balance, coins, price chart, selling), Hardware Shop, Service (repair / sell second-hand) and Stats (income
 * and bills over time). Opened with the wallet key or at a Hardware Shop block. All actions are requests; the server
 * validates them and syncs the wallet back.
 */
public class WalletScreen extends Screen {
    private static final int W = 288, H = 232;
    private static final int GRID = StatsLedger.Category.GRID.ordinal();
    private static final int GRID_COLOR = 0xFF60A5FA;
    private static final int TOP = 38;
    private static final int CELL = 20;
    // wallet tab
    private static final int COIN_W = 168, COIN_H = 28;
    private static final int CHART_X = 6, CHART_Y = 132, CHART_W = 276, CHART_H = 64;
    // shop tab
    private static final int GRID_X = 90, GRID_Y = TOP, COLS = 9, VISIBLE_ROWS = 6;
    // service tab
    private static final int SERVICE_X = 6, SERVICE_Y = TOP, SERVICE_COLS = 9;
    // stats tab
    private static final int STATS_X = 6, STATS_Y = 72, STATS_W = 276, STATS_H = 108;

    private enum Tab { WALLET, SHOP, SERVICE, STATS }

    private enum Category {
        MOTHERBOARD, CPU, MEMORY, GPU, PSU, STORAGE, ASIC, COOLING, PREBUILT, ENERGY, MATERIALS, INFRASTRUCTURE;

        Component label() {
            return switch (this) {
                case INFRASTRUCTURE -> Component.translatable("gui.datacenterextended.shop.infrastructure");
                case COOLING -> Component.translatable("gui.datacenterextended.shop.cooling");
                case PREBUILT -> Component.translatable("gui.datacenterextended.shop.prebuilt");
                case ENERGY -> Component.translatable("gui.datacenterextended.shop.energy");
                case MATERIALS -> Component.translatable("gui.datacenterextended.shop.materials");
                default -> Component.translatable(HardwareType.valueOf(name()).translationKey());
            };
        }

        static Category of(Item item) {
            if (item instanceof PrebuiltServerItem) return PREBUILT;
            if (item instanceof net.minecraft.world.item.BlockItem blockItem
                    && (blockItem.getBlock() instanceof de.f4ked.datacenterextended.power.PowerBlock
                    || blockItem.getBlock() instanceof de.f4ked.datacenterextended.power.PowerCableBlock)) {
                return ENERGY;
            }
            if (ModItems.MATERIALS.stream().anyMatch(m -> m.get() == item)) return MATERIALS;
            ItemStack stack = new ItemStack(item);
            if (ModDataMaps.cooling(stack) != null) return COOLING;
            HardwareStats stats = ModDataMaps.hardware(stack);
            if (stats == null) return INFRASTRUCTURE;
            return switch (stats.type()) {
                case MOTHERBOARD -> MOTHERBOARD;
                case CPU -> CPU;
                case MEMORY -> MEMORY;
                case GPU -> GPU;
                case PSU -> PSU;
                case STORAGE -> STORAGE;
                case ASIC -> ASIC;
            };
        }
    }

    private final boolean atShop;
    private Tab tab;
    private Coin selectedCoin = Coin.BITCOIN;
    private Category category = Category.MOTHERBOARD;
    private final Map<Category, List<Item>> catalog = new EnumMap<>(Category.class);
    private int scroll;
    private @Nullable Item selectedItem;
    private int serviceSlot = -1;
    private StatsLedger.Scale scale = StatsLedger.Scale.DAY;
    private int statsTimer;
    private int left, top;

    private final Map<Tab, List<net.minecraft.client.gui.components.AbstractWidget>> widgets = new EnumMap<>(Tab.class);
    private final Map<Tab, FlatButton> tabButtons = new EnumMap<>(Tab.class);
    private final Map<Category, FlatButton> categoryButtons = new EnumMap<>(Category.class);
    private final Map<StatsLedger.Scale, FlatButton> scaleButtons = new EnumMap<>(StatsLedger.Scale.class);
    private @Nullable EditBox amountBox;
    private @Nullable EditBox quantityBox;
    private @Nullable FlatButton buyButton, repairButton, sellPartButton;

    public WalletScreen(boolean atShop) {
        super(Component.translatable("gui.datacenterextended.wallet.title"));
        this.atShop = atShop;
        this.tab = atShop ? Tab.SHOP : Tab.WALLET;
        buildCatalog();
    }

    private boolean shopAvailable() {
        return atShop || Config.safe(Config.REMOTE_SHOP, true);
    }

    private void buildCatalog() {
        for (Category c : Category.values()) catalog.put(c, new ArrayList<>());
        for (Item item : BuiltInRegistries.ITEM) {
            if (ShopPricing.sold(item)) catalog.get(Category.of(item)).add(item);
        }
        Comparator<Item> byRelease = Comparator.comparingLong(i -> {
            LocalDate d = ShopPricing.releaseDate(i);
            return d == null ? Long.MIN_VALUE : d.toEpochDay();
        });
        for (List<Item> items : catalog.values()) items.sort(byRelease.thenComparingLong(i -> ShopPricing.priceCents(i, 0)));
        for (Category c : Category.values()) {
            if (!catalog.get(c).isEmpty()) {
                category = c;
                break;
            }
        }
    }

    // ================================================================== widgets

    private <T extends net.minecraft.client.gui.components.AbstractWidget> T add(Tab owner, T widget) {
        widgets.computeIfAbsent(owner, t -> new ArrayList<>()).add(widget);
        return addRenderableWidget(widget);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        widgets.clear();
        tabButtons.clear();
        categoryButtons.clear();
        scaleButtons.clear();

        int tabW = (W - 12 - 6) / 4;
        for (Tab t : Tab.values()) {
            FlatButton b = addRenderableWidget(FlatButton.of(Component.translatable("gui.datacenterextended.tab." + t.name().toLowerCase(Locale.ROOT)),
                    btn -> switchTab(t), left + 6 + t.ordinal() * (tabW + 2), top + 20, tabW, 14));
            b.active = t == Tab.WALLET || t == Tab.STATS || shopAvailable();
            tabButtons.put(t, b);
        }

        // wallet: sell buttons per coin card, amount box + sell
        for (Coin coin : Coin.values()) {
            int y = top + TOP + coin.ordinal() * (COIN_H + 2) + 8;
            add(Tab.WALLET, sellButton(Component.literal("25%"), coin, 0.25, left + 180, y));
            add(Tab.WALLET, sellButton(Component.literal("50%"), coin, 0.50, left + 214, y));
            add(Tab.WALLET, sellButton(Component.translatable("gui.datacenterextended.wallet.all"), coin, 1.0, left + 248, y));
        }
        amountBox = new EditBox(font, left + 6, top + 204, 96, 14, Component.translatable("gui.datacenterextended.wallet.amount"));
        amountBox.setHint(Component.translatable("gui.datacenterextended.wallet.amount"));
        amountBox.setMaxLength(20);
        add(Tab.WALLET, amountBox);
        add(Tab.WALLET, FlatButton.of(Component.translatable("gui.datacenterextended.wallet.sell"), b -> sellTyped(),
                left + 106, top + 204, 56, 14));

        // shop
        int i = 0;
        for (Category c : Category.values()) {
            FlatButton button = add(Tab.SHOP, FlatButton.of(c.label(), b -> {
                category = c;
                scroll = 0;
                selectedItem = null;
            }, left + 6, top + TOP + i++ * 13, 80, 12));
            button.active = !catalog.get(c).isEmpty();
            categoryButtons.put(c, button);
        }
        add(Tab.SHOP, FlatButton.of(Component.literal("-"), b -> addQuantity(-1), left + 90, top + 208, 14, 14));
        quantityBox = new EditBox(font, left + 106, top + 208, 28, 14, Component.translatable("gui.datacenterextended.shop.quantity"));
        quantityBox.setValue("1");
        quantityBox.setMaxLength(2);
        add(Tab.SHOP, quantityBox);
        add(Tab.SHOP, FlatButton.of(Component.literal("+"), b -> addQuantity(1), left + 136, top + 208, 14, 14));
        buyButton = add(Tab.SHOP, FlatButton.of(Component.translatable("gui.datacenterextended.shop.buy"), b -> buy(),
                left + W - 6 - 60, top + 206, 60, 18).labelColor(Theme.OK));

        // service
        repairButton = add(Tab.SERVICE, FlatButton.of(Component.translatable("gui.datacenterextended.service.repair"), b -> service(true),
                left + W - 6 - 124, top + 206, 60, 18));
        sellPartButton = add(Tab.SERVICE, FlatButton.of(Component.translatable("gui.datacenterextended.wallet.sell"), b -> service(false),
                left + W - 6 - 60, top + 206, 60, 18));

        // stats
        int s = 0;
        for (StatsLedger.Scale sc : StatsLedger.Scale.values()) {
            FlatButton b = add(Tab.STATS, FlatButton.of(Component.translatable("gui.datacenterextended.stats.scale." + sc.name().toLowerCase(Locale.ROOT)),
                    btn -> {
                        scale = sc;
                        requestStats();
                    }, left + 6 + s++ * 56, top + TOP, 54, 13));
            scaleButtons.put(sc, b);
        }
        updateVisibility();
        if (tab == Tab.STATS) requestStats();
    }

    private FlatButton sellButton(Component label, Coin coin, double fraction, int x, int y) {
        return FlatButton.of(label, b -> {
            double amount = ClientEconomy.coins(coin) * fraction;
            if (amount > 0) ClientPacketDistributor.sendToServer(new ModPayloads.SellCoin(coin.ordinal(), amount));
        }, x, y, 32, 13);
    }

    private void switchTab(Tab tab) {
        if ((tab == Tab.SHOP || tab == Tab.SERVICE) && !shopAvailable()) return;
        this.tab = tab;
        updateVisibility();
        if (tab == Tab.STATS) requestStats();
    }

    private void updateVisibility() {
        for (Map.Entry<Tab, List<net.minecraft.client.gui.components.AbstractWidget>> e : widgets.entrySet()) {
            for (var w : e.getValue()) w.visible = e.getKey() == tab;
        }
    }

    private void requestStats() {
        statsTimer = 0;
        ClientPacketDistributor.sendToServer(new ModPayloads.RequestStats(scale.ordinal()));
    }

    private void sellTyped() {
        if (amountBox == null) return;
        try {
            double amount = Double.parseDouble(amountBox.getValue().trim().replace(',', '.'));
            if (amount > 0) ClientPacketDistributor.sendToServer(new ModPayloads.SellCoin(selectedCoin.ordinal(), amount));
        } catch (NumberFormatException ignored) {
            // not a number: nothing to sell
        }
    }

    private int quantity() {
        if (quantityBox == null) return 1;
        try {
            return Mth.clamp(Integer.parseInt(quantityBox.getValue().trim()), 1, 64);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void addQuantity(int delta) {
        if (quantityBox != null) quantityBox.setValue(Integer.toString(Mth.clamp(quantity() + delta, 1, 64)));
    }

    private void buy() {
        if (selectedItem != null) ClientPacketDistributor.sendToServer(new ModPayloads.BuyItem(selectedItem, quantity()));
    }

    private ItemStack serviceStack() {
        if (serviceSlot < 0 || minecraft == null || minecraft.player == null) return ItemStack.EMPTY;
        return minecraft.player.getInventory().getItem(serviceSlot);
    }

    private void service(boolean repair) {
        ItemStack stack = serviceStack();
        if (!stack.isEmpty()) ClientPacketDistributor.sendToServer(new ModPayloads.ServiceItem(serviceSlot, stack.getItem(), repair));
    }

    private static double today() {
        return ClientEconomy.epochDay();
    }

    @Override
    public void tick() {
        super.tick();
        for (Map.Entry<Tab, FlatButton> e : tabButtons.entrySet()) e.getValue().selected(e.getKey() == tab);
        for (Map.Entry<Category, FlatButton> e : categoryButtons.entrySet()) e.getValue().selected(e.getKey() == category);
        for (Map.Entry<StatsLedger.Scale, FlatButton> e : scaleButtons.entrySet()) e.getValue().selected(e.getKey() == scale);
        if (buyButton != null) {
            long price = selectedItem == null ? -1 : ShopPricing.priceCents(selectedItem, today());
            buyButton.active = price >= 0 && selectedItem != null && ShopPricing.released(selectedItem, today())
                    && price * quantity() <= ClientEconomy.cents();
        }
        ItemStack stack = serviceStack();
        if (repairButton != null) {
            long cost = ItemValue.repairCents(stack, today());
            repairButton.active = cost > 0 && cost * stack.getCount() <= ClientEconomy.cents();
        }
        if (sellPartButton != null) sellPartButton.active = ItemValue.resaleCents(stack, today()) >= 0;
        if (tab == Tab.STATS && ++statsTimer >= 100) requestStats();
    }

    // ================================================================== input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        int mx = (int) event.x() - left, my = (int) event.y() - top;
        switch (tab) {
            case WALLET -> {
                for (Coin coin : Coin.values()) {
                    int y = TOP + coin.ordinal() * (COIN_H + 2);
                    if (mx >= 6 && mx < 6 + COIN_W && my >= y && my < y + COIN_H) {
                        selectedCoin = coin;
                        return true;
                    }
                }
            }
            case SHOP -> {
                Item item = itemAt(mx, my);
                if (item != null) {
                    selectedItem = item;
                    return true;
                }
            }
            case SERVICE -> {
                int slot = serviceSlotAt(mx, my);
                if (slot >= 0) {
                    serviceSlot = slot;
                    return true;
                }
            }
            case STATS -> {
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tab == Tab.SHOP) {
            int rows = (catalog.get(category).size() + COLS - 1) / COLS;
            scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, Math.max(0, rows - VISIBLE_ROWS));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        boolean typing = (amountBox != null && amountBox.isFocused()) || (quantityBox != null && quantityBox.isFocused());
        if (!typing && ModKeys.WALLET.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private @Nullable Item itemAt(int mx, int my) {
        if (mx < GRID_X || my < GRID_Y) return null;
        int col = (mx - GRID_X) / CELL, row = (my - GRID_Y) / CELL;
        if (col >= COLS || row >= VISIBLE_ROWS) return null;
        int index = (row + scroll) * COLS + col;
        List<Item> items = catalog.get(category);
        return index < items.size() ? items.get(index) : null;
    }

    private static int serviceSlotAt(int mx, int my) {
        if (mx < SERVICE_X || my < SERVICE_Y) return -1;
        int col = (mx - SERVICE_X) / CELL, row = (my - SERVICE_Y) / CELL;
        if (col >= SERVICE_COLS || row >= 4) return -1;
        return inventorySlot(row, col);
    }

    /** Inventory index for a service grid cell: rows 0-2 = slots 9-35, row 3 = hotbar 0-8. */
    private static int inventorySlot(int row, int col) {
        return row < 3 ? 9 + row * SERVICE_COLS + col : col;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ================================================================== rendering

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        Ui.window(g, left, top, W, H);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        // header: title, date, balance
        Component balance = Component.literal(Money.format(ClientEconomy.cents()));
        int chipLeft = Ui.chip(g, font, balance, left + W - 5, top + 3, Theme.OK, 110);
        String date = Calendar.format(ClientEconomy.today());
        Ui.fitRight(g, font, date, chipLeft - 6, top + 5, 80, Theme.MUTED);
        Ui.fit(g, font, title, left + 6, top + 5, chipLeft - 92 - left, Theme.TEXT);
        switch (tab) {
            case WALLET -> extractWallet(g);
            case SHOP -> extractShop(g, mouseX, mouseY);
            case SERVICE -> extractService(g, mouseX, mouseY);
            case STATS -> extractStats(g, mouseX, mouseY);
        }
    }

    // ------------------------------------------------------------------ wallet

    private void extractWallet(GuiGraphicsExtractor g) {
        double today = today();
        for (Coin coin : Coin.values()) {
            int x = left + 6, y = top + TOP + coin.ordinal() * (COIN_H + 2);
            Ui.panel(g, x, y, COIN_W, COIN_H);
            if (coin == selectedCoin) g.outline(x, y, COIN_W, COIN_H, coin.color());
            g.fill(x + 1, y + 1, x + 3, y + COIN_H - 1, coin.color());
            double held = ClientEconomy.coins(coin), price = ClientEconomy.price(coin), change = ClientEconomy.change(coin);
            Ui.fit(g, font, Component.translatable(coin.translationKey()), x + 7, y + 4, 70, coin.color());
            Ui.fit(g, font, Money.formatCoin(held, coin), x + 7, y + 15, 90, Theme.TEXT);
            if (!PriceHistory.launched(coin, today)) {
                Ui.fitRight(g, font, Component.translatable("gui.datacenterextended.wallet.launches",
                        Calendar.format(PriceHistory.launchDate(coin))), x + COIN_W - 4, y + 9, 90, Theme.DIM);
                continue;
            }
            Ui.fitRight(g, font, Money.formatPrice(price), x + COIN_W - 4, y + 4, 64, Theme.TEXT);
            String changeText = String.format(Locale.ROOT, "%s%.1f%%", change >= 0 ? "▲" : "▼", Math.abs(change * 100));
            Ui.fit(g, font, changeText, x + 80, y + 4, 36, change >= 0 ? Theme.OK : Theme.ERR);
            Ui.fitRight(g, font, "≈ " + Money.format((long) Math.floor(held * price * 100)), x + COIN_W - 4, y + 15, 70, Theme.OK);
        }
        Ui.fit(g, font, Component.translatable("gui.datacenterextended.wallet.sell_label"), left + 180, top + TOP - 1 + 0, 100, Theme.DIM);
        extractChart(g);
        String fee = Component.translatable("gui.datacenterextended.wallet.fee",
                Math.round(Config.safe(Config.SELL_FEE, 0.02) * 1000) / 10.0, selectedCoin.symbol()).getString();
        Ui.fitRight(g, font, fee, left + W - 6, top + 207, 110, Theme.MUTED);
        Ui.fit(g, font, Component.translatable("gui.datacenterextended.wallet.hint"), left + 6, top + 221, W - 12, Theme.DIM);
    }

    private void extractChart(GuiGraphicsExtractor g) {
        int x0 = left + CHART_X, y0 = top + CHART_Y;
        Ui.inset(g, x0, y0, CHART_W, CHART_H);
        List<Double> history = ClientEconomy.history(selectedCoin);
        Ui.fit(g, font, Component.translatable("gui.datacenterextended.wallet.chart", selectedCoin.symbol()), x0 + 4, y0 + 3, 120, Theme.MUTED);
        if (history.size() < 2) {
            Ui.fit(g, font, Component.translatable("gui.datacenterextended.wallet.no_history"), x0 + 4, y0 + 28, CHART_W - 8, Theme.DIM);
            extractHalving(g, x0 + 4, y0 + CHART_H - 10);
            return;
        }
        double min = history.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = history.stream().mapToDouble(Double::doubleValue).max().orElse(1);
        if (max - min < 1e-12) max = min + 1e-12;
        int plotTop = y0 + 13, plotH = CHART_H - 26, plotW = CHART_W - 8;
        for (int i = 1; i < 4; i++) g.fill(x0 + 4, plotTop + plotH * i / 4, x0 + 4 + plotW, plotTop + plotH * i / 4 + 1, 0xFF151C24);
        int prevY = -1;
        for (int i = 0; i < history.size(); i++) {
            int px = x0 + 4 + (int) ((long) i * (plotW - 1) / Math.max(1, history.size() - 1));
            int py = plotTop + plotH - 1 - (int) Math.round((history.get(i) - min) / (max - min) * (plotH - 1));
            if (prevY >= 0) g.fill(px, Math.min(prevY, py), px + 1, Math.max(prevY, py) + 1, selectedCoin.color());
            g.fill(px, py + 1, px + 1, plotTop + plotH, (selectedCoin.color() & 0x00FFFFFF) | 0x22000000);
            prevY = py;
        }
        Ui.fitRight(g, font, Money.formatPrice(max), x0 + CHART_W - 4, y0 + 3, 80, Theme.MUTED);
        Ui.fitRight(g, font, Money.formatPrice(min), x0 + CHART_W - 4, y0 + CHART_H - 10, 80, Theme.MUTED);
        extractHalving(g, x0 + 4, y0 + CHART_H - 10);
    }

    /** "Reward 1/4 · halving 20 Apr 2024 (in 2h 3m)" for the selected coin. */
    private void extractHalving(GuiGraphicsExtractor g, int x, int y) {
        LocalDate next = ClientEconomy.nextHalving(selectedCoin);
        if (selectedCoin != Coin.BITCOIN && !(selectedCoin == Coin.ETHEREUM)) return;
        Component text;
        if (selectedCoin == Coin.ETHEREUM) {
            text = PriceHistory.afterMerge(today())
                    ? Component.translatable("gui.datacenterextended.wallet.merged", Math.round(selectedCoin.postMergeReward() * 100))
                    : Component.translatable("gui.datacenterextended.wallet.merge_on", Calendar.format(PriceHistory.THE_MERGE));
        } else {
            String reward = "1/" + (1L << Math.min(62, ClientEconomy.halvings(selectedCoin)));
            text = next == null ? Component.translatable("gui.datacenterextended.wallet.reward_final", reward)
                    : Component.translatable("gui.datacenterextended.wallet.reward", reward, Calendar.format(next),
                    DataUnits.formatDuration(ClientEconomy.secondsToNextHalving(selectedCoin)));
        }
        Ui.fit(g, font, text, x, y, CHART_W - 96, Theme.WARN);
    }

    // ------------------------------------------------------------------ shop

    private void extractShop(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        double today = today();
        List<Item> items = catalog.get(category);
        Item hovered = null;
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int x = left + GRID_X + col * CELL, y = top + GRID_Y + row * CELL;
                Ui.slot(g, x + 1, y + 1);
                int index = (row + scroll) * COLS + col;
                if (index >= items.size()) continue;
                Item item = items.get(index);
                g.item(new ItemStack(item), x + 1, y + 1);
                if (!ShopPricing.released(item, today)) {
                    g.fill(x + 1, y + 1, x + 17, y + 17, Theme.LOCKED);
                    g.text(font, "⌛", x + 5, y + 5, Theme.MUTED, false);
                }
                if (item == selectedItem) g.outline(x, y, 18, 18, Theme.ACCENT);
                if (Ui.inside(mouseX, mouseY, x, y, 18, 18)) hovered = item;
            }
        }
        int rows = (items.size() + COLS - 1) / COLS;
        if (rows > VISIBLE_ROWS) {
            Ui.fitRight(g, font, (scroll + 1) + "/" + (rows - VISIBLE_ROWS + 1), left + W - 6, top + GRID_Y + VISIBLE_ROWS * CELL + 1, 40, Theme.DIM);
        }

        int dx = left + GRID_X, dy = top + 164, dw = W - GRID_X - 6;
        Ui.panel(g, dx, dy, dw, 38);
        if (selectedItem != null) {
            ItemStack stack = new ItemStack(selectedItem);
            g.item(stack, dx + 4, dy + 4);
            Ui.fit(g, font, stack.getHoverName(), dx + 24, dy + 4, dw - 28, Theme.TEXT);
            long price = ShopPricing.priceCents(selectedItem, today);
            LocalDate release = ShopPricing.releaseDate(selectedItem);
            boolean released = ShopPricing.released(selectedItem, today);
            Component info = released
                    ? Component.translatable("gui.datacenterextended.shop.price", Money.format(price))
                    : Component.translatable("gui.datacenterextended.shop.locked", release == null ? "?" : Calendar.format(release));
            Ui.fit(g, font, info, dx + 24, dy + 15, dw - 28, released ? Theme.MUTED : Theme.WARN);
            if (release != null && released) {
                Ui.fit(g, font, Component.translatable("gui.datacenterextended.shop.released", Calendar.format(release)),
                        dx + 24, dy + 26, dw / 2, Theme.DIM);
            }
            long total = price * quantity();
            Ui.fitRight(g, font, Component.translatable("gui.datacenterextended.shop.total", Money.format(total)), dx + dw - 4, dy + 26,
                    dw / 2 - 24, total <= ClientEconomy.cents() ? Theme.OK : Theme.ERR);
        } else {
            Ui.fit(g, font, Component.translatable("gui.datacenterextended.shop.select"), dx + 6, dy + 15, dw - 12, Theme.DIM);
        }
        if (!atShop) Ui.fit(g, font, Component.translatable("gui.datacenterextended.shop.remote"), left + 6, top + 212, 80, Theme.DIM);
        if (hovered != null) {
            List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(minecraft, new ItemStack(hovered)));
            long price = ShopPricing.priceCents(hovered, today);
            lines.add(Component.literal(Money.format(price)).withColor(Theme.OK));
            if (!ShopPricing.released(hovered, today)) {
                LocalDate release = ShopPricing.releaseDate(hovered);
                lines.add(Component.translatable("gui.datacenterextended.shop.locked", release == null ? "?" : Calendar.format(release)).withColor(Theme.WARN));
            }
            g.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }

    // ------------------------------------------------------------------ service

    private void extractService(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (minecraft == null || minecraft.player == null) return;
        double today = today();
        var inventory = minecraft.player.getInventory();
        ItemStack hovered = ItemStack.EMPTY;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < SERVICE_COLS; col++) {
                int slot = inventorySlot(row, col);
                int x = left + SERVICE_X + col * CELL, y = top + SERVICE_Y + row * CELL;
                Ui.slot(g, x + 1, y + 1);
                ItemStack stack = inventory.getItem(slot);
                if (stack.isEmpty()) continue;
                g.item(stack, x + 1, y + 1);
                g.itemDecorations(font, stack, x + 1, y + 1);
                if (ItemValue.resaleCents(stack, today) < 0) {
                    g.fill(x + 1, y + 1, x + 17, y + 17, Theme.LOCKED);
                } else if (ItemValue.repairCents(stack, today) > 0) {
                    double condition = ItemValue.condition(stack);
                    g.fill(x + 1, y + 16, x + 1 + (int) Math.round(16 * condition), y + 17, Theme.condition(condition));
                }
                if (slot == serviceSlot) g.outline(x, y, 18, 18, Theme.ACCENT);
                if (Ui.inside(mouseX, mouseY, x, y, 18, 18)) hovered = stack;
            }
        }
        int hx = left + SERVICE_X + SERVICE_COLS * CELL + 6, hw = W - (hx - left) - 6;
        Ui.panel(g, hx, top + SERVICE_Y, hw, 4 * CELL);
        g.textWithWordWrap(font, Component.translatable("gui.datacenterextended.service.help"), hx + 4, top + SERVICE_Y + 4, hw - 8, Theme.MUTED, false);

        int dx = left + 6, dy = top + 124, dw = W - 12;
        Ui.inset(g, dx, dy, dw, 76);
        ItemStack stack = serviceStack();
        if (stack.isEmpty()) {
            Ui.fit(g, font, Component.translatable("gui.datacenterextended.service.select"), dx + 6, dy + 6, dw - 12, Theme.DIM);
        } else {
            g.item(stack, dx + 5, dy + 5);
            Ui.fit(g, font, stack.getHoverName(), dx + 26, dy + 5, dw - 32, Theme.TEXT);
            long resale = ItemValue.resaleCents(stack, today);
            long repair = ItemValue.repairCents(stack, today);
            int count = stack.getCount();
            if (resale < 0) {
                Ui.fit(g, font, Component.translatable("gui.datacenterextended.service.not_sellable"), dx + 26, dy + 17, dw - 32, Theme.ERR);
            } else {
                double condition = ItemValue.condition(stack);
                Ui.fit(g, font, Component.translatable("gui.datacenterextended.service.condition", Math.round(condition * 100)),
                        dx + 26, dy + 17, 100, Theme.condition(condition));
                Ui.bar(g, dx + 130, dy + 19, dw - 140, 5, condition, Theme.condition(condition));
                Ui.row(g, font, Component.translatable("gui.datacenterextended.service.resale_label"),
                        Component.literal(Money.format(resale * count)), dx + 6, dy + 36, dw - 12, Theme.OK);
                Component repairValue = repair > 0 ? Component.literal(Money.format(repair * count))
                        : Component.translatable("gui.datacenterextended.service.no_repair");
                Ui.row(g, font, Component.translatable("gui.datacenterextended.service.repair_label"), repairValue,
                        dx + 6, dy + 48, dw - 12, repair > 0 ? Theme.WARN : Theme.MUTED);
            }
        }
        if (!atShop) Ui.fit(g, font, Component.translatable("gui.datacenterextended.shop.remote"), left + 6, top + 212, 120, Theme.DIM);
        if (!hovered.isEmpty()) g.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
    }

    // ------------------------------------------------------------------ stats

    private void extractStats(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        StatsLedger.Series series = ClientStats.get(scale);
        long income = 0, expenses = 0, sales = 0;
        if (series != null) {
            for (long[] b : series.values()) {
                income += b[StatsLedger.Category.HOSTING.ordinal()] + b[StatsLedger.Category.MINING.ordinal()]
                        + b[StatsLedger.Category.GRID.ordinal()];
                expenses += b[StatsLedger.Category.BILLS.ordinal()] + b[StatsLedger.Category.SHOP.ordinal()];
                sales += b[StatsLedger.Category.SALES.ordinal()];
            }
        }
        int cw = (W - 12 - 8) / 3, cy = top + 55;
        statCard(g, left + 6, cy, cw, Component.translatable("gui.datacenterextended.stats.income"), Money.format(income), Theme.OK);
        statCard(g, left + 10 + cw, cy, cw, Component.translatable("gui.datacenterextended.stats.expenses"), Money.format(expenses), Theme.ERR);
        long profit = income - expenses;
        statCard(g, left + 14 + 2 * cw, cy, cw, Component.translatable("gui.datacenterextended.stats.profit"), Money.format(profit),
                profit >= 0 ? Theme.ACCENT : Theme.ERR);

        int x0 = left + STATS_X, y0 = top + STATS_Y + 14;
        Ui.inset(g, x0, y0, STATS_W, STATS_H);
        if (series == null) {
            Ui.fitCentered(g, font, Component.translatable("gui.datacenterextended.stats.loading"), x0 + STATS_W / 2, y0 + STATS_H / 2 - 4, STATS_W - 8, Theme.DIM);
            return;
        }
        long[][] v = series.values();
        long maxUp = 1, maxDown = 1;
        for (long[] b : v) {
            maxUp = Math.max(maxUp, b[0] + b[1] + b[GRID]);
            maxDown = Math.max(maxDown, b[3] + b[4]);
        }
        int plotTop = y0 + 4, plotH = STATS_H - 18;
        int zeroY = plotTop + (int) Math.round(plotH * (double) maxUp / (maxUp + maxDown));
        g.fill(x0 + 2, zeroY, x0 + STATS_W - 2, zeroY + 1, Theme.BORDER_LIGHT);
        int n = v.length;
        double slot = (STATS_W - 8) / (double) n;
        int barW = Math.max(2, (int) (slot * 0.7));
        double scaleUp = (zeroY - plotTop) / (double) maxUp, scaleDown = (plotTop + plotH - zeroY - 1) / (double) maxDown;
        int hoveredBar = -1;
        int prevNetY = Integer.MIN_VALUE, prevNetX = 0;
        for (int i = 0; i < n; i++) {
            int bx = x0 + 4 + (int) (i * slot + (slot - barW) / 2);
            long[] b = v[i];
            int hostH = (int) Math.round(b[0] * scaleUp), mineH = (int) Math.round(b[1] * scaleUp);
            int billH = (int) Math.round(b[3] * scaleDown), shopH = (int) Math.round(b[4] * scaleDown);
            if (hostH > 0) g.fill(bx, zeroY - hostH, bx + barW, zeroY, Theme.OK);
            if (mineH > 0) g.fill(bx, zeroY - hostH - mineH, bx + barW, zeroY - hostH, 0xFFF7931A);
            int gridH = (int) Math.round(b[GRID] * scaleUp);
            if (gridH > 0) g.fill(bx, zeroY - hostH - mineH - gridH, bx + barW, zeroY - hostH - mineH, GRID_COLOR);
            if (billH > 0) g.fill(bx, zeroY + 1, bx + barW, zeroY + 1 + billH, Theme.ERR);
            if (shopH > 0) g.fill(bx, zeroY + 1 + billH, bx + barW, zeroY + 1 + billH + shopH, 0xFFA78BFA);
            long net = b[0] + b[1] + b[GRID] - b[3] - b[4];
            int netY = net >= 0 ? zeroY - (int) Math.round(net * scaleUp) : zeroY + (int) Math.round(-net * scaleDown);
            int cx = bx + barW / 2;
            if (prevNetY != Integer.MIN_VALUE) line(g, prevNetX, prevNetY, cx, netY, Theme.ACCENT);
            prevNetY = netY;
            prevNetX = cx;
            if (Ui.inside(mouseX, mouseY, (int) (x0 + 4 + i * slot), plotTop, (int) Math.ceil(slot), plotH)) hoveredBar = i;
        }
        // x axis labels: first, middle, last bucket
        for (int i : new int[]{0, n / 2, n - 1}) {
            String label = bucketLabel(series, i);
            int cx = x0 + 4 + (int) (i * slot + slot / 2);
            Ui.fitCentered(g, font, Component.literal(label), Mth.clamp(cx, x0 + 24, x0 + STATS_W - 24), y0 + STATS_H - 10, 50, Theme.DIM);
        }
        // legend
        int ly = top + STATS_Y + STATS_H + 18;
        legend(g, left + 6, ly, Theme.OK, Component.translatable("gui.datacenterextended.stats.hosting"));
        legend(g, left + 62, ly, 0xFFF7931A, Component.translatable("gui.datacenterextended.stats.mining"));
        legend(g, left + 118, ly, GRID_COLOR, Component.translatable("gui.datacenterextended.stats.grid"));
        legend(g, left + 174, ly, Theme.ERR, Component.translatable("gui.datacenterextended.stats.bills"));
        legend(g, left + 230, ly, 0xFFA78BFA, Component.translatable("gui.datacenterextended.stats.shop"));
        Ui.fit(g, font, Component.translatable("gui.datacenterextended.stats.sales", Money.format(sales)), left + 6, ly + 12, W - 12, Theme.DIM);

        if (hoveredBar >= 0) {
            long[] b = v[hoveredBar];
            List<Component> lines = List.of(
                    Component.literal(bucketLabel(series, hoveredBar)).withColor(Theme.ACCENT),
                    Component.translatable("gui.datacenterextended.stats.hosting").append(": " + Money.format(b[0])).withColor(Theme.OK),
                    Component.translatable("gui.datacenterextended.stats.mining").append(": " + Money.format(b[1])).withColor(0xFFF7931A),
                    Component.translatable("gui.datacenterextended.stats.grid").append(": " + Money.format(b[GRID])).withColor(GRID_COLOR),
                    Component.translatable("gui.datacenterextended.stats.bills").append(": -" + Money.format(b[3])).withColor(Theme.ERR),
                    Component.translatable("gui.datacenterextended.stats.shop").append(": -" + Money.format(b[4])).withColor(0xFFA78BFA),
                    Component.translatable("gui.datacenterextended.stats.profit").append(": " + Money.format(b[0] + b[1] + b[GRID] - b[3] - b[4])).withColor(Theme.TEXT));
            g.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }

    private void statCard(GuiGraphicsExtractor g, int x, int y, int w, Component label, String value, int color) {
        Ui.panel(g, x, y, w, 26);
        Ui.fit(g, font, label, x + 5, y + 4, w - 10, Theme.MUTED);
        Ui.fit(g, font, value, x + 5, y + 14, w - 10, color);
    }

    private void legend(GuiGraphicsExtractor g, int x, int y, int color, Component label) {
        g.fill(x, y + 1, x + 6, y + 7, color);
        Ui.fit(g, font, label, x + 9, y, 45, Theme.MUTED);
    }

    private static void line(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int s = 0; s <= steps; s++) {
            int x = x0 + (steps == 0 ? 0 : (x1 - x0) * s / steps);
            int y = y0 + (steps == 0 ? 0 : (y1 - y0) * s / steps);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static String bucketLabel(StatsLedger.Series series, int i) {
        return switch (series.scale()) {
            case DAY -> {
                long hour = series.firstKey() + i;
                yield String.format(Locale.ROOT, "%02d:00", Math.floorMod(hour, 24));
            }
            case WEEK, MONTH -> {
                LocalDate d = LocalDate.ofEpochDay(series.firstKey() + i);
                yield d.getDayOfMonth() + "." + d.getMonthValue() + ".";
            }
            case YEAR -> Calendar.formatMonth(LocalDate.ofEpochDay(series.firstKey()).plusMonths(i));
        };
    }
}
