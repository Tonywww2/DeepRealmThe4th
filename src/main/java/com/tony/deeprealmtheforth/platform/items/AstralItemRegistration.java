package com.tony.deeprealmtheforth.platform.items;

import com.tony.deeprealmtheforth.DeepRealmTheForth;
import com.tony.deeprealmtheforth.astral.AstralContainerItem;
import com.tony.deeprealmtheforth.astral.AstralFillerItem;
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

public final class AstralItemRegistration {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, DeepRealmTheForth.MOD_ID);

    public static final Supplier<AstralContainerItem> BASE_CONTAINER =
            ITEMS.register("base_container", () -> new AstralContainerItem(new Item.Properties()));
    public static final Supplier<Item> WARRIOR_MEDAL =
            ITEMS.register("warrior_medal", () -> new AstralFillerItem(new Item.Properties().stacksTo(1),
                    "tooltip.deeprealm_4th.warrior_medal.effect",
                    "tooltip.deeprealm_4th.warrior_medal.example"));
    public static final Supplier<Item> STRENGTH_GEM =
            ITEMS.register("strength_gem", () -> new AstralFillerItem(new Item.Properties().stacksTo(1),
                    "tooltip.deeprealm_4th.strength_gem.effect"));

    private AstralItemRegistration() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
