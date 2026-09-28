package com.tonywww.deeprealm4th.platform.registry;

import com.tonywww.deeprealm4th.astral.content.AstralItemCatalog;

import com.tonywww.deeprealm4th.client.render.ProjectionFrameRenderer;

import com.tonywww.deeprealm4th.DeepRealmTheForth;
import com.tonywww.deeprealm4th.menu.AstralMenu;
import com.tonywww.deeprealm4th.menu.ForgingPadMenu;
import com.tonywww.deeprealm4th.screen.AstralScreen;
import com.tonywww.deeprealm4th.screen.ForgingPadScreen;
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
    public static final Supplier<MenuType<ForgingPadMenu>> FORGING_PAD = MENUS.register("forging_pad", () -> {
        //? if forge {
        return IForgeMenuType.create(ForgingPadMenu::fromNetwork);
        //?} else {
        /*return IMenuTypeExtension.create(ForgingPadMenu::fromNetwork);
        *///?}
    });

    private AstralMenuRegistration() {}

    public static void register(IEventBus bus) {
        MENUS.register(bus);
        //? if forge {
        bus.addListener(AstralMenuRegistration::clientSetup);
        //?} else {
        /*bus.addListener(AstralMenuRegistration::registerScreens);
        bus.addListener(AstralMenuRegistration::registerFrameRenderer);
        *///?}
    }

    //? if forge {
    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.client.gui.screens.MenuScreens.register(MENU.get(), AstralScreen::new);
            net.minecraft.client.gui.screens.MenuScreens.register(FORGING_PAD.get(), ForgingPadScreen::new);
        });
    }
    //?} else {
    /*private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MENU.get(), AstralScreen::new);
        event.register(FORGING_PAD.get(), ForgingPadScreen::new);
    }
    private static void registerFrameRenderer(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.tonywww.deeprealm4th.client.render.ProjectionFrameRenderer.INSTANCE;
            }
        }, AstralItemCatalog.PROJECTION_FRAME.get());
    }
    *///?}
}
