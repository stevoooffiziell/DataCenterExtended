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

/**
 * Coin prices, their recent history (the chart) and announced halvings, saved with the overworld. A price is the real
 * reference price for the calendar date ({@link PriceHistory}) times a small mean-reverting random factor
 * ({@code exp(noise)}), so it tracks real history but still wiggles between updates.
 */
public final class MarketData extends SavedData {
    public static final int HISTORY = 288;
    /** Noise is clamped to ±this in log space (about ×0.22 .. ×4.5). */
    private static final double MAX_NOISE = 1.5;

    private static final Codec<MarketData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.listOf().fieldOf("prices").forGetter(m -> toList(m.prices)),
            Codec.DOUBLE.listOf().listOf().fieldOf("history").forGetter(m -> m.history),
            Codec.DOUBLE.listOf().optionalFieldOf("noise", List.of()).forGetter(m -> toList(m.noise)),
            Codec.INT.listOf().optionalFieldOf("halvings", List.of()).forGetter(m -> toList(m.announcedHalvings))
    ).apply(i, MarketData::new));

    public static final SavedDataType<MarketData> TYPE =
            new SavedDataType<>(Datacenterextended.id("market"), MarketData::new, CODEC);

    private final double[] prices = new double[Coin.count()];
    private final double[] noise = new double[Coin.count()];
    /** Oldest first, per coin. */
    private final List<List<Double>> history = new ArrayList<>();
    private final RandomSource random = RandomSource.create();
    /** Halvings already announced per coin (to detect new ones). -1 = not initialised yet. */
    private final int[] announcedHalvings = new int[Coin.count()];

    public MarketData() {
        this(List.of(), List.of(), List.of(), List.of());
    }

    private MarketData(List<Double> prices, List<List<Double>> history, List<Double> noise, List<Integer> halvings) {
        for (Coin coin : Coin.values()) {
            int i = coin.ordinal();
            this.prices[i] = i < prices.size() ? prices.get(i) : 0;
            this.noise[i] = i < noise.size() ? noise.get(i) : 0;
            this.history.add(new ArrayList<>(i < history.size() ? history.get(i) : List.of()));
            this.announcedHalvings[i] = i < halvings.size() ? halvings.get(i) : -1;
        }
    }

    public static MarketData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** Current price in dollars (0 before the coin's launch). Refreshed by {@link #refresh}. */
    public double price(Coin coin) {
        return prices[coin.ordinal()];
    }

    public double[] pricesCopy() {
        return prices.clone();
    }

    public List<List<Double>> history() {
        return history;
    }

    // ------------------------------------------------------------------ date based values

    public static double referencePrice(Coin coin, double epochDay) {
        return PriceHistory.referencePrice(coin, epochDay, coin.futureDrift());
    }

    public static boolean launched(Coin coin, double epochDay) {
        return PriceHistory.launched(coin, epochDay);
    }

    /** Hashes to mine one coin: the configured difficulty, scaled with the real price (see config difficultyExponent). */
    public static double hashesPerCoin(Coin coin, double epochDay) {
        double ref = referencePrice(coin, epochDay);
        if (ref <= 0) return Double.MAX_VALUE;
        return coin.hashesPerCoin() * Math.pow(ref / coin.basePrice(), coin.difficultyExponent());
    }

    public static int halvings(Coin coin, double epochDay) {
        return PriceHistory.halvings(coin, epochDay);
    }

    /** Coins per hash compared to launch: halvings (Bitcoin) and the post-Merge reduction (Ethereum). */
    public static double rewardMultiplier(Coin coin, double epochDay) {
        double reward = Math.scalb(1.0, -halvings(coin, epochDay));
        if (coin == Coin.ETHEREUM && PriceHistory.afterMerge(epochDay)) reward *= coin.postMergeReward();
        return reward;
    }

    /** Coins that halved since the last call (each reported once; the first call only initialises). */
    public List<Coin> newHalvings(double epochDay) {
        List<Coin> result = new ArrayList<>();
        for (Coin coin : Coin.values()) {
            int now = halvings(coin, epochDay);
            int i = coin.ordinal();
            if (announcedHalvings[i] < 0) {
                announcedHalvings[i] = now;
                setDirty();
            } else if (now > announcedHalvings[i]) {
                announcedHalvings[i] = now;
                result.add(coin);
                setDirty();
            } else if (now < announcedHalvings[i]) {
                announcedHalvings[i] = now; // calendar was moved back
                setDirty();
            }
        }
        return result;
    }

    // ------------------------------------------------------------------ prices

    /** Recomputes the current prices for the calendar time (keeps the noise). */
    public void refresh(double epochDay) {
        for (Coin coin : Coin.values()) {
            int i = coin.ordinal();
            double ref = referencePrice(coin, epochDay);
            prices[i] = ref > 0 ? ref * Math.exp(noise[i]) : 0;
        }
        setDirty();
    }

    /** One market step: new noise for every coin, refreshed prices, one chart point per launched coin. */
    public void update(double epochDay) {
        for (Coin coin : Coin.values()) {
            int i = coin.ordinal();
            Config.CoinSettings s = coin.settings();
            noise[i] = PriceModel.nextNoise(noise[i], Config.safe(s.volatility(), 0.02), Config.safe(s.reversion(), 0.1),
                    random.nextGaussian(), MAX_NOISE);
        }
        refresh(epochDay);
        for (Coin coin : Coin.values()) {
            int i = coin.ordinal();
            if (prices[i] <= 0) continue;
            List<Double> h = history.get(i);
            h.add(prices[i]);
            while (h.size() > HISTORY) h.removeFirst();
        }
        setDirty();
    }

    /** Clears the chart (after jumping in time, old points would be misleading). */
    public void clearHistory() {
        for (List<Double> h : history) h.clear();
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

    private static List<Integer> toList(int[] values) {
        List<Integer> list = new ArrayList<>(values.length);
        for (int v : values) list.add(v);
        return list;
    }
}
