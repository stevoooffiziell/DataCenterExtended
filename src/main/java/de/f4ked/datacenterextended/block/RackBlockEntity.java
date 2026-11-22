package de.f4ked.datacenterextended.block;

import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.advancement.ModAdvancements;
import de.f4ked.datacenterextended.economy.Coin;
import de.f4ked.datacenterextended.economy.ElectricityBill;
import de.f4ked.datacenterextended.economy.MarketData;
import de.f4ked.datacenterextended.economy.EconomyService;
import de.f4ked.datacenterextended.economy.StatsData;
import de.f4ked.datacenterextended.economy.StatsLedger;
import de.f4ked.datacenterextended.economy.WalletData;
import de.f4ked.datacenterextended.network.ModPayloads;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import de.f4ked.datacenterextended.hardware.HardwareStats;
import de.f4ked.datacenterextended.hardware.HardwareType;
import de.f4ked.datacenterextended.hardware.ServerProblem;
import de.f4ked.datacenterextended.hardware.ServerSpec;
import de.f4ked.datacenterextended.hardware.Wear;
import de.f4ked.datacenterextended.heat.CoolingStats;
import de.f4ked.datacenterextended.heat.HeatModel;
import de.f4ked.datacenterextended.hosting.CustomerBase;
import de.f4ked.datacenterextended.hosting.HostingPlan;
import de.f4ked.datacenterextended.menu.DataSlots;
import de.f4ked.datacenterextended.menu.RackMenu;
import de.f4ked.datacenterextended.network.ComputeNetworkManager;
import de.f4ked.datacenterextended.network.ComputeNode;
import de.f4ked.datacenterextended.network.NetworkInfo;
import de.f4ked.datacenterextended.registry.ModBlockEntities;
import de.f4ked.datacenterextended.registry.ModDataMaps;
import de.f4ked.datacenterextended.registry.ModItems;
import de.f4ked.datacenterextended.time.CalendarData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * One rack segment (4 bays + 1 cooling slot). The bottom segment of a merged rack is the controller: it runs the logic
 * for the whole stack, owns the target/output slots and is the stack's compute network node. Every segment has its own
 * FE buffer (so cables can feed any block) and cooling slot; the controller draws from all of them.
 *
 * <p>Per tick the controller: evaluates the servers / ASICs in all bays, powers them (FE or electricity bill), heats up
 * and throttles, takes its share of the compute network, produces (coins, hosting fees or items) and wears the parts.
 */
public class RackBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, ComputeNode {

    // ------------------------------------------------------------------ slots
    public static final int SERVERS = 4;
    public static final int SLOT_TARGET = 4;
    public static final int SLOT_OUTPUT_START = 5;
    /** Exclusive end of the slots that belong to the controller (target + outputs). */
    public static final int SLOT_OUTPUT_END = 8;
    public static final int SLOT_COOLING = 8;
    public static final int SLOT_COUNT = 9;
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
    /** (°C + {@link #TEMP_OFFSET}) * 10. */
    public static final int DATA_TEMPERATURE = 67;
    public static final int DATA_SPEED_PERMILLE = 68;
    public static final int DATA_COOLING = 69;
    public static final int DATA_HEAT = 73;
    /** Electricity bill in cents per hour. */
    public static final int DATA_BILL = 77;
    public static final int DATA_PLANS = 81;                       // one slot per hosting plan
    public static final int DATA_SATISFACTION = 84;
    public static final int DATA_CONTRACTED = 85;
    /** Comparator mode (low 4 bits) and signal (next 4 bits). */
    public static final int DATA_COMPARATOR = 89;
    public static final int DATA_SELF_POWERED = 90;                 // permille of the wall power covered by own FE (bill mode)
    public static final int DATA_COUNT = 91;

    private static final int[] LONG_FIELDS = {DATA_LOCAL, DATA_EFFECTIVE, DATA_RECEIVED, DATA_EXPORTED, DATA_BANDWIDTH,
            DATA_WATTS, DATA_PROGRESS, DATA_GOAL, DATA_ENERGY, DATA_ENERGY_CAP, DATA_FE_PER_TICK, DATA_INCOME,
            DATA_COOLING, DATA_HEAT, DATA_BILL, DATA_CONTRACTED};

