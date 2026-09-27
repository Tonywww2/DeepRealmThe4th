package com.tony.deeprealmtheforth.astral;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** One palette for every score shown by the astral body and its fillers. */
public final class AstralScoreColors {
    private AstralScoreColors() {}

    public static int rgb(ScoreType type) {
        return switch (type) {
            case STRENGTH -> 0xe76868;
            case AGILITY -> 0x55cbb3;
            case INTELLIGENCE -> 0x71a9ed;
            case CONSTITUTION -> 0xe6b85c;
            case PERCEPTION -> 0xbca1ec;
            case MAGIC -> 0xe182d9;
        };
    }

    public static int argb(ScoreType type) {
        return 0xff000000 | rgb(type);
    }

    public static MutableComponent name(ScoreType type) {
        return color(Component.translatable("score.deeprealm_4th." + type.id()), type);
    }

    public static MutableComponent gemName(ScoreType type) {
        return color(Component.translatable("item.deeprealm_4th." + type.id() + "_gem"), type);
    }

    public static MutableComponent gain(ScoreType type, double amount) {
        return color(Component.translatable("tooltip.deeprealm_4th.score_gain",
                name(type), (amount > 0 ? "+" : "") + AstralTooltips.number(amount)), type);
    }

    public static MutableComponent color(MutableComponent component, ScoreType type) {
        return component.withStyle(style -> style.withColor(rgb(type)));
    }
}
