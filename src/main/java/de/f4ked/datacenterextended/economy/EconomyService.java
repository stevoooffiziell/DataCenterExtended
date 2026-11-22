package de.f4ked.datacenterextended.economy;

import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.Datacenterextended;
import de.f4ked.datacenterextended.advancement.ModAdvancements;
import de.f4ked.datacenterextended.item.PrebuiltServerItem;
import de.f4ked.datacenterextended.registry.ModItems;
import de.f4ked.datacenterextended.time.CalendarData;
import de.f4ked.datacenterextended.block.HardwareShopBlock;
import de.f4ked.datacenterextended.network.ModPayloads;
import de.f4ked.datacenterextended.registry.ModDataMaps;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Prediction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-side economy: market ticks, wallet syncing, selling coins and buying from the shop. */
@EventBusSubscriber(modid = Datacenterextended.MODID)
public final class EconomyService {
    public static final double SHOP_REACH = 8.0;

    /** Where each player last opened a Hardware Shop block (allows buying there even with remoteShop = false). */
    private static final Map<UUID, ShopVisit> SHOP_VISITS = new HashMap<>();

    private record ShopVisit(ResourceKey<Level> dimension, BlockPos pos) {
    }

    public enum BuyResult { OK, NOT_FOR_SALE, NOT_RELEASED, NO_SHOP_ACCESS, TOO_EXPENSIVE, INVALID }

    private EconomyService() {
    }

