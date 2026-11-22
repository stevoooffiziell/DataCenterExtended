package de.f4ked.datacenterextended.power;

import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.advancement.ModAdvancements;
import de.f4ked.datacenterextended.economy.StatsData;
import de.f4ked.datacenterextended.economy.StatsLedger;
import de.f4ked.datacenterextended.economy.WalletData;
import de.f4ked.datacenterextended.menu.DataSlots;
import de.f4ked.datacenterextended.power.model.GridModel;
import de.f4ked.datacenterextended.registry.ModBlockEntities;
import de.f4ked.datacenterextended.time.CalendarData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Sells FE to the grid: everything it receives (from adjacent generators or a power network) is exported at the
 * feed-in tariff and paid into the owner's wallet. Networks feed it last, after consumers and batteries.
 */
public class GridConnectionBlockEntity extends PowerBlockEntity {
    private static final int PAYOUT_INTERVAL = 100;

    private final GridTier tier;
    private double feSincePayout;
    private double cents;
    private double standingDue;
    private long lastDay = Long.MIN_VALUE;
    private long earned;
    /** Average export in FE/t. */
    private double exportRate;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= DATA_EXTRA && index < DATA_EXTRA + 4) return DataSlots.chunk(earned, index, DATA_EXTRA);
            if (index >= DATA_EXTRA2 && index < DATA_EXTRA2 + 4) {
                return DataSlots.chunk(Math.round(priceCentsPerKWh() * 100), index, DATA_EXTRA2);
            }
            return baseData(index, owner() == null ? 1 : 0, exportRate, tier.rate());
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public GridConnectionBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, state.getBlock() instanceof GridConnectionBlock block ? block.tier() : GridTier.RESIDENTIAL);
    }

    private GridConnectionBlockEntity(BlockPos pos, BlockState state, GridTier tier) {
        super(ModBlockEntities.GRID_CONNECTION.get(), pos, state, 0, tier.rate() * 2, tier.rate(), 0);
        this.tier = tier;
    }

    public GridTier tier() {
        return tier;
    }

    public double exportRate() {
        return exportRate;
    }

    public long earned() {
        return earned;
    }

    @Override
    protected boolean acceptsEnergy() {
        return owner() != null;
    }

    @Override
    public int kind() {
        return KIND_GRID;
    }

    @Override
    public int typeOrdinal() {
        return tier.ordinal();
    }

    @Override
    protected ContainerData dataAccess() {
        return dataAccess;
    }

    /** Current feed-in tariff in cents per kWh (per real-time hour). */
    public double priceCentsPerKWh() {
        double year = 2010;
        if (level != null && level.getServer() != null) {
            LocalDate today = CalendarData.get(level.getServer()).today();
            year = today.getYear() + (today.getDayOfYear() - 1) / 365.25;
        }
        return Config.safe(Config.GRID_EXPORT_PRICE, 3.0) * 100.0 * GridModel.growth(year, Config.safe(Config.GRID_YEARLY_GROWTH, 0.065));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GridConnectionBlockEntity be) {
        be.tick((ServerLevel) level);
    }

    private void tick(ServerLevel level) {
        long received = energy.getAmountAsLong();
        if (received > 0) energy.set(0);
        feSincePayout += received;
        exportRate += (received - exportRate) * 0.05;

        MinecraftServer server = level.getServer();
        long day = (long) Math.floor(CalendarData.now(server));
        if (day != lastDay) {
            // the standing charge is only ever taken from the current day's earnings
            standingDue = lastDay == Long.MIN_VALUE ? 0 : Config.safe(Config.GRID_STANDING_CHARGE, 0.01) * 100.0;
            lastDay = day;
        }
        if ((level.getGameTime() + worldPosition.asLong()) % PAYOUT_INTERVAL == 0) payOut(server);
    }

    private void payOut(MinecraftServer server) {
        UUID payee = owner();
        if (payee == null || feSincePayout <= 0) return;
        double fePerWatt = Config.safe(Config.FE_PER_WATT, 0.1);
        cents += GridModel.centsForFe(feSincePayout, fePerWatt, priceCentsPerKWh() / 100.0, 1.0);
        feSincePayout = 0;
        long whole = (long) Math.floor(cents);
        if (whole <= 0) return;
        long fee = Math.min(whole, (long) Math.ceil(standingDue));
        standingDue = Math.max(0, standingDue - fee);
        cents -= whole;
        long pay = whole - fee;
        if (pay <= 0) return;
        WalletData.get(server).deposit(payee, pay);
        StatsData.record(server, payee, StatsLedger.Category.GRID, pay);
        earned += pay;
        setChanged();
        ModAdvancements.award(server, payee, ModAdvancements.FEEDING_THE_GRID);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        cents = input.getDoubleOr("cents", 0);
        earned = input.getLongOr("earned", 0);
        standingDue = input.getDoubleOr("standing_due", 0);
        lastDay = input.getLongOr("last_day", Long.MIN_VALUE);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (cents > 0) output.putDouble("cents", cents);
        if (earned > 0) output.putLong("earned", earned);
        if (standingDue > 0) output.putDouble("standing_due", standingDue);
        if (lastDay != Long.MIN_VALUE) output.putLong("last_day", lastDay);
    }
}
