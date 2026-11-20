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
    public record MarketSync(List<Double> prices, List<List<Double>> history) implements CustomPacketPayload {
        public static final Type<MarketSync> TYPE = new Type<>(Datacenterextended.id("market_sync"));
        public static final StreamCodec<ByteBuf, MarketSync> CODEC = StreamCodec.composite(
                DOUBLES, MarketSync::prices, DOUBLES.apply(ByteBufCodecs.list()), MarketSync::history, MarketSync::new);

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

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(WalletSync.TYPE, WalletSync.CODEC, (p, ctx) -> ClientPayloadHandler.wallet(p));
        registrar.playToClient(MarketSync.TYPE, MarketSync.CODEC, (p, ctx) -> ClientPayloadHandler.market(p));
        registrar.playToClient(OpenWallet.TYPE, OpenWallet.CODEC, (p, ctx) -> ClientPayloadHandler.openWallet(p));
        registrar.playToServer(SellCoin.TYPE, SellCoin.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer player) EconomyService.sell(player, Coin.byId(p.coin()), p.amount());
        });
        registrar.playToServer(BuyItem.TYPE, BuyItem.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer player) EconomyService.buy(player, p.item(), p.count());
        });
    }
}
