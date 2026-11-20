package de.f4ked.datacenterextended.client;

import de.f4ked.datacenterextended.hardware.DataUnits;
import de.f4ked.datacenterextended.hardware.ServerLayout;
import de.f4ked.datacenterextended.hardware.ServerSpec;
import de.f4ked.datacenterextended.menu.ServerWorkbenchMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import static de.f4ked.datacenterextended.menu.ServerWorkbenchMenu.*;

public class ServerWorkbenchScreen extends AbstractContainerScreen<ServerWorkbenchMenu> {
    private static final int INFO_X = 8, INFO_Y = 84, INFO_W = 160, INFO_H = 42;

    public ServerWorkbenchScreen(ServerWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        this.inventoryLabelY = INVENTORY_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        GuiDraw.window(g, leftPos, topPos, imageWidth, imageHeight);
        for (Slot slot : menu.slots) {
            GuiDraw.slot(g, leftPos + slot.x, topPos + slot.y);
            if (slot instanceof ServerWorkbenchMenu.PartSlot part && !menu.isPartSlotEnabled(part.partIndex())) {
                GuiDraw.lockedOverlay(g, leftPos + slot.x, topPos + slot.y);
            }
        }
        GuiDraw.monitor(g, leftPos + INFO_X, topPos + INFO_Y, INFO_W, INFO_H);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        int tx = INFO_X + 4, ty = INFO_Y + 3;
        if (!menu.hasServer()) {
            g.text(font, Component.translatable("gui.datacenterextended.workbench.insert"), tx, ty, GuiDraw.TEXT_WARN, false);
            return;
        }
        ServerSpec spec = menu.spec();
        Component status = spec.boots()
                ? Component.translatable("gui.datacenterextended.workbench.boots")
                : Component.translatable(spec.problem().translationKey());
        g.text(font, status, tx, ty, spec.boots() ? GuiDraw.TEXT_OK : GuiDraw.TEXT_ERR, false);
        g.text(font, Component.translatable("gui.datacenterextended.performance",
                DataUnits.formatBytesPerSecond(spec.computeBps()), DataUnits.formatHashrate(spec.hashesPerSecond())), tx, ty + 10, GuiDraw.TEXT_INFO, false);
        g.text(font, Component.translatable("gui.datacenterextended.power", spec.watts(), spec.psuCapacity()), tx, ty + 20,
                spec.watts() > spec.psuCapacity() ? GuiDraw.TEXT_ERR : GuiDraw.TEXT_INFO, false);
        g.text(font, Component.translatable("gui.datacenterextended.storage", DataUnits.formatBytes(spec.storageGb() * DataUnits.GB)),
                tx, ty + 30, GuiDraw.TEXT_INFO, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        Slot slot = hoveredSlot;
        if (slot == null || slot.hasItem() || !menu.getCarried().isEmpty() || slot.index >= ServerLayout.SIZE + 1) return;
        Component text;
        if (!(slot instanceof ServerWorkbenchMenu.PartSlot part)) {
            text = Component.translatable("gui.datacenterextended.slot.server");
        } else if (!menu.hasServer()) {
            text = Component.translatable("gui.datacenterextended.workbench.insert");
        } else if (!menu.isPartSlotEnabled(part.partIndex())) {
            text = Component.translatable("gui.datacenterextended.slot.locked");
        } else {
            text = Component.translatable(ServerLayout.slotType(part.partIndex()).translationKey());
        }
        g.setTooltipForNextFrame(font, text, mouseX, mouseY);
    }
}
