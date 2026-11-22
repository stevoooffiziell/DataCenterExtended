package de.f4ked.datacenterextended.menu;

import de.f4ked.datacenterextended.block.RackBlockEntity;
import de.f4ked.datacenterextended.block.RackStatus;
import de.f4ked.datacenterextended.block.RackType;
import de.f4ked.datacenterextended.economy.Coin;
import de.f4ked.datacenterextended.hosting.HostingPlan;

import de.f4ked.datacenterextended.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static de.f4ked.datacenterextended.block.RackBlockEntity.*;

/** GUI of a merged rack: 4 server slots per segment (up to 3 segments) plus the type specific production slots. */
public class RackMenu extends AbstractContainerMenu {
    // layout (shared with the screen)
    public static final int IMAGE_WIDTH = 204;
    public static final int IMAGE_HEIGHT = 244;
    public static final int SERVER_X = 8, SERVER_Y = 22, ROW_SPACING = 22;
    /** One cooling slot per segment, right of its 4 bays. */
    public static final int COOLING_X = 84;
    /** Left edge of the production area (target, outputs, income panel). */
    public static final int RIGHT_X = 126, RIGHT_W = 70;
    public static final int TARGET_X = RIGHT_X, TARGET_Y = 22;
    public static final int OUTPUT_Y = 44;
    public static final int BAR_X = RIGHT_X, BAR_Y = 68, BAR_W = RIGHT_W, BAR_H = 6;
    /** Vertical FE gauge and temperature gauge between the cooling slots and the production area. */
    public static final int ENERGY_X = 106, ENERGY_Y = 22, ENERGY_W = 6, ENERGY_H = 62;
    public static final int TEMP_X = 116;
    public static final int INVENTORY_X = 22, INVENTORY_Y = 162;

    private final RackType type;
    private final int segments;
    private final ContainerData data;
    private final @Nullable RackBlockEntity controller;
    private final int serverSlotCount;
    private final int coolingSlotEnd;
    private final int machineSlotEnd;

