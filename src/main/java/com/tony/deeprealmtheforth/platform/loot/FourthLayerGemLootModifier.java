package com.tony.deeprealmtheforth.platform.loot;

import com.tony.deeprealmtheforth.platform.commands.FourthLayerCommands;
import com.tony.deeprealmtheforth.platform.items.AstralItemRegistration;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

//? if forge {
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
//?} else {
/*import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
*///?}

/** Adds gems to selected structure chests only when their loot is opened in the Fourth Layer. */
public final class FourthLayerGemLootModifier implements IGlobalLootModifier {
    //? if forge {
    public static final Codec<FourthLayerGemLootModifier> CODEC = Codec.unit(new FourthLayerGemLootModifier());
    //?} else {
    /*public static final MapCodec<FourthLayerGemLootModifier> CODEC = MapCodec.unit(new FourthLayerGemLootModifier());
    *///?}

    private static final List<Supplier<Item>> BASE_GEMS = List.of(
            AstralItemRegistration.STRENGTH_GEM,
            AstralItemRegistration.AGILITY_GEM,
            AstralItemRegistration.INTELLIGENCE_GEM,
            AstralItemRegistration.CONSTITUTION_GEM,
            AstralItemRegistration.PERCEPTION_GEM,
            AstralItemRegistration.MAGIC_GEM);
    private static final List<Supplier<Item>> DESERT_RARE_GEMS = List.of(
            AstralItemRegistration.FACET_BRIDGE_GEM,
            AstralItemRegistration.TWIN_MIRROR_GEM,
            AstralItemRegistration.BALANCE_SHIFT_GEM,
            AstralItemRegistration.SPLIT_EDGE_GEM,
            AstralItemRegistration.VEIN_AMPLITUDE_GEM,
            AstralItemRegistration.FOLDED_REFLECTION_GEM,
            AstralItemRegistration.CLUSTER_MIRROR_GEM,
            AstralItemRegistration.ETCHED_STEP_GEM,
            AstralItemRegistration.LAST_EDGE_GEM,
            AstralItemRegistration.NIGHTGLOW_GEM);
    private static final List<Supplier<Item>> FORTRESS_RARE_GEMS = List.of(
            AstralItemRegistration.FACET_BRIDGE_GEM,
            AstralItemRegistration.EMBER_REMNANT_GEM,
            AstralItemRegistration.RETURNING_RAY_GEM,
            AstralItemRegistration.SPLIT_EDGE_GEM,
            AstralItemRegistration.VEIN_AMPLITUDE_GEM,
            AstralItemRegistration.LINKED_VEIN_GEM,
            AstralItemRegistration.LOOPED_TRACE_GEM,
            AstralItemRegistration.LAST_EDGE_GEM);
    private static final List<Supplier<Item>> JUNGLE_RARE_GEMS = List.of(
            AstralItemRegistration.WANDERING_STRIPE_GEM,
            AstralItemRegistration.EMBER_REMNANT_GEM,
            AstralItemRegistration.BALANCE_SHIFT_GEM,
            AstralItemRegistration.STEADY_ANCHOR_GEM,
            AstralItemRegistration.WANDERING_SHADOW_GEM,
            AstralItemRegistration.LINKED_VEIN_GEM,
            AstralItemRegistration.CLUSTER_MIRROR_GEM,
            AstralItemRegistration.ETCHED_STEP_GEM,
            AstralItemRegistration.FULL_BREATH_GEM,
            AstralItemRegistration.WELL_FED_GLOW_GEM);
    private static final List<Supplier<Item>> SHIPWRECK_RARE_GEMS = List.of(
            AstralItemRegistration.TWIN_MIRROR_GEM,
            AstralItemRegistration.WANDERING_STRIPE_GEM,
            AstralItemRegistration.RETURNING_RAY_GEM,
            AstralItemRegistration.STEADY_ANCHOR_GEM,
            AstralItemRegistration.WANDERING_SHADOW_GEM,
            AstralItemRegistration.FOLDED_REFLECTION_GEM,
            AstralItemRegistration.LOOPED_TRACE_GEM,
            AstralItemRegistration.FULL_BREATH_GEM,
            AstralItemRegistration.WELL_FED_GLOW_GEM,
            AstralItemRegistration.NIGHTGLOW_GEM);

    private FourthLayerGemLootModifier() {}

    @Override
    public ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!context.getLevel().dimension().equals(FourthLayerCommands.DIMENSION)) return generatedLoot;
        Supplier<Item> themedGem = themedGem(context.getQueriedLootTableId());
        if (themedGem == null) return generatedLoot;

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
        return generatedLoot;
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
            case "chests/desert_pyramid" -> AstralItemRegistration.ARID_RIDGE_GEM;
            case "chests/nether_bridge" -> AstralItemRegistration.MAGMA_VEIN_GEM;
            case "chests/jungle_temple" -> AstralItemRegistration.CANOPY_GEM;
            case "chests/shipwreck_map", "chests/shipwreck_supply", "chests/shipwreck_treasure" ->
                    AstralItemRegistration.TIDAL_GEM;
            default -> null;
        };
    }

    //? if forge {
    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
    //?} else {
    /*@Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
    *///?}
}
