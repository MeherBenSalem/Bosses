#!/usr/bin/env python3
"""Hollow Citadel generator.

Builds the whole ruined Vael fortress in one global block plan (deterministic, fixed seed),
cuts it into jigsaw pieces (each <= 48x48x48) joined by floor-level jigsaw blocks, and stages a
datapack for 1.20.1 and 1.21.1 under tools/remnants/structures/out/<version>/data/remnant_bosses/.
Nothing is written into the mod resources; install.py does that once the referenced entities
and items are registered.

Plan frame (= assembled frame at start rotation 0): x east, z south, y up. y=0 is the floor /
ground course (the start piece's minY lands on the surface block), mobs stand at y=1.

Rotation-proof altar offerings: vanilla rotates jigsaw structures randomly, but the Sovereign
ritual checks absolute east/west (gold) and north/south (soul lantern). The throne hall therefore
places magenta glazed terracotta direction markers on the pedestals, and a 7x1x7 "offerings"
piece, attached inside the hall by a vertical aligned jigsaw, swaps each marker for the correct
offering with blockstate_match location predicates on the marker's rotated facing.

Run:  python tools/remnants/structures/hollow_citadel.py
"""
from __future__ import annotations

import math
import random
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from common import (AIR, EMPTY_POOL, ENTITY_NS, NS, OUT, VERSIONS, Plan, block_table, clean_staged,  # noqa: E402
                    cut_pieces as _cut_pieces, data_dir, fix_connections, fmt_state, make_noise, mirror_x_state,
                    offerings_template as _offerings_template, pick, piece_of as _piece_of, save_template,
                    state_name, write_json)
from nbtlib import Byte, Compound, Float, Int, List, String  # noqa: E402

SEED = 714309221
STRUCTURE = f"{NS}:hollow_citadel"
WEATHERING = f"{NS}:hollow_citadel_weathering"
OFFERING_PROC = f"{NS}:hollow_citadel_offerings"
LOOT_BARRACKS = f"{NS}:chests/hollow_citadel_barracks"
LOOT_VAULT = f"{NS}:chests/hollow_citadel_vault"


def pool_id(name):
    return f"{NS}:hollow_citadel/{name}"


def loc_id(name):
    return f"{NS}:hollow_citadel/{name}"


def jig_name(name):
    return f"{NS}:hollow_citadel/{name}"


def M(n):
    return "minecraft:" + n


DB, CDB = M("deepslate_bricks"), M("cracked_deepslate_bricks")
DT, CDT = M("deepslate_tiles"), M("cracked_deepslate_tiles")
CHD, PD, CD = M("chiseled_deepslate"), M("polished_deepslate"), M("cobbled_deepslate")
PB, PBB, CPBB = M("polished_blackstone"), M("polished_blackstone_bricks"), M("cracked_polished_blackstone_bricks")
CHPB, GB, BS = M("chiseled_polished_blackstone"), M("gilded_blackstone"), M("blackstone")
GOLD, TUFF, GRAVEL = M("gold_block"), M("tuff"), M("coarse_dirt")  # no gravity blocks on the bottom layer
MOSS, MOSSC, WEB = M("moss_block"), M("moss_carpet"), M("cobweb")
COARSE, ROOTED = M("coarse_dirt"), M("rooted_dirt")
WATER = M("water[level=0]")
BARS = M("iron_bars")
CHAIN = M("chain[axis=y]")
PLANKS, LOG_X, LOG_Z, LOG_Y = M("dark_oak_planks"), M("dark_oak_log[axis=x]"), M("dark_oak_log[axis=z]"), M("dark_oak_log[axis=y]")
FENCE = M("dark_oak_fence")
MARKER = "minecraft:magenta_glazed_terracotta"
ALTAR = f"{NS}:ancient_altar"
PEDESTAL = f"{NS}:ancient_pedestal"


def stairs(mat, facing, half="bottom", shape="straight"):
    return f"minecraft:{mat}_stairs[facing={facing},half={half},shape={shape}]"


def slab(mat, t="bottom"):
    return f"minecraft:{mat}_slab[type={t}]"


def wallb(mat):
    return f"minecraft:{mat}_wall"


def lantern(hanging=False):
    return f"minecraft:soul_lantern[hanging={'true' if hanging else 'false'}]"


def carpet(c):
    return f"minecraft:{c}_carpet"


def wool(c):
    return f"minecraft:{c}_wool"


def wall_banner(facing, color="blue"):
    return f"minecraft:{color}_wall_banner[facing={facing}]"


VAEL_BANNER = [("gradient_up", "black"), ("rhombus", "brown"), ("triangles_bottom", "black"), ("border", "black")]
VAEL_BANNER_TORN = [("stripe_top", "black"), ("circle", "brown"), ("triangles_bottom", "black"), ("gradient_up", "black")]


h3, vnoise = make_noise(SEED)


def mason(x, y, z, band=6, plinth=1):
    """Wall course by height: blackstone plinth, deepslate tile lower courses, polished band, brick upper courses."""
    r = h3(x, y, z, 11)
    if y <= plinth:
        return CPBB if r < .07 else PBB
    if y == band:
        return PD if r > .06 else CHD
    if y < band:
        return CDT if r < .10 else DT
    return CDB if r < .07 else DB


RUBBLE = [(CD, 30), (DB, 13), (CDB, 12), (DT, 8), (TUFF, 8), (PBB, 5), (CDT, 6), (MOSS, 5)]


def rubble(P, cx, cz, rx, rz, hmax, salt, top=24):
    for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
        for z in range(int(cz - rz) - 1, int(cz + rz) + 2):
            d = ((x - cx) / rx) ** 2 + ((z - cz) / rz) ** 2 + (h3(x, 0, z, salt) - .5) * .6
            if d > 1:
                continue
            g = P.ground(x, z, top)
            h = int(round(hmax * (1 - d) + (h3(x, 1, z, salt) - .4)))
            y = g
            for k in range(max(0, h)):
                y = g + k
                if not P.deco(x, y, z, pick(RUBBLE, h3(x, y, z, salt + 3))):
                    break
            else:
                y = g + max(0, h)
                r = h3(x, 2, z, salt)
                f = ["north", "south", "east", "west"][int(h3(x, 3, z, salt) * 4)]
                if r < .16:
                    P.deco(x, y, z, slab(["cobbled_deepslate", "deepslate_brick", "deepslate_tile"][int(h3(x, 4, z, salt) * 3)]))
                elif r < .27:
                    P.deco(x, y, z, stairs(["cobbled_deepslate", "deepslate_brick"][int(h3(x, 4, z, salt) * 2)], f))
                elif r < .38:
                    P.deco(x, y, z, MOSSC)


def drape(P, x0, x1, z, ytop, lengths, face, salt):
    """Faded Vael cloth hanging against a wall: blue field, black border, ragged hem."""
    for i, x in enumerate(range(x0, x1 + 1)):
        ln = lengths[i % len(lengths)]
        for k in range(ln):
            y = ytop - k
            edge = x in (x0, x1)
            r = h3(x, y, z, salt)
            if edge:
                c = "black"
            elif r < .08:
                c = "gray"
            elif r < .13:
                c = "light_blue"
            else:
                c = "blue"
            if k == ln - 1 and r < .3 and not edge:
                c = "black"
            P.put(x, y, z, wool(c))


def drape_z(P, x, z0, z1, ytop, lengths, salt):
    for i, z in enumerate(range(z0, z1 + 1)):
        ln = lengths[i % len(lengths)]
        for k in range(ln):
            y = ytop - k
            edge = z in (z0, z1)
            r = h3(x, y, z, salt)
            c = "black" if edge else ("gray" if r < .08 else "blue")
            P.put(x, y, z, wool(c))


def cobwebs(P, x0, y0, z0, x1, y1, z1, chance, salt):
    """Cobwebs in upper corners: cells touching at least two solid neighbours (wall + ceiling)."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            for y in range(y0, y1 + 1):
                if not P.is_empty(x, y, z) or h3(x, y, z, salt) > chance:
                    continue
                side = sum(1 for d in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1))
                           if not P.is_empty(x + d[0], y + d[1], z + d[2]))
                if side >= 1 and not P.is_empty(x, y + 1, z) and (side >= 2 or h3(x, y, z, salt + 1) < .4):
                    P.deco(x, y, z, WEB)


def moss_floor(P, x0, z0, x1, z1, top, chance, salt, scale=4.0):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if vnoise(x, z, scale, salt) * .7 + h3(x, 0, z, salt) * .3 < 1 - chance:
                continue
            y = P.ground(x, z, top)
            below = P.get(x, y - 1, z)
            if below and (state_name(below).endswith("_stairs") or state_name(below).endswith("_slab")
                          or state_name(below).endswith("_wall") or "fence" in below or "lantern" in below):
                continue
            P.deco(x, y, z, MOSSC)


def floor_tiles(x, z, salt=0):
    r = h3(x, 0, z, 31 + salt)
    if r < .12:
        return CDT
    if r < .2:
        return PD
    return DT


def curtain(P, x0, z0, x1, z1, outer, top=8, band=6):
    """Solid curtain wall with wall-walk floor at y=top and a crenellated parapet on the outer edge."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            P.put(x, 0, z, PBB)
            for y in range(1, top):
                P.put(x, y, z, mason(x, y, z, band))
            P.put(x, top, z, PD if h3(x, top, z, 5) > .25 else DT)
    if outer in ("west", "east"):
        x = x0 if outer == "west" else x1
        for i, z in enumerate(range(z0, z1 + 1)):
            P.put(x, top + 1, z, mason(x, top + 1, z, band))
            if i % 2 == 0:
                P.put(x, top + 2, z, mason(x, top + 2, z, band))
            if z % 4 == 1:  # arrow slit recess on the outer face
                for y in (3, 4):
                    P.put(x, y, z, AIR)
    else:
        z = z0 if outer == "north" else z1
        for i, x in enumerate(range(x0, x1 + 1)):
            P.put(x, top + 1, z, mason(x, top + 1, z, band))
            if i % 2 == 0:
                P.put(x, top + 2, z, mason(x, top + 2, z, band))
            if x % 4 == 1:
                for y in (3, 4):
                    P.put(x, y, z, AIR)


def battlement_ring(P, x0, z0, x1, z1, y, phase=0):
    """Parapet course at y and alternating merlons at y+1 around a rectangle."""
    ring = [(x, z0) for x in range(x0, x1 + 1)] + [(x1, z) for z in range(z0 + 1, z1 + 1)]
    ring += [(x, z1) for x in range(x1 - 1, x0 - 1, -1)] + [(x0, z) for z in range(z1 - 1, z0, -1)]
    for i, (x, z) in enumerate(ring):
        P.put(x, y, z, mason(x, y, z))
        if (i + phase) % 2 == 0:
            P.put(x, y + 1, z, mason(x, y + 1, z))


def shell(P, x0, z0, x1, z1, y0, y1, band=6):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                for y in range(y0, y1 + 1):
                    P.put(x, y, z, mason(x, y, z, band))


def arch_x(P, x0, x1, z, ytop, mat="deepslate_brick"):
    """Doorway (opening spans x0..x1 in a wall running along x) with upside-down stair corners at ytop."""
    P.put(x0, ytop, z, stairs(mat, "west", "top"))
    P.put(x1, ytop, z, stairs(mat, "east", "top"))


