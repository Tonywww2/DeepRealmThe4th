package com.tonywww.deeprealm4th.astral.process;

import com.tonywww.deeprealm4th.platform.data.ProjectionFrameData;
import com.tonywww.deeprealm4th.platform.recipe.AstralProcessLookup;
import com.tonywww.deeprealm4th.platform.recipe.ProjectionRecipe;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;

/** Server-authoritative next-step matching for both inventory click directions. */
public final class ProjectionCombining {
    public record Result(boolean accepted, boolean completed, ItemStack frameOrOutput, List<ItemStack> returned) {
        static Result rejected() { return new Result(false, false, ItemStack.EMPTY, List.of()); }
    }

    private ProjectionCombining() {}

    public static Result apply(Player player, ItemStack frame, ItemStack material,
            ProjectionRecipe.Direction direction) {
        if (frame.isEmpty() || material.isEmpty()) return Result.rejected();
        List<ProjectionFrameData.Absorbed> absorbed = ProjectionFrameData.read(frame);
        if (absorbed.size() >= 9) return Result.rejected();
        List<ProjectionFrameData.Absorbed> next = new ArrayList<>(absorbed);
        next.add(new ProjectionFrameData.Absorbed(material.copyWithCount(1), direction));
        var steps = ProjectionFrameData.steps(next);
        int complete = 0, unfinished = 0;
        AstralProcessLookup.ProjectionEntry completedEntry = null;
        for (var entry : AstralProcessLookup.projection(player.level())) {
            ProjectionRecipe recipe = entry.recipe();
            if (!recipe.matchesPrefix(steps)) continue;
            if (recipe.steps().size() == next.size()) {
                complete++;
                completedEntry = entry;
            } else unfinished++;
        }
        if (complete + unfinished == 0 || complete > 1 || (complete == 1 && unfinished > 0)) {
            return Result.rejected();
        }
        if (complete == 1) {
            ItemStack beforeFrame = frame.copy();
            ItemStack beforeMaterial = material.copy();
            var outcome = completedEntry.recipe().produce(player, completedEntry.id(), next);
            if (!outcome.ok() || !ItemStack.matches(beforeFrame, frame)
                    || !ItemStack.matches(beforeMaterial, material)) return Result.rejected();
            AstralTransforms.ProcessOutput produced = outcome.value();
            return new Result(!produced.result().isEmpty(), true, produced.result(), produced.returned());
        }
        return new Result(true, false, ProjectionFrameData.write(frame, next), List.of());
    }
}
