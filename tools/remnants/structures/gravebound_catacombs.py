#!/usr/bin/env python3
"""Gravebound Catacombs generator.

The Ossuary Covenant's soul-prison: an underground jigsaw structure built as one global block plan
(deterministic, fixed seed), cut into jigsaw pieces (each <= 48x48x48) joined by floor-level jigsaws,
and staged for 1.20.1 and 1.21.1 under tools/remnants/structures/out/<version>/data/remnant_bosses/.
Nothing is written into the mod resources; install.py does that once the Cryptborn entities and
items are registered.

Plan frame (= assembled frame at start rotation 0): x east, z south, y up. y=0 is the floor course of
the main level (mobs stand at y=1); the chain chapel lies 6 lower (floor course y=-6), the entry stair
climbs 27 blocks to a ruined mausoleum (floor course y=27).

    entry_stair (shaft + mausoleum, north)
          |
    cells_west -- crossing (ossuary rotunda, start) -- cells_east (rough gaol)
          |                                                  : breach tunnel
    ossuary_gallery (three-aisle nave) ---- burial_vault (Burial Guardian)
          |
    chain_chapel (23x23 arena, altar ring pre-filled via the offerings piece)

terrain_adaptation is beard_box: the beardifier carves every piece box to air before placement, so
every piece stores its whole volume (architecture + rock fill). Rock that faces the interior is
hewn cobbled deepslate, never spring/lichen/ore-replaceable natural stone.

Rotation-proof altar offerings (same technique as the Hollow Citadel): the ritual checks absolute
directions (bone block east, soul soil west, chain south, iron bars north) but vanilla rotates jigsaw
pieces. The chapel puts magenta glazed terracotta direction markers on the pedestals; a 7x1x7
"offerings" piece, attached by a vertical aligned jigsaw above the altar, swaps each marker for the
offering its rotated facing asks for (blockstate_match location predicates).

Run:  python tools/remnants/structures/gravebound_catacombs.py
"""
from __future__ import annotations

import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from common import (AIR, EMPTY_POOL, ENTITY_NS, NS, OUT, VERSIONS, Plan, block_table, clean_staged,  # noqa: E402
                    cut_pieces, data_dir, fix_connections, fmt_state, is_full_solid, make_noise,
                    offerings_template, pick, save_template, state_name, write_json)
from nbtlib import Byte, Compound, Float, List, String  # noqa: E402

SEED = 330471913
NAME = "gravebound_catacombs"
STRUCTURE = f"{NS}:{NAME}"
WEATHERING = f"{NS}:{NAME}_weathering"
OFFERING_PROC = f"{NS}:{NAME}_offerings"
LOOT_CELLS = f"{NS}:chests/{NAME}_cells"
LOOT_VAULT = f"{NS}:chests/{NAME}_vault"
SET_SALT = 330471913

h3, vnoise = make_noise(SEED)


def pool_id(name):
    return f"{NS}:{NAME}/{name}"


def loc_id(name):
    return f"{NS}:{NAME}/{name}"


def jig_name(name):
    return f"{NS}:{NAME}/{name}"


def M(n):
    return "minecraft:" + n


DB, CDB = M("deepslate_bricks"), M("cracked_deepslate_bricks")
DT, CDT = M("deepslate_tiles"), M("cracked_deepslate_tiles")
CHD, PD, CD = M("chiseled_deepslate"), M("polished_deepslate"), M("cobbled_deepslate")
PB, PBB, CPBB = M("polished_blackstone"), M("polished_blackstone_bricks"), M("cracked_polished_blackstone_bricks")
CHPB, BS = M("chiseled_polished_blackstone"), M("blackstone")
BONE, BONE_X, BONE_Z = M("bone_block[axis=y]"), M("bone_block[axis=x]"), M("bone_block[axis=z]")
SOUL_SOIL, SOUL_FIRE = M("soul_soil"), M("soul_fire")
BARS = M("iron_bars")
CHAIN, CHAIN_X, CHAIN_Z = M("chain[axis=y]"), M("chain[axis=x]"), M("chain[axis=z]")
WEB = M("cobweb")
WEB_SPOT = M("cave_air")          # cobweb candidate: the weathering processor turns ~half into cobwebs
MOSS, MOSSC = M("moss_block"), M("moss_carpet")
TUFF, DEEP, STONE, ANDESITE = M("tuff"), M("deepslate[axis=y]"), M("stone"), M("andesite")
ROOTED, ROOTS = M("rooted_dirt"), M("hanging_roots")
MARKER = "minecraft:magenta_glazed_terracotta"
ALTAR = f"{NS}:ancient_altar"
PEDESTAL = f"{NS}:ancient_pedestal"


def stairs(mat, facing, half="bottom", shape="straight"):
    return f"minecraft:{mat}_stairs[facing={facing},half={half},shape={shape}]"


def slab(mat, t="bottom"):
    return f"minecraft:{mat}_slab[type={t}]"


def wallb(mat="deepslate_brick"):
    return f"minecraft:{mat}_wall"


def lantern(hanging=False):
    return f"minecraft:soul_lantern[hanging={'true' if hanging else 'false'}]"


def candle(n=1):
    return f"minecraft:candle[candles={n},lit=false]"


def skull(rot):
    return f"minecraft:skeleton_skull[rotation={rot % 16}]"


def wall_skull(facing):
    return f"minecraft:skeleton_wall_skull[facing={facing}]"


FACE_ROT = {"south": 0, "west": 4, "north": 8, "east": 12}
OPPF = {"north": "south", "south": "north", "east": "west", "west": "east"}


def face_toward(dx, dz):
    """Standing-skull rotation looking along (dx, dz)."""
    return int(round(math.degrees(math.atan2(-dx, dz)) / 22.5)) % 16


def mason(x, y, z, f=0, band=4):
    """Wall course by height above the floor course f: blackstone plinth, tile lower courses, polished band, bricks."""
    r = h3(x, y, z, 11)
    rel = y - f
    if rel <= 1:
        return CPBB if r < .08 else PBB
    if rel == band:
        return PD if r > .07 else CHD
    if rel < band:
        return CDT if r < .12 else DT
    return CDB if r < .1 else DB


def floor_tiles(x, z, salt=0):
    r = h3(x, 0, z, 31 + salt)
    return CDT if r < .14 else (PD if r < .22 else DT)


def rough_floor(x, z, salt=0):
    r = h3(x, 1, z, 33 + salt)
    return CD if r < .55 else (CDT if r < .75 else (DT if r < .92 else MOSS))


def rock(x, y, z):
    """Natural rock fill (ores and cave features may replace it later, as in any terrain)."""
    n = vnoise(x + y * .7, z - y * .5, 6.0, 902)
    if n > .74:
        return TUFF
    upper = (y - 8) / 20.0
    if y > 8 and h3(x, y, z, 903) < min(.85, upper):
        return ANDESITE if n < .22 else STONE
    return DEEP


def hewn(x, y, z):
    """Rough-cut rock facing open space: no spring / glow-lichen / moss-replaceable natural stone."""
    r = h3(x, y, z, 905)
    return CD if r < .7 else (CDB if r < .84 else (CDT if r < .94 else BS))


RUBBLE = [(CD, 34), (CDB, 14), (DB, 10), (CDT, 10), (PBB, 5), (BONE, 6), (BONE_X, 3), (BONE_Z, 3)]


def ground_at(P, x, z, top, bottom):
    for y in range(top, bottom - 1, -1):
        s = P.b.get((x, y, z))
        if s is None or s in (AIR, WEB_SPOT, WEB) or state_name(s).endswith("carpet"):
            continue
        return y + 1
    return bottom


def heap(P, cx, cz, rx, rz, hmax, salt, top, bottom=0):
    """Rubble mound (no gravity blocks): masonry chunks, bones and a few skulls."""
    for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
        for z in range(int(cz - rz) - 1, int(cz + rz) + 2):
            d = ((x - cx) / rx) ** 2 + ((z - cz) / rz) ** 2 + (h3(x, 0, z, salt) - .5) * .5
            if d > 1:
                continue
            g = ground_at(P, x, z, top, bottom)
            h = int(round(hmax * (1 - d) + (h3(x, 1, z, salt) - .4)))
            y = g
            ok = True
            for k in range(max(0, h)):
                y = g + k
                if not P.deco(x, y, z, pick(RUBBLE, h3(x, y, z, salt + 3))):
                    ok = False
                    break
            if not ok:
                continue
            y = g + max(0, h)
            r = h3(x, 2, z, salt)
            f = ["north", "south", "east", "west"][int(h3(x, 3, z, salt) * 4)]
            if r < .16:
                P.deco(x, y, z, slab(["cobbled_deepslate", "deepslate_brick", "deepslate_tile"][int(h3(x, 4, z, salt) * 3)]))
            elif r < .26:
                P.deco(x, y, z, stairs(["cobbled_deepslate", "deepslate_brick"][int(h3(x, 4, z, salt) * 2)], f))
            elif r < .33:
                P.deco(x, y, z, skull(int(h3(x, 5, z, salt) * 16)))
            elif r < .38:
                P.deco(x, y, z, MOSSC)