def arch_z(P, x, z0, z1, ytop, mat="deepslate_brick"):
    P.put(x, ytop, z0, stairs(mat, "north", "top"))
    P.put(x, ytop, z1, stairs(mat, "south", "top"))


def collapse(P, cells, base_fn, salt):
    """Remove plan blocks above a noisy break line; base_fn(x, z) gives the lowest y removed."""
    for (x, z) in cells:
        b = base_fn(x, z)
        for y in range(int(b), 40):
            if (x, y, z) in P.b and (x, y, z) not in P.locked:
                P.b[(x, y, z)] = AIR
                P.be.pop((x, y, z), None)


def clean_floaters(P, box, passes=2):
    """Drop single blocks left hanging in mid-air by collapses (keeps the ruin plausible)."""
    x0, y0, z0, x1, y1, z1 = box
    for _ in range(passes):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                for y in range(max(2, y0), y1 + 1):
                    s = P.b.get((x, y, z))
                    if s is None or s == AIR or (x, y, z) in P.locked:
                        continue
                    n = state_name(s)
                    if "chain" in n or "lantern" in n or "banner" in n or "wool" in n or n == WEB:
                        continue
                    sup = 0
                    for d in ((0, -1, 0), (1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, 1, 0)):
                        t = P.b.get((x + d[0], y + d[1], z + d[2]))
                        if t is not None and t != AIR and t != WEB:
                            sup += 1
                    if sup == 0:
                        P.b[(x, y, z)] = AIR


BOXES = {
    "courtyard":   (0, 0, 0, 36, 16, 30),
    "gatehouse":   (6, 0, 31, 30, 22, 44),
    "barracks":    (-18, 0, 3, -1, 22, 27),
    "east_tower":  (37, 0, 9, 47, 22, 21),
    "antechamber": (11, 0, -9, 25, 14, -1),
    "wall_west":   (0, 0, -9, 10, 17, -1),
    "wall_east":   (26, 0, -9, 36, 17, -1),
    "throne_hall": (0, 0, -47, 36, 42, -10),
    "royal_vault": (37, 0, -44, 47, 12, -32),
}
OFFERINGS_BOX = (15, 2, -28, 21, 2, -22)
PIECE_POOL = {"courtyard": "start", "gatehouse": "gatehouse", "barracks": "barracks", "east_tower": "towers",
              "antechamber": "antechamber", "wall_west": "walls", "wall_east": "walls",
              "throne_hall": "throne_hall", "royal_vault": "royal_vault", "offerings": "offerings"}

# (parent, child, parent jigsaw pos, parent front, connection name)
CONNECTIONS = [
    ("courtyard", "gatehouse", (18, 0, 30), "south", "gate"),
    ("courtyard", "barracks", (0, 0, 15), "west", "barracks"),
    ("courtyard", "east_tower", (36, 0, 15), "east", "east_tower"),
    ("courtyard", "antechamber", (18, 0, 0), "north", "antechamber"),
    ("antechamber", "wall_west", (11, 0, -5), "west", "wall_west"),
    ("antechamber", "wall_east", (25, 0, -5), "east", "wall_east"),
    ("antechamber", "throne_hall", (18, 0, -9), "north", "throne_hall"),
    ("throne_hall", "royal_vault", (36, 0, -37), "east", "royal_vault"),
]
ALTAR_POS = (18, 1, -25)
ARENA = (8, -35, 28, -15)  # x0, z0, x1, z1 (21 x 21 around the altar)
ARENA_CLEAR = 10            # clear blocks above the floor inside the arena (validate.py requires >= 9)


def yard_floor(x, z):
    r = h3(x, 0, z, 7)
    path = 16 <= x <= 20 or 13 <= z <= 17
    if path:
        return PD if r < .55 else (DT if r < .82 else (CDT if r < .94 else CD))
    n = vnoise(x, z, 5.0, 3)
    if n > .78:
        return MOSS if r < .75 else COARSE
    if n > .70:
        return CD if r < .5 else (MOSS if r < .7 else COARSE)
    if n < .18:
        return TUFF if r < .6 else CD
    return pick([(CD, 34), (DT, 24), (PD, 16), (CDT, 12), (TUFF, 10), (GRAVEL, 3)], r)


def build_courtyard(P):
    for x in range(0, 37):
        for z in range(0, 31):
            P.put(x, 0, z, yard_floor(x, z))
    P.air(3, 1, 3, 33, 13, 27)
    # curtain walls (3 thick, wall walk at y=8, parapet outside)
    curtain(P, 0, 0, 2, 30, "west")
    curtain(P, 34, 0, 36, 30, "east")
    curtain(P, 0, 0, 36, 2, "north")
    curtain(P, 0, 28, 36, 30, "south")
    # inner face pilasters
    for z in (6, 12, 18, 24):
        for y in range(1, 8):
            P.put(2, y, z, CHD if y == 6 else PD)
            P.put(34, y, z, CHD if y == 6 else PD)
    for x in (6, 12, 24, 30):
        for y in range(1, 8):
            P.put(x, y, 2, CHD if y == 6 else PD)
            P.put(x, y, 28, CHD if y == 6 else PD)
    # gate (south) and keep door (north)
    P.air(16, 1, 28, 20, 6, 30)
    for z in (28, 29, 30):
        arch_x(P, 16, 20, z, 6)
    P.air(16, 1, 0, 20, 5, 2)
    for z in (0, 1, 2):
        arch_x(P, 16, 20, z, 5)
    for x in range(16, 21):
        for z in range(0, 3):
            P.put(x, 0, z, PD if (x + z) % 3 else PBB)
        for z in range(28, 31):
            P.put(x, 0, z, PD if (x + z) % 3 else PBB)
    # side doors: barracks (west) and east tower
    for xs in ((0, 2), (34, 36)):
        P.air(xs[0], 1, 14, xs[1], 3, 16)
        for x in range(xs[0], xs[1] + 1):
            arch_z(P, x, 14, 16, 4)
            for z in range(14, 17):
                P.put(x, 0, z, PD)
    # stair runs to the wall walks (2 wide, along the inner faces, rising north)
    for (xa, xb) in ((3, 4), (32, 33)):
        for y in range(1, 9):
            z = 12 - y
            for x in (xa, xb):
                for yy in range(1, y):
                    P.put(x, yy, z, mason(x, yy, z))
                P.put(x, y, z, stairs("deepslate_brick", "north"))
        for x in (xa, xb):  # landing
            for yy in range(1, 8):
                P.put(x, yy, 3, mason(x, yy, 3))
            P.put(x, 8, 3, PD)
    P.passage("courtyard west stair", [(x, y + 1, 12 - y) for y in range(1, 9) for x in (3, 4)], 3)
    P.passage("courtyard east stair", [(x, y + 1, 12 - y) for y in range(1, 9) for x in (32, 33)], 3)
    # corner towers SW / SE (hollow, rooms at ground and wall-walk level)
    for (tx0, tx1) in ((0, 6), (30, 36)):
        tz0, tz1 = 24, 30
        shell(P, tx0, tz0, tx1, tz1, 1, 13)
        P.air(tx0 + 1, 1, tz0 + 1, tx1 - 1, 7, tz1 - 1)
        P.air(tx0 + 1, 9, tz0 + 1, tx1 - 1, 12, tz1 - 1)
        P.fill(tx0 + 1, 8, tz0 + 1, tx1 - 1, 8, tz1 - 1, lambda x, y, z: PD)
        P.fill(tx0 + 1, 13, tz0 + 1, tx1 - 1, 13, tz1 - 1, lambda x, y, z: DT)
        P.fill(tx0 + 1, 0, tz0 + 1, tx1 - 1, 0, tz1 - 1, lambda x, y, z: floor_tiles(x, z))
        battlement_ring(P, tx0, tz0, tx1, tz1, 14)
        for y in range(1, 14):  # corner quoins
            for (qx, qz) in ((tx0, tz0), (tx1, tz0), (tx0, tz1), (tx1, tz1)):
                P.put(qx, y, qz, PD if y % 2 else PBB if y < 3 else DB)
    # SW tower doors: yard (east face) + wall walks
    P.air(6, 1, 26, 6, 3, 26)
    P.air(1, 9, 24, 2, 11, 24)
    P.air(6, 9, 28, 6, 11, 29)
    P.passage("SW tower yard door", [(6, 1, 26), (5, 1, 26), (7, 1, 26)], 3)
    P.passage("SW tower walk door", [(1, 9, 24), (2, 9, 24), (6, 9, 28), (6, 9, 29)], 3)
    # SE tower door (yard, west face) and wall-walk doors
    P.air(30, 1, 26, 30, 3, 26)
    P.air(34, 9, 24, 35, 11, 24)
    P.passage("SE tower yard door", [(30, 1, 26), (29, 1, 26), (31, 1, 26)], 3)
    # collapsed fountain
    build_fountain(P, 18, 15)
    # banner poles along the processional way
    for (px, pz, broken) in ((13, 8, False), (23, 8, False), (13, 22, False), (23, 22, True)):
        top = 3 if broken else 8
        for y in range(1, top + 1):
            P.put(px, y, pz, wallb("polished_blackstone"))
        if not broken:
            P.put(px, 9, pz, lantern())
            P.put(px, 7, pz - 1, wall_banner("north"), {"kind": "banner", "patterns": VAEL_BANNER})
            P.put(px, 7, pz + 1, wall_banner("south"), {"kind": "banner", "patterns": VAEL_BANNER_TORN})
            P.put(px, 1, pz, CHPB)
        else:  # fallen pole and its cloth across the yard
            for k in range(1, 6):
                P.put(px + k, 1, pz, slab("polished_blackstone_brick"))
            for (cx, cz) in ((px + 2, pz + 1), (px + 3, pz + 1), (px + 4, pz + 1), (px + 3, pz + 2), (px + 5, pz - 1)):
                P.deco(cx, 1, cz, carpet("blue" if (cx + cz) % 3 else "black"))
    # faded drapes on the inner face of the north wall flanking the keep door
    drape(P, 11, 13, 3, 7, [5, 4, 3], "south", 41)
    drape(P, 23, 25, 3, 7, [3, 5, 4], "south", 42)
    # wall-walk clear zones and main path
    P.clear_zones.append((15, 1, 0, 21, 6, 30))      # processional way
    P.clear_zones.append((3, 1, 13, 33, 4, 17))      # cross path to side doors
    P.passage("courtyard gate", [(x, 1, z) for x in range(16, 21) for z in range(28, 31)], 4)
    P.passage("courtyard keep door", [(x, 1, z) for x in range(16, 21) for z in range(0, 3)], 4)
    P.passage("courtyard barracks door", [(x, 1, z) for x in range(0, 3) for z in range(14, 17)], 3)
    P.passage("courtyard east tower door", [(x, 1, z) for x in range(34, 37) for z in range(14, 17)], 3)
    # ruin: east wall breach, SE tower collapse, crenels missing
    breach = [(x, z) for x in range(31, 37) for z in range(18, 24)]
    collapse(P, breach, lambda x, z: 3 + abs(z - 20.5) * 1.6 + (h3(x, 0, z, 77) * 1.5) + (0 if x >= 34 else 9), 77)
    se = [(x, z) for x in range(30, 37) for z in range(24, 31)]
    collapse(P, se, lambda x, z: 15 - max(0, (x - 30) + (z - 24) - 4) * 0.9 + h3(x, 0, z, 78) * 2, 78)
    nw_crumble = [(x, z) for x in range(0, 3) for z in range(4, 12)]
    collapse(P, nw_crumble, lambda x, z: 10 if z % 3 else 9, 79)
    rubble(P, 31.5, 21.0, 3.2, 3.6, 2, 101, top=12)
    rubble(P, 30.0, 27.0, 2.0, 2.0, 1, 102, top=12)
    rubble(P, 8.0, 24.5, 2.5, 1.6, 1, 103, top=12)
    rubble(P, 27.5, 6.0, 2.0, 1.5, 1, 104, top=12)
    moss_floor(P, 3, 3, 33, 27, 12, .2, 105)
    for (x, z) in ((5, 23), (28, 27), (33, 12), (3, 18)):
        P.deco(x, 1, z, WEB)


