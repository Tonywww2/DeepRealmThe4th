package com.tonywww.deeprealm4th.platform.recipe;

import com.tonywww.deeprealm4th.platform.registry.AstralProcessRegistration;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.Level;

/** Reads both process types from the world's datapack-backed recipe manager. */
public final class AstralProcessLookup {
    public record ForgingEntry(String id, CombinationForgingRecipe recipe) {}
    public record ProjectionEntry(String id, ProjectionRecipe recipe) {}

    private AstralProcessLookup() {}

    public static List<ForgingEntry> forging(Level level) {
        List<ForgingEntry> result = new ArrayList<>();
        //? if forge {
        for (CombinationForgingRecipe recipe : level.getRecipeManager().getAllRecipesFor(AstralProcessRegistration.FORGING_TYPE.get())) {
            result.add(new ForgingEntry(recipe.getId().toString(), recipe));
        }
        //?} else {
        /*for (var holder : level.getRecipeManager().getAllRecipesFor(AstralProcessRegistration.FORGING_TYPE.get())) {
            result.add(new ForgingEntry(holder.id().toString(), holder.value()));
        }
        *///?}
        return result;
    }

    public static List<ProjectionEntry> projection(Level level) {
        List<ProjectionEntry> result = new ArrayList<>();
        //? if forge {
        for (ProjectionRecipe recipe : level.getRecipeManager().getAllRecipesFor(AstralProcessRegistration.PROJECTION_TYPE.get())) {
            result.add(new ProjectionEntry(recipe.getId().toString(), recipe));
        }
        //?} else {
        /*for (var holder : level.getRecipeManager().getAllRecipesFor(AstralProcessRegistration.PROJECTION_TYPE.get())) {
            result.add(new ProjectionEntry(holder.id().toString(), holder.value()));
        }
        *///?}
        return result;
    }
}
