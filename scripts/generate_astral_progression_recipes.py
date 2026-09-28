"""Generate ordinary crafting recipes; custom process recipes use the Gradle Java datagen task."""

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/data/deeprealm_4th"
MOD = "deeprealm_4th:"


def write(name, value):
    for directory in ("recipes", "recipe"):
        path = ROOT / directory / f"{name}.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        data = dict(value)
        if data["type"].startswith("minecraft:crafting"):
            data["result"] = {"item" if directory == "recipes" else "id": value["result"]}
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n",
                        encoding="utf-8", newline="\n")


def shapeless(name, output, *inputs):
    write(name, {"type": "minecraft:crafting_shapeless",
                 "ingredients": [{"item": item} for item in inputs], "result": output})


def shaped(name, output, pattern, key):
    write(name, {"type": "minecraft:crafting_shaped", "pattern": pattern,
                 "key": {letter: {"item": item} for letter, item in key.items()}, "result": output})


shaped("projection_frame", MOD + "projection_frame", ["GCG", "CAC", "GCG"],
       {"G": "minecraft:glass_pane", "C": "minecraft:copper_ingot", "A": "minecraft:amethyst_shard"})
shaped("forging_pad", MOD + "forging_pad", ["LSL", "LSL"],
       {"L": "minecraft:leather", "S": "minecraft:string"})
shapeless("star_slurry_blank", MOD + "star_slurry_blank", MOD + "star_slurry", "minecraft:amethyst_shard")
shapeless("unfinished_etched_blank", MOD + "unfinished_etched_blank",
          MOD + "etched_step_gem", MOD + "stabilized_star_slurry")
shapeless("unfinished_breath_blank", MOD + "unfinished_breath_blank",
          MOD + "full_breath_gem", MOD + "stabilized_star_slurry")
shapeless("mimetic_star_slurry", MOD + "mimetic_star_slurry",
          "minecraft:chorus_fruit", "minecraft:dragon_breath", "minecraft:nether_star",
          "minecraft:amethyst_shard")
shaped("base_container", MOD + "base_container", ["AGA", "GMG", "AGA"],
       {"A": "minecraft:amethyst_shard", "G": "minecraft:gold_ingot", "M": MOD + "mimetic_star_slurry"})

for name, projection_id, blank, base in [
    ("convergent_facet_gem", "crux_projection", "star_slurry_blank", "strength_gem"),
    ("gathered_radiance_gem", "telescopium_projection", "star_slurry_blank", "magic_gem"),
    ("balance_crystal_gem", "triangulum_australe_projection", "star_slurry_blank", "constitution_gem"),
]:
    shapeless(name, MOD + name, MOD + projection_id, MOD + blank, MOD + base)

write("frame_disassembly", {"type": MOD + "frame_disassembly"})

print("Generated ordinary astral crafting recipes for both loaders")
