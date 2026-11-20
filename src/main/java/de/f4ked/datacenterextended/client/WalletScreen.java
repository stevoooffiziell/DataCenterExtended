package de.f4ked.datacenterextended.client;

import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.economy.Coin;
import de.f4ked.datacenterextended.economy.Money;
import de.f4ked.datacenterextended.hardware.HardwareStats;
import de.f4ked.datacenterextended.hardware.HardwareType;
import de.f4ked.datacenterextended.network.ModPayloads;
import de.f4ked.datacenterextended.registry.ModDataMaps;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Wallet (balance, coins, price chart, selling) and Hardware Shop. Opened with the wallet key or at a Hardware Shop
 * block. All actions are requests; the server validates them and syncs the wallet back.
 */
public class WalletScreen extends Screen {
    private static final int W = 256, H = 212;
    private static final int ROW_Y = 28, ROW_H = 24;
    private static final int CHART_X = 8, CHART_Y = 104, CHART_W = 240, CHART_H = 60;
    private static final int GRID_X = 88, GRID_Y = 28, COLS = 8, VISIBLE_ROWS = 6, CELL = 20;

    private enum Tab { WALLET, SHOP }

    private enum Category {
        MOTHERBOARD, CPU, MEMORY, GPU, PSU, STORAGE, INFRASTRUCTURE;

        Component label() {
            return this == INFRASTRUCTURE
                    ? Component.translatable("gui.datacenterextended.shop.infrastructure")
                    : Component.translatable(HardwareType.valueOf(name()).translationKey());
        }

        static Category of(Item item) {
            HardwareStats stats = ModDataMaps.hardware(new ItemStack(item));
            if (stats == null) return INFRASTRUCTURE;
            return switch (stats.type()) {
                case MOTHERBOARD -> MOTHERBOARD;
                case CPU -> CPU;
                case MEMORY -> MEMORY;
                case GPU -> GPU;
                case PSU -> PSU;
                case STORAGE -> STORAGE;
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
    private int left, top;

    private final List<Button> walletWidgets = new ArrayList<>();
    private final List<Button> shopWidgets = new ArrayList<>();
    private @Nullable EditBox amountBox;
    private @Nullable EditBox quantityBox;
    private @Nullable Button buyButton;

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
            if (ModDataMaps.shopPrice(item) >= 0) catalog.get(Category.of(item)).add(item);
        }
        for (List<Item> items : catalog.values()) {
            items.sort(Comparator.comparingDouble(ModDataMaps::shopPrice));
        }
        for (Category c : Category.values()) {
            if (!catalog.get(c).isEmpty()) {
                category = c;
                break;
            }
        }
    }

    // ================================================================== widgets

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        walletWidgets.clear();
        shopWidgets.clear();

        Button walletTab = addRenderableWidget(Button.builder(Component.translatable("gui.datacenterextended.wallet.tab"),
                b -> switchTab(Tab.WALLET)).bounds(left + 8, top + 6, 60, 16).build());
        Button shopTab = addRenderableWidget(Button.builder(Component.translatable("gui.datacenterextended.shop.tab"),
                b -> switchTab(Tab.SHOP)).bounds(left + 70, top + 6, 60, 16).build());
        shopTab.active = shopAvailable();
        walletTab.active = true;

        // wallet: sell buttons per coin row, amount box + sell button for the selected coin
        for (Coin coin : Coin.values()) {
            int y = top + ROW_Y + coin.ordinal() * ROW_H + 4;
            walletWidgets.add(sellButton("25%", coin, 0.25, left + 172, y));
            walletWidgets.add(sellButton("50%", coin, 0.50, left + 197, y));
            walletWidgets.add(sellButton(Component.translatable("gui.datacenterextended.wallet.all").getString(), coin, 1.0, left + 222, y));
        }
        amountBox = new EditBox(font, left + 8, top + 172, 100, 16, Component.translatable("gui.datacenterextended.wallet.amount"));
        amountBox.setHint(Component.translatable("gui.datacenterextended.wallet.amount").withStyle(ChatFormatting.DARK_GRAY));
        amountBox.setMaxLength(20);
        addRenderableWidget(amountBox);
        walletWidgets.add(addRenderableWidget(Button.builder(Component.translatable("gui.datacenterextended.wallet.sell"), b -> sellTyped())
                .bounds(left + 112, top + 172, 60, 16).build()));

