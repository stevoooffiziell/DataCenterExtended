package de.f4ked.datacenterextended.block;

import de.f4ked.datacenterextended.hosting.HostingPlan;
import de.f4ked.datacenterextended.registry.ModBlockEntities;
import de.f4ked.datacenterextended.time.CalendarData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

/**
 * Copies the state of an adjacent rack (behind the screen first, then any other side) once per second and sends it to
 * clients when it changes. The renderer draws it on the front face.
 */
public class ServerMonitorBlockEntity extends BlockEntity {
    private static final int UPDATE_INTERVAL = 20;

    /** What the screen shows. {@code connected = false}: no rack next to it. */
    public record Snapshot(boolean connected, int status, int type, int coin, double temperature, double speed,
                           double income, int[] customers, double satisfaction, double epochDay, int signal) {
        public static final Snapshot NONE = new Snapshot(false, 0, 0, 0, 0, 1, 0, new int[HostingPlan.count()], 1, 0, 0);

        boolean sameAs(Snapshot o) {
            return connected == o.connected && status == o.status && type == o.type && coin == o.coin
                    && Math.abs(temperature - o.temperature) < 0.05 && Math.abs(speed - o.speed) < 0.001
                    && Math.abs(income - o.income) < Math.max(0.01, Math.abs(income) * 0.001)
                    && Arrays.equals(customers, o.customers) && Math.abs(satisfaction - o.satisfaction) < 0.001
                    && (long) epochDay == (long) o.epochDay && signal == o.signal;
        }
    }

    private Snapshot snapshot = Snapshot.NONE;

    public ServerMonitorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SERVER_MONITOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ServerMonitorBlockEntity be) {
        if (level.getGameTime() % UPDATE_INTERVAL != Math.floorMod(pos.asLong(), UPDATE_INTERVAL)) return;
        Snapshot next = be.read((ServerLevel) level, state);
        if (!next.sameAs(be.snapshot)) {
            boolean signalChanged = next.signal != be.snapshot.signal;
            be.snapshot = next;
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, 2);
            if (signalChanged) level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    private Snapshot read(ServerLevel level, BlockState state) {
        RackBlockEntity rack = findRack(level, state);
        if (rack == null) return Snapshot.NONE;
        RackBlockEntity c = rack.controllerOrSelf();
        int[] customers = new int[HostingPlan.count()];
        for (int i = 0; i < customers.length; i++) customers[i] = c.customers().count(i);
        return new Snapshot(true, c.status().ordinal(), c.type().ordinal(), c.coin().ordinal(), c.temperature(), c.speedFactor(),
                c.incomePerHour(), customers, c.customers().satisfaction(), CalendarData.now(level.getServer()), c.comparatorSignal());
    }

    private @Nullable RackBlockEntity findRack(Level level, BlockState state) {
        Direction facing = state.hasProperty(ServerMonitorBlock.FACING) ? state.getValue(ServerMonitorBlock.FACING) : Direction.NORTH;
        if (level.getBlockEntity(worldPosition.relative(facing.getOpposite())) instanceof RackBlockEntity behind) return behind;
        for (Direction d : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(d)) instanceof RackBlockEntity rack) return rack;
        }
        return null;
    }

    public Snapshot snapshot() {
        return snapshot;
    }

    public int signal() {
        return snapshot.signal;
    }

    // ------------------------------------------------------------------ sync + persistence

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        Snapshot s = snapshot;
        output.putBoolean("connected", s.connected);
        output.putInt("status", s.status);
        output.putInt("type", s.type);
        output.putInt("coin", s.coin);
        output.putDouble("temperature", s.temperature);
        output.putDouble("speed", s.speed);
        output.putDouble("income", s.income);
        for (int i = 0; i < s.customers.length; i++) output.putInt("plan" + i, s.customers[i]);
        output.putDouble("satisfaction", s.satisfaction);
        output.putDouble("day", s.epochDay);
        output.putInt("signal", s.signal);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int[] customers = new int[HostingPlan.count()];
        for (int i = 0; i < customers.length; i++) customers[i] = input.getIntOr("plan" + i, 0);
        snapshot = new Snapshot(input.getBooleanOr("connected", false), input.getIntOr("status", 0), input.getIntOr("type", 0),
                input.getIntOr("coin", 0), input.getDoubleOr("temperature", 0), input.getDoubleOr("speed", 1),
                input.getDoubleOr("income", 0), customers, input.getDoubleOr("satisfaction", 1), input.getDoubleOr("day", 0),
                input.getIntOr("signal", 0));
        Objects.requireNonNull(snapshot);
    }
}
