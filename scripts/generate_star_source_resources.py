"""Generate block models, six-way states and loot for the star-source growth family."""

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
ASSETS = ROOT / "assets/deeprealm_4th"
STAGES = (
    "small_star_slurry_bud",
    "medium_star_slurry_bud",
    "large_star_slurry_bud",
    "star_slurry_cluster",
)


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main():
    write(ASSETS / "blockstates/star_source.json", {"variants": {"": {"model": "deeprealm_4th:block/star_source"}}})
    write(ASSETS / "models/block/star_source.json", {
        "parent": "minecraft:block/cube_all", "textures": {"all": "deeprealm_4th:block/star_source"}})
    write(ASSETS / "models/item/star_source.json", {"parent": "deeprealm_4th:block/star_source"})
    for name in STAGES:
        model = "deeprealm_4th:block/" + name
        variants = {}
        for direction, x, y in (("down", 180, 0), ("up", 0, 0), ("north", 90, 0),
                                ("east", 90, 90), ("south", 90, 180), ("west", 90, 270)):
            variants["facing=" + direction] = {"model": model}
            if x:
                variants["facing=" + direction]["x"] = x
            if y:
                variants["facing=" + direction]["y"] = y
        write(ASSETS / f"blockstates/{name}.json", {"variants": variants})
        # Vanilla-sized cross planes preserve one texture pixel per model unit.
        # Each stage's silhouette is drawn directly on its own 16x16 canvas.
        write(ASSETS / f"models/block/{name}.json", {
            "parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
            "textures": {"cross": "deeprealm_4th:block/" + name},
        })
        write(ASSETS / f"models/item/{name}.json", {
            "parent": "minecraft:item/generated", "textures": {"layer0": "deeprealm_4th:block/" + name},
            "display": {"head": {"translation": [0, 14, -5]}},
        })
    write(ASSETS / "models/item/star_slurry_crystal.json", {
        "parent": "minecraft:item/generated", "textures": {"layer0": "deeprealm_4th:item/star_slurry_crystal"}})
    for folder in ("loot_tables", "loot_table"):
        for name in ("star_source", *STAGES):
            pools = []
            if name == "star_slurry_cluster":
                pools = [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "deeprealm_4th:star_slurry_crystal"}],
                          "conditions": [{"condition": "minecraft:survives_explosion"}]}]
            write(ROOT / f"data/deeprealm_4th/{folder}/blocks/{name}.json", {"type": "minecraft:block", "pools": pools})
    for folder in ("blocks", "block"):
        write(ROOT / f"data/minecraft/tags/{folder}/mineable/pickaxe.json", {
            "replace": False, "values": ["deeprealm_4th:star_source", *["deeprealm_4th:" + name for name in STAGES]]})
    print("Generated star-source block resources for both Minecraft versions")


if __name__ == "__main__":
    main()
