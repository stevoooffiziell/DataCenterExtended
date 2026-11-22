package de.f4ked.datacenterextended.network;

import de.f4ked.datacenterextended.Datacenterextended;
import de.f4ked.datacenterextended.client.ClientPayloadHandler;
import de.f4ked.datacenterextended.economy.Coin;
import de.f4ked.datacenterextended.economy.EconomyService;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

/** Wallet, market and shop packets. Buy/sell requests are validated on the server. */
public final class ModPayloads {
    private static final StreamCodec<ByteBuf, List<Double>> DOUBLES = ByteBufCodecs.DOUBLE.apply(ByteBufCodecs.list());

    private ModPayloads() {
    }

    // ------------------------------------------------------------------ server -> client

    /** The receiving player's wallet. */
    public record WalletSync(long cents, List<Double> coins) implements CustomPacketPayload {
        public static final Type<WalletSync> TYPE = new Type<>(Datacenterextended.id("wallet_sync"));
        public static final StreamCodec<ByteBuf, WalletSync> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, WalletSync::cents, DOUBLES, WalletSync::coins, WalletSync::new);

        @Override
        public Type<WalletSync> type() {
            return TYPE;
        }
    }

    /** Current prices and the price history of every coin. */
    /** Current prices, the price history and the halving state (count, epoch day of the next one or -1) and the calendar time of every coin. */
    public record MarketSync(List<Double> prices, List<List<Double>> history, List<Integer> halvings,
                             List<Long> nextHalving, double epochDay, int ticksPerDay) implements CustomPacketPayload {
        public static final Type<MarketSync> TYPE = new Type<>(Datacenterextended.id("market_sync"));
        public static final StreamCodec<ByteBuf, MarketSync> CODEC = StreamCodec.composite(
                DOUBLES, MarketSync::prices, DOUBLES.apply(ByteBufCodecs.list()), MarketSync::history,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), MarketSync::halvings,
                ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()), MarketSync::nextHalving,
                ByteBufCodecs.DOUBLE, MarketSync::epochDay, ByteBufCodecs.VAR_INT, MarketSync::ticksPerDay, MarketSync::new);

        @Override
        public Type<MarketSync> type() {
            return TYPE;
        }
    }

    /** Opens the wallet screen; {@code shop} = opened at a Hardware Shop block (shop tab, always allowed). */
    public record OpenWallet(boolean shop) implements CustomPacketPayload {
        public static final Type<OpenWallet> TYPE = new Type<>(Datacenterextended.id("open_wallet"));
        public static final StreamCodec<ByteBuf, OpenWallet> CODEC = ByteBufCodecs.BOOL.map(OpenWallet::new, OpenWallet::shop);

        @Override
        public Type<OpenWallet> type() {
            return TYPE;
        }
    }

    // ------------------------------------------------------------------ client -> server

    public record SellCoin(int coin, double amount) implements CustomPacketPayload {
        public static final Type<SellCoin> TYPE = new Type<>(Datacenterextended.id("sell_coin"));
        public static final StreamCodec<ByteBuf, SellCoin> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SellCoin::coin, ByteBufCodecs.DOUBLE, SellCoin::amount, SellCoin::new);

        @Override
        public Type<SellCoin> type() {
            return TYPE;
        }
    }

    public record BuyItem(Item item, int count) implements CustomPacketPayload {
        public static final Type<BuyItem> TYPE = new Type<>(Datacenterextended.id("buy_item"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BuyItem> CODEC = StreamCodec.composite(
                ByteBufCodecs.registry(Registries.ITEM), BuyItem::item, ByteBufCodecs.VAR_INT, BuyItem::count, BuyItem::new);

        @Override
        public Type<BuyItem> type() {
            return TYPE;
        }
    }

    /** Repair or sell the item in a slot of the player's inventory (Service tab). {@code item} guards against races. */
    public record ServiceItem(int slot, Item item, boolean repair) implements CustomPacketPayload {
        public static final Type<ServiceItem> TYPE = new Type<>(Datacenterextended.id("service_item"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServiceItem> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ServiceItem::slot, ByteBufCodecs.registry(Registries.ITEM), ServiceItem::item,
                ByteBufCodecs.BOOL, ServiceItem::repair, ServiceItem::new);

        @Override
        public Type<ServiceItem> type() {
            return TYPE;
        }
    }

    /** A part of one of the player's racks dropped below the warning threshold or broke (shown as a toast). */
    public record WearWarning(ItemStack part, float condition, boolean broke, String where) implements CustomPacketPayload {
        public static final Type<WearWarning> TYPE = new Type<>(Datacenterextended.id("wear_warning"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WearWarning> CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, WearWarning::part, ByteBufCodecs.FLOAT, WearWarning::condition,
                ByteBufCodecs.BOOL, WearWarning::broke, ByteBufCodecs.STRING_UTF8, WearWarning::where, WearWarning::new);

        @Override
        public Type<WearWarning> type() {
            return TYPE;
        }
    }

    /** Statistics of the player for one scale: buckets x categories flattened, oldest first. */
    public record StatsSync(int scale, long firstKey, List<Long> values) implements CustomPacketPayload {
        public static final Type<StatsSync> TYPE = new Type<>(Datacenterextended.id("stats_sync"));
        public static final StreamCodec<ByteBuf, StatsSync> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, StatsSync::scale, ByteBufCodecs.VAR_LONG, StatsSync::firstKey,
                ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()), StatsSync::values, StatsSync::new);

        @Override
        public Type<StatsSync> type() {
            return TYPE;
        }
    }

    /** Asks for {@link StatsSync} of one scale (Stats tab). */
    public record RequestStats(int scale) implements CustomPacketPayload {
        public static final Type<RequestStats> TYPE = new Type<>(Datacenterextended.id("request_stats"));
        public static final StreamCodec<ByteBuf, RequestStats> CODEC = ByteBufCodecs.VAR_INT.map(RequestStats::new, RequestStats::scale);

        @Override
        public Type<RequestStats> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("3");
        registrar.playToClient(WalletSync.TYPE, WalletSync.CODEC, (p, ctx) -> ClientPayloadHandler.wallet(p));
        registrar.playToClient(MarketSync.TYPE, MarketSync.CODEC, (p, ctx) -> ClientPayloadHandler.market(p));
        registrar.playToClient(WearWarning.TYPE, WearWarning.CODEC, (p, ctx) -> ClientPayloadHandler.wearWarning(p));
        registrar.playToClient(StatsSync.TYPE, StatsSync.CODEC, (p, ctx) -> ClientPayloadHandler.stats(p));
        registrar.playToServer(RequestStats.TYPE, RequestStats.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer player) EconomyService.sendStats(player, p.scale());
        });
        registrar.playToClient(OpenWallet.TYPE, OpenWallet.CODEC, (p, ctx) -> ClientPayloadHandler.openWallet(p));
        registrar.playToServer(SellCoin.TYPE, SellCoin.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer player) EconomyService.sell(player, Coin.byId(p.coin()), p.amount());
        });
        registrar.playToServer(BuyItem.TYPE, BuyItem.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer player) EconomyService.buy(player, p.item(), p.count());
        });
        registrar.playToServer(ServiceItem.TYPE, ServiceItem.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer player) EconomyService.service(player, p.slot(), p.item(), p.repair());
        });
    }
}
