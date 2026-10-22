#!/usr/bin/env python3
"""Arrival Shrine template generator.

Template `remnant_bosses:realm/arrival_shrine`, 11 (x) x 9 (y) x 11 (z). No worldgen JSON: Java
(realm/ArrivalShrine) places it with local (5, 0, 5) on the Realm's top surface block and fills
air/water under the 11 x 11 y=0 layer with eclipse stone (up to 16 blocks).

Layers (template frame, x east, z south):
  y = 0      walking floor: a circular eclipse-stone-brick platform (ring pattern, chiseled centre)
             on a fully solid 11 x 11 foundation (eclipse stone / cobbled corners) so nothing floats.
  y = 1      standing layer: the open Waygate facing south (core (5,1,2) with an empty block entity,
             frame (4,1..4,2), (6,1..4,2), (5,4,2), veil (5,2,2), (5,3,2)), the Grace Ember at
             (5,1,7), three broken pillars, soul lanterns, candles and two gravestones.
  y = 1..8   every other cell is stored as air, so placement clears the terrain above the floor.
The player lands at (5,1,4) facing south; (5,1..2,3..6) is kept clear between gate and grace.

Writes out/<version>/data/remnant_bosses/<structures>/realm/arrival_shrine.nbt (DataVersion per
version); --install copies exactly that file into the mod resources, --preview renders PNGs.

Usage: python tools/remnants/structures/arrival_shrine.py [--install] [--preview]
"""
from __future__ import annotations

import argparse
import math
import shutil
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from common import AIR, NS, OUT, REPO, VERSIONS, Template, block_table, make_noise, save_template  # noqa: E402
from nbtlib import Compound, String  # noqa: E402

NAME = "arrival_shrine"
LOCATION = f"{NS}:realm/{NAME}"
SIZE = (11, 9, 11)
SEED = 5_120_412
h3, _ = make_noise(SEED)


def rb(n, **props):
    s = f"{NS}:{n}"
    return s + ("[" + ",".join(f"{k}={v}" for k, v in sorted(props.items())) + "]" if props else "")


E, CE = rb("eclipse_stone"), rb("cobbled_eclipse_stone")
BR, CBR, CH = rb("eclipse_stone_bricks"), rb("cracked_eclipse_stone_bricks"), rb("chiseled_eclipse_stone_bricks")
CORE = rb("eclipse_waygate", facing="south", phase="open")
FRAME = rb("waygate_frame")
VEIL = rb("waygate_veil", open="true")
EMBER = rb("grace_ember")

# Template-frame positions (y=0 floor, y=1 standing layer)
CORE_POS = (5, 1, 2)
FRAME_POS = [(4, y, 2) for y in range(1, 5)] + [(6, y, 2) for y in range(1, 5)] + [(5, 4, 2)]
VEIL_POS = [(5, 2, 2), (5, 3, 2)]
EMBER_POS = (5, 1, 7)
LANDING = (5, 1, 4)
CLEAR_PATH = [(5, y, z) for y in (1, 2) for z in (3, 4, 5, 6)]


def floor_state(x, z):
    dx, dz = x - 5, z - 5
    d = math.hypot(dx, dz)
    r = h3(x, 0, z, 7)
    if d < 0.5:
        return CH                                   # centre emblem
    if d <= 2.3:
        return CBR if r < 0.15 else BR
    if d <= 3.3:                                    # the ring: chiseled at the 8 compass points
        return CH if (dx == 0 or dz == 0 or abs(dx) == abs(dz)) else CBR
    if d <= 5.2:
        if d > 4.6 and r < 0.12:
            return CE
        return CBR if r < 0.3 else BR
    return CE if r < 0.35 else E                    # solid corners outside the platform


