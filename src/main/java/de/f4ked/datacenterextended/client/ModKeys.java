package de.f4ked.datacenterextended.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.f4ked.datacenterextended.Datacenterextended;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** The wallet key (default K, rebindable under Controls > DataCenterExtended). */
@EventBusSubscriber(modid = Datacenterextended.MODID, value = Dist.CLIENT)
public final class ModKeys {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Datacenterextended.id("main"));
    public static final KeyMapping WALLET = new KeyMapping("key.datacenterextended.wallet", InputConstants.Type.KEYBOARD,
            InputConstants.KEY_K, CATEGORY);

    private ModKeys() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(WALLET);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (WALLET.consumeClick()) {
            if (minecraft.player != null && minecraft.gui.screen() == null) {
                minecraft.gui.setScreen(new WalletScreen(false));
            }
        }
    }
}
