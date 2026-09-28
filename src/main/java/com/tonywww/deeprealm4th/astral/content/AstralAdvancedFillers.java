package com.tonywww.deeprealm4th.astral.content;

import com.tonywww.deeprealm4th.astral.AstralFillers;
import com.tonywww.deeprealm4th.astral.FillerActivation;
import com.tonywww.deeprealm4th.astral.FillerDefinition;
import com.tonywww.deeprealm4th.astral.ScoreRules;
import com.tonywww.deeprealm4th.astral.ScoreType;

/** Approved progression fillers. Their acquisition recipes live in the process registry. */
public final class AstralAdvancedFillers {
    private static final String MOD = "deeprealm_4th:";

    private AstralAdvancedFillers() {}

    public static void registerAll() {
        unique("convergent_facet_gem", FillerDefinition.builder()
                .base(ScoreType.STRENGTH, 3)
                .bonus(ScoreRules.adjacent(MOD + "perception_gem", ScoreType.STRENGTH, 2))
                .suppressedBy(MOD + "convergent_facet_core"));
        unique("gathered_radiance_gem", FillerDefinition.builder()
                .base(ScoreType.MAGIC, 3)
                .bonus(ScoreRules.countAtLeast(MOD + "magic_gem", 2, ScoreType.INTELLIGENCE, 2))
                .suppressedBy(MOD + "gathered_radiance_core"));
        unique("balance_crystal_gem", FillerDefinition.builder()
                .base(ScoreType.CONSTITUTION, 3)
                .conversion(ScoreRules.convert(ScoreType.CONSTITUTION, ScoreType.AGILITY, 0.25, 1.5))
                .suppressedBy(MOD + "balance_core"));
        unique("etched_step_core", FillerDefinition.builder()
                .base(ScoreType.INTELLIGENCE, 3)
                .bonus(ScoreRules.whenExperienceLevelAbove(30, ScoreType.INTELLIGENCE, 3)));
        unique("full_breath_core", FillerDefinition.builder()
                .base(ScoreType.CONSTITUTION, 3)
                .bonus(ScoreRules.whenHealthAbove(16, ScoreType.CONSTITUTION, 3)));
        unique("convergent_facet_core", FillerDefinition.builder()
                .base(ScoreType.STRENGTH, 4)
                .bonus(ScoreRules.when(context -> context.countAdjacentTag("deeprealm_4th:astral/gems") >= 2,
                        ScoreType.PERCEPTION, 3)));
        unique("gathered_radiance_core", FillerDefinition.builder()
                .base(ScoreType.MAGIC, 4)
                .bonus(ScoreRules.countAtLeast(MOD + "magic_gem", 3, ScoreType.INTELLIGENCE, 3)));
        unique("balance_core", FillerDefinition.builder()
                .base(ScoreType.CONSTITUTION, 4)
                .conversion(ScoreRules.convert(ScoreType.CONSTITUTION, ScoreType.AGILITY, 1.0 / 3, 2)));
        unique("reflected_radiance_core", FillerDefinition.builder()
                .base(ScoreType.MAGIC, 3)
                .bonus(ScoreRules.whenPercent(
                        context -> context.countAdjacentTag("deeprealm_4th:astral/medals") >= 2,
                        ScoreType.MAGIC, ScoreType.MAGIC, 0.5, 6)));
        AstralFillers.register(MOD + "sixfold_balance_core", FillerDefinition.builder()
                .activation(FillerActivation.UNIQUE_WORN)
                .base(ScoreType.STRENGTH, 1)
                .base(ScoreType.AGILITY, 1)
                .base(ScoreType.INTELLIGENCE, 1)
                .base(ScoreType.CONSTITUTION, 1)
                .base(ScoreType.PERCEPTION, 1)
                .base(ScoreType.MAGIC, 1)
                .build());
    }

    private static void unique(String id, FillerDefinition.Builder builder) {
        AstralFillers.register(MOD + id, builder.activation(FillerActivation.UNIQUE_EFFECT).build());
    }
}