def web_spots(P, x0, y0, z0, x1, y1, z1, chance, salt):
    """Cobweb candidates in upper corners (cells touching a wall and the ceiling)."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            for y in range(y0, y1 + 1):
                if not P.is_empty(x, y, z) or (x, y, z) not in P.b or h3(x, y, z, salt) > chance:
                    continue
                above = P.b.get((x, y + 1, z))
                if above is None or above == AIR or not is_full_solid(above):
                    continue
                side = sum(1 for d in ((1, 0), (-1, 0), (0, 1), (0, -1))
                           if is_full_solid(P.b.get((x + d[0], y, z + d[1]))))
                if side >= 1:
                    P.deco(x, y, z, WEB_SPOT)


def moss_patches(P, x0, z0, x1, z1, top, bottom, chance, salt):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if vnoise(x, z, 3.5, salt) * .7 + h3(x, 0, z, salt) * .3 < 1 - chance:
                continue
            y = ground_at(P, x, z, top, bottom)
            below = P.get(x, y - 1, z)
            if below is None or not is_full_solid(below):
                continue
            P.deco(x, y, z, MOSSC)


def hanging_lantern(P, x, z, y_top, y_lantern):
    """Chain from y_top down to y_lantern + 1 and a hanging soul lantern at y_lantern."""
    for y in range(y_lantern + 1, y_top + 1):
        P.put(x, y, z, CHAIN)
    P.put(x, y_lantern, z, lantern(True))


def loculus(P, x, y, z, face, kind, salt):
    """Fill one niche cell that opens toward `face` (the direction a skull in it looks)."""
    along = BONE_Z if face in ("east", "west") else BONE_X
    r = h3(x, y, z, salt)
    if kind == "bones":
        P.put(x, y, z, along)
    elif kind == "skull":
        P.put(x, y, z, skull(FACE_ROT[face]))
    elif kind == "candle":
        P.put(x, y, z, candle(1 + int(r * 3)))
    elif kind == "web":
        P.put(x, y, z, WEB_SPOT)
    elif kind == "pile":
        P.put(x, y, z, BONE)
    else:
        P.put(x, y, z, AIR)


LOCULUS_KINDS = [("bones", 30), ("skull", 22), ("pile", 8), ("web", 14), ("candle", 6), ("empty", 20)]


def loculi_wall(P, cells, face, salt):
    """cells: niche positions (x, y, z) grouped in pairs along the wall; fills each pair as one burial."""
    for i in range(0, len(cells), 2):
        pair = cells[i:i + 2]
        r = h3(*pair[0], salt)
        kind = pick(LOCULUS_KINDS, r)
        for j, (x, y, z) in enumerate(pair):
            if kind == "bones" and j == len(pair) - 1 and h3(x, y, z, salt + 1) < .45:
                loculus(P, x, y, z, face, "skull", salt)
            elif kind in ("skull", "candle") and j == 0:
                loculus(P, x, y, z, face, "bones" if h3(x, y, z, salt + 2) < .5 else "web", salt)
            else:
                loculus(P, x, y, z, face, kind, salt)


BOXES = {
    "crossing":        (0, 0, 0, 22, 15, 22),
    "entry_stair":     (4, 0, -18, 18, 35, -1),
    "cells_west":      (-33, 0, 2, -1, 14, 20),
    "cells_east":      (23, 0, -4, 46, 12, 24),
    "ossuary_gallery": (2, 0, 23, 20, 13, 52),
    "burial_vault":    (21, 0, 25, 41, 12, 50),
    "chain_chapel":    (-5, -7, 53, 27, 11, 87),
}
PIECE_POOL = {"crossing": "start", "entry_stair": "entry", "cells_west": "cells_west", "cells_east": "cells_east",
              "ossuary_gallery": "gallery", "burial_vault": "vault", "chain_chapel": "chapel", "offerings": "offerings"}
CONNECTIONS = [
    ("crossing", "entry_stair", (11, 0, 0), "north", "entry"),
    ("crossing", "cells_west", (0, 0, 11), "west", "cells_west"),
    ("crossing", "cells_east", (22, 0, 11), "east", "cells_east"),
    ("crossing", "ossuary_gallery", (11, 0, 22), "south", "gallery"),
    ("ossuary_gallery", "burial_vault", (20, 0, 41), "east", "vault"),
    ("ossuary_gallery", "chain_chapel", (11, 0, 52), "south", "chapel"),
]
ALTAR_POS = (11, -5, 71)
ARENA = (0, 60, 22, 82)            # x0, z0, x1, z1: 23 x 23 around the altar
OFFERINGS_BOX = (8, -4, 68, 14, -4, 74)
GUARDIAN = (33.0, 2.0, 42.0)        # block corner: the 1.6-wide Burial Guardian stands on a 2x2 column
JIGSAW_SIZE = 4
MAX_DISTANCE = 96

CHESTS = {}                         # name -> (x, y, z, loot)
DESTINATIONS = {}
OPENINGS = []                       # cells where the catacomb opens into the terrain on purpose


def chest(P, name, x, y, z, facing, loot):
    P.put(x, y, z, M(f"chest[facing={facing},type=single]"), {"kind": "chest", "loot": loot}, force=True)
    P.lock((x, y, z))
    CHESTS[name] = (x, y, z, loot)


def build_crossing(P):
    cx, cz = 11, 11
    R_IN, R_OUT = 8.6, 10.7
    BASE, TOP = 6, 12

    def ceil(d):
        return BASE + int(round((TOP - BASE) * math.sqrt(max(0.0, 1 - (d / R_IN) ** 2))))

    for x in range(0, 23):
        for z in range(0, 23):
            dx, dz = x - cx, z - cz
            d = math.hypot(dx, dz)
            if d >= R_OUT:
                continue
            ring = d >= R_IN
            # floor
            if d < 1.6:
                f = CHPB
            elif 3.0 <= d < 3.8:
                f = PBB
            elif 6.1 <= d < 6.9:
                f = PD
            elif (dx == 0 or dz == 0) and d < R_IN:
                f = PB
            elif ring:
                f = PBB
            else:
                f = floor_tiles(x, z, 1)
            P.put(x, 0, z, f)
            if ring:
                for y in range(1, BASE + 2):
                    P.put(x, y, z, mason(x, y, z, 0, 5))
                continue
            c = ceil(d)
            for y in range(1, c):
                P.put(x, y, z, AIR)
            rib = abs(abs(dx) - abs(dz)) == 0 or dx == 0 or dz == 0
            P.put(x, c, z, PD if rib else (DB if h3(x, c, z, 3) > .1 else CDB))
            P.put(x, c + 1, z, DT)
    # exits: N (entry), W (cells), E (gaol) 3 wide x 4 tall; S (gallery) 5 wide x 5 tall
    def exit_z(x0, x1, z0, z1, h):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                P.put(x, 0, z, PD if x in (x0, x1) else PB)
                for y in range(1, h + 1):
                    P.put(x, y, z, AIR)
                P.put(x, h + 1, z, mason(x, h + 1, z, 0, 5))
                P.put(x, h + 2, z, DB)
            for y in range(1, h + 2):
                P.put(x0 - 1, y, z, CHPB if y == h + 1 else PBB)
                P.put(x1 + 1, y, z, CHPB if y == h + 1 else PBB)
        for z in range(z0, z1 + 1):
            P.put(x0, h, z, stairs("polished_blackstone_brick", "west", "top"))
            P.put(x1, h, z, stairs("polished_blackstone_brick", "east", "top"))

    def exit_x(x0, x1, z0, z1, h):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                P.put(x, 0, z, PD if z in (z0, z1) else PB)
                for y in range(1, h + 1):
                    P.put(x, y, z, AIR)
                P.put(x, h + 1, z, mason(x, h + 1, z, 0, 5))
                P.put(x, h + 2, z, DB)
            for y in range(1, h + 2):
                P.put(x, y, z0 - 1, CHPB if y == h + 1 else PBB)
                P.put(x, y, z1 + 1, CHPB if y == h + 1 else PBB)
        for x in range(x0, x1 + 1):
            P.put(x, h, z0, stairs("polished_blackstone_brick", "north", "top"))
            P.put(x, h, z1, stairs("polished_blackstone_brick", "south", "top"))

    exit_z(10, 12, 0, 2, 4)
    exit_z(9, 13, 20, 22, 5)
    exit_x(0, 2, 10, 12, 4)
    exit_x(20, 22, 10, 12, 4)
    # wall niches (columbarium) between the exits: two rows facing the column
    for x in range(0, 23):
        for z in range(0, 23):
            dx, dz = x - cx, z - cz
            d = math.hypot(dx, dz)
            if not (R_IN <= d < R_IN + 1.0):
                continue
            if min(abs(dx), abs(dz)) <= 3:
                continue  # keep the door surrounds solid
            ang = math.atan2(dz, dx)
            slot = int((ang + math.pi) / (2 * math.pi) * 40)
            if slot % 2:
                continue
            face = ("east" if dx < 0 else "west") if abs(dx) > abs(dz) else ("south" if dz < 0 else "north")
            for y in (2, 4):
                r = h3(x, y, z, 41)
                if r < .34:
                    P.put(x, y, z, skull(face_toward(-dx, -dz)))
                elif r < .62:
                    P.put(x, y, z, BONE_X if face in ("north", "south") else BONE_Z)
                elif r < .72:
                    P.put(x, y, z, candle(1 + int(h3(x, y, z, 42) * 3)))
                elif r < .86:
                    P.put(x, y, z, WEB_SPOT)
                else:
                    P.put(x, y, z, AIR)
    # central ossuary column: blackstone corners, bone core, skull niches on each face
    for y in range(1, TOP + 1):
        for x in range(10, 13):
            for z in range(10, 13):
                corner = x != 11 and z != 11
                if y == 1:
                    P.put(x, y, z, PBB)
                elif corner:
                    P.put(x, y, z, CHPB if y in (2, 7) else (PB if y < 7 else PD))
                else:
                    P.put(x, y, z, BONE)
    for (fx, fz, face) in ((11, 9, "north"), (11, 13, "south"), (9, 11, "west"), (13, 11, "east")):
        ix, iz = (fx + 11) // 2 if fx != 11 else 11, (fz + 11) // 2 if fz != 11 else 11
        # niche cut into the face cell with a skull looking out, a lantern shelf higher up
        P.put(ix, 3, iz, skull(FACE_ROT[face]))
        P.put(ix, 5, iz, skull(FACE_ROT[face]))
        P.put(fx, 6, fz, wall_skull(face))
        P.put(fx, 2, fz, slab("polished_blackstone", "bottom"))
    for (x, z) in ((10, 9), (12, 9), (10, 13), (12, 13), (9, 10), (9, 12), (13, 10), (13, 12)):
        P.put(x, 1, z, slab("polished_blackstone_brick"))
    for (x, z, f) in ((11, 9, "north"), (11, 13, "south"), (9, 11, "west"), (13, 11, "east")):
        P.put(x, 1, z, stairs("polished_blackstone_brick", OPPF[f]))
    # column crown flares into the dome ribs, chains and lanterns hang from the dome
    for (x, z, f) in ((11, 9, "north"), (11, 13, "south"), (9, 11, "west"), (13, 11, "east")):
        P.put(x, TOP - 1, z, stairs("polished_deepslate", OPPF[f], "top"))
    for (x, z) in ((7, 7), (15, 7), (7, 15), (15, 15)):
        top = ceil(math.hypot(x - cx, z - cz))
        hanging_lantern(P, x, z, top - 1, 6)
    # collapse: the north-east of the dome has caved in, rubble against the wall (the cavity stays inside the
    # piece box, so no terrain or aquifer water is exposed; the lantern chain at (15, 7) keeps its anchor)
    for x in range(13, 21):
        for z in range(1, 10):
            n = vnoise(x, z, 2.5, 44)
            d = math.hypot(x - cx, z - cz)
            if d < R_IN and n > .52 and (x - cx) > 2 and (cz - z) > 2 and (x, z) != (15, 7):
                c = ceil(d)
                for y in range(c, min(c + 3 + int(n * 3), BOXES["crossing"][4])):
                    P.put(x, y, z, AIR)
    # passages first: decoration (rubble, cobweb spots, moss) never goes into their clear zones
    ring = [(x, 1, z) for x in range(0, 23) for z in range(0, 23)
            if 4.2 <= math.hypot(x - cx, z - cz) < 5.4]
    P.passage("rotunda ring", ring, 4)
    P.passage("rotunda north door", [(x, 1, z) for x in range(10, 13) for z in range(0, 3)], 3)
    P.passage("rotunda south door", [(x, 1, z) for x in range(9, 14) for z in range(20, 23)], 4)
    P.passage("rotunda west door", [(x, 1, z) for x in range(0, 3) for z in range(10, 13)], 3)
    P.passage("rotunda east door", [(x, 1, z) for x in range(20, 23) for z in range(10, 13)], 3)
    heap(P, 16.5, 5.0, 2.6, 2.0, 2, 45, top=10)
    P.clear_zones.append((0, 1, 9, 22, 4, 13))      # E-W axis
    P.clear_zones.append((9, 1, 0, 13, 5, 22))      # N-S axis
    heap(P, 16.0, 4.5, 2.2, 1.6, 2, 46, top=10)
    web_spots(P, 1, 4, 1, 21, 11, 21, .4, 47)
    moss_patches(P, 2, 2, 20, 20, 3, 1, .14, 48)
    DESTINATIONS["ossuary rotunda"] = (6, 1, 11)


SHAFT = (7, 15, -15, -7)            # interior x0, x1, z0, z1
WELL = (10, 12, -12, -10)
TOP_FLOOR = 27


def spiral_cells():
    """Ordered (kind, cells, height, facing): landings and steps of the square spiral, bottom to top."""
    out = []
    corners = {"SW": [(x, z) for x in range(7, 10) for z in range(-9, -6)],
               "NW": [(x, z) for x in range(7, 10) for z in range(-15, -12)],
               "NE": [(x, z) for x in range(13, 16) for z in range(-15, -12)],
               "SE": [(x, z) for x in range(13, 16) for z in range(-9, -6)]}
    sides = {"W": ([[(x, z) for x in range(7, 10)] for z in (-10, -11, -12)], "north"),
             "N": ([[(x, z) for z in range(-15, -12)] for x in (10, 11, 12)], "east"),
             "E": ([[(x, z) for x in range(13, 16)] for z in (-12, -11, -10)], "south"),
             "S": ([[(x, z) for z in range(-9, -6)] for x in (12, 11, 10)], "west")}
    h = 0
    out.append(("landing", corners["SW"], 0, None))
    order = [("W", "NW"), ("N", "NE"), ("E", "SE"), ("S", "SW")]
    while True:
        for side, corner in order:
            rows, facing = sides[side]
            for row in rows:
                h += 1
                out.append(("step", row, h, facing))
            out.append(("landing", corners[corner], h, None))
            if h >= TOP_FLOOR:
                return out


def build_entry(P):
    X0, X1, Z0, Z1 = SHAFT
    wx0, wx1, wz0, wz1 = WELL
    top_roof = 35

    def shaft_mason(x, y, z):
        rel = y % 12
        r = h3(x, y, z, 51)
        if rel == 0:
            return CPBB if r < .1 else PBB
        if rel == 6:
            return PD if r > .08 else CHD
        if rel < 6:
            return CDT if r < .12 else DT
        return CDB if r < .1 else DB

    # outer walls (2 thick) from the floor to the roof, floor course everywhere
    for x in range(X0 - 2, X1 + 3):
        for z in range(Z0 - 2, Z1 + 3):
            inside = X0 <= x <= X1 and Z0 <= z <= Z1
            P.put(x, 0, z, floor_tiles(x, z, 5) if inside else PBB)
            for y in range(1, top_roof + 1):
                if not inside:
                    P.put(x, y, z, shaft_mason(x, y, z))
    for x in range(X0 - 2, X1 + 3):        # quoins
        for z in (Z0 - 2, Z1 + 2):
            if x in (X0 - 2, X1 + 2):
                for y in range(1, top_roof + 1):
                    P.put(x, y, z, PD if y % 2 else PBB)
    # interior open shaft (filled below by the stair)
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            for y in range(1, TOP_FLOOR + 6):
                P.put(x, y, z, AIR)
    # spiral stair
    cells = spiral_cells()
    surface = {}                              # (x, z) -> list of surface heights (ascending)
    first = {}
    for kind, row, h, facing in cells:
        for (x, z) in row:
            surface.setdefault((x, z), []).append(h)
            if kind == "step":
                P.put(x, h, z, stairs("deepslate_tile", facing))
            elif h > 0:
                P.put(x, h, z, PD if h3(x, h, z, 52) > .2 else DT)
            first.setdefault((x, z), h)
    for (x, z), hs in surface.items():
        prev = 0
        for i, h in enumerate(hs):
            if h == 0:
                continue
            if i == 0 or hs[i - 1] == 0:
                below = hs[i - 1] if i > 0 else 0
                if h < 7 or below == h:
                    for y in range(1, h):
                        P.put(x, y, z, shaft_mason(x, y, z))
                else:
                    P.put(x, h - 1, z, PBB)
            else:
                P.put(x, h - 1, z, PBB)
            prev = h
        del prev
    # railing around the open well on every turn (and the ground pit)
    ring = [(x, z) for x in range(wx0 - 1, wx1 + 2) for z in range(wz0 - 1, wz1 + 2)
            if not (wx0 <= x <= wx1 and wz0 <= z <= wz1)]
    for (x, z) in ring:
        for h in surface.get((x, z), []):
            if h >= TOP_FLOOR:
                continue
            P.put(x, h + 1, z, wallb("deepslate_tile"))
        if 0 in surface.get((x, z), []) or (x, z) not in surface:
            pass
    for (x, z) in ring:
        hs = surface.get((x, z), [])
        if hs and min(hs) >= 7:
            P.put(x, 1, z, wallb("deepslate_tile"))
    # ground pit under the well: bones thrown down the shaft
    for x in range(wx0, wx1 + 1):
        for z in range(wz0, wz1 + 1):
            P.put(x, 0, z, SOUL_SOIL if (x, z) == (11, -11) else CD)
            r = h3(x, 1, z, 53)
            P.put(x, 1, z, BONE if r < .5 else (BONE_X if r < .7 else BONE_Z))
            if h3(x, 2, z, 54) < .45:
                P.put(x, 2, z, skull(int(h3(x, 2, z, 55) * 16)))
    P.put(11, 2, -11, BONE)
    P.put(11, 3, -11, skull(0))
    # outer-wall niches with skulls and candles along the climb
    for kind, row, h, facing in cells:
        if kind != "step" or h % 3 != 2:
            continue
        x, z = row[0]
        # outer column is the one touching the wall
        outer = [c for c in row if c[0] in (X0, X1) or c[1] in (Z0, Z1)]
        if not outer:
            continue
        ox, oz = outer[0]
        nx = ox - 1 if ox == X0 else (ox + 1 if ox == X1 else ox)
        nz = oz - 1 if oz == Z0 else (oz + 1 if oz == Z1 else oz)
        face = "east" if nx < X0 else ("west" if nx > X1 else ("south" if nz < Z0 else "north"))
        r = h3(nx, h, nz, 56)
        P.put(nx, h + 2, nz, skull(FACE_ROT[face]) if r < .5 else (candle(2) if r < .75 else BONE))
    # passage to the rotunda
    for x in range(9, 14):
        for z in range(Z1 + 1, -1 + 1):
            P.put(x, 0, z, PBB if x in (9, 13) else (PB if x == 11 else PD))
            for y in range(1, 6):
                if x in (9, 13):
                    P.put(x, y, z, mason(x, y, z, 0, 4))
                elif y <= 4:
                    P.put(x, y, z, AIR)
                else:
                    P.put(x, y, z, DB)
    for z in range(Z1 + 1, 0):
        P.put(10, 4, z, stairs("deepslate_brick", "west", "top"))
        P.put(12, 4, z, stairs("deepslate_brick", "east", "top"))
    for z in range(Z1 + 1, 0, 2):
        P.put(9, 3, z, wall_skull("east") if z % 4 else candle(1))
    P.passage("entry passage", [(x, 1, z) for x in (10, 11, 12) for z in range(Z1 + 1, 0)], 3)
    # ground floor walkable cells, the stair run
    ground = [(x, 1, z) for (x, z), hs in surface.items() if hs[0] == 0 and P.get(x, 1, z) == AIR]
    P.passage("shaft ground floor", ground, 3)
    steps = []
    for kind, row, h, facing in cells:
        for (x, z) in row:
            if (x, z) in ring:
                continue
            if h + 1 > TOP_FLOOR:
                continue
            steps.append((x, h + 1, z))
    P.passage("spiral stair", steps, 3)
    # mausoleum (floor course 27)
    F = TOP_FLOOR
    opening = set()
    for kind, row, h, facing in cells:
        for (x, z) in row:
            if h >= F - 3 and h < F + 1 and not (kind == "landing" and h == F):
                opening.add((x, z))
    for x in range(wx0, wx1 + 1):
        for z in range(wz0, wz1 + 1):
            opening.add((x, z))
    nw_landing = {(x, z) for x in range(7, 10) for z in range(-15, -12)}
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            if (x, z) in opening:
                continue
            P.put(x, F, z, PD if (x + z) % 4 else DT)
            for y in range(F + 1, F + 6):
                P.put(x, y, z, AIR)
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            for y in range(F + 1, F + 6):
                if (x, z) in opening:
                    P.put(x, y, z, AIR)
    # balustrade around the stairwell opening (not across the arrival from the last flight)
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            if (x, z) in opening or (x, z) in nw_landing:
                continue
            if any((x + d[0], z + d[1]) in opening for d in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                P.put(x, F + 1, z, wallb("deepslate_brick"))
    # vaulted ceiling (barrel along z), roof, broken grate over the well
    for z in range(Z0, Z1 + 1):
        P.put(X0, F + 5, z, stairs("deepslate_brick", "west", "top"))
        P.put(X1, F + 5, z, stairs("deepslate_brick", "east", "top"))
        for x in range(X0, X1 + 1):
            P.put(x, F + 6, z, DB if h3(x, F + 6, z, 57) > .12 else CDB)
            for y in range(F + 7, top_roof + 1):
                P.put(x, y, z, DT)
    grate = top_roof - 1                   # the broken grate is choked by root-bound earth above it
    for x in range(wx0, wx1 + 1):
        for z in range(wz0, wz1 + 1):
            for y in range(F + 6, grate + 1):
                P.put(x, y, z, AIR)
            P.put(x, top_roof, z, ROOTED)
    for (x, z) in ((10, -12), (12, -12), (11, -11), (10, -10), (11, -12)):
        P.put(x, grate, z, BARS)
    for (x, z) in ((12, -10), (10, -11)):
        P.put(x, grate, z, ROOTS)
    # chains and soul lanterns down the well from the grate
    for y in range(8, grate):
        P.put(11, y, -11, lantern(True) if y in (24, 16, 8) else CHAIN)
    # roots reach down through the grate
    for (x, z) in ((9, -12), (13, -11), (10, -13), (12, -9)):
        P.put(x, F + 6, z, ROOTED)
        P.put(x, F + 5, z, ROOTS)
    # sarcophagus on the east side, skulls and candles
    for z in range(-12, -9):                    # against the east wall; x=14 stays a walkway to the SE corner
        P.put(15, F + 1, z, PBB if z != -12 else CHPB)
        P.put(15, F + 2, z, slab("polished_blackstone") if z != -12 else CHPB)
    P.put(15, F + 3, -11, skull(4))
    P.put(15, F + 3, -12, lantern(False))
    P.put(13, F + 2, -11, candle(3))            # on the balustrade post
    for (x, z, f) in ((8, -16, "south"), (14, -16, "south"), (16, -14, "west"), (6, -9, "east"), (16, -8, "west")):
        P.put(x, F + 3, z, AIR)
        P.put(x, F + 3, z, skull(FACE_ROT[f]) if h3(x, 0, z, 58) < .6 else candle(2))
    # north door and the buried dromos (opens into the terrain at the box edge)
    for x in range(10, 13):
        for z in range(-18, Z0):
            P.put(x, F, z, PD)
            for y in range(F + 1, F + 4):
                P.put(x, y, z, AIR)
            P.put(x, F + 4, z, DB)
            P.put(x, F + 5, z, DT)
    for z in range(-18, Z0):
        P.put(9, F + 1, z, PBB)
        P.put(13, F + 1, z, PBB)
        for y in range(F + 2, F + 4):
            P.put(9, y, z, mason(9, y, z, F, 3))
            P.put(13, y, z, mason(13, y, z, F, 3))
        P.put(9, F + 4, z, CHPB)
        P.put(13, F + 4, z, CHPB)
    P.put(10, F + 3, -17, stairs("deepslate_brick", "west", "top"))
    P.put(12, F + 3, -17, stairs("deepslate_brick", "east", "top"))
    for x in range(10, 13):
        for y in range(F + 1, F + 4):
            r = h3(x, y, -18, 64)
            P.put(x, y, -18, CD if r < .45 else (CDB if r < .65 else (ROOTED if r < .85 else BONE_Z)))
    P.put(10, F + 1, -17, stairs("cobbled_deepslate", "north"))
    P.put(12, F + 1, -17, slab("cobbled_deepslate"))
    # partial roof collapse in the south-east corner (the cavity stays inside the box)
    for x in range(13, 18):
        for z in range(-9, -4):
            if vnoise(x, z, 2.0, 59) > .45:
                for y in range(F + 5, top_roof):
                    P.put(x, y, z, AIR if X0 <= x <= X1 and Z0 <= z <= Z1 else None)
    P.passage("mausoleum", [(x, F + 1, z) for x in range(7, 13) for z in (-15, -14)]
              + [(x, F + 1, -13) for x in range(7, 10)], 4)
    P.passage("mausoleum door", [(11, F + 1, z) for z in (-17, -16)], 3)
    heap(P, 14.0, -8.0, 1.6, 1.4, 1, 60, top=F + 4, bottom=F + 1)
    web_spots(P, X0, F + 3, Z0, X1, F + 5, Z1, .4, 61)
    web_spots(P, X0, 6, Z0, X1, F - 1, Z1, .05, 62)
    moss_patches(P, X0, Z0, X1, Z1, F + 3, F + 1, .2, 63)
    DESTINATIONS["mausoleum"] = (8, F + 1, -14)
    DESTINATIONS["shaft bottom"] = (11, 1, -8)


def build_cells_west(P):
    XW, XE = -31, -3              # interior x range
    UP = 5                        # upper floor course
    for x in range(-33, 0):
        for z in range(2, 21):
            P.put(x, 0, z, PBB if z in (2, 20) or x <= -32 or x >= -2 else floor_tiles(x, z, 7))
    # shell walls
    for x in range(-33, 0):
        for z in range(2, 21):
            edge = z in (2, 20) or x <= -32 or x >= -2
            for y in range(1, 13):
                if edge:
                    P.put(x, y, z, mason(x, y, z, 0, 5) if y < 10 else (DB if h3(x, y, z, 70) > .1 else CDB))
                else:
                    P.put(x, y, z, AIR)
            if not edge:
                P.put(x, 13, z, DT)
    # aisle floor: polished strip with drains
    for x in range(XW, XE + 1):
        for z in range(10, 13):
            P.put(x, 0, z, CHPB if (z == 11 and x % 4 == 2) else PB)
    # upper floors and ceilings over cells/balconies
    cells_x = [(-7 - 4 * k, -5 - 4 * k) for k in range(6)]          # (x0, x1) east to west
    dividers = [-4 - 4 * k for k in range(7)]                        # -4 .. -28
    for x in range(XW, XE + 1):
        for z in list(range(3, 10)) + list(range(13, 20)):
            if x >= -29:
                P.put(x, UP, z, PD if z in (6, 16) else DT)
            P.put(x, 10, z, DT)
            for y in (11, 12):                   # solid masonry above the upper tier (no sealed attic)
                P.put(x, y, z, DB if h3(x, y, z, 69) > .1 else CDB)
    for x in range(XW, XE + 1):
        P.put(x, 11, 10, stairs("deepslate_tile", "north", "top"))
        P.put(x, 11, 12, stairs("deepslate_tile", "south", "top"))
        P.put(x, 12, 11, PD if x % 4 == 0 else DT)
        for z in (10, 12):
            P.put(x, 12, z, DT)
    # transverse ribs over the void at every divider
    for x in dividers:
        for z in (10, 11, 12):
            P.put(x, 12, z, PD)
        P.put(x, 11, 11, AIR)
    # dividers / piers along both bars lines and between cells (both tiers)
    for x in dividers:
        for z in list(range(3, 7)) + list(range(16, 20)):
            for y in range(1, 10):
                if y == UP:
                    continue
                P.put(x, y, z, PBB if z in (6, 16) else mason(x, y, z, 0 if y < UP else UP, 3))
    for x in range(XW, -28):
        for z in list(range(3, 7)) + list(range(16, 20)):
            for y in range(1, 10):
                P.put(x, y, z, mason(x, y, z, 0, 5))
    # cells: bars fronts on both tiers, contents
    chest_cells = {("N", 0, 3): "west_cells_lower", ("S", 1, 1): "west_cells_upper"}
    broken = {("N", 0, 3), ("S", 1, 1), ("S", 0, 5), ("N", 1, 0), ("N", 1, 4)}
    lit = {("N", 0, 1), ("S", 0, 4), ("S", 1, 2), ("N", 1, 5)}
    for k, (x0, x1) in enumerate(cells_x):
        for side, zb, zc, zback, face in (("N", 6, (3, 5), 3, "south"), ("S", 16, (17, 19), 19, "north")):
            for tier, fy in ((0, 0), (1, UP)):
                key = (side, tier, k)
                for x in range(x0, x1 + 1):
                    for y in range(fy + 1, fy + 5):
                        gap = key in broken and x == x0 + 1 and y <= fy + 3
                        if key in broken and (x, y) == (x0 + 2, fy + 1) and h3(x, y, zb, 71) < .5:
                            gap = True
                        P.put(x, y, zb, AIR if gap else BARS)
                    for z in range(zc[0], zc[1] + 1):
                        P.put(x, fy, z, rough_floor(x, z, 3))
                # contents
                r = h3(x0, fy, zc[0], 72)
                bx = x0 + int(h3(x0, fy, 1, 73) * 3)
                P.put(bx, fy + 1, zback, BONE_X)
                P.put(x0 + (2 if bx == x0 else 0), fy + 1, zback, skull(FACE_ROT[face] + (1 if r < .5 else -1)))
                P.put(x0 + 1, fy + 4, zback, CHAIN)
                P.put(x0 + 1, fy + 3, zback, CHAIN)
                if r < .4:
                    P.put(x1, fy + 1, (zc[0] + zc[1]) // 2, BONE_Z if side == "N" else BONE_Z)
                if key in lit:
                    P.put(x0 + 1, fy + 1, (zc[0] + zc[1]) // 2, lantern(False))
                if key in chest_cells:
                    cxx = x0 + 1
                    chest(P, chest_cells[key], cxx, fy + 1, zback, face, LOOT_CELLS)
                    P.put(x0, fy + 1, zback, candle(2))
                    P.put(cxx, fy + 3, zback, AIR)
                    P.put(cxx, fy + 4, zback, CHAIN)
                for z in range(zc[0], zc[1] + 1):
                    for x in range(x0, x1 + 1):
                        if P.get(x, fy + 4, z) == AIR and h3(x, fy + 4, z, 74) < .35:
                            P.put(x, fy + 4, z, WEB_SPOT)
    # balconies: railing of bars on the void edge, bridge and stair at the west end
    for x in range(-28, XE + 1):
        P.put(x, UP + 1, 9, BARS)
        P.put(x, UP + 1, 13, BARS)
    for z in range(7, 16):
        P.put(-29, UP, z, PD if z in (10, 11, 12) else DT)
    for z in (10, 11, 12):
        P.put(-28, UP + 1, z, BARS)
        P.put(-28, UP, z, PBB)
    P.put(-29, UP + 1, 9, AIR)
    P.put(-29, UP + 1, 13, AIR)
    for x in (-31, -30):
        for z in range(7, 10):
            P.put(x, UP, z, DT)
        for k, z in enumerate((14, 13, 12, 11, 10)):
            h = k + 1
            for y in range(1, h):
                P.put(x, y, z, mason(x, y, z, 0, 5))
            P.put(x, h, z, stairs("deepslate_brick", "north"))
    # passages first: decoration never goes into their clear zones
    P.passage("west cells aisle", [(x, 1, z) for x in range(-29, XE + 1) for z in range(10, 15)], 4)
    P.passage("west cells door", [(x, 1, z) for x in (-2, -1) for z in range(10, 13)], 3)
    P.passage("west balcony north", [(x, UP + 1, z) for x in range(-31, XE + 1) for z in (7, 8)], 4)
    P.passage("west balcony south", [(x, UP + 1, z) for x in range(-29, XE + 1) for z in (14, 15)], 4)
    P.passage("west bridge", [(-29, UP + 1, z) for z in range(9, 14)], 4)
    P.passage("west stair", [(x, k + 2, z) for x in (-31, -30) for k, z in enumerate((14, 13, 12, 11, 10))], 3)
    # balcony collapse: the outer edge of the north balcony fell in
    for x in range(-21, -16):
        if h3(x, UP, 9, 75) < .8:
            P.put(x, UP, 9, AIR)
            P.put(x, UP + 1, 9, AIR)
    heap(P, -19.0, 9.0, 2.6, 1.3, 2, 76, top=4)
    # hanging lanterns in the void, lanterns on the east door piers
    for x in (-26, -15, -6):
        hanging_lantern(P, x, 11, 11, 7)
    # east door to the rotunda
    for z in range(10, 13):
        for x in (-2, -1):
            P.put(x, 0, z, PD if z != 11 else PB)
            for y in range(1, 5):
                P.put(x, y, z, AIR)
    for x in (-2, -1):
        P.put(x, 4, 10, stairs("polished_blackstone_brick", "north", "top"))
        P.put(x, 4, 12, stairs("polished_blackstone_brick", "south", "top"))
        for y in range(1, 6):
            P.put(x, y, 9, CHPB if y == 5 else PBB)
            P.put(x, y, 13, CHPB if y == 5 else PBB)
    web_spots(P, XW, 8, 3, XE, 9, 19, .12, 77)
    web_spots(P, XW, 10, 10, XE, 11, 12, .2, 78)
    moss_patches(P, XW, 7, XE, 15, 3, 1, .1, 79)
    DESTINATIONS["west cells aisle"] = (-15, 1, 12)
    DESTINATIONS["west upper north balcony"] = (-10, UP + 1, 8)
    DESTINATIONS["west upper south balcony"] = (-20, UP + 1, 15)
    DESTINATIONS["west lower chest cell"] = (-18, 1, 4)
    DESTINATIONS["west upper chest cell"] = (-10, UP + 1, 18)


def build_cells_east(P):
    # entry corridor, hewn, with masonry ribs
    for x in range(23, 31):
        for z in range(10, 13):
            P.put(x, 0, z, rough_floor(x, z, 8))
            for y in range(1, 5):
                P.put(x, y, z, AIR)
    for x in (25, 28):
        for y in range(1, 5):
            P.put(x, y, 9, PBB if y < 4 else CHPB)
            P.put(x, y, 13, PBB if y < 4 else CHPB)
        for z in range(9, 14):
            P.put(x, 5, z, DB)
        P.put(x, 4, 10, stairs("deepslate_brick", "north", "top"))
        P.put(x, 4, 12, stairs("deepslate_brick", "south", "top"))
    P.put(26, 3, 9, wall_skull("south"))
    P.put(29, 2, 13, wall_skull("north"))
    # guard room
    GX0, GX1, GZ0, GZ1 = 31, 39, 6, 16
    for x in range(GX0 - 1, GX1 + 2):
        for z in range(GZ0 - 1, GZ1 + 2):
            edge = x in (GX0 - 1, GX1 + 1) or z in (GZ0 - 1, GZ1 + 1)
            P.put(x, 0, z, PBB if edge else floor_tiles(x, z, 9))
            for y in range(1, 6):
                P.put(x, y, z, mason(x, y, z, 0, 4) if edge else AIR)
            P.put(x, 6, z, DT if not edge else DB)
    for (x, z) in ((GX0, GZ0), (GX1, GZ0), (GX0, GZ1), (GX1, GZ1)):
        for y in range(1, 6):
            P.put(x, y, z, PD if y != 5 else CHD)
    for z in range(10, 13):
        for y in range(1, 5):
            P.put(GX0 - 1, y, z, AIR)
        P.put(GX0 - 1, 0, z, PD)
    for x in range(34, 37):
        for zz in (GZ0 - 1, GZ1 + 1):
            for y in range(1, 4):
                P.put(x, y, zz, AIR)
            P.put(x, 4, zz, mason(x, 4, zz, 0, 4))
            P.put(x, 0, zz, PD)
    for zz in (GZ0 - 1, GZ1 + 1):
        P.put(34, 3, zz, stairs("deepslate_brick", "west", "top"))
        P.put(36, 3, zz, stairs("deepslate_brick", "east", "top"))
    # table, benches, rack, chest, lantern, the last guard
    for x in (35, 36):
        for z in (10, 11, 12):
            P.put(x, 1, z, slab("polished_blackstone", "top"))
    for z in (10, 11, 12):
        P.put(34, 1, z, stairs("deepslate_tile", "east"))
        P.put(37, 1, z, stairs("deepslate_tile", "west"))
    P.put(35, 2, 11, skull(12))
    P.put(36, 2, 10, candle(2))
    P.put(36, 2, 12, lantern(False))
    P.put(34, 1, 12, BONE_X)
    chest(P, "gaol guard room", GX1, 1, GZ0 + 1, "west", LOOT_CELLS)
    P.put(GX1, 1, GZ0 + 2, M("barrel[facing=up,open=false]"))
    P.put(GX1, 2, GZ0 + 2, candle(1))
    for z in (13, 14, 15):
        P.put(GX1, 2, z, CHAIN_Z)
        P.put(GX1, 1, z, BONE if z == 14 else AIR)
    P.put(GX1, 3, 14, wall_skull("west"))
    for x in (32, 33):
        P.put(x, 3, GZ1, CHAIN_X)
    # north wing: corridor with hewn cells, ends in a cave-in
    for x in range(34, 37):
        for z in range(-3, 5):
            P.put(x, 0, z, rough_floor(x, z, 10))
            for y in range(1, 5):
                P.put(x, y, z, AIR)
    for (zc0, zc1) in ((-2, 0), (2, 4)):
        for (bx, cx0, cx1, face) in ((33, 30, 32, "east"), (37, 38, 40, "west")):
            for z in range(zc0, zc1 + 1):
                for y in range(1, 4):
                    P.put(bx, y, z, BARS)
                P.put(bx, 4, z, PBB)
                for x in range(cx0, cx1 + 1):
                    P.put(x, 0, z, rough_floor(x, z, 11))
                    for y in range(1, 4):
                        P.put(x, y, z, AIR)
            for y in range(1, 5):
                P.put(bx, y, zc0 - 1, PBB)
                P.put(bx, y, zc1 + 1, PBB)
            backx = cx0 if face == "east" else cx1
            P.put(backx, 1, zc0, BONE_Z)
            P.put(backx, 1, zc1, skull(FACE_ROT[face]))
            P.put(backx, 3, zc0 + 1, CHAIN)
            P.put(backx, 2, zc0 + 1, CHAIN)
    # the west-south cell holds a chest and is broken open; the east-north one has collapsed
    # passages first: decoration never goes into their clear zones
    P.passage("gaol corridor", [(x, 1, 11) for x in range(23, 31)], 3)
    P.passage("gaol guard room", [(x, 1, z) for x in (32, 33, 38) for z in range(7, 16)], 4)
    P.passage("gaol north wing", [(35, 1, z) for z in range(-1, 6)], 3)
    P.passage("gaol south wing", [(35, 1, z) for z in range(16, 25)], 3)
    for y in range(1, 4):
        P.put(33, y, 3, AIR)
    chest(P, "gaol north cell", 30, 1, 3, "east", LOOT_CELLS)
    P.put(30, 1, 2, AIR)
    for z in range(-2, 1):
        for y in range(1, 3):
            if h3(37, y, z, 81) < .6:
                P.put(37, y, z, AIR)
    heap(P, 39.0, -1.0, 1.4, 1.4, 2, 82, top=3)
    heap(P, 35.0, -3.0, 1.6, 1.2, 2, 83, top=3)
    # south wing: corridor with cells, a cave-in squeezes it toward the breach tunnel
    for x in range(34, 37):
        for z in range(18, 25):
            P.put(x, 0, z, rough_floor(x, z, 12))
            for y in range(1, 4 if z >= 22 else 5):
                P.put(x, y, z, AIR)
    for (bx, cx0, cx1, face) in ((33, 30, 32, "east"), (37, 38, 40, "west")):
        for z in range(19, 22):
            for y in range(1, 4):
                P.put(bx, y, z, BARS if not (bx == 37 and z == 20 and y < 3) else AIR)
            P.put(bx, 4, z, PBB)
            for x in range(cx0, cx1 + 1):
                P.put(x, 0, z, rough_floor(x, z, 13))
                for y in range(1, 4):
                    P.put(x, y, z, AIR)
        for y in range(1, 5):
            P.put(bx, y, 18, PBB)
            P.put(bx, y, 22, PBB)
        backx = cx0 if face == "east" else cx1
        P.put(backx, 1, 20, BONE_Z)
        P.put(backx, 2, 20, skull(FACE_ROT[face]))
        P.put(backx, 1, 21, lantern(False) if face == "west" else candle(3))
    P.put(34, 1, 23, CD)
    P.put(34, 2, 23, slab("cobbled_deepslate"))
    P.put(36, 1, 22, stairs("cobbled_deepslate", "west"))
    P.put(36, 1, 24, CDB)
    web_spots(P, 23, 3, -3, 41, 5, 24, .3, 84)
    moss_patches(P, 23, -3, 41, 24, 4, 1, .12, 85)
    DESTINATIONS["gaol guard room"] = (32, 1, 11)
    DESTINATIONS["gaol north cell"] = (31, 1, 3)
    DESTINATIONS["gaol south wing"] = (35, 1, 20)


def build_gallery(P):
    Z0, Z1 = 25, 50                 # interior rows
    piers = (27, 31, 35, 39, 43, 47)
    for x in range(3, 20):
        for z in range(23, 53):
            inner = 5 <= x <= 17 and Z0 <= z <= Z1
            if not inner:
                P.put(x, 0, z, PBB)
                for y in range(1, 9):
                    P.put(x, y, z, mason(x, y, z, 0, 5))
                continue
            if 9 <= x <= 13:
                f = PB if x == 11 else floor_tiles(x, z, 14)
            else:
                f = rough_floor(x, z, 15) if h3(x, 0, z, 90) < .35 else floor_tiles(x, z, 16)
            P.put(x, 0, z, f)
    # ledger stones in the nave
    for zc in (29, 33, 37, 41, 45, 49):
        for z in (zc, zc + 1):
            for x in (10, 12):
                P.put(x, 0, z, PBB)
        P.put(11, 0, zc, CHPB)
    # aisles (ceiling y=6, corbelled), arcades, nave vault
    for z in range(Z0, Z1 + 1):
        for x in list(range(5, 8)) + list(range(15, 18)):
            for y in range(1, 6):
                P.put(x, y, z, AIR)
            P.put(x, 6, z, DT)
            P.put(x, 7, z, DB)
        P.put(5, 5, z, stairs("deepslate_tile", "west", "top"))
        P.put(17, 5, z, stairs("deepslate_tile", "east", "top"))
        for x in (8, 14):
            pier = z in piers
            for y in range(1, 9):
                if pier or y >= 6:
                    P.put(x, y, z, (CHPB if y in (1, 5) else PB) if pier and y <= 5 else mason(x, y, z, 0, 5))
                else:
                    P.put(x, y, z, AIR)
            if not pier and (z - 1 in piers or z + 1 in piers):
                P.put(x, 5, z, stairs("deepslate_brick", "north" if z - 1 in piers else "south", "top"))
        for x in range(9, 14):
            for y in range(1, 11):
                P.put(x, y, z, AIR)
        P.put(9, 9, z, stairs("deepslate_brick", "west", "top"))
        P.put(13, 9, z, stairs("deepslate_brick", "east", "top"))
        P.put(9, 10, z, DB)
        P.put(13, 10, z, DB)
        P.put(10, 10, z, stairs("deepslate_brick", "west", "top"))
        P.put(12, 10, z, stairs("deepslate_brick", "east", "top"))
        for x in range(9, 14):
            P.put(x, 11, z, PD if z in piers else DB)
            P.put(x, 12, z, DT)
        if z in piers:
            for x in (9, 13):
                P.put(x, 9, z, PD)
            for x in (10, 12):
                P.put(x, 10, z, PD)
    # outer walls: loculi in two rows facing the aisles; skull alcoves; the vault door on the east
    alcoves = {"west": (36, 38), "east": (30, 32)}
    door_z = (40, 42)
    for wall_x, face, side in ((4, "east", "west"), (18, "west", "east")):
        cells_low, cells_high = [], []
        for z in range(Z0, Z1 + 1):
            a0, a1 = alcoves[side]
            if a0 - 1 <= z <= a1 + 1:
                continue
            if side == "east" and door_z[0] - 1 <= z <= door_z[1] + 1:
                continue
            if (z - Z0) % 3 == 2:
                continue
            cells_low.append((wall_x, 1, z))
            cells_high.append((wall_x, 3, z))
        for y in (2, 4):
            for z in range(Z0, Z1 + 1):
                P.put(wall_x, y, z, mason(wall_x, y, z, 0, 5))
        loculi_wall(P, cells_low, face, 91 + (wall_x == 18))
        loculi_wall(P, cells_high, face, 93 + (wall_x == 18))
        a0, a1 = alcoves[side]
        back = wall_x - 1 if side == "west" else wall_x + 1
        for z in range(a0, a1 + 1):
            for x in (wall_x, back):
                for y in range(1, 4):
                    P.put(x, y, z, AIR)
                P.put(x, 0, z, CD)
            P.put(back, 1, z, BONE)
            P.put(back, 2, z, BONE if z == (a0 + a1) // 2 else skull(FACE_ROT[face]))
            P.put(wall_x, 1, z, BONE_Z if z != (a0 + a1) // 2 else BONE)
            P.put(wall_x, 2, z, skull(FACE_ROT[face] + (1 if z == a0 else (-1 if z == a1 else 0))))
        P.put(back, 3, (a0 + a1) // 2, lantern(False))
        P.put(wall_x, 4, a0, stairs("polished_blackstone_brick", "north", "top"))
        P.put(wall_x, 4, a1, stairs("polished_blackstone_brick", "south", "top"))
        P.put(wall_x, 4, (a0 + a1) // 2, CHPB)
    for z in range(door_z[0], door_z[1] + 1):
        for x in (18, 19, 20):
            P.put(x, 0, z, PD)
            for y in range(1, 5):
                P.put(x, y, z, AIR)
            P.put(x, 5, z, DB)
    for x in (18, 19, 20):
        for y in range(1, 6):
            P.put(x, y, door_z[0] - 1, CHPB if y == 5 else PBB)
            P.put(x, y, door_z[1] + 1, CHPB if y == 5 else PBB)
    P.put(20, 0, 41, PD)
    # north and south doors
    for (zz, h) in (((23, 24), 5), ((51, 52), 6)):
        for z in zz:
            for x in range(9, 14):
                P.put(x, 0, z, PD if x != 11 else PB)
                for y in range(1, h + 1):
                    P.put(x, y, z, AIR)
            P.put(9, h, z, stairs("polished_blackstone_brick", "west", "top"))
            P.put(13, h, z, stairs("polished_blackstone_brick", "east", "top"))
            for y in range(1, h + 1):
                P.put(8, y, z, PBB if y < h else CHPB)
                P.put(14, y, z, PBB if y < h else CHPB)
    # chain curtains hang at the sides of the chapel door
    for x in (9, 13):
        for y in (5, 6):
            P.put(x, y, 52, CHAIN) if P.get(x, y, 52) == AIR else None
    # lighting: two hanging lanterns over the nave, dark stretches between
    for z in (29, 45):
        hanging_lantern(P, 11, z, 10, 7)
    # collapse in the west aisle: vault fallen, bones spilled
    for z in range(34, 40):
        for x in range(5, 8):
            if vnoise(x, z, 2.0, 95) > .3:
                for y in range(6, 9):
                    P.put(x, y, z, AIR)
        for x in (3, 4):
            if vnoise(x, z, 2.0, 96) > .55:
                for y in range(3, 6):
                    P.put(x, y, z, AIR)
    # passages first: decoration never goes into their clear zones
    P.passage("gallery nave", [(x, 1, z) for x in range(9, 14) for z in range(23, 53)], 4)
    P.passage("gallery east aisle", [(x, 1, z) for x in (16, 17) for z in range(Z0, Z1 + 1)], 4)
    P.passage("gallery west aisle north", [(x, 1, z) for x in (6, 7) for z in range(Z0, 33)], 4)
    P.passage("gallery vault door", [(x, 1, z) for x in (18, 19, 20) for z in range(door_z[0], door_z[1] + 1)], 4)
    heap(P, 6.0, 36.5, 1.6, 2.6, 2, 97, top=5)
    P.clear_zones.append((9, 1, Z0, 13, 6, Z1))
    P.clear_zones.append((15, 1, 39, 17, 4, 43))
    web_spots(P, 5, 4, Z0, 17, 10, Z1, .22, 98)
    moss_patches(P, 5, Z0, 17, Z1, 3, 1, .12, 99)
    DESTINATIONS["gallery nave"] = (11, 1, 38)
    DESTINATIONS["gallery west aisle"] = (6, 1, 28)
    DESTINATIONS["gallery east aisle"] = (16, 1, 48)


def build_vault(P):
    RX0, RX1, RZ0, RZ1 = 27, 39, 34, 48           # room interior
    # approach corridor with a broken gate
    for x in range(21, 26):
        for z in range(39, 44):
            edge = z in (39, 43)
            P.put(x, 0, z, PBB if edge else (PD if z == 41 else floor_tiles(x, z, 17)))
            for y in range(1, 6):
                P.put(x, y, z, mason(x, y, z, 0, 4) if edge else (AIR if y <= 4 else DB))
            P.put(x, 6, z, DT)
    for z in (40, 41, 42):
        P.put(23, 4, z, BARS if z != 41 else AIR)
    for z in (40, 42):
        for y in range(1, 5):
            P.put(24, y, z, BARS)
    P.put(24, 4, 41, BARS)
    P.put(25, 1, 40, BARS)                       # a bent bar lying inside
    # room shell
    for x in range(RX0 - 1, RX1 + 3):
        for z in range(RZ0 - 1, RZ1 + 2):
            edge = x in (RX0 - 1, RX1 + 1, RX1 + 2) or z in (RZ0 - 1, RZ1 + 1)
            P.put(x, 0, z, PBB if edge else floor_tiles(x, z, 18))
            for y in range(1, 9):
                if edge:
                    P.put(x, y, z, mason(x, y, z, 0, 5))
                else:
                    P.put(x, y, z, AIR if y < 8 else DT)
            P.put(x, 9, z, DB)
    for x in range(RX0, RX1 + 1):
        P.put(x, 7, RZ0, stairs("deepslate_brick", "north", "top"))
        P.put(x, 7, RZ1, stairs("deepslate_brick", "south", "top"))
    for z in range(RZ0, RZ1 + 1):
        P.put(RX0, 7, z, stairs("deepslate_brick", "west", "top"))
        P.put(RX1, 7, z, stairs("deepslate_brick", "east", "top"))
    for x in range(RX0, RX1 + 1):        # floor: blackstone cross and border
        for z in range(RZ0, RZ1 + 1):
            if x in (RX0, RX1) or z in (RZ0, RZ1):
                P.put(x, 0, z, PBB)
            elif z == 41 or x == 33:
                P.put(x, 0, z, PB)
    # door from the approach
    for z in (40, 41, 42):
        P.put(26, 0, z, PD)
        for y in range(1, 5):
            P.put(26, y, z, AIR)
        P.put(26, 5, z, mason(26, 5, z, 0, 5))
    P.put(26, 4, 40, stairs("polished_blackstone_brick", "north", "top"))
    P.put(26, 4, 42, stairs("polished_blackstone_brick", "south", "top"))
    # pillars with capitals
    for (px, pz) in ((30, 37), (36, 37), (30, 45), (36, 45)):
        for y in range(1, 8):
            P.put(px, y, pz, CHPB if y in (1, 6) else PB)
        for (dx, dz, f) in ((1, 0, "west"), (-1, 0, "east"), (0, 1, "north"), (0, -1, "south")):
            P.put(px + dx, 7, pz + dz, stairs("polished_blackstone", f, "top"))
    # the guardian's dais and its open sarcophagus (it rose from it)
    for x in range(31, 36):
        for z in range(38, 45):
            P.put(x, 1, z, PBB if x in (31, 35) or z in (38, 44) else PD)
    for z in range(38, 45):
        P.put(30, 1, z, stairs("polished_blackstone_brick", "east")) if (30, 1, z) != (30, 1, 37) else None
        P.put(36, 1, z, stairs("polished_blackstone_brick", "west")) if z not in (37, 45) else None
    for x in range(31, 36):
        P.put(x, 1, 45, stairs("polished_blackstone_brick", "north"))
        P.put(x, 1, 37, stairs("polished_blackstone_brick", "south"))
    for (x, z) in ((32, 38), (34, 38), (32, 39), (34, 39)):
        P.put(x, 2, z, PBB)
    P.put(33, 2, 38, CHPB)
    P.put(33, 2, 39, AIR)
    P.put(32, 3, 38, slab("polished_blackstone_brick"))
    P.put(34, 3, 38, slab("polished_blackstone_brick"))
    P.put(31, 2, 39, slab("polished_blackstone", "bottom"))   # the lid, shoved aside
    P.put(31, 2, 40, slab("polished_blackstone", "bottom"))
    # corner sarcophagi with skulls and candles
    for (sx, sz, along) in ((28, 35, "z"), (38, 35, "z"), (28, 46, "z"), (38, 46, "z")):
        for k in range(3):
            x, z = (sx, sz + k) if along == "z" else (sx + k, sz)
            if sz > 41:
                z = sz - k + 1
            P.put(x, 1, z, CHPB if k == 0 else PBB)
            P.put(x, 2, z, slab("polished_blackstone"))
        P.put(sx, 3, sz + 1 if sz < 41 else sz, skull(4 if sx < 33 else 12) if h3(sx, 3, sz, 100) < .6 else candle(2))
    # chest apse on the east wall with soul-fire braziers
    for z in (40, 41, 42):
        for y in range(1, 5):
            P.put(40, y, z, AIR)
        P.put(40, 0, z, PBB)
        P.put(40, 5, z, PD)
    P.put(40, 4, 40, stairs("polished_blackstone_brick", "north", "top"))
    P.put(40, 4, 42, stairs("polished_blackstone_brick", "south", "top"))
    chest(P, "burial vault", 40, 1, 41, "west", LOOT_VAULT)
    P.put(40, 1, 40, candle(4))
    P.put(40, 1, 42, candle(3))
    P.put(40, 2, 41, AIR)
    P.put(40, 3, 41, wall_skull("west"))
    for z in (38, 44):
        P.put(39, 0, z, SOUL_SOIL)
        P.put(39, 1, z, SOUL_FIRE)
        P.put(39, 2, z, AIR)
    # wall loculi along the north and south walls
    for wall_z, face in ((RZ0 - 1, "south"), (RZ1 + 1, "north")):
        low = [(x, 2, wall_z) for x in range(RX0 + 1, RX1) if (x - RX0) % 3 != 0 and x not in (33,)]
        loculi_wall(P, low, face, 101 + (wall_z > 40))
        high = [(x, 4, wall_z) for x in range(RX0 + 1, RX1) if (x - RX0) % 3 != 0 and x not in (33,)]
        loculi_wall(P, high, face, 103 + (wall_z > 40))
    # chain garlands along the walls, lanterns high enough for the guardian
    for x in range(RX0 + 1, RX1):
        if x % 4 != 1:
            P.put(x, 6, RZ0, CHAIN_X)
            P.put(x, 6, RZ1, CHAIN_X)
    for (x, z) in ((30, 41), (36, 41)):
        P.put(x, 7, z, CHAIN)
        P.put(x, 6, z, lantern(True))
    # breach tunnel from the gaol: hewn, 3 tall (the guardian cannot follow), through a cracked wall
    for x in range(34, 37):
        for z in range(25, RZ0):
            P.put(x, 0, z, rough_floor(x, z, 19))
            for y in range(1, 4):
                P.put(x, y, z, AIR)
    for z in range(26, 32):
        if z % 2:
            P.put(35, 3, z, AIR)
    for (x, y) in ((33, 1), (33, 2), (37, 1), (37, 3), (34, 4), (36, 4), (35, 4)):
        P.put(x, y, RZ0 - 1, CDB)
    # passages first: decoration never goes into their clear zones. The gate and the approach are 3 tall on
    # purpose: the 3.3-tall guardian cannot follow through them (nor through the 3-tall breach tunnel).
    P.passage("vault approach", [(x, 1, z) for x in range(21, 24) for z in (40, 41, 42)], 3)
    P.passage("vault gate", [(x, 1, 41) for x in range(21, 27)], 3)
    P.passage("vault room north", [(x, 1, z) for x in range(RX0 + 2, RX1 - 1) for z in (35, 36)], 4)
    P.passage("vault room south", [(x, 1, z) for x in range(RX0 + 2, RX1 - 1) for z in (46, 47)], 4)
    P.passage("vault dais", [(x, 2, z) for x in range(32, 35) for z in range(40, 44)], 4)
    P.passage("vault breach tunnel", [(35, 1, z) for z in range(25, RZ0 + 1)], 3)
    heap(P, 35.0, 34.3, 2.2, 0.6, 1, 105, top=3)       # wall debris either side of the breach
    P.put(34, 1, 27, BONE_X)
    web_spots(P, RX0, 5, RZ0, RX1, 7, RZ1, .3, 106)
    web_spots(P, 34, 2, 25, 36, 3, 32, .25, 107)
    moss_patches(P, RX0, RZ0, RX1, RZ1, 2, 1, .1, 108)
    P.entity("mob", GUARDIAN, 90.0, id=f"{ENTITY_NS}:burial_guardian")
    DESTINATIONS["vault approach"] = (22, 1, 41)
    DESTINATIONS["burial vault"] = (29, 1, 41)
    DESTINATIONS["vault chest"] = (39, 1, 41)
    DESTINATIONS["breach tunnel"] = (35, 1, 28)


def build_chapel(P):
    ax, ay, az = ALTAR_POS
    F = ay - 1                      # arena floor course (-6)
    X0, X1, Z0, Z1 = -3, 25, 54, 85  # hall interior
    AX0, AZ0, AX1, AZ1 = ARENA[0], ARENA[1], ARENA[2], ARENA[3]

    def vault_y(x, z):
        u = max(abs(x - ax), abs(z - az))
        return 5 + int(round(5 * max(0.0, 1 - (u / 15.0) ** 2)))

    # shell: walls, floor, vault
    for x in range(-5, 28):
        for z in range(53, 88):
            inside = X0 <= x <= X1 and Z0 <= z <= Z1
            P.put(x, F - 1, z, PBB)
            P.put(x, F, z, PBB)
            if not inside:
                for y in range(F + 1, 10):
                    P.put(x, y, z, mason(x, y, z, F, 6) if z != 53 else mason(x, y, z, 0, 5))
                continue
            c = vault_y(x, z)
            for y in range(F + 1, c):
                P.put(x, y, z, AIR)
            dx, dz = x - ax, z - az
            rib = abs(dx) == abs(dz) or dx == 0 or dz == 0
            P.put(x, c, z, PD if rib else (DB if h3(x, c, z, 110) > .1 else CDB))
            if c + 1 <= 11:
                P.put(x, c + 1, z, DT)
    # arena floor: rings and a cross of blackstone, tiles elsewhere
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            dx, dz = x - ax, z - az
            u = max(abs(dx), abs(dz))
            d = math.hypot(dx, dz)
            if AX0 <= x <= AX1 and AZ0 <= z <= AZ1:
                if u in (11, 7):
                    s = PBB
                elif d < 1.6:
                    s = CHPB
                elif 2.5 <= d < 3.6:
                    s = PB
                elif 4.6 <= d < 5.4:
                    s = PD
                elif dx == 0 or dz == 0:
                    s = PB
                else:
                    r = h3(x, F, z, 111)
                    s = DT if r > .16 else CDT
            else:
                s = PBB if h3(x, F, z, 112) > .1 else CPBB
            P.put(x, F, z, s)
    # north: landing at the gallery level, stair down, balconies over the north cells
    for x in range(X0, X1 + 1):
        for z in range(53, 60):
            for y in range(F + 1, 1):
                P.put(x, y, z, mason(x, y, z, F, 6))
            P.put(x, 0, z, PD if (x + z) % 3 else DT)
    for x in range(9, 14):
        P.put(x, 0, 53, PB if x == 11 else PD)
        for y in range(1, 7):
            P.put(x, y, 53, AIR)
        for k in range(1, 7):          # six steps: the top one is flush with the landing, the last sits on the floor
            z = 53 + k
            sy = 1 - k
            for y in range(sy + 1, 5):
                P.put(x, y, z, AIR)
            P.put(x, sy, z, stairs("polished_blackstone_brick", "north"))
            for y in range(F + 1, sy):
                P.put(x, y, z, PBB)
    # arena fog in the chapel door: challengers pass, the Warden stays
    for x in range(9, 14):
        for y in range(1, 7):
            if P.get(x, y, 53) == AIR:
                P.put(x, y, 53, f"{NS}:fog_gate", force=True)
    for y in range(1, 6):
        P.put(8, y, 53, PBB if y < 5 else CHPB)
        P.put(14, y, 53, PBB if y < 5 else CHPB)
    P.put(9, 5, 53, stairs("polished_blackstone_brick", "west", "top"))
    P.put(13, 5, 53, stairs("polished_blackstone_brick", "east", "top"))
    for x in list(range(X0, 9)) + list(range(14, X1 + 1)):
        for z in range(54, 60):
            for y in range(1, vault_y(x, z)):
                P.put(x, y, z, AIR)
    for x in list(range(X0, 9)) + list(range(14, X1 + 1)):
        P.put(x, 1, 59, wallb("deepslate_brick"))
    for z in range(55, 60):            # stair parapets; at z=54 they open onto the balconies (level with the top step)
        P.put(8, 1, z, wallb("deepslate_brick"))
        P.put(14, 1, z, wallb("deepslate_brick"))
    for (x, z) in ((2, 55), (20, 55)):
        P.put(x, 1, z, CHPB)
        P.put(x, 2, z, lantern(False))
    def cell_roof(x, y, z):
        """Bottom slabs: the cornice over the arena cells is no spawning ledge (mobs cannot spawn on bottom slabs)."""
        return slab("deepslate_tile" if h3(x, y, z, 119) > .15 else "cobbled_deepslate")

    # cells around the arena (west, east, south and north under the balconies)
    def cell_line(cells, bars_cells, contents_fn):
        for c in cells:
            P.put(c[0], c[1], c[2], AIR)
        for b in bars_cells:
            P.put(b[0], b[1], b[2], BARS)
        contents_fn()

    piers_z = [59, 63, 67, 71, 75, 79, 83]
    piers_x = [-1, 3, 7, 15, 19, 23]
    broken = {("W", 75), ("E", 63), ("S", 19), ("N", 3)}
    lit = {("W", 63), ("E", 79), ("S", 3), ("N", 19)}
    for side, line, back0, back1 in (("W", -1, -3, -2), ("E", 23, 24, 25)):
        for i in range(len(piers_z) - 1):
            za, zb = piers_z[i] + 1, piers_z[i + 1] - 1
            for z in range(za, zb + 1):
                P.put(line, F, z, PBB)
                for y in range(F + 1, F + 5):
                    gap = (side, za - 1) in broken and z == za + 1 and y <= F + 3
                    P.put(line, y, z, AIR if gap else BARS)
                for x in range(back0, back1 + 1):
                    P.put(x, F, z, rough_floor(x, z, 20))
                    for y in range(F + 1, F + 5):
                        P.put(x, y, z, AIR)
                    P.put(x, F + 5, z, cell_roof(x, F + 5, z))
                P.put(line, F + 5, z, PBB)
            backx = back0 if side == "W" else back1
            face = "east" if side == "W" else "west"
            P.put(backx, F + 1, za, BONE_Z)
            P.put(backx, F + 1, zb, skull(FACE_ROT[face] + 1))
            P.put(backx, F + 4, za + 1, CHAIN)
            P.put(backx, F + 3, za + 1, CHAIN)
            if (side, za - 1) in lit:
                P.put(backx, F + 1, za + 1, lantern(False))
            elif h3(backx, F, za, 113) < .5:
                P.put(backx, F + 1, za + 1, BONE)
                P.put(backx, F + 2, za + 1, skull(FACE_ROT[face]))
        for z in piers_z:
            for y in range(F + 1, F + 6):
                P.put(line, y, z, CHPB if y == F + 5 else PB)
            for x in range(back0, back1 + 1):
                for y in range(F + 1, F + 6):
                    P.put(x, y, z, mason(x, y, z, F, 6))
    # south cells, with the Warden's own great cell in the middle
    for i in range(len(piers_x) - 1):
        xa, xb = piers_x[i] + 1, piers_x[i + 1] - 1
        great = xa == 8
        if great:
            xa, xb = 8, 14
        for x in range(xa, xb + 1):
            P.put(x, F, 83, PBB)
            for y in range(F + 1, F + 5):
                gap = (great and 10 <= x <= 12 and y <= F + 3) or (("S", xa - 1) in broken and x == xa + 1 and y <= F + 3)
                P.put(x, y, 83, AIR if gap else BARS)
            P.put(x, F + 5, 83, PBB)
            for z in (84, 85):
                P.put(x, F, z, rough_floor(x, z, 21) if not great else PBB)
                for y in range(F + 1, F + 5):
                    P.put(x, y, z, AIR)
                P.put(x, F + 5, z, cell_roof(x, F + 5, z))   # (the great cell's roof is lifted below)
        if great:
            for x in range(8, 15):
                P.put(x, F + 5, 84, AIR)
                P.put(x, F + 5, 85, AIR)
                P.put(x, F + 6, 84, PBB)
                P.put(x, F + 6, 85, PBB)
            P.put(11, F + 1, 85, CHPB)
            P.put(11, F + 2, 85, wallb("polished_blackstone_brick"))
            P.put(11, F + 3, 85, wallb("polished_blackstone_brick"))
            for (x, z) in ((9, 84), (13, 84), (10, 85), (12, 85)):
                for y in range(F + 2, F + 6):
                    P.put(x, y, z, CHAIN)
            P.put(9, F + 1, 84, skull(8))
            P.put(13, F + 1, 84, skull(7))
            P.put(10, F + 1, 85, BONE)
            P.put(12, F + 1, 85, BONE_X)
            for x in (8, 14):
                P.put(x, F + 1, 85, lantern(False))
        else:
            P.put(xa, F + 1, 85, BONE_X)
            P.put(xb, F + 1, 85, skull(8 + (1 if xa < 11 else -1)))
            P.put(xa + 1, F + 4, 85, CHAIN)
            P.put(xa + 1, F + 3, 85, CHAIN)
            if ("S", xa - 1) in lit:
                P.put(xa + 1, F + 1, 84, lantern(False))
    for x in piers_x:
        if x in (7, 15):
            for y in range(F + 1, F + 7):
                P.put(x, y, 83, CHPB if y in (F + 5, F + 6) else PB)
                for z in (84, 85):
                    P.put(x, y, z, mason(x, y, z, F, 6))
            continue
        for y in range(F + 1, F + 6):
            P.put(x, y, 83, CHPB if y == F + 5 else PB)
        for z in (84, 85):
            for y in range(F + 1, F + 6):
                P.put(x, y, z, mason(x, y, z, F, 6))
    for x in (-3, -2, 24, 25):
        for z in (84, 85):
            for y in range(F + 1, F + 5):
                P.put(x, y, z, mason(x, y, z, F, 6))
            P.put(x, F + 5, z, cell_roof(x, F + 5, z))
    # north cells under the balconies (bars at z=59)
    for i, (xa, xb) in enumerate(((-3, -1), (0, 2), (4, 6), (16, 18), (20, 22), (23, 25))):
        for x in range(xa, xb + 1):
            P.put(x, F, 59, PBB)
            for y in range(F + 1, F + 5):
                gap = ("N", xa - 1) in broken and x == xa + 1 and y <= F + 3
                P.put(x, y, 59, AIR if gap else BARS)
            for z in (57, 58):
                P.put(x, F, z, rough_floor(x, z, 22))
                for y in range(F + 1, F + 5):
                    P.put(x, y, z, AIR)
        P.put(xa, F + 1, 57, BONE_X)
        P.put(xb, F + 1, 57, skull(0))
        P.put(xa + 1, F + 4, 57, CHAIN)
        if ("N", xa - 1) in lit:
            P.put(xa + 1, F + 1, 58, lantern(False))
    for x in (-1, 3, 7, 8, 14, 15, 19, 23):
        if x in (8, 14):
            continue
        for y in range(F + 1, F + 5):
            P.put(x, y, 59, PB)
    # soul-fire braziers on the pier tops around the arena
    for (x, z) in ((-1, 63), (-1, 79), (23, 63), (23, 79), (3, 83), (19, 83), (-1, 59), (23, 59)):
        P.put(x, F + 5, z, SOUL_SOIL)
        P.put(x, F + 6, z, SOUL_FIRE)
    # altar ring with rotation-proof offering markers (locked)
    P.put(ax, ay, az, fmt_state(ALTAR, {"facing": "north"}), force=True)
    for (dx, dz, face, toward) in ((3, 0, "east", "west"), (-3, 0, "west", "east"), (0, 3, "south", "north"), (0, -3, "north", "south")):
        P.put(ax + dx, ay, az + dz, fmt_state(PEDESTAL, {"facing": toward}), force=True)
        P.put(ax + dx, ay + 1, az + dz, fmt_state(MARKER, {"facing": face}), force=True)
        P.lock((ax + dx, ay, az + dz), (ax + dx, ay + 1, az + dz))
    P.lock((ax, ay, az))
    # chains: the chapel's namesake. Clusters hang from the vault, lowest link at y >= 4 over the arena
    for (dx, dz, low) in ((-8, -8, 4), (8, -8, 5), (-8, 8, 5), (8, 8, 4), (0, -9, 6), (0, 9, 6), (-9, 0, 6), (9, 0, 6),
                          (-5, -2, 7), (5, 2, 7), (2, -5, 7), (-2, 5, 7), (-11, -11, 2), (11, -11, 2), (-11, 11, 2), (11, 11, 2)):
        x, z = ax + dx, az + dz
        top = vault_y(x, z) - 1
        lit_ = (abs(dx), abs(dz)) in ((8, 8), (0, 9), (9, 0))
        for y in range(low + (1 if lit_ else 0), top + 1):
            P.put(x, y, z, CHAIN)
        if lit_:
            P.put(x, low, z, lantern(True))
    # the cage chandelier above the altar
    for (dx, dz) in ((-1, -1), (0, -1), (1, -1), (-1, 0), (1, 0), (-1, 1), (0, 1), (1, 1)):
        for y in (6, 7):
            P.put(ax + dx, y, az + dz, BARS)
    P.put(ax, 7, az, lantern(True))
    for y in range(8, vault_y(ax, az)):
        P.put(ax, y, az, CHAIN)
    for (dx, dz) in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        for y in range(8, vault_y(ax + dx, az + dz)):
            P.put(ax + dx, y, az + dz, CHAIN)
    # horizontal chains strung between the piers above the bars lines
    for z in range(AZ0, AZ1 + 1):
        if z not in piers_z:
            P.put(-1, F + 7, z, CHAIN_Z)
            P.put(23, F + 7, z, CHAIN_Z)
    for x in range(AX0, AX1 + 1):
        if x not in piers_x:
            P.put(x, F + 7, 83, CHAIN_X)
    for z in piers_z:
        for y in (F + 6, F + 7):
            if P.get(-1, y, z) in (AIR, None) or y == F + 7:
                P.put(-1, y, z, PB if y == F + 6 and (-1, F + 6, z) not in ((-1, F + 6, 63), (-1, F + 6, 79), (-1, F + 6, 59)) else P.get(-1, y, z) or PB)
    # arena clear volume
    for x in range(AX0, AX1 + 1):
        for z in range(AZ0, AZ1 + 1):
            for y in range(ay, 4):
                if (x, y, z) not in P.locked:
                    P.put(x, y, z, AIR)
    P.clear_zones.append((AX0, ay, AZ0, AX1, 3, AZ1))
    P.clear_zones.append((9, -5, 53, 13, 4, 59))
    # ruin: the south-east corner of the vault has come down into the cells
    for x in range(19, 27):
        for z in range(77, 87):
            n = vnoise(x, z, 2.5, 114)
            if n > .5 and (x >= 23 or z >= 83):
                c = vault_y(x, z)
                for y in range(c, 12):
                    if (x, y, z) in P.b:
                        P.put(x, y, z, AIR if X0 <= x <= X1 and Z0 <= z <= Z1 else None)
    # passages first: decoration never goes into their clear zones
    P.passage("chapel landing", [(x, 1, 53) for x in range(9, 14)], 4)
    P.passage("chapel stair", [(x, 2 - k, 53 + k) for x in range(9, 14) for k in range(1, 7)], 4)
    P.passage("chapel west balcony", [(x, 1, z) for x in range(X0, 9) for z in range(54, 59)
                                      if (x, z) != (2, 55) and (x != 8 or z == 54)], 4)
    P.passage("chapel east balcony", [(x, 1, z) for x in range(14, X1 + 1) for z in range(54, 59)
                                      if (x, z) != (20, 55) and (x != 14 or z == 54)], 4)
    heap(P, 24.5, 81.0, 1.2, 2.5, 2, 115, top=F + 4, bottom=F + 1)
    heap(P, 21.0, 84.6, 2.0, 0.9, 2, 116, top=F + 4, bottom=F + 1)
    web_spots(P, X0, F + 3, Z0, X1, 9, Z1, .2, 117)
    moss_patches(P, X0, Z0, X1, Z1, F + 3, F + 1, .08, 118)
    DESTINATIONS["chapel landing"] = (11, 1, 53)
    DESTINATIONS["chapel west balcony"] = (0, 1, 56)
    DESTINATIONS["chapel east balcony"] = (22, 1, 56)
    DESTINATIONS["chain chapel arena"] = (11, ay, 63)
    DESTINATIONS["chapel south"] = (11, ay, 81)
    DESTINATIONS["warden cell"] = (11, ay, 84)


def fill_rock(P):
    """Store every cell of every piece box (beard_box carves the boxes): natural rock, hewn where it faces open space."""
    def open_cell(q):
        s = P.b.get(q)
        return s is not None and not is_full_solid(s)

    for name, b in BOXES.items():
        for x in range(b[0], b[3] + 1):
            for y in range(b[1], b[4] + 1):
                for z in range(b[2], b[5] + 1):
                    if (x, y, z) in P.b:
                        continue
                    nb = ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1))
                    if any(open_cell(q) for q in nb):
                        P.b[(x, y, z)] = hewn(x, y, z)
                    else:
                        P.b[(x, y, z)] = rock(x, y, z)
    # natural rock must never face open space (water/lava springs, lichen, lush-cave moss)
    natural = {TUFF, DEEP, STONE, ANDESITE}
    for p, s in list(P.b.items()):
        if s in natural:
            x, y, z = p
            if any(open_cell(q) for q in ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1))):
                P.b[p] = hewn(x, y, z)


def entity_nbt(e, version, salt):
    rot = List[Float]([Float(e["yaw"]), Float(0.0)])
    return Compound({"id": String(e["id"]), "PersistenceRequired": Byte(1), "Rotation": rot})


def build_plan():
    CHESTS.clear()
    DESTINATIONS.clear()
    OPENINGS.clear()
    P = Plan()
    P.boxes = dict(BOXES)
    build_crossing(P)
    build_entry(P)
    build_cells_west(P)
    build_cells_east(P)
    build_gallery(P)
    build_vault(P)
    build_chapel(P)
    fill_rock(P)
    fix_connections(P)
    return P


# Biome tag: the Eclipsed Realm's land biomes. Remnant arenas generate only beneath the eclipse, so the tag
# points at the Realm's own biome tag instead of the overworld.
BIOMES = [f"#{NS}:is_eclipsed_realm"]


def worldgen_json(version):
    spawns = [
        {"type": f"{ENTITY_NS}:gravebound_soldier", "weight": 10, "minCount": 1, "maxCount": 3},
        {"type": f"{ENTITY_NS}:crypt_lurker", "weight": 6, "minCount": 1, "maxCount": 1},
        {"type": f"{ENTITY_NS}:catacomb_rat", "weight": 8, "minCount": 2, "maxCount": 4},
    ]
    structure = {
        "type": "minecraft:jigsaw",
        "biomes": f"#{NS}:has_structure/{NAME}",
        "step": "underground_structures",
        "terrain_adaptation": "beard_box",
        "spawn_overrides": {"monster": {"bounding_box": "piece", "spawns": spawns}},
        "start_pool": pool_id("start"),
        "size": JIGSAW_SIZE,
        "start_height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": -20}, "max_inclusive": {"absolute": 10}},
        "max_distance_from_center": MAX_DISTANCE,
        "use_expansion_hack": False,
    }
    if version != "1.20.1":
        structure["dimension_padding"] = 0
        structure["liquid_settings"] = "ignore_waterlogging"
    structure_set = {
        "structures": [{"structure": STRUCTURE, "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": 34, "separation": 12, "salt": SET_SALT},
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

    def rule(block, out, prob=None, out_props=None):
        return {"input_predicate": ({"predicate_type": "minecraft:random_block_match", "block": block, "probability": prob}
                                    if prob is not None else {"predicate_type": "minecraft:block_match", "block": block}),
                "location_predicate": {"predicate_type": "minecraft:always_true"},
                "output_state": {"Name": out} | ({"Properties": out_props} if out_props else {})}

    weathering = {"processors": [
        {"processor_type": "minecraft:rule", "rules": [
            rule("minecraft:deepslate_bricks", "minecraft:cracked_deepslate_bricks", 0.2),
            rule("minecraft:deepslate_tiles", "minecraft:cracked_deepslate_tiles", 0.14),
            rule("minecraft:polished_blackstone_bricks", "minecraft:cracked_polished_blackstone_bricks", 0.12),
            rule("minecraft:cobbled_deepslate", "minecraft:bone_block", 0.06, {"axis": "y"}),
            rule("minecraft:cave_air", "minecraft:cobweb", 0.5),
            rule("minecraft:moss_carpet", "minecraft:air", 0.2),
        ]},
        {"processor_type": "minecraft:protected_blocks", "value": "#minecraft:features_cannot_replace"},
    ]}

    def offering_rule(face, out, props=None):
        return {"input_predicate": {"predicate_type": "minecraft:block_match", "block": "minecraft:soul_lantern"},
                "location_predicate": {"predicate_type": "minecraft:blockstate_match",
                                       "block_state": {"Name": MARKER, "Properties": {"facing": face}}},
                "output_state": {"Name": out} | ({"Properties": props} if props else {})}

    offerings = {"processors": [{"processor_type": "minecraft:rule", "rules": [
        offering_rule("east", "minecraft:bone_block", {"axis": "y"}),
        offering_rule("west", "minecraft:soul_soil"),
        offering_rule("south", "minecraft:chain", {"axis": "y", "waterlogged": "false"}),
        offering_rule("north", "minecraft:iron_bars", {"east": "false", "north": "false", "south": "false",
                                                       "waterlogged": "false", "west": "false"}),
    ]}]}
    biome_tag = {"replace": False, "values": BIOMES}
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
        chance_pool(f"{NS}:crypt_key", 0.35),
        {"rolls": {"type": "minecraft:uniform", "min": 1, "max": 2}, "bonus_rolls": 0.0, "entries": [
            entry(f"{NS}:grave_iron", 1, 2, 5),
        ]},
        {"rolls": {"type": "minecraft:uniform", "min": 4, "max": 7}, "bonus_rolls": 0.0, "entries": [
            entry("minecraft:bone", 20, 3, 9),
            entry("minecraft:iron_ingot", 14, 1, 4),
            entry("minecraft:iron_nugget", 14, 4, 12),
            entry(f"{NS}:grave_iron", 10, 1, 3),
            entry("minecraft:chain", 8, 1, 3),
            entry("minecraft:soul_lantern", 4, 1, 2),
            entry("minecraft:iron_sword", 4, damage=(0.25, 0.7)),
            entry("minecraft:chainmail_helmet", 3, damage=(0.25, 0.7)),
            entry("minecraft:chainmail_chestplate", 2, damage=(0.25, 0.7)),
            entry("minecraft:golden_apple", 2),
            entry("minecraft:name_tag", 1),
        ]},
    ], "random_sequence": LOOT_VAULT}
    cells = {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 7}, "bonus_rolls": 0.0, "entries": [
            entry("minecraft:bone", 22, 2, 7),
            entry("minecraft:rotten_flesh", 12, 1, 4),
            entry("minecraft:iron_nugget", 12, 2, 8),
            entry(f"{NS}:grave_iron", 8, 1, 2),
            entry("minecraft:string", 8, 1, 4),
            entry("minecraft:candle", 6, 1, 3),
            entry("minecraft:chain", 6, 1, 2),
            entry("minecraft:coal", 8, 1, 4),
            entry("minecraft:arrow", 6, 2, 8),
            entry("minecraft:bread", 5, 1, 2),
            entry("minecraft:iron_ingot", 4, 1, 2),
            entry("minecraft:iron_axe", 2, damage=(0.3, 0.8)),
            entry("minecraft:chainmail_boots", 2, damage=(0.3, 0.8)),
            entry("minecraft:skeleton_skull", 1),
        ]},
    ], "random_sequence": LOOT_CELLS}
    return {f"{NAME}_vault": vault, f"{NAME}_cells": cells}


def emit(P):
    meta = {
        "boxes": BOXES, "offerings_box": OFFERINGS_BOX, "altar": ALTAR_POS, "arena": ARENA, "arena_clear": 9,
        "passages": [{"name": n, "headroom": h, "cells": c} for n, h, c in P.passages],
        "seal": [],
        "destinations": {k: list(v) for k, v in DESTINATIONS.items()},
        "behind_seal": [],
        "chests": {k: list(v) for k, v in CHESTS.items()},
        "fixed_entities": {f"{ENTITY_NS}:burial_guardian": 1},
        "guardian": {"id": f"{ENTITY_NS}:burial_guardian", "pos": list(GUARDIAN), "width": 1.6, "height": 3.3,
                     "room": [27, 1, 34, 39, 2, 48]},
        "openings": sorted(set(OPENINGS)),
        "start_height": [-20, 10],
    }
    write_json(OUT / "meta" / f"{NAME}.json", meta)
    summary = {}
    for version, info in VERSIONS.items():
        root = data_dir(version)
        clean_staged(version, NAME)
        table = block_table(version)
        ax, ay, az = ALTAR_POS
        anchor = ("chain_chapel", (ax, ay + 2, az), "down_north", jig_name("offering_anchor"), jig_name("offering_ring"),
                  pool_id("offerings"), AIR)
        tpls, dropped = cut_pieces(P, version, BOXES, CONNECTIONS, PIECE_POOL, jig_name, pool_id, anchors=[anchor],
                                   entity_nbt=entity_nbt)
        tpls["offerings"] = offerings_template(version, jig_name("offering_ring"), jig_name("offering_anchor"), lantern(False))
        for name, tpl in tpls.items():
            save_template(root / info["structures"] / NAME / f"{name}.nbt", tpl, info["data_version"], table)
            summary.setdefault(name, {"size": tpl.size, "blocks": 0, "solid": 0, "entities": len(tpl.entities)})
            summary[name]["blocks"] = len(tpl.blocks)
            summary[name]["solid"] = sum(1 for s, _ in tpl.blocks.values() if s not in (AIR, WEB_SPOT))
        structure, structure_set, pools, weathering, offerings, biome_tag = worldgen_json(version)
        write_json(root / "worldgen" / "structure" / f"{NAME}.json", structure)
        write_json(root / "worldgen" / "structure_set" / f"{NAME}.json", structure_set)
        for pool, obj in pools.items():
            write_json(root / "worldgen" / "template_pool" / NAME / f"{pool}.json", obj)
        write_json(root / "worldgen" / "processor_list" / f"{NAME}_weathering.json", weathering)
        write_json(root / "worldgen" / "processor_list" / f"{NAME}_offerings.json", offerings)
        write_json(root / "tags" / "worldgen" / "biome" / "has_structure" / f"{NAME}.json", biome_tag)
        for name, obj in loot_tables().items():
            write_json(root / info["loot"] / "chests" / f"{name}.json", obj)
        print(f"[{version}] staged {NAME} under {root.relative_to(OUT.parent)} (dropped {dropped} out-of-box plan blocks)")
    print("pieces:")
    for name, s in summary.items():
        print(f"  {name:16s} size {s['size'][0]:2d}x{s['size'][1]:2d}x{s['size'][2]:2d}  stored {s['blocks']:6d}  "
              f"non-air {s['solid']:6d}  entities {s['entities']}")
    return summary


def main():
    P = build_plan()
    emit(P)


if __name__ == "__main__":
    main()
