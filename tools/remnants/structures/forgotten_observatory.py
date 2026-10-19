#!/usr/bin/env python3
"""Forgotten Observatory generator.

Seyr's Lantern Archive on a Lantern Heights ridge: a surface jigsaw structure built as one global block plan
(deterministic, fixed seed), cut into jigsaw pieces (each <= 48 on every axis) joined by floor-level jigsaws,
and staged for 1.20.1 and 1.21.1 under tools/remnants/structures/out/<version>/data/remnant_bosses/.
Nothing is written into the mod resources; install.py --only forgotten_observatory does that once the
Forgotten Souls and the items it references are registered.

Plan frame (= assembled frame at start rotation 0): x east, z south, y up. y=0 is the floor course of the
main level (mobs stand at y=1), as in the Hollow Citadel. Every piece stores a 12-course foundation under
its footprint (y=-12..-1: sandstone and calcite courses over eclipse stone bricks, eclipse stone inside),
so the building sits level on a steep ridge and never floats; every piece box therefore starts at y=-12.

    dome (start: 25x25 round arena, altar, four lens platforms, oculus)      memory_vault
          | (fog-gated door)                                                   | (Soul Collector)
    west_hall ---- lens_gallery (antechamber, fallen lens) ---- east_hall -----+
                          |
                    approach (forecourt, broken gate under a verdigris arch, stair, lantern posts)

Placement: start projected to WORLD_SURFACE_WG with start_height -11. Vanilla puts the start piece's
minY + 1 (groundLevelDelta) on first-free-height + start_height, i.e. plan y=-11 on (surface + 1) - 11:
the floor course (plan y=0) lands one block above the surface at the dome centre, so the plinth's top
course always shows. terrain_adaptation beard_thin then feeds terrain up under the foundation bottom
(each piece's ground level is its minY + 1 = plan y=-11) without carving the slopes above the floor.
Interiors, the forecourt and the stair are stored as air (terrain is cut there); everything else
outside the masonry is structure void, so uphill terrain simply abuts the walls.

Rotation-proof altar offerings (as in the Catacombs, extended for a directional offering): the ritual
checks absolute directions (amethyst block east, calcite west, chiseled sandstone south, lectern north),
but vanilla rotates jigsaw pieces. The dome puts magenta glazed terracotta direction markers on the
pedestals; a 7x1x7 offerings piece, attached by a vertical aligned jigsaw above the altar, carries a
different placeholder in each of its four ring cells (soul lantern E, lantern W, white candle S, black
candle N in its own frame). Its processor list maps (placeholder, marker facing) to the offering: the
marker says the world direction, the placeholder says the piece's rotation, so the lectern can be
turned to face the altar in every rotation (16 blockstate_match rules).

Run:  python tools/remnants/structures/forgotten_observatory.py
"""
from __future__ import annotations

import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from common import (AIR, EMPTY_POOL, ENTITY_NS, NS, OUT, VERSIONS, Plan, Template, be_nbt, block_table,  # noqa: E402
                    clean_staged, cut_pieces, data_dir, fix_connections, fmt_state, is_full_solid, make_noise,
                    mirror_x_state, pick, save_template, state_name, write_json)
from nbtlib import Byte, Compound, Float, List, String  # noqa: E402

SEED = 902114517
NAME = "forgotten_observatory"
STRUCTURE = f"{NS}:{NAME}"
WEATHERING = f"{NS}:{NAME}_weathering"
OFFERING_PROC = f"{NS}:{NAME}_offerings"
LOOT_ARCHIVE = f"{NS}:chests/{NAME}_archive"
LOOT_VAULT = f"{NS}:chests/{NAME}_vault"
SET_SALT = 902_114_517
SPACING, SEPARATION = 28, 10
START_HEIGHT = -11
JIGSAW_SIZE = 4
MAX_DISTANCE = 64

h3, vnoise = make_noise(SEED)


def pool_id(name):
    return f"{NS}:{NAME}/{name}"


def loc_id(name):
    return f"{NS}:{NAME}/{name}"


def jig_name(name):
    return f"{NS}:{NAME}/{name}"


def M(n):
    return "minecraft:" + n


def RB(n):
    return f"{NS}:{n}"


SS, CSS, SMS, CHS = M("sandstone"), M("cut_sandstone"), M("smooth_sandstone"), M("chiseled_sandstone")
CAL = M("calcite")
OXC, WXC = M("oxidized_copper"), M("weathered_copper")
OXCC, WXCC = M("oxidized_cut_copper"), M("weathered_cut_copper")
AME = M("amethyst_block")
TG = M("tinted_glass")
BOOK = M("bookshelf")
WEB = M("cobweb")
WEB_SPOT = M("cave_air")          # cobweb candidate: the weathering processor turns ~half into cobwebs
CHAIN, CHAIN_X, CHAIN_Z = M("chain[axis=y]"), M("chain[axis=x]"), M("chain[axis=z]")
ES, CES = RB("eclipse_stone"), RB("cobbled_eclipse_stone")
ESB, CESB = RB("eclipse_stone_bricks"), RB("cracked_eclipse_stone_bricks")
FOG = RB("fog_gate")
MARKER = "minecraft:magenta_glazed_terracotta"
ALTAR = RB("ancient_altar")
PEDESTAL = RB("ancient_pedestal")
OPPF = {"north": "south", "south": "north", "east": "west", "west": "east"}


def stairs(mat, facing, half="bottom", shape="straight"):
    return f"minecraft:{mat}_stairs[facing={facing},half={half},shape={shape}]"


def slab(mat, t="bottom"):
    return f"minecraft:{mat}_slab[type={t}]"


SS_WALL = M("sandstone_wall")


def lantern(hanging=False):
    return f"minecraft:soul_lantern[hanging={'true' if hanging else 'false'}]"


def candle(n=1, lit=False, color=None):
    c = f"{color}_candle" if color else "candle"
    return f"minecraft:{c}[candles={n},lit={'true' if lit else 'false'}]"


def crystal(facing="up", size="amethyst_cluster"):
    return f"minecraft:{size}[facing={facing}]"


def cbook(facing):
    """Chiseled bookshelf in its default empty state (every slot unoccupied)."""
    return f"minecraft:chiseled_bookshelf[facing={facing}]"


def lectern(facing):
    return f"minecraft:lectern[facing={facing},has_book=false,powered=false]"


def ashlar(x, y, z, band=5):
    """Wall course by height above the floor course: smooth plinth, cut sandstone ashlar, a calcite string course."""
    r = h3(x, y, z, 11)
    if y <= 1:
        return SMS if r > .12 else SS
    if y == band:
        return CAL
    return CSS if r < .62 else (SS if r < .86 else SMS)


def paving(x, z, salt=0):
    r = h3(x, 0, z, 31 + salt)
    return SMS if r < .55 else (CSS if r < .85 else SS)


def foundation(x, y, z, face):
    """Foundation course y (< 0). Faces: sandstone courses, a calcite band, eclipse stone bricks below."""
    depth = -y
    r = h3(x, y, z, 21)
    if not face:
        return SS if depth <= 2 else ES
    if depth == 1:
        return CSS if r > .1 else SS
    if depth <= 4:
        return CSS if r < .55 else (SS if r < .88 else SMS)
    if depth == 5:
        return CAL
    if depth <= 8:
        return SS if r < .45 else (CSS if r < .72 else ESB)
    return ESB if r < .55 else (CESB if r < .82 else CES)


RUBBLE = [(CSS, 30), (SS, 26), (SMS, 12), (CAL, 6), (CHS, 3)]


