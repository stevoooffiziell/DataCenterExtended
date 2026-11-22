package de.f4ked.datacenterextended.power;

import de.f4ked.datacenterextended.menu.DataSlots;
import de.f4ked.datacenterextended.menu.PowerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Shared base of generators, batteries and grid connections: an owner, an FE buffer, an optional inventory, a GUI
 * ({@link PowerMenu}) and membership in the {@link PowerNetworkManager}.
 */
public abstract class PowerBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    // ContainerData layout shared with the menu
    public static final int DATA_KIND = 0;
    public static final int DATA_STATUS = 1;
    public static final int DATA_OUTPUT = 2;     // FE/t × 10 (long)
    public static final int DATA_PEAK = 6;       // FE/t × 10 (long)
    public static final int DATA_STORED = 10;
    public static final int DATA_CAPACITY = 14;
    public static final int DATA_HEAT = 18;      // permille
    public static final int DATA_BURN = 19;      // permille
    public static final int DATA_EXTRA = 20;     // long: battery net flow ×10 + offset / grid earned cents
    public static final int DATA_EXTRA2 = 24;    // long: grid price (cents per kWh × 100)
    public static final int DATA_COUNT = 28;
    /** Added to signed values so they survive the unsigned 16-bit chunks. */
    public static final long SIGNED_OFFSET = 1L << 40;

    public static final int KIND_GENERATOR = 0, KIND_BATTERY = 1, KIND_GRID = 2;

    protected NonNullList<ItemStack> items;
    protected final SimpleEnergyHandler energy;
    private @Nullable UUID owner;
    private String ownerName = "";
    @SuppressWarnings("unchecked")
    private final @Nullable BlockCapabilityCache<EnergyHandler, Direction>[] neighbours = new BlockCapabilityCache[6];

    protected PowerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots,
                               int capacity, int maxInsert, int maxExtract) {
        super(type, pos, state);
        this.items = NonNullList.withSize(slots, ItemStack.EMPTY);
        this.energy = new SimpleEnergyHandler(capacity, maxInsert, maxExtract) {
            @Override
            public int insert(int amount, TransactionContext transaction) {
                return acceptsEnergy() ? super.insert(amount, transaction) : 0;
            }

            @Override
            protected void onEnergyChanged(int previousAmount) {
                setChanged();
            }
        };
    }

    /** Whether other blocks may insert FE right now (the grid connection refuses without an owner). */
    protected boolean acceptsEnergy() {
        return true;
    }

    public abstract int kind();

    /** Ordinal of the tier/type enum (the client menu needs it before the data arrives). */
    public abstract int typeOrdinal();

    public EnergyHandler energy() {
        return energy;
    }

    /** Raises the stored energy, capped at the capacity. */
    protected void addEnergy(long amount) {
        if (amount <= 0) return;
        energy.set((int) Math.min(energy.getCapacityAsLong(), energy.getAmountAsLong() + amount));
    }

    // ------------------------------------------------------------------ pushing into neighbours

    /** Moves up to {@code rate} FE into the neighbour on {@code side} (not into power cables: the network does that). */
    protected long pushTo(ServerLevel level, Direction side, int rate) {
        if (energy.getAmountAsLong() <= 0) return 0;
        if (level.getBlockState(worldPosition.relative(side)).getBlock() instanceof PowerCableBlock) return 0;
        BlockCapabilityCache<EnergyHandler, Direction> cache = neighbours[side.ordinal()];
        if (cache == null) {
            cache = BlockCapabilityCache.create(Capabilities.Energy.BLOCK, level, worldPosition.relative(side), side.getOpposite());
            neighbours[side.ordinal()] = cache;
        }
        EnergyHandler target = cache.getCapability();
        if (target == null || target == energy) return 0;
        return EnergyHandlerUtil.move(energy, target, (int) Math.min(rate, energy.getAmountAsLong()), null);
    }

    // ------------------------------------------------------------------ owner

    public void setOwner(UUID owner, String name) {
        this.owner = owner;
        this.ownerName = name;
        setChanged();
    }

    public @Nullable UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    // ------------------------------------------------------------------ network membership

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) PowerNetworkManager.registerNode(serverLevel, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel serverLevel) PowerNetworkManager.unregisterNode(serverLevel, worldPosition);
    }

    // ------------------------------------------------------------------ GUI

    protected abstract ContainerData dataAccess();

    public void openMenu(Player player) {
        int kind = kind(), type = typeOrdinal(), slots = items.size();
        player.openMenu(this, buf -> {
            buf.writeVarInt(kind);
            buf.writeVarInt(type);
            buf.writeVarInt(slots);
        });
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new PowerMenu(containerId, inventory, kind(), typeOrdinal(), this, dataAccess(), this);
    }

    @Override
    protected Component getDefaultName() {
        return getBlockState().getBlock().getName();
    }

    /** Server-side button press from the GUI. */
    public void handleButton(int id) {
    }

    /** Shared ContainerData values; subclasses add their own. */
    protected int baseData(int index, int status, double outputPerTick, double peakPerTick) {
        if (index == DATA_KIND) return kind();
        if (index == DATA_STATUS) return status;
        if (index >= DATA_OUTPUT && index < DATA_OUTPUT + 4) return DataSlots.chunk(Math.round(outputPerTick * 10), index, DATA_OUTPUT);
        if (index >= DATA_PEAK && index < DATA_PEAK + 4) return DataSlots.chunk(Math.round(peakPerTick * 10), index, DATA_PEAK);
        if (index >= DATA_STORED && index < DATA_STORED + 4) return DataSlots.chunk(energy.getAmountAsLong(), index, DATA_STORED);
        if (index >= DATA_CAPACITY && index < DATA_CAPACITY + 4) return DataSlots.chunk(energy.getCapacityAsLong(), index, DATA_CAPACITY);
        return 0;
    }

    // ------------------------------------------------------------------ container

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        int[] slots = new int[items.size()];
        for (int i = 0; i < slots.length; i++) slots[i] = i;
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    /** Fill level for comparators. */
    public int comparatorSignal() {
        return EnergyHandlerUtil.getRedstoneSignalFromEnergyHandler(energy);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        energy.deserialize(input.childOrEmpty("energy"));
        owner = input.getString("owner").map(text -> {
            try {
                return UUID.fromString(text);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }).orElse(null);
        ownerName = input.getStringOr("owner_name", "");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        energy.serialize(output.child("energy"));
        if (owner != null) output.putString("owner", owner.toString());
        if (!ownerName.isEmpty()) output.putString("owner_name", ownerName);
    }
}
