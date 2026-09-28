package com.tonywww.deeprealm4th.astral.container;

import com.tonywww.deeprealm4th.item.AstralContainerItem;
import com.tonywww.deeprealm4th.astral.score.AstralCurioAttributes;
import com.tonywww.deeprealm4th.platform.data.AstralStoredItems;
import com.tonywww.deeprealm4th.astral.ScoreType;

import com.tonywww.deeprealm4th.astral.score.AstralAttributeIds;
import com.tonywww.deeprealm4th.astral.score.AstralAttributeKey;
import com.tonywww.deeprealm4th.astral.score.AstralAttributeOperation;
import com.tonywww.deeprealm4th.astral.score.AstralScoreColors;
import com.tonywww.deeprealm4th.astral.score.AstralScoreEngine;
import com.tonywww.deeprealm4th.astral.score.ScoreSheet;
import com.tonywww.deeprealm4th.astral.tooltip.AstralTooltips;

import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Small client-only preview for the container item's own tooltip. */
public final class AstralClientPreview {
    private AstralClientPreview() {}

    public static void appendContainer(ItemStack body, AstralContainerItem item, List<Component> tooltip) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ContainerLayout layout = item.layout(body);
        ScoreSheet serverScores = AstralCurioAttributes.scoreSnapshot(body);
        AstralScoreEngine.Result result = serverScores == null
                ? AstralScoreEngine.calculate(player, body, layout,
                        AstralStoredItems.read(body, layout.size())) : null;
        ScoreSheet scores = serverScores == null ? result.scores() : serverScores;
        for (ScoreType type : ScoreType.values()) {
            double value = scores.get(type);
            if (value == 0) continue;
            tooltip.add(AstralScoreColors.color(Component.translatable("tooltip.deeprealm_4th.base_container.score",
                    AstralScoreColors.name(type),
                    (value > 0 ? "+" : "") + AstralTooltips.number(value)), type));
        }
        Map<AstralAttributeKey, Double> bonuses = serverScores == null ? result.attributeBonuses()
                : AstralCurioAttributes.attributeSnapshot(body);
        double attack = bonuses.getOrDefault(new AstralAttributeKey(
                AstralAttributeIds.ATTACK_DAMAGE, AstralAttributeOperation.MULTIPLY_TOTAL), 0.0);
        if (attack != 0) {
            tooltip.add(Component.translatable("tooltip.deeprealm_4th.base_container.attack",
                    (attack > 0 ? "+" : "") + AstralTooltips.number(attack * 100))
                    .withStyle(ChatFormatting.GOLD));
        }
        double movement = bonuses.getOrDefault(new AstralAttributeKey(
                AstralAttributeIds.MOVEMENT_SPEED, AstralAttributeOperation.MULTIPLY_TOTAL), 0.0);
        if (movement != 0) {
            tooltip.add(Component.translatable("tooltip.deeprealm_4th.base_container.movement",
                    (movement > 0 ? "+" : "") + AstralTooltips.number(movement * 100))
                    .withStyle(ChatFormatting.GOLD));
        }
        double health = bonuses.getOrDefault(new AstralAttributeKey(
                AstralAttributeIds.MAX_HEALTH, AstralAttributeOperation.ADD), 0.0);
        if (health != 0) {
            tooltip.add(Component.translatable("tooltip.deeprealm_4th.base_container.health",
                    (health > 0 ? "+" : "") + AstralTooltips.number(health))
                    .withStyle(ChatFormatting.GOLD));
        }
    }
}