def ground_at(P, x, z, top, bottom):
    for y in range(top, bottom - 1, -1):
        s = P.b.get((x, y, z))
        if s is None or s in (AIR, WEB_SPOT, WEB) or state_name(s).endswith("carpet"):
            continue
        return y + 1
    return bottom


def heap(P, cx, cz, rx, rz, hmax, salt, top, bottom=1):
    """Rubble mound of fallen masonry (no gravity blocks), topped with slabs and stairs."""
    for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
        for z in range(int(cz - rz) - 1, int(cz + rz) + 2):
            d = ((x - cx) / rx) ** 2 + ((z - cz) / rz) ** 2 + (h3(x, 0, z, salt) - .5) * .5
            if d > 1:
                continue
            g = ground_at(P, x, z, top, bottom)
            below = P.b.get((x, g - 1, z))
            if below is None or not is_full_solid(below):
                continue
            h = int(round(hmax * (1 - d) + (h3(x, 1, z, salt) - .4)))
            ok = True
            for k in range(max(0, h)):
                if not P.deco(x, g + k, z, pick(RUBBLE, h3(x, g + k, z, salt + 3))):
                    ok = False
                    break
            if not ok:
                continue
            y = g + max(0, h)
            r = h3(x, 2, z, salt)
            f = ["north", "south", "east", "west"][int(h3(x, 3, z, salt) * 4)]
            if r < .2:
                P.deco(x, y, z, slab(["sandstone", "cut_sandstone", "smooth_sandstone"][int(h3(x, 4, z, salt) * 3)]))
            elif r < .32:
                P.deco(x, y, z, stairs(["sandstone", "smooth_sandstone"][int(h3(x, 4, z, salt) * 2)], f))