        // shop: categories, quantity, buy
        int i = 0;
        for (Category c : Category.values()) {
            Button button = addRenderableWidget(Button.builder(c.label(), b -> {
                category = c;
                scroll = 0;
                selectedItem = null;
            }).bounds(left + 8, top + 28 + i++ * 18, 76, 16).build());
            button.active = !catalog.get(c).isEmpty();
            shopWidgets.add(button);
        }
        shopWidgets.add(addRenderableWidget(Button.builder(Component.literal("-"), b -> addQuantity(-1))
                .bounds(left + 88, top + 186, 16, 16).build()));
        quantityBox = new EditBox(font, left + 106, top + 186, 28, 16, Component.translatable("gui.datacenterextended.shop.quantity"));
        quantityBox.setValue("1");
        quantityBox.setMaxLength(2);
        addRenderableWidget(quantityBox);
        shopWidgets.add(addRenderableWidget(Button.builder(Component.literal("+"), b -> addQuantity(1))
                .bounds(left + 136, top + 186, 16, 16).build()));
        buyButton = addRenderableWidget(Button.builder(Component.translatable("gui.datacenterextended.shop.buy"), b -> buy())
                .bounds(left + 196, top + 184, 52, 20).build());
        shopWidgets.add(buyButton);

        updateVisibility();
    }

    private Button sellButton(String label, Coin coin, double fraction, int x, int y) {
        return addRenderableWidget(Button.builder(Component.literal(label), b -> {
            double amount = ClientEconomy.coins(coin) * fraction;
            if (amount > 0) ClientPacketDistributor.sendToServer(new ModPayloads.SellCoin(coin.ordinal(), amount));
        }).bounds(x, y, 24, 14).build());
    }

    private void switchTab(Tab tab) {
        if (tab == Tab.SHOP && !shopAvailable()) return;
        this.tab = tab;
        updateVisibility();
    }

