package com.tonywww.deeprealm4th.platform.config;

import com.tonywww.deeprealm4th.astral.container.ContainerLayout;

import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

//? if forge {
import net.minecraftforge.common.ForgeConfigSpec;
//?} else {
/*import net.neoforged.neoforge.common.ModConfigSpec;
*///?}

/** Server-owned layout configuration for the built-in astral body. */
public final class AstralConfig {
    //? if forge {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.IntValue BASE_WIDTH;
    private static final ForgeConfigSpec.IntValue BASE_HEIGHT;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BASE_ROWS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> VOID_ENTRY_DIMENSIONS;
    //?} else {
    /*public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.IntValue BASE_WIDTH;
    private static final ModConfigSpec.IntValue BASE_HEIGHT;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> BASE_ROWS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> VOID_ENTRY_DIMENSIONS;
    *///?}

    static {
        //? if forge {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        //?} else {
        /*ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        *///?}
        BASE_WIDTH = builder.comment("Base astral body grid width; layout rows must match.")
                .defineInRange("base_container.width", ContainerLayout.BASE_WIDTH, 1, 64);
        BASE_HEIGHT = builder.comment("Base astral body grid height; layout row count must match.")
                .defineInRange("base_container.height", ContainerLayout.BASE_HEIGHT, 1, 64);
        BASE_ROWS = builder.comment(
                        "Base astral body layout: height rows, each containing width characters.",
                        "X opens a cell; . closes it. Items in newly closed cells are retained but inactive.")
                .defineList("base_container.layout", ContainerLayout.BASE_ROWS,
                        value -> value instanceof String row && row.length() >= 1 && row.length() <= 64
                                && row.chars().allMatch(ch -> ch == 'X' || ch == '.'));
        VOID_ENTRY_DIMENSIONS = builder.comment(
                        "Dimensions where holding mimetic star slurry and falling into the void opens the Fourth Layer.",
                        "Default: only the End. Use namespaced dimension IDs; an empty list disables this entrance.")
                .defineList("travel.void_entry_dimensions", List.of("minecraft:the_end"),
                        value -> value instanceof String id && id.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+"));
        SPEC = builder.build();
    }

    private AstralConfig() {}

    public static ContainerLayout baseLayout() {
        return ContainerLayout.parse(BASE_WIDTH.get(), BASE_HEIGHT.get(),
                BASE_ROWS.get().stream().map(String::valueOf).toList());
    }

    public static boolean permitsVoidEntry(ResourceKey<Level> dimension) {
        return VOID_ENTRY_DIMENSIONS.get().contains(dimension.location().toString());
    }
}
