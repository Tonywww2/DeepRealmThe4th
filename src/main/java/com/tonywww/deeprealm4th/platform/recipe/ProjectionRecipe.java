package com.tonywww.deeprealm4th.platform.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tonywww.deeprealm4th.platform.registry.AstralProcessRegistration;
import com.tonywww.deeprealm4th.astral.process.ProcessItemIds;
import com.tonywww.deeprealm4th.astral.process.ProjectionRules;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

//? if forge {
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

    public record Step(String itemId, Direction direction) {
        //? if !forge {
        /*public static final Codec<Step> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("item").forGetter(Step::itemId),
                Codec.STRING.fieldOf("direction").forGetter(step -> step.direction().name().toLowerCase())
        ).apply(instance, (item, direction) -> new Step(item,
                Direction.valueOf(direction.toUpperCase()))));
        *///?}
        public Step {
            ProcessItemIds.requireValid(itemId);
            if (direction == null) throw new IllegalArgumentException("Missing projection direction");
        }
    }

    private final String resultId;
    private final List<Step> steps;
    //? if forge {
    private final ResourceLocation id;
    //?} else {
    /*public static final MapCodec<ProjectionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("result").forGetter(ProjectionRecipe::resultId),
            Step.CODEC.listOf().fieldOf("steps").forGetter(ProjectionRecipe::steps)
    ).apply(instance, ProjectionRecipe::new));
    *///?}

    //? if forge {
    public ProjectionRecipe(ResourceLocation id, String resultId, List<Step> steps) {
        this.id = id;
        this.resultId = resultId;
        this.steps = List.copyOf(steps);
        validate();
    }
    //?} else {
    /*public ProjectionRecipe(String resultId, List<Step> steps) {
        this.resultId = resultId;
        this.steps = List.copyOf(steps);
        validate();
    }
    *///?}

    private void validate() {
        ProjectionRules.validate(resultId, steps);
    }

    public String resultId() { return resultId; }
    public List<Step> steps() { return steps; }
    public ItemStack output() {
        return ProcessItemIds.output(resultId);
    }

    public boolean matchesPrefix(List<Step> input) {
        return ProjectionRules.matchesPrefix(steps, input);
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
                steps.add(new Step(GsonHelper.getAsString(step, "item"),
                        Direction.valueOf(GsonHelper.getAsString(step, "direction").toUpperCase())));
            }
            return new ProjectionRecipe(id, GsonHelper.getAsString(json, "result"), steps);
        }
        @Override public ProjectionRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            String result = buffer.readUtf();
            int count = buffer.readVarInt();
            if (count < 2 || count > 9) throw new IllegalArgumentException("Invalid projection step count");
            List<Step> steps = new java.util.ArrayList<>();
            for (int i = 0; i < count; i++) {
                steps.add(new Step(buffer.readUtf(), Direction.values()[buffer.readVarInt()]));
            }
            return new ProjectionRecipe(id, result, steps);
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, ProjectionRecipe recipe) {
            buffer.writeUtf(recipe.resultId);
            buffer.writeVarInt(recipe.steps.size());
            for (Step step : recipe.steps) {
                buffer.writeUtf(step.itemId());
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
