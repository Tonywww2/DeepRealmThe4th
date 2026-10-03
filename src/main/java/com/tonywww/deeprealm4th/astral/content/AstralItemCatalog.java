package com.tonywww.deeprealm4th.astral.content;

import com.tonywww.deeprealm4th.astral.ScoreType;
import com.tonywww.deeprealm4th.astral.score.AstralScoreColors;
import com.tonywww.deeprealm4th.item.AstralContainerItem;
import com.tonywww.deeprealm4th.item.AstralFillerItem;
import com.tonywww.deeprealm4th.item.AstralMaterialItem;
import com.tonywww.deeprealm4th.item.ProjectionFrameItem;
import com.tonywww.deeprealm4th.platform.registry.AstralBlockRegistration;
import com.tonywww.deeprealm4th.platform.registry.AstralItemRegistration;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/** Built-in astral item catalog and its translated tooltips. */
public final class AstralItemCatalog {
    public static final Supplier<AstralContainerItem> BASE_CONTAINER =
            registerItem("base_container", () -> new AstralContainerItem(new Item.Properties()));
    public static final Supplier<Item> WARRIOR_MEDAL =
            registerItem("warrior_medal", () -> new AstralFillerItem(new Item.Properties().stacksTo(1), List.of(
                    Component.translatable("tooltip.deeprealm_4th.warrior_medal.effect",
                            AstralScoreColors.name(ScoreType.STRENGTH)).withStyle(ChatFormatting.GRAY))));
    public static final Supplier<Item> STRENGTH_GEM =
            scoreGem("strength_gem", ScoreType.STRENGTH);
    public static final Supplier<Item> AGILITY_GEM = scoreGem("agility_gem", ScoreType.AGILITY);
    public static final Supplier<Item> INTELLIGENCE_GEM = scoreGem("intelligence_gem", ScoreType.INTELLIGENCE);
    public static final Supplier<Item> CONSTITUTION_GEM = scoreGem("constitution_gem", ScoreType.CONSTITUTION);
    public static final Supplier<Item> PERCEPTION_GEM = scoreGem("perception_gem", ScoreType.PERCEPTION);
    public static final Supplier<Item> MAGIC_GEM = scoreGem("magic_gem", ScoreType.MAGIC);
    public static final Supplier<Item> ARID_RIDGE_GEM = regionalGem("arid_ridge_gem",
            ScoreType.PERCEPTION, ScoreType.STRENGTH, ScoreType.PERCEPTION,
            ScoreType.PERCEPTION, ScoreType.STRENGTH);
    public static final Supplier<Item> MAGMA_VEIN_GEM = regionalGem("magma_vein_gem",
            ScoreType.CONSTITUTION, ScoreType.STRENGTH, ScoreType.CONSTITUTION,
            ScoreType.MAGIC, ScoreType.MAGIC);
    public static final Supplier<Item> CANOPY_GEM = regionalGem("canopy_gem",
            ScoreType.AGILITY, ScoreType.PERCEPTION, ScoreType.AGILITY,
            ScoreType.AGILITY, ScoreType.PERCEPTION);
    public static final Supplier<Item> TIDAL_GEM = regionalGem("tidal_gem",
            ScoreType.MAGIC, ScoreType.MAGIC, ScoreType.MAGIC,
            ScoreType.INTELLIGENCE, ScoreType.INTELLIGENCE);

