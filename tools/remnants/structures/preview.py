#!/usr/bin/env python3
"""Preview renderer for staged jigsaw structures (Hollow Citadel, Gravebound Catacombs).

Writes to tools/remnants/structures/out/previews/ (the Hollow Citadel) or out/previews/<structure>/ (others):
  <piece>_iso.png, <piece>_layers.png           one per template
  assembled_iso_se.png / _nw.png                 pieces assembled like the jigsaw would (start rotation 0)
  assembled_weathered_iso_se.png                 same, with the processor lists simulated
  assembled_plan.png, assembled_layers.png       top-down height map and layer slices
  assembled_rot90_plan.png                       start rotation 90 (offerings must still read gold E/W)
  assembled_cutaway_y<N>.png                     underground structures: everything above y=N removed

Underground structures hide their natural rock fill (deepslate, tuff, stone, andesite) in every render.

Usage: python tools/remnants/structures/preview.py [hollow_citadel | gravebound_catacombs] [--version 1.20.1]
"""
from __future__ import annotations

import argparse
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
from common import (NS, OUT, STRUCTURES, VERSIONS, assemble, build_world, is_full_solid,  # noqa: E402
                    load_template, parse_state, rot_pos, state_name, template_path)

PREVIEWS = OUT / "previews"
CUTAWAY = {"throne_hall": 9, "barracks": 7, "gatehouse": 9, "royal_vault": 4, "antechamber": 6, "courtyard": 5,
           "east_tower": 7, "wall_west": 6, "wall_east": 6, "offerings": 2}
ROCK = ("minecraft:deepslate", "minecraft:tuff", "minecraft:stone", "minecraft:andesite")
VIEWS = {   # per structure: title, output subfolder, piece cutaways (template y), assembled layers, rock hiding
    "hollow_citadel": {"title": "Hollow Citadel", "dir": "", "cutaway": CUTAWAY, "layers": [0, 1, 3, 9, 13, 17],
                       "hide": (), "cutaways": []},
    "gravebound_catacombs": {"title": "Gravebound Catacombs", "dir": "gravebound_catacombs",
                             "cutaway": {"crossing": 5, "entry_stair": 30, "cells_west": 7, "cells_east": 4,
                                         "ossuary_gallery": 6, "burial_vault": 6, "chain_chapel": 9, "offerings": 2},
                             "layers": [-6, -5, -3, 0, 1, 2, 4, 6, 9, 16, 27, 28], "hide": ROCK, "cutaways": [-3, 2, 4, 7, 31]},
    # every piece starts at the foundation bottom (template y 0 = assembled y 0); the floor course is y=12
    "forgotten_observatory": {"title": "Forgotten Observatory", "dir": "forgotten_observatory",
                              "cutaway": {"dome": 19, "lens_gallery": 18, "west_hall": 17, "east_hall": 17,
                                          "memory_vault": 16, "approach": 16, "offerings": 2},
                              "piece_layers": [0, 7, 11, 12, 13, 14, 16, 18, 20, 23, 26, 29],
                              "layers": [0, 7, 12, 13, 14, 15, 17, 19, 22, 25, 27, 29], "hide": (),
                              "cutaways": [14, 18, 23]},
}
BG = (24, 26, 32)
VOID = (16, 17, 21)

