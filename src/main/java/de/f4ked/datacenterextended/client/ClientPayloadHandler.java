package de.f4ked.datacenterextended.client;

import de.f4ked.datacenterextended.network.ModPayloads;
import net.minecraft.client.Minecraft;

/** Client-side handling of economy packets (kept out of the common payload class). */
public final class ClientPayloadHandler {
    private ClientPayloadHandler() {
    }

    public static void wallet(ModPayloads.WalletSync payload) {
        ClientEconomy.setWallet(payload.cents(), payload.coins());
    }

    public static void market(ModPayloads.MarketSync payload) {
        ClientEconomy.setMarket(payload.prices(), payload.history());
    }

    public static void openWallet(ModPayloads.OpenWallet payload) {
        Minecraft.getInstance().gui.setScreen(new WalletScreen(payload.shop()));
    }
}