    public static final Supplier<Item> FACET_BRIDGE_GEM = gem("facet_bridge_gem",
            AstralScoreColors.gain(ScoreType.STRENGTH, 1),
            adjacentGain(ScoreType.CONSTITUTION, ScoreType.PERCEPTION, 1));
    public static final Supplier<Item> TWIN_MIRROR_GEM = gem("twin_mirror_gem",
            AstralScoreColors.gain(ScoreType.INTELLIGENCE, 1),
            adjacentGain(ScoreType.MAGIC, ScoreType.INTELLIGENCE, 1));
    public static final Supplier<Item> WANDERING_STRIPE_GEM = gem("wandering_stripe_gem",
            AstralScoreColors.gain(ScoreType.AGILITY, 1),
            countGain(ScoreType.PERCEPTION, 2, ScoreType.MAGIC, 1));
    public static final Supplier<Item> EMBER_REMNANT_GEM = gem("ember_remnant_gem",
            AstralScoreColors.gain(ScoreType.CONSTITUTION, 1),
            countGain(ScoreType.STRENGTH, 2, ScoreType.STRENGTH, 1));
    public static final Supplier<Item> BALANCE_SHIFT_GEM = gem("balance_shift_gem",
            conversionConsume(ScoreType.STRENGTH), conversionGain(ScoreType.CONSTITUTION));
    public static final Supplier<Item> RETURNING_RAY_GEM = gem("returning_ray_gem",
            conversionConsume(ScoreType.MAGIC), conversionGain(ScoreType.INTELLIGENCE));
    public static final Supplier<Item> SPLIT_EDGE_GEM = gem("split_edge_gem",
            AstralScoreColors.gain(ScoreType.STRENGTH, 2),
            AstralScoreColors.gain(ScoreType.CONSTITUTION, -1));
    public static final Supplier<Item> STEADY_ANCHOR_GEM = gem("steady_anchor_gem",
            AstralScoreColors.gain(ScoreType.CONSTITUTION, 2),
            AstralScoreColors.gain(ScoreType.AGILITY, -1));
    public static final Supplier<Item> WAYFARER_MEDAL = registerItem("wayfarer_medal",
            () -> new AstralFillerItem(new Item.Properties().stacksTo(1), List.of(
                    gray("tooltip.deeprealm_4th.wayfarer_medal.effect",
                            AstralScoreColors.name(ScoreType.AGILITY)))));
    public static final Supplier<Item> WARDEN_MEDAL = registerItem("warden_medal",
            () -> new AstralFillerItem(new Item.Properties().stacksTo(1), List.of(
                    gray("tooltip.deeprealm_4th.warden_medal.effect",
                            AstralScoreColors.name(ScoreType.CONSTITUTION)))));

    public static final Supplier<Item> VEIN_AMPLITUDE_GEM = percentGem("vein_amplitude_gem", ScoreType.STRENGTH, ScoreType.STRENGTH, 20);
    public static final Supplier<Item> WANDERING_SHADOW_GEM = percentGem("wandering_shadow_gem", ScoreType.AGILITY, ScoreType.PERCEPTION, 25);
    public static final Supplier<Item> FOLDED_REFLECTION_GEM = percentGem("folded_reflection_gem", ScoreType.INTELLIGENCE, ScoreType.MAGIC, 25);
    public static final Supplier<Item> LINKED_VEIN_GEM = gem("linked_vein_gem",
            gray("tooltip.deeprealm_4th.percent_adjacent",
                    AstralScoreColors.gemName(ScoreType.CONSTITUTION),
                    AstralScoreColors.name(ScoreType.CONSTITUTION), 30,
                    AstralScoreColors.name(ScoreType.STRENGTH), 4));
    public static final Supplier<Item> CLUSTER_MIRROR_GEM = gem("cluster_mirror_gem",
            gray("tooltip.deeprealm_4th.percent_count",
                    AstralScoreColors.gemName(ScoreType.PERCEPTION), 2,
                    AstralScoreColors.name(ScoreType.PERCEPTION), 30,
                    AstralScoreColors.name(ScoreType.INTELLIGENCE), 4));
    public static final Supplier<Item> LOOPED_TRACE_GEM = gem("looped_trace_gem",
            gray("tooltip.deeprealm_4th.percent_adjacent",
                    AstralScoreColors.gemName(ScoreType.MAGIC),
                    AstralScoreColors.name(ScoreType.MAGIC), 30,
                    AstralScoreColors.name(ScoreType.MAGIC), 4));
    public static final Supplier<Item> ETCHED_STEP_GEM = conditionGem("etched_step_gem",
            "level_above_30", ScoreType.INTELLIGENCE, 2);
    public static final Supplier<Item> FULL_BREATH_GEM = conditionGem("full_breath_gem",
            "health_above_16", ScoreType.CONSTITUTION, 2);
    public static final Supplier<Item> LAST_EDGE_GEM = conditionGem("last_edge_gem",
            "health_at_most_8", ScoreType.STRENGTH, 3);
    public static final Supplier<Item> WELL_FED_GLOW_GEM = conditionGem("well_fed_glow_gem",
            "food_at_least_18", ScoreType.AGILITY, 2);
    public static final Supplier<Item> NIGHTGLOW_GEM = conditionGem("nightglow_gem",
            "night_vision", ScoreType.PERCEPTION, 2);

