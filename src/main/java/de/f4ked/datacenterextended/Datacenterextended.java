package de.f4ked.datacenterextended;

import de.f4ked.datacenterextended.network.ModPayloads;
import de.f4ked.datacenterextended.recipe.PartRecipesCondition;
import com.mojang.logging.LogUtils;
import de.f4ked.datacenterextended.gametest.ModGameTests;
import de.f4ked.datacenterextended.registry.ModBlockEntities;
import de.f4ked.datacenterextended.registry.ModBlocks;
import de.f4ked.datacenterextended.network.ComputeNode;
import de.f4ked.datacenterextended.registry.ModCreativeTabs;
import de.f4ked.datacenterextended.registry.ModDataComponents;
import de.f4ked.datacenterextended.registry.ModDataMaps;
import de.f4ked.datacenterextended.registry.ModItems;
import de.f4ked.datacenterextended.registry.ModMenus;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import org.slf4j.Logger;

/**
 * DataCenterExtended: assemble servers from motherboards, CPUs, memory, GPUs, PSUs and storage, mount them in racks and
 * use their compute to mine crypto, host services for dollars or fabricate items by their "file size". Hosting and
 * fabricating racks share compute over data cables; money lives in each player's wallet.
 */
@Mod(Datacenterextended.MODID)
public class Datacenterextended {
    public static final String MODID = "datacenterextended";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Datacenterextended(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModGameTests.TEST_FUNCTIONS.register(modEventBus);
        PartRecipesCondition.CONDITION_CODECS.register(modEventBus);

        modEventBus.addListener(ModDataMaps::register);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(ModPayloads::register);

        // SYNCED: config/datacenterextended-synced.toml, sent from the server to connecting clients.
        modContainer.registerConfig(ModConfig.Type.SYNCED, Config.SPEC);
        // STARTUP: config/datacenterextended-startup.toml, read before datapacks (recipe conditions).
        modContainer.registerConfig(ModConfig.Type.STARTUP, StartupConfig.SPEC);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Pipes / conveyors of other mods can pull fabricated items out of any rack segment.
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.RACK.get(),
                (be, side) -> new WorldlyContainerWrapper(be.controllerOrSelf(), side));
        // Accepts Forge Energy from any mod (only consumed when requireEnergy = true).
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.RACK.get(), (be, side) -> be.getEnergyHandler());
        // Hosting / fabricating racks join compute networks; other mods can expose this capability too.
        event.registerBlockEntity(ComputeNode.CAPABILITY, ModBlockEntities.RACK.get(), (be, side) -> be.computeNode());
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