def build_fountain(P, cx, cz):
    cells = []
    for x in range(cx - 5, cx + 6):
        for z in range(cz - 5, cz + 6):
            d = math.hypot(x - cx, z - cz)
            cells.append((x, z, d))
    broken = lambda x, z: (x - cx) > 0 and (z - cz) > 0 and h3(x, 0, z, 61) < .85
    for x, z, d in cells:
        if d < 3.5:
            P.put(x, 0, z, PB if h3(x, 0, z, 60) > .2 else BS)
        if 3.5 <= d < 4.6:
            if broken(x, z):
                if h3(x, 1, z, 62) < .5:
                    P.put(x, 1, z, CPBB)
                continue
            P.put(x, 1, z, PBB if h3(x, 1, z, 63) > .15 else CPBB)
            if h3(x, 2, z, 64) < .7:
                P.put(x, 2, z, slab("polished_blackstone_brick"))
        elif 4.6 <= d < 5.4:
            P.put(x, 0, z, PBB)
    # plinth and broken statue column
    P.fill(cx - 1, 1, cz - 1, cx + 1, 1, cz + 1, lambda x, y, z: CHPB if (x, z) == (cx, cz) else PBB)
    P.put(cx, 2, cz, CHD)
    P.put(cx, 3, cz, wallb("deepslate_brick"))
    P.put(cx, 4, cz, stairs("deepslate_brick", "east", "bottom"))
    # water in the intact north half, dammed by moss and rubble along z = cz
    for x, z, d in cells:
        if d < 3.5 and not (abs(x - cx) <= 1 and abs(z - cz) <= 1):
            if z < cz:
                P.put(x, 1, z, WATER)
            elif z == cz:
                P.put(x, 1, z, MOSS if h3(x, 1, z, 65) < .6 else CD)
            else:
                r = h3(x, 1, z, 66)
                P.put(x, 1, z, MOSS if r < .3 else (CD if r < .55 else (GRAVEL if r < .7 else AIR)))
                if r > .75:
                    P.put(x, 1, z, MOSSC)
    # fallen statue pieces in the broken quadrant
    for (x, y, z, s) in ((cx + 3, 1, cz + 3, CHD), (cx + 4, 1, cz + 3, PD), (cx + 2, 1, cz + 4, stairs("polished_deepslate", "south")),
                         (cx + 5, 1, cz + 2, slab("deepslate_brick"))):
        P.put(x, y, z, s)
    # keep the water enclosed
    for (x, y, z), s in list(P.b.items()):
        if s == WATER:
            for d in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = P.get(x + d[0], y, z + d[1])
                if n is None or n == AIR or n == MOSSC:
                    P.put(x + d[0], y, z + d[1], PBB)


def build_gatehouse(P):
    # ground course under the gatehouse and approach
    for x in range(8, 29):
        for z in range(31, 40):
            P.put(x, 0, z, PBB if not (16 <= x <= 20) else (PD if h3(x, 0, z, 9) > .2 else PBB))
    for x in range(14, 23):
        for z in range(40, 45):
            r = h3(x, 0, z, 12)
            if 15 <= x <= 21:
                P.put(x, 0, z, PD if r < .45 else (CD if r < .7 else (CDT if r < .85 else TUFF)))
    # towers
    for (x0, x1) in ((8, 15), (21, 28)):
        shell(P, x0, 31, x1, 39, 1, 19)
        P.air(x0 + 1, 1, 32, x1 - 1, 6, 38)
        P.air(x0 + 1, 8, 32, x1 - 1, 11, 38)
        P.air(x0 + 1, 13, 32, x1 - 1, 18, 38)
        P.fill(x0 + 1, 0, 32, x1 - 1, 0, 38, lambda x, y, z: floor_tiles(x, z, 2))
        P.fill(x0 + 1, 7, 32, x1 - 1, 7, 38, lambda x, y, z: PD if (x + z) % 5 else DT)
        P.fill(x0 + 1, 12, 32, x1 - 1, 12, 38, lambda x, y, z: DT)
        P.fill(x0 + 1, 19, 32, x1 - 1, 19, 38, lambda x, y, z: DT)
        battlement_ring(P, x0, 31, x1, 39, 20)
        for y in range(1, 20):
            for (qx, qz) in ((x0, 39), (x1, 39), (x0, 31), (x1, 31)):
                P.put(qx, y, qz, PD if y % 2 else (PBB if y < 3 else DB))
        # machicolation corbels under the south parapet
        for x in range(x0, x1 + 1):
            if (x - x0) % 2 == 0:
                P.put(x, 18, 40, stairs("deepslate_brick", "north", "top"))
        # slit windows
        for z in (34, 36):
            for y in (3, 4, 9, 10, 14):
                P.put(x0 if x0 == 8 else x1, y, z, AIR if y != 14 else BARS)
        for x in (x0 + 2, x1 - 2):
            for y in (9, 10):
                P.put(x, y, 39, BARS)
    # passage and gate block
    P.air(16, 1, 31, 20, 6, 39)
    for x in range(16, 21):
        for z in range(31, 40):
            P.put(x, 7, z, PD if (x + z) % 4 else DT)
            for y in range(8, 13):
                if z in (31, 39) or y == 12:
                    P.put(x, y, z, mason(x, y, z))
    P.air(16, 8, 32, 20, 11, 38)
    for x in range(16, 21):
        P.put(x, 13, 39, mason(x, 13, 39))
        P.put(x, 13, 31, mason(x, 13, 31))
        if x % 2 == 0:
            P.put(x, 14, 39, mason(x, 14, 39))
            P.put(x, 14, 31, mason(x, 14, 31))
    for z in (31, 39):
        arch_x(P, 16, 20, z, 6)
    # murder holes as grates in the passage ceiling
    P.put(17, 7, 35, BARS)
    P.put(19, 7, 35, BARS)
    # portcullis: outer one jammed half open (bars at y 5..6, one bar missing), inner one raised
    for x in range(16, 21):
        P.put(x, 6, 37, BARS)
        if x != 18:
            P.put(x, 5, 37, BARS)
        P.put(x, 6, 33, BARS)
    # front window and banner over the gate
    for x in (17, 18, 19):
        P.put(x, 9, 39, BARS)
        P.put(x, 10, 39, BARS)
    P.put(18, 12, 40, wall_banner("south"), {"kind": "banner", "patterns": VAEL_BANNER})
    # doors: passage -> towers (ground), gate chamber -> towers (mid), gate roof -> towers (top)
    for x in (15, 21):
        P.air(x, 1, 35, x, 3, 35)
        P.air(x, 8, 35, x, 10, 35)
        P.air(x, 13, 35, x, 15, 35)
    P.passage("gate passage", [(x, 1, z) for x in range(16, 21) for z in range(31, 40)], 4)
    P.passage("gate tower ground doors", [(15, 1, 35), (21, 1, 35), (14, 1, 35), (22, 1, 35)], 3)
    P.passage("gate chamber doors", [(15, 8, 35), (21, 8, 35), (14, 8, 35), (22, 8, 35)], 3)
    P.passage("gate roof doors", [(15, 13, 35), (21, 13, 35)], 3)
    # tower stairs: ground -> mid (x 9..10 rising north), mid -> top (x 26..27 rising south)
    for (xa, xb) in ((9, 10), (26, 27)):
        for y in range(1, 8):
            z = 39 - y
            for x in (xa, xb):
                for yy in range(1, y):
                    P.put(x, yy, z, mason(x, yy, z))
                P.put(x, y, z, stairs("deepslate_tile", "north"))
        for z in (33, 34, 35):
            for x in (xa, xb):
                P.put(x, 7, z, AIR)
        P.passage(f"gate tower stair {xa}", [(x, y + 1, 39 - y) for y in range(1, 8) for x in (xa, xb)], 3)
    for (xs, f) in ((range(9, 14), "east"), (range(27, 22, -1), "west")):
        xs = list(xs)
        for i, x in enumerate(xs):
            y = 8 + i
            for z in (37, 38):
                for yy in range(8, y):
                    P.put(x, yy, z, mason(x, yy, z))
                P.put(x, y, z, stairs("deepslate_tile", f))
        for x in xs[1:-1]:
            for z in (37, 38):
                P.put(x, 12, z, AIR)
        P.passage(f"gate tower upper stair {xs[0]}", [(x, 9 + i, z) for i, x in enumerate(xs) for z in (37, 38)], 3)
    # sentinel posts: niches in front of the towers flanking the gate
    for nx in (12, 24):
        for y in range(1, 4):
            P.put(nx - 1, y, 40, CHPB if y == 2 else PBB)
            P.put(nx + 1, y, 40, CHPB if y == 2 else PBB)
        P.put(nx, 0, 40, CHPB)
        P.put(nx, 4, 40, stairs("polished_blackstone_brick", "north", "top"))
        P.put(nx - 1, 4, 40, lantern())
        P.put(nx + 1, 4, 40, lantern())
        P.put(nx, 7, 40, wall_banner("south"), {"kind": "banner", "patterns": VAEL_BANNER})
        P.air(nx, 1, 40, nx, 3, 40)
        P.entity("mob", (nx + .5, 1.0, 40.5), 0.0, id=f"{ENTITY_NS}:hollow_sentinel")
    # broken barbican stubs and rubble on the approach
    for x in list(range(7, 14)) + list(range(23, 30)):
        h = int(1 + 2.5 * vnoise(x, 44, 2.5, 13))
        for y in range(1, h + 1):
            P.put(x, y, 44, wallb("deepslate_brick") if y == h else mason(x, y, 44))
        P.put(x, 0, 44, PBB)
    # collapse: east tower top
    cells = [(x, z) for x in range(21, 29) for z in range(31, 40)]
    collapse(P, cells, lambda x, z: 21 - max(0, (x - 21) * .9 + (z - 31) * .7 - 3) + h3(x, 0, z, 81) * 2, 81)
    rubble(P, 27.0, 42.0, 3.0, 2.0, 2, 111, top=10)
    rubble(P, 9.0, 42.5, 2.2, 1.5, 1, 112, top=10)
    rubble(P, 29.5, 36.0, 1.5, 3.0, 2, 113, top=10)
    moss_floor(P, 6, 40, 30, 44, 4, .25, 114)
    cobwebs(P, 9, 1, 32, 27, 15, 38, .35, 115)
    P.clear_zones.append((15, 1, 40, 21, 4, 44))


