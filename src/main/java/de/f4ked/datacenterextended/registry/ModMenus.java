package de.f4ked.datacenterextended.registry;

import de.f4ked.datacenterextended.Datacenterextended;
import de.f4ked.datacenterextended.menu.RackMenu;
import de.f4ked.datacenterextended.menu.ServerWorkbenchMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Datacenterextended.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ServerWorkbenchMenu>> SERVER_WORKBENCH =
            MENUS.register("server_workbench", () -> IMenuTypeExtension.create(ServerWorkbenchMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RackMenu>> RACK =
            MENUS.register("rack", () -> IMenuTypeExtension.create(RackMenu::new));

    private ModMenus() {
    }
}