def web_spots(P, x0, y0, z0, x1, y1, z1, chance, salt):
    """Cobweb candidates in upper corners (cells touching a wall and the ceiling)."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            for y in range(y0, y1 + 1):
                if (x, y, z) not in P.b or not P.is_empty(x, y, z) or h3(x, y, z, salt) > chance:
                    continue
                above = P.b.get((x, y + 1, z))
                if above is None or not is_full_solid(above):
                    continue
                side = sum(1 for d in ((1, 0), (-1, 0), (0, 1), (0, -1))
                           if is_full_solid(P.b.get((x + d[0], y, z + d[1]))))
                if side >= 1:
                    P.deco(x, y, z, WEB_SPOT)


def hanging_lantern(P, x, z, y_top, y_lantern):
    """Chain from y_top down to y_lantern + 1 and a hanging soul lantern at y_lantern."""
    for y in range(y_lantern + 1, y_top + 1):
        P.put(x, y, z, CHAIN)
    P.put(x, y_lantern, z, lantern(True))


def ring_window(P, cx, cy, plane, axis, r_glass=1.6, r_frame=2.6):
    """Round 'lens' window in a wall plane: verdigris frame ring around tinted glass.

    axis 'z': the wall runs along x at z=plane (cells (cx+i, cy+j, plane)); axis 'x': runs along z at x=plane.
    """
    for i in range(-3, 4):
        for j in range(-3, 4):
            r = math.hypot(i, j)
            if r > r_frame:
                continue
            p = (cx + i, cy + j, plane) if axis == "z" else (plane, cy + j, cx + i)
            P.put(*p, TG if r <= r_glass else OXCC)


FB = -12                             # foundation bottom course; every piece box starts here
BOXES = {
    "dome":         (9, FB, 0, 37, 19, 28),
    "lens_gallery": (15, FB, 29, 31, 16, 38),
    "west_hall":    (0, FB, 29, 14, 11, 44),
    "east_hall":    (32, FB, 29, 46, 11, 44),
    "memory_vault": (38, FB, 12, 46, 10, 28),
    "approach":     (15, FB, 39, 31, 12, 50),
}
PIECE_POOL = {"dome": "start", "lens_gallery": "gallery", "west_hall": "west_hall", "east_hall": "east_hall",
              "memory_vault": "vault", "approach": "approach", "offerings": "offerings"}
# (parent, child, parent jigsaw pos, parent front, connection name): floor-course cells of the doorways
CONNECTIONS = [
    ("dome", "lens_gallery", (23, 0, 28), "south", "gallery"),
    ("lens_gallery", "west_hall", (15, 0, 34), "west", "west_hall"),
    ("lens_gallery", "east_hall", (31, 0, 34), "east", "east_hall"),
    ("lens_gallery", "approach", (23, 0, 38), "south", "approach"),
    ("east_hall", "memory_vault", (42, 0, 29), "north", "vault"),
]
CX, CZ = 23, 14                      # dome centre column
ALTAR_POS = (CX, 1, CZ)
R_ARENA = 12.5                       # round arena floor: cells with d <= 12.5 (radius 12, 25 x 25)
R_DRUM = 14.6                        # drum outer radius
OCULUS_R = 2.5
DRUM_TOP = 6
LENS_R = 9
LENSES = {"east": (CX + LENS_R, 1, CZ), "west": (CX - LENS_R, 1, CZ),
          "south": (CX, 1, CZ + LENS_R), "north": (CX, 1, CZ - LENS_R)}
LENS_CLEAR = 5                       # clear air above each lens top (Watcher: hover 0.6 + 3.2 tall)
ARENA = (CX - 12, CZ - 12, CX + 12, CZ + 12)
OFFERINGS_BOX = (CX - 3, 2, CZ - 3, CX + 3, 2, CZ + 3)
DOME_DOOR = (21, 25)                 # door x range through the drum at z 27..28
COLLECTOR = (42.0, 1.0, 21.0)        # block corner: the 1.1-wide Soul Collector stands on a 2x2 column

FOOT: set = set()                    # (x, z) columns that get the stored foundation
CHESTS: dict = {}                    # name -> (x, y, z, loot)
DESTINATIONS: dict = {}
LENS_CELLS: list = []                # (x, z) footprint of the lens platforms and their steps
FOG_CELLS: list = []


def chest(P, name, x, y, z, facing, loot):
    P.put(x, y, z, M(f"chest[facing={facing},type=single]"), {"kind": "chest", "loot": loot}, force=True)
    P.lock((x, y, z))
    CHESTS[name] = (x, y, z, loot)


def ceil_y(d):
    """First solid course above the arena floor at horizontal distance d (the dome's inner surface)."""
    return 6 + int(round(9 * math.sqrt(max(0.0, 1 - (d / 12.9) ** 2))))


def roof_y(d):
    """Top course of the dome shell (its outer surface)."""
    return 6 + int(round(11 * math.sqrt(max(0.0, 1 - (d / 14.0) ** 2))))


def on_rib(dx, dz, d, width=0.5):
    """Cell on one of the eight radial ribs (cardinal and diagonal lines through the centre)."""
    if d < 0.5:
        return False
    a = math.atan2(dz, dx)
    for k in range(8):
        t = k * math.pi / 4
        diff = a - t
        if math.cos(diff) > 0 and abs(d * math.sin(diff)) < width:
            return True
    return False


def ang_dist(a, b):
    return abs((a - b + 180) % 360 - 180)


WINDOW_ANGLES = (157.5, 202.5, 247.5, 292.5, 337.5)   # degrees, atan2(dz, dx): 0 east, 90 south
PILASTER_ANGLES = (45.0, 135.0, 225.0, 315.0)


def arena_floor(x, z):
    dx, dz = x - CX, z - CZ
    d = math.hypot(dx, dz)
    for (lx, _, lz) in LENSES.values():
        if 2.6 <= math.hypot(x - lx, z - lz) < 3.4:
            return CAL                                   # a calcite circle around each lens
    if d < 1.5:
        return CHS
    if 2.5 <= d < 3.5:
        return OXCC                                      # verdigris ring through the pedestals
    if 5.6 <= d < 6.4:
        return CAL
    if d >= 11.6:
        return OXCC                                      # verdigris border ring
    if dx == 0 or dz == 0:
        return CAL                                       # the lens axes
    if abs(dx) == abs(dz):
        return CSS
    r = h3(x, 0, z, 41)
    return SMS if r < .6 else (CSS if r < .88 else SS)


def build_dome(P):
    ax, ay, az = ALTAR_POS
    inside = lambda x, z: math.hypot(x - CX, z - CZ) <= R_ARENA            # noqa: E731
    in_dome = lambda x, z: math.hypot(x - CX, z - CZ) <= R_DRUM             # noqa: E731
    for x in range(9, 38):
        for z in range(0, 29):
            dx, dz = x - CX, z - CZ
            d = math.hypot(dx, dz)
            if d > R_DRUM:
                continue
            FOOT.add((x, z))
            ang = math.degrees(math.atan2(dz, dx)) % 360
            if d <= R_ARENA:
                P.put(x, 0, z, arena_floor(x, z))
                if d <= OCULUS_R:
                    for y in range(1, 20):
                        P.put(x, y, z, AIR)             # the oculus: open to the eclipse
                    continue
                c, o = ceil_y(d), roof_y(d)
                for y in range(1, c):
                    P.put(x, y, z, AIR)
                for y in range(c, o + 1):
                    if y == c:
                        if on_rib(dx, dz, d):
                            s = CAL
                        elif d <= 3.6:
                            s = OXCC
                        else:
                            s = CSS if h3(x, y, z, 42) < .8 else SMS
                    elif y == o:
                        if on_rib(dx, dz, d) or d <= 3.6:
                            s = OXCC
                        elif d <= 5.2:
                            s = CAL
                        else:
                            s = SMS if h3(x, y, z, 43) < .78 else CSS
                    else:
                        s = SS
                    P.put(x, y, z, s)
                if (dx == 0 or dz == 0) and 4.0 <= d <= 12.0:
                    P.put(x, o + 1, z, OXCC)              # raised verdigris ribs on the four lens axes
                elif d <= 3.6:
                    P.put(x, o + 1, z, slab("oxidized_cut_copper"))   # a low collar around the oculus
                continue
            # drum ring
            top = max(DRUM_TOP, roof_y(d))
            inner = any(inside(x + a, z + b) for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            outer = any(not in_dome(x + a, z + b) for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            P.put(x, 0, z, CSS)
            pil = inner and any(ang_dist(ang, a) * math.pi / 180 * d < 0.8 for a in PILASTER_ANGLES)
            for y in range(1, top + 1):
                if y == top and not outer:
                    s = OXCC if on_rib(dx, dz, d) else (SMS if h3(x, y, z, 44) < .78 else CSS)
                elif outer and y == DRUM_TOP:
                    s = OXCC                              # verdigris cornice ring
                elif outer and y == top and top > DRUM_TOP:
                    s = SMS
                elif pil and y <= 7:
                    s = CHS if y in (1, 6) else CAL
                elif inner or outer:
                    s = ashlar(x, y, z)
                else:
                    s = SS
                P.put(x, y, z, s)
            # tall windows (tinted glass in the outer layer, a recess behind)
            for wa in WINDOW_ANGLES:
                if ang_dist(ang, wa) * math.pi / 180 * d < 1.0:
                    for y in range(2, 6):
                        P.put(x, y, z, TG if outer else AIR)
                    P.put(x, 1, z, CSS)
                    P.put(x, 6, z, CHS if not outer else OXCC)
    # the front: a straight screen wall (z 27..28) that rises to the lens gallery's roof
    for x in range(15, 32):
        for z in (27, 28):
            if inside(x, z):
                continue
            FOOT.add((x, z))
            P.put(x, 0, z, CSS)
            for y in range(1, 15):
                P.put(x, y, z, ashlar(x, y, z, 6))
            P.put(x, 13, z, OXCC)
            P.put(x, 15, z, CSS)
    for (x0, x1, y) in ((18, 28, 16), (20, 26, 17), (22, 24, 18)):   # stepped crest over the door
        for x in range(x0, x1 + 1):
            for z in (27, 28):
                P.put(x, y, z, CSS if x in (x0, x1) else SMS)
    P.put(23, 19, 27, OXCC)
    P.put(23, 19, 28, OXCC)
    for z in (27, 28):
        for y in range(1, 16):                               # quoins at the screen ends
            for x in (15, 31):
                P.put(x, y, z, CHS if y % 4 == 1 else SMS)
    # dome door (5 wide, 5 tall) with an arch and the fog seal in the inner layer
    x0, x1 = DOME_DOOR
    for x in range(x0, x1 + 1):
        for z in (27, 28):
            P.put(x, 0, z, CSS if x in (x0, x1) else SMS)
            for y in range(1, 6):
                P.put(x, y, z, AIR)
    for z in (27, 28):
        P.put(x0, 5, z, stairs("sandstone", "west", "top"))
        P.put(x1, 5, z, stairs("sandstone", "east", "top"))
        for x in range(x0, x1 + 1):
            P.put(x, 6, z, OXCC if x == 23 else CHS)
    for y in range(1, 7):                                    # jambs on the gallery side
        P.put(x0 - 1, y, 28, CHS if y in (1, 6) else CAL)
        P.put(x1 + 1, y, 28, CHS if y in (1, 6) else CAL)
    for x in range(x0, x1 + 1):
        for y in range(1, 6):
            if P.get(x, y, 27) == AIR:
                P.put(x, y, 27, FOG, force=True)
                FOG_CELLS.append((x, y, 27))
    ring_window(P, 23, 10, 28, "z")                          # the great lens over the door (blind, set in the wall)
    # lens frames in the drum behind the east, west and north lenses
    ring_window(P, CZ, 5, CX + 13, "x")
    ring_window(P, CZ, 5, CX - 13, "x")
    ring_window(P, CX, 5, CZ - 13, "z")
    # buttresses on the diagonals
    for a in PILASTER_ANGLES:
        ux, uz = math.cos(math.radians(a)), math.sin(math.radians(a))
        cells = {}
        for i in range(0, 41):
            t = 14.0 + i * 0.12
            for j in range(-10, 11):
                o = j * 0.1
                x = int(math.floor(CX + .5 + ux * t - uz * o))
                z = int(math.floor(CZ + .5 + uz * t + ux * o))
                h = 6 if t <= 15.4 else max(1, 6 - int((t - 15.4) * 1.6))
                cells[(x, z)] = max(cells.get((x, z), 0), h)
        for (x, z), h in cells.items():
            if in_dome(x, z) or not (9 <= x <= 37 and 0 <= z <= 28):
                continue
            FOOT.add((x, z))
            P.put(x, 0, z, CSS)
            for y in range(1, h + 1):
                P.put(x, y, z, CSS if h3(x, y, z, 45) < .7 else SS)
            P.put(x, h + 1, z, slab("smooth_sandstone"))
    # altar ring with rotation-proof offering markers (locked)
    P.put(ax, ay, az, fmt_state(ALTAR, {"facing": "south"}), force=True)
    for (dx, dz, face, toward) in ((3, 0, "east", "west"), (-3, 0, "west", "east"),
                                   (0, 3, "south", "north"), (0, -3, "north", "south")):
        P.put(ax + dx, ay, az + dz, fmt_state(PEDESTAL, {"facing": toward}), force=True)
        P.put(ax + dx, ay + 1, az + dz, fmt_state(MARKER, {"facing": face}), force=True)
        P.lock((ax + dx, ay, az + dz), (ax + dx, ay + 1, az + dz))
    P.lock((ax, ay, az))
    # four raised lens platforms (1 high: amethyst centre, calcite / verdigris rim, a step on each side)
    for name, (lx, ly, lz) in LENSES.items():
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                s = AME if dx == dz == 0 else (CAL if dx and dz else OXCC)
                P.put(lx + dx, ly, lz + dz, s, force=True)
                P.lock((lx + dx, ly, lz + dz))
                LENS_CELLS.append((lx + dx, lz + dz))
        for (sx, sz, face) in ((2, 0, "west"), (-2, 0, "east"), (0, 2, "north"), (0, -2, "south")):
            for k in (-1, 0, 1):
                x = lx + sx + (k if sz else 0)
                z = lz + sz + (k if sx else 0)
                P.put(x, ly, z, stairs("smooth_sandstone", face), force=True)
                P.lock((x, ly, z))
                LENS_CELLS.append((x, z))
    # ceiling stars: amethyst set in the vault with clusters hanging (lowest at y >= 9)
    for x in range(CX - 12, CX + 13):
        for z in range(CZ - 12, CZ + 13):
            dx, dz = x - CX, z - CZ
            d = math.hypot(dx, dz)
            if not (4.5 <= d <= 11.0) or on_rib(dx, dz, d, 0.8) or h3(x, 7, z, 46) > .07:
                continue
            c = ceil_y(d)
            P.put(x, c, z, AME)
            P.put(x, c - 1, z, crystal("down", "amethyst_cluster" if h3(x, 8, z, 46) < .6 else "large_amethyst_bud"))
    # soul lanterns hanging by the pilasters, high over the arena edge
    for a in PILASTER_ANGLES:
        ux, uz = math.cos(math.radians(a)), math.sin(math.radians(a))
        x, z = int(round(CX + ux * 11.6)), int(round(CZ + uz * 11.6))
        c = ceil_y(math.hypot(x - CX, z - CZ))
        P.put(x, c - 1, z, lantern(True))
    # window sills: candles in the recesses
    for x in range(9, 38):
        for z in range(0, 29):
            s = P.b.get((x, 2, z))
            if s == AIR and not inside(x, z) and P.b.get((x, 1, z)) == CSS and h3(x, 2, z, 47) < .5:
                P.put(x, 2, z, candle(1 + int(h3(x, 3, z, 47) * 3), lit=h3(x, 4, z, 47) < .3))
    # arena clear volume (the altar ring and the lens platforms are locked)
    for x in range(CX - 12, CX + 13):
        for z in range(CZ - 12, CZ + 13):
            if not inside(x, z):
                continue
            for y in range(1, 7):
                if (x, y, z) not in P.locked:
                    P.put(x, y, z, AIR)
    P.clear_zones.append((CX - 12, 1, CZ - 12, CX + 12, 8, CZ + 12))
    P.passage("dome door", [(x, 1, z) for x in range(x0, x1 + 1) for z in (27, 28)], 4)
    DESTINATIONS["dome arena"] = (CX, 1, CZ + 6)
    DESTINATIONS["altar"] = (CX + 1, 1, CZ + 1)
    for name, (lx, ly, lz) in LENSES.items():
        DESTINATIONS[f"{name} lens"] = (lx, ly + 1, lz)


def build_gallery(P):
    X0, X1, Z0, Z1 = 15, 31, 29, 38
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            FOOT.add((x, z))
            wall = x in (X0, X1) or z == Z1
            if wall:
                P.put(x, 0, z, CSS)
                for y in range(1, 13):
                    P.put(x, y, z, ashlar(x, y, z, 6))
                P.put(x, 13, z, OXCC)
                P.put(x, 14, z, CSS)
                P.put(x, 15, z, CSS)
                if (x + z) % 2 == 0 and z != Z0 and h3(x, 16, z, 55) > .15:
                    P.put(x, 16, z, SMS)
                continue
            dx = x - 23
            dz = z - 33
            r = math.hypot(dx, dz * 1.4)
            if r < 1.2:
                f = CHS
            elif 2.6 <= r < 3.5:
                f = CAL
            elif x in (16, 30) or z == 29:
                f = CSS
            elif dx == 0:
                f = CAL
            else:
                f = paving(x, z, 2)
            P.put(x, 0, z, f)
            for y in range(1, 13):
                P.put(x, y, z, AIR)
            P.put(x, 13, z, CAL if z in (31, 34, 37) else CSS)
            P.put(x, 14, z, SMS if h3(x, 14, z, 56) < .8 else CSS)
    # engaged pilasters and corner quoins
    for (x, z) in ((X0, 31), (X0, 36), (X1, 31), (X1, 36), (X0, Z1), (X1, Z1), (19, Z1), (27, Z1)):
        for y in range(1, 13):
            P.put(x, y, z, CHS if y in (1, 12) else (CAL if (x, z) not in ((X0, Z1), (X1, Z1)) else SMS))
    # south facade: door, two tall windows and the rose lens above the door
    for x in range(21, 26):
        P.put(x, 0, Z1, SMS if x != 23 else CAL)
        for y in range(1, 6):
            P.put(x, y, Z1, AIR)
        P.put(x, 6, Z1, CHS if x != 23 else OXCC)
    P.put(21, 5, Z1, stairs("sandstone", "west", "top"))
    P.put(25, 5, Z1, stairs("sandstone", "east", "top"))
    for wx in (17, 29):
        for y in range(2, 11):
            P.put(wx, y, Z1, TG)
        P.put(wx, 1, Z1, CHS)
        P.put(wx, 11, Z1, OXCC)
    ring_window(P, 23, 9, Z1, "z")
    # side doors into the reading halls, clerestory windows above the hall roofs
    for wx in (X0, X1):
        for z in (33, 34, 35):
            P.put(wx, 0, z, SMS)
            for y in range(1, 5):
                P.put(wx, y, z, AIR)
        P.put(wx, 4, 33, stairs("sandstone", "north", "top"))
        P.put(wx, 4, 35, stairs("sandstone", "south", "top"))
        for z in (32, 35):
            for y in (10, 11, 12):
                P.put(wx, y, z, TG)
    # the fallen lens: a broken ring of verdigris and tinted glass across the north-east corner
    for x in range(25, 31):
        for z in range(29, 33):
            e = ((x - 27.8) / 2.7) ** 2 + ((z - 30.6) / 1.9) ** 2
            if e > 1:
                continue
            rr = h3(x, 1, z, 51)
            if e > .5:
                P.put(x, 1, z, OXCC if z <= 30 else slab("oxidized_cut_copper"))
                if z == 29 and rr < .7:
                    P.put(x, 2, z, slab("oxidized_cut_copper"))    # the edge that came down against the wall
            elif rr < .62:
                P.put(x, 1, z, TG)
            elif rr < .8:
                P.put(x, 1, z, crystal("up", "medium_amethyst_bud"))
    P.put(29, 1, 36, TG)                                       # shards that skidded away (clear of the aisles)
    P.put(27, 1, 37, crystal("up", "small_amethyst_bud"))
    for y in range(8, 13):                                     # its snapped chain
        P.put(28, y, 31, CHAIN)
    P.put(28, 11, 29, stairs("oxidized_cut_copper", "north", "top"))   # the empty mount on the screen wall
    P.put(27, 11, 29, stairs("oxidized_cut_copper", "north", "top"))
    # a reading lectern and candles in the north-west corner
    P.put(17, 1, 30, lectern("east"))
    P.put(16, 1, 29, candle(3, lit=True))
    P.put(16, 1, 31, candle(2))
    for (x, z) in ((19, 30), (18, 31)):
        P.put(x, 1, z, candle(1 + (x % 2)))
    # lens stands in front of the tall windows: a calcite plinth, a post and a tinted lens in a verdigris cradle
    for (x, z) in ((18, 37), (28, 37)):
        P.put(x, 1, z, CHS)
        P.put(x, 2, z, SS_WALL)
        P.put(x, 3, z, OXCC)
        P.put(x, 4, z, TG)
        P.put(x, 5, z, slab("oxidized_cut_copper"))
    # hanging lanterns along the nave
    hanging_lantern(P, 18, 33, 12, 9)
    hanging_lantern(P, 28, 34, 12, 9)
    hanging_lantern(P, 23, 30, 12, 10)
    P.passage("gallery door", [(x, 1, Z1) for x in range(21, 26)], 4)
    P.passage("gallery nave", [(x, 1, z) for x in range(21, 26) for z in range(29, 38)], 5)
    P.passage("gallery cross aisle", [(x, 1, z) for x in range(16, 31) for z in (33, 34, 35)], 3)
    P.passage("gallery west door", [(x, 1, z) for x in (14, 15) for z in (33, 34, 35)], 3)
    P.passage("gallery east door", [(x, 1, z) for x in (31, 32) for z in (33, 34, 35)], 3)
    for (x, z) in ((16, 37), (30, 37), (20, 37), (26, 36)):
        P.deco(x, 1, z, candle(1 + int(h3(x, 1, z, 52) * 3), lit=h3(x, 2, z, 52) < .3))
    heap(P, 17.0, 36.6, 1.4, 0.8, 1, 53, top=4)
    web_spots(P, 16, 9, 29, 30, 12, 37, .3, 54)
    DESTINATIONS["lens gallery"] = (23, 1, 33)
    DESTINATIONS["fallen lens"] = (26, 1, 33)


def build_hall(P, east):
    """Reading hall in the west wing's local frame; the east wing is its mirror (x -> 46 - x) plus the vault door."""
    X = (lambda x: 46 - x) if east else (lambda x: x)

    def S(s):
        return mirror_x_state(s) if east and s is not None else s

    def put(x, y, z, s, be=None, force=False):
        P.put(X(x), y, z, S(s), be, force=force)

    def deco(x, y, z, s):
        return P.deco(X(x), y, z, S(s))

    def get(x, y, z):
        return P.get(X(x), y, z)

    side = "east" if east else "west"
    n_windows = (8, 12) if east else (4, 8)
    for x in range(0, 15):
        for z in range(29, 45):
            FOOT.add((X(x), z))
            wx = X(x)
            if x == 0 or z == 44:
                put(x, 0, z, paving(wx, z, 5) if h3(wx, 0, z, 61) < .8 else CSS)
                continue
            wall = x in (1, 14) or z in (29, 43)
            if wall:
                put(x, 0, z, CSS)
                for y in range(1, 8):
                    put(x, y, z, ashlar(wx, y, z))
                put(x, 8, z, OXCC)
                put(x, 9, z, CSS)
                if (x + z) % 2 == 0 and h3(wx, 10, z, 60) > .14:
                    put(x, 10, z, SMS)
                continue
            put(x, 0, z, CAL if z == 34 and x >= 3 else (CSS if x == 2 or z in (30, 42) else paving(wx, z, 6)))
            for y in range(1, 7):
                put(x, y, z, AIR)
            put(x, 7, z, CAL if x in (4, 8, 12) else CSS)
            put(x, 8, z, SMS)
    # pilasters on the outer faces
    for z in (30, 34, 38, 42):
        for y in range(1, 8):
            put(1, y, z, CHS if y in (1, 7) else SMS)
    for x in (2, 6, 10, 14):
        for y in range(1, 8):
            put(x, y, 43, CHS if y in (1, 7) else SMS)
    # door from the lens gallery
    for z in (33, 34, 35):
        put(14, 0, z, SMS)
        for y in range(1, 5):
            put(14, y, z, AIR)
    put(14, 4, 33, stairs("sandstone", "north", "top"))
    put(14, 4, 35, stairs("sandstone", "south", "top"))
    # windows
    for z in (32, 36, 40):
        for y in range(2, 6):
            put(1, y, z, TG)
    for x in (4, 8, 12):
        for y in range(2, 6):
            put(x, y, 43, TG)
    for x in n_windows:
        for y in range(2, 6):
            put(x, y, 29, TG)
    # vault door (east wing only): 1 wide so the Soul Collector (1.1 wide) stays in its vault
    if east:
        for y in range(1, 4):
            put(4, y, 29, AIR)
        put(4, 0, 29, CHS)
    door_cols = {4} if east else set()
    # perimeter shelves (3 high; 1 high under windows), chiseled bookshelves facing the room
    shelves = []
    for z in range(30, 43):
        if z in (33, 34, 35):
            continue
        shelves.append((2, z, "east", 1 if z in (32, 36, 40) else 3))
    for x in range(3, 14):
        if x in door_cols or (east and x in (3, 5)):
            continue
        shelves.append((x, 30, "south", 1 if x in n_windows else 3))
    for x in range(3, 14):
        if x in (11, 12):
            continue                                   # the archive chest corner
        shelves.append((x, 42, "north", 1 if x in (4, 8, 12) else 3))
    for z in list(range(31, 33)) + list(range(37, 42)):
        shelves.append((13, z, "west", 3))
    for (x, z, face, h) in shelves:
        for y in range(1, h + 1):
            wx = X(x)
            r = h3(wx, y, z, 62)
            if y == 2 and r < .3:
                s = cbook(face)
            elif r < .06:
                s = AIR if y == h and h > 1 else BOOK   # a missing shelf on top
            else:
                s = BOOK
            put(x, y, z, s)
        if h == 1 and h3(X(x), 2, z, 63) < .5:
            put(x, 2, z, candle(1 + int(h3(X(x), 3, z, 63) * 3), lit=h3(X(x), 4, z, 63) < .25))
    # the reading lectern at the end of the aisle, between two low shelves with candles
    put(2, 1, 34, lectern("east"))
    for z in (33, 35):
        put(2, 1, z, BOOK)
        put(2, 2, z, candle(2, lit=True))
    # reading desks along the north side of the aisle (top slabs) with sandstone-stair chairs
    desk_x = range(6, 12) if east else range(4, 12)
    for x in desk_x:
        put(x, 1, 32, slab("smooth_sandstone", "top"))
        if x % 2 == 1:
            put(x, 1, 31, stairs("sandstone", "north"))
    for x in desk_x:
        r = h3(X(x), 2, 32, 64)
        if r < .3:
            put(x, 2, 32, candle(1 + int(r * 10) % 3, lit=r < .12))
    put(desk_x[0] - 1, 1, 32, lectern("south"))
    # freestanding double shelf rows in the south half, chiseled ends
    for z in (37, 40):
        for x in range(4, 11):
            for y in (1, 2):
                r = h3(X(x), y, z, 65)
                if x == 4:
                    put(x, y, z, cbook("west"))
                elif x == 10:
                    put(x, y, z, cbook("east"))
                else:
                    put(x, y, z, cbook("north" if z == 37 and r < .3 else "south") if r < .3 else BOOK)
            if h3(X(x), 3, z, 66) < .25:
                put(x, 3, z, candle(1 + int(h3(X(x), 4, z, 66) * 3)))
    # a toppled shelf (row end fallen across the walkway) and a rubble pile from the vault ceiling
    if not east:
        put(10, 2, 40, AIR)
        put(10, 1, 41, BOOK)
        put(9, 1, 41, BOOK)
        put(8, 2, 40, AIR)
    # ruin: the outer corner's parapet has fallen onto the apron below
    corner = ((1, 43), (2, 43), (3, 43), (1, 42), (1, 41)) if not east else ((1, 43), (2, 43), (1, 42))
    for (x, z) in corner:
        put(x, 10, z, None)
        if (x, z) != (1, 41):
            put(x, 9, z, None)
    heap(P, X(0) + (0.4 if east else -0.4), 43.4, 0.9, 1.6, 1, 69 + east, top=3)
    heap(P, X(1.6), 44.2, 1.4, 0.7, 1, 71 + east, top=3)
    # the archive chest
    chest(P, f"{side} archive", X(12), 1, 41, "north", LOOT_ARCHIVE)
    put(11, 1, 41, candle(3))
    put(11, 1, 42, BOOK)
    put(11, 2, 42, BOOK)
    put(12, 1, 42, BOOK)
    put(12, 2, 42, candle(2))
    # hanging lanterns over the aisle
    for x in (5, 10):
        P.put(X(x), 6, 34, CHAIN)
        P.put(X(x), 5, 34, lantern(True))
    # passages first, decoration after
    P.passage(f"{side} hall aisle", [(X(x), 1, z) for x in range(3, 14) for z in (33, 34, 35)], 3)
    P.passage(f"{side} hall rows", [(X(x), 1, z) for x in range(3, 13) for z in (36, 38, 39)], 3)
    P.passage(f"{side} hall east walk", [(X(x), 1, z) for x in (11, 12) for z in range(36, 41)], 3)
    if east:
        P.passage("vault path", [(X(4), 1, z) for z in range(29, 33)], 3)
    web_spots(P, min(X(2), X(13)), 4, 30, max(X(2), X(13)), 6, 42, .35, 68 + east)
    DESTINATIONS[f"{side} reading hall"] = (X(6), 1, 34)
    DESTINATIONS[f"{side} archive chest"] = (X(12), 1, 40)
    DESTINATIONS[f"{side} shelf rows"] = (X(7), 1, 38)
    DESTINATIONS[f"{side} desks"] = (X(7), 1, 33)


def build_vault(P):
    X0, X1, Z0, Z1 = 39, 44, 14, 27            # interior
    for x in range(38, 47):
        for z in range(12, 29):
            FOOT.add((x, z))
            if x == 46 or z == 12:
                P.put(x, 0, z, paving(x, z, 7))
                continue
            wall = x in (38, 45) or z in (13, 28)
            if wall:
                # the north and east walls face the outside: dressed sandstone with a few crystal veins breaking
                # through; the west wall (against the dome) and the south wall (against the hall) are crust
                hidden = (x == 38 or z == 28) and not (x == 45 or z == 13)
                P.put(x, 0, z, CSS)
                for y in range(1, 7):
                    r = h3(x, y, z, 71)
                    if y == 1:
                        s = SMS
                    elif y == 6:
                        s = CAL if hidden else OXCC
                    elif hidden:
                        s = AME if r < .45 else (CAL if r < .7 else CSS)
                    else:
                        s = AME if r < .06 else ashlar(x, y, z)
                    P.put(x, y, z, s)
                P.put(x, 7, z, CSS)
                if (x + z) % 2 == 0 and not hidden:
                    P.put(x, 8, z, SMS)
                continue
            r = h3(x, 0, z, 72)
            P.put(x, 0, z, AME if r < .18 else (CAL if r < .7 else SMS))
            for y in range(1, 6):
                P.put(x, y, z, AIR)
            P.put(x, 6, z, AME if h3(x, 6, z, 73) < .35 else CAL)
            P.put(x, 7, z, SMS)
    # crystal crust lining the upper walls (y 4..5, above the Collector's head room) on the outward-facing walls
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            if x == X1 or z == Z0:
                for y in (4, 5):
                    r = h3(x, y, z, 79)
                    if r < .75 and (x, z) not in ((42, Z0), (41, Z0), (43, Z0)):
                        P.put(x, y, z, AME if r < .55 else CAL)
    # crystal crust: clusters grow from the amethyst in the walls and ceiling (above the Collector's head room)
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            for (dx, dz, face) in ((-1, 0, "east"), (1, 0, "west"), (0, -1, "south"), (0, 1, "north")):
                for y in (4, 5):
                    if P.get(x + dx, y, z + dz) == AME and P.get(x, y, z) == AIR and h3(x, y, z, 74) < .55:
                        P.put(x, y, z, crystal(face, "amethyst_cluster" if h3(x, y, z, 75) < .5 else "large_amethyst_bud"))
            if P.get(x, 6, z) == AME and P.get(x, 5, z) == AIR and h3(x, 5, z, 76) < .5:
                P.put(x, 5, z, crystal("down", "amethyst_cluster" if h3(x, 5, z, 77) < .5 else "medium_amethyst_bud"))
    # door from the east hall (1 wide, 3 tall)
    for y in range(1, 4):
        P.put(42, y, 28, AIR)
    P.put(42, 0, 28, CHS)
    P.put(42, 4, 28, CHS)
    # the vault chest at the north end between two crystal plinths
    chest(P, "memory vault", 42, 1, 14, "south", LOOT_VAULT)
    for x in (40, 44):
        P.put(x, 1, 14, CAL)
        P.put(x, 2, 14, crystal("up"))
    P.put(41, 1, 14, candle(3, lit=True, color="purple"))
    P.put(43, 1, 14, candle(2, lit=True, color="purple"))
    for x in range(41, 44):
        P.put(x, 0, 15, CHS if x == 42 else CAL)
    # a memory well in the floor: an amethyst eye in a calcite ring
    for x in range(40, 45):
        for z in range(19, 24):
            d = math.hypot(x - 42, z - 21)
            if d < .5:
                P.put(x, 0, z, AME)
            elif d < 1.5:
                P.put(x, 0, z, CAL)
            elif d < 2.3:
                P.put(x, 0, z, OXCC)
    # soul lanterns
    for (x, z) in ((40, 18), (43, 24)):
        P.put(x, 5, z, CHAIN)
        P.put(x, 4, z, lantern(True))
    P.passage("vault door", [(42, 1, 28), (42, 1, 29)], 3)
    P.passage("vault floor", [(x, 1, z) for x in range(40, 44) for z in range(16, 27)], 3)
    for (x, z) in ((39, 26), (44, 25), (39, 16)):
        P.deco(x, 1, z, crystal("up", "small_amethyst_bud"))
    web_spots(P, X0, 4, Z0, X1, 5, Z1, .2, 78)
    P.entity("mob", COLLECTOR, 180.0, id=f"{ENTITY_NS}:soul_collector")
    DESTINATIONS["memory vault"] = (42, 1, 26)
    DESTINATIONS["vault chest"] = (42, 1, 15)


def build_approach(P):
    # forecourt between the wings
    for x in range(15, 32):
        for z in range(39, 45):
            FOOT.add((x, z))
            dx = x - 23
            f = CAL if dx == 0 else (CSS if abs(dx) == 2 or z == 39 else paving(x, z, 8))
            P.put(x, 0, z, f)
            for y in range(1, 13):
                P.put(x, y, z, AIR)
    # balustrade along the south edge of the forecourt
    for x in list(range(15, 19)) + list(range(28, 32)):
        P.put(x, 1, 44, SS_WALL)
    # the broken sandstone gate: two piers (the east one cracked open) under a verdigris arch
    for (px, broken) in ((19, False), (26, True)):
        for x in (px, px + 1):
            for z in (43, 44):
                for y in range(1, 8):
                    if broken and x == px + 1 and z == 44 and y >= 4:
                        continue
                    P.put(x, y, z, CHS if y in (1, 7) else (CSS if h3(x, y, z, 81) < .75 else SS))
    P.put(27, 3, 44, stairs("sandstone", "south", "top"))
    # the verdigris arch springs from the pier tops (y 7) and rises to a keystone at y 11
    arch = {19: [(8, "slab")], 20: [(8, "full")], 21: [(8, "stair_w"), (9, "full")], 22: [(9, "stair_w"), (10, "full")],
            23: [(10, "full"), (11, "full")], 24: [(9, "stair_e"), (10, "full")], 25: [(8, "stair_e"), (9, "full")],
            26: [(8, "full")], 27: [(8, "slab")]}
    for x, parts in arch.items():
        for z in (43, 44):
            for (y, kind) in parts:
                if kind == "slab":
                    s = slab("oxidized_cut_copper")
                elif kind == "full":
                    s = OXCC
                else:
                    s = stairs("oxidized_cut_copper", "west" if kind == "stair_w" else "east", "top")
                P.put(x, y, z, s)
    P.put(23, 12, 43, crystal("up"))
    P.put(23, 12, 44, crystal("up", "large_amethyst_bud"))
    P.put(23, 9, 43, lantern(True))                      # a lantern under the keystone
    # the stair down the slope (5 steps) with stepped cheek walls
    for k in range(5):
        z = 45 + k
        ty = -k
        for x in range(20, 27):
            FOOT.add((x, z))
            if x in (20, 26):
                for y in range(ty, ty + 2):
                    P.put(x, y, z, CSS if h3(x, y, z, 82) < .8 else SS)
                for y in range(ty + 2, 13):
                    P.put(x, y, z, AIR)
            else:
                P.put(x, ty, z, stairs("smooth_sandstone", "north"))
                for y in range(ty + 1, 13):
                    P.put(x, y, z, AIR)
    # the foot of the stair and its two lantern posts
    for x in range(18, 29):
        FOOT.add((x, 50))
        P.put(x, -5, 50, CSS if x in (18, 28) else paving(x, 50, 9))
        for y in range(-4, 13):
            P.put(x, y, 50, AIR)
    for px in (19, 27):
        P.put(px, -4, 50, CHS)
        P.put(px, -3, 50, SS_WALL)
        P.put(px, -2, 50, SS_WALL)
        P.put(px, -1, 50, OXCC)
        P.put(px, 0, 50, lantern(False))
    # fallen pieces of the broken pier
    P.put(28, 1, 42, CHS)
    P.put(29, 1, 42, stairs("sandstone", "east"))
    P.passage("forecourt", [(x, 1, z) for x in range(16, 31) for z in range(39, 42)], 3)
    P.passage("approach gate", [(x, 1, z) for x in range(21, 26) for z in (42, 43, 44)], 4)
    P.passage("approach stair", [(x, 1 - k, 45 + k) for x in range(21, 26) for k in range(5)], 3)
    P.passage("approach foot", [(x, -4, 50) for x in range(20, 27)], 3)
    heap(P, 29.6, 41.0, 1.2, 1.0, 1, 83, top=3)
    DESTINATIONS["approach foot"] = (23, -4, 50)
    DESTINATIONS["forecourt"] = (17, 1, 40)


def fill_foundation(P):
    """Store the foundation under every footprint column (y=-12..-1); faces are dressed masonry."""
    for (x, z) in FOOT:
        face = any((x + a, z + b) not in FOOT for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        for y in range(FB, 0):
            if (x, y, z) in P.b:
                continue
            P.b[(x, y, z)] = foundation(x, y, z, face)


def floating(P):
    """Non-air plan blocks not connected (6-neighbourhood) to the foundation bottom: should be none."""
    solid = {p for p, s in P.b.items() if state_name(s) not in (AIR, WEB_SPOT)}
    seen = {p for p in solid if p[1] == FB}
    stack = list(seen)
    while stack:
        x, y, z = stack.pop()
        for q in ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1)):
            if q in solid and q not in seen:
                seen.add(q)
                stack.append(q)
    return sorted(solid - seen)


def entity_nbt(e, version, salt):
    rot = List[Float]([Float(e["yaw"]), Float(0.0)])
    return Compound({"id": String(e["id"]), "PersistenceRequired": Byte(1), "Rotation": rot})


def build_plan():
    for coll in (FOOT, CHESTS, DESTINATIONS):
        coll.clear()
    LENS_CELLS.clear()
    FOG_CELLS.clear()
    P = Plan()
    P.boxes = dict(BOXES)
    build_dome(P)
    build_gallery(P)
    build_hall(P, east=False)
    build_hall(P, east=True)
    build_vault(P)
    build_approach(P)
    fill_foundation(P)
    fix_connections(P)
    return P


# Biome tag: the Lantern Heights only (the biome is added to the Realm by realm_data.py).
BIOMES = [f"{NS}:lantern_heights"]

# offerings: placeholder per ring cell of the offerings piece (its own frame) and offering per world direction
PLACEHOLDERS = {"east": "minecraft:soul_lantern[hanging=false]", "west": "minecraft:lantern[hanging=false]",
                "south": "minecraft:white_candle[candles=1,lit=false]", "north": "minecraft:black_candle[candles=1,lit=false]"}
OFFERINGS = {"east": ("minecraft:amethyst_block", None), "west": ("minecraft:calcite", None),
             "south": ("minecraft:chiseled_sandstone", None), "north": ("minecraft:lectern", "lectern")}


def offerings_template(version):
    """7x1x7 piece: an up-facing jigsaw in the centre and a distinct placeholder in each ring cell."""
    tpl = Template(size=(7, 1, 7))
    jig = {"kind": "jigsaw", "name": jig_name("offering_ring"), "target": jig_name("offering_anchor"),
           "pool": EMPTY_POOL, "joint": "aligned", "final_state": AIR}
    tpl.blocks[(3, 0, 3)] = ("minecraft:jigsaw[orientation=up_north]", be_nbt(jig, version))
    for d, p in (("east", (6, 0, 3)), ("west", (0, 0, 3)), ("south", (3, 0, 6)), ("north", (3, 0, 0))):
        tpl.blocks[p] = (PLACEHOLDERS[d], None)
    return tpl


def worldgen_json(version):
    spawns = [
        {"type": f"{ENTITY_NS}:lost_pilgrim", "weight": 10, "minCount": 1, "maxCount": 2},
        {"type": f"{ENTITY_NS}:memory_wraith", "weight": 6, "minCount": 1, "maxCount": 1},
        {"type": f"{ENTITY_NS}:soul_collector", "weight": 1, "minCount": 1, "maxCount": 1},
    ]
    structure = {
        "type": "minecraft:jigsaw",
        "biomes": f"#{NS}:has_structure/{NAME}",
        "step": "surface_structures",
        "terrain_adaptation": "beard_thin",
        "spawn_overrides": {"monster": {"bounding_box": "piece", "spawns": spawns}},
        "start_pool": pool_id("start"),
        "size": JIGSAW_SIZE,
        "start_height": {"absolute": START_HEIGHT},
        "project_start_to_heightmap": "WORLD_SURFACE_WG",
        "max_distance_from_center": MAX_DISTANCE,
        "use_expansion_hack": False,
    }
    if version != "1.20.1":
        structure["dimension_padding"] = 0
        structure["liquid_settings"] = "ignore_waterlogging"
    structure_set = {
        "structures": [{"structure": STRUCTURE, "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": SPACING, "separation": SEPARATION, "salt": SET_SALT},
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

    def rule(block, out, prob, out_props=None):
        return {"input_predicate": {"predicate_type": "minecraft:random_block_match", "block": block, "probability": prob},
                "location_predicate": {"predicate_type": "minecraft:always_true"},
                "output_state": {"Name": out} | ({"Properties": out_props} if out_props else {})}

    weathering = {"processors": [
        {"processor_type": "minecraft:rule", "rules": [
            rule("minecraft:cut_sandstone", "minecraft:sandstone", 0.12),
            rule("minecraft:smooth_sandstone", "minecraft:sandstone", 0.05),
            rule("minecraft:oxidized_cut_copper", "minecraft:weathered_cut_copper", 0.14),
            rule(f"{NS}:eclipse_stone_bricks", f"{NS}:cracked_eclipse_stone_bricks", 0.2),
            rule("minecraft:cave_air", "minecraft:cobweb", 0.5),
            rule("minecraft:candle", "minecraft:air", 0.2),
        ]},
        {"processor_type": "minecraft:protected_blocks", "value": "#minecraft:features_cannot_replace"},
    ]}
    rules = []
    for p, ph in PLACEHOLDERS.items():
        for w, (out, kind) in OFFERINGS.items():
            props = None
            if kind == "lectern":
                # the piece is rotated after processing: facing OPP(p) in the piece frame ends up facing the altar
                props = {"facing": OPPF[p], "has_book": "false", "powered": "false"}
            rules.append({"input_predicate": {"predicate_type": "minecraft:block_match", "block": state_name(ph)},
                          "location_predicate": {"predicate_type": "minecraft:blockstate_match",
                                                 "block_state": {"Name": MARKER, "Properties": {"facing": w}}},
                          "output_state": {"Name": out} | ({"Properties": props} if props else {})})
    offerings = {"processors": [{"processor_type": "minecraft:rule", "rules": rules}]}
    biome_tag = {"replace": False, "values": BIOMES}
    return structure, structure_set, pool_json, weathering, offerings, biome_tag


def loot_tables():
    def entry(name, weight, lo=1, hi=1):
        e = {"type": "minecraft:item", "name": name, "weight": weight}
        if hi > 1 or lo != 1:
            e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
        return e

    def chance_pool(name, chance):
        return {"rolls": 1, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": name}],
                "conditions": [{"condition": "minecraft:random_chance", "chance": chance}]}

    vault = {"type": "minecraft:chest", "pools": [
        chance_pool(f"{NS}:memory_lens", 0.25),
        chance_pool(f"{NS}:knowledge_fragment", 0.08),
        {"rolls": 1, "bonus_rolls": 0.0, "entries": [entry(f"{NS}:memory_shard", 1, 2, 5)]},
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "bonus_rolls": 0.0, "entries": [
            entry("minecraft:amethyst_shard", 22, 2, 7),
            entry("minecraft:gold_ingot", 14, 1, 4),
            entry("minecraft:gold_nugget", 14, 3, 10),
            entry(f"{NS}:memory_shard", 8, 1, 2),
            entry("minecraft:experience_bottle", 6, 1, 3),
            entry("minecraft:candle", 6, 1, 3),
            entry("minecraft:diamond", 3, 1, 2),
            entry("minecraft:spyglass", 2),
            entry("minecraft:echo_shard", 2),
        ]},
    ], "random_sequence": LOOT_VAULT}
    archive = {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 7}, "bonus_rolls": 0.0, "entries": [
            entry("minecraft:paper", 22, 2, 9),
            entry("minecraft:book", 14, 1, 3),
            entry("minecraft:candle", 12, 1, 3),
            entry("minecraft:amethyst_shard", 12, 1, 4),
            entry(f"{NS}:memory_shard", 10, 1, 3),
            entry("minecraft:glass_bottle", 8, 1, 3),
            entry("minecraft:lapis_lazuli", 8, 2, 6),
            entry("minecraft:ink_sac", 6, 1, 3),
            entry("minecraft:feather", 6, 1, 3),
            entry("minecraft:experience_bottle", 2, 1, 2),
            entry("minecraft:spyglass", 1),
        ]},
    ], "random_sequence": LOOT_ARCHIVE}
    return {f"{NAME}_vault": vault, f"{NAME}_archive": archive}


