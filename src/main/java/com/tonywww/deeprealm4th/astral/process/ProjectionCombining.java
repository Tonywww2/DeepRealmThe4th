package com.tonywww.deeprealm4th.astral.process;

import com.tonywww.deeprealm4th.platform.data.ProjectionFrameData;
import com.tonywww.deeprealm4th.platform.recipe.AstralProcessLookup;
import com.tonywww.deeprealm4th.platform.recipe.ProjectionRecipe;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Server-authoritative next-step matching for both inventory click directions. */
public final class ProjectionCombining {
    public record Result(boolean accepted, boolean completed, ItemStack frameOrOutput) {
        static Result rejected() { return new Result(false, false, ItemStack.EMPTY); }
    }

    private ProjectionCombining() {}

    public static Result apply(Level level, ItemStack frame, ItemStack material,
            ProjectionRecipe.Direction direction) {
        if (frame.isEmpty() || material.isEmpty()) return Result.rejected();
        List<ProjectionFrameData.Absorbed> absorbed = ProjectionFrameData.read(frame);
        if (absorbed.size() >= 9) return Result.rejected();
        String materialId = BuiltInRegistries.ITEM.getKey(material.getItem()).toString();
        List<ProjectionFrameData.Absorbed> next = new ArrayList<>(absorbed);
        next.add(new ProjectionFrameData.Absorbed(material.copyWithCount(1), direction));
        var steps = ProjectionFrameData.steps(next);
        int complete = 0, unfinished = 0;
        ItemStack product = ItemStack.EMPTY;
        for (var entry : AstralProcessLookup.projection(level)) {
            ProjectionRecipe recipe = entry.recipe();
            if (!recipe.matchesPrefix(steps)) continue;
            if (recipe.steps().size() == next.size()) {
                complete++;
                product = recipe.output();
            } else unfinished++;
        }
        if (complete + unfinished == 0 || complete > 1 || (complete == 1 && unfinished > 0)) {
            return Result.rejected();
        }
        if (complete == 1) return new Result(!product.isEmpty(), true, product);
        return new Result(true, false, ProjectionFrameData.write(frame, next));
    }
}
