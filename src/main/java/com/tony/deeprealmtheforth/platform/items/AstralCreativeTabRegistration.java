package com.tony.deeprealmtheforth.platform.items;

import com.tony.deeprealmtheforth.DeepRealmTheForth;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

//? if forge {
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
//?} else {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?}

public final class AstralCreativeTabRegistration {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DeepRealmTheForth.MOD_ID);

    public static final Supplier<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("creative_mode_tab." + DeepRealmTheForth.MOD_ID + ".main"))
            .icon(() -> new ItemStack(AstralItemRegistration.BASE_CONTAINER.get()))
            .displayItems((parameters, output) -> {
                output.accept(AstralItemRegistration.BASE_CONTAINER.get());
                for (Item item : BuiltInRegistries.ITEM) {
                    if (item != AstralItemRegistration.BASE_CONTAINER.get()
                            && DeepRealmTheForth.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())) {
                        output.accept(item);
                    }
                }
            })
            .build());

    private AstralCreativeTabRegistration() {}

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
