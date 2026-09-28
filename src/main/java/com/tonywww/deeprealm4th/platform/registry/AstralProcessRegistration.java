package com.tonywww.deeprealm4th.platform.registry;

import com.tonywww.deeprealm4th.DeepRealmTheForth;
import com.tonywww.deeprealm4th.platform.recipe.CombinationForgingRecipe;
import com.tonywww.deeprealm4th.platform.recipe.FrameDisassemblyRecipe;
import com.tonywww.deeprealm4th.platform.recipe.ProjectionRecipe;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

//? if forge {
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
//?} else {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?}

/** Two real datapack recipe types, shared by Java and KubeJS custom recipe JSONs. */
public final class AstralProcessRegistration {
    private static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, DeepRealmTheForth.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, DeepRealmTheForth.MOD_ID);

    public static final Supplier<RecipeType<CombinationForgingRecipe>> FORGING_TYPE =
            TYPES.register("combination_forging", () -> new RecipeType<>() {
                @Override public String toString() { return "deeprealm_4th:combination_forging"; }
            });
    public static final Supplier<RecipeSerializer<CombinationForgingRecipe>> FORGING_SERIALIZER =
            SERIALIZERS.register("combination_forging", CombinationForgingRecipe.Serializer::new);
    public static final Supplier<RecipeType<ProjectionRecipe>> PROJECTION_TYPE =
            TYPES.register("projection_combining", () -> new RecipeType<>() {
                @Override public String toString() { return "deeprealm_4th:projection_combining"; }
            });
    public static final Supplier<RecipeSerializer<ProjectionRecipe>> PROJECTION_SERIALIZER =
            SERIALIZERS.register("projection_combining", ProjectionRecipe.Serializer::new);
    public static final Supplier<RecipeSerializer<FrameDisassemblyRecipe>> DISASSEMBLY_SERIALIZER =
            SERIALIZERS.register("frame_disassembly", () ->
                    new SimpleCraftingRecipeSerializer<>(FrameDisassemblyRecipe::new));

    private AstralProcessRegistration() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
    }
}
