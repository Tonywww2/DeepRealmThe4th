package com.tonywww.deeprealm4th.platform.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tonywww.deeprealm4th.platform.registry.AstralProcessRegistration;
import com.tonywww.deeprealm4th.astral.process.ProjectionRules;
import com.tonywww.deeprealm4th.astral.process.AstralDataPredicates;
import com.tonywww.deeprealm4th.astral.process.AstralTransforms;
import com.tonywww.deeprealm4th.astral.process.ProcessItemIds;
import com.tonywww.deeprealm4th.platform.data.ProjectionFrameData;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
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

    public record Step(ItemStack stack, String tag, Direction direction,
            String name, String predicate, boolean exactData) {
        //? if !forge {
        /*public static final Codec<Step> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.STRICT_CODEC.optionalFieldOf("stack", ItemStack.EMPTY).forGetter(Step::stack),
                Codec.STRING.optionalFieldOf("tag", "").forGetter(Step::tag),
                Codec.STRING.fieldOf("direction").forGetter(step -> step.direction().name().toLowerCase()),
                Codec.STRING.optionalFieldOf("name", "").forGetter(Step::name),
                Codec.STRING.optionalFieldOf("data_predicate", "").forGetter(Step::predicate),
                Codec.BOOL.optionalFieldOf("exact_data", true).forGetter(Step::exactData)
        ).apply(instance, (stack, tag, direction, name, predicate, exactData) -> new Step(stack, tag,
                Direction.valueOf(direction.toUpperCase()), name, predicate, exactData)));
        *///?}
        public Step(ItemStack stack, Direction direction) { this(stack, "", direction, "", "", true); }
        public Step {
            if (stack == null || tag == null || name == null || predicate == null
                    || stack.isEmpty() == tag.isEmpty() || !stack.isEmpty() && stack.getCount() != 1)
                throw new IllegalArgumentException("Projection step requires one stack or tag");
            stack = stack.copy();
            if (!tag.isEmpty()) ProcessItemIds.requireValid(tag);
            if (!predicate.isEmpty()) ProcessItemIds.requireValid(predicate);
            if (!exactData && predicate.isEmpty())
                throw new IllegalArgumentException("A non-exact projection step requires a data_predicate");
            if (direction == null) throw new IllegalArgumentException("Missing projection direction");
        }
        @Override public ItemStack stack() { return stack.copy(); }
        public boolean accepts(ItemStack candidate) {
            if (candidate.isEmpty()) return false;
            boolean itemMatches = tag.isEmpty() ? candidate.is(stack.getItem())
                    : candidate.is(TagKey.create(Registries.ITEM, ResourceLocation.tryParse(tag)));
            return itemMatches && (!exactData || !tag.isEmpty()
                    || ItemStack.matches(stack, candidate.copyWithCount(1)))
                    && (predicate.isEmpty() || AstralDataPredicates.test(predicate, candidate));
        }
        public ItemStack displayStack() {
            if (tag.isEmpty()) return stack();
            var entries = net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(TagKey.create(
                    Registries.ITEM, ResourceLocation.tryParse(tag)));
            return entries.flatMap(items -> items.stream().findFirst())
                    .map(holder -> new ItemStack(holder.value())).orElse(ItemStack.EMPTY);
        }
    }

    private final ItemStack result;
    private final List<Step> steps;
    private final String dataTransform;
    //? if forge {
    private final ResourceLocation id;
    //?} else {
    /*public static final MapCodec<ProjectionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ProjectionRecipe::output),
            Step.CODEC.listOf().fieldOf("steps").forGetter(ProjectionRecipe::steps),
            Codec.STRING.optionalFieldOf("data_transform", "").forGetter(ProjectionRecipe::dataTransform)
    ).apply(instance, ProjectionRecipe::new));
    *///?}

    //? if forge {
    public ProjectionRecipe(ResourceLocation id, ItemStack result, List<Step> steps) {
        this(id, result, steps, "");
    }
    public ProjectionRecipe(ResourceLocation id, ItemStack result, List<Step> steps, String dataTransform) {
        this.id = id;
        this.result = result.copy();
        this.steps = List.copyOf(steps);
        this.dataTransform = dataTransform == null ? "" : dataTransform;
        validate();
    }
    //?} else {
    /*public ProjectionRecipe(ItemStack result, List<Step> steps) {
        this(result, steps, "");
    }
    public ProjectionRecipe(ItemStack result, List<Step> steps, String dataTransform) {
        this.result = result.copy();
        this.steps = List.copyOf(steps);
        this.dataTransform = dataTransform == null ? "" : dataTransform;
        validate();
    }
    *///?}

    private void validate() {
        ProjectionRules.validate(result, steps);
        if (!dataTransform.isEmpty()) ProcessItemIds.requireValid(dataTransform);
    }

    public List<Step> steps() { return steps; }
    public String dataTransform() { return dataTransform; }
    public ItemStack output() {
        return result.copy();
    }

    public boolean matchesPrefix(List<Step> input) {
        if (input.size() > steps.size()) return false;
        for (int i = 0; i < input.size(); i++) {
            Step expected = steps.get(i), actual = input.get(i);
            if (expected.direction() != actual.direction()
                    || !expected.accepts(actual.stack())) return false;
        }
        return true;
    }

    public AstralTransforms.Result<AstralTransforms.ProcessOutput> produce(Player player, String recipeId,
            List<ProjectionFrameData.Absorbed> absorbed) {
        AstralTransforms.Result<AstralTransforms.ProcessContext> prepared = processContext(player,
                recipeId, "produce", absorbed);
        if (!prepared.ok()) return AstralTransforms.Result.failure(prepared.error());
        if (dataTransform.isEmpty())
            return AstralTransforms.Result.success(AstralTransforms.ProcessOutput.of(output()));
        AstralTransforms.Result<AstralTransforms.ProcessOutput> produced =
                AstralTransforms.produce(dataTransform, prepared.value());
        if (!produced.ok()) return produced;
        for (String name : produced.value().consumed().keySet())
            if (prepared.value().source(name).isEmpty() || produced.value().consumed().get(name) != 1)
                return AstralTransforms.Result.failure("invalid_projection_consumption");
        return produced;
    }

    public AstralTransforms.Result<ItemStack> preview(Player player, String recipeId,
            List<ProjectionFrameData.Absorbed> absorbed) {
        AstralTransforms.Result<AstralTransforms.ProcessContext> prepared = processContext(player,
                recipeId, "preview", absorbed);
        if (!prepared.ok()) return AstralTransforms.Result.failure(prepared.error());
        return dataTransform.isEmpty() ? AstralTransforms.Result.success(output())
                : AstralTransforms.preview(dataTransform, prepared.value());
    }

    private AstralTransforms.Result<AstralTransforms.ProcessContext> processContext(Player player,
            String recipeId, String stage, List<ProjectionFrameData.Absorbed> absorbed) {
        if (absorbed.size() != steps.size()
                || !matchesPrefix(ProjectionFrameData.steps(absorbed)))
            return AstralTransforms.Result.failure("projection_steps_changed");
        Map<String, ItemStack> sources = new LinkedHashMap<>();
        for (int i = 0; i < absorbed.size(); i++) {
            String name = steps.get(i).name().isEmpty() ? "step_" + i : steps.get(i).name();
            if (sources.putIfAbsent(name, absorbed.get(i).stack().copy()) != null)
                return AstralTransforms.Result.failure("duplicate_projection_step_name");
        }
        return AstralTransforms.Result.success(new AstralTransforms.ProcessContext(player,
                player.level(), recipeId, stage, sources, List.of(), output()));
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
                steps.add(new Step(step.has("stack")
                                ? CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(step, "stack"), true, true)
                                : ItemStack.EMPTY,
                        GsonHelper.getAsString(step, "tag", ""),
                        Direction.valueOf(GsonHelper.getAsString(step, "direction").toUpperCase()),
                        GsonHelper.getAsString(step, "name", ""), GsonHelper.getAsString(step, "data_predicate", ""),
                        GsonHelper.getAsBoolean(step, "exact_data", true)));
            }
            return new ProjectionRecipe(id, CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "result"), true, true),
                    steps, GsonHelper.getAsString(json, "data_transform", ""));
        }
        @Override public ProjectionRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            ItemStack result = buffer.readItem();
            int count = buffer.readVarInt();
            if (count < 2 || count > 9) throw new IllegalArgumentException("Invalid projection step count");
            List<Step> steps = new java.util.ArrayList<>();
            for (int i = 0; i < count; i++) {
                steps.add(new Step(buffer.readItem(), buffer.readUtf(), Direction.values()[buffer.readVarInt()],
                        buffer.readUtf(), buffer.readUtf(), buffer.readBoolean()));
            }
            return new ProjectionRecipe(id, result, steps, buffer.readUtf());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, ProjectionRecipe recipe) {
            buffer.writeItem(recipe.result);
            buffer.writeVarInt(recipe.steps.size());
            for (Step step : recipe.steps) {
                buffer.writeItem(step.stack());
                buffer.writeUtf(step.tag());
                buffer.writeVarInt(step.direction().ordinal());
                buffer.writeUtf(step.name());
                buffer.writeUtf(step.predicate());
                buffer.writeBoolean(step.exactData());
            }
            buffer.writeUtf(recipe.dataTransform);
        }
        //?} else {
        /*@Override public MapCodec<ProjectionRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ProjectionRecipe> streamCodec() {
            return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
        }
        *///?}
    }
}