    private void updateVisibility() {
        boolean wallet = tab == Tab.WALLET;
        for (Button b : walletWidgets) b.visible = wallet;
        for (Button b : shopWidgets) b.visible = !wallet;
        if (amountBox != null) amountBox.visible = wallet;
        if (quantityBox != null) quantityBox.visible = !wallet;
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

    private static long priceCents(Item item) {
        double dollars = ModDataMaps.shopPrice(item);
        return dollars < 0 ? -1 : Money.toCents(dollars * Config.safe(Config.SHOP_PRICE_MULTIPLIER, 1.0));
    }

    @Override
    public void tick() {
        super.tick();
        if (buyButton != null) {
            long price = selectedItem == null ? -1 : priceCents(selectedItem);
            buyButton.active = price >= 0 && price * quantity() <= ClientEconomy.cents();
        }
    }

    // ================================================================== input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        int mx = (int) event.x() - left, my = (int) event.y() - top;
        if (tab == Tab.WALLET) {
            for (Coin coin : Coin.values()) {
                int y = ROW_Y + coin.ordinal() * ROW_H;
                if (mx >= 8 && mx < 168 && my >= y && my < y + ROW_H - 2) {
                    selectedCoin = coin;
                    return true;
                }
            }
        } else {
            Item item = itemAt(mx, my);
            if (item != null) {
                selectedItem = item;
                return true;
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ================================================================== rendering

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        GuiDraw.window(g, left, top, W, H);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        String balance = Money.format(ClientEconomy.cents());
        g.text(font, balance, left + W - 8 - font.width(balance), top + 10, 0xFF1B5E20, false);
        if (tab == Tab.WALLET) {
            extractWallet(g);
        } else {
            extractShop(g, mouseX, mouseY);
        }
    }

    private void extractWallet(GuiGraphicsExtractor g) {
        for (Coin coin : Coin.values()) {
            int x = left + 8, y = top + ROW_Y + coin.ordinal() * ROW_H;
            GuiDraw.monitor(g, x, y, 160, ROW_H - 3);
            if (coin == selectedCoin) g.outline(x - 1, y - 1, 162, ROW_H - 1, coin.color());
            double held = ClientEconomy.coins(coin), price = ClientEconomy.price(coin), change = ClientEconomy.change(coin);
            g.text(font, Component.translatable(coin.translationKey()), x + 3, y + 2, coin.color(), false);
            g.text(font, Money.formatCoin(held, coin), x + 3, y + 11, GuiDraw.TEXT_INFO, false);
            String priceText = Money.formatPrice(price);
            g.text(font, priceText, x + 157 - font.width(priceText), y + 2, 0xFFFFFFFF, false);
            String changeText = String.format(Locale.ROOT, "%s %.1f%%", change >= 0 ? "▲" : "▼", Math.abs(change * 100));
            String value = "≈ " + Money.format((long) Math.floor(held * price * 100));
            g.text(font, value, x + 157 - font.width(value), y + 11, GuiDraw.TEXT_OK, false);
            g.text(font, changeText, x + 82, y + 2, change >= 0 ? GuiDraw.TEXT_OK : GuiDraw.TEXT_ERR, false);
        }
        extractChart(g);
        String fee = Component.translatable("gui.datacenterextended.wallet.fee",
                Math.round(Config.safe(Config.SELL_FEE, 0.02) * 1000) / 10.0, selectedCoin.symbol()).getString();
        g.text(font, fee, left + 178, top + 176, 0xFF404040, false);
        g.text(font, Component.translatable("gui.datacenterextended.wallet.hint"), left + 8, top + 194, 0xFF606060, false);
    }

    private void extractChart(GuiGraphicsExtractor g) {
        int x0 = left + CHART_X, y0 = top + CHART_Y;
        GuiDraw.monitor(g, x0, y0, CHART_W, CHART_H);
        List<Double> history = ClientEconomy.history(selectedCoin);
        g.text(font, Component.translatable("gui.datacenterextended.wallet.chart", selectedCoin.symbol()), x0 + 3, y0 + 2, 0xFF7F8FA0, false);
        if (history.size() < 2) {
            g.text(font, Component.translatable("gui.datacenterextended.wallet.no_history"), x0 + 3, y0 + 26, 0xFF7F8FA0, false);
            return;
        }
        double min = history.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = history.stream().mapToDouble(Double::doubleValue).max().orElse(1);
        if (max - min < 1e-9) max = min + 1e-9;
        int plotTop = y0 + 12, plotH = CHART_H - 16, plotW = CHART_W - 6;
        int prevY = -1;
        for (int i = 0; i < history.size(); i++) {
            int px = x0 + 3 + (int) ((long) i * (plotW - 1) / Math.max(1, history.size() - 1));
            int py = plotTop + plotH - 1 - (int) Math.round((history.get(i) - min) / (max - min) * (plotH - 1));
            if (prevY >= 0) {
                g.fill(px, Math.min(prevY, py), px + 1, Math.max(prevY, py) + 1, selectedCoin.color());
            } else {
                g.fill(px, py, px + 1, py + 1, selectedCoin.color());
            }
            prevY = py;
        }
        String hi = Money.formatPrice(max), lo = Money.formatPrice(min);
        g.text(font, hi, x0 + CHART_W - 3 - font.width(hi), y0 + 2, 0xFF7F8FA0, false);
        g.text(font, lo, x0 + CHART_W - 3 - font.width(lo), y0 + CHART_H - 9, 0xFF7F8FA0, false);
    }

    private void extractShop(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        List<Item> items = catalog.get(category);
        Item hovered = null;
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int x = left + GRID_X + col * CELL, y = top + GRID_Y + row * CELL;
                GuiDraw.slot(g, x + 1, y + 1);
                int index = (row + scroll) * COLS + col;
                if (index >= items.size()) continue;
                Item item = items.get(index);
                if (item == selectedItem) g.outline(x, y, 18, 18, 0xFFFFD54F);
                g.item(new ItemStack(item), x + 1, y + 1);
                if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) hovered = item;
            }
        }
        int rows = (items.size() + COLS - 1) / COLS;
        if (rows > VISIBLE_ROWS) {
            String page = (scroll + 1) + "/" + (rows - VISIBLE_ROWS + 1);
            g.text(font, page, left + 248 - font.width(page), top + 150, 0xFF404040, false);
        }

        // detail panel
        int dx = left + 88, dy = top + 152;
        if (selectedItem != null) {
            ItemStack stack = new ItemStack(selectedItem);
            g.item(stack, dx, dy + 6);
            g.text(font, stack.getHoverName(), dx + 20, dy + 2, 0xFF202020, false);
            long price = priceCents(selectedItem);
            long total = price * quantity();
            g.text(font, Component.translatable("gui.datacenterextended.shop.price", Money.format(price)), dx + 20, dy + 13, 0xFF404040, false);
            g.text(font, Component.translatable("gui.datacenterextended.shop.total", Money.format(total)), left + 156, top + 190,
                    total <= ClientEconomy.cents() ? 0xFF1B5E20 : 0xFFB71C1C, false);
        } else {
            g.text(font, Component.translatable("gui.datacenterextended.shop.select"), dx, dy + 6, 0xFF606060, false);
        }
        if (!atShop) {
            g.text(font, Component.translatable("gui.datacenterextended.shop.remote"), left + 8, top + 160, 0xFF606060, false);
        }
        if (hovered != null) {
            g.setTooltipForNextFrame(font, new ItemStack(hovered), mouseX, mouseY);
        }
    }
}