    public static final double TEMP_OFFSET = 100.0;
    public static final double COIN_SCALE = 1.0E8;
    public static final int BUTTON_NEXT_COIN = 0;
    public static final int BUTTON_COMPARATOR = 1;
    private static final int PAYOUT_INTERVAL = 20;
    private static final int WEAR_INTERVAL = 1200;
    private static final int ADVANCEMENT_INTERVAL = 100;
    private static final double TICKS_PER_HOUR = 72_000.0;

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

    // heat
    private double temperature = Double.NaN;
    private double ambient = 20;
    private long ambientCheckedAt = Long.MIN_VALUE;
    private long heatLoad;
    private long coolingCapacity;
    private double speedFactor = 1.0;
    private boolean coinLaunched = true;

    // comparator output (controller)
    private RackComparatorMode comparatorMode = RackComparatorMode.STATUS;
    private int lastSignal;

    // electricity bill (cents)
    private final ElectricityBill bill = new ElectricityBill();
    private double billPerHour;
    /** Bill mode: share of the power taken from the rack's own FE buffers this tick. */
    private double selfPowered;

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

    // hosting customers
    private final CustomerBase customers = new CustomerBase(HostingPlan.count());
    private double contracted;

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
            if (index == DATA_TEMPERATURE) return (int) Math.round((temperature() + TEMP_OFFSET) * 10) & 0x7FFF;
            if (index == DATA_SPEED_PERMILLE) return (int) Math.round(speedFactor * 1000);
            if (index >= DATA_PLANS && index < DATA_PLANS + HostingPlan.count()) {
                return Math.min(Short.MAX_VALUE, customers.count(index - DATA_PLANS));
            }
            if (index == DATA_SATISFACTION) return (int) Math.round(customers.satisfaction() * 1000);
            if (index == DATA_COMPARATOR) return comparatorMode.ordinal() | (lastSignal << 4);
            if (index == DATA_SELF_POWERED) return (int) Math.round(selfPowered * 1000);
            for (int base : LONG_FIELDS) {
                if (index >= base && index < base + 4) return DataSlots.chunk(longValue(base), index, base);
            }
            return 0;
        }

        private long longValue(int base) {
            return switch (base) {
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
                case DATA_COOLING -> coolingCapacity;
                case DATA_HEAT -> heatLoad;
                case DATA_BILL -> Math.round(billPerHour);
                case DATA_CONTRACTED -> (long) contracted;
                default -> 0L;
            };
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

    /** Items that go into a rack bay: assembled servers and ASIC miners. */
    public static boolean isBayItem(ItemStack stack) {
        if (stack.is(ModItems.SERVER.get())) return true;
        HardwareStats stats = ModDataMaps.hardware(stack);
        return stats != null && stats.type() == HardwareType.ASIC;
    }

    public static boolean isCoolingItem(ItemStack stack) {
        return ModDataMaps.cooling(stack) != null;
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
        for (int i = SLOT_TARGET; i < SLOT_OUTPUT_END; i++) any |= !items.get(i).isEmpty();
        if (!any) return;
        RackBlockEntity controller = controllerOf(level, worldPosition);
        if (controller == null || controller == this) return;
        if (!items.get(SLOT_TARGET).isEmpty() && controller.items.get(SLOT_TARGET).isEmpty()) {
            controller.items.set(SLOT_TARGET, items.get(SLOT_TARGET));
            items.set(SLOT_TARGET, ItemStack.EMPTY);
        }
        for (int i = SLOT_OUTPUT_START; i < SLOT_OUTPUT_END; i++) {
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
        boolean bills = Config.safe(Config.ELECTRICITY_BILLS, false);
        long gameTime = level.getGameTime();
        MinecraftServer server = level.getServer();
        double today = CalendarData.now(server);
        coinLaunched = !mining || MarketData.launched(coin, today);
        UUID payee = payee();
        if (!bills) bill.reset();
        boolean unpaid = bill.unpaid();

        // ---- 1. evaluate servers / ASICs
        Arrays.fill(serverCodes, CODE_EMPTY);
        List<ServerSpec> running = new ArrayList<>();
        List<Integer> runningIdx = new ArrayList<>();
        for (int s = 0; s < segs.size(); s++) {
            for (int slot = 0; slot < SERVERS; slot++) {
                ItemStack bay = segs.get(s).items.get(slot);
                int idx = s * SERVERS + slot;
                if (bay.isEmpty()) continue;
                ServerSpec spec = ServerSpec.of(bay);
                ServerProblem problem = spec.problem();
                if (problem == ServerProblem.NONE && spec.asic() && (!mining || !coin.asicMineable())) {
                    problem = mining ? ServerProblem.ASIC_WRONG_COIN : ServerProblem.NO_COMPUTE;
                }
                if (problem == ServerProblem.NONE) {
                    double relevant = mining ? spec.hashesPerSecond() : spec.computeBps();
                    if (relevant <= 0) problem = mining ? ServerProblem.NO_HASHRATE : ServerProblem.NO_COMPUTE;
                }
                if (problem == ServerProblem.NONE && unpaid) problem = ServerProblem.UNPAID_BILL;
                if (problem == ServerProblem.NONE) {
                    running.add(spec);
                    runningIdx.add(idx);
                    serverCodes[idx] = CODE_RUNNING;
                } else {
                    serverCodes[idx] = 2 + problem.ordinal();
                }
            }
        }

        // ---- 2. cooling items of all segments
        boolean heat = Config.safe(Config.HEAT_ENABLED, true);
        long coolingWatts = (long) Config.safe(Config.PASSIVE_COOLING, 300) * segs.size();
        long coolingPower = 0;
        for (RackBlockEntity seg : segs) {
            CoolingStats cooling = ModDataMaps.cooling(seg.items.get(SLOT_COOLING));
            if (cooling != null && heat) {
                coolingWatts += cooling.capacity();
                coolingPower += cooling.power();
            }
        }

        // ---- 3. energy: power cooling, then servers one by one while the buffers of all segments last
        long coolingFe = ServerSpec.energyPerTick(coolingPower, 1.0);
        if (Config.usesFe()) {
            long budget = 0;
            for (RackBlockEntity seg : segs) budget += seg.energy.getAmountAsLong();
            long needed = running.isEmpty() ? 0 : coolingFe;
            List<ServerSpec> powered = new ArrayList<>(running.size());
            List<Integer> poweredIdx = new ArrayList<>(running.size());
            for (int i = 0; i < running.size(); i++) {
                long cost = running.get(i).energyPerTick();
                if (needed + cost <= budget) {
                    needed += cost;
                    powered.add(running.get(i));
                    poweredIdx.add(runningIdx.get(i));
                } else {
                    serverCodes[runningIdx.get(i)] = 2 + ServerProblem.NO_ENERGY.ordinal();
                }
            }
            if (powered.isEmpty()) needed = 0;
            running = powered;
            runningIdx = poweredIdx;
            drainEnergy(segs, needed);
            fePerTick = needed;
            selfPowered = 0;
        } else if (bills && Config.safe(Config.SELF_CONSUMPTION, true) && !running.isEmpty()) {
            // electricity bill mode: own generators cover what they can, the rest goes on the bill (step 5)
            long needed = coolingFe;
            for (ServerSpec spec : running) needed += spec.energyPerTick();
            long budget = 0;
            for (RackBlockEntity seg : segs) budget += seg.energy.getAmountAsLong();
            long used = Math.min(needed, budget);
            drainEnergy(segs, used);
            fePerTick = used;
            selfPowered = needed > 0 ? used / (double) needed : 0;
        } else {
            fePerTick = 0;
            selfPowered = 0;
        }
        if (running.isEmpty()) coolingPower = 0;

        // ---- 4. heat: the running servers' watts heat the rack, cooling removes it; too hot = throttled
        watts = running.stream().mapToLong(ServerSpec::watts).sum();
        heatLoad = heat ? watts : 0;
        coolingCapacity = heat ? coolingWatts : 0;
        updateAmbient(level, gameTime);
        double throttleTemp = Config.safe(Config.THROTTLE_TEMPERATURE, 70.0);
        double criticalTemp = Config.safe(Config.CRITICAL_TEMPERATURE, 95.0);
        if (Double.isNaN(temperature)) temperature = ambient;
        if (heat) {
            temperature = HeatModel.step(temperature, HeatModel.targetTemperature(ambient, throttleTemp, heatLoad, coolingWatts));
            speedFactor = HeatModel.speedFactor(temperature, throttleTemp, criticalTemp, Config.safe(Config.MIN_SPEED, 0.25));
        } else {
            temperature = ambient;
            speedFactor = 1.0;
        }
        localRate = running.stream().mapToDouble(sp -> mining ? sp.hashesPerSecond() : sp.computeBps()).sum() * speedFactor;
        double storageTb = running.stream().mapToDouble(ServerSpec::storageGb).sum() / 1024.0;

        // ---- 5. electricity bill: wall power of servers and cooling
        if (bills && !running.isEmpty()) {
            double wallWatts = coolingPower;
            for (ServerSpec spec : running) wallWatts += spec.watts() / Math.max(0.01, spec.psuEfficiency());
            billPerHour = ElectricityBill.centsPerHour(wallWatts * (1.0 - selfPowered), Config.safe(Config.PRICE_PER_KWH, 40.0));
            bill.accrue(billPerHour / TICKS_PER_HOUR);
        } else {
            billPerHour = 0;
        }

        // ---- 6. what are we producing?
        ItemStack product = ItemStack.EMPTY;
        double speedBonus = 1.0;
        long ownDemand = 0;
        storageBonus = 0;
        CustomerBase.Params plans = null;
        long hostingLimit = 0;
        switch (type) {
            case MINING -> goal = 0;
            case HOSTING -> {
                goal = 0;
                plans = HostingPlan.params();
                storageBonus = Math.min(Config.MAX_STORAGE_BONUS.get(), storageTb * Config.STORAGE_BONUS_PER_TB.get());
                speedBonus = 1.0 + storageBonus;
                contracted = customers.contractedBps(plans.computeBps());
                hostingLimit = Config.HOSTING_CAPACITY_PER_SEGMENT.get() * segmentCount;
                // contracted compute plus room for the biggest plan, so new customers can also run on network compute
                ownDemand = payee != null ? (long) Math.min(hostingLimit, contracted + largestPlan(plans)) : 0;
            }
            default -> {
                ItemStack target = items.get(SLOT_TARGET);
                Item targetItem = target.isEmpty() ? null : target.getItem();
                if (targetItem != lastTarget) progress = 0;
                lastTarget = targetItem;
                product = target.isEmpty() ? ItemStack.EMPTY : new ItemStack(target.getItem()); // never copy components
                goal = Math.max(0, ModDataMaps.fileSize(target));
                speedBonus = Config.FABRICATION_SPEED.get();
                ownDemand = !product.isEmpty() && goal > 0 && canOutput(product)
                        ? Config.FABRICATING_INTAKE_PER_SEGMENT.get() * segmentCount : 0;
            }
        }
        if (type != RackType.HOSTING) contracted = 0;

        // ---- 7. local use first, then the network
        boolean allocationValid = type.networked() && allocationTick >= gameTime - 2;
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

        // ---- 8. status + production
        double served = contracted > 0 ? Math.min(1.0, effectiveRate / contracted) : 1.0;
        status = evaluate(type, running.isEmpty(), product, served);
        incomePerHour = 0;
        boolean producing = status.ok() && status != RackStatus.EXPORTING && status != RackStatus.NO_CUSTOMERS;
        if (producing) {
            switch (type) {
                case MINING -> earn(effectiveRate / 20.0 / MarketData.hashesPerCoin(coin, today) * MarketData.rewardMultiplier(coin, today));
                case HOSTING -> earn(customers.revenuePerHour(plans.pricePerHour()) * served * speedBonus * 100.0 / TICKS_PER_HOUR);
                default -> produce(product, effectiveRate * speedBonus / 20.0);
            }
        } else if (type == RackType.HOSTING && status == RackStatus.OVERLOADED && plans != null) {
            earn(customers.revenuePerHour(plans.pricePerHour()) * served * speedBonus * 100.0 / TICKS_PER_HOUR);
        }

        // ---- 9. hosting customers sign up / cancel
        if (type == RackType.HOSTING && plans != null && payee != null && gameTime % Config.safe(Config.CUSTOMER_INTERVAL, 200) == 0) {
            double spare = Math.min(effectiveRate, hostingLimit) - contracted;
            if (running.isEmpty() && effectiveRate <= 0) {
                served = contracted > 0 ? 0 : 1;
                spare = 0;
            }
            CustomerBase.Result result = customers.check(served, spare, plans, level.getRandom()::nextDouble);
            if (result.signedUp() >= 0) {
                ModAdvancements.award(server, payee, ModAdvancements.FIRST_CUSTOMER);
                if (result.signedUp() == HostingPlan.DEDICATED.ordinal()) {
                    ModAdvancements.award(server, payee, ModAdvancements.DEDICATED_CUSTOMER);
                }
            }
            contracted = customers.contractedBps(plans.computeBps());
        }

        // ---- 10. wear, payouts, bills, advancements
        if (Config.safe(Config.WEAR_ENABLED, true) && gameTime % WEAR_INTERVAL == 0 && !running.isEmpty()) {
            wearParts(segs, runningIdx, HeatModel.overheat(temperature, throttleTemp, criticalTemp));
        }
        if (gameTime % PAYOUT_INTERVAL == 0) {
            payOut(level);
            if (bills) payBill(server);
        }
        if (gameTime % ADVANCEMENT_INTERVAL == 0 && payee != null && !running.isEmpty()) {
            ModAdvancements.award(server, payee, ModAdvancements.FIRST_BOOT);
            if (running.size() == MAX_SERVERS) ModAdvancements.award(server, payee, ModAdvancements.FULL_RACK);
            if (speedFactor < 1.0) ModAdvancements.award(server, payee, ModAdvancements.OVERHEATED);
        }

        boolean powered = !running.isEmpty();
        for (RackBlockEntity seg : segs) {
            seg.setRunningState(powered);
        }
        int signal = computeSignal(segs);
        if (signal != lastSignal) {
            lastSignal = signal;
            for (RackBlockEntity seg : segs) level.updateNeighbourForOutputSignal(seg.worldPosition, seg.getBlockState().getBlock());
        }
        setChanged();
    }

    private static double largestPlan(CustomerBase.Params plans) {
        double largest = 0;
        for (int i = 0; i < plans.computeBps().length; i++) {
            if (plans.weight()[i] > 0) largest = Math.max(largest, plans.computeBps()[i]);
        }
        return largest;
    }

    private void updateAmbient(ServerLevel level, long gameTime) {
        if (gameTime - ambientCheckedAt < 200 && ambientCheckedAt != Long.MIN_VALUE) return;
        ambientCheckedAt = gameTime;
        ambient = Config.safe(Config.BIOME_TEMPERATURE, true)
                ? HeatModel.ambientFromBiome(level.getBiome(worldPosition).value().getBaseTemperature())
                : 20.0;
    }

    /** Wears the parts of every running server (more when the rack is hot). */
    private void wearParts(List<RackBlockEntity> segs, List<Integer> runningIdx, double overheat) {
        double amount = Config.safe(Config.WEAR_PER_HOUR, 0.03) * WEAR_INTERVAL / TICKS_PER_HOUR
                * (1.0 + (Config.safe(Config.HEAT_WEAR_MULTIPLIER, 3.0) - 1.0) * overheat);
        double threshold = Config.safe(Config.WARNING_THRESHOLD, 0.10);
        List<Wear.WearEvent> events = new ArrayList<>();
        for (int idx : runningIdx) {
            RackBlockEntity seg = segs.get(idx / SERVERS);
            ItemStack bay = seg.items.get(idx % SERVERS);
            if (bay.is(ModItems.SERVER.get())) {
                events.addAll(Wear.wearServer(bay, amount, threshold));
            } else {
                Wear.WearEvent event = Wear.wear(bay, amount, threshold);
                if (event != null) events.add(event);
            }
            seg.setChanged();
        }
        if (!events.isEmpty() && level instanceof ServerLevel serverLevel) warnOwner(serverLevel, events);
    }

    /** Tells the (online) owner about parts that dropped below the warning threshold or broke: chat + toast. */
    private void warnOwner(ServerLevel level, List<Wear.WearEvent> events) {
        UUID payee = payee();
        if (payee == null) return;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(payee);
        if (player == null) return;
        Component rack = Component.translatable("block.datacenterextended." + type().id());
        String where = worldPosition.getX() + " " + worldPosition.getY() + " " + worldPosition.getZ();
        for (Wear.WearEvent event : events) {
            Component message = event.broke()
                    ? Component.translatable("message.datacenterextended.wear.broke", rack, where, event.part().getHoverName())
                    .withStyle(ChatFormatting.RED)
                    : Component.translatable("message.datacenterextended.wear.low", rack, where, event.part().getHoverName(),
                    Math.round(event.condition() * 100)).withStyle(ChatFormatting.GOLD);
            player.sendSystemMessage(message);
            if (Config.safe(Config.WEAR_TOASTS, true)) {
                EconomyService.send(player, new ModPayloads.WearWarning(event.part(), (float) event.condition(), event.broke(), where));
            }
        }
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

    private RackStatus evaluate(RackType type, boolean noneRunning, ItemStack product, double served) {
        boolean bills = Config.safe(Config.ELECTRICITY_BILLS, false);
        if ((type != RackType.FABRICATING || bills) && payee() == null) return RackStatus.NO_OWNER;
        if (bill.unpaid()) return RackStatus.NO_FUNDS;
        if (!coinLaunched) return RackStatus.COIN_NOT_LAUNCHED;
        // a rack without own servers still works on compute it receives over the network
        if (noneRunning && effectiveRate <= 0) {
            for (int code : serverCodes) {
                if (code == 2 + ServerProblem.NO_ENERGY.ordinal()) return RackStatus.NO_ENERGY;
            }
            return contracted > 0 ? RackStatus.OVERLOADED : RackStatus.NO_SERVERS;
        }
        if (type == RackType.FABRICATING) {
            if (product.isEmpty()) return supply > 0 && exported > 0 ? RackStatus.EXPORTING : RackStatus.NO_TARGET;
            if (goal <= 0) return RackStatus.NOT_FABRICABLE;
            if (!canOutput(product)) return supply > 0 && exported > 0 ? RackStatus.EXPORTING : RackStatus.OUTPUT_FULL;
        }
        if (type == RackType.HOSTING) {
            if (contracted > 0 && served < 0.999) return RackStatus.OVERLOADED;
            if (customers.total() == 0) return RackStatus.NO_CUSTOMERS;
        }
        if (effectiveRate <= 0) return exported > 0 ? RackStatus.EXPORTING : RackStatus.IDLE;
        if (speedFactor < 1.0) return RackStatus.OVERHEATING;
        return RackStatus.RUNNING;
    }

    /** Accumulates income for this tick: cents (hosting) or coins (mining). */
    private void earn(double perTick) {
        pending += perTick;
        incomePerHour = perTick * TICKS_PER_HOUR;
    }

    /** Pays the accumulated income into the owner's wallet. */
    private void payOut(ServerLevel level) {
        UUID payee = payee();
        if (payee == null || pending <= 0) return;
        WalletData wallets = WalletData.get(level.getServer());
        if (type() == RackType.MINING) {
            wallets.depositCoins(payee, coin, pending);
            StatsData.record(level.getServer(), payee, StatsLedger.Category.MINING,
                    Math.round(pending * MarketData.get(level.getServer()).price(coin) * 100.0));
            pending = 0;
            ModAdvancements.award(level.getServer(), payee, ModAdvancements.FIRST_COIN);
        } else if (type() == RackType.HOSTING) {
            long cents = (long) Math.floor(pending);
            wallets.deposit(payee, cents);
            StatsData.record(level.getServer(), payee, StatsLedger.Category.HOSTING, cents);
            pending -= cents;
        }
    }

    /** Takes the electricity bill from the owner's wallet; a wallet that can't pay shuts the rack down until it can. */
    private void payBill(MinecraftServer server) {
        UUID payee = payee();
        if (payee == null) return;
        WalletData wallets = WalletData.get(server);
        long take = bill.settle(wallets.wallet(payee).cents());
        if (take > 0 && wallets.withdraw(payee, take)) StatsData.record(server, payee, StatsLedger.Category.BILLS, take);
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

    /** The owner name of the merged rack (any segment). */
    public String payeeName() {
        if (owner != null) return ownerName;
        for (RackBlockEntity seg : segments()) {
            if (seg.owner != null) return seg.ownerName;
        }
        return "";
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

    /** Own compute (B/s) or hashrate (H/s) of all running servers in the merged rack (after throttling). */
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

    /** Rack temperature in °C. */
    public double temperature() {
        return Double.isNaN(temperature) ? ambient : temperature;
    }

    /** Comparator signal 0..15 of the merged rack, by the selected {@link RackComparatorMode}. */
    private int computeSignal(List<RackBlockEntity> segs) {
        return switch (comparatorMode) {
            case STATUS -> !status.ok() ? 0 : status.warning() ? 8 : 15;
            case TEMPERATURE -> {
                double critical = Config.safe(Config.CRITICAL_TEMPERATURE, 95.0);
                double t = (temperature() - ambient) / Math.max(1.0, critical - ambient);
                yield (int) Math.round(Math.max(0, Math.min(1, t)) * 15);
            }
            case ENERGY -> {
                if (!Config.usesFe()) yield bill.unpaid() ? 0 : 15;
                long stored = 0, capacity = 0;
                for (RackBlockEntity seg : segs) {
                    stored += seg.energy.getAmountAsLong();
                    capacity += seg.energy.getCapacityAsLong();
                }
                yield capacity <= 0 ? 0 : (int) Math.round(15.0 * stored / capacity);
            }
        };
    }

    /** The comparator signal of the rack this block belongs to. */
    public int comparatorSignal() {
        return controllerOrSelf().lastSignal;
    }

    public RackComparatorMode comparatorMode() {
        return comparatorMode;
    }

    public void cycleComparatorMode() {
        comparatorMode = RackComparatorMode.byId((comparatorMode.ordinal() + 1) % RackComparatorMode.values().length);
        setChanged();
    }

    /** Sets the temperature directly (game tests, commands). */
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    /** Speed multiplier from heat (1.0 = not throttled). */
    public double speedFactor() {
        return speedFactor;
    }

    /** Watts of heat the running servers produce. */
    public long heatLoad() {
        return heatLoad;
    }

    /** Watts of heat the rack can remove (passive + cooling items). */
    public long coolingCapacity() {
        return coolingCapacity;
    }

    /** Electricity bill in cents per hour (0 unless electricity bills are on). */
    public double billPerHour() {
        return billPerHour;
    }

    public ElectricityBill bill() {
        return bill;
    }

    public CustomerBase customers() {
        return customers;
    }

    /** Sets the number of customers of a plan (game tests, commands). */
    public void setCustomers(HostingPlan plan, int count) {
        customers.setCount(plan.ordinal(), count);
        setChanged();
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

    /** FE per tick the running servers (and cooling) of this merged rack use. */
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
        if (slot < SERVERS) return isBayItem(stack);
        if (slot == SLOT_COOLING) return isCoolingItem(stack);
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
        temperature = input.getDoubleOr("temperature", Double.NaN);
        bill.load(input.getDoubleOr("bill", 0), input.getBooleanOr("unpaid", false));
        for (HostingPlan plan : HostingPlan.values()) {
            customers.setCount(plan.ordinal(), input.getIntOr("customers_" + plan.name().toLowerCase(java.util.Locale.ROOT), 0));
        }
        customers.setSatisfaction(input.getDoubleOr("satisfaction", 1.0));
        comparatorMode = RackComparatorMode.byId(input.getIntOr("comparator", 0));
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
        if (!Double.isNaN(temperature)) output.putDouble("temperature", temperature);
        if (bill.pending() > 0) output.putDouble("bill", bill.pending());
        if (bill.unpaid()) output.putBoolean("unpaid", true);
        for (HostingPlan plan : HostingPlan.values()) {
            int count = customers.count(plan.ordinal());
            if (count > 0) output.putInt("customers_" + plan.name().toLowerCase(java.util.Locale.ROOT), count);
        }
        output.putDouble("satisfaction", customers.satisfaction());
        output.putInt("comparator", comparatorMode.ordinal());
    }
}