    // ================================================================== events

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int interval = Config.safe(Config.MARKET_UPDATE_TICKS, 1200);
        CalendarData calendar = CalendarData.get(server);
        calendar.tick();
        double today = calendar.epochDay();
        boolean marketChanged = false;
        MarketData market = MarketData.get(server);
        if (server.getTickCount() % interval == 0) {
            market.update(today);
            marketChanged = true;
        } else if (server.getTickCount() % 20 == 0) {
            market.refresh(today);
        }
        if (server.getTickCount() % 20 == 0) {
            for (Coin coin : market.newHalvings(today)) {
                announceHalving(server, coin, MarketData.halvings(coin, today));
                marketChanged = true;
            }
        }
        // keep the clients' date display in step every few seconds
        if (server.getTickCount() % 200 == 0) marketChanged = true;
        if (marketChanged) {
            ModPayloads.MarketSync payload = marketPayload(server);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) send(player, payload);
        }
        WalletData wallets = WalletData.get(server);
        for (UUID id : wallets.drainPendingSync()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) continue;
            Wallet wallet = wallets.wallet(id);
            send(player, walletPayload(wallet));
            if (wallet.cents() >= 1_000_000L) ModAdvancements.award(player, ModAdvancements.TEN_THOUSAND);
            if (wallet.cents() >= 100_000_000L) ModAdvancements.award(player, ModAdvancements.MILLIONAIRE);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MinecraftServer server = player.level().getServer();
            WalletData wallets = WalletData.get(server);
            boolean firstJoin = !wallets.has(player.getUUID());
            Wallet wallet = wallets.wallet(player.getUUID());
            send(player, walletPayload(wallet));
            send(player, marketPayload(server));
            if (firstJoin && Config.safe(Config.GIVE_HANDBOOK, true)) {
                ItemStack book = new ItemStack(ModItems.HANDBOOK.get());
                if (!player.getInventory().add(book)) player.drop(book, false, Prediction.SERVER_ONLY);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SHOP_VISITS.remove(event.getEntity().getUUID());
    }

    /** Sends only to clients that have this mod (vanilla clients and fake players lack the channel). */
    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection != null && player.connection.hasChannel(payload)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public static ModPayloads.WalletSync walletPayload(Wallet wallet) {
        List<Double> coins = new ArrayList<>();
        for (double c : wallet.coinsCopy()) coins.add(c);
        return new ModPayloads.WalletSync(wallet.cents(), coins);
    }

    public static ModPayloads.MarketSync marketPayload(MinecraftServer server) {
        MarketData market = MarketData.get(server);
        double today = CalendarData.now(server);
        List<Double> prices = new ArrayList<>();
        for (double p : market.pricesCopy()) prices.add(p);
        List<List<Double>> history = new ArrayList<>();
        for (List<Double> h : market.history()) history.add(List.copyOf(h));
        List<Integer> halvings = new ArrayList<>();
        List<Long> next = new ArrayList<>();
        for (Coin coin : Coin.values()) {
            halvings.add(MarketData.halvings(coin, today));
            java.time.LocalDate nextHalving = PriceHistory.nextHalving(coin, today);
            next.add(nextHalving == null ? -1L : nextHalving.toEpochDay());
        }
        return new ModPayloads.MarketSync(prices, history, halvings, next, today, Config.safe(Config.TICKS_PER_DAY, 2400));
    }

    /** Sends the player's statistics for one scale (Stats tab). */
    public static void sendStats(ServerPlayer player, int scaleId) {
        MinecraftServer server = player.level().getServer();
        StatsLedger.Series series = StatsData.get(server).series(player.getUUID(), StatsLedger.Scale.byId(scaleId), CalendarData.now(server));
        List<Long> flat = new ArrayList<>();
        for (long[] bucket : series.values()) for (long v : bucket) flat.add(v);
        send(player, new ModPayloads.StatsSync(series.scale().ordinal(), series.firstKey(), flat));
    }

    private static void announceHalving(MinecraftServer server, Coin coin, int halvings) {
        Component message = Component.translatable("message.datacenterextended.halving", Component.translatable(coin.translationKey()),
                "1/" + (1L << Math.min(62, halvings))).withStyle(ChatFormatting.GOLD);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.connection != null) {
                player.sendSystemMessage(message);
                ModAdvancements.award(player, ModAdvancements.HALVING);
            }
        }
    }

    // ================================================================== selling

    /** Sells coins at the current price minus the fee. Returns the cents paid, or -1 if the player doesn't hold that much. */
    public static long sell(ServerPlayer player, Coin coin, double amount) {
        MinecraftServer server = player.level().getServer();
        if (!(amount > 0) || Double.isInfinite(amount)) return -1;
        WalletData wallets = WalletData.get(server);
        double held = wallets.wallet(player.getUUID()).coins(coin);
        amount = Math.min(amount, held); // "sell all" may be a hair above due to float rounding on the client
        if (MarketData.get(server).price(coin) <= 0) return -1; // not launched yet
        long cents = saleValue(coin, amount, MarketData.get(server).price(coin));
        if (amount <= 0 || !wallets.withdrawCoins(player.getUUID(), coin, amount)) return -1;
        wallets.deposit(player.getUUID(), cents);
        StatsData.record(server, player.getUUID(), StatsLedger.Category.SALES, cents);
        player.sendOverlayMessage(Component.translatable("message.datacenterextended.sold_coin",
                Money.formatCoin(amount, coin), Money.format(cents)).withStyle(ChatFormatting.GREEN));
        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.4F, 1.4F);
        return cents;
    }

    /** Cents paid for {@code amount} coins at {@code price}, after the fee. */
    public static long saleValue(Coin coin, double amount, double price) {
        return (long) Math.floor(amount * price * (1.0 - Config.safe(Config.SELL_FEE, 0.02)) * 100.0);
    }

    // ================================================================== shop

    /** Price in cents at the calendar date, or -1 if the item is not sold. */
    public static long priceCents(Item item, double epochDay) {
        return ShopPricing.priceCents(item, epochDay);
    }

    public static void rememberShop(Player player, Level level, BlockPos pos) {
        SHOP_VISITS.put(player.getUUID(), new ShopVisit(level.dimension(), pos.immutable()));
    }

    public static boolean canUseShop(ServerPlayer player) {
        if (Config.safe(Config.REMOTE_SHOP, true)) return true;
        ShopVisit visit = SHOP_VISITS.get(player.getUUID());
        return visit != null && visit.dimension() == player.level().dimension()
                && player.level().getBlockState(visit.pos()).getBlock() instanceof HardwareShopBlock
                && visit.pos().closerToCenterThan(player.position(), SHOP_REACH);
    }

    public static BuyResult buy(ServerPlayer player, Item item, int count) {
        if (count < 1 || count > 64) return BuyResult.INVALID;
        double today = CalendarData.now(player.level().getServer());
        long price = priceCents(item, today);
        if (price < 0) return BuyResult.NOT_FOR_SALE;
        if (!ShopPricing.released(item, today)) {
            player.sendOverlayMessage(Component.translatable("message.datacenterextended.shop.not_released",
                    String.valueOf(ShopPricing.releaseDate(item))).withStyle(ChatFormatting.RED));
            return BuyResult.NOT_RELEASED;
        }
        if (!canUseShop(player)) {
            player.sendOverlayMessage(Component.translatable("message.datacenterextended.shop.no_access").withStyle(ChatFormatting.RED));
            return BuyResult.NO_SHOP_ACCESS;
        }
        long total = Math.multiplyExact(price, count);
        WalletData wallets = WalletData.get(player.level().getServer());
        if (!wallets.withdraw(player.getUUID(), total)) {
            player.sendOverlayMessage(Component.translatable("message.datacenterextended.shop.too_expensive", Money.format(total))
                    .withStyle(ChatFormatting.RED));
            return BuyResult.TOO_EXPENSIVE;
        }
        StatsData.record(player.level().getServer(), player.getUUID(), StatsLedger.Category.SHOP, total);
        int remaining = count;
        while (remaining > 0) {
            // prebuilt servers arrive assembled
            ItemStack stack = item instanceof PrebuiltServerItem prebuilt ? prebuilt.prebuilt().assemble()
                    : new ItemStack(item, Math.min(remaining, item.getDefaultMaxStackSize()));
            remaining -= stack.getCount();
            if (!player.getInventory().add(stack) && !stack.isEmpty()) {
                player.drop(stack, false, Prediction.SERVER_ONLY);
            }
        }
        player.sendOverlayMessage(Component.translatable("message.datacenterextended.shop.bought", count,
                item.getName(new ItemStack(item)), Money.format(total)).withStyle(ChatFormatting.GREEN));
        player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 0.5F, 1.2F);
        return BuyResult.OK;
    }

    // ================================================================== service (repair / sell second-hand)

    public enum ServiceResult { OK, NO_SHOP_ACCESS, NOTHING_TO_DO, TOO_EXPENSIVE, INVALID }

    /** Repairs or sells the stack in a slot (0..35) of the player's inventory. */
    public static ServiceResult service(ServerPlayer player, int slot, Item item, boolean repair) {
        if (slot < 0 || slot >= 36) return ServiceResult.INVALID;
        ItemStack stack = player.getInventory().getItem(slot);
        if (stack.isEmpty() || !stack.is(item)) return ServiceResult.INVALID;
        if (!canUseShop(player)) {
            player.sendOverlayMessage(Component.translatable("message.datacenterextended.shop.no_access").withStyle(ChatFormatting.RED));
            return ServiceResult.NO_SHOP_ACCESS;
        }
        WalletData wallets = WalletData.get(player.level().getServer());
        Component name = stack.getHoverName();
        int count = stack.getCount();
        if (repair) {
            long each = ItemValue.repairCents(stack, CalendarData.now(player.level().getServer()));
            if (each <= 0) return ServiceResult.NOTHING_TO_DO;
            long total = Math.multiplyExact(each, count);
            if (!wallets.withdraw(player.getUUID(), total)) {
                player.sendOverlayMessage(Component.translatable("message.datacenterextended.shop.too_expensive", Money.format(total))
                        .withStyle(ChatFormatting.RED));
                return ServiceResult.TOO_EXPENSIVE;
            }
            ItemValue.repair(stack);
            StatsData.record(player.level().getServer(), player.getUUID(), StatsLedger.Category.SHOP, total);
            player.getInventory().setChanged();
            player.sendOverlayMessage(Component.translatable("message.datacenterextended.service.repaired", name, Money.format(total))
                    .withStyle(ChatFormatting.GREEN));
            player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.4F, 1.4F);
            ModAdvancements.award(player, ModAdvancements.REPAIRED);
        } else {
            long each = ItemValue.resaleCents(stack, CalendarData.now(player.level().getServer()));
            if (each < 0) return ServiceResult.INVALID;
            long total = Math.multiplyExact(each, count);
            player.getInventory().setItem(slot, ItemStack.EMPTY);
            wallets.deposit(player.getUUID(), total);
            StatsData.record(player.level().getServer(), player.getUUID(), StatsLedger.Category.SALES, total);
            player.sendOverlayMessage(Component.translatable("message.datacenterextended.service.sold", count, name, Money.format(total))
                    .withStyle(ChatFormatting.GREEN));
            player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.4F, 1.2F);
        }
        return ServiceResult.OK;
    }
}
