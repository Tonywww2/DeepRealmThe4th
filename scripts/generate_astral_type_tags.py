"""Keep Forge and NeoForge item-tag folder variants in sync."""

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/data/deeprealm_4th/tags"
NAMESPACE = "deeprealm_4th:"

CATEGORIES = {
    "base_gems": "strength_gem agility_gem intelligence_gem constitution_gem perception_gem magic_gem",
    "combination_gems": "arid_ridge_gem magma_vein_gem canopy_gem tidal_gem facet_bridge_gem twin_mirror_gem wandering_stripe_gem ember_remnant_gem",
    "conversion_gems": "balance_shift_gem returning_ray_gem",
    "tradeoff_gems": "split_edge_gem steady_anchor_gem",
    "percentage_gems": "vein_amplitude_gem wandering_shadow_gem folded_reflection_gem linked_vein_gem cluster_mirror_gem looped_trace_gem",
    "player_condition_gems": "etched_step_gem full_breath_gem last_edge_gem well_fed_glow_gem nightglow_gem",
    "advanced_gems": "convergent_facet_gem gathered_radiance_gem balance_crystal_gem etched_step_core full_breath_core convergent_facet_core gathered_radiance_core balance_core reflected_radiance_core sixfold_balance_core",
    "medals": "warrior_medal wayfarer_medal warden_medal",
    "containers": "base_container",
}


def main() -> None:
    tags = {
        category: [NAMESPACE + item for item in names.split()]
        for category, names in CATEGORIES.items()
    }
    tags["gems"] = ["#deeprealm_4th:astral/" + category for category in CATEGORIES
                    if category.endswith("_gems")]
    tags["fillers"] = ["#deeprealm_4th:astral/gems", "#deeprealm_4th:astral/medals"]
    for folder in ("items", "item"):
        directory = ROOT / folder / "astral"
        directory.mkdir(parents=True, exist_ok=True)
        for name, values in tags.items():
            (directory / f"{name}.json").write_text(
                json.dumps({"replace": False, "values": values}, ensure_ascii=False, indent=2) + "\n",
                encoding="utf-8", newline="\n")
    print(f"Generated {len(tags)} astral type tags for both Minecraft versions")


if __name__ == "__main__":
    main()
