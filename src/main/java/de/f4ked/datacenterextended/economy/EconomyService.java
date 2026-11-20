package de.f4ked.datacenterextended.economy;

import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.Datacenterextended;
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

    public enum BuyResult { OK, NOT_FOR_SALE, NO_SHOP_ACCESS, TOO_EXPENSIVE, INVALID }

    private EconomyService() {
    }

    // ================================================================== events

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int interval = Config.safe(Config.MARKET_UPDATE_TICKS, 1200);
        if (server.getTickCount() % interval == 0) {
            MarketData market = MarketData.get(server);
            market.update();
            ModPayloads.MarketSync payload = marketPayload(market);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) send(player, payload);
        }
        WalletData wallets = WalletData.get(server);
        for (UUID id : wallets.drainPendingSync()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) send(player, walletPayload(wallets.wallet(id)));
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MinecraftServer server = player.level().getServer();
            Wallet wallet = WalletData.get(server).wallet(player.getUUID());
            send(player, walletPayload(wallet));
            send(player, marketPayload(MarketData.get(server)));
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

    public static ModPayloads.MarketSync marketPayload(MarketData market) {
        List<Double> prices = new ArrayList<>();
        for (double p : market.pricesCopy()) prices.add(p);
        List<List<Double>> history = new ArrayList<>();
        for (List<Double> h : market.history()) history.add(List.copyOf(h));
        return new ModPayloads.MarketSync(prices, history);
    }

    // ================================================================== selling

    /** Sells coins at the current price minus the fee. Returns the cents paid, or -1 if the player doesn't hold that much. */
    public static long sell(ServerPlayer player, Coin coin, double amount) {
        MinecraftServer server = player.level().getServer();
        if (!(amount > 0) || Double.isInfinite(amount)) return -1;
        WalletData wallets = WalletData.get(server);
        double held = wallets.wallet(player.getUUID()).coins(coin);
        amount = Math.min(amount, held); // "sell all" may be a hair above due to float rounding on the client
        long cents = saleValue(coin, amount, MarketData.get(server).price(coin));
        if (amount <= 0 || !wallets.withdrawCoins(player.getUUID(), coin, amount)) return -1;
        wallets.deposit(player.getUUID(), cents);
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

    /** Price in cents including the multiplier, or -1 if the item is not sold. */
    public static long priceCents(Item item) {
        double dollars = ModDataMaps.shopPrice(item);
        if (dollars < 0) return -1;
        return Money.toCents(dollars * Config.safe(Config.SHOP_PRICE_MULTIPLIER, 1.0));
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
        long price = priceCents(item);
        if (price < 0) return BuyResult.NOT_FOR_SALE;
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
        int remaining = count;
        while (remaining > 0) {
            ItemStack stack = new ItemStack(item, Math.min(remaining, item.getDefaultMaxStackSize()));
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
}