def build_barracks(P):
    X0, X1, Z0, Z1 = -16, -1, 4, 26
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            P.put(x, 0, z, PBB if x == X0 or z in (Z0, Z1) else floor_tiles(x, z, 4))
    # walls: west / north / south full height; east above the courtyard wall walk only
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            edge = x == X0 or z in (Z0, Z1)
            for y in range(1, 14):
                if edge:
                    P.put(x, y, z, mason(x, y, z, band=8))
                elif x == X1 and y >= 9:
                    P.put(x, y, z, mason(x, y, z, band=8))
                else:
                    P.put(x, y, z, AIR)
    # upper floor (y=8): dark oak planks on log beams
    for x in range(X0 + 1, X1 + 1):
        for z in range(Z0 + 1, Z1):
            P.put(x, 8, z, LOG_X if z % 5 == 0 else PLANKS)
    for z in range(Z0 + 1, Z1):
        if z % 5 == 0:
            for x in range(X0 + 1, X1 + 1):
                if not (z == 5 and -15 <= x <= -8):  # keep the beam clear of the stair run
                    P.put(x, 7, z, LOG_X)
    # gable roof (ridge along z): deepslate tile stairs over a dark oak rafter layer, masonry gables
    for k in range(0, 9):
        yw = 14 + k
        xw, xe = -17 + k, -1 - k
        for z in range(3, 28):
            if xw < xe:
                P.put(xw, yw, z, stairs("deepslate_tile", "east"))
                P.put(xe, yw, z, stairs("deepslate_tile", "west"))
            elif xw == xe:
                P.put(xw, yw, z, slab("deepslate_tile"))
            for x in range(xw + 1, xe):
                if z in (Z0, Z1):
                    P.put(x, yw, z, mason(x, yw, z, band=8))
                elif Z0 < z < Z1:
                    P.put(x, yw, z, PLANKS if x in (xw + 1, xe - 1) else AIR)
    for x in range(X0 + 1, X1):  # open attic above the upper floor
        for z in range(Z0 + 1, Z1):
            for y in range(9, 14):
                if P.get(x, y, z) is None:
                    P.put(x, y, z, AIR)
    # buttresses on the west face
    for z in (6, 11, 16, 21, 25):
        for y in range(1, 12):
            P.put(-17, y, z, mason(-17, y, z, band=8))
        P.put(-17, 12, z, stairs("deepslate_brick", "east"))
    # windows (ground y 3..5, upper y 10..12) between buttresses
    for z in (8, 13, 18, 23):
        for y in (3, 4, 5, 10, 11, 12):
            P.put(X0, y, z, BARS if y in (4, 11) else AIR)
    for x in (-12, -5):
        for y in (3, 4, 10, 11):
            P.put(x, y, Z0, AIR)
            P.put(x, y, Z1, AIR)
    for y in (16, 17):
        P.put(-9, y, Z0, AIR)
        P.put(-9, y, Z1, AIR)
    # stair run ground -> upper floor along the north wall (rising east)
    for x in range(-15, -7):
        y = x + 16
        for z in (5, 6):
            for yy in range(1, y):
                P.put(x, yy, z, mason(x, yy, z, band=8))
            P.put(x, y, z, stairs("deepslate_brick", "east"))
    for x in (-11, -10, -9):
        for z in (5, 6):
            P.put(x, 8, z, AIR)
    for x in (-11, -10, -9, -8):
        P.put(x, 9, 7, FENCE)
    P.passage("barracks stair", [(x, x + 17, z) for x in range(-15, -7) for z in (5, 6)], 3)
    # bunks along the west wall (both floors) and on the upper floor's east side
    for fy in (1, 9):
        for z in range(9, 25, 2):
            for x in (-15, -14):
                P.put(x, fy, z, slab("dark_oak", "top"))
                P.put(x, fy + 2, z, slab("dark_oak", "top"))
                r = h3(x, fy, z, 120)
                if r < .75:
                    P.put(x, fy + 1, z, carpet("blue" if r < .4 else ("black" if r < .6 else "gray")))
                r = h3(x, fy + 3, z, 121)
                if r < .55:
                    P.put(x, fy + 3, z, carpet("blue" if r < .3 else "black"))
                elif r < .7:
                    P.put(x, fy + 3, z, WEB)
            for y in range(fy, fy + 3):
                P.put(-13, y, z, FENCE)
    for z in (10, 12, 21, 23, 25):
        for x in (-3, -2):
            P.put(x, 9, z, slab("dark_oak", "top"))
            if h3(x, 10, z, 122) < .7:
                P.put(x, 10, z, carpet("blue"))
    # table and benches
    for x in (-9, -8):
        for z in range(16, 20):
            P.put(x, 1, z, slab("dark_oak", "top"))
    for z in range(16, 20):
        P.put(-10, 1, z, stairs("dark_oak", "west"))
        P.put(-7, 1, z, stairs("dark_oak", "east") if z != 18 else AIR)
    P.put(-9, 2, 17, lantern())
    P.put(-8, 2, 19, M("candle[candles=3,lit=false]"))
    P.put(-8, 2, 16, M("candle[candles=1,lit=false]"))
    # hearth on the north wall (unlit)
    for y in (1, 2, 3):
        P.put(-5, y, 5, PBB)
        P.put(-3, y, 5, PBB)
    P.put(-4, 1, 5, M("campfire[lit=false,facing=south,signal_fire=false]"))
    P.put(-4, 3, 5, stairs("polished_blackstone_brick", "south", "top"))
    P.put(-4, 2, 5, AIR)
    # armory: armor stands on the east side, workstations
    for z in (7, 9, 11, 19, 21, 23):
        full = h3(0, 0, z, 123)
        P.entity("armor_stand", (-1.5, 1.0, z + .5), 90.0,
                 armor=[None, "minecraft:chainmail_leggings" if full < .5 else None,
                        "minecraft:chainmail_chestplate", "minecraft:iron_helmet" if full > .3 else "minecraft:chainmail_helmet"],
                 hands=["minecraft:iron_sword" if full < .6 else "minecraft:crossbow", None], damaged=True)
    P.put(-2, 1, 25, M("grindstone[face=floor,facing=north]"))
    P.put(-3, 1, 25, M("chipped_anvil[facing=west]"))
    P.put(-15, 1, 25, M("barrel[facing=up,open=false]"))
    P.put(-14, 1, 25, M("barrel[facing=up,open=false]"))
    P.put(-15, 2, 25, M("barrel[facing=north,open=false]"))
    P.put(-13, 1, 25, M("fletching_table"))
    P.put(-8, 1, 25, M("chest[facing=north,type=single]"), {"kind": "chest", "loot": LOOT_BARRACKS})
    # hanging soul lanterns from the beams
    for (x, z) in ((-11, 10), (-6, 15), (-11, 20), (-5, 25)):
        if P.get(x, 7, z) == LOG_X:
            P.put(x, 6, z, lantern(True))
    # upper floor collapse with rubble below
    hole = [(x, z) for x in range(-11, -6) for z in range(10, 14)]
    for (x, z) in hole:
        if h3(x, 8, z, 124) < .85 or (x, z) == (-9, 12):
            P.put(x, 8, z, AIR)
            if P.get(x, 7, z) == LOG_X and h3(x, 7, z, 125) < .5:
                P.put(x, 7, z, AIR)
    rubble(P, -9.0, 11.5, 2.4, 2.0, 2, 126, top=7)
    for (x, z) in ((-10, 10), (-8, 13)):
        P.deco(x, P.ground(x, z, 7), z, slab("dark_oak"))
    # roof collapse (west slope middle, east slope south) and gable damage
    roof_hole = [(x, z) for x in range(-17, -1) for z in range(11, 20)]
    for (x, z) in roof_hole:
        if vnoise(x, z, 3.0, 127) > .38 and x < -9:
            for y in range(14, 23):
                if P.get(x, y, z):
                    P.put(x, y, z, AIR)
    for (x, z) in [(x, z) for x in range(-9, 0) for z in range(18, 25)]:
        if vnoise(x, z, 2.5, 128) > .45:
            for y in range(15, 23):
                if P.get(x, y, z):
                    P.put(x, y, z, AIR)
    rubble(P, -12.0, 15.0, 2.5, 3.0, 1, 129, top=12)
    rubble(P, -17.5, 14.5, 0.8, 3.5, 2, 130, top=8)
    # door to the courtyard wall walk (upper floor) through the barracks east wall
    P.air(-1, 9, 18, -1, 11, 19)
    P.air(0, 9, 18, 0, 10, 19)
    P.passage("barracks walk door", [(-1, 9, 18), (-1, 9, 19), (0, 9, 18), (0, 9, 19), (-2, 9, 18), (-2, 9, 19)], 3)
    P.passage("barracks ground aisle", [(-1, 1, z) for z in range(13, 18)] + [(x, 1, 15) for x in range(-12, 0)], 3)
    cobwebs(P, -15, 6, 5, -2, 7, 25, .08, 131)
    cobwebs(P, -15, 12, 5, -2, 20, 25, .1, 132)
    moss_floor(P, -18, 3, -17, 27, 6, .4, 133)


def build_east_tower(P):
    cx, cz = 42, 15
    for x in range(37, 48):
        for z in range(9, 22):
            d = math.hypot(x - cx, z - cz)
            if d < 5.45:
                P.put(x, 0, z, PBB if d >= 3.3 else floor_tiles(x, z, 6))
            if 3.3 <= d < 5.45:
                for y in range(1, 17):
                    P.put(x, y, z, mason(x, y, z))
            elif d < 3.3:
                P.air(x, 1, z, x, 7, z)
                P.put(x, 8, z, PD if h3(x, 8, z, 140) > .2 else DT)
                P.air(x, 9, z, x, 15, z)
                P.put(x, 16, z, DT)
            if 4.4 <= d < 5.45:
                P.put(x, 17, z, mason(x, 17, z))
                ang = math.atan2(z - cz, x - cx)
                if int((ang + math.pi) / (2 * math.pi) * 16) % 2 == 0:
                    P.put(x, 18, z, mason(x, 18, z))
            if 3.3 <= d < 4.4 and h3(x, 16, z, 141) < .3:
                P.put(x, 16, z, PD)
    # slit windows
    for (x, z) in ((42, 10), (47, 15), (42, 20), (45, 11), (45, 19)):
        for y in (4, 5, 12, 13):
            P.put(x, y, z, AIR if y in (4, 12) else BARS)
    # ground door (from the courtyard) and wall-walk door
    P.air(37, 1, 14, 38, 3, 16)
    for z in (14, 15, 16):
        P.put(37, 0, z, PD)
        P.put(38, 0, z, PD)
    P.air(36, 9, 15, 38, 11, 15)
    P.passage("east tower ground door", [(x, 1, z) for x in (37, 38, 39) for z in (14, 15, 16)], 3)
    P.passage("east tower walk door", [(x, 9, 15) for x in (35, 36, 37, 38, 39)], 3)
    # collapse of the east half
    cells = [(x, z) for x in range(40, 48) for z in range(9, 22)]
    collapse(P, cells, lambda x, z: 5 + 9 * vnoise(x, z, 3.0, 142) - max(0, x - 43) * 1.3 + (8 if x < 42 else 0), 142)
    for (x, z) in cells:
        if x >= 43 and P.get(x, 8, z) in (PD, DT) and h3(x, 8, z, 143) < .8:
            P.put(x, 8, z, AIR)
    rubble(P, 43.0, 15.0, 2.6, 2.6, 2, 144, top=8)
    rubble(P, 46.5, 10.0, 1.0, 1.2, 2, 145, top=8)
    rubble(P, 46.5, 20.0, 1.0, 1.2, 1, 146, top=8)
    cobwebs(P, 38, 5, 11, 46, 15, 19, .3, 147)


