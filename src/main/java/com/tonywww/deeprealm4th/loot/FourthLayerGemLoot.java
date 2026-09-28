package com.tonywww.deeprealm4th.loot;

import com.tonywww.deeprealm4th.worldgen.FourthLayerDimension;
import com.tonywww.deeprealm4th.astral.content.AstralItemCatalog;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

/** Gem pools and rolls shared by both loader loot modifier adapters. */
public final class FourthLayerGemLoot {
    private static final List<Supplier<Item>> BASE_GEMS = List.of(
            AstralItemCatalog.STRENGTH_GEM,
            AstralItemCatalog.AGILITY_GEM,
            AstralItemCatalog.INTELLIGENCE_GEM,
            AstralItemCatalog.CONSTITUTION_GEM,
            AstralItemCatalog.PERCEPTION_GEM,
            AstralItemCatalog.MAGIC_GEM);
    private static final List<Supplier<Item>> DESERT_RARE_GEMS = List.of(
            AstralItemCatalog.FACET_BRIDGE_GEM,
            AstralItemCatalog.TWIN_MIRROR_GEM,
            AstralItemCatalog.BALANCE_SHIFT_GEM,
            AstralItemCatalog.SPLIT_EDGE_GEM,
            AstralItemCatalog.VEIN_AMPLITUDE_GEM,
            AstralItemCatalog.FOLDED_REFLECTION_GEM,
            AstralItemCatalog.CLUSTER_MIRROR_GEM,
            AstralItemCatalog.ETCHED_STEP_GEM,
            AstralItemCatalog.LAST_EDGE_GEM,
            AstralItemCatalog.NIGHTGLOW_GEM);
    private static final List<Supplier<Item>> FORTRESS_RARE_GEMS = List.of(
            AstralItemCatalog.FACET_BRIDGE_GEM,
            AstralItemCatalog.EMBER_REMNANT_GEM,
            AstralItemCatalog.RETURNING_RAY_GEM,
            AstralItemCatalog.SPLIT_EDGE_GEM,
            AstralItemCatalog.VEIN_AMPLITUDE_GEM,
            AstralItemCatalog.LINKED_VEIN_GEM,
            AstralItemCatalog.LOOPED_TRACE_GEM,
            AstralItemCatalog.LAST_EDGE_GEM);
    private static final List<Supplier<Item>> JUNGLE_RARE_GEMS = List.of(
            AstralItemCatalog.WANDERING_STRIPE_GEM,
            AstralItemCatalog.EMBER_REMNANT_GEM,
            AstralItemCatalog.BALANCE_SHIFT_GEM,
            AstralItemCatalog.STEADY_ANCHOR_GEM,
            AstralItemCatalog.WANDERING_SHADOW_GEM,
            AstralItemCatalog.LINKED_VEIN_GEM,
            AstralItemCatalog.CLUSTER_MIRROR_GEM,
            AstralItemCatalog.ETCHED_STEP_GEM,
            AstralItemCatalog.FULL_BREATH_GEM,
            AstralItemCatalog.WELL_FED_GLOW_GEM);
    private static final List<Supplier<Item>> SHIPWRECK_RARE_GEMS = List.of(
            AstralItemCatalog.TWIN_MIRROR_GEM,
            AstralItemCatalog.WANDERING_STRIPE_GEM,
            AstralItemCatalog.RETURNING_RAY_GEM,
            AstralItemCatalog.STEADY_ANCHOR_GEM,
            AstralItemCatalog.WANDERING_SHADOW_GEM,
            AstralItemCatalog.FOLDED_REFLECTION_GEM,
            AstralItemCatalog.LOOPED_TRACE_GEM,
            AstralItemCatalog.FULL_BREATH_GEM,
            AstralItemCatalog.WELL_FED_GLOW_GEM,
            AstralItemCatalog.NIGHTGLOW_GEM);

    private FourthLayerGemLoot() {}

    public static void add(List<ItemStack> generatedLoot, LootContext context) {
        if (!context.getLevel().dimension().equals(FourthLayerDimension.KEY)) return;
        Supplier<Item> themedGem = themedGem(context.getQueriedLootTableId());
        if (themedGem == null) return;

        // Each eligible chest rolls independently for a base gem, its themed gem, and one rare gem.
        if (context.getRandom().nextInt(3) == 0) {
            generatedLoot.add(new ItemStack(BASE_GEMS.get(context.getRandom().nextInt(BASE_GEMS.size())).get()));
        }
        if (context.getRandom().nextInt(9) == 0) {
            generatedLoot.add(new ItemStack(themedGem.get()));
        }
        // Both expansion batches share one roll, so adding content does not stack extra rolls.
        if (context.getRandom().nextInt(4) == 0) {
            List<Supplier<Item>> rare = rareGems(context.getQueriedLootTableId());
            generatedLoot.add(new ItemStack(rare.get(context.getRandom().nextInt(rare.size())).get()));
        }
        return;
    }

    private static List<Supplier<Item>> rareGems(ResourceLocation lootTable) {
        return switch (lootTable.getPath()) {
            case "chests/desert_pyramid" -> DESERT_RARE_GEMS;
            case "chests/nether_bridge" -> FORTRESS_RARE_GEMS;
            case "chests/jungle_temple" -> JUNGLE_RARE_GEMS;
            case "chests/shipwreck_map", "chests/shipwreck_supply", "chests/shipwreck_treasure" ->
                    SHIPWRECK_RARE_GEMS;
            default -> List.of();
        };
    }

    private static Supplier<Item> themedGem(ResourceLocation lootTable) {
        if (!"minecraft".equals(lootTable.getNamespace())) return null;
        return switch (lootTable.getPath()) {
            case "chests/desert_pyramid" -> AstralItemCatalog.ARID_RIDGE_GEM;
            case "chests/nether_bridge" -> AstralItemCatalog.MAGMA_VEIN_GEM;
            case "chests/jungle_temple" -> AstralItemCatalog.CANOPY_GEM;
            case "chests/shipwreck_map", "chests/shipwreck_supply", "chests/shipwreck_treasure" ->
                    AstralItemCatalog.TIDAL_GEM;
            default -> null;
        };
    }

}
