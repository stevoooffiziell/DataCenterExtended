package de.f4ked.datacenterextended.compat.jade;

import de.f4ked.datacenterextended.block.RackBlock;
import de.f4ked.datacenterextended.block.RackBlockEntity;
import de.f4ked.datacenterextended.power.PowerBlock;
import de.f4ked.datacenterextended.power.PowerBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade integration (optional: Jade only loads this class when it is installed). Looking at any rack block shows the
 * merged rack's status, income, energy and temperature.
 */
@WailaPlugin
public class JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(RackDataProvider.INSTANCE, RackBlockEntity.class);
        registration.registerBlockDataProvider(PowerDataProvider.INSTANCE, PowerBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(RackComponentProvider.INSTANCE, RackBlock.class);
        registration.registerBlockComponent(PowerComponentProvider.INSTANCE, PowerBlock.class);
    }
}
