package de.f4ked.datacenterextended.advancement;

import de.f4ked.datacenterextended.Datacenterextended;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Grants the mod's advancements that have no vanilla trigger. Those advancements use a single
 * {@code minecraft:impossible} criterion named {@value #CRITERION}, which only code can complete.
 */
public final class ModAdvancements {
    public static final String CRITERION = "done";

    public static final String FIRST_BOOT = "first_boot";
    public static final String FULL_RACK = "full_rack";
    public static final String FIRST_COIN = "first_coin";
    public static final String FIRST_CUSTOMER = "first_customer";
    public static final String DEDICATED_CUSTOMER = "dedicated_customer";
    public static final String OVERHEATED = "overheated";
    public static final String HALVING = "halving";
    public static final String REPAIRED = "repaired";
    public static final String TEN_THOUSAND = "ten_thousand";
    public static final String MILLIONAIRE = "millionaire";
    public static final String FIRST_GENERATOR = "first_generator";
    public static final String GRID_STORAGE = "grid_storage";
    public static final String FEEDING_THE_GRID = "feeding_the_grid";
    public static final String SPLITTING_ATOMS = "splitting_atoms";

    private ModAdvancements() {
    }

    public static void award(ServerPlayer player, String name) {
        MinecraftServer server = player.level().getServer();
        AdvancementHolder holder = server.getAdvancements().get(Datacenterextended.id(name));
        if (holder != null && !player.getAdvancements().getOrStartProgress(holder).isDone()) {
            player.getAdvancements().award(holder, CRITERION);
        }
    }

    /** Awards the advancement if the player is online (racks pay offline owners, but only online players progress). */
    public static void award(MinecraftServer server, @Nullable UUID playerId, String name) {
        if (playerId == null) return;
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) award(player, name);
    }
}
