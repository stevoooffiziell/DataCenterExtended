package de.f4ked.datacenterextended.compat.jade;

import de.f4ked.datacenterextended.Datacenterextended;
import de.f4ked.datacenterextended.power.BatteryBlockEntity;
import de.f4ked.datacenterextended.power.GeneratorBlockEntity;
import de.f4ked.datacenterextended.power.GridConnectionBlockEntity;
import de.f4ked.datacenterextended.power.PowerBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/** Server side of the Jade integration for generators, batteries and grid connections. */
public enum PowerDataProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    static final Identifier UID = Datacenterextended.id("power");

    @Override
    public Identifier getUid() {
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof PowerBlockEntity be)) return;
        data.putInt("kind", be.kind());
        data.putLong("stored", be.energy().getAmountAsLong());
        data.putLong("capacity", be.energy().getCapacityAsLong());
        data.putString("owner", be.ownerName());
        switch (be) {
            case GeneratorBlockEntity gen -> {
                data.putInt("status", gen.status().ordinal());
                data.putDouble("output", gen.output());
                data.putDouble("peak", gen.peak());
                data.putDouble("heat", gen.heat());
                data.putBoolean("nuclear", gen.type().family() == de.f4ked.datacenterextended.power.GeneratorFamily.NUCLEAR);
            }
            case BatteryBlockEntity battery -> data.putDouble("flow", battery.netFlow());
            case GridConnectionBlockEntity grid -> {
                data.putDouble("output", grid.exportRate());
                data.putDouble("peak", grid.tier().rate());
                data.putLong("earned", grid.earned());
            }
            default -> {
            }
        }
    }
}
