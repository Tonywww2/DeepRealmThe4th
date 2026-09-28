package com.tonywww.deeprealm4th.astral;

import com.tonywww.deeprealm4th.astral.content.AstralAdvancedFillers;
import com.tonywww.deeprealm4th.astral.score.AstralAttributeIds;
import com.tonywww.deeprealm4th.astral.score.AstralMedalFormulas;
import com.tonywww.deeprealm4th.astral.score.WarriorMedalFormula;

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
        scoreGem("agility_gem", ScoreType.AGILITY);
        scoreGem("intelligence_gem", ScoreType.INTELLIGENCE);
        scoreGem("constitution_gem", ScoreType.CONSTITUTION);
        scoreGem("perception_gem", ScoreType.PERCEPTION);
        scoreGem("magic_gem", ScoreType.MAGIC);
        register("deeprealm_4th:arid_ridge_gem", FillerDefinition.builder()
                .activation(FillerActivation.UNIQUE_EFFECT)
                .base(ScoreType.PERCEPTION, 1)
                .bonus(ScoreRules.adjacent("deeprealm_4th:strength_gem", ScoreType.PERCEPTION, 1))
                .bonus(ScoreRules.adjacent("deeprealm_4th:perception_gem", ScoreType.STRENGTH, 1))
                .build());
        register("deeprealm_4th:magma_vein_gem", FillerDefinition.builder()
                .activation(FillerActivation.UNIQUE_EFFECT)
                .base(ScoreType.CONSTITUTION, 1)
                .bonus(ScoreRules.countAtLeast("deeprealm_4th:strength_gem", 2, ScoreType.CONSTITUTION, 1))
                .bonus(ScoreRules.adjacent("deeprealm_4th:magic_gem", ScoreType.MAGIC, 1))
                .build());
        register("deeprealm_4th:canopy_gem", FillerDefinition.builder()
                .activation(FillerActivation.UNIQUE_EFFECT)
                .base(ScoreType.AGILITY, 1)
                .bonus(ScoreRules.adjacent("deeprealm_4th:perception_gem", ScoreType.AGILITY, 1))
                .bonus(ScoreRules.adjacent("deeprealm_4th:agility_gem", ScoreType.PERCEPTION, 1))
                .build());
        register("deeprealm_4th:tidal_gem", FillerDefinition.builder()
                .activation(FillerActivation.UNIQUE_EFFECT)
                .base(ScoreType.MAGIC, 1)
                .bonus(ScoreRules.countAtLeast("deeprealm_4th:magic_gem", 2, ScoreType.MAGIC, 1))
                .bonus(ScoreRules.adjacent("deeprealm_4th:intelligence_gem", ScoreType.INTELLIGENCE, 1))
                .build());
        register("deeprealm_4th:warrior_medal", FillerDefinition.builder()
                .activation(FillerActivation.UNIQUE_EFFECT)
                .medal((context, scores, attributes) -> attributes.multiplyTotal(
                        AstralAttributeIds.ATTACK_DAMAGE,
                        WarriorMedalFormula.attackFraction(scores.get(ScoreType.STRENGTH))))
                .build());

        unique("facet_bridge_gem", FillerDefinition.builder()
                .base(ScoreType.STRENGTH, 1)
                .bonus(ScoreRules.adjacent("deeprealm_4th:constitution_gem", ScoreType.PERCEPTION, 1)));
        unique("twin_mirror_gem", FillerDefinition.builder()
                .base(ScoreType.INTELLIGENCE, 1)
                .bonus(ScoreRules.adjacent("deeprealm_4th:magic_gem", ScoreType.INTELLIGENCE, 1)));
        unique("wandering_stripe_gem", FillerDefinition.builder()
                .base(ScoreType.AGILITY, 1)
                .bonus(ScoreRules.countAtLeast("deeprealm_4th:perception_gem", 2, ScoreType.MAGIC, 1)));
        unique("ember_remnant_gem", FillerDefinition.builder()
                .base(ScoreType.CONSTITUTION, 1)
                .bonus(ScoreRules.countAtLeast("deeprealm_4th:strength_gem", 2, ScoreType.STRENGTH, 1)));
        unique("balance_shift_gem", FillerDefinition.builder()
                .conversion(ScoreRules.convert(ScoreType.STRENGTH, ScoreType.CONSTITUTION, 1.0 / 3, 1.5)));
        unique("returning_ray_gem", FillerDefinition.builder()
                .conversion(ScoreRules.convert(ScoreType.MAGIC, ScoreType.INTELLIGENCE, 1.0 / 3, 1.5)));
        unique("split_edge_gem", FillerDefinition.builder()
                .base(ScoreType.STRENGTH, 2)
                .base(ScoreType.CONSTITUTION, -1)
                .suppressedBy("deeprealm_4th:steady_anchor_gem"));
        unique("steady_anchor_gem", FillerDefinition.builder()
                .base(ScoreType.CONSTITUTION, 2)
                .base(ScoreType.AGILITY, -1));
        unique("wayfarer_medal", FillerDefinition.builder()
                .medal((context, scores, attributes) -> attributes.multiplyTotal(
                        AstralAttributeIds.MOVEMENT_SPEED,
                        AstralMedalFormulas.movementFraction(scores.get(ScoreType.AGILITY)))));
        unique("warden_medal", FillerDefinition.builder()
                .medal((context, scores, attributes) -> attributes.add(
                        AstralAttributeIds.MAX_HEALTH,
                        AstralMedalFormulas.maxHealthBonus(scores.get(ScoreType.CONSTITUTION)))));

        unique("vein_amplitude_gem", FillerDefinition.builder()
                .bonus(ScoreRules.percentOf(ScoreType.STRENGTH, ScoreType.STRENGTH, 0.20, 4)));
        unique("wandering_shadow_gem", FillerDefinition.builder()
                .bonus(ScoreRules.percentOf(ScoreType.AGILITY, ScoreType.PERCEPTION, 0.25, 4)));
        unique("folded_reflection_gem", FillerDefinition.builder()
                .bonus(ScoreRules.percentOf(ScoreType.INTELLIGENCE, ScoreType.MAGIC, 0.25, 4)));
        unique("linked_vein_gem", FillerDefinition.builder()
                .bonus(ScoreRules.percentWhenAdjacent("deeprealm_4th:constitution_gem",
                        ScoreType.CONSTITUTION, ScoreType.STRENGTH, 0.30, 4)));
        unique("cluster_mirror_gem", FillerDefinition.builder()
                .bonus(ScoreRules.percentWhenCountAtLeast("deeprealm_4th:perception_gem", 2,
                        ScoreType.PERCEPTION, ScoreType.INTELLIGENCE, 0.30, 4)));
        unique("looped_trace_gem", FillerDefinition.builder()
                .bonus(ScoreRules.percentWhenAdjacent("deeprealm_4th:magic_gem",
                        ScoreType.MAGIC, ScoreType.MAGIC, 0.30, 4)));
        unique("etched_step_gem", FillerDefinition.builder()
                .bonus(ScoreRules.whenExperienceLevelAbove(30, ScoreType.INTELLIGENCE, 2))
                .suppressedBy("deeprealm_4th:etched_step_core"));
        unique("full_breath_gem", FillerDefinition.builder()
                .bonus(ScoreRules.whenHealthAbove(16, ScoreType.CONSTITUTION, 2))
                .suppressedBy("deeprealm_4th:full_breath_core"));
        unique("last_edge_gem", FillerDefinition.builder()
                .bonus(ScoreRules.whenHealthAtMost(8, ScoreType.STRENGTH, 3)));
        unique("well_fed_glow_gem", FillerDefinition.builder()
                .bonus(ScoreRules.whenFoodAtLeast(18, ScoreType.AGILITY, 2)));
        unique("nightglow_gem", FillerDefinition.builder()
                .bonus(ScoreRules.whenNightVision(ScoreType.PERCEPTION, 2)));
        AstralAdvancedFillers.registerAll();
    }

    private AstralFillers() {}

    private static void scoreGem(String id, ScoreType type) {
        register("deeprealm_4th:" + id, FillerDefinition.builder()
                .activation(FillerActivation.STACKABLE)
                .base(type, 1)
                .build());
    }

    private static void unique(String id, FillerDefinition.Builder builder) {
        register("deeprealm_4th:" + id, builder.activation(FillerActivation.UNIQUE_EFFECT).build());
    }

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