def build_antechamber(P):
    X0, X1, Z0, Z1 = 11, 25, -9, -1
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            edge = x in (X0, X1) or z in (Z0, Z1)
            P.put(x, 0, z, PBB if edge else (PD if 16 <= x <= 20 else floor_tiles(x, z, 8)))
            for y in range(1, 10):
                P.put(x, y, z, mason(x, y, z) if edge else AIR)
            P.put(x, 10, z, DT)
    battlement_ring(P, X0, Z0, X1, Z1, 11, 1)
    for (x, z) in ((12, -8), (24, -8), (12, -2), (24, -2)):
        for y in range(1, 10):
            P.put(x, y, z, CHD if y in (1, 9) else PD)
    for x in range(13, 24):  # ceiling cornice
        P.put(x, 9, -8, stairs("deepslate_tile", "north", "top"))
        P.put(x, 9, -2, stairs("deepslate_tile", "south", "top"))
    # doors
    P.air(16, 1, Z1, 20, 5, Z1)
    arch_x(P, 16, 20, Z1, 5)
    P.air(16, 1, Z0, 20, 7, Z0)
    arch_x(P, 16, 20, Z0, 7)
    for x in (X0, X1):  # side doors to the connector yards, 3 tall with arch corners above
        P.air(x, 1, -6, x, 3, -4)
        arch_z(P, x, -6, -4, 4)
    P.passage("antechamber south door", [(x, 1, z) for x in range(16, 21) for z in (-1, -2)], 4)
    P.passage("antechamber north door", [(x, 1, z) for x in range(16, 21) for z in (-9, -8)], 4)
    P.passage("antechamber side doors", [(x, 1, z) for x in (10, 11, 12, 24, 25, 26) for z in (-6, -5, -4)], 3)
    P.passage("antechamber hall", [(x, 1, z) for x in range(16, 21) for z in range(-8, -1)], 4)
    # statues on pedestals and the knight
    for (sx, yaw) in ((13, 270.0), (23, 90.0)):
        P.put(sx, 1, -5, CHPB)
        P.put(sx, 1, -6, slab("polished_blackstone_brick"))
        P.put(sx, 1, -4, slab("polished_blackstone_brick"))
        P.entity("armor_stand", (sx + .5, 2.0, -4.5), yaw, armor=["minecraft:iron_boots", "minecraft:iron_leggings",
                 "minecraft:iron_chestplate", "minecraft:iron_helmet"], hands=["minecraft:iron_sword", None],
                 pose={"RightArm": [-110.0, 0.0, 0.0], "Head": [10.0, 0.0, 0.0]}, statue=True)
    P.entity("mob", (18.5, 1.0, -5.5), 0.0, id=f"{ENTITY_NS}:soulbound_knight")
    # runner carpet (torn), banners, lanterns
    for z in range(-8, -1):
        for x in (17, 18, 19):
            if h3(x, 1, z, 150) < .82:
                P.put(x, 1, z, carpet("blue" if x == 18 else "black"))
    for (bx, bz, f) in ((14, -8, "south"), (22, -8, "south"), (14, -2, "north"), (22, -2, "north")):
        P.put(bx, 7, bz, wall_banner(f), {"kind": "banner", "patterns": VAEL_BANNER if bx == 14 else VAEL_BANNER_TORN})
    for x in (15, 21):
        P.put(x, 9, -5, CHAIN)
        P.put(x, 8, -5, lantern(True))
    # partial ceiling collapse at the east end
    cells = [(x, z) for x in range(20, 26) for z in range(-9, -5)]
    collapse(P, cells, lambda x, z: 10 if vnoise(x, z, 2.0, 151) > .35 else 40, 151)
    for (x, z) in cells:
        if x == 25 and vnoise(x, z, 2.0, 151) > .55:
            for y in range(7, 13):
                if P.get(x, y, z):
                    P.put(x, y, z, AIR)
    rubble(P, 22.5, -7.5, 1.8, 1.0, 1, 152, top=9)
    cobwebs(P, 12, 7, -8, 24, 9, -2, .3, 153)


def build_connector(P, east):
    """West connector (east=False) in x 0..10; the east one is its mirror (x -> 36 - x), more ruined."""
    Q = Plan()
    # tower 7x7 at x 0..6, z -7..-1
    shell(Q, 0, -7, 6, -1, 1, 13)
    Q.fill(1, 0, -6, 5, 0, -2, lambda x, y, z: floor_tiles(x, z, 10))
    Q.air(1, 1, -6, 5, 7, -2)
    Q.fill(1, 8, -6, 5, 8, -2, lambda x, y, z: PD)
    Q.air(1, 9, -6, 5, 12, -2)
    Q.fill(1, 13, -6, 5, 13, -2, lambda x, y, z: DT)
    battlement_ring(Q, 0, -7, 6, -1, 14, 1)
    for y in range(1, 14):
        for (qx, qz) in ((0, -7), (6, -7), (0, -1), (6, -1)):
            Q.put(qx, y, qz, PD if y % 2 else (PBB if y < 3 else DB))
    for x in range(0, 7):
        Q.put(x, 0, -7, PBB)
        Q.put(x, 0, -1, PBB)
    # thick link wall to the throne hall (z -9..-8) with walk at y=8
    for x in range(0, 11):
        for z in (-9, -8):
            Q.put(x, 0, z, PBB)
            for y in range(1, 8):
                Q.put(x, y, z, mason(x, y, z))
            Q.put(x, 8, z, PD)
        Q.put(x, 9, -9, mason(x, 9, -9))
        if x % 2 == 0:
            Q.put(x, 10, -9, mason(x, 10, -9))
    # side yard x 7..10, z -7..-1
    for x in range(7, 11):
        for z in range(-7, 0):
            r = h3(x, 0, z, 160)
            Q.put(x, 0, z, MOSS if r < .2 else (COARSE if r < .35 else (CD if r < .7 else DT)))
            Q.air(x, 1, z, x, 7, z)
    # doors: tower ground door to the yard, walk door from the courtyard north wall walk
    Q.air(6, 1, -4, 6, 3, -4)
    Q.air(3, 9, -1, 3, 11, -1)
    Q.passage("connector tower door", [(6, 1, -4), (5, 1, -4), (7, 1, -4)], 3)
    Q.passage("connector walk door", [(3, 9, 0), (3, 9, -1), (3, 9, -2)], 3)
    Q.passage("connector yard", [(x, 1, -5) for x in range(7, 11)], 3)
    # lean-to ruin in the yard
    for z in range(-7, -1):
        Q.put(7, 4, z, stairs("deepslate_tile", "west"))
        if h3(8, 5, z, 161) < .6:
            Q.put(8, 5, z, stairs("deepslate_tile", "west"))
    for (x, z) in ((10, -7), (10, -2)):
        for y in range(1, 4):
            Q.put(x, y, z, FENCE if not east else wallb("cobbled_deepslate"))
    Q.put(9, 1, -7, M("barrel[facing=up,open=false]"))
    Q.put(8, 1, -7, M("barrel[facing=east,open=false]"))
    for z in (-3, -2):
        Q.put(10, 1, z, CD)
    Q.put(10, 2, -2, slab("cobbled_deepslate"))
    # tower slits
    for y in (3, 4, 10, 11):
        Q.put(0, y, -4, AIR)
        Q.put(3, y, -7, AIR)
    Q.boxes = {"x": (0, 0, -9, 10, 17, -1)}
    if east:
        cells = [(x, z) for x in range(0, 7) for z in range(-7, 0)]
        collapse(Q, cells, lambda x, z: 9 + 4 * vnoise(x, z, 2.5, 162) - max(0, 3 - x) * 1.2, 162)
        rubble(Q, 2.5, -4.0, 2.0, 2.2, 1, 163, top=8)
    else:
        cells = [(x, z) for x in range(0, 7) for z in range(-7, 0)]
        collapse(Q, cells, lambda x, z: 13 + 3 * vnoise(x, z, 2.0, 164), 164)
    rubble(Q, 8.5, -5.0 if east else -6.0, 1.4, 1.0, 1, 165 + east, top=7)
    moss_floor(Q, 7, -7, 10, -1, 7, .35, 167 + east)
    cobwebs(Q, 1, 5, -6, 5, 12, -2, .35, 169 + east)
    for (x, y, z), s in Q.b.items():
        gx = 36 - x if east else x
        P.put(gx, y, z, mirror_x_state(s) if east else s, Q.be.get((x, y, z)))
    gx = 33 if east else 3
    P.air(gx, 9, 0, gx, 11, 0)
    for name, hr, cells in Q.passages:
        P.passage(("east " if east else "west ") + name, [((36 - x) if east else x, y, z) for (x, y, z) in cells], hr)


