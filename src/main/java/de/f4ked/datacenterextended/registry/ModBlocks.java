package de.f4ked.datacenterextended.registry;

import de.f4ked.datacenterextended.Datacenterextended;
import de.f4ked.datacenterextended.block.DataCableBlock;
import de.f4ked.datacenterextended.block.RackBlock;
import de.f4ked.datacenterextended.block.RackType;
import de.f4ked.datacenterextended.block.ServerWorkbenchBlock;
import de.f4ked.datacenterextended.block.HardwareShopBlock;
import de.f4ked.datacenterextended.network.CableTier;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Datacenterextended.MODID);

    public static final DeferredBlock<ServerWorkbenchBlock> SERVER_WORKBENCH = BLOCKS.registerBlock("server_workbench", ServerWorkbenchBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(2.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final DeferredBlock<HardwareShopBlock> HARDWARE_SHOP = BLOCKS.registerBlock("hardware_shop", HardwareShopBlock::new,
            p -> p.mapColor(MapColor.COLOR_GRAY).strength(3.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().lightLevel(state -> 5));

    public static final Map<RackType, DeferredBlock<RackBlock>> RACKS = new EnumMap<>(RackType.class);
    public static final Map<CableTier, DeferredBlock<DataCableBlock>> CABLES = new EnumMap<>(CableTier.class);

    static {
        for (RackType type : RackType.values()) {
            RACKS.put(type, BLOCKS.registerBlock(type.id(), p -> new RackBlock(type, p),
                    p -> p.mapColor(MapColor.COLOR_BLACK).strength(3.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()
                            .lightLevel(state -> state.getValue(RackBlock.RUNNING) ? 6 : 0)));
        }
        for (CableTier tier : CableTier.values()) {
            CABLES.put(tier, BLOCKS.registerBlock(tier.id(), p -> new DataCableBlock(tier, p),
                    p -> p.mapColor(MapColor.COLOR_GRAY).strength(0.5F).sound(SoundType.WOOL).noOcclusion()));
        }
    }

    private ModBlocks() {
    }
}
