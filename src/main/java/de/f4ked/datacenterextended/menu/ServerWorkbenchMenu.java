package de.f4ked.datacenterextended.menu;

import de.f4ked.datacenterextended.hardware.HardwareStats;
import de.f4ked.datacenterextended.hardware.HardwareType;
import de.f4ked.datacenterextended.hardware.ServerLayout;
import de.f4ked.datacenterextended.hardware.ServerSpec;
import de.f4ked.datacenterextended.registry.ModBlocks;
import de.f4ked.datacenterextended.registry.ModItems;
import de.f4ked.datacenterextended.registry.ModMenus;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Server assembly. The parts slots mirror the {@code server_parts} component of the server in the server slot:
 * inserting a server loads its parts, every change to a part slot is written straight back into the server item.
 */
public class ServerWorkbenchMenu extends AbstractContainerMenu {
    // layout (shared with the screen)
    public static final int IMAGE_WIDTH = 176;
    public static final int IMAGE_HEIGHT = 254;
    public static final int SERVER_X = 8, SERVER_Y = 40;
    public static final int BOARD_X = 34;
    public static final int PARTS_X = 60;
    public static final int ROW1_Y = 18, ROW2_Y = 40, ROW3_Y = 62, ROW4_Y = 84;
    public static final int MEMORY_X = 8;
    public static final int INFO_Y = 106, INFO_H = 52;
    public static final int INVENTORY_Y = 172;

    private static final int SERVER_SLOT = 0;
    private static final int PARTS_START = 1;
    private static final int PLAYER_START = PARTS_START + ServerLayout.SIZE;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final ContainerLevelAccess access;
    private final SimpleContainer serverContainer = new SimpleContainer(1) {
        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            ServerWorkbenchMenu.this.slotsChanged(this);
        }
    };
    private final SimpleContainer partsContainer = new SimpleContainer(ServerLayout.SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            ServerWorkbenchMenu.this.slotsChanged(this);
        }
    };
    private final boolean authoritative;
    private ItemStack loadedServer = ItemStack.EMPTY;
    private boolean syncing;

    public ServerWorkbenchMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public ServerWorkbenchMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.SERVER_WORKBENCH.get(), containerId);
        this.access = access;
        this.authoritative = !inventory.player.level().isClientSide();

        addSlot(new Slot(serverContainer, 0, SERVER_X, SERVER_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.SERVER.get());
            }
        });
        addSlot(new PartSlot(ServerLayout.SLOT_MOTHERBOARD, BOARD_X, ROW1_Y));
        addSlot(new PartSlot(ServerLayout.SLOT_PSU, BOARD_X, ROW2_Y));
        for (int i = 0; i < HardwareStats.MAX_CPU_SOCKETS; i++) {
            addSlot(new PartSlot(ServerLayout.SLOT_CPU_START + i, PARTS_X + i * 18, ROW1_Y));
        }
        for (int i = 0; i < HardwareStats.MAX_GPU_SLOTS; i++) {
            addSlot(new PartSlot(ServerLayout.SLOT_GPU_START + i, PARTS_X + i * 18, ROW2_Y));
        }
        for (int i = 0; i < HardwareStats.MAX_STORAGE_SLOTS; i++) {
            addSlot(new PartSlot(ServerLayout.SLOT_STORAGE_START + i, PARTS_X + i * 18, ROW3_Y));
        }
        for (int i = 0; i < HardwareStats.MAX_MEMORY_SLOTS; i++) {
            addSlot(new PartSlot(ServerLayout.SLOT_MEMORY_START + i, MEMORY_X + i * 18, ROW4_Y));
        }
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (!authoritative || syncing) return;
        ItemStack server = serverContainer.getItem(0);
        if (container == serverContainer) {
            if (server != loadedServer) {
                loadParts(server);
            }
        } else if (container == partsContainer && !server.isEmpty()) {
            List<ItemStack> parts = new ArrayList<>(ServerLayout.SIZE);
            for (int i = 0; i < ServerLayout.SIZE; i++) parts.add(partsContainer.getItem(i).copy());
            ServerLayout.writeParts(server, parts);
            serverContainer.setChanged();
        }
    }

    private void loadParts(ItemStack server) {
        syncing = true;
        try {
            NonNullList<ItemStack> parts = server.isEmpty() ? NonNullList.withSize(ServerLayout.SIZE, ItemStack.EMPTY) : ServerLayout.readParts(server);
            for (int i = 0; i < ServerLayout.SIZE; i++) {
                partsContainer.setItem(i, parts.get(i));
            }
            loadedServer = server;
        } finally {
            syncing = false;
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // parts live inside the server item – only the server itself is handed back
        access.execute((level, pos) -> clearContainer(player, serverContainer));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.SERVER_WORKBENCH.get());
    }

    public boolean hasServer() {
        return !serverContainer.getItem(0).isEmpty();
    }

    /** Live stats of the parts currently in the slots (client side uses the synced slot contents). */
    public ServerSpec spec() {
        return ServerSpec.of(parts());
    }

    private List<ItemStack> parts() {
        List<ItemStack> parts = new ArrayList<>(ServerLayout.SIZE);
        for (int i = 0; i < ServerLayout.SIZE; i++) parts.add(partsContainer.getItem(i));
        return parts;
    }

    public ItemStack motherboard() {
        return partsContainer.getItem(ServerLayout.SLOT_MOTHERBOARD);
    }

    /** Stats of the inserted motherboard, or null. */
    public @Nullable HardwareStats boardStats() {
        return ServerLayout.statsIn(parts(), ServerLayout.SLOT_MOTHERBOARD, HardwareType.MOTHERBOARD);
    }

    public boolean isPartSlotEnabled(int partIndex) {
        return hasServer() && ServerLayout.isSlotEnabled(partIndex, motherboard());
    }

    /** False if the installed part does not fit the motherboard (wrong socket / memory type). */
    public boolean isPartCompatible(int partIndex) {
        ItemStack part = partsContainer.getItem(partIndex);
        return part.isEmpty() || ServerLayout.isCompatible(partIndex, part, motherboard());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int i = 0; i < PLAYER_START && !moved; i++) {
                Slot target = slots.get(i);
                if (!target.hasItem() && target.mayPlace(stack)) {
                    moved = moveItemStackTo(stack, i, i + 1, false);
                }
            }
            if (!moved) {
                int hotbar = PLAYER_START + 27;
                if (index < hotbar ? !moveItemStackTo(stack, hotbar, PLAYER_END, false)
                        : !moveItemStackTo(stack, PLAYER_START, hotbar, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    /** A part slot; only usable while a server is inserted and the motherboard supports the slot. */
    public class PartSlot extends Slot {
        private final int partIndex;

        PartSlot(int partIndex, int x, int y) {
            super(partsContainer, partIndex, x, y);
            this.partIndex = partIndex;
        }

        public int partIndex() {
            return partIndex;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return hasServer() && ServerLayout.isValidForSlot(partIndex, stack) && isPartSlotEnabled(partIndex)
                    && ServerLayout.isCompatible(partIndex, stack, motherboard())
                    && ServerLayout.fitsMemoryLimit(parts(), partIndex, stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return hasServer();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