def build_throne_hall(P):
    WX = (3, 4, 32, 33)
    IX0, IX1, IZ0, IZ1 = 5, 31, -42, -12
    EAVE = 20                      # wall plate; roof pitch 1:1, ridge at x=18
    ax, ay, az = ALTAR_POS
    ribs = (-16, -22, -28, -34, -40)

    def underside(x):              # lowest roof block above column x (interior is open to the roof)
        return EAVE + (x - 3 if x <= 18 else 33 - x)

    # floor
    for x in range(3, 34):
        for z in range(-44, -9):
            inside = IX0 <= x <= IX1 and IZ0 <= z <= IZ1
            if not inside:
                P.put(x, 0, z, PBB)
                continue
            d = math.hypot(x - ax, z - az)
            in_arena = ARENA[0] <= x <= ARENA[2] and ARENA[1] <= z <= ARENA[3]
            if in_arena:
                if x in (ARENA[0], ARENA[2]) or z in (ARENA[1], ARENA[3]):
                    s = PBB
                elif d < 1.6:
                    s = PBB
                elif 2.5 <= d < 3.5:
                    s = PD
                elif 4.5 <= d < 5.5:
                    s = GB if h3(x, 0, z, 170) < .25 else PB
                elif 8.5 <= d < 9.3:
                    s = PBB
                else:
                    r = h3(x, 0, z, 171)
                    s = PD if r > .3 else (DT if r > .08 else CDT)
            else:
                s = floor_tiles(x, z, 12)
                if 16 <= x <= 20 and z > ARENA[3]:
                    s = PD
            P.put(x, 0, z, s)
    for (dx, dz) in ((5, 0), (-5, 0), (0, 5), (0, -5)):
        P.put(ax + dx, 0, az + dz, CHPB)
    # walls up to the wall plate, interior open up to the roof
    for x in range(3, 34):
        for z in range(-44, -9):
            if x in WX or z in (-44, -43, -11, -10):
                for y in range(1, EAVE + 1):
                    P.put(x, y, z, mason(x, y, z))
            elif IX0 <= x <= IX1 and IZ0 <= z <= IZ1:
                P.air(x, 1, z, x, underside(x) - 1, z)
    # gable ends (north and south) up to the roof line, coped by the roof itself
    for x in range(5, 32):
        for z in (-44, -43, -11, -10):
            for y in range(EAVE + 1, underside(x)):
                P.put(x, y, z, mason(x, y, z))
    # steep roof: deepslate tile stairs over a rafter layer; stone ribs under it at the rib lines
    for k in range(0, 16):
        y = EAVE + k
        for z in range(-44, -9):
            P.put(2 + k, y, z, stairs("deepslate_tile", "east"))
            P.put(34 - k, y, z, stairs("deepslate_tile", "west"))
            if k >= 2 and IZ0 <= z <= IZ1:
                P.put(3 + k, y, z, DT if z in ribs else PLANKS)
                P.put(33 - k, y, z, DT if z in ribs else PLANKS)
            else:
                P.put(3 + k, y, z, mason(3 + k, y, z))
                P.put(33 - k, y, z, mason(33 - k, y, z))
    for z in range(-44, -9):
        P.put(18, EAVE + 16, z, slab("deepslate_tile"))
        P.put(18, EAVE + 15, z, DT if z in ribs or z in (-44, -43, -11, -10) else PLANKS)
    # crown finials on the north gable apex
    P.put(18, EAVE + 17, -44, CHD)
    P.put(18, EAVE + 18, -44, wallb("deepslate_brick"))
    P.put(18, EAVE + 19, -44, wallb("deepslate_brick"))
    for x in (15, 21):
        P.put(x, underside(x) + 1, -44, CHD)
        P.put(x, underside(x) + 2, -44, wallb("deepslate_brick"))
    # corner turrets (5x5) with pyramid spires
    for (xa, za) in ((0, -47), (32, -47), (0, -14), (32, -14)):
        xb, zb = xa + 4, za + 4
        for x in range(xa, xb + 1):
            for z in range(za, zb + 1):
                P.put(x, 0, z, PBB)
                for y in range(1, 28):
                    P.put(x, y, z, mason(x, y, z))
        for y in range(1, 28):
            for (qx, qz) in ((xa, za), (xb, za), (xa, zb), (xb, zb)):
                P.put(qx, y, qz, PD if y % 2 else (PBB if y < 3 else DB))
        for x in range(xa, xb + 1):  # corbel course
            P.put(x, 24, za, CHD if x % 2 else PD)
            P.put(x, 24, zb, CHD if x % 2 else PD)
        for lvl in range(3):
            y = 28 + lvl
            x0, x1, z0, z1 = xa + lvl, xb - lvl, za + lvl, zb - lvl
            if x0 == x1:
                P.put(x0, y, z0, CHD)
                break
            for x in range(x0, x1 + 1):
                for z in range(z0, z1 + 1):
                    if x in (x0, x1) and z in (z0, z1):
                        P.put(x, y, z, PD)
                    elif z == z0:
                        P.put(x, y, z, stairs("deepslate_tile", "south"))
                    elif z == z1:
                        P.put(x, y, z, stairs("deepslate_tile", "north"))
                    elif x == x0:
                        P.put(x, y, z, stairs("deepslate_tile", "east"))
                    elif x == x1:
                        P.put(x, y, z, stairs("deepslate_tile", "west"))
        cx_, cz_ = xa + 2, za + 2
        P.put(cx_, 31, cz_, wallb("deepslate_brick"))
        P.put(cx_, 32, cz_, M("lightning_rod[facing=up]"))
        for y in (12, 18):  # turret slits facing out
            P.put(xa if xa == 0 else xb, y, cz_, AIR)
            P.put(cx_, y, za if za == -47 else zb, AIR)
    # buttresses
    for z in ribs:
        for (bx, inner) in ((1, 2), (35, 34)):
            if z == -40 and bx == 35:
                continue
            f = "east" if bx == 1 else "west"
            for y in range(1, 10):
                P.put(bx, y, z, mason(bx, y, z))
                P.put(inner, y, z, mason(inner, y, z))
            P.put(bx, 10, z, stairs("deepslate_brick", f))
            for y in range(10, 17):
                P.put(inner, y, z, mason(inner, y, z))
            P.put(inner, 17, z, stairs("deepslate_brick", f))
            P.put(bx, 0, z, PBB)
            P.put(inner, 0, z, PBB)
    for x in (9, 14, 22, 27):
        for y in range(1, 10):
            P.put(x, y, -45, mason(x, y, -45))
            P.put(x, y, -46, mason(x, y, -46))
        P.put(x, 10, -46, stairs("deepslate_brick", "north"))
        for y in range(10, 16):
            P.put(x, y, -45, mason(x, y, -45))
        P.put(x, 16, -45, stairs("deepslate_brick", "north"))
    # lancet windows in the side walls
    for zc in (-19, -25, -31, -37):
        for (xo, xi) in ((3, 4), (33, 32)):
            for z in (zc - 1, zc, zc + 1):
                for y in range(10, 16):
                    P.put(xo, y, z, BARS)
                    P.put(xi, y, z, AIR)
            P.put(xo, 16, zc, BARS)
            P.put(xi, 16, zc, AIR)
            P.put(xi, 16, zc - 1, stairs("deepslate_brick", "north", "top"))
            P.put(xi, 16, zc + 1, stairs("deepslate_brick", "south", "top"))
            P.put(xi, 9, zc, slab("polished_deepslate", "top"))
    # rose window over the throne and gable lancets
    for x in range(14, 23):
        for y in range(11, 20):
            d = math.hypot(x - 18, y - 15)
            if d < 2.6:
                spoke = x == 18 or y == 15 or abs(d - 1.6) < .4
                P.put(x, y, -44, M("black_stained_glass_pane") if spoke else M("blue_stained_glass_pane"))
                P.put(x, y, -43, AIR)
    for (zo, zi) in ((-44, -43), (-10, -11)):
        for x in (17, 18, 19):
            for y in range(25, 30 if x == 18 else 29):
                P.put(x, y, zo, BARS)
                P.put(x, y, zi, AIR)
    # south entrance (from the antechamber) with a raised portcullis
    P.air(16, 1, -11, 20, 7, -10)
    for z in (-11, -10):
        arch_x(P, 16, 20, z, 7)
    for x in (17, 18, 19):
        P.put(x, 7, -11, BARS)
    for x in range(16, 21):
        for z in (-11, -10):
            P.put(x, 0, z, PD)
    P.passage("throne hall door", [(x, 1, z) for x in range(16, 21) for z in (-11, -10, -12)], 4)
    # arena fog: challengers walk in, the Sovereign and his guard stay
    for x in range(16, 21):
        for y in range(1, 7):
            P.put(x, y, -11, f"{NS}:fog_gate", force=True)
    # columns, springers, stone ribs, sconces, banners
    for z in ribs:
        for x in (6, 30):
            for y in range(1, 18):
                P.put(x, y, z, CHD if y in (1, 17) else PD)
            P.put(x + (1 if x == 6 else -1), 17, z, stairs("polished_deepslate", "west" if x == 6 else "east", "top"))
        for x in (5, 31):
            for y in range(16, underside(x)):
                P.put(x, y, z, mason(x, y, z))
        for x in (6, 30):
            for y in range(18, underside(x)):
                P.put(x, y, z, PD)
        for x in range(IX0, IX1 + 1):
            P.put(x, underside(x) - 1, z, DT)
        for x in range(7, 12):
            P.put(x, underside(x) - 2, z, stairs("deepslate_tile", "west", "top"))
            P.put(36 - x, underside(36 - x) - 2, z, stairs("deepslate_tile", "east", "top"))
        P.put(7, 8, z, stairs("polished_blackstone", "west", "top"))
        P.put(7, 7, z, lantern(True))
        P.put(29, 8, z, stairs("polished_blackstone", "east", "top"))
        P.put(29, 7, z, lantern(True))
        P.put(7, 12, z, wall_banner("east"), {"kind": "banner", "patterns": VAEL_BANNER})
        P.put(29, 12, z, wall_banner("west"), {"kind": "banner", "patterns": VAEL_BANNER_TORN})
    # chandeliers hanging from the ribs (lanterns at y=13, above the arena clearance)
    for (x, z) in ((12, -22), (24, -22), (12, -28), (24, -28), (18, -40)):
        for y in range(14, underside(x) - 1):
            P.put(x, y, z, CHAIN)
        P.put(x, 13, z, lantern(True))
    # dais and throne
    for x in range(11, 26):
        P.put(x, 1, -36, stairs("polished_blackstone_brick", "north"))
        P.put(x, 1, -37, PBB)
        P.put(x, 2, -37, stairs("polished_blackstone_brick", "north"))
        for z in range(-42, -37):
            P.put(x, 1, z, PBB)
            P.put(x, 2, z, PBB if x in (11, 25) else (PD if (x + z) % 2 else DT))
    for z in range(-42, -37):
        P.put(10, 1, z, stairs("polished_blackstone_brick", "east"))
        P.put(26, 1, z, stairs("polished_blackstone_brick", "west"))
    for z in (-39, -38):
        for x in range(15, 22):
            P.put(x, 3, z, carpet("blue" if 16 <= x <= 20 else "black"))
    P.put(18, 2, -40, GOLD)
    P.put(18, 3, -40, stairs("polished_blackstone", "north"))
    P.put(17, 3, -40, stairs("polished_blackstone_brick", "east"))
    P.put(19, 3, -40, stairs("polished_blackstone_brick", "west"))
    P.put(18, 3, -41, GOLD)
    P.put(18, 4, -41, GOLD)
    P.put(18, 5, -41, CHPB)
    P.put(18, 6, -41, GOLD)
    for x in (17, 19):
        P.put(x, 3, -41, PBB)
        P.put(x, 4, -41, CHPB)
        P.put(x, 5, -41, stairs("polished_blackstone_brick", "east" if x == 19 else "west", "top"))
        P.put(x, 6, -41, wallb("polished_blackstone_brick"))
    for x in (13, 23):
        P.put(x, 3, -41, PBB)
        P.put(x, 4, -41, lantern())
    drape(P, 15, 21, -42, 12, [6, 7, 8, 8, 8, 7, 6], "south", 173)
    drape_z(P, 5, -26, -24, 8, [5, 6, 4], 174)
    drape_z(P, 31, -26, -24, 8, [4, 6, 5], 175)
    for z in range(-14, -11):
        for x in (17, 18, 19):
            if h3(x, 1, z, 176) < .85:
                P.put(x, 1, z, carpet("blue" if x == 18 else "black"))
    # altar ring (locked) with rotation-proof offering markers
    P.put(ax, ay, az, fmt_state(ALTAR, {"facing": "north"}), force=True)
    for (dx, dz, face, toward) in ((3, 0, "east", "west"), (-3, 0, "west", "east"), (0, 3, "south", "north"), (0, -3, "north", "south")):
        P.put(ax + dx, ay, az + dz, fmt_state(PEDESTAL, {"facing": toward}), force=True)
        P.put(ax + dx, ay + 1, az + dz, fmt_state(MARKER, {"facing": face}), force=True)
        P.lock((ax + dx, ay, az + dz), (ax + dx, ay + 1, az + dz))
    P.lock((ax, ay, az))
    for x in range(ARENA[0], ARENA[2] + 1):
        for z in range(ARENA[1], ARENA[3] + 1):
            for y in range(1, ARENA_CLEAR + 1):
                if (x, y, z) not in P.locked:
                    P.put(x, y, z, AIR)
    P.clear_zones.append((ARENA[0], 1, ARENA[1], ARENA[2], ARENA_CLEAR + 2, ARENA[3]))
    P.clear_zones.append((16, 1, -14, 20, 6, -10))
    P.clear_zones.append((29, 1, -40, 31, 4, -36))
    P.passage("throne dais steps", [(x, 2, -36) for x in range(11, 26)] + [(x, 3, -37) for x in range(11, 26)], 4)
    P.passage("throne aisle east", [(x, 1, z) for x in (29, 31) for z in range(-40, -35)], 3)
    # hidden royal vault: cracked brick seal in the east wall, tunnel through a buttress mass
    for x in range(34, 37):
        for z in range(-40, -34):
            for y in range(1, 10):
                P.put(x, y, z, mason(x, y, z))
            P.put(x, 0, z, PBB)
        P.put(x, 10, -40, stairs("deepslate_brick", "west"))
        P.put(x, 10, -35, stairs("deepslate_brick", "west"))
    for x in (32, 33):
        for z in (-38, -37):
            for y in (1, 2, 3):
                P.put(x, y, z, CDB, force=True)
                P.lock((x, y, z))
    P.air(34, 1, -38, 36, 3, -37)
    P.passage("vault tunnel", [(x, 1, z) for x in (34, 35, 36) for z in (-38, -37)], 3)
    # decay
    cobwebs(P, IX0, 14, IZ0, IX1, 30, IZ1, .12, 177)
    moss_floor(P, IX0, IZ0, IX1, IZ1, 6, .12, 178)
    # ruin: the roof has fallen in over the south-east (ribs survive), the east wall top and SE turret crumble
    for x in range(2, 35):
        for z in range(-44, -9):
            if z in ribs and z != -16:
                continue
            n = vnoise(x, z, 4.0, 179)
            east_hole = x >= 15 and -33 <= z <= -12 and n > .30 + (z + 33) * -.004 + max(0, 18 - x) * .05
            west_hole = x < 15 and -27 <= z <= -15 and n > .62
            north_hole = 6 <= x <= 12 and -41 <= z <= -36 and n > .55
            if east_hole or west_hole or north_hole:
                for y in range(EAVE + (0 if x not in (2, 3, 4, 32, 33, 34) else 1), EAVE + 18):
                    if (x, y, z) in P.b and z not in (-44, -43):
                        P.put(x, y, z, AIR if IX0 <= x <= IX1 and IZ0 <= z <= IZ1 else None)
    for x in range(30, 37):
        for z in range(-23, -9):
            n = vnoise(x, z, 3.0, 185)
            base = 13 + 6 * n + max(0, -16 - z) * .35 if x <= 33 else 9 + 6 * n
            if x >= 32 and z >= -14:
                base = 15 + 5 * n
            for y in range(int(base), 40):
                if (x, y, z) in P.b and (x, y, z) not in P.locked:
                    P.put(x, y, z, AIR if IX0 <= x <= IX1 and IZ0 <= z <= IZ1 else None)
    for z in range(-12, -9):  # south gable top broken
        for x in range(13, 26):
            for y in range(EAVE + 6 + int(4 * vnoise(x, z, 2.0, 186)), EAVE + 18):
                if (x, y, z) in P.b:
                    P.put(x, y, z, None)
    rubble(P, 30.0, -16.0, 1.6, 4.0, 2, 181, top=8)
    rubble(P, 26.0, -13.0, 4.0, 1.2, 1, 182, top=8)
    rubble(P, 35.0, -18.0, 1.2, 4.5, 2, 183, top=8)
    rubble(P, 6.0, -38.0, 1.2, 2.5, 1, 184, top=8)
    rubble(P, 6.0, -20.0, 1.2, 3.0, 1, 187, top=8)
    for (x, y, z) in ((30, 2, -13), (31, 1, -20)):
        P.deco(x, y, z, stairs("deepslate_tile", "west", "top"))


