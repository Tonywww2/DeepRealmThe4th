package com.tonywww.deeprealm4th.platform.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tonywww.deeprealm4th.astral.process.ProcessItemIds;
import com.tonywww.deeprealm4th.platform.registry.AstralProcessRegistration;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

//? if forge {
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
//?} else {
/*import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeInput;
*///?}

/** Shapeless twelve-slot forging with per-slot stack counts and configurable hammering. */
public final class CombinationForgingRecipe implements Recipe<
        //? if forge {
        Container
        //?} else {
        /*RecipeInput
        *///?}
        > {
    public record Input(String item, String tag, int count) {
        //? if !forge {
        /*public static final Codec<Input> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("item", "").forGetter(Input::item),
                Codec.STRING.optionalFieldOf("tag", "").forGetter(Input::tag),
                Codec.INT.optionalFieldOf("count", 1).forGetter(Input::count)
        ).apply(instance, Input::new));
        *///?}

        public Input {
            if (item == null || tag == null || item.isEmpty() == tag.isEmpty()) {
                throw new IllegalArgumentException("Exactly one of item or tag is required");
            }
            ProcessItemIds.requireValid(item.isEmpty() ? tag : item);
            if (count < 1) throw new IllegalArgumentException("Input count must be positive");
        }

        public boolean accepts(ItemStack stack) {
            if (stack.isEmpty() || stack.getCount() < count) return false;
            ResourceLocation id = ResourceLocation.tryParse(item.isEmpty() ? tag : item);
            return item.isEmpty() ? stack.is(TagKey.create(Registries.ITEM, id))
                    : net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id);
        }

        public ItemStack displayStack() {
            if (!item.isEmpty()) {
                ItemStack stack = ProcessItemIds.output(item);
                if (!stack.isEmpty()) stack.setCount(count);
                return stack;
            }
            var entry = net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(TagKey.create(
                    Registries.ITEM, ResourceLocation.tryParse(tag)));
            return entry.flatMap(items -> items.stream().findFirst())
                    .map(holder -> new ItemStack(holder.value(), count)).orElse(ItemStack.EMPTY);
        }
    }

    private final String resultId;
    private final int resultCount;
    private final List<Input> inputs;
    private final int clicks;
    private final int cooldown;
    private final int levels;
    private final String nameKey;
    //? if forge {
    private final ResourceLocation id;
    //?} else {
    /*public static final MapCodec<CombinationForgingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("result").forGetter(CombinationForgingRecipe::resultId),
            Codec.INT.optionalFieldOf("result_count", 1).forGetter(CombinationForgingRecipe::resultCount),
            Input.CODEC.listOf().fieldOf("ingredients").forGetter(CombinationForgingRecipe::inputs),
            Codec.INT.optionalFieldOf("clicks", 3).forGetter(CombinationForgingRecipe::clicks),
            Codec.INT.optionalFieldOf("cooldown", 10).forGetter(CombinationForgingRecipe::cooldown),
            Codec.INT.optionalFieldOf("levels", 0).forGetter(CombinationForgingRecipe::levels),
            Codec.STRING.optionalFieldOf("name_key", "").forGetter(CombinationForgingRecipe::nameKey)
    ).apply(instance, CombinationForgingRecipe::new));
    *///?}

    //? if forge {
    public CombinationForgingRecipe(ResourceLocation id, String resultId, int resultCount, List<Input> inputs,
                                    int clicks, int cooldown, int levels, String nameKey) {
        this.id = id;
        this.resultId = ProcessItemIds.requireValid(resultId);
        this.resultCount = resultCount;
        this.inputs = List.copyOf(inputs);
        this.clicks = clicks;
        this.cooldown = cooldown;
        this.levels = levels;
        this.nameKey = nameKey == null ? "" : nameKey;
        validate();
    }
    //?} else {
    /*public CombinationForgingRecipe(String resultId, int resultCount, List<Input> inputs,
                                    int clicks, int cooldown, int levels, String nameKey) {
        this.resultId = ProcessItemIds.requireValid(resultId);
        this.resultCount = resultCount;
        this.inputs = List.copyOf(inputs);
        this.clicks = clicks;
        this.cooldown = cooldown;
        this.levels = levels;
        this.nameKey = nameKey == null ? "" : nameKey;
        validate();
    }
    *///?}

    private void validate() {
        if (inputs.isEmpty() || inputs.size() > 12) throw new IllegalArgumentException("Forging needs 1..12 inputs");
        if (resultCount < 1 || resultCount > 64) throw new IllegalArgumentException("Invalid result count");
        if (clicks < 1 || clicks > 64) throw new IllegalArgumentException("Clicks must be 1..64");
        if (cooldown < 0 || cooldown > 1200) throw new IllegalArgumentException("Invalid click cooldown");
        if (levels < 0 || levels > 10000) throw new IllegalArgumentException("Invalid level cost");
    }

    public String resultId() { return resultId; }
    public int resultCount() { return resultCount; }
    public List<Input> inputs() { return inputs; }
    public int clicks() { return clicks; }
    public int cooldown() { return cooldown; }
    public int levels() { return levels; }
    public String nameKey() { return nameKey; }
    public ItemStack output() {
        ItemStack stack = ProcessItemIds.output(resultId);
        if (!stack.isEmpty()) stack.setCount(resultCount);
        return stack;
    }

    /** Returns the slot index for each ingredient, or null when any stack or count differs. */
    public int[] assignment(Container inventory) {
        if (inventory.getContainerSize() < 12) return null;
        int nonempty = 0;
        for (int slot = 0; slot < 12; slot++) if (!inventory.getItem(slot).isEmpty()) nonempty++;
        if (nonempty != inputs.size()) return null;
        int[] result = new int[inputs.size()];
        return assign(inventory, 0, 0, result) ? result : null;
    }

    private boolean assign(Container inventory, int ingredient, int used, int[] result) {
        if (ingredient == inputs.size()) return true;
        for (int slot = 0; slot < 12; slot++) {
            if ((used & (1 << slot)) != 0 || !inputs.get(ingredient).accepts(inventory.getItem(slot))) continue;
            result[ingredient] = slot;
            if (assign(inventory, ingredient + 1, used | (1 << slot), result)) return true;
        }
        return false;
    }

    @Override public boolean matches(
            //? if forge {
            Container input,
            //?} else {
            /*RecipeInput input,
            *///?}
            Level level) {
        //? if forge {
        return assignment(input) != null;
        //?} else {
        /*if (input.size() < 12) return false;
        SimpleContainer slots = new SimpleContainer(12);
        for (int i = 0; i < 12; i++) slots.setItem(i, input.getItem(i));
        return assignment(slots) != null;
        *///?}
    }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 12; }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<?> getSerializer() { return AstralProcessRegistration.FORGING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return AstralProcessRegistration.FORGING_TYPE.get(); }
    //? if forge {
    @Override public ResourceLocation getId() { return id; }
    @Override public ItemStack assemble(Container input, RegistryAccess registries) { return output(); }
    @Override public ItemStack getResultItem(RegistryAccess registries) { return output(); }
    //?} else {
    /*@Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) { return output(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return output(); }
    *///?}

    public static final class Serializer implements RecipeSerializer<CombinationForgingRecipe> {
        //? if forge {
        @Override public CombinationForgingRecipe fromJson(ResourceLocation id, JsonObject json) {
            JsonArray array = GsonHelper.getAsJsonArray(json, "ingredients");
            List<Input> inputs = new ArrayList<>();
            for (var value : array) {
                JsonObject entry = value.getAsJsonObject();
                inputs.add(new Input(GsonHelper.getAsString(entry, "item", ""),
                        GsonHelper.getAsString(entry, "tag", ""), GsonHelper.getAsInt(entry, "count", 1)));
            }
            return new CombinationForgingRecipe(id, GsonHelper.getAsString(json, "result"),
                    GsonHelper.getAsInt(json, "result_count", 1), inputs,
                    GsonHelper.getAsInt(json, "clicks", 3), GsonHelper.getAsInt(json, "cooldown", 10),
                    GsonHelper.getAsInt(json, "levels", 0), GsonHelper.getAsString(json, "name_key", ""));
        }
        @Override public CombinationForgingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            String result = buffer.readUtf();
            int resultCount = buffer.readVarInt();
            int size = buffer.readVarInt();
            if (size < 1 || size > 12) throw new IllegalArgumentException("Invalid forging input count");
            List<Input> inputs = new ArrayList<>();
            for (int i = 0; i < size; i++) inputs.add(new Input(buffer.readUtf(), buffer.readUtf(), buffer.readVarInt()));
            return new CombinationForgingRecipe(id, result, resultCount, inputs,
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, CombinationForgingRecipe recipe) {
            buffer.writeUtf(recipe.resultId);
            buffer.writeVarInt(recipe.resultCount);
            buffer.writeVarInt(recipe.inputs.size());
            for (Input input : recipe.inputs) {
                buffer.writeUtf(input.item());
                buffer.writeUtf(input.tag());
                buffer.writeVarInt(input.count());
            }
            buffer.writeVarInt(recipe.clicks);
            buffer.writeVarInt(recipe.cooldown);
            buffer.writeVarInt(recipe.levels);
            buffer.writeUtf(recipe.nameKey);
        }
        //?} else {
        /*@Override public MapCodec<CombinationForgingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, CombinationForgingRecipe> streamCodec() {
            return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
        }
        *///?}
    }
}
