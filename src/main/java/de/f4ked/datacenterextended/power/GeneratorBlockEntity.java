package de.f4ked.datacenterextended.power;

import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.advancement.ModAdvancements;
import de.f4ked.datacenterextended.power.model.HydroModel;
import de.f4ked.datacenterextended.power.model.Output;
import de.f4ked.datacenterextended.power.model.ReactorModel;
import de.f4ked.datacenterextended.power.model.SolarModel;
import de.f4ked.datacenterextended.power.model.WindModel;
import de.f4ked.datacenterextended.registry.ModBlockEntities;
import de.f4ked.datacenterextended.registry.ModDataComponents;
import de.f4ked.datacenterextended.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Containers;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * One generator of any {@link GeneratorType}. Each tick it produces FE into its buffer according to its family's
 * model and pushes it into adjacent energy receivers; power cables pull it through the {@link PowerNetworkManager}.
 */
public class GeneratorBlockEntity extends PowerBlockEntity {
    private static final int ENVIRONMENT_INTERVAL = 20;

    private final GeneratorType type;
    private double output;
    private double carry;
    private GeneratorStatus status = GeneratorStatus.RUNNING;
    private @Nullable Output environment;
    // combustion
    private int burnTime;
    private int burnTotal;
    // nuclear
    private double heat;
    private boolean scrammed;
    private double coolant = -1;
    private boolean awardedRunning;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == DATA_HEAT) return (int) Math.round(heat * 1000);
            if (index == DATA_BURN) return burnTotal <= 0 ? 0 : (int) Math.round(1000.0 * burnTime / burnTotal);
            return baseData(index, status.ordinal(), output, peak());
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, state.getBlock() instanceof GeneratorBlock block ? block.type() : GeneratorType.POLYCRYSTALLINE_SOLAR);
    }

    private GeneratorBlockEntity(BlockPos pos, BlockState state, GeneratorType type) {
        super(ModBlockEntities.GENERATOR.get(), pos, state, type.slotCount(), type.bufferCapacity(), 0, type.pushRate());
        this.type = type;
    }

    public GeneratorType type() {
        return type;
    }

    @Override
    public int kind() {
        return KIND_GENERATOR;
    }

    @Override
    public int typeOrdinal() {
        return type.ordinal();
    }

    /** Peak FE/t including the config multiplier. */
    public double peak() {
        return type.peak() * Config.outputMultiplier(type.family());
    }

    public double output() {
        return output;
    }

    public GeneratorStatus status() {
        return status;
    }

    public double heat() {
        return heat;
    }

    @Override
    protected ContainerData dataAccess() {
        return dataAccess;
    }

    // ================================================================== ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, GeneratorBlockEntity be) {
        be.tick((ServerLevel) level, state);
    }

    private void tick(ServerLevel level, BlockState state) {
        boolean full = energy.getAmountAsLong() >= energy.getCapacityAsLong();
        Output out = switch (type.family()) {
            case COMBUSTION -> combustion(level, full);
            case NUCLEAR -> reactor(level, full);
            default -> environment(level, state);
        };
        double produced = full ? 0 : peak() * out.factor();
        status = full && out.status().producing() ? GeneratorStatus.BUFFER_FULL : out.status();
        output = produced;
        carry += produced;
        long whole = (long) carry;
        carry -= whole;
        addEnergy(whole);

        for (Direction side : Direction.values()) pushTo(level, side, type.pushRate());

        // a full buffer keeps the state: avoids flickering block updates (and network rebuilds) every tick
        boolean active = status.producing();
        if (state.hasProperty(GeneratorBlock.ACTIVE) && state.getValue(GeneratorBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(GeneratorBlock.ACTIVE, active), 3);
        }
        if (type.family() == GeneratorFamily.NUCLEAR && produced > 0 && !awardedRunning && owner() != null) {
            awardedRunning = true;
            ModAdvancements.award(level.getServer(), owner(), ModAdvancements.SPLITTING_ATOMS);
        }
    }

    /** Solar, wind, water and geothermal depend on the surroundings, which are checked once per second. */
    private Output environment(ServerLevel level, BlockState state) {
        if (environment == null || (level.getGameTime() + worldPosition.asLong()) % ENVIRONMENT_INTERVAL == 0) {
            environment = switch (type.family()) {
                case SOLAR -> SolarModel.output(level.getDefaultClockTime(), level.getRainLevel(1.0F), level.getThunderLevel(1.0F),
                        level.canSeeSky(worldPosition.above()), level.dimensionType().hasSkyLight() && !level.dimensionType().hasFixedTime());
                case WIND -> WindModel.output(worldPosition.getY(), level.getSeaLevel(), freeRotorArea(level, state),
                        level.getRainLevel(1.0F), level.getThunderLevel(1.0F), level.getGameTime(), worldPosition.asLong());
                case WATER -> type == GeneratorType.WATER_WHEEL ? HydroModel.wheel(flowingSides(level))
                        : HydroModel.turbine(waterHead(level), type.maxHead());
                case GEOTHERMAL -> HydroModel.geothermal(adjacentLava(level), type.lavaForFull(), level.dimension() == Level.NETHER ? 1.5 : 1.0);
                default -> Output.off(GeneratorStatus.RUNNING);
            };
        }
        return environment;
    }

    /** Share of the square of radius {@link GeneratorType#rotorRadius()} in front of the turbine that is free. */
    private double freeRotorArea(ServerLevel level, BlockState state) {
        Direction facing = state.hasProperty(GeneratorBlock.FACING) ? state.getValue(GeneratorBlock.FACING) : Direction.NORTH;
        Direction right = facing.getClockWise();
        BlockPos front = worldPosition.relative(facing);
        int r = type.rotorRadius(), free = 0, total = 0;
        for (int a = -r; a <= r; a++) {
            for (int b = -r; b <= r; b++) {
                BlockPos p = front.relative(right, a).above(b);
                total++;
                if (level.getBlockState(p).getCollisionShape(level, p).isEmpty()) free++;
            }
        }
        return total == 0 ? 1 : free / (double) total;
    }

    private int flowingSides(ServerLevel level) {
        int count = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            FluidState fluid = level.getFluidState(worldPosition.relative(d));
            if (fluid.is(FluidTags.WATER) && !fluid.isSource()) count++;
        }
        return count;
    }

    private int waterHead(ServerLevel level) {
        int head = 0;
        BlockPos.MutableBlockPos p = worldPosition.mutable();
        for (int i = 0; i < type.maxHead(); i++) {
            p.move(Direction.UP);
            if (!level.getFluidState(p).is(FluidTags.WATER)) break;
            head++;
        }
        return head;
    }

    private int adjacentLava(ServerLevel level) {
        int count = 0;
        for (Direction d : Direction.values()) {
            FluidState fluid = level.getFluidState(worldPosition.relative(d));
            if (fluid.is(FluidTags.LAVA) && fluid.isSource()) count++;
        }
        return count;
    }

    // ------------------------------------------------------------------ combustion

    private Output combustion(ServerLevel level, boolean full) {
        if (burnTime > 0) {
            if (full) return new Output(0, GeneratorStatus.BUFFER_FULL); // pause, keep the fuel burning later
            burnTime--;
            return new Output(1, GeneratorStatus.RUNNING);
        }
        burnTotal = 0;
        if (full) return Output.off(GeneratorStatus.BUFFER_FULL);
        ItemStack fuel = items.getFirst();
        int duration = burnDuration(level, fuel);
        if (duration <= 0) return Output.off(GeneratorStatus.NO_FUEL);
        // the basic generator turns one furnace burn tick into 40 FE; the gas turbine is twice as efficient
        burnTotal = burnTime = (int) Math.max(1, Math.round(duration * type.fuelEfficiency() * 40.0 / type.peak()));
        ItemStackTemplate remainder = fuel.getCraftingRemainder();
        fuel.shrink(1);
        if (remainder != null) {
            if (fuel.isEmpty()) items.set(0, remainder.create());
            else Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), remainder.create());
        }
        setChanged();
        burnTime--;
        return new Output(1, GeneratorStatus.RUNNING);
    }

    private int burnDuration(ServerLevel level, ItemStack fuel) {
        if (fuel.isEmpty()) return 0;
        return ResolvableInt.getFromItem(fuel, DataComponents.COOKING_FUEL, CookingFuel::burnTime, getLootContext(level, fuel), 0);
    }

    // ------------------------------------------------------------------ nuclear

    private Output reactor(ServerLevel level, boolean full) {
        if ((level.getGameTime() + worldPosition.asLong()) % ENVIRONMENT_INTERVAL == 0 || coolant < 0) coolant = coolantUnits(level);
        int rods = 0;
        for (ItemStack stack : items) if (isFreshRod(stack)) rods++;
        double power = 0;
        GeneratorStatus reason;
        if (scrammed) {
            reason = GeneratorStatus.SCRAM;
        } else if (rods == 0) {
            reason = GeneratorStatus.NO_RODS;
        } else if (full) {
            reason = GeneratorStatus.BUFFER_FULL; // load following: control rods in, nothing produced
        } else {
            double throttle = ReactorModel.throttle(heat);
            power = rods / (double) GeneratorType.REACTOR_RODS * throttle;
            reason = throttle < 1.0 ? GeneratorStatus.THROTTLED : GeneratorStatus.RUNNING;
            burnRods(throttle);
        }
        heat = ReactorModel.step(heat, power, coolant);
        if (!scrammed && heat >= ReactorModel.SCRAM) {
            scrammed = true;
            power = 0;
            reason = GeneratorStatus.SCRAM;
            warnOwner(level);
        }
        return new Output(power, reason);
    }

    /** Adjacent water sources cool 1 unit each, blue ice 1.5, packed ice 0.75. */
    private double coolantUnits(ServerLevel level) {
        double units = 0;
        for (Direction d : Direction.values()) {
            BlockPos p = worldPosition.relative(d);
            FluidState fluid = level.getFluidState(p);
            if (fluid.is(FluidTags.WATER) && fluid.isSource()) units += 1.0;
            else if (level.getBlockState(p).is(Blocks.BLUE_ICE)) units += 1.5;
            else if (level.getBlockState(p).is(Blocks.PACKED_ICE)) units += 0.75;
        }
        return units;
    }

    private static boolean isFreshRod(ItemStack stack) {
        return stack.is(ModItems.FUEL_ROD.get()) && stack.getOrDefault(ModDataComponents.WEAR.get(), 0.0F) < 1.0F;
    }

    private void burnRods(double throttle) {
        double ticksPerRod = Config.safe(Config.FUEL_ROD_DAYS, 7.0) * Config.safe(Config.TICKS_PER_DAY, 2400);
        float step = (float) (throttle / Math.max(1.0, ticksPerRod));
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!isFreshRod(stack)) continue;
            float wear = stack.getOrDefault(ModDataComponents.WEAR.get(), 0.0F) + step;
            if (wear >= 1.0F) items.set(i, new ItemStack(ModItems.SPENT_FUEL_ROD.get()));
            else stack.set(ModDataComponents.WEAR.get(), wear);
        }
        setChanged();
    }

    private void warnOwner(ServerLevel level) {
        UUID id = owner();
        if (id == null) return;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
        if (player == null) return;
        String where = worldPosition.getX() + " " + worldPosition.getY() + " " + worldPosition.getZ();
        player.sendSystemMessage(Component.translatable("message.datacenterextended.reactor.scram", where).withStyle(ChatFormatting.RED));
    }

    /** GUI button 0: restart a cooled-down reactor after a SCRAM. */
    @Override
    public void handleButton(int id) {
        if (id == 0 && scrammed && heat < ReactorModel.RESTART_BELOW) {
            scrammed = false;
            setChanged();
        }
    }

    /** Sets the core heat (game tests, admin fixes). */
    public void setHeat(double heat) {
        this.heat = Math.max(0, Math.min(1, heat));
        setChanged();
    }

    public boolean scrammed() {
        return scrammed;
    }

    // ================================================================== container

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (type.family()) {
            case COMBUSTION -> stack.has(DataComponents.COOKING_FUEL);
            case NUCLEAR -> stack.is(ModItems.FUEL_ROD.get()) && items.get(slot).isEmpty();
            default -> false;
        };
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return type.family() == GeneratorFamily.NUCLEAR ? 1 : super.getMaxStackSize(stack);
    }

    /** Hoppers may pull out spent rods and empty buckets. */
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return stack.is(ModItems.SPENT_FUEL_ROD.get()) || (type.family() == GeneratorFamily.COMBUSTION && !stack.has(DataComponents.COOKING_FUEL));
    }

    // ================================================================== persistence

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        burnTime = input.getIntOr("burn", 0);
        burnTotal = input.getIntOr("burn_total", 0);
        heat = input.getDoubleOr("heat", 0);
        scrammed = input.getBooleanOr("scram", false);
        carry = input.getDoubleOr("carry", 0);
        coolant = -1;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (burnTime > 0) {
            output.putInt("burn", burnTime);
            output.putInt("burn_total", burnTotal);
        }
        if (heat > 0) output.putDouble("heat", heat);
        if (scrammed) output.putBoolean("scram", true);
        if (carry > 0) output.putDouble("carry", carry);
    }

    /** Data for Jade and the multimeter. */
    public long storedEnergy() {
        return energy.getAmountAsLong();
    }
}