def build_vault(P):
    X0, X1, Z0, Z1 = 37, 47, -44, -32
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            edge = x in (X0, X1) or z in (Z0, Z1)
            P.put(x, 0, z, PBB if edge else (GB if (x + z) % 7 == 0 else (CHPB if z == -38 and x % 2 else PBB)))
            for y in range(1, 6):
                if edge:
                    r = h3(x, y, z, 190)
                    P.put(x, y, z, PBB if y <= 1 else (mason(x, y, z, band=4) if r > .3 else (CPBB if r < .1 else PBB)))
                else:
                    P.put(x, y, z, AIR)
            P.put(x, 6, z, DT)
    for (x, z) in ((X0, Z0), (X1, Z0), (X0, Z1), (X1, Z1)):
        for y in range(1, 7):
            P.put(x, y, z, PD if y % 2 else CHD)
    # hipped roof of deepslate tile stairs, ridge along z
    for k in range(0, 6):
        y = 7 + k
        x0, x1, z0, z1 = X0 + k, X1 - k, Z0 + k, Z1 - k
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x0 == x1:
                    P.put(x, y, z, slab("deepslate_tile"))
                elif x in (x0, x1) and z in (z0, z1):
                    P.put(x, y, z, DT)
                elif z == z0:
                    P.put(x, y, z, stairs("deepslate_tile", "south"))
                elif z == z1:
                    P.put(x, y, z, stairs("deepslate_tile", "north"))
                elif x == x0:
                    P.put(x, y, z, stairs("deepslate_tile", "east"))
                elif x == x1:
                    P.put(x, y, z, stairs("deepslate_tile", "west"))
    for (x, z) in ((44, -41), (45, -40), (44, -40), (45, -41)):  # a few slipped roof tiles
        for y in range(7, 13):
            if P.get(x, y, z) and "stairs" in P.get(x, y, z):
                P.put(x, y, z, None)
                break
    for (x, z) in ((X0 + 1, Z0 + 1), (X1 - 1, Z0 + 1), (X0 + 1, Z1 - 1), (X1 - 1, Z1 - 1)):
        for y in range(1, 6):
            P.put(x, y, z, CHPB if y in (1, 5) else PB)
    # entrance from the tunnel
    P.air(X0, 1, -38, X0, 3, -37)
    # treasury dais with the vault chest, gold used sparingly
    for z in range(-40, -35):
        for x in (44, 45, 46):
            P.put(x, 1, z, PBB)
        P.put(43, 1, z, stairs("polished_blackstone_brick", "east"))
    P.put(45, 2, -38, M("chest[facing=west,type=single]"), {"kind": "chest", "loot": LOOT_VAULT})
    P.put(45, 2, -40, GOLD)
    P.put(45, 2, -36, GOLD)
    P.put(45, 3, -40, lantern())
    P.put(45, 3, -36, lantern())
    P.put(46, 2, -38, CHPB)
    P.put(46, 3, -38, M("candle[candles=4,lit=false]"))
    P.put(44, 2, -39, M("candle[candles=2,lit=false]"))
    P.put(44, 2, -37, M("candle[candles=3,lit=false]"))
    for (x, z) in ((40, -42), (40, -34)):
        P.put(x, 1, z, CHPB)
        P.put(x, 2, z, M("candle[candles=3,lit=false]"))
    P.entity("armor_stand", (41.5, 1.0, -42.5), 0.0, armor=[None, None, "minecraft:chainmail_chestplate", "minecraft:golden_helmet"],
             hands=[None, None], pose={"Head": [25.0, 0.0, 8.0], "RightArm": [-15.0, 0.0, 10.0]}, damaged=True)
    P.put(41, 5, -38, CHAIN)
    P.put(41, 4, -38, lantern(True))
    P.put(42, 4, -43, wall_banner("south"), {"kind": "banner", "patterns": VAEL_BANNER})
    P.put(42, 4, -33, wall_banner("north"), {"kind": "banner", "patterns": VAEL_BANNER_TORN})
    for x in range(38, 43):
        if h3(x, 1, -38, 191) < .7:
            P.put(x, 1, -38, carpet("blue"))
    cobwebs(P, 38, 4, -43, 46, 5, -33, .45, 192)
    P.passage("vault entry", [(x, 1, z) for x in (37, 38) for z in (-38, -37)], 3)


def offerings_template(version, table):
    return _offerings_template(version, jig_name("offering_ring"), jig_name("offering_anchor"), lantern(False))


def item(id_, version, damaged=False, salt=0):
    if id_ is None:
        return Compound({})
    c = {"id": String(id_)}
    dmg = int(40 + 120 * h3(len(id_), salt, 3, 200)) if damaged and not id_.endswith("crossbow") else 0
    if version == "1.20.1":
        c["Count"] = Byte(1)
        if dmg:
            c["tag"] = Compound({"Damage": Int(dmg)})
    else:
        c["count"] = Int(1)
        if dmg:
            c["components"] = Compound({"minecraft:damage": Int(dmg)})
    return Compound(c)


def entity_nbt(e, version, salt):
    rot = List[Float]([Float(e["yaw"]), Float(0.0)])
    if e["kind"] == "mob":
        return Compound({"id": String(e["id"]), "PersistenceRequired": Byte(1), "Rotation": rot})
    c = {"id": String("minecraft:armor_stand"), "Rotation": rot, "ShowArms": Byte(1),
         "ArmorItems": List[Compound]([item(i, version, e.get("damaged"), salt + n) for n, i in enumerate(e["armor"])]),
         "HandItems": List[Compound]([item(i, version, e.get("damaged"), salt + 9 + n) for n, i in enumerate(e["hands"])]),
         "PersistenceRequired": Byte(1)}
    if e.get("statue"):
        c["NoBasePlate"] = Byte(1)
    if e.get("pose"):
        c["Pose"] = Compound({k: List[Float]([Float(v) for v in vals]) for k, vals in e["pose"].items()})
    return Compound(c)


def build_plan():
    P = Plan()
    P.boxes = dict(BOXES)
    build_courtyard(P)
    build_gatehouse(P)
    build_barracks(P)
    build_east_tower(P)
    build_antechamber(P)
    build_connector(P, east=False)
    build_connector(P, east=True)
    build_throne_hall(P)
    build_vault(P)
    for name, box in BOXES.items():
        if name != "throne_hall":
            clean_floaters(P, box)
    clean_floaters(P, (0, 12, -47, 36, 42, -10))
    fix_connections(P)
    return P


def piece_of(p):
    return _piece_of(BOXES, p)