    public static final Supplier<Item> STAR_SLURRY = material("star_slurry", "crystal");
    public static final Supplier<Item> STAR_SLURRY_CRYSTAL = material("star_slurry_crystal");
    public static final Supplier<Item> STAR_SOURCE = blockItem("star_source", AstralBlockRegistration.STAR_SOURCE);
    public static final Supplier<Item> SMALL_STAR_SLURRY_BUD = blockItem("small_star_slurry_bud", AstralBlockRegistration.SMALL_STAR_SLURRY_BUD);
    public static final Supplier<Item> MEDIUM_STAR_SLURRY_BUD = blockItem("medium_star_slurry_bud", AstralBlockRegistration.MEDIUM_STAR_SLURRY_BUD);
    public static final Supplier<Item> LARGE_STAR_SLURRY_BUD = blockItem("large_star_slurry_bud", AstralBlockRegistration.LARGE_STAR_SLURRY_BUD);
    public static final Supplier<Item> STAR_SLURRY_CLUSTER = blockItem("star_slurry_cluster", AstralBlockRegistration.STAR_SLURRY_CLUSTER);
    public static final Supplier<Item> FORGING_PAD = registerItem("forging_pad",
            () -> new BlockItem(AstralBlockRegistration.FORGING_PAD.get(), new Item.Properties()));
    public static final Supplier<Item> MIMETIC_STAR_SLURRY = material("mimetic_star_slurry", "mimetic");
    public static final Supplier<Item> PROJECTION_FRAME = registerItem("projection_frame",
            () -> new ProjectionFrameItem(new Item.Properties().stacksTo(1)));
    /** Plain model used inside the dynamic frame renderer; not a player-facing item. */
    public static final Supplier<Item> PROJECTION_FRAME_SHELL = registerItem("projection_frame_shell",
            () -> new Item(new Item.Properties()));
    public static final Supplier<Item> STAR_SLURRY_BLANK = material("star_slurry_blank");
    public static final Supplier<Item> ASTRAL_LENS = material("astral_lens");
    public static final Supplier<Item> STABILIZED_STAR_SLURRY = material("stabilized_star_slurry");
    public static final Supplier<Item> CRUX_PROJECTION = material("crux_projection");
    public static final Supplier<Item> TELESCOPIUM_PROJECTION = material("telescopium_projection");
    public static final Supplier<Item> TRIANGULUM_AUSTRALE_PROJECTION = material("triangulum_australe_projection");
    public static final Supplier<Item> CRUX_FRAGMENT = material("crux_fragment");
    public static final Supplier<Item> TELESCOPIUM_FRAGMENT = material("telescopium_fragment");
    public static final Supplier<Item> TRIANGULUM_AUSTRALE_FRAGMENT = material("triangulum_australe_fragment");
    public static final Supplier<Item> UNFINISHED_ETCHED_BLANK = material("unfinished_etched_blank");
    public static final Supplier<Item> UNFINISHED_BREATH_BLANK = material("unfinished_breath_blank");

    public static final Supplier<Item> CONVERGENT_FACET_GEM = advancedGem("convergent_facet_gem",
            AstralScoreColors.gain(ScoreType.STRENGTH, 3), adjacentGain(ScoreType.PERCEPTION, ScoreType.STRENGTH, 2));
    public static final Supplier<Item> GATHERED_RADIANCE_GEM = advancedGem("gathered_radiance_gem",
            AstralScoreColors.gain(ScoreType.MAGIC, 3), countGain(ScoreType.MAGIC, 2, ScoreType.INTELLIGENCE, 2));
    public static final Supplier<Item> BALANCE_CRYSTAL_GEM = advancedGem("balance_crystal_gem",
            AstralScoreColors.gain(ScoreType.CONSTITUTION, 3), gray("tooltip.deeprealm_4th.advanced.convert_quarter",
                    AstralScoreColors.name(ScoreType.CONSTITUTION), AstralScoreColors.name(ScoreType.AGILITY)));
    public static final Supplier<Item> ETCHED_STEP_CORE = advancedGem("etched_step_core",
            AstralScoreColors.gain(ScoreType.INTELLIGENCE, 3), gray("tooltip.deeprealm_4th.condition.level_above_30",
                    AstralScoreColors.gain(ScoreType.INTELLIGENCE, 3)));
    public static final Supplier<Item> FULL_BREATH_CORE = advancedGem("full_breath_core",
            AstralScoreColors.gain(ScoreType.CONSTITUTION, 3), gray("tooltip.deeprealm_4th.condition.health_above_16",
                    AstralScoreColors.gain(ScoreType.CONSTITUTION, 3)));
    public static final Supplier<Item> CONVERGENT_FACET_CORE = advancedGem("convergent_facet_core",
            AstralScoreColors.gain(ScoreType.STRENGTH, 4), gray("tooltip.deeprealm_4th.advanced.two_adjacent_gems",
                    AstralScoreColors.gain(ScoreType.PERCEPTION, 3)));
    public static final Supplier<Item> GATHERED_RADIANCE_CORE = advancedGem("gathered_radiance_core",
            AstralScoreColors.gain(ScoreType.MAGIC, 4), countGain(ScoreType.MAGIC, 3, ScoreType.INTELLIGENCE, 3));
    public static final Supplier<Item> BALANCE_CORE = advancedGem("balance_core",
            AstralScoreColors.gain(ScoreType.CONSTITUTION, 4), gray("tooltip.deeprealm_4th.advanced.convert_third",
                    AstralScoreColors.name(ScoreType.CONSTITUTION), AstralScoreColors.name(ScoreType.AGILITY)));
    public static final Supplier<Item> REFLECTED_RADIANCE_CORE = advancedGem("reflected_radiance_core",
            AstralScoreColors.gain(ScoreType.MAGIC, 3), gray("tooltip.deeprealm_4th.advanced.two_adjacent_medals",
                    AstralScoreColors.name(ScoreType.MAGIC)));
    public static final Supplier<Item> SIXFOLD_BALANCE_CORE = advancedGem("sixfold_balance_core",
            gray("tooltip.deeprealm_4th.advanced.six_scores",
                    AstralScoreColors.name(ScoreType.STRENGTH), AstralScoreColors.name(ScoreType.AGILITY),
                    AstralScoreColors.name(ScoreType.INTELLIGENCE), AstralScoreColors.name(ScoreType.CONSTITUTION),
                    AstralScoreColors.name(ScoreType.PERCEPTION), AstralScoreColors.name(ScoreType.MAGIC)));

