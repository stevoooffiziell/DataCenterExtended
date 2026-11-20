package de.f4ked.datacenterextended.economy;

import com.mojang.serialization.Codec;
import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.Datacenterextended;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * All wallets of a world, keyed by player UUID. Saved with the overworld, so racks can pay players who are offline.
 * Every change marks the wallet for syncing to its (online) owner, see {@link EconomyService}.
 */
public final class WalletData extends SavedData {
    private static final Codec<WalletData> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Wallet.CODEC)
            .xmap(WalletData::new, data -> data.wallets);

    public static final SavedDataType<WalletData> TYPE =
            new SavedDataType<>(Datacenterextended.id("wallets"), WalletData::new, CODEC);

    private final Map<UUID, Wallet> wallets;
    private final Set<UUID> pendingSync = new HashSet<>();

    public WalletData() {
        this(Map.of());
    }

    private WalletData(Map<UUID, Wallet> wallets) {
        this.wallets = new HashMap<>(wallets);
    }

    public static WalletData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** The player's wallet; a new wallet starts with the configured starting balance. */
    public Wallet wallet(UUID player) {
        return wallets.computeIfAbsent(player, id -> {
            setDirty();
            return new Wallet(Money.toCents(Config.safe(Config.STARTING_BALANCE, 250.0)));
        });
    }

    public boolean has(UUID player) {
        return wallets.containsKey(player);
    }

    public void deposit(UUID player, long cents) {
        if (cents == 0) return;
        wallet(player).addCents(cents);
        changed(player);
    }

    public boolean withdraw(UUID player, long cents) {
        boolean ok = wallet(player).tryTakeCents(cents);
        if (ok) changed(player);
        return ok;
    }

    public void depositCoins(UUID player, Coin coin, double amount) {
        if (amount <= 0) return;
        wallet(player).addCoins(coin, amount);
        changed(player);
    }

    public boolean withdrawCoins(UUID player, Coin coin, double amount) {
        boolean ok = wallet(player).tryTakeCoins(coin, amount);
        if (ok) changed(player);
        return ok;
    }

    private void changed(UUID player) {
        pendingSync.add(player);
        setDirty();
    }

    /** Owners whose wallets changed since the last call. */
    Set<UUID> drainPendingSync() {
        if (pendingSync.isEmpty()) return Set.of();
        Set<UUID> copy = new HashSet<>(pendingSync);
        pendingSync.clear();
        return copy;
    }
}
