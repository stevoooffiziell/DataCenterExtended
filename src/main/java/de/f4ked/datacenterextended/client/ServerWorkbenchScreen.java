package de.f4ked.datacenterextended.client;

import de.f4ked.datacenterextended.hardware.DataUnits;
import de.f4ked.datacenterextended.hardware.HardwareStats;
import de.f4ked.datacenterextended.hardware.HardwareType;
import de.f4ked.datacenterextended.hardware.ServerLayout;
import de.f4ked.datacenterextended.hardware.ServerSpec;
import de.f4ked.datacenterextended.client.ui.Theme;
import de.f4ked.datacenterextended.client.ui.Ui;
import de.f4ked.datacenterextended.menu.ServerWorkbenchMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static de.f4ked.datacenterextended.menu.ServerWorkbenchMenu.*;

public class ServerWorkbenchScreen extends AbstractContainerScreen<ServerWorkbenchMenu> {
    private static final int INFO_X = 8, INFO_W = 160;

    public ServerWorkbenchScreen(ServerWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        Ui.window(g, leftPos, topPos, imageWidth, imageHeight);
        for (Slot slot : menu.slots) {
            Ui.slot(g, leftPos + slot.x, topPos + slot.y);
            if (slot instanceof ServerWorkbenchMenu.PartSlot part) {
                int sx = leftPos + slot.x, sy = topPos + slot.y;
                if (!menu.isPartSlotEnabled(part.partIndex())) {
                    g.fill(sx, sy, sx + 16, sy + 16, Theme.LOCKED);
                    g.fill(sx + 3, sy + 7, sx + 13, sy + 9, Theme.BORDER);
                } else if (!menu.isPartCompatible(part.partIndex())) {
                    g.fill(sx, sy, sx + 16, sy + 16, 0x80F87171);
                }
            }
        }
        Ui.inset(g, leftPos + INFO_X, topPos + INFO_Y, INFO_W, INFO_H);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Ui.fit(g, font, title, 6, 4, imageWidth - 12, Theme.TEXT);
        Ui.fit(g, font, playerInventoryTitle, 8, INVENTORY_Y - 11, 100, Theme.MUTED);
        int tx = INFO_X + 4, ty = INFO_Y + 3, w = INFO_W - 8;
        if (!menu.hasServer()) {
            Ui.fit(g, font, Component.translatable("gui.datacenterextended.workbench.insert"), tx, ty, w, Theme.WARN);
            return;
        }
        ServerSpec spec = menu.spec();
        HardwareStats board = menu.boardStats();
        Component status = spec.boots()
                ? Component.translatable("gui.datacenterextended.workbench.boots")
                : Component.translatable(spec.problem().translationKey());
        Ui.fit(g, font, status, tx, ty, w, spec.boots() ? Theme.OK : Theme.ERR);
        Ui.fit(g, font, Component.translatable("gui.datacenterextended.performance",
                DataUnits.formatBytesPerSecond(spec.computeBps()), DataUnits.formatHashrate(spec.hashesPerSecond())), tx, ty + 10, w, Theme.TEXT);
        int memColor = spec.memoryFactor() < 1.0 ? Theme.WARN : Theme.TEXT;
        Ui.fit(g, font, HardwareText.memoryLine(spec, board != null ? board.memoryType() : ""), tx, ty + 20, w, memColor);
        Ui.fit(g, font, Component.translatable("gui.datacenterextended.power_fe", spec.watts(), spec.psuCapacity(), spec.energyPerTick()), tx, ty + 30, w,
                spec.watts() > spec.psuCapacity() ? Theme.ERR : Theme.TEXT);
        Component last = board != null
                ? HardwareText.boardRequirements(board)
                : Component.translatable("gui.datacenterextended.storage", DataUnits.formatBytes(spec.storageGb() * DataUnits.GB));
        Ui.fit(g, font, last, tx, ty + 40, w, Theme.MUTED);
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> tooltip = super.getTooltipFromContainerItem(stack);
        if (hoveredSlot instanceof ServerWorkbenchMenu.PartSlot part && menu.isPartSlotEnabled(part.partIndex())
                && !menu.isPartCompatible(part.partIndex())) {
            tooltip.add(Component.translatable("gui.datacenterextended.slot.incompatible").withStyle(ChatFormatting.RED));
        }
        return tooltip;
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        Slot slot = hoveredSlot;
        if (slot == null || !menu.getCarried().isEmpty() || slot.index >= ServerLayout.SIZE + 1) return;
        if (slot.hasItem()) return;
        Component text;
        if (!(slot instanceof ServerWorkbenchMenu.PartSlot part)) {
            text = Component.translatable("gui.datacenterextended.slot.server");
        } else if (!menu.hasServer()) {
            text = Component.translatable("gui.datacenterextended.workbench.insert");
        } else if (!menu.isPartSlotEnabled(part.partIndex())) {
            text = Component.translatable("gui.datacenterextended.slot.locked");
        } else {
            HardwareType type = ServerLayout.slotType(part.partIndex());
            HardwareStats board = menu.boardStats();
            Component name = Component.translatable(type.translationKey());
            if (board != null && type == HardwareType.CPU && !board.socket().isEmpty()) {
                text = Component.translatable("gui.datacenterextended.slot.cpu", name, HardwareText.socket(board.socket()));
            } else if (board != null && type == HardwareType.MEMORY && !board.memoryType().isEmpty()) {
                text = Component.translatable("gui.datacenterextended.slot.memory", name, HardwareText.memoryType(board.memoryType()),
                        board.maxMemory() > 0 ? HardwareText.gb(board.maxMemory()) : "∞");
            } else {
                text = name;
            }
        }
        g.setTooltipForNextFrame(font, text, mouseX, mouseY);
    }
}