COLORS = [
    ("cracked_deepslate_bricks", (62, 62, 66)), ("deepslate_brick", (74, 74, 78)), ("cracked_deepslate_tiles", (46, 46, 50)),
    ("deepslate_tile", (54, 54, 58)), ("chiseled_deepslate", (66, 66, 70)), ("polished_deepslate", (82, 82, 86)),
    ("cobbled_deepslate", (90, 90, 94)), ("cracked_polished_blackstone_bricks", (44, 38, 46)),
    ("polished_blackstone_brick", (52, 46, 55)), ("chiseled_polished_blackstone", (60, 52, 62)),
    ("gilded_blackstone", (110, 82, 40)), ("polished_blackstone", (58, 52, 62)), ("blackstone", (44, 38, 44)),
    ("gold_block", (246, 208, 61)), ("soul_lantern", (100, 220, 215)), ("chain", (70, 74, 86)), ("iron_bars", (150, 152, 160)),
    ("blue_wool", (53, 57, 157)), ("blue_carpet", (53, 57, 157)), ("light_blue_wool", (58, 175, 217)),
    ("black_wool", (25, 25, 30)), ("black_carpet", (25, 25, 30)), ("gray_wool", (62, 68, 71)), ("gray_carpet", (62, 68, 71)),
    ("moss_carpet", (89, 115, 44)), ("moss_block", (89, 115, 44)), ("cobweb", (225, 225, 230)), ("tuff", (108, 109, 102)),
    ("gravel", (131, 127, 126)), ("coarse_dirt", (119, 85, 59)), ("rooted_dirt", (144, 103, 76)), ("water", (50, 90, 200)),
    ("dark_oak_log", (60, 46, 26)), ("dark_oak", (66, 43, 20)), ("chest", (164, 116, 44)), ("barrel", (128, 92, 52)),
    ("candle", (210, 200, 170)), ("magenta_glazed_terracotta", (208, 100, 191)), ("jigsaw", (120, 90, 120)),
    ("ancient_altar", (150, 120, 170)), ("ancient_pedestal", (120, 100, 150)), ("campfire", (90, 70, 50)),
    ("anvil", (72, 72, 72)), ("grindstone", (140, 140, 140)), ("fletching_table", (180, 160, 110)),
    ("blue_stained_glass_pane", (60, 70, 170)), ("black_stained_glass_pane", (20, 20, 26)), ("banner", (53, 57, 157)),
    ("glazed", (200, 100, 190)), ("lightning_rod", (190, 110, 80)), ("armor_stand", (160, 130, 90)),
    ("bone_block", (196, 190, 168)), ("skull", (214, 214, 200)), ("soul_soil", (75, 57, 46)), ("soul_fire", (90, 230, 230)),
    ("hanging_roots", (150, 100, 70)),
    # Forgotten Observatory (Seyr): base colours are divided by the 1.55 boost so pale stone reads true
    ("chiseled_sandstone", (135, 124, 88)), ("cut_sandstone", (142, 133, 100)), ("smooth_sandstone", (146, 139, 108)),
    ("sandstone", (136, 128, 96)), ("calcite", (146, 147, 144)), ("oxidized_cut_copper", (52, 108, 88)),
    ("oxidized_copper", (52, 108, 88)), ("weathered_cut_copper", (70, 100, 72)), ("weathered_copper", (70, 100, 72)),
    ("amethyst_cluster", (112, 84, 140)), ("amethyst_bud", (112, 84, 140)), ("amethyst", (88, 64, 124)),
    ("tinted_glass", (38, 32, 44)), ("chiseled_bookshelf", (84, 62, 40)), ("bookshelf", (92, 70, 46)),
    ("lectern", (100, 76, 50)), ("fog_gate", (150, 144, 120)),
    ("cracked_eclipse_stone_bricks", (70, 66, 82)), ("eclipse_stone_brick", (86, 82, 100)),
    ("cobbled_eclipse_stone", (74, 72, 84)), ("eclipse_stone", (64, 62, 76)),
]
AIRS = ("minecraft:air", "minecraft:cave_air", "minecraft:structure_void")  # cave_air marks cobweb candidates
THIN = ("chain", "iron_bars", "pane", "fence", "_wall", "lantern", "candle", "banner")


def color(s, boost=1.55):
    """Block top colour, brightened for legibility (deepslate is very dark at true value)."""
    n = state_name(s).split(":", 1)[1]
    for key, c in COLORS:
        if key in n:
            break
    else:
        h = sum(map(ord, n)) * 2654435761 & 0xFFFFFF
        c = (90 + (h & 63), 90 + ((h >> 6) & 63), 90 + ((h >> 12) & 63))
    return tuple(min(255, int(v * boost)) for v in c)


