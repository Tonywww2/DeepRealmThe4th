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
//?} else {
/*import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeInput;
*///?}

/** Shapeless twelve-slot forging with exact stack data and per-slot counts. */
public final class CombinationForgingRecipe implements Recipe<
        //? if forge {
        Container
        //?} else {
        /*RecipeInput
        *///?}
        > {
    /** A required stack, or an explicit tag alternative with a required count. */
    public record Input(ItemStack stack, String tag, int tagCount) {
        //? if !forge {
        /*public static final Codec<Input> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.STRICT_CODEC.optionalFieldOf("stack", ItemStack.EMPTY).forGetter(Input::stack),
                Codec.STRING.optionalFieldOf("tag", "").forGetter(Input::tag),
                Codec.INT.optionalFieldOf("count", 1).forGetter(Input::tagCount)
        ).apply(instance, Input::new));
        *///?}

        public Input {
            if (stack == null || tag == null) throw new IllegalArgumentException("Missing forging input");
            stack = stack.copy();
            if (stack.isEmpty() == tag.isEmpty())
                throw new IllegalArgumentException("Exactly one of stack or tag is required");
            if (!tag.isEmpty()) ProcessItemIds.requireValid(tag);
            if (tagCount < 1 || tagCount > 64 || !stack.isEmpty() && tagCount != 1)
                throw new IllegalArgumentException("Invalid forging input count");
        }

        @Override public ItemStack stack() { return stack.copy(); }
        public int count() { return tag.isEmpty() ? stack.getCount() : tagCount; }

        public boolean accepts(ItemStack candidate) {
            if (candidate.isEmpty() || candidate.getCount() < count()) return false;
            return tag.isEmpty() ? ItemStack.matches(stack.copyWithCount(1), candidate.copyWithCount(1))
                    : candidate.is(TagKey.create(Registries.ITEM, ResourceLocation.tryParse(tag)));
        }

        public ItemStack displayStack() {
            if (tag.isEmpty()) return stack();
            var entry = net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(TagKey.create(
                    Registries.ITEM, ResourceLocation.tryParse(tag)));
            return entry.flatMap(items -> items.stream().findFirst())
                    .map(holder -> new ItemStack(holder.value(), tagCount)).orElse(ItemStack.EMPTY);
        }
    }

    private final ItemStack result;
    private final List<Input> inputs;
    private final int clicks;
    private final int cooldown;
    private final int levels;
    private final String nameKey;
    //? if forge {
    private final ResourceLocation id;
    //?} else {
    /*public static final MapCodec<CombinationForgingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CombinationForgingRecipe::output),
            Input.CODEC.listOf().fieldOf("ingredients").forGetter(CombinationForgingRecipe::inputs),
            Codec.INT.optionalFieldOf("clicks", 3).forGetter(CombinationForgingRecipe::clicks),
            Codec.INT.optionalFieldOf("cooldown", 10).forGetter(CombinationForgingRecipe::cooldown),
            Codec.INT.optionalFieldOf("levels", 0).forGetter(CombinationForgingRecipe::levels),
            Codec.STRING.optionalFieldOf("name_key", "").forGetter(CombinationForgingRecipe::nameKey)
    ).apply(instance, CombinationForgingRecipe::new));
    *///?}

    //? if forge {
    public CombinationForgingRecipe(ResourceLocation id, ItemStack result, List<Input> inputs,
                                    int clicks, int cooldown, int levels, String nameKey) {
        this.id = id;
        this.result = result.copy();
        this.inputs = List.copyOf(inputs);
        this.clicks = clicks;
        this.cooldown = cooldown;
        this.levels = levels;
        this.nameKey = nameKey == null ? "" : nameKey;
        validate();
    }
    //?} else {
    /*public CombinationForgingRecipe(ItemStack result, List<Input> inputs,
                                    int clicks, int cooldown, int levels, String nameKey) {
        this.result = result.copy();
        this.inputs = List.copyOf(inputs);
        this.clicks = clicks;
        this.cooldown = cooldown;
        this.levels = levels;
        this.nameKey = nameKey == null ? "" : nameKey;
        validate();
    }
    *///?}

    private void validate() {
        if (result.isEmpty() || result.getCount() > result.getMaxStackSize())
            throw new IllegalArgumentException("Invalid forging result stack");
        if (inputs.isEmpty() || inputs.size() > 12) throw new IllegalArgumentException("Forging needs 1..12 inputs");
        if (clicks < 1 || clicks > 64) throw new IllegalArgumentException("Clicks must be 1..64");
        if (cooldown < 0 || cooldown > 1200) throw new IllegalArgumentException("Invalid click cooldown");
        if (levels < 0 || levels > 10000) throw new IllegalArgumentException("Invalid level cost");
    }

    public List<Input> inputs() { return inputs; }
    public int clicks() { return clicks; }
    public int cooldown() { return cooldown; }
    public int levels() { return levels; }
    public String nameKey() { return nameKey; }
    public ItemStack output() { return result.copy(); }

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
                inputs.add(new Input(entry.has("stack") ? CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(entry, "stack"), true, true)
                                : ItemStack.EMPTY,
                        GsonHelper.getAsString(entry, "tag", ""), GsonHelper.getAsInt(entry, "count", 1)));
            }
            return new CombinationForgingRecipe(id, CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "result"), true, true),
                    inputs, GsonHelper.getAsInt(json, "clicks", 3), GsonHelper.getAsInt(json, "cooldown", 10),
                    GsonHelper.getAsInt(json, "levels", 0), GsonHelper.getAsString(json, "name_key", ""));
        }
        @Override public CombinationForgingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            ItemStack result = buffer.readItem();
            int size = buffer.readVarInt();
            if (size < 1 || size > 12) throw new IllegalArgumentException("Invalid forging input count");
            List<Input> inputs = new ArrayList<>();
            for (int i = 0; i < size; i++) inputs.add(new Input(buffer.readItem(), buffer.readUtf(), buffer.readVarInt()));
            return new CombinationForgingRecipe(id, result, inputs,
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, CombinationForgingRecipe recipe) {
            buffer.writeItem(recipe.result);
            buffer.writeVarInt(recipe.inputs.size());
            for (Input input : recipe.inputs) {
                buffer.writeItem(input.stack());
                buffer.writeUtf(input.tag());
                buffer.writeVarInt(input.tagCount());
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
