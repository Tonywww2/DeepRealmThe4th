package com.tony.deeprealmtheforth.astral;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Public registration point for Java addons and KubeJS bindings. */
public final class AstralFillers {
    private static final Map<String, FillerDefinition> DEFINITIONS = new ConcurrentHashMap<>();

    static {
        register("deeprealm_4th:strength_gem", FillerDefinition.builder()
                .activation(FillerActivation.STACKABLE)
                .base(ScoreType.STRENGTH, 1)
                .build());
        register("deeprealm_4th:warrior_medal", FillerDefinition.builder()
                .activation(FillerActivation.UNIQUE_EFFECT)
                .medal((context, scores, attributes) -> attributes.multiplyTotal(
                        "minecraft:generic.attack_damage",
                        WarriorMedalFormula.attackFraction(scores.get(ScoreType.STRENGTH))))
                .build());
    }

    private AstralFillers() {}

    public static void register(String itemId, FillerDefinition definition) {
        validateId(itemId);
        Objects.requireNonNull(definition, "definition");
        if (DEFINITIONS.putIfAbsent(itemId, definition) != null) {
            throw new IllegalArgumentException("Astral filler already registered: " + itemId);
        }
    }

    public static FillerDefinition find(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return DEFINITIONS.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    public static FillerDefinition find(String itemId) {
        return DEFINITIONS.get(itemId);
    }

    private static void validateId(String itemId) {
        if (itemId == null || !itemId.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+")) {
            throw new IllegalArgumentException("Expected a namespaced item ID, got: " + itemId);
        }
    }
}