def shape(s):
    """(inset, y0, height) of a rough cuboid for the block."""
    n = state_name(s)
    _, p = parse_state(s)
    if n.endswith("_carpet"):
        return (0, 0, .08)
    if n.endswith("_slab"):
        return (0, .5, .5) if p.get("type") == "top" else (0, 0, 1 if p.get("type") == "double" else .5)
    if "lantern" in n:
        return (.3, .1 if p.get("hanging") == "true" else 0, .55)
    if "candle" in n:
        return (.35, 0, .4)
    if n.endswith("_wall") and "banner" not in n:
        return (.25, 0, 1)
    if "chain" in n or "iron_bars" in n or "pane" in n or "fence" in n:
        return (.4, 0, 1)
    if "banner" in n:
        return (.2, 0, 1)
    if "cobweb" in n:
        return (.15, .1, .8)
    if "campfire" in n:
        return (0, 0, .45)
    if "amethyst_cluster" in n or "amethyst_bud" in n:
        return (.25, 0, .55)
    if "lectern" in n:
        return (.1, 0, .9)
    return (0, 0, 1)


def shade(c, k, jitter=0.0):
    return tuple(max(0, min(255, int(v * k * (1 + jitter)))) for v in c)


def render_iso(world, path, scale=6, title=None, view="se"):
    if not world:
        return
    # rotate so the requested viewer side becomes the +x/+z (south-east) side
    r = {"se": 0, "ne": 1, "nw": 2, "sw": 3}[view]
    W = {}
    for p, s in world.items():
        if s is None or state_name(s) in AIRS:
            continue
        q = rot_pos(p, r)
        W[q] = s
    if not W:
        return
    s_ = scale

    def proj(x, y, z):
        return ((x - z) * s_, (x + z) * s_ / 2 - y * s_)

    xs = [p[0] for p in W]
    ys = [p[1] for p in W]
    zs = [p[2] for p in W]
    umin = (min(xs) - max(zs) - 2) * s_
    umax = (max(xs) - min(zs) + 2) * s_
    vmin = (min(xs) + min(zs)) * s_ / 2 - (max(ys) + 2) * s_
    vmax = (max(xs) + max(zs) + 2) * s_ / 2 - (min(ys) - 2) * s_
    pad = 20
    head = 28 if title else 0
    w, h = int(umax - umin) + 2 * pad, int(vmax - vmin) + 2 * pad + head
    img = Image.new("RGB", (w, h), BG)
    dr = ImageDraw.Draw(img, "RGBA")
    ox, oy = pad - umin, pad - vmin + head

    def opaque(q):
        t = W.get(q)
        return t is not None and is_full_solid(t)

    order = sorted(W, key=lambda p: (p[0] + p[2], p[1], p[0]))
    for (x, y, z) in order:
        s = W[(x, y, z)]
        if opaque((x, y + 1, z)) and opaque((x + 1, y, z)) and opaque((x, y, z + 1)):
            continue
        inset, y0, hh = shape(s)
        c = color(s)
        j = (((x * 73856093) ^ (y * 19349663) ^ (z * 83492791)) & 255) / 255.0 * 0.12 - 0.06
        a = (x + inset, y + y0, z + inset)
        b = (x + 1 - inset, y + y0 + hh, z + 1 - inset)

        def P(px, py, pz):
            u, v = proj(px, py, pz)
            return (u + ox, v + oy)
        top = [P(a[0], b[1], a[2]), P(b[0], b[1], a[2]), P(b[0], b[1], b[2]), P(a[0], b[1], b[2])]
        east = [P(b[0], b[1], a[2]), P(b[0], b[1], b[2]), P(b[0], a[1], b[2]), P(b[0], a[1], a[2])]
        south = [P(a[0], b[1], b[2]), P(b[0], b[1], b[2]), P(b[0], a[1], b[2]), P(a[0], a[1], b[2])]
        alpha = 150 if "cobweb" in s else (190 if "pane" in s or "water" in s else 255)
        dr.polygon(east, fill=shade(c, .62, j) + (alpha,))
        dr.polygon(south, fill=shade(c, .80, j) + (alpha,))
        dr.polygon(top, fill=shade(c, 1.08, j) + (alpha,))
        if "lantern" in s:  # glow
            u, v = P(x + .5, y + .5, z + .5)
            dr.ellipse([u - 2.2 * s_, v - 2.2 * s_, u + 2.2 * s_, v + 2.2 * s_], fill=(80, 220, 210, 28))
    if title:
        dr.text((10, 8), title, fill=(220, 220, 225))
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def render_layers(world, path, ys, cell=6, title=None, box=None):
    pts = [p for p, s in world.items() if s is not None]
    if not pts:
        return
    if box is None:
        x0, x1 = min(p[0] for p in pts), max(p[0] for p in pts)
        z0, z1 = min(p[2] for p in pts), max(p[2] for p in pts)
    else:
        x0, _, z0, x1, _, z1 = box
    gw, gh = (x1 - x0 + 1) * cell, (z1 - z0 + 1) * cell
    cols = min(4, len(ys))
    rows = math.ceil(len(ys) / cols)
    pad, lab = 10, 16
    head = 24 if title else 0
    img = Image.new("RGB", (cols * (gw + pad) + pad, rows * (gh + pad + lab) + pad + head), BG)
    dr = ImageDraw.Draw(img)
    if title:
        dr.text((10, 6), title, fill=(220, 220, 225))
    for i, y in enumerate(ys):
        cx = pad + (i % cols) * (gw + pad)
        cy = head + pad + (i // cols) * (gh + pad + lab)
        dr.text((cx, cy), f"y={y}", fill=(200, 200, 200))
        cy += lab
        dr.rectangle([cx, cy, cx + gw - 1, cy + gh - 1], fill=VOID)
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                s = world.get((x, y, z))
                if s is None:
                    continue
                if state_name(s) in AIRS:
                    c = (38, 41, 50)
                else:
                    c = color(s)
                    if not is_full_solid(s):
                        c = shade(c, 1.15)
                px, pz = cx + (x - x0) * cell, cy + (z - z0) * cell
                dr.rectangle([px, pz, px + cell - 1, pz + cell - 1], fill=c)
        # north arrow
        dr.text((cx + gw - 10, cy + 2), "N", fill=(240, 240, 120))
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def render_plan(world, path, cell=7, title=None, marks=None):
    top = {}
    for (x, y, z), s in world.items():
        if s is None or state_name(s) in AIRS:
            continue
        if (x, z) not in top or y > top[(x, z)][0]:
            top[(x, z)] = (y, s)
    if not top:
        return
    xs = [k[0] for k in top]
    zs = [k[1] for k in top]
    x0, x1, z0, z1 = min(xs), max(xs), min(zs), max(zs)
    ymax = max(v[0] for v in top.values())
    head = 24 if title else 0
    img = Image.new("RGB", ((x1 - x0 + 1) * cell + 20, (z1 - z0 + 1) * cell + 20 + head), BG)
    dr = ImageDraw.Draw(img)
    for (x, z), (y, s) in top.items():
        k = 0.45 + 0.75 * (y / max(1, ymax))
        c = shade(color(s), k)
        px, pz = 10 + (x - x0) * cell, head + 10 + (z - z0) * cell
        dr.rectangle([px, pz, px + cell - 1, pz + cell - 1], fill=c)
    for (x, z, label, c) in (marks or []):
        px, pz = 10 + (x - x0) * cell, head + 10 + (z - z0) * cell
        dr.rectangle([px - 1, pz - 1, px + cell, pz + cell], outline=c)
        if label:
            dr.text((px + cell + 2, pz - 3), label, fill=c)
    if title:
        dr.text((10, 6), title, fill=(220, 220, 225))
    img.save(path)


def template_world(tpl):
    w = {}
    for p, (s, nbt) in tpl.blocks.items():
        w[p] = s  # jigsaws stay visible (purple) in piece previews
    return w


def hide_rock(world, names):
    if not names:
        return world
    return {p: s for p, s in world.items() if s is None or state_name(s) not in names}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("structure", nargs="?", default="hollow_citadel", choices=[n for n in STRUCTURES if n in VIEWS])
    ap.add_argument("--version", default="1.20.1", choices=list(VERSIONS))
    ap.add_argument("--quick", action="store_true", help="assembled renders only")
    args = ap.parse_args()
    v = args.version
    name, view = args.structure, VIEWS[args.structure]
    title, CUTAWAY = view["title"], view["cutaway"]
    PREVIEWS = OUT / "previews" / view["dir"] if view["dir"] else OUT / "previews"
    PREVIEWS.mkdir(parents=True, exist_ok=True)
    tdir = template_path(v, f"{NS}:{name}/x").parent
    if not args.quick:
        for f in sorted(tdir.glob("*.nbt")):
            tpl, _ = load_template(f)
            w = hide_rock(template_world(tpl), view["hide"])
            render_iso(w, PREVIEWS / f"{f.stem}_iso.png", scale=9 if max(tpl.size) < 30 else 7,
                       title=f"{f.stem}  {tpl.size[0]}x{tpl.size[1]}x{tpl.size[2]}  (jigsaws shown purple)")
            cap = CUTAWAY.get(f.stem, 6)
            render_iso({q: t for q, t in w.items() if q[1] <= cap}, PREVIEWS / f"{f.stem}_cutaway.png", scale=10 if max(tpl.size) < 30 else 8,
                       title=f"{f.stem} cutaway (y <= {cap})")
            ys = sorted(set(view.get("piece_layers") or
                            [0, 1, 2, 4, 7, 9, 12, 15, 19, 21, 24] + ([28, 29, 31, 34] if view["hide"] else []))
                        & set(range(tpl.size[1])))
            render_layers(w, PREVIEWS / f"{f.stem}_layers.png", ys, cell=7 if max(tpl.size) < 30 else 6,
                          title=f"{f.stem} layers (template frame, north up)", box=(0, 0, 0, tpl.size[0] - 1, 0, tpl.size[2] - 1))
    sid = f"{NS}:{name}"
    placed = assemble(v, structure_id=sid)
    world, _ = build_world(v, placed, weathering=False)
    world = hide_rock(world, view["hide"])
    render_iso(world, PREVIEWS / "assembled_iso_se.png", scale=6, title=f"{title} assembled ({len(placed)} pieces), view from south-east")
    render_iso(world, PREVIEWS / "assembled_iso_nw.png", scale=6, title=f"{title} assembled, view from north-west", view="nw")
    for cap in view["cutaways"]:
        render_iso({q: t for q, t in world.items() if q[1] <= cap}, PREVIEWS / f"assembled_cutaway_y{cap}.png", scale=6,
                   title=f"{title} assembled, cut above y={cap}, view from south-east")
    wworld, _ = build_world(v, placed, weathering=True)
    wworld = hide_rock(wworld, view["hide"])
    render_iso(wworld, PREVIEWS / "assembled_weathered_iso_se.png", scale=6, title="Assembled with weathering processors simulated (SE)")
    render_iso(wworld, PREVIEWS / "assembled_weathered_iso_sw.png", scale=6, title="Assembled with weathering processors simulated (SW)", view="sw")
    marks = [(pc.box[0], pc.box[2], pc.location.split("/")[-1], (240, 200, 90)) for pc in placed]
    render_plan(wworld, PREVIEWS / "assembled_plan.png", title="Assembled plan (height shaded, weathered), north up", marks=marks)
    render_layers(world, PREVIEWS / "assembled_layers.png", view["layers"], cell=4, title="Assembled layers (north up)")
    p90 = assemble(v, structure_id=sid, start_rotation=1)
    w90, _ = build_world(v, p90, weathering=False)
    w90 = hide_rock(w90, view["hide"])
    render_plan(w90, PREVIEWS / "assembled_rot90_plan.png", title="Start rotation 90 (cw)")
    print("previews written to", PREVIEWS)


if __name__ == "__main__":
    main()