def cut_pieces(P, version):
    ax, ay, az = ALTAR_POS
    anchor = ("throne_hall", (ax, ay + 2, az), "down_north", jig_name("offering_anchor"), jig_name("offering_ring"),
              pool_id("offerings"), AIR)
    return _cut_pieces(P, version, BOXES, CONNECTIONS, PIECE_POOL, jig_name, pool_id, anchors=[anchor],
                       entity_nbt=entity_nbt)


def worldgen_json(version):
    spawns = [
        {"type": f"{ENTITY_NS}:hollow_sentinel", "weight": 10, "minCount": 1, "maxCount": 2},
        {"type": f"{ENTITY_NS}:hollow_arbalist", "weight": 6, "minCount": 1, "maxCount": 1},
        {"type": f"{ENTITY_NS}:fallen_standard_bearer", "weight": 2, "minCount": 1, "maxCount": 1},
        {"type": f"{ENTITY_NS}:soulbound_knight", "weight": 1, "minCount": 1, "maxCount": 1},
    ]
    structure = {
        "type": "minecraft:jigsaw",
        "biomes": f"#{NS}:has_structure/hollow_citadel",
        "step": "surface_structures",
        "terrain_adaptation": "beard_thin",
        "spawn_overrides": {"monster": {"bounding_box": "full", "spawns": spawns}},
        "start_pool": pool_id("start"),
        "size": 4,
        "start_height": {"absolute": 0},
        "project_start_to_heightmap": "WORLD_SURFACE_WG",
        "max_distance_from_center": 80,
        "use_expansion_hack": False,
    }
    if version != "1.20.1":
        structure["dimension_padding"] = 0
        structure["liquid_settings"] = "ignore_waterlogging"
    structure_set = {
        "structures": [{"structure": STRUCTURE, "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": 44, "separation": 18, "salt": 714309221},
    }
    pools = {}
    for piece, pool in PIECE_POOL.items():
        pools.setdefault(pool, []).append(piece)
    pool_json = {}
    for pool, pieces in pools.items():
        pool_json[pool] = {
            "fallback": EMPTY_POOL,
            "elements": [{"weight": 1, "element": {
                "element_type": "minecraft:single_pool_element",
                "location": loc_id(p),
                "processors": OFFERING_PROC if p == "offerings" else WEATHERING,
                "projection": "rigid"}} for p in pieces],
        }

    def rule(block, out, prob=None, pos=None, out_props=None):
        r = {"input_predicate": ({"predicate_type": "minecraft:random_block_match", "block": block, "probability": prob}
                                 if prob is not None else {"predicate_type": "minecraft:block_match", "block": block}),
             "location_predicate": {"predicate_type": "minecraft:always_true"},
             "output_state": {"Name": out} | ({"Properties": out_props} if out_props else {})}
        if pos:
            r["position_predicate"] = pos
        return r

    upper_courses = {"predicate_type": "minecraft:axis_aligned_linear_pos", "axis": "y",
                     "min_dist": 9, "max_dist": 26, "min_chance": 0.0, "max_chance": 0.24}
    weathering = {"processors": [
        {"processor_type": "minecraft:rule", "rules": [
            rule("minecraft:deepslate_bricks", "minecraft:air", pos=upper_courses),
            rule("minecraft:deepslate_bricks", "minecraft:cracked_deepslate_bricks", 0.22),
            rule("minecraft:deepslate_tiles", "minecraft:cracked_deepslate_tiles", 0.14),
            rule("minecraft:polished_blackstone_bricks", "minecraft:cracked_polished_blackstone_bricks", 0.12),
            rule("minecraft:cobbled_deepslate", "minecraft:moss_block", 0.18),
            rule("minecraft:blue_carpet", "minecraft:air", 0.12),
            rule("minecraft:black_carpet", "minecraft:air", 0.12),
        ]},
        {"processor_type": "minecraft:protected_blocks", "value": "#minecraft:features_cannot_replace"},
    ]}

    def offering_rule(face, out, props=None):
        return {"input_predicate": {"predicate_type": "minecraft:block_match", "block": "minecraft:soul_lantern"},
                "location_predicate": {"predicate_type": "minecraft:blockstate_match",
                                       "block_state": {"Name": MARKER, "Properties": {"facing": face}}},
                "output_state": {"Name": out} | ({"Properties": props} if props else {})}

    lantern_props = {"hanging": "false", "waterlogged": "false"}
    offerings = {"processors": [{"processor_type": "minecraft:rule", "rules": [
        offering_rule("east", "minecraft:gold_block"),
        offering_rule("west", "minecraft:gold_block"),
        offering_rule("south", "minecraft:soul_lantern", lantern_props),
        offering_rule("north", "minecraft:soul_lantern", lantern_props),
    ]}]}
    # The citadel generates in the Eclipsed Realm only.
    biome_tag = {"replace": False, "values": [f"{NS}:tarnished_fields", f"{NS}:ashen_wastes"]}
    return structure, structure_set, pool_json, weathering, offerings, biome_tag


def loot_tables():
    def entry(name, weight, lo=1, hi=1, damage=None):
        e = {"type": "minecraft:item", "name": name, "weight": weight}
        fns = []
        if hi > 1 or lo != 1:
            fns.append({"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}})
        if damage:
            fns.append({"function": "minecraft:set_damage", "damage": {"type": "minecraft:uniform", "min": damage[0], "max": damage[1]}})
        if fns:
            e["functions"] = fns
        return e

    def chance_pool(name, chance):
        return {"rolls": 1, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": name}],
                "conditions": [{"condition": "minecraft:random_chance", "chance": chance}]}

    vault = {"type": "minecraft:chest", "pools": [
        chance_pool(f"{NS}:sovereigns_seal", 0.2),
        chance_pool(f"{NS}:royal_soul_fragment", 0.05),
        {"rolls": {"type": "minecraft:uniform", "min": 4, "max": 7}, "bonus_rolls": 0.0, "entries": [
            entry(f"{NS}:tarnished_regalia", 24, 2, 6),
            entry("minecraft:gold_ingot", 18, 2, 7),
            entry("minecraft:gold_nugget", 16, 4, 14),
            entry("minecraft:echo_shard", 4, 1, 1),
            entry("minecraft:golden_apple", 3),
            entry("minecraft:golden_helmet", 3, damage=(0.3, 0.8)),
            entry("minecraft:name_tag", 2),
            entry("minecraft:clock", 2),
        ]},
    ], "random_sequence": LOOT_VAULT}
    barracks = {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 4, "max": 8}, "bonus_rolls": 0.0, "entries": [
            entry("minecraft:arrow", 16, 4, 14),
            entry("minecraft:bread", 14, 1, 4),
            entry("minecraft:iron_nugget", 12, 3, 9),
            entry("minecraft:iron_ingot", 8, 1, 4),
            entry(f"{NS}:tarnished_regalia", 12, 1, 3),
            entry("minecraft:iron_sword", 5, damage=(0.2, 0.7)),
            entry("minecraft:iron_helmet", 4, damage=(0.2, 0.7)),
            entry("minecraft:chainmail_helmet", 5, damage=(0.2, 0.8)),
            entry("minecraft:chainmail_chestplate", 4, damage=(0.2, 0.8)),
            entry("minecraft:chainmail_leggings", 4, damage=(0.2, 0.8)),
            entry("minecraft:chainmail_boots", 5, damage=(0.2, 0.8)),
            entry("minecraft:crossbow", 3, damage=(0.3, 0.8)),
            entry("minecraft:shield", 2, damage=(0.3, 0.8)),
        ]},
    ], "random_sequence": LOOT_BARRACKS}
    return {"hollow_citadel_vault": vault, "hollow_citadel_barracks": barracks}


def emit(P):
    meta = {
        "boxes": BOXES, "offerings_box": OFFERINGS_BOX, "altar": ALTAR_POS, "arena": ARENA, "arena_clear": ARENA_CLEAR,
        "passages": [{"name": n, "headroom": h, "cells": c} for n, h, c in P.passages],
        "seal": [[x, y, z] for x in (32, 33) for y in (1, 2, 3) for z in (-38, -37)],
        "destinations": {
            "gate approach": [18, 1, 43], "courtyard": [10, 1, 22], "barracks ground": [-8, 1, 21],
            "barracks upper floor": [-6, 9, 22], "west wall walk": [1, 9, 20], "east wall walk": [35, 9, 8],
            "north wall walk": [10, 9, 1], "gate chamber": [18, 8, 35], "gate roof": [18, 13, 35],
            "SW tower": [3, 1, 26], "east tower ground": [40, 1, 15], "east tower upper": [40, 9, 15],
            "antechamber": [18, 1, -5], "west side yard": [8, 1, -5], "east side yard": [28, 1, -5],
            "throne arena": [18, 1, -20], "throne dais": [18, 3, -38], "royal vault": [41, 1, -38],
        },
        "behind_seal": ["royal vault"],
        "chests": {"barracks": [-8, 1, 25, LOOT_BARRACKS], "vault": [45, 2, -38, LOOT_VAULT]},
        "fixed_entities": {f"{ENTITY_NS}:hollow_sentinel": 2, f"{ENTITY_NS}:soulbound_knight": 1},
    }
    write_json(OUT / "meta" / "hollow_citadel.json", meta)
    summary = {}
    for version, info in VERSIONS.items():
        root = data_dir(version)
        clean_staged(version, "hollow_citadel")
        table = block_table(version)
        tpls, dropped = cut_pieces(P, version)
        tpls["offerings"] = offerings_template(version, table)
        for name, tpl in tpls.items():
            save_template(root / info["structures"] / "hollow_citadel" / f"{name}.nbt", tpl, info["data_version"], table)
            summary.setdefault(name, {"size": tpl.size, "blocks": 0, "solid": 0, "entities": len(tpl.entities)})
            summary[name]["blocks"] = len(tpl.blocks)
            summary[name]["solid"] = sum(1 for s, _ in tpl.blocks.values() if s != AIR)
        structure, structure_set, pools, weathering, offerings, biome_tag = worldgen_json(version)
        write_json(root / "worldgen" / "structure" / "hollow_citadel.json", structure)
        write_json(root / "worldgen" / "structure_set" / "hollow_citadel.json", structure_set)
        for pool, obj in pools.items():
            write_json(root / "worldgen" / "template_pool" / "hollow_citadel" / f"{pool}.json", obj)
        write_json(root / "worldgen" / "processor_list" / "hollow_citadel_weathering.json", weathering)
        write_json(root / "worldgen" / "processor_list" / "hollow_citadel_offerings.json", offerings)
        write_json(root / "tags" / "worldgen" / "biome" / "has_structure" / "hollow_citadel.json", biome_tag)
        for name, obj in loot_tables().items():
            write_json(root / info["loot"] / "chests" / f"{name}.json", obj)
        print(f"[{version}] staged {root.relative_to(OUT.parent)} (dropped {dropped} out-of-box plan blocks)")
    print("pieces:")
    for name, s in summary.items():
        print(f"  {name:12s} size {s['size'][0]:2d}x{s['size'][1]:2d}x{s['size'][2]:2d}  stored {s['blocks']:6d}  non-air {s['solid']:6d}  entities {s['entities']}")
    return summary


def main():
    random.seed(SEED)
    P = build_plan()
    emit(P)


if __name__ == "__main__":
    main()
