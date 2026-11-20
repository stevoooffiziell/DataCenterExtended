package de.f4ked.datacenterextended.economy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** One player's money: dollars as cents plus an amount per {@link Coin}. Mutable, owned by {@link WalletData}. */
public final class Wallet {
    public static final Codec<Wallet> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("cents").forGetter(w -> w.cents),
            Codec.DOUBLE.listOf().optionalFieldOf("coins", List.of()).forGetter(Wallet::coinList)
    ).apply(i, Wallet::new));

    private long cents;
    private final double[] coins = new double[Coin.count()];

    public Wallet(long cents) {
        this.cents = cents;
    }

    private Wallet(long cents, List<Double> coins) {
        this.cents = cents;
        for (int i = 0; i < Math.min(coins.size(), this.coins.length); i++) this.coins[i] = coins.get(i);
    }

    public long cents() {
        return cents;
    }

    public double coins(Coin coin) {
        return coins[coin.ordinal()];
    }

    public double[] coinsCopy() {
        return Arrays.copyOf(coins, coins.length);
    }

    void addCents(long amount) {
        cents = Math.addExact(cents, amount);
    }

    /** Removes the amount if the balance covers it. */
    boolean tryTakeCents(long amount) {
        if (amount < 0 || cents < amount) return false;
        cents -= amount;
        return true;
    }

    void addCoins(Coin coin, double amount) {
        coins[coin.ordinal()] = Math.max(0, coins[coin.ordinal()] + amount);
    }

    boolean tryTakeCoins(Coin coin, double amount) {
        double held = coins[coin.ordinal()];
        if (amount <= 0 || amount > held + 1e-12) return false;
        coins[coin.ordinal()] = Math.max(0, held - amount);
        return true;
    }

    private List<Double> coinList() {
        List<Double> list = new ArrayList<>(coins.length);
        for (double c : coins) list.add(c);
        return list;
    }
}