# The validator and the previewer assemble the jigsaw with the start piece's box minimum at the origin, so the
# meta is written in that assembled frame: assembled = plan - FRAME (the dome box's minimum corner).
FRAME = BOXES["dome"][:3]


def A(p):
    return [p[0] - FRAME[0], p[1] - FRAME[1], p[2] - FRAME[2]]


def A_box(b):
    return A(b[:3]) + A(b[3:])


def A_xz(c):
    return [c[0] - FRAME[0], c[1] - FRAME[2]]


def emit(P):
    lost = floating(P)
    if lost:
        print(f"WARNING: {len(lost)} plan blocks are not connected to the foundation, e.g. {lost[:8]}")
    meta = {
        "frame_offset": list(FRAME),
        "frame_note": "assembled frame (start box minimum at the origin) = plan frame - frame_offset; "
                      "plan y=0 is the floor course, assembled y=12",
        "boxes": {k: A_box(v) for k, v in BOXES.items()}, "offerings_box": A_box(OFFERINGS_BOX),
        "altar": A(ALTAR_POS), "arena": A_xz(ARENA[:2]) + A_xz(ARENA[2:]), "arena_clear": 6,
        "arena_round": {"radius": 12, "lenses": {k: A(v) for k, v in LENSES.items()}, "lens_clear": LENS_CLEAR,
                        "lens_cells": sorted(A_xz(c) for c in set(LENS_CELLS)), "centre_clear": [6.0, 10]},
        "passages": [{"name": n, "headroom": h, "cells": [A(c) for c in cs]} for n, h, cs in P.passages],
        "seal": [],
        "fog_seal": {"cells": sorted(A(c) for c in FOG_CELLS), "inside": "dome arena", "centre": A_xz((CX, CZ)),
                     "radius": R_DRUM, "y": [1 - FRAME[1], 9 - FRAME[1]],
                     "outside": ["lens gallery", "forecourt", "approach foot", "west reading hall",
                                 "east reading hall", "memory vault"]},
        "destinations": {k: A(v) for k, v in DESTINATIONS.items()},
        "behind_seal": [],
        "chests": {k: A(v[:3]) + [v[3]] for k, v in CHESTS.items()},
        "fixed_entities": {f"{ENTITY_NS}:soul_collector": 1},
        "guardian": {"id": f"{ENTITY_NS}:soul_collector", "pos": [float(c) for c in A(COLLECTOR)], "width": 1.1,
                     "height": 2.9, "room": A_box((39, 1, 14, 44, 2, 27))},
        "start_height": START_HEIGHT,
        "foundation_bottom": A((0, FB, 0))[1],
    }
    write_json(OUT / "meta" / f"{NAME}.json", meta)
    summary = {}
    for version, info in VERSIONS.items():
        root = data_dir(version)
        clean_staged(version, NAME)
        table = block_table(version)
        ax, ay, az = ALTAR_POS
        anchor = ("dome", (ax, ay + 2, az), "down_north", jig_name("offering_anchor"), jig_name("offering_ring"),
                  pool_id("offerings"), AIR)
        tpls, dropped = cut_pieces(P, version, BOXES, CONNECTIONS, PIECE_POOL, jig_name, pool_id, anchors=[anchor],
                                   entity_nbt=entity_nbt)
        tpls["offerings"] = offerings_template(version)
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
        print(f"  {name:13s} size {s['size'][0]:2d}x{s['size'][1]:2d}x{s['size'][2]:2d}  stored {s['blocks']:6d}  "
              f"non-air {s['solid']:6d}  entities {s['entities']}")
    return summary


def main():
    P = build_plan()
    emit(P)


if __name__ == "__main__":
    main()
