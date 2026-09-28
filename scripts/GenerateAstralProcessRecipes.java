import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Standalone data generator shared by the Forge and NeoForge resource tasks. */
public final class GenerateAstralProcessRecipes {
    private static final String MOD = "deeprealm_4th:";
    private static final String MATERIAL_ON_FRAME = "material_on_frame";
    private static final String FRAME_ON_MATERIAL = "frame_on_material";
    private static Path output;

    private GenerateAstralProcessRecipes() {}

    public static void main(String[] args) throws IOException {
        if (args.length != 2 || !(args[1].equals("recipes") || args[1].equals("recipe"))) {
            throw new IllegalArgumentException("Expected output directory and recipe path name");
        }
        output = Path.of(args[0], "data", "deeprealm_4th", args[1]);
        Files.createDirectories(output);

        projection("astral_lens", step("minecraft:glass_pane", MATERIAL_ON_FRAME),
                step(id("star_slurry"), FRAME_ON_MATERIAL));
        projection("crux_projection", step(id("strength_gem"), MATERIAL_ON_FRAME),
                step(id("perception_gem"), FRAME_ON_MATERIAL), step(id("astral_lens"), MATERIAL_ON_FRAME));
        projection("telescopium_projection", step(id("magic_gem"), MATERIAL_ON_FRAME),
                step(id("perception_gem"), MATERIAL_ON_FRAME), step(id("intelligence_gem"), FRAME_ON_MATERIAL),
                step(id("astral_lens"), FRAME_ON_MATERIAL));
        projection("triangulum_australe_projection", step(id("constitution_gem"), FRAME_ON_MATERIAL),
                step(id("agility_gem"), MATERIAL_ON_FRAME), step(id("magic_gem"), FRAME_ON_MATERIAL),
                step(id("astral_lens"), MATERIAL_ON_FRAME));
        projection("convergent_facet_core", step(id("crux_fragment"), FRAME_ON_MATERIAL),
                step(id("stabilized_star_slurry"), MATERIAL_ON_FRAME), step(id("strength_gem"), FRAME_ON_MATERIAL));
        projection("gathered_radiance_core", step(id("telescopium_fragment"), MATERIAL_ON_FRAME),
                step(id("stabilized_star_slurry"), FRAME_ON_MATERIAL), step(id("magic_gem"), MATERIAL_ON_FRAME));
        projection("balance_core", step(id("triangulum_australe_fragment"), FRAME_ON_MATERIAL),
                step(id("stabilized_star_slurry"), FRAME_ON_MATERIAL), step(id("constitution_gem"), MATERIAL_ON_FRAME));
        projection("reflected_radiance_core", step(id("telescopium_fragment"), MATERIAL_ON_FRAME),
                step(id("stabilized_star_slurry"), MATERIAL_ON_FRAME), step(id("astral_lens"), FRAME_ON_MATERIAL));
        projection("sixfold_balance_core", step(id("crux_fragment"), MATERIAL_ON_FRAME),
                step(id("telescopium_fragment"), FRAME_ON_MATERIAL),
                step(id("triangulum_australe_fragment"), MATERIAL_ON_FRAME),
                step(id("stabilized_star_slurry"), FRAME_ON_MATERIAL));

        forging("stabilized_star_slurry", "star_slurry_blank", 2,
                "minecraft:amethyst_shard", id("star_slurry"));
        forging("crux_fragment", "crux_projection", 3,
                id("strength_gem"), id("perception_gem"), id("stabilized_star_slurry"));
        forging("telescopium_fragment", "telescopium_projection", 4,
                id("magic_gem"), id("perception_gem"), id("intelligence_gem"), id("stabilized_star_slurry"));
        forging("triangulum_australe_fragment", "triangulum_australe_projection", 5,
                id("constitution_gem"), id("agility_gem"), id("magic_gem"),
                id("star_slurry"), id("stabilized_star_slurry"));
        forging("etched_step_core", "unfinished_etched_blank", 4,
                id("intelligence_gem"), id("etched_step_gem"), "minecraft:amethyst_shard",
                id("stabilized_star_slurry"));
        forging("full_breath_core", "unfinished_breath_blank", 4,
                id("constitution_gem"), id("full_breath_gem"), "minecraft:amethyst_shard",
                id("stabilized_star_slurry"));
    }

    private record Step(String item, String direction) {}
    private static Step step(String item, String direction) { return new Step(item, direction); }
    private static String id(String path) { return MOD + path; }

    private static void projection(String name, Step... steps) throws IOException {
        StringBuilder json = new StringBuilder("{\n  \"type\": \"deeprealm_4th:projection_combining\",\n")
                .append("  \"result\": \"").append(id(name)).append("\",\n  \"steps\": [\n");
        for (int i = 0; i < steps.length; i++) {
            Step step = steps[i];
            json.append("    {\"item\": \"").append(step.item()).append("\", \"direction\": \"")
                    .append(step.direction()).append("\"}")
                    .append(i + 1 == steps.length ? "\n" : ",\n");
        }
        json.append("  ]\n}\n");
        write(name, json.toString());
    }

    private static void forging(String name, String base, int clicks, String... materials) throws IOException {
        Map<String, Integer> ingredients = new LinkedHashMap<>();
        ingredients.merge(id(base), 1, Integer::sum);
        for (String material : materials) ingredients.merge(material, 1, Integer::sum);
        StringBuilder json = new StringBuilder("{\n  \"type\": \"deeprealm_4th:combination_forging\",\n")
                .append("  \"result\": \"").append(id(name)).append("\",\n  \"ingredients\": [\n");
        int index = 0;
        for (var entry : ingredients.entrySet()) {
            json.append("    {\"item\": \"").append(entry.getKey()).append("\", \"count\": ")
                    .append(entry.getValue()).append("}")
                    .append(++index == ingredients.size() ? "\n" : ",\n");
        }
        json.append("  ],\n  \"clicks\": ").append(clicks)
                .append(",\n  \"cooldown\": 10,\n  \"levels\": ")
                .append((1 << clicks) - 1).append("\n}\n");
        write(name, json.toString());
    }

    private static void write(String name, String content) throws IOException {
        Files.writeString(output.resolve(name + ".json"), content, StandardCharsets.UTF_8);
    }
}
