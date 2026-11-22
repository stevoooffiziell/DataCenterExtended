package de.f4ked.datacenterextended.power;

import de.f4ked.datacenterextended.advancement.ModAdvancements;
import de.f4ked.datacenterextended.menu.DataSlots;
import de.f4ked.datacenterextended.registry.ModBlockEntities;
import de.f4ked.datacenterextended.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Battery bank: accepts FE on every side, pushes it out of its front face and acts as storage on power networks.
 * Keeps its charge when broken.
 */
public class BatteryBlockEntity extends PowerBlockEntity {
    private final BatteryTier tier;
    private long lastAmount = -1;
    /** Average net flow in FE/t (positive = charging). */
    private double netFlow;
    private int lastSignal = -1;
    private boolean awardedFull;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= DATA_EXTRA && index < DATA_EXTRA + 4) {
                return DataSlots.chunk(Math.round(netFlow * 10) + SIGNED_OFFSET, index, DATA_EXTRA);
            }
            return baseData(index, 0, Math.max(0, -netFlow), tier.rate());
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, state.getBlock() instanceof BatteryBlock block ? block.tier() : BatteryTier.LEAD_ACID);
    }

    private BatteryBlockEntity(BlockPos pos, BlockState state, BatteryTier tier) {
        super(ModBlockEntities.BATTERY.get(), pos, state, 0, tier.capacity(), tier.rate(), tier.rate());
        this.tier = tier;
    }

    public BatteryTier tier() {
        return tier;
    }

    public double netFlow() {
        return netFlow;
    }

    @Override
    public int kind() {
        return KIND_BATTERY;
    }

    @Override
    public int typeOrdinal() {
        return tier.ordinal();
    }

    @Override
    protected ContainerData dataAccess() {
        return dataAccess;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BatteryBlockEntity be) {
        ServerLevel serverLevel = (ServerLevel) level;
        Direction front = state.hasProperty(BatteryBlock.FACING) ? state.getValue(BatteryBlock.FACING) : Direction.NORTH;
        be.pushTo(serverLevel, front, be.tier.rate());

        long amount = be.energy.getAmountAsLong();
        if (be.lastAmount >= 0) be.netFlow += ((amount - be.lastAmount) - be.netFlow) * 0.05;
        be.lastAmount = amount;

        int signal = be.comparatorSignal();
        if (signal != be.lastSignal) {
            be.lastSignal = signal;
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
        if (!be.awardedFull && be.tier == BatteryTier.LFP && amount >= be.energy.getCapacityAsLong() && be.owner() != null) {
            be.awardedFull = true;
            ModAdvancements.award(serverLevel.getServer(), be.owner(), ModAdvancements.GRID_STORAGE);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    // keep the charge in the item
    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        Integer stored = components.get(ModDataComponents.STORED_ENERGY.get());
        if (stored != null) energy.set((int) Math.min(energy.getCapacityAsLong(), stored));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (energy.getAmountAsLong() > 0) components.set(ModDataComponents.STORED_ENERGY.get(), energy.getAmountAsInt());
    }
}