def build() -> dict:
    b = {}
    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            b[(x, 0, z)] = floor_state(x, z)
            for y in range(1, SIZE[1]):
                b[(x, y, z)] = AIR
    # the Waygate (open, facing south); its arch spans x 4..6 at z = 2
    b[CORE_POS] = CORE
    for p in FRAME_POS:
        b[p] = FRAME
    for p in VEIL_POS:
        b[p] = VEIL
    b[EMBER_POS] = EMBER
    # broken pillars flanking the gate and one stub at the back
    for y, s in ((1, BR), (2, CBR), (3, BR)):
        b[(1, y, 3)] = s
    b[(1, 4, 3)] = "minecraft:soul_lantern[hanging=false,waterlogged=false]"
    for y, s in ((1, CBR), (2, BR)):
        b[(9, y, 3)] = s
    b[(9, 3, 3)] = rb("eclipse_stone_brick_wall", east="none", north="none", south="none", west="none", up="true",
                      waterlogged="false")
    b[(9, 4, 3)] = "minecraft:soul_lantern[hanging=false,waterlogged=false]"
    b[(8, 1, 8)] = CBR
    b[(8, 2, 8)] = rb("eclipse_stone_brick_slab", type="bottom", waterlogged="false")
    b[(9, 1, 7)] = rb("eclipse_stone_brick_stairs", facing="west", half="bottom", shape="straight", waterlogged="false")
    b[(2, 1, 2)] = rb("eclipse_stone_brick_slab", type="bottom", waterlogged="false")       # fallen capstone
    # candles beside the grace, gravestones toward the back
    b[(4, 1, 8)] = "minecraft:candle[candles=3,lit=true,waterlogged=false]"
    b[(6, 1, 8)] = "minecraft:candle[candles=2,lit=true,waterlogged=false]"
    b[(2, 1, 7)] = rb("weathered_gravestone", facing="east")
    b[(3, 1, 9)] = rb("weathered_gravestone", facing="north")
    for p in CLEAR_PATH:
        assert b[p] == AIR, p
    return b


def template(version) -> Template:
    tpl = Template(size=SIZE)
    for p, s in build().items():
        nbt = Compound({"id": String(f"{NS}:eclipse_waygate")}) if p == CORE_POS else None
        tpl.blocks[p] = (s, nbt)
    return tpl


def staged_path(version) -> Path:
    return OUT / version / "data" / NS / VERSIONS[version]["structures"] / "realm" / f"{NAME}.nbt"


def installed_path(version) -> Path:
    return (REPO / version / "common" / "src" / "main" / "resources" / "data" / NS / VERSIONS[version]["structures"]
            / "realm" / f"{NAME}.nbt")


def preview(version):
    import preview as pv  # noqa: PLC0415  (PIL only needed here)
    from common import load_template  # noqa: PLC0415
    _extend_colors(pv)
    tpl, _ = load_template(staged_path(version))
    world = {p: s for p, (s, _) in tpl.blocks.items()}
    out = pv.PREVIEWS
    pv.render_iso(world, out / f"{NAME}_iso.png", scale=14, title=f"{NAME} {SIZE[0]}x{SIZE[1]}x{SIZE[2]} (view from SE)")
    pv.render_iso(world, out / f"{NAME}_iso_nw.png", scale=14, title=f"{NAME} (view from NW)", view="nw")
    pv.render_layers(world, out / f"{NAME}_layers.png", [0, 1, 2, 3, 4], cell=12,
                     title=f"{NAME} layers (template frame, north up; gate z=2, grace z=7)",
                     box=(0, 0, 0, SIZE[0] - 1, 0, SIZE[2] - 1))
    print("previews written to", out)


def _extend_colors(pv):
    """Preview colours for the realm blocks (preview.py only knows vanilla ones)."""
    extra = [("cracked_eclipse_stone_bricks", (70, 66, 82)), ("chiseled_eclipse_stone_bricks", (150, 120, 60)),
             ("eclipse_stone_brick", (86, 82, 100)), ("cobbled_eclipse_stone", (74, 72, 84)),
             ("eclipse_stone", (64, 62, 76)), ("eclipse_waygate", (200, 170, 70)), ("waygate_frame", (120, 110, 140)),
             ("waygate_veil", (90, 200, 220)), ("grace_ember", (255, 200, 60)), ("weathered_gravestone", (130, 130, 120)),
             ("gilded_leaves", (230, 190, 60)), ("graceroot_log", (200, 190, 160))]
    known = {k for k, _ in pv.COLORS}
    pv.COLORS[:0] = [kc for kc in extra if kc[0] not in known]


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--install", action="store_true", help="copy the staged template into the mod resources")
    ap.add_argument("--preview", action="store_true", help="render preview PNGs into out/previews/")
    args = ap.parse_args()
    for version, info in VERSIONS.items():
        tpl = template(version)
        save_template(staged_path(version), tpl, info["data_version"], block_table(version))
        print(f"[{version}] staged {staged_path(version).relative_to(OUT.parent).as_posix()} "
              f"({len(tpl.blocks)} blocks, {sum(1 for s, _ in tpl.blocks.values() if s != AIR)} non-air)")
        if args.install:
            dst = installed_path(version)
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(staged_path(version), dst)
            print(f"[{version}] installed {dst.relative_to(REPO).as_posix()}")
    if args.preview:
        preview("1.20.1")


if __name__ == "__main__":
    main()
