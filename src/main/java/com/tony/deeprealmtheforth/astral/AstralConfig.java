package com.tony.deeprealmtheforth.astral;

import java.util.List;

//? if forge {
import net.minecraftforge.common.ForgeConfigSpec;
//?} else {
/*import net.neoforged.neoforge.common.ModConfigSpec;
*///?}

/** Server-owned layout configuration for the built-in astral body. */
public final class AstralConfig {
    //? if forge {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BASE_ROWS;
    //?} else {
    /*public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> BASE_ROWS;
    *///?}

    static {
        //? if forge {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        //?} else {
        /*ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        *///?}
        BASE_ROWS = builder.comment(
                        "Base astral body layout: exactly 9 rows of 12 characters.",
                        "X opens a cell; . closes it. Items in newly closed cells are retained but inactive.")
                .defineList("base_container.layout", ContainerLayout.BASE_ROWS,
                        value -> value instanceof String row && row.length() == ContainerLayout.BASE_WIDTH
                                && row.chars().allMatch(ch -> ch == 'X' || ch == '.'));
        SPEC = builder.build();
    }

    private AstralConfig() {}

    public static ContainerLayout baseLayout() {
        return ContainerLayout.parse(ContainerLayout.BASE_WIDTH, ContainerLayout.BASE_HEIGHT,
                BASE_ROWS.get().stream().map(String::valueOf).toList());
    }
}
