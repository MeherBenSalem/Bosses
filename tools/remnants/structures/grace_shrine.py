#!/usr/bin/env python3
"""Grace shrine generator.

A 7 x 6 x 7 ruin in every Eclipsed Realm surface land biome: a cracked circular floor of eclipse
stone bricks around a Grace Ember, four broken pillars (one still capped, with gilded leaves
draped over it), two gravestones, a fallen capstone and a few lit candles. Single-piece jigsaw
structure (one pool element, no jigsaw blocks), surface step, beard_thin terrain adaptation,
random_spread spacing 24 separation 8.

Template frame: x east, z south; y=0 is the floor course (it replaces the surface block, like the
Hollow Citadel's ground course), y=1 is the standing layer. Cells outside the floor circle are not
stored (structure void: terrain is kept); cells above the floor circle are stored as air.

Stages under out/<version>/data/remnant_bosses/ (template, structure, structure_set, template pool,
weathering processor list, biome tag has_structure/grace_shrine); --install copies exactly those
files into the mod resources, --preview renders PNGs.

Usage: python tools/remnants/structures/grace_shrine.py [--install] [--preview]
"""
from __future__ import annotations

import argparse
import shutil
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from common import (AIR, EMPTY_POOL, NS, OUT, REPO, VERSIONS, Template, block_table, clean_staged, data_dir,  # noqa: E402
                    make_noise, save_template, structure_files, write_json)

NAME = "grace_shrine"
STRUCTURE = f"{NS}:{NAME}"
TEMPLATE = f"{NS}:{NAME}/shrine"
POOL = f"{NS}:{NAME}/start"
WEATHERING = f"{NS}:{NAME}_weathering"
BIOME_TAG = f"{NS}:has_structure/{NAME}"
BIOMES = [f"{NS}:tarnished_fields", f"{NS}:gracewood", f"{NS}:gravefield_barrows", f"{NS}:ashen_wastes",
          f"{NS}:lantern_heights"]
SET_SALT = 1_402_385_771
SPACING, SEPARATION = 24, 8
SIZE = (7, 6, 7)
SEED = 1_402_385_771
h3, _ = make_noise(SEED)


def rb(n, **props):
    s = f"{NS}:{n}"
    return s + ("[" + ",".join(f"{k}={v}" for k, v in sorted(props.items())) + "]" if props else "")


E, CE = rb("eclipse_stone"), rb("cobbled_eclipse_stone")
BR, CBR, CH = rb("eclipse_stone_bricks"), rb("cracked_eclipse_stone_bricks"), rb("chiseled_eclipse_stone_bricks")
SLAB = rb("eclipse_stone_brick_slab", type="bottom", waterlogged="false")
WALL = rb("eclipse_stone_brick_wall", east="none", north="none", south="none", west="none", up="true", waterlogged="false")
LEAVES = rb("gilded_leaves", distance="7", persistent="true", waterlogged="false")  # persistent: never decays
EMBER = rb("grace_ember")
EMBER_POS = (3, 1, 3)
R2 = 10  # floor circle: (x-3)^2 + (z-3)^2 <= 10


def stairs(facing, half="bottom"):
    return rb("eclipse_stone_brick_stairs", facing=facing, half=half, shape="straight", waterlogged="false")


def grave(facing):
    return rb("weathered_gravestone", facing=facing)


def candle(n):
    return f"minecraft:candle[candles={n},lit=true,waterlogged=false]"


def in_circle(x, z):
    return (x - 3) ** 2 + (z - 3) ** 2 <= R2


PILLARS = {  # base (x, z) -> column bottom to top
    (1, 1): [BR, CBR, BR, CH],
    (5, 1): [CBR, SLAB],
    (1, 5): [BR, CBR, WALL],
    (5, 5): [CBR],
}


def build() -> dict:
    b = {}
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            if not in_circle(x, z):
                continue  # structure void: keep the terrain
            d2 = (x - 3) ** 2 + (z - 3) ** 2
            r = h3(x, 0, z, 3)
            if d2 == 0:
                floor = CH
            elif d2 <= 2:
                floor = BR if r < 0.7 else CBR
            elif (x, z) in PILLARS:
                floor = BR
            elif r < 0.12:
                floor = None          # a missing slab of floor: the ground shows through
            else:
                floor = CBR if r < 0.55 else (BR if r < 0.85 else CE)
            if floor:
                b[(x, 0, z)] = floor
            for y in range(1, SIZE[1]):
                b[(x, y, z)] = AIR
    for (x, z), column in PILLARS.items():
        for i, s in enumerate(column):
            b[(x, 1 + i, z)] = s
    # gilded leaves draped over the capped pillar
    for p in ((1, 5, 1), (2, 4, 1), (1, 4, 2), (2, 3, 1)):
        b[p] = LEAVES
    b[EMBER_POS] = EMBER
    b[(6, 1, 4)] = stairs("west")            # the fallen capstone of (5,1)
    b[(3, 1, 6)] = grave("north")            # facing the grace
    b[(0, 1, 3)] = grave("east")
    b[(2, 1, 4)] = candle(2)
    b[(4, 1, 2)] = candle(1)
    return b


