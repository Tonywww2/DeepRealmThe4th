package com.tonywww.deeprealm4th.astral.process;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Startup registration of dynamic, server-authoritative recipe outputs. */
public final class AstralTransforms {
    private static final Map<String, Transform> TRANSFORMS = new ConcurrentHashMap<>();
    private AstralTransforms() {}

    public record Result<T>(T value, String error) {
        public boolean ok() { return error == null; }
        public static <T> Result<T> success(T value) { return new Result<>(value, null); }
        public static <T> Result<T> failure(String reason) { return new Result<>(null, reason); }
    }

    public static final class ProcessContext {
        private final Player player;
        private final Level level;
        private final String recipeId, stage;
        private final Map<String, ItemStack> sources;
        private final List<ItemStack> catalysts;
        private final ItemStack template;

        public ProcessContext(Player player, Level level, String recipeId, String stage,
                Map<String, ItemStack> sources, List<ItemStack> catalysts) {
            this(player, level, recipeId, stage, sources, catalysts, ItemStack.EMPTY);
        }

        public ProcessContext(Player player, Level level, String recipeId, String stage,
                Map<String, ItemStack> sources, List<ItemStack> catalysts, ItemStack template) {
            this.player = player;
            this.level = Objects.requireNonNull(level);
            this.recipeId = recipeId;
            this.stage = stage;
            Map<String, ItemStack> copies = new LinkedHashMap<>();
            sources.forEach((name, stack) -> copies.put(name, stack.copy()));
            this.sources = Map.copyOf(copies);
            this.catalysts = catalysts.stream().map(ItemStack::copy).toList();
            this.template = Objects.requireNonNull(template, "template").copy();
        }
        public Player player() { return player; }
        public Level level() { return level; }
        public String recipeId() { return recipeId; }
        public String stage() { return stage; }
        public ItemStack source(String name) {
            ItemStack value = sources.get(name);
            return value == null ? ItemStack.EMPTY : value.copy();
        }
        public Map<String, ItemStack> sources() {
            Map<String, ItemStack> copy = new LinkedHashMap<>();
            sources.forEach((name, stack) -> copy.put(name, stack.copy()));
            return Map.copyOf(copy);
        }
        public List<ItemStack> catalysts() { return catalysts.stream().map(ItemStack::copy).toList(); }
        public ItemStack template() { return template.copy(); }
    }

    public static final class ProcessOutput {
        private final ItemStack result;
        private final Map<String, Integer> consumed;
        private final List<ItemStack> returned;
        public ProcessOutput(ItemStack result, Map<String, Integer> consumed, List<ItemStack> returned) {
            if (result == null || result.isEmpty() || result.getCount() > result.getMaxStackSize())
                throw new IllegalArgumentException("Invalid transform result");
            this.result = result.copy();
            this.consumed = Map.copyOf(consumed);
            this.returned = returned.stream().map(ItemStack::copy).toList();
            if (this.consumed.values().stream().anyMatch(amount -> amount < 0))
                throw new IllegalArgumentException("Negative transform consumption");
            if (this.returned.stream().anyMatch(stack -> stack.isEmpty() || stack.getCount() > stack.getMaxStackSize()))
                throw new IllegalArgumentException("Invalid returned item stack");
        }
        public static ProcessOutput of(ItemStack result) { return new ProcessOutput(result, Map.of(), List.of()); }
        public ItemStack result() { return result.copy(); }
        public Map<String, Integer> consumed() { return consumed; }
        public List<ItemStack> returned() { return returned.stream().map(ItemStack::copy).toList(); }
    }

    @FunctionalInterface public interface Matcher { boolean matches(ProcessContext context); }
    @FunctionalInterface public interface Preview { Result<ItemStack> preview(ProcessContext context); }
    @FunctionalInterface public interface Producer { Result<ProcessOutput> produce(ProcessContext context); }
    public record Transform(Matcher matches, Preview preview, Producer produce) {
        public Transform {
            Objects.requireNonNull(matches);
            Objects.requireNonNull(preview);
            Objects.requireNonNull(produce);
        }
    }

    public static void register(String transformId, Transform callbacks) {
        ProcessItemIds.requireValid(transformId);
        if (TRANSFORMS.putIfAbsent(transformId, Objects.requireNonNull(callbacks)) != null)
            throw new IllegalArgumentException("Astral transform already registered: " + transformId);
    }
    public static void register(String transformId, Matcher matches, Preview preview, Producer produce) {
        register(transformId, new Transform(matches, preview, produce));
    }
    public static Transform find(String transformId) { return TRANSFORMS.get(transformId); }

    /** Script-friendly callback return values without nested generic constructor calls. */
    public static Result<ItemStack> previewItem(ItemStack stack) { return Result.success(stack.copy()); }
    public static Result<ProcessOutput> producedItem(ItemStack stack) {
        return Result.success(ProcessOutput.of(stack));
    }
    public static Result<ItemStack> failedPreview(String reason) { return Result.failure(reason); }
    public static Result<ProcessOutput> failedProduce(String reason) { return Result.failure(reason); }

    public static Result<ItemStack> preview(String transformId, ProcessContext context) {
        Transform transform = find(transformId);
        if (transform == null) return Result.failure("missing_transform");
        try {
            if (!transform.matches().matches(context)) return Result.failure("transform_does_not_match");
            Result<ItemStack> result = transform.preview().preview(context);
            if (result == null) return Result.failure("transform_preview_failed");
            if (!result.ok()) return Result.failure(result.error());
            if (result.value() == null || result.value().isEmpty())
                return Result.failure("transform_preview_empty");
            return Result.success(result.value().copy());
        } catch (RuntimeException exception) { return Result.failure("transform_preview_error: " + exception.getMessage()); }
    }

    public static Result<ProcessOutput> produce(String transformId, ProcessContext context) {
        Transform transform = find(transformId);
        if (transform == null) return Result.failure("missing_transform");
        try {
            if (!transform.matches().matches(context)) return Result.failure("transform_does_not_match");
            Result<ProcessOutput> result = transform.produce().produce(context);
            if (result == null) return Result.failure("transform_produce_failed");
            if (!result.ok()) return Result.failure(result.error());
            if (result.value() == null) return Result.failure("transform_produce_empty");
            return result;
        } catch (RuntimeException exception) { return Result.failure("transform_produce_error: " + exception.getMessage()); }
    }
}
