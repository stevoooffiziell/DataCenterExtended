package de.f4ked.datacenterextended.menu;

import de.f4ked.datacenterextended.power.GeneratorFamily;
import de.f4ked.datacenterextended.power.GeneratorStatus;
import de.f4ked.datacenterextended.power.GeneratorType;
import de.f4ked.datacenterextended.power.PowerBlockEntity;
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

import static de.f4ked.datacenterextended.power.PowerBlockEntity.*;

/** GUI of generators, batteries and grid connections (fuel / fuel rod slots for combustion and nuclear). */
public class PowerMenu extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 176;
    public static final int IMAGE_HEIGHT = 196;
    public static final int SLOT_Y = 86;
    public static final int INVENTORY_X = 8, INVENTORY_Y = 114;

    private final int kind;
    private final int typeOrdinal;
    private final ContainerData data;
    private final Container container;
    private final @Nullable PowerBlockEntity be;
    private final int machineSlots;

    /** Client constructor. */
    public PowerMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, buf.readVarInt(), buf.readVarInt(), new SimpleContainer(Math.max(1, buf.readVarInt())),
                new SimpleContainerData(DATA_COUNT), null);
    }

    public PowerMenu(int containerId, Inventory inventory, int kind, int typeOrdinal, Container container, ContainerData data,
                     @Nullable PowerBlockEntity be) {
        super(ModMenus.POWER.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.kind = kind;
        this.typeOrdinal = typeOrdinal;
        this.data = data;
        this.container = container;
        this.be = be;
        int slots = kind == KIND_GENERATOR ? GeneratorType.byId(typeOrdinal).slotCount() : 0;
        int x0 = IMAGE_WIDTH / 2 - slots * 9;
        for (int i = 0; i < slots; i++) {
            addSlot(new Slot(container, i, x0 + i * 18 + 1, SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return container.canPlaceItem(getContainerSlot(), stack);
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return container.getMaxStackSize(stack);
                }
            });
        }
        machineSlots = slots;
        addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);
        addDataSlots(data);
    }

    public int kind() {
        return kind;
    }

    public int typeOrdinal() {
        return typeOrdinal;
    }

    public @Nullable GeneratorType generatorType() {
        return kind == KIND_GENERATOR ? GeneratorType.byId(typeOrdinal) : null;
    }

    public boolean hasSlots() {
        return machineSlots > 0;
    }

    public GeneratorStatus status() {
        return GeneratorStatus.byId(data.get(DATA_STATUS));
    }

    public int rawStatus() {
        return data.get(DATA_STATUS);
    }

    public double output() {
        return DataSlots.readLong(data, DATA_OUTPUT) / 10.0;
    }

    public double peak() {
        return DataSlots.readLong(data, DATA_PEAK) / 10.0;
    }

    public long stored() {
        return DataSlots.readLong(data, DATA_STORED);
    }

    public long capacity() {
        return DataSlots.readLong(data, DATA_CAPACITY);
    }

    public double heat() {
        return (data.get(DATA_HEAT) & 0xFFFF) / 1000.0;
    }

    public double burn() {
        return (data.get(DATA_BURN) & 0xFFFF) / 1000.0;
    }

    /** Battery: net flow in FE/t (positive = charging). */
    public double netFlow() {
        return (DataSlots.readLong(data, DATA_EXTRA) - SIGNED_OFFSET) / 10.0;
    }

    /** Grid: cents earned in total. */
    public long earned() {
        return DataSlots.readLong(data, DATA_EXTRA);
    }

    /** Grid: export price in cents per kWh. */
    public double priceCentsPerKWh() {
        return DataSlots.readLong(data, DATA_EXTRA2) / 100.0;
    }

    public boolean nuclear() {
        GeneratorType type = generatorType();
        return type != null && type.family() == GeneratorFamily.NUCLEAR;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be != null) be.handleButton(id);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return be == null || Container.stillValidBlockEntity(be, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int playerStart = machineSlots, playerEnd = playerStart + 36;
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
        } else {
            boolean moved = machineSlots > 0 && moveItemStackTo(stack, 0, machineSlots, false);
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
}