def template() -> Template:
    tpl = Template(size=SIZE)
    for p, s in build().items():
        tpl.blocks[p] = (s, None)
    return tpl


def worldgen_json(version):
    structure = {
        "type": "minecraft:jigsaw",
        "biomes": f"#{BIOME_TAG}",
        "step": "surface_structures",
        "terrain_adaptation": "beard_thin",
        "spawn_overrides": {},
        "start_pool": POOL,
        "size": 1,
        "start_height": {"absolute": 0},
        "project_start_to_heightmap": "WORLD_SURFACE_WG",
        "max_distance_from_center": 16,
        "use_expansion_hack": False,
    }
    if version != "1.20.1":
        structure["dimension_padding"] = 0
        structure["liquid_settings"] = "ignore_waterlogging"
    structure_set = {
        "structures": [{"structure": STRUCTURE, "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": SPACING, "separation": SEPARATION, "salt": SET_SALT},
    }
    pool = {"fallback": EMPTY_POOL, "elements": [{"weight": 1, "element": {
        "element_type": "minecraft:single_pool_element", "location": TEMPLATE, "processors": WEATHERING,
        "projection": "rigid"}}]}

    def rule(block, out, prob):
        return {"input_predicate": {"predicate_type": "minecraft:random_block_match", "block": block, "probability": prob},
                "location_predicate": {"predicate_type": "minecraft:always_true"},
                "output_state": {"Name": out}}

    weathering = {"processors": [{"processor_type": "minecraft:rule", "rules": [
        rule(f"{NS}:eclipse_stone_bricks", f"{NS}:cracked_eclipse_stone_bricks", 0.2),
        rule(f"{NS}:cracked_eclipse_stone_bricks", f"{NS}:cobbled_eclipse_stone", 0.1),
    ]}]}
    biome_tag = {"replace": False, "values": list(BIOMES)}
    return structure, structure_set, pool, weathering, biome_tag


def stage(version):
    info = VERSIONS[version]
    root = data_dir(version)
    clean_staged(version, NAME)
    tpl = template()
    save_template(root / info["structures"] / NAME / "shrine.nbt", tpl, info["data_version"], block_table(version))
    structure, structure_set, pool, weathering, biome_tag = worldgen_json(version)
    write_json(root / "worldgen" / "structure" / f"{NAME}.json", structure)
    write_json(root / "worldgen" / "structure_set" / f"{NAME}.json", structure_set)
    write_json(root / "worldgen" / "template_pool" / NAME / "start.json", pool)
    write_json(root / "worldgen" / "processor_list" / f"{NAME}_weathering.json", weathering)
    write_json(root / "tags" / "worldgen" / "biome" / "has_structure" / f"{NAME}.json", biome_tag)
    print(f"[{version}] staged {NAME}: {len(tpl.blocks)} blocks, "
          f"{sum(1 for s, _ in tpl.blocks.values() if s != AIR)} non-air")


def install(version):
    src_root = OUT / version / "data"
    dst_root = REPO / version / "common" / "src" / "main" / "resources" / "data"
    for f in structure_files(version, NAME):
        rel = f.relative_to(src_root)
        dst = dst_root / rel
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(f, dst)
        print(f"[{version}] installed data/{rel.as_posix()}")


def preview():
    import arrival_shrine  # noqa: PLC0415
    import preview as pv  # noqa: PLC0415
    arrival_shrine._extend_colors(pv)
    world = {p: s for p, (s, _) in template().blocks.items()}
    pv.render_iso(world, pv.PREVIEWS / f"{NAME}_iso.png", scale=16, title=f"{NAME} {SIZE[0]}x{SIZE[1]}x{SIZE[2]} (SE)")
    pv.render_iso(world, pv.PREVIEWS / f"{NAME}_iso_nw.png", scale=16, title=f"{NAME} (NW)", view="nw")
    pv.render_layers(world, pv.PREVIEWS / f"{NAME}_layers.png", [0, 1, 2, 3, 4], cell=14,
                     title=f"{NAME} layers (north up)", box=(0, 0, 0, SIZE[0] - 1, 0, SIZE[2] - 1))
    print("previews written to", pv.PREVIEWS)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--install", action="store_true", help="copy this structure's staged files into the mod resources")
    ap.add_argument("--preview", action="store_true", help="render preview PNGs into out/previews/")
    args = ap.parse_args()
    for version in VERSIONS:
        stage(version)
        if args.install:
            install(version)
    if args.preview:
        preview()


if __name__ == "__main__":
    main()
