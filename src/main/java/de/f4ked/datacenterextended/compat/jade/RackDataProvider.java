package de.f4ked.datacenterextended.compat.jade;

import de.f4ked.datacenterextended.Datacenterextended;
import de.f4ked.datacenterextended.block.RackBlockEntity;
import de.f4ked.datacenterextended.hosting.HostingPlan;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/** Server side of the Jade integration: sends the controller's state (see {@link RackComponentProvider}). */
public enum RackDataProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    static final Identifier UID = Datacenterextended.id("rack");

    @Override
    public Identifier getUid() {
        return UID;
    }


    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof RackBlockEntity be)) return;
        RackBlockEntity rack = be.controllerOrSelf();
        data.putInt("status", rack.status().ordinal());
        data.putInt("type", rack.type().ordinal());
        data.putInt("coin", rack.coin().ordinal());
        data.putDouble("income", rack.incomePerHour());
        data.putDouble("rate", rack.localRate());
        data.putLong("energy", rack.getEnergyHandler().getAmountAsLong());
        long stored = 0, capacity = 0;
        for (RackBlockEntity seg : rack.segments()) {
            stored += seg.getEnergyHandler().getAmountAsLong();
            capacity += seg.getEnergyHandler().getCapacityAsLong();
        }
        data.putLong("stored", stored);
        data.putLong("capacity", capacity);
        data.putLong("fe", rack.energyPerTick());
        data.putDouble("bill", rack.billPerHour());
        data.putDouble("temperature", rack.temperature());
        data.putDouble("speed", rack.speedFactor());
        for (HostingPlan plan : HostingPlan.values()) {
            data.putInt("plan" + plan.ordinal(), rack.customers().count(plan.ordinal()));
        }
        data.putDouble("satisfaction", rack.customers().satisfaction());
        data.putString("owner", rack.payeeName());
    }
}
