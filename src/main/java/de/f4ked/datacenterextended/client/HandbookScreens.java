package de.f4ked.datacenterextended.client;

import de.f4ked.datacenterextended.item.HandbookItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Opens the handbook in the vanilla book screen (client only). */
public final class HandbookScreens {
    private HandbookScreens() {
    }

    public static void open() {
        List<Component> pages = new ArrayList<>(HandbookItem.PAGES);
        for (int i = 1; i <= HandbookItem.PAGES; i++) {
            pages.add(Component.translatable("book.datacenterextended.page." + i));
        }
        Minecraft.getInstance().gui.setScreen(new BookViewScreen(new BookViewScreen.BookAccess(pages)));
    }
}
