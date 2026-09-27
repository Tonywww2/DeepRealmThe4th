package com.tony.deeprealmtheforth.platform.items;

import com.tony.deeprealmtheforth.DeepRealmTheForth;
import com.tony.deeprealmtheforth.astral.AstralContainerItem;
import com.tony.deeprealmtheforth.astral.AstralFillerItem;
import com.tony.deeprealmtheforth.astral.AstralScoreColors;
import com.tony.deeprealmtheforth.astral.ScoreType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

//? if forge {
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
//?} else {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?}

public final class AstralItemRegistration {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, DeepRealmTheForth.MOD_ID);

    public static final Supplier<AstralContainerItem> BASE_CONTAINER =
            ITEMS.register("base_container", () -> new AstralContainerItem(new Item.Properties()));
    public static final Supplier<Item> WARRIOR_MEDAL =
            ITEMS.register("warrior_medal", () -> new AstralFillerItem(new Item.Properties().stacksTo(1), List.of(
                    Component.translatable("tooltip.deeprealm_4th.warrior_medal.effect",
                            AstralScoreColors.name(ScoreType.STRENGTH)).withStyle(ChatFormatting.GRAY),
                    gray("tooltip.deeprealm_4th.common.medal_origin"))));
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

    public static final Supplier<Item> FACET_BRIDGE_GEM = gem("facet_bridge_gem", "desert_fortress",
            AstralScoreColors.gain(ScoreType.STRENGTH, 1),
            adjacentGain(ScoreType.CONSTITUTION, ScoreType.PERCEPTION, 1));
    public static final Supplier<Item> TWIN_MIRROR_GEM = gem("twin_mirror_gem", "desert_shipwreck",
            AstralScoreColors.gain(ScoreType.INTELLIGENCE, 1),
            adjacentGain(ScoreType.MAGIC, ScoreType.INTELLIGENCE, 1));
    public static final Supplier<Item> WANDERING_STRIPE_GEM = gem("wandering_stripe_gem", "jungle_shipwreck",
            AstralScoreColors.gain(ScoreType.AGILITY, 1),
            countGain(ScoreType.PERCEPTION, 2, ScoreType.MAGIC, 1));
    public static final Supplier<Item> EMBER_REMNANT_GEM = gem("ember_remnant_gem", "fortress_jungle",
            AstralScoreColors.gain(ScoreType.CONSTITUTION, 1),
            countGain(ScoreType.STRENGTH, 2, ScoreType.STRENGTH, 1));
    public static final Supplier<Item> BALANCE_SHIFT_GEM = gem("balance_shift_gem", "desert_jungle",
            conversionConsume(ScoreType.STRENGTH), conversionGain(ScoreType.CONSTITUTION));
    public static final Supplier<Item> RETURNING_RAY_GEM = gem("returning_ray_gem", "fortress_shipwreck",
            conversionConsume(ScoreType.MAGIC), conversionGain(ScoreType.INTELLIGENCE));
    public static final Supplier<Item> SPLIT_EDGE_GEM = gem("split_edge_gem", "desert_fortress",
            AstralScoreColors.gain(ScoreType.STRENGTH, 2),
            AstralScoreColors.gain(ScoreType.CONSTITUTION, -1));
    public static final Supplier<Item> STEADY_ANCHOR_GEM = gem("steady_anchor_gem", "jungle_shipwreck",
            AstralScoreColors.gain(ScoreType.CONSTITUTION, 2),
            AstralScoreColors.gain(ScoreType.AGILITY, -1));
    public static final Supplier<Item> WAYFARER_MEDAL = ITEMS.register("wayfarer_medal",
            () -> new AstralFillerItem(new Item.Properties().stacksTo(1), List.of(
                    gray("tooltip.deeprealm_4th.wayfarer_medal.effect",
                            AstralScoreColors.name(ScoreType.AGILITY)),
                    gray("tooltip.deeprealm_4th.common.medal_origin"))));
    public static final Supplier<Item> WARDEN_MEDAL = ITEMS.register("warden_medal",
            () -> new AstralFillerItem(new Item.Properties().stacksTo(1), List.of(
                    gray("tooltip.deeprealm_4th.warden_medal.effect",
                            AstralScoreColors.name(ScoreType.CONSTITUTION)),
                    gray("tooltip.deeprealm_4th.common.medal_origin"))));

    public static final Supplier<Item> VEIN_AMPLITUDE_GEM = percentGem("vein_amplitude_gem",
            "desert_fortress", ScoreType.STRENGTH, ScoreType.STRENGTH, 20);
    public static final Supplier<Item> WANDERING_SHADOW_GEM = percentGem("wandering_shadow_gem",
            "jungle_shipwreck", ScoreType.AGILITY, ScoreType.PERCEPTION, 25);
    public static final Supplier<Item> FOLDED_REFLECTION_GEM = percentGem("folded_reflection_gem",
            "desert_shipwreck", ScoreType.INTELLIGENCE, ScoreType.MAGIC, 25);
    public static final Supplier<Item> LINKED_VEIN_GEM = gem("linked_vein_gem", "fortress_jungle",
            gray("tooltip.deeprealm_4th.percent_adjacent",
                    AstralScoreColors.gemName(ScoreType.CONSTITUTION),
                    AstralScoreColors.name(ScoreType.CONSTITUTION), 30,
                    AstralScoreColors.name(ScoreType.STRENGTH), 4));
    public static final Supplier<Item> CLUSTER_MIRROR_GEM = gem("cluster_mirror_gem", "desert_jungle",
            gray("tooltip.deeprealm_4th.percent_count",
                    AstralScoreColors.gemName(ScoreType.PERCEPTION), 2,
                    AstralScoreColors.name(ScoreType.PERCEPTION), 30,
                    AstralScoreColors.name(ScoreType.INTELLIGENCE), 4));
    public static final Supplier<Item> LOOPED_TRACE_GEM = gem("looped_trace_gem", "fortress_shipwreck",
            gray("tooltip.deeprealm_4th.percent_adjacent",
                    AstralScoreColors.gemName(ScoreType.MAGIC),
                    AstralScoreColors.name(ScoreType.MAGIC), 30,
                    AstralScoreColors.name(ScoreType.MAGIC), 4));
    public static final Supplier<Item> ETCHED_STEP_GEM = conditionGem("etched_step_gem", "desert_jungle",
            "level_above_30", ScoreType.INTELLIGENCE, 2);
    public static final Supplier<Item> FULL_BREATH_GEM = conditionGem("full_breath_gem", "jungle_shipwreck",
            "health_above_16", ScoreType.CONSTITUTION, 2);
    public static final Supplier<Item> LAST_EDGE_GEM = conditionGem("last_edge_gem", "desert_fortress",
            "health_at_most_8", ScoreType.STRENGTH, 3);
    public static final Supplier<Item> WELL_FED_GLOW_GEM = conditionGem("well_fed_glow_gem", "jungle_shipwreck",
            "food_at_least_18", ScoreType.AGILITY, 2);
    public static final Supplier<Item> NIGHTGLOW_GEM = conditionGem("nightglow_gem", "desert_shipwreck",
            "night_vision", ScoreType.PERCEPTION, 2);

    private static Supplier<Item> gem(String id, String source, Component... effects) {
        List<Component> lines = new ArrayList<>(List.of(effects));
        lines.add(gray("tooltip.deeprealm_4th.source." + source));
        return ITEMS.register(id, () -> new AstralFillerItem(new Item.Properties().stacksTo(1), lines));
    }

    private static Supplier<Item> percentGem(String id, String source,
            ScoreType input, ScoreType output, int rate) {
        return gem(id, source, gray("tooltip.deeprealm_4th.percent",
                AstralScoreColors.name(input), rate, AstralScoreColors.name(output), 4));
    }

    private static Supplier<Item> conditionGem(String id, String source,
            String condition, ScoreType target, int amount) {
        return gem(id, source, gray("tooltip.deeprealm_4th.condition." + condition,
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
        return ITEMS.register(id, () -> new AstralFillerItem(new Item.Properties().stacksTo(1), List.of(
                AstralScoreColors.gain(type, 1),
                Component.translatable("tooltip.deeprealm_4th.common.gem_origin")
                        .withStyle(ChatFormatting.GRAY))));
    }

    private static Supplier<Item> regionalGem(String id, ScoreType base,
            ScoreType firstSource, ScoreType firstTarget,
            ScoreType secondSource, ScoreType secondTarget) {
        String prefix = "tooltip.deeprealm_4th." + id + ".";
        return ITEMS.register(id, () -> new AstralFillerItem(new Item.Properties().stacksTo(1), List.of(
                AstralScoreColors.gain(base, 1),
                Component.translatable(prefix + "combination",
                        AstralScoreColors.gemName(firstSource), AstralScoreColors.gain(firstTarget, 1))
                        .withStyle(ChatFormatting.GRAY),
                Component.translatable(prefix + "combination_extra",
                        AstralScoreColors.gemName(secondSource), AstralScoreColors.gain(secondTarget, 1))
                        .withStyle(ChatFormatting.GRAY),
                Component.translatable(prefix + "origin").withStyle(ChatFormatting.GRAY))));
    }

    private AstralItemRegistration() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
