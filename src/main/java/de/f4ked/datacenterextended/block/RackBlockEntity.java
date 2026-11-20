package de.f4ked.datacenterextended.block;

import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.economy.Coin;
import de.f4ked.datacenterextended.economy.WalletData;
import de.f4ked.datacenterextended.hardware.ServerProblem;
import de.f4ked.datacenterextended.hardware.ServerSpec;
import de.f4ked.datacenterextended.menu.DataSlots;
import de.f4ked.datacenterextended.menu.RackMenu;
import de.f4ked.datacenterextended.network.ComputeNetworkManager;
import de.f4ked.datacenterextended.network.ComputeNode;
import de.f4ked.datacenterextended.network.NetworkInfo;
import de.f4ked.datacenterextended.registry.ModBlockEntities;
import de.f4ked.datacenterextended.registry.ModDataMaps;
import de.f4ked.datacenterextended.registry.ModItems;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One rack segment (4 servers). The bottom segment of a merged rack is the controller: it runs the logic for the whole
 * stack, owns the target/output slots and is the stack's compute network node. Every segment has its own FE buffer
 * (so cables can feed any block); the controller draws from all of them.
 */
public class RackBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, ComputeNode {

    // ------------------------------------------------------------------ slots
    public static final int SERVERS = 4;
    public static final int SLOT_TARGET = 4;
    public static final int SLOT_OUTPUT_START = 5;
    public static final int SLOT_COUNT = 8;
    public static final int MAX_SERVERS = SERVERS * RackStack.MAX_HEIGHT;

    // ------------------------------------------------------------------ synced data (see DataSlots)
    public static final int DATA_STATUS = 0;
    public static final int DATA_SEGMENTS = 1;
    public static final int DATA_SERVER_CODES = 2;                 // 12 slots: 0 empty, 1 running, 2+ = 2 + problem
    public static final int DATA_LOCAL = 14;                       // B/s or H/s of own running servers
    public static final int DATA_EFFECTIVE = 18;                   // rate actually used for production
    public static final int DATA_RECEIVED = 22;
    public static final int DATA_EXPORTED = 26;
    public static final int DATA_BANDWIDTH = 30;
    public static final int DATA_WATTS = 34;
    public static final int DATA_PROGRESS = 38;
    public static final int DATA_GOAL = 42;
    public static final int DATA_ENERGY = 46;
    public static final int DATA_ENERGY_CAP = 50;
    public static final int DATA_BONUS_PERMILLE = 54;
    public static final int DATA_NETWORKED = 55;
    public static final int DATA_CABLES = 56;
    public static final int DATA_FE_PER_TICK = 57;
    /** Income per hour: cents (hosting) or coins * 1e8 (mining). */
    public static final int DATA_INCOME = 61;
    public static final int DATA_COIN = 65;
    public static final int DATA_HAS_OWNER = 66;
    public static final int DATA_COUNT = 67;

    public static final double COIN_SCALE = 1.0E8;
    public static final int BUTTON_NEXT_COIN = 0;
    private static final int PAYOUT_INTERVAL = 20;

    public static final int CODE_EMPTY = 0;
    public static final int CODE_RUNNING = 1;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private double progress;
    private @Nullable Item lastTarget;

    // controller state, recomputed every tick
    private RackStatus status = RackStatus.NO_SERVERS;
    private int segmentCount = 1;
    private final int[] serverCodes = new int[MAX_SERVERS];
    private double localRate;
    private double effectiveRate;
    private long watts;
    private long fePerTick;

    // wallet payout (mining / hosting)
    private @Nullable UUID owner;
    private String ownerName = "";
    private Coin coin = Coin.BITCOIN;
    /** Not yet paid out: cents (hosting) or coins (mining). */
    private double pending;
    private double incomePerHour;
    private long goal;
    private double storageBonus;
    private long supply;
    private long demand;

