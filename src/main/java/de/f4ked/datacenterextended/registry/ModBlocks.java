package de.f4ked.datacenterextended.registry;

import de.f4ked.datacenterextended.Datacenterextended;
import de.f4ked.datacenterextended.block.DataCableBlock;
import de.f4ked.datacenterextended.block.RackBlock;
import de.f4ked.datacenterextended.block.RackType;
import de.f4ked.datacenterextended.block.ServerMonitorBlock;
import de.f4ked.datacenterextended.block.ServerWorkbenchBlock;
import de.f4ked.datacenterextended.block.HardwareShopBlock;
import de.f4ked.datacenterextended.network.CableTier;
import de.f4ked.datacenterextended.power.BatteryBlock;
import de.f4ked.datacenterextended.power.BatteryTier;
import de.f4ked.datacenterextended.power.GeneratorBlock;
import de.f4ked.datacenterextended.power.GeneratorFamily;
import de.f4ked.datacenterextended.power.GeneratorType;
import de.f4ked.datacenterextended.power.GridConnectionBlock;
import de.f4ked.datacenterextended.power.GridTier;
import de.f4ked.datacenterextended.power.PowerCableBlock;
import de.f4ked.datacenterextended.power.PowerCableTier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Datacenterextended.MODID);

    public static final DeferredBlock<ServerWorkbenchBlock> SERVER_WORKBENCH = BLOCKS.registerBlock("server_workbench", ServerWorkbenchBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(2.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final DeferredBlock<HardwareShopBlock> HARDWARE_SHOP = BLOCKS.registerBlock("hardware_shop", HardwareShopBlock::new,
            p -> p.mapColor(MapColor.COLOR_GRAY).strength(3.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().lightLevel(state -> 5));

    public static final DeferredBlock<ServerMonitorBlock> SERVER_MONITOR = BLOCKS.registerBlock("server_monitor", ServerMonitorBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(2.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().lightLevel(state -> 4));

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

    // ------------------------------------------------------------------ power
    public static final Map<GeneratorType, DeferredBlock<GeneratorBlock>> GENERATORS = new EnumMap<>(GeneratorType.class);
    public static final Map<BatteryTier, DeferredBlock<BatteryBlock>> BATTERIES = new EnumMap<>(BatteryTier.class);
    public static final Map<PowerCableTier, DeferredBlock<PowerCableBlock>> POWER_CABLES = new EnumMap<>(PowerCableTier.class);
    public static final Map<GridTier, DeferredBlock<GridConnectionBlock>> GRID_CONNECTIONS = new EnumMap<>(GridTier.class);

    static {
        for (GeneratorType type : GeneratorType.values()) {
            GENERATORS.put(type, BLOCKS.registerBlock(type.id(), p -> new GeneratorBlock(type, p), generatorProperties(type)));
        }
        for (BatteryTier tier : BatteryTier.values()) {
            BATTERIES.put(tier, BLOCKS.registerBlock(tier.id(), p -> new BatteryBlock(tier, p),
                    p -> p.mapColor(MapColor.COLOR_GRAY).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
        }
        for (PowerCableTier tier : PowerCableTier.values()) {
            POWER_CABLES.put(tier, BLOCKS.registerBlock(tier.id(), p -> new PowerCableBlock(tier, p),
                    p -> p.mapColor(MapColor.COLOR_BLACK).strength(0.5F).sound(SoundType.WOOL).noOcclusion()));
        }
        for (GridTier tier : GridTier.values()) {
            GRID_CONNECTIONS.put(tier, BLOCKS.registerBlock(tier.id(), p -> new GridConnectionBlock(tier, p),
                    p -> p.mapColor(MapColor.COLOR_GRAY).strength(3.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
        }
    }

    private static UnaryOperator<BlockBehaviour.Properties> generatorProperties(GeneratorType type) {
        return switch (type.family()) {
            case SOLAR -> p -> p.mapColor(MapColor.COLOR_BLUE).strength(1.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion();
            case WIND -> p -> p.mapColor(MapColor.SNOW).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion();
            case COMBUSTION, NUCLEAR -> p -> p.mapColor(MapColor.METAL).strength(3.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(GeneratorBlock.ACTIVE) ? (type.family() == GeneratorFamily.NUCLEAR ? 10 : 8) : 0);
            default -> p -> p.mapColor(MapColor.METAL).strength(3.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
        };
    }

    // ------------------------------------------------------------------ ores and storage blocks
    /** Ores, raw ore blocks and metal blocks by id, in creative tab order. */
    public static final Map<String, DeferredBlock<Block>> RESOURCE_BLOCKS = new LinkedHashMap<>();

    static {
        ore("bauxite_ore", false, 3.0F);
        ore("deepslate_bauxite_ore", true, 4.5F);
        ore("spodumene_ore", false, 3.0F);
        ore("deepslate_spodumene_ore", true, 4.5F);
        ore("uraninite_ore", false, 3.0F);
        ore("deepslate_uraninite_ore", true, 4.5F);
        resource("raw_bauxite_block", MapColor.COLOR_ORANGE, 5.0F);
        resource("raw_spodumene_block", MapColor.COLOR_PINK, 5.0F);
        resource("raw_uraninite_block", MapColor.COLOR_BLACK, 5.0F);
        resource("aluminium_block", MapColor.METAL, 5.0F);
        resource("steel_block", MapColor.COLOR_GRAY, 6.0F);
    }

    private static void ore(String id, boolean deepslate, float hardness) {
        RESOURCE_BLOCKS.put(id, BLOCKS.registerSimpleBlock(id, p -> p.mapColor(deepslate ? MapColor.DEEPSLATE : MapColor.STONE)
                .strength(hardness, 3.0F).sound(deepslate ? SoundType.DEEPSLATE : SoundType.STONE).requiresCorrectToolForDrops()));
    }

    private static void resource(String id, MapColor color, float hardness) {
        RESOURCE_BLOCKS.put(id, BLOCKS.registerSimpleBlock(id, p -> p.mapColor(color).strength(hardness, 6.0F)
                .sound(SoundType.METAL).requiresCorrectToolForDrops()));
    }

    private ModBlocks() {
    }
}