    private static Supplier<Item> gem(String id, Component... effects) {
        return registerItem(id, () -> new AstralFillerItem(new Item.Properties().stacksTo(1),
                List.of(effects)));
    }

    private static Supplier<Item> material(String id) {
        return material(id, null);
    }

    private static Supplier<Item> blockItem(String id, Supplier<? extends net.minecraft.world.level.block.Block> block) {
        return registerItem(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static Supplier<Item> material(String id, String hint) {
        return registerItem(id, () -> new AstralMaterialItem(new Item.Properties()
                .stacksTo(id.endsWith("star_slurry") ? 16 : 64), hint));
    }

    private static Supplier<Item> advancedGem(String id, Component... effects) {
        return gem(id, effects);
    }

    private static Supplier<Item> percentGem(String id, ScoreType input, ScoreType output, int rate) {
        return gem(id, gray("tooltip.deeprealm_4th.percent",
                AstralScoreColors.name(input), rate, AstralScoreColors.name(output), 4));
    }

    private static Supplier<Item> conditionGem(String id, String condition,
            ScoreType target, int amount) {
        return gem(id, gray("tooltip.deeprealm_4th.condition." + condition,
                AstralScoreColors.gain(target, amount)));
    }

    private static Component adjacentGain(ScoreType neighbor, ScoreType target, int amount) {
        return gray("tooltip.deeprealm_4th.adjacent_gain",
                AstralScoreColors.gemName(neighbor), AstralScoreColors.gain(target, amount));
    }

    private static Component countGain(ScoreType counted, int minimum, ScoreType target, int amount) {
        return gray("tooltip.deeprealm_4th.count_gain",
                AstralScoreColors.gemName(counted), minimum, AstralScoreColors.gain(target, amount));
    }

    private static Component conversionConsume(ScoreType source) {
        return gray("tooltip.deeprealm_4th.convert.consume", AstralScoreColors.name(source));
    }

    private static Component conversionGain(ScoreType target) {
        return gray("tooltip.deeprealm_4th.convert.gain", AstralScoreColors.name(target));
    }

    private static Component gray(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.GRAY);
    }

    private static Supplier<Item> scoreGem(String id, ScoreType type) {
        return registerItem(id, () -> new AstralFillerItem(new Item.Properties().stacksTo(1), List.of(
                AstralScoreColors.gain(type, 1))));
    }

    private static Supplier<Item> regionalGem(String id, ScoreType base,
            ScoreType firstSource, ScoreType firstTarget,
            ScoreType secondSource, ScoreType secondTarget) {
        String prefix = "tooltip.deeprealm_4th." + id + ".";
        return registerItem(id, () -> new AstralFillerItem(new Item.Properties().stacksTo(1), List.of(
                AstralScoreColors.gain(base, 1),
                Component.translatable(prefix + "combination",
                        AstralScoreColors.gemName(firstSource), AstralScoreColors.gain(firstTarget, 1))
                        .withStyle(ChatFormatting.GRAY),
                Component.translatable(prefix + "combination_extra",
                        AstralScoreColors.gemName(secondSource), AstralScoreColors.gain(secondTarget, 1))
                        .withStyle(ChatFormatting.GRAY))));
    }

    private AstralItemCatalog() {}

    /** Forces item declarations to register before the loader freezes its item registry. */
    public static void initialize() {}

    private static <T extends Item> Supplier<T> registerItem(String id, Supplier<? extends T> factory) {
        return AstralItemRegistration.registerItem(id, factory);
    }
}