    // last network allocation
    private long received;
    private long exported;
    private long allocationTick = Long.MIN_VALUE;
    private @Nullable NetworkInfo networkInfo;

    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(
            Config.safe(Config.ENERGY_CAPACITY, 200_000), Config.safe(Config.MAX_ENERGY_INPUT, 20_000), 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            RackBlockEntity.super.setChanged();
        }
    };

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == DATA_STATUS) return status.ordinal();
            if (index == DATA_SEGMENTS) return segmentCount;
            if (index >= DATA_SERVER_CODES && index < DATA_LOCAL) return serverCodes[index - DATA_SERVER_CODES];
            if (index == DATA_BONUS_PERMILLE) return (int) Math.round(storageBonus * 1000);
            if (index == DATA_NETWORKED) return hasNetwork() ? 1 : 0;
            if (index == DATA_CABLES) return hasNetwork() ? Math.min(Short.MAX_VALUE, networkInfo.cables()) : 0;
            if (index == DATA_COIN) return coin.ordinal();
            if (index == DATA_HAS_OWNER) return payee() != null ? 1 : 0;
            int base = index >= DATA_INCOME ? DATA_INCOME : index >= DATA_FE_PER_TICK ? DATA_FE_PER_TICK
                    : DATA_LOCAL + ((index - DATA_LOCAL) / 4) * 4;
            long value = switch (base) {
                case DATA_LOCAL -> (long) localRate;
                case DATA_EFFECTIVE -> (long) effectiveRate;
                case DATA_RECEIVED -> hasNetwork() ? received : 0;
                case DATA_EXPORTED -> hasNetwork() ? exported : 0;
                case DATA_BANDWIDTH -> hasNetwork() ? networkInfo.bandwidth() : 0;
                case DATA_WATTS -> watts;
                case DATA_PROGRESS -> (long) progress;
                case DATA_GOAL -> goal;
                case DATA_ENERGY -> totalEnergy(false);
                case DATA_ENERGY_CAP -> totalEnergy(true);
                case DATA_FE_PER_TICK -> fePerTick;
                case DATA_INCOME -> (long) (type() == RackType.MINING ? incomePerHour * COIN_SCALE : incomePerHour);
                default -> 0L;
            };
            return DataSlots.chunk(value, index, base);
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public RackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RACK.get(), pos, state);
    }

    public RackType type() {
        return getBlockState().getBlock() instanceof RackBlock rack ? rack.type() : RackType.MINING;
    }

    // ================================================================== stack helpers

    public static @Nullable RackBlockEntity controllerOf(Level level, BlockPos pos) {
        List<BlockPos> group = RackStack.group(level, pos);
        return level.getBlockEntity(group.getFirst()) instanceof RackBlockEntity be ? be : null;
    }

    /** The segments of this block's merged rack, controller first. */
    public List<RackBlockEntity> segments() {
        List<RackBlockEntity> result = new ArrayList<>(RackStack.MAX_HEIGHT);
        if (level == null) return List.of(this);
        for (BlockPos p : RackStack.group(level, worldPosition)) {
            if (level.getBlockEntity(p) instanceof RackBlockEntity be) result.add(be);
        }
        return result;
    }

    public boolean isController() {
        return level == null || RackStack.group(level, worldPosition).getFirst().equals(worldPosition);
    }

    public void openMenu(Player player) {
        int segs = segments().size();
        RackType type = type();
        player.openMenu(this, buf -> {
            buf.writeVarInt(type.ordinal());
            buf.writeVarInt(segs);
        });
    }

    /** Used by the menu to close itself when the stack is rebuilt while it is open. */
    public boolean matchesGroup(int expectedSegments) {
        return !isRemoved() && isController() && segments().size() == expectedSegments;
    }

    // ================================================================== ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, RackBlockEntity be) {
        if (be.isController()) {
            be.controllerTick((ServerLevel) level);
        } else {
            be.handOverToController();
        }
    }

    /** A segment that stopped being a controller passes leftover outputs / its target to the new controller. */
    private void handOverToController() {
        if (level == null) return;
        boolean any = false;
        for (int i = SLOT_TARGET; i < SLOT_COUNT; i++) any |= !items.get(i).isEmpty();
        if (!any) return;
        RackBlockEntity controller = controllerOf(level, worldPosition);
        if (controller == null || controller == this) return;
        if (!items.get(SLOT_TARGET).isEmpty() && controller.items.get(SLOT_TARGET).isEmpty()) {
            controller.items.set(SLOT_TARGET, items.get(SLOT_TARGET));
            items.set(SLOT_TARGET, ItemStack.EMPTY);
        }
        for (int i = SLOT_OUTPUT_START; i < SLOT_COUNT; i++) {
            ItemStack stack = items.get(i);
            while (!stack.isEmpty() && controller.insertOutput(stack.copyWithCount(1))) {
                stack.shrink(1);
            }
        }
        controller.setChanged();
        setChanged();
    }

    private void controllerTick(ServerLevel level) {
        RackType type = type();
        List<RackBlockEntity> segs = segments();
        segmentCount = segs.size();
        boolean mining = type == RackType.MINING;

        // ---- 1. evaluate servers
        java.util.Arrays.fill(serverCodes, CODE_EMPTY);
        List<ServerSpec> running = new ArrayList<>();
        List<Integer> runningIdx = new ArrayList<>();
        for (int s = 0; s < segs.size(); s++) {
            for (int slot = 0; slot < SERVERS; slot++) {
                ItemStack server = segs.get(s).items.get(slot);
                int idx = s * SERVERS + slot;
                if (server.isEmpty()) continue;
                ServerSpec spec = ServerSpec.of(server);
                ServerProblem problem = spec.problem();
                if (problem == ServerProblem.NONE) {
                    double relevant = mining ? spec.hashesPerSecond() : spec.computeBps();
                    if (relevant <= 0) problem = mining ? ServerProblem.NO_HASHRATE : ServerProblem.NO_COMPUTE;
                }
                if (problem == ServerProblem.NONE) {
                    running.add(spec);
                    runningIdx.add(idx);
                    serverCodes[idx] = CODE_RUNNING;
                } else {
                    serverCodes[idx] = 2 + problem.ordinal();
                }
            }
        }

        // ---- 2. energy: power servers one by one while the buffers of all segments last
        if (Config.REQUIRE_ENERGY.get()) {
            long budget = 0;
            for (RackBlockEntity seg : segs) budget += seg.energy.getAmountAsLong();
            long needed = 0;
            List<ServerSpec> powered = new ArrayList<>(running.size());
            for (int i = 0; i < running.size(); i++) {
                long cost = running.get(i).energyPerTick();
                if (needed + cost <= budget) {
                    needed += cost;
                    powered.add(running.get(i));
                } else {
                    serverCodes[runningIdx.get(i)] = 2 + ServerProblem.NO_ENERGY.ordinal();
                }
            }
            running = powered;
            drainEnergy(segs, needed);
        }
        fePerTick = running.stream().mapToLong(ServerSpec::energyPerTick).sum();

        localRate = running.stream().mapToDouble(sp -> mining ? sp.hashesPerSecond() : sp.computeBps()).sum();
        watts = running.stream().mapToLong(ServerSpec::watts).sum();
        double storageTb = running.stream().mapToDouble(ServerSpec::storageGb).sum() / 1024.0;

        // ---- 3. what are we producing?
        ItemStack product = ItemStack.EMPTY;
        double speedFactor = 1.0;
        long ownDemand = 0;
        storageBonus = 0;
        switch (type) {
            case MINING -> goal = 0;
            case HOSTING -> {
                goal = 0;
                storageBonus = Math.min(Config.MAX_STORAGE_BONUS.get(), storageTb * Config.STORAGE_BONUS_PER_TB.get());
                speedFactor = 1.0 + storageBonus;
                ownDemand = payee() != null ? Config.HOSTING_CAPACITY_PER_SEGMENT.get() * segmentCount : 0;
            }
            default -> {
                ItemStack target = items.get(SLOT_TARGET);
                Item targetItem = target.isEmpty() ? null : target.getItem();
                if (targetItem != lastTarget) progress = 0;
                lastTarget = targetItem;
                product = target.isEmpty() ? ItemStack.EMPTY : new ItemStack(target.getItem()); // never copy components
                goal = Math.max(0, ModDataMaps.fileSize(target));
                speedFactor = Config.FABRICATION_SPEED.get();
                ownDemand = !product.isEmpty() && goal > 0 && canOutput(product)
                        ? Config.FABRICATING_INTAKE_PER_SEGMENT.get() * segmentCount : 0;
            }
        }

        // ---- 4. local use first, then the network
        boolean allocationValid = type.networked() && allocationTick >= level.getGameTime() - 2;
        if (type.networked()) {
            long local = (long) localRate;
            long used = Math.min(local, ownDemand);
            supply = local - used;
            demand = ownDemand - used;
            long in = allocationValid ? Math.min(received, demand) : 0;
            effectiveRate = used + in;
        } else {
            supply = 0;
            demand = 0;
            effectiveRate = localRate;
        }
        if (!allocationValid) {
            received = 0;
            exported = 0;
            networkInfo = null;
        }

        // ---- 5. status + production
        status = evaluate(type, running.isEmpty(), product);
        incomePerHour = 0;
        if (status == RackStatus.RUNNING) {
            switch (type) {
                case MINING -> earn(effectiveRate / 20.0 / coin.hashesPerCoin());
                case HOSTING -> earn(effectiveRate * speedFactor / 20.0 / Config.BYTES_PER_DOLLAR.get() * 100.0);
                default -> produce(product, effectiveRate * speedFactor / 20.0);
            }
        }
        if (level.getGameTime() % PAYOUT_INTERVAL == 0) payOut(level);

        boolean powered = !running.isEmpty();
        for (RackBlockEntity seg : segs) {
            seg.setRunningState(powered);
        }
        setChanged();
    }

    private static void drainEnergy(List<RackBlockEntity> segs, long amount) {
        for (RackBlockEntity seg : segs) {
            if (amount <= 0) return;
            long take = Math.min(amount, seg.energy.getAmountAsLong());
            if (take > 0) {
                seg.energy.set((int) (seg.energy.getAmountAsLong() - take));
                amount -= take;
            }
        }
    }

    /** Stored FE (or capacity) of all segments of this merged rack. */
    private long totalEnergy(boolean capacity) {
        long total = 0;
        for (RackBlockEntity seg : segments()) {
            total += capacity ? seg.energy.getCapacityAsLong() : seg.energy.getAmountAsLong();
        }
        return total;
    }

    private RackStatus evaluate(RackType type, boolean noneRunning, ItemStack product) {
        if (type != RackType.FABRICATING && payee() == null) return RackStatus.NO_OWNER;
        // a rack without own servers still works on compute it receives over the network
        if (noneRunning && effectiveRate <= 0) {
            for (int code : serverCodes) {
                if (code == 2 + ServerProblem.NO_ENERGY.ordinal()) return RackStatus.NO_ENERGY;
            }
            return RackStatus.NO_SERVERS;
        }
        if (type == RackType.FABRICATING) {
            if (product.isEmpty()) return supply > 0 && exported > 0 ? RackStatus.EXPORTING : RackStatus.NO_TARGET;
            if (goal <= 0) return RackStatus.NOT_FABRICABLE;
            if (!canOutput(product)) return supply > 0 && exported > 0 ? RackStatus.EXPORTING : RackStatus.OUTPUT_FULL;
        }
        if (effectiveRate <= 0) return exported > 0 ? RackStatus.EXPORTING : RackStatus.IDLE;
        return RackStatus.RUNNING;
    }

    /** Accumulates income for this tick: cents (hosting) or coins (mining). */
    private void earn(double perTick) {
        pending += perTick;
        incomePerHour = perTick * 20 * 3600;
    }

    /** Pays the accumulated income into the owner's wallet. */
    private void payOut(ServerLevel level) {
        UUID payee = payee();
        if (payee == null || pending <= 0) return;
        WalletData wallets = WalletData.get(level.getServer());
        if (type() == RackType.MINING) {
            wallets.depositCoins(payee, coin, pending);
            pending = 0;
        } else if (type() == RackType.HOSTING) {
            long cents = (long) Math.floor(pending);
            wallets.deposit(payee, cents);
            pending -= cents;
        }
    }

    /** Who gets paid: the controller's owner, or the owner of any other segment. */
    private @Nullable UUID payee() {
        if (owner != null) return owner;
        for (RackBlockEntity seg : segments()) {
            if (seg.owner != null) return seg.owner;
        }
        return null;
    }

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

    public Coin coin() {
        return coin;
    }

    /** Switches the mined coin; income mined so far is paid out first. */
    public void setCoin(Coin coin) {
        if (coin == this.coin) return;
        if (level instanceof ServerLevel serverLevel) payOut(serverLevel);
        pending = 0;
        this.coin = coin;
        setChanged();
    }

    /** Income per hour of the last tick: cents (hosting) or coins (mining). */
    public double incomePerHour() {
        return incomePerHour;
    }

    private void produce(ItemStack product, double perTick) {
        if (goal <= 0 || product.isEmpty()) return;
        progress += perTick;
        int made = 0;
        int max = Config.MAX_OUTPUTS_PER_TICK.get();
        while (progress >= goal && made < max && insertOutput(product.copy())) {
            progress -= goal;
            made++;
        }
        if (progress > goal) progress = goal; // don't bank progress while the output is blocked
    }

    private int outputEnd() {
        return SLOT_OUTPUT_START + type().outputSlots();
    }

    private boolean canOutput(ItemStack product) {
        if (product.isEmpty()) return false;
        for (int i = SLOT_OUTPUT_START; i < outputEnd(); i++) {
            ItemStack out = items.get(i);
            if (out.isEmpty() || (ItemStack.isSameItemSameComponents(out, product) && out.getCount() < out.getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }

    /** Inserts a single-count stack into the output slots. */
    private boolean insertOutput(ItemStack one) {
        for (int i = SLOT_OUTPUT_START; i < outputEnd(); i++) {
            ItemStack out = items.get(i);
            if (!out.isEmpty() && ItemStack.isSameItemSameComponents(out, one) && out.getCount() < out.getMaxStackSize()) {
                out.grow(1);
                return true;
            }
        }
        for (int i = SLOT_OUTPUT_START; i < outputEnd(); i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, one);
                return true;
            }
        }
        return false;
    }

    // ================================================================== visuals

    private void setRunningState(boolean running) {
        if (level != null && getBlockState().hasProperty(RackBlock.RUNNING) && getBlockState().getValue(RackBlock.RUNNING) != running) {
            level.setBlock(worldPosition, getBlockState().setValue(RackBlock.RUNNING, running), 3);
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (!state.hasProperty(RackBlock.SERVER[0])) return;
        BlockState updated = state;
        for (int i = 0; i < SERVERS; i++) {
            updated = updated.setValue(RackBlock.SERVER[i], !items.get(i).isEmpty());
        }
        if (updated != state) {
            level.setBlock(worldPosition, updated, 3);
        }
    }

    // ================================================================== compute network node

    @Override
    public long supply() {
        return supply;
    }

    @Override
    public long demand() {
        return demand;
    }

    @Override
    public void applyAllocation(long received, long exported, NetworkInfo info, long gameTime) {
        this.received = received;
        this.exported = exported;
        this.networkInfo = info;
        this.allocationTick = gameTime;
    }

    public RackStatus status() {
        return status;
    }

    /** Own compute (B/s) or hashrate (H/s) of all running servers in the merged rack. */
    public double localRate() {
        return localRate;
    }

    /** Rate actually used for production this tick (own share + received from the network). */
    public double effectiveRate() {
        return effectiveRate;
    }

    public double storageBonus() {
        return storageBonus;
    }

    public long exported() {
        return exported;
    }

    public boolean hasNetwork() {
        return networkInfo != null && networkInfo.cables() > 0;
    }

    public @Nullable NetworkInfo networkInfo() {
        return networkInfo;
    }

    /** Capability target for any segment: the controller, if this rack type is networked. */
    public @Nullable ComputeNode computeNode() {
        if (level == null || !type().networked()) return null;
        return controllerOf(level, worldPosition);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) ComputeNetworkManager.registerNode(serverLevel, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel serverLevel) ComputeNetworkManager.unregisterNode(serverLevel, worldPosition);
    }

    /** This block's own FE buffer; the controller draws from the buffers of all segments. */
    public EnergyHandler getEnergyHandler() {
        return energy;
    }

    /** FE per tick the running servers of this merged rack use. */
    public long energyPerTick() {
        return fePerTick;
    }

    public RackBlockEntity controllerOrSelf() {
        RackBlockEntity controller = level == null ? null : controllerOf(level, worldPosition);
        return controller != null ? controller : this;
    }

    // ================================================================== container

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.datacenterextended." + type().id());
    }

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
        return SLOT_COUNT;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < SERVERS) return stack.is(ModItems.SERVER.get());
        return slot == SLOT_TARGET && type() == RackType.FABRICATING;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        List<RackBlockEntity> segs = segments();
        return new RackMenu(containerId, inventory, type(), new ArrayList<>(segs), dataAccess, this);
    }

    // automation: outputs only, on every face
    @Override
    public int[] getSlotsForFace(Direction side) {
        int n = type().outputSlots();
        int[] slots = new int[n];
        for (int i = 0; i < n; i++) slots[i] = SLOT_OUTPUT_START + i;
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_OUTPUT_START && slot < outputEnd();
    }

    // ================================================================== persistence

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        progress = input.getDoubleOr("progress", 0);
        pending = input.getDoubleOr("pending", 0);
        coin = Coin.byId(input.getIntOr("coin", 0));
        owner = input.getString("owner").map(text -> {
            try {
                return UUID.fromString(text);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }).orElse(null);
        ownerName = input.getStringOr("owner_name", "");
        energy.deserialize(input.childOrEmpty("energy"));
        lastTarget = items.get(SLOT_TARGET).isEmpty() ? null : items.get(SLOT_TARGET).getItem();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putDouble("progress", progress);
        output.putDouble("pending", pending);
        output.putInt("coin", coin.ordinal());
        if (owner != null) output.putString("owner", owner.toString());
        if (!ownerName.isEmpty()) output.putString("owner_name", ownerName);
        energy.serialize(output.child("energy"));
    }
}
