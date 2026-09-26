package com.tony.deeprealmtheforth.platform.items;

import com.tony.deeprealmtheforth.DeepRealmTheForth;
import com.tony.deeprealmtheforth.astral.AstralMenu;
import com.tony.deeprealmtheforth.astral.AstralScreen;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

//? if forge {
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
//?} else {
/*import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?}

public final class AstralMenuRegistration {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, DeepRealmTheForth.MOD_ID);
    public static final Supplier<MenuType<AstralMenu>> MENU = MENUS.register("astral_container", () -> {
        //? if forge {
        return IForgeMenuType.create(AstralMenu::fromNetwork);
        //?} else {
        /*return IMenuTypeExtension.create(AstralMenu::fromNetwork);
        *///?}
    });

    private AstralMenuRegistration() {}

    public static void register(IEventBus bus) {
        MENUS.register(bus);
        //? if forge {
        bus.addListener(AstralMenuRegistration::clientSetup);
        //?} else {
        /*bus.addListener(AstralMenuRegistration::registerScreens);
        *///?}
    }

    //? if forge {
    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(MENU.get(), AstralScreen::new));
    }
    //?} else {
    /*private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MENU.get(), AstralScreen::new);
    }
    *///?}
}
