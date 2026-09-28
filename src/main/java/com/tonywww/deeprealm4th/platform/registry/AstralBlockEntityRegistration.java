package com.tonywww.deeprealm4th.platform.registry;

import com.tonywww.deeprealm4th.DeepRealmTheForth;
import com.tonywww.deeprealm4th.blockentity.ForgingPadBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

//? if forge {
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
//?} else {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?}

public final class AstralBlockEntityRegistration {
    private static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DeepRealmTheForth.MOD_ID);
    public static final Supplier<BlockEntityType<ForgingPadBlockEntity>> FORGING_PAD =
            TYPES.register("forging_pad", () -> BlockEntityType.Builder.of(
                    ForgingPadBlockEntity::new, AstralBlockRegistration.FORGING_PAD.get()).build(null));

    private AstralBlockEntityRegistration() {}
    public static void register(IEventBus bus) { TYPES.register(bus); }
}