    /** Client constructor. */
    public RackMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, RackType.byId(buf.readVarInt()), clientContainers(buf.readVarInt()),
                new SimpleContainerData(DATA_COUNT), null);
    }

    private static List<Container> clientContainers(int segments) {
        List<Container> list = new ArrayList<>();
        for (int i = 0; i < Math.max(1, Math.min(segments, 3)); i++) list.add(new SimpleContainer(SLOT_COUNT));
        return list;
    }

    public RackMenu(int containerId, Inventory inventory, RackType type, List<? extends Container> segmentContainers,
                    ContainerData data, @Nullable RackBlockEntity controller) {
        super(ModMenus.RACK.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.type = type;
        this.segments = segmentContainers.size();
        this.data = data;
        this.controller = controller;

        for (int s = 0; s < segments; s++) {
            for (int i = 0; i < SERVERS; i++) {
                addSlot(new ServerSlot(segmentContainers.get(s), i, SERVER_X + i * 18, SERVER_Y + s * ROW_SPACING));
            }
        }
        serverSlotCount = slots.size();
        for (int s = 0; s < segments; s++) {
            addSlot(new CoolingSlot(segmentContainers.get(s), SLOT_COOLING, COOLING_X, SERVER_Y + s * ROW_SPACING));
        }
        coolingSlotEnd = slots.size();
        Container main = segmentContainers.getFirst();
        if (type == RackType.FABRICATING) {
            addSlot(new Slot(main, SLOT_TARGET, TARGET_X, TARGET_Y) {
                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int i = 0; i < type.outputSlots(); i++) {
            int x = RIGHT_X + RIGHT_W + 2 - 18 * (type.outputSlots() - i);
            addSlot(new Slot(main, SLOT_OUTPUT_START + i, x, OUTPUT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        machineSlotEnd = slots.size();
        addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);
        addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        if (controller == null) return true;
        return Container.stillValidBlockEntity(controller, player) && controller.matchesGroup(segments);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int playerStart = machineSlotEnd, playerEnd = playerStart + 36;

        if (index < machineSlotEnd) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int i = 0; i < machineSlotEnd && !moved; i++) {
                Slot target = slots.get(i);
                if (!target.hasItem() && target.mayPlace(stack)) {
                    moved = moveItemStackTo(stack, i, i + 1, false);
                }
            }
            if (!moved) {
                int hotbar = playerStart + 27;
                if (index < hotbar ? !moveItemStackTo(stack, hotbar, playerEnd, false)
                        : !moveItemStackTo(stack, playerStart, hotbar, false)) {
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

    // ------------------------------------------------------------------ accessors for the screen

    public RackType type() {
        return type;
    }

    public int segments() {
        return segments;
    }

    public int serverSlotCount() {
        return serverSlotCount;
    }

    /** Exclusive end of the cooling slots (they follow the server slots). */
    public int coolingSlotEnd() {
        return coolingSlotEnd;
    }

    public RackStatus status() {
        return RackStatus.byId(data.get(DATA_STATUS));
    }

    /** 0 empty, 1 running, 2+ = 2 + {@link de.f4ked.datacenterextended.hardware.ServerProblem} ordinal. */
    public int serverCode(int serverIndex) {
        return DataSlots.readSmall(data, DATA_SERVER_CODES + serverIndex);
    }

    public long localRate() {
        return DataSlots.readLong(data, DATA_LOCAL);
    }

    public long effectiveRate() {
        return DataSlots.readLong(data, DATA_EFFECTIVE);
    }

    public long received() {
        return DataSlots.readLong(data, DATA_RECEIVED);
    }

    public long exported() {
        return DataSlots.readLong(data, DATA_EXPORTED);
    }

    public long bandwidth() {
        return DataSlots.readLong(data, DATA_BANDWIDTH);
    }

    public long watts() {
        return DataSlots.readLong(data, DATA_WATTS);
    }

    public long progress() {
        return DataSlots.readLong(data, DATA_PROGRESS);
    }

    public long goal() {
        return DataSlots.readLong(data, DATA_GOAL);
    }

    public long energy() {
        return DataSlots.readLong(data, DATA_ENERGY);
    }

    public long energyCapacity() {
        return DataSlots.readLong(data, DATA_ENERGY_CAP);
    }

    public long fePerTick() {
        return DataSlots.readLong(data, DATA_FE_PER_TICK);
    }

    /** Income per hour: cents (hosting) or coins (mining). */
    public double incomePerHour() {
        long raw = DataSlots.readLong(data, DATA_INCOME);
        return type == RackType.MINING ? raw / COIN_SCALE : raw;
    }

    public Coin coin() {
        return Coin.byId(data.get(DATA_COIN));
    }

    public boolean hasOwner() {
        return data.get(DATA_HAS_OWNER) != 0;
    }

    /** Coin selector of mining racks (sent through the vanilla container button packet). */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_NEXT_COIN && type == RackType.MINING && controller != null) {
            controller.setCoin(Coin.byId((controller.coin().ordinal() + 1) % Coin.count()));
            return true;
        }
        if (id == BUTTON_COMPARATOR && controller != null) {
            controller.cycleComparatorMode();
            return true;
        }
        return false;
    }

    public double storageBonus() {
        return DataSlots.readSmall(data, DATA_BONUS_PERMILLE) / 1000.0;
    }

    public boolean networked() {
        return data.get(DATA_NETWORKED) != 0;
    }

    public int cables() {
        return DataSlots.readSmall(data, DATA_CABLES);
    }

    /** °C of the rack. */
    public double temperature() {
        return DataSlots.readSmall(data, DATA_TEMPERATURE) / 10.0 - TEMP_OFFSET;
    }

    /** Heat throttling: 1.0 = full speed. */
    public double speedFactor() {
        return DataSlots.readSmall(data, DATA_SPEED_PERMILLE) / 1000.0;
    }

    public long coolingCapacity() {
        return DataSlots.readLong(data, DATA_COOLING);
    }

    public long heatLoad() {
        return DataSlots.readLong(data, DATA_HEAT);
    }

    /** Electricity bill in cents per hour. */
    public long billPerHour() {
        return DataSlots.readLong(data, DATA_BILL);
    }

    public int customers(HostingPlan plan) {
        return DataSlots.readSmall(data, DATA_PLANS + plan.ordinal());
    }

    public double satisfaction() {
        return DataSlots.readSmall(data, DATA_SATISFACTION) / 1000.0;
    }

    public de.f4ked.datacenterextended.block.RackComparatorMode comparatorMode() {
        return de.f4ked.datacenterextended.block.RackComparatorMode.byId(data.get(DATA_COMPARATOR) & 0xF);
    }

    /** Bill mode: share of the power covered by the rack's own FE (0..1). */
    public double selfPowered() {
        return DataSlots.readSmall(data, DATA_SELF_POWERED) / 1000.0;
    }

    public int comparatorSignal() {
        return (data.get(DATA_COMPARATOR) >> 4) & 0xF;
    }

    /** Compute sold to hosting customers, B/s. */
    public long contracted() {
        return DataSlots.readLong(data, DATA_CONTRACTED);
    }

    /** Accepts servers and ASIC miners, one per bay. */
    public static class ServerSlot extends Slot {
        public ServerSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return RackBlockEntity.isBayItem(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    /** Accepts one cooling item (rack fan, water cooling kit). */
    public static class CoolingSlot extends Slot {
        public CoolingSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return RackBlockEntity.isCoolingItem(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
