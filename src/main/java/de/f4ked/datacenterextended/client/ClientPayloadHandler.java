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
        ClientEconomy.setHalvings(payload.halvings(), payload.nextHalving(), payload.epochDay(), payload.ticksPerDay());
    }

    private static final net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId WEAR_TOAST =
            new net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId(8000L);

    public static void wearWarning(ModPayloads.WearWarning payload) {
        net.minecraft.network.chat.Component title = net.minecraft.network.chat.Component.translatable(payload.broke()
                ? "toast.datacenterextended.wear.broke" : "toast.datacenterextended.wear.low");
        net.minecraft.network.chat.Component message = payload.broke()
                ? net.minecraft.network.chat.Component.translatable("toast.datacenterextended.wear.broke_message",
                payload.part().getHoverName(), payload.where())
                : net.minecraft.network.chat.Component.translatable("toast.datacenterextended.wear.low_message",
                payload.part().getHoverName(), Math.round(payload.condition() * 100), payload.where());
        net.minecraft.client.gui.components.toasts.SystemToast.add(Minecraft.getInstance().gui.toastManager(), WEAR_TOAST, title, message);
    }

    public static void stats(ModPayloads.StatsSync payload) {
        ClientStats.set(payload);
    }

    public static void openWallet(ModPayloads.OpenWallet payload) {
        Minecraft.getInstance().gui.setScreen(new WalletScreen(payload.shop()));
    }
}
