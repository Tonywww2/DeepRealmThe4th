package com.tonywww.deeprealm4th.platform.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tonywww.deeprealm4th.platform.registry.AstralProcessRegistration;
import com.tonywww.deeprealm4th.astral.process.ProjectionRules;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

//? if forge {
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
//?} else {
/*import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.SingleRecipeInput;
*///?}

/** Direction-sensitive item-on-frame sequence. A completed recipe consumes its frame. */
public final class ProjectionRecipe implements Recipe<
        //? if forge {
        Container
        //?} else {
        /*SingleRecipeInput
        *///?}
        > {
    public enum Direction { MATERIAL_ON_FRAME, FRAME_ON_MATERIAL }

    public record Step(ItemStack stack, Direction direction) {
        //? if !forge {
        /*public static final Codec<Step> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.STRICT_CODEC.fieldOf("stack").forGetter(Step::stack),
                Codec.STRING.fieldOf("direction").forGetter(step -> step.direction().name().toLowerCase())
        ).apply(instance, (stack, direction) -> new Step(stack,
                Direction.valueOf(direction.toUpperCase()))));
        *///?}
        public Step {
            if (stack == null || stack.isEmpty() || stack.getCount() != 1)
                throw new IllegalArgumentException("Projection step requires one material stack");
            stack = stack.copy();
            if (direction == null) throw new IllegalArgumentException("Missing projection direction");
        }
        @Override public ItemStack stack() { return stack.copy(); }
    }

    private final ItemStack result;
    private final List<Step> steps;
    //? if forge {
    private final ResourceLocation id;
    //?} else {
    /*public static final MapCodec<ProjectionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ProjectionRecipe::output),
            Step.CODEC.listOf().fieldOf("steps").forGetter(ProjectionRecipe::steps)
    ).apply(instance, ProjectionRecipe::new));
    *///?}

    //? if forge {
    public ProjectionRecipe(ResourceLocation id, ItemStack result, List<Step> steps) {
        this.id = id;
        this.result = result.copy();
        this.steps = List.copyOf(steps);
        validate();
    }
    //?} else {
    /*public ProjectionRecipe(ItemStack result, List<Step> steps) {
        this.result = result.copy();
        this.steps = List.copyOf(steps);
        validate();
    }
    *///?}

    private void validate() {
        ProjectionRules.validate(result, steps);
    }

    public List<Step> steps() { return steps; }
    public ItemStack output() {
        return result.copy();
    }

    public boolean matchesPrefix(List<Step> input) {
        if (input.size() > steps.size()) return false;
        for (int i = 0; i < input.size(); i++) {
            Step expected = steps.get(i), actual = input.get(i);
            if (expected.direction() != actual.direction()
                    || !ItemStack.matches(expected.stack(), actual.stack())) return false;
        }
        return true;
    }

    @Override public boolean matches(
            //? if forge {
            Container input,
            //?} else {
            /*SingleRecipeInput input,
            *///?}
            Level level) { return false; }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<?> getSerializer() { return AstralProcessRegistration.PROJECTION_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return AstralProcessRegistration.PROJECTION_TYPE.get(); }
    //? if forge {
    @Override public ResourceLocation getId() { return id; }
    @Override public ItemStack assemble(Container input, RegistryAccess registries) { return output(); }
    @Override public ItemStack getResultItem(RegistryAccess registries) { return output(); }
    //?} else {
    /*@Override public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) { return output(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return output(); }
    *///?}

    public static final class Serializer implements RecipeSerializer<ProjectionRecipe> {
        //? if forge {
        @Override public ProjectionRecipe fromJson(ResourceLocation id, JsonObject json) {
            JsonArray entries = GsonHelper.getAsJsonArray(json, "steps");
            List<Step> steps = new java.util.ArrayList<>();
            for (var element : entries) {
                JsonObject step = element.getAsJsonObject();
                steps.add(new Step(CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(step, "stack"), true, true),
                        Direction.valueOf(GsonHelper.getAsString(step, "direction").toUpperCase())));
            }
            return new ProjectionRecipe(id, CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "result"), true, true), steps);
        }
        @Override public ProjectionRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            ItemStack result = buffer.readItem();
            int count = buffer.readVarInt();
            if (count < 2 || count > 9) throw new IllegalArgumentException("Invalid projection step count");
            List<Step> steps = new java.util.ArrayList<>();
            for (int i = 0; i < count; i++) {
                steps.add(new Step(buffer.readItem(), Direction.values()[buffer.readVarInt()]));
            }
            return new ProjectionRecipe(id, result, steps);
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, ProjectionRecipe recipe) {
            buffer.writeItem(recipe.result);
            buffer.writeVarInt(recipe.steps.size());
            for (Step step : recipe.steps) {
                buffer.writeItem(step.stack());
                buffer.writeVarInt(step.direction().ordinal());
            }
        }
        //?} else {
        /*@Override public MapCodec<ProjectionRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ProjectionRecipe> streamCodec() {
            return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
        }
        *///?}
    }
}
