package com.tonywww.deeprealm4th.platform.registry;

import com.tonywww.deeprealm4th.DeepRealmTheForth;
import com.tonywww.deeprealm4th.astral.content.AstralItemCatalog;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

//? if forge {
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
//?} else {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?}

/** Loader registry adapter; item definitions live in the astral catalog. */
public final class AstralItemRegistration {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, DeepRealmTheForth.MOD_ID);

    private AstralItemRegistration() {}

    public static <T extends Item> Supplier<T> registerItem(String id, Supplier<? extends T> factory) {
        return ITEMS.register(id, factory);
    }

    public static void register(IEventBus bus) {
        AstralItemCatalog.initialize();
        ITEMS.register(bus);
    }
}
