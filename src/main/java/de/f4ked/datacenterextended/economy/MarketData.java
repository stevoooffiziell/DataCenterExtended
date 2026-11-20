package de.f4ked.datacenterextended.economy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.Datacenterextended;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/** Current coin prices and their recent history (the chart), saved with the overworld. */
public final class MarketData extends SavedData {
    public static final int HISTORY = 288;

    private static final Codec<MarketData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.listOf().fieldOf("prices").forGetter(m -> toList(m.prices)),
            Codec.DOUBLE.listOf().listOf().fieldOf("history").forGetter(m -> m.history)
    ).apply(i, MarketData::new));

    public static final SavedDataType<MarketData> TYPE =
            new SavedDataType<>(Datacenterextended.id("market"), MarketData::new, CODEC);

    private final double[] prices = new double[Coin.count()];
    /** Oldest first, per coin. */
    private final List<List<Double>> history = new ArrayList<>();
    private final RandomSource random = RandomSource.create();

    public MarketData() {
        this(List.of(), List.of());
    }

    private MarketData(List<Double> prices, List<List<Double>> history) {
        for (Coin coin : Coin.values()) {
            int i = coin.ordinal();
            this.prices[i] = i < prices.size() && prices.get(i) > 0 ? prices.get(i) : coin.basePrice();
            this.history.add(new ArrayList<>(i < history.size() ? history.get(i) : List.of()));
        }
    }

    public static MarketData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public double price(Coin coin) {
        return prices[coin.ordinal()];
    }

    public double[] pricesCopy() {
        return prices.clone();
    }

    public List<List<Double>> history() {
        return history;
    }

    /** One market step for every coin. */
    public void update() {
        for (Coin coin : Coin.values()) {
            int i = coin.ordinal();
            Config.CoinSettings s = coin.settings();
            prices[i] = PriceModel.next(prices[i], coin.basePrice(), Config.safe(s.volatility(), 0.03),
                    Config.safe(s.reversion(), 0.02), random.nextGaussian());
            List<Double> h = history.get(i);
            h.add(prices[i]);
            while (h.size() > HISTORY) h.removeFirst();
        }
        setDirty();
    }

    /** Sets a price directly (game tests). */
    public void setPrice(Coin coin, double price) {
        prices[coin.ordinal()] = price;
        setDirty();
    }

    private static List<Double> toList(double[] values) {
        List<Double> list = new ArrayList<>(values.length);
        for (double v : values) list.add(v);
        return list;
    }
}
