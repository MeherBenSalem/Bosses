#!/usr/bin/env python3
"""Validate one staged structure datapack (Hollow Citadel or Gravebound Catacombs) for 1.20.1 and 1.21.1.

Checks: template NBT (DataVersion, size <= 48, palette states against the vanilla block report,
block entities, entities), every staged JSON parses and its references resolve, per-version
worldgen schema fields, jigsaw name/target pairing, jigsaw assembly for all four start rotations
(no overlaps, all pieces placed), the altar ring and its rotation-proof offerings in world space,
arena clearance, chest loot tables, fixed entities, headroom along annotated passages and
reachability (headroom >= 3) from the entrance to every part of the structure; for the Catacombs also
the Burial Guardian's standing room. The Forgotten Observatory adds a round arena with lens platforms
(meta 'arena_round'), a directional offering (SPECS 'offering_props'), the fog-gate seal of the arena
door (meta 'fog_seal') and a no-floating-blocks check. Per-structure expectations live in SPECS.

Exit code 0 when no errors (warnings are informational).
Usage: python tools/remnants/structures/validate.py [hollow_citadel | gravebound_catacombs | forgotten_observatory]
       (default hollow_citadel)
"""
from __future__ import annotations

import argparse
import json
import math
import sys
from collections import deque
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from common import (AIR, EMPTY_POOL, ENTITY_NS, MOD_BLOCKS, NS, OUT, STRUCTURES, VERSIONS, assemble,  # noqa: E402
                    block_table, build_world, data_dir, is_passable, load_template, parse_state, piece_of,
                    resource_json, state_name, vanilla, HORIZ, OPP)

E = ENTITY_NS
SPECS = {
    "hollow_citadel": {
        "step": "surface_structures", "terrain": "beard_thin", "spawn_box": "full",
        "start_height": lambda st: (st.get("start_height") == {"absolute": 0}
                                    and st.get("project_start_to_heightmap") == "WORLD_SURFACE_WG"),
        "spawns": {f"{E}:hollow_sentinel": (10, 1, 2), f"{E}:hollow_arbalist": (6, 1, 1),
                   f"{E}:fallen_standard_bearer": (2, 1, 1), f"{E}:soulbound_knight": (1, 1, 1)},
        "biomes": [f"{NS}:tarnished_fields", f"{NS}:ashen_wastes"],  # Eclipsed Realm only
        "placement": ("minecraft:random_spread", 44, 18, 714309221),
        "offerings": {(3, 0): "minecraft:gold_block", (-3, 0): "minecraft:gold_block",
                      (0, 3): "minecraft:soul_lantern", (0, -3): "minecraft:soul_lantern"},
        "offerings_text": "gold E/W, soul lanterns N/S",
        "arena": 21, "arena_clear": 9,
        "fixed": {f"{E}:hollow_sentinel": ["gatehouse", "gatehouse"], f"{E}:soulbound_knight": ["antechamber"]},
        "start": "gate approach",
    },
    "gravebound_catacombs": {
        "step": "underground_structures", "terrain": "beard_box", "spawn_box": "piece",
        "start_height": lambda st: (st.get("start_height", {}).get("type") == "minecraft:uniform"
                                    and "project_start_to_heightmap" not in st
                                    and -20 <= st["start_height"]["min_inclusive"].get("absolute", -999)
                                    <= st["start_height"]["max_inclusive"].get("absolute", 999) <= 20),
        "spawns": {f"{E}:gravebound_soldier": (10, 1, 3), f"{E}:crypt_lurker": (6, 1, 1), f"{E}:catacomb_rat": (8, 2, 4)},
        "biomes": [f"#{NS}:is_eclipsed_realm"],  # Eclipsed Realm only
        "placement": ("minecraft:random_spread", 34, 12, None),   # salt: any int
        "offerings": {(3, 0): "minecraft:bone_block", (-3, 0): "minecraft:soul_soil",
                      (0, 3): "minecraft:chain", (0, -3): "minecraft:iron_bars"},
        "offerings_text": "bone block E, soul soil W, chain S, iron bars N",
        "arena": 23, "arena_clear": 7,            # the Warden is 4.6 tall; corner chains hang to 7 above the floor
        "fixed": {f"{E}:burial_guardian": ["burial_vault"]},
        "start": "mausoleum",
    },
    "forgotten_observatory": {
        "step": "surface_structures", "terrain": "beard_thin", "spawn_box": "piece",
        # every piece stores a 12-course foundation (box minY = plan y -12): -11 puts the floor course one block
        # above the surface at the dome centre (vanilla: start minY + 1 = first free height + start_height)
        "start_height": lambda st: (st.get("start_height") == {"absolute": -11}
                                    and st.get("project_start_to_heightmap") == "WORLD_SURFACE_WG"),
        "spawns": {f"{E}:lost_pilgrim": (10, 1, 2), f"{E}:memory_wraith": (6, 1, 1), f"{E}:soul_collector": (1, 1, 1)},
        "biomes": [f"{NS}:lantern_heights"],
        "placement": ("minecraft:random_spread", 28, 10, 902114517),
        "offerings": {(3, 0): "minecraft:amethyst_block", (-3, 0): "minecraft:calcite",
                      (0, 3): "minecraft:chiseled_sandstone", (0, -3): "minecraft:lectern"},
        "offering_props": {(0, -3): {"facing": "south", "has_book": "false", "powered": "false"}},
        "offerings_text": "amethyst block E, calcite W, chiseled sandstone S, lectern N facing the altar",
        "arena": 25, "arena_clear": 6,            # round floor radius 12; the Watcher is 3.2 tall + 0.6 hover
        "lens_radius": 9,
        "fixed": {f"{E}:soul_collector": ["memory_vault"]},
        "start": "approach foot",
        "floating": True,                         # surface ruin on a stored foundation: nothing may float
    },
}
NAME = "hollow_citadel"
STRUCTURE = f"{NS}:{NAME}"
SPEC = SPECS[NAME]


def select(name):
    global NAME, STRUCTURE, SPEC
    NAME, STRUCTURE, SPEC = name, f"{NS}:{name}", SPECS[name]

PROTECTED = {f"{NS}:ancient_altar", f"{NS}:ancient_pedestal", "minecraft:magenta_glazed_terracotta",
             "minecraft:gold_block", "minecraft:soul_lantern", "minecraft:jigsaw", "minecraft:chest"}


class Report:
    def __init__(self):
        self.errors, self.warnings, self.info = [], [], []

    def err(self, m):
        self.errors.append(m)

    def warn(self, m):
        self.warnings.append(m)

    def ok(self, m):
        self.info.append(m)


def check_state(s, table, R, where):
    try:
        name, props = parse_state(s)
    except ValueError as e:
        R.err(f"{where}: {e}")
        return
    info = table.get(name)
    if info is None:
        if table and name.startswith("minecraft:"):
            R.err(f"{where}: unknown vanilla block {name}")
        elif name.split(":")[0] not in ("minecraft",) and name not in MOD_BLOCKS:
            R.err(f"{where}: non-vanilla block {name} is not an allowed mod block")
        return
    for k, v in props.items():
        allowed = info["properties"].get(k)
        if allowed is None:
            R.err(f"{where}: {name} has no property {k}")
        elif v not in allowed:
            R.err(f"{where}: {name}[{k}={v}] not in {allowed}")
    missing = set(info["properties"]) - set(props)
    if missing:
        R.warn(f"{where}: {name} missing properties {sorted(missing)} (defaults apply)")


def check_templates(version, R, vdata, table):
    tdir = data_dir(version) / VERSIONS[version]["structures"]
    tpls = {}
    for f in sorted((tdir / NAME).rglob("*.nbt")):
        loc = f"{NS}:" + f.relative_to(tdir).with_suffix("").as_posix()
        try:
            tpl, root = load_template(f)
        except Exception as e:  # noqa: BLE001
            R.err(f"{version} {loc}: cannot load NBT ({e})")
            continue
        tpls[loc] = tpl
        dv = int(root["DataVersion"])
        if dv != VERSIONS[version]["data_version"]:
            R.err(f"{version} {loc}: DataVersion {dv} != {VERSIONS[version]['data_version']}")
        if any(c > 48 or c < 1 for c in tpl.size):
            R.err(f"{version} {loc}: size {tpl.size} exceeds 48")
        pal_seen = set()
        for p, (s, nbt) in tpl.blocks.items():
            if not all(0 <= p[i] < tpl.size[i] for i in range(3)):
                R.err(f"{version} {loc}: block {p} outside size {tpl.size}")
            if s not in pal_seen:
                pal_seen.add(s)
                check_state(s, table, R, f"{version} {loc} palette")
            n = state_name(s)
            if n in ("minecraft:jigsaw", "minecraft:chest") and nbt is None:
                R.err(f"{version} {loc}: {n} at {p} has no block entity NBT")
            if nbt is not None and "id" not in nbt:
                R.err(f"{version} {loc}: block entity at {p} without id")
            if n == "minecraft:jigsaw":
                for key in ("name", "target", "pool", "joint", "final_state"):
                    if key not in nbt:
                        R.err(f"{version} {loc}: jigsaw {p} lacks {key}")
                if version == "1.20.1" and ("selection_priority" in nbt or "placement_priority" in nbt):
                    R.warn(f"{version} {loc}: jigsaw priorities are 1.20.3+ fields (ignored by 1.20.1)")
                check_state(str(nbt.get("final_state", AIR)), table, R, f"{version} {loc} jigsaw final_state")
        for e in tpl.entities:
            nbt = e["nbt"]
            eid = str(nbt.get("id", ""))
            if not eid:
                R.err(f"{version} {loc}: entity without id")
            elif eid.startswith("minecraft:") and vdata and eid not in vdata.get("entity_types", []):
                R.err(f"{version} {loc}: unknown vanilla entity {eid}")
            if not all(0 <= e["pos"][i] < tpl.size[i] for i in range(3)):
                R.err(f"{version} {loc}: entity {eid} at {e['pos']} outside the template")
            if int(nbt.get("PersistenceRequired", 0)) != 1:
                R.err(f"{version} {loc}: entity {eid} lacks PersistenceRequired:1b")
            items = list(nbt.get("ArmorItems", [])) + list(nbt.get("HandItems", []))
            for it in items:
                if not it:
                    continue
                if version == "1.20.1" and "Count" not in it:
                    R.err(f"{version} {loc}: item stack {it.get('id')} needs Count (1.20.1 format)")
                if version != "1.20.1" and "count" not in it:
                    R.err(f"{version} {loc}: item stack {it.get('id')} needs count (1.21 format)")
                if vdata and str(it["id"]) not in vdata.get("items", []):
                    R.err(f"{version} {loc}: unknown item {it['id']}")
        # banner NBT format per version
        for p, (s, nbt) in tpl.blocks.items():
            if nbt is not None and str(nbt.get("id")) == "minecraft:banner":
                key = "Patterns" if version == "1.20.1" else "patterns"
                if key not in nbt:
                    R.err(f"{version} {loc}: banner at {p} lacks {key}")
    return tpls


def load_all_json(version, R):
    out = {}
    for f in sorted(staged_dir(version).rglob("*.json")):
        try:
            out[f] = json.loads(f.read_text(encoding="utf-8"))
        except Exception as e:  # noqa: BLE001
            R.err(f"{version} {f}: JSON parse error {e}")
    return out


def staged_dir(version):
    return OUT / version / "data"


def check_worldgen(version, R, vdata, tpls):
    jsn = lambda kind, rid: resource_json(version, kind, rid)  # noqa: E731
    sp = jsn("worldgen/structure", STRUCTURE)
    if not sp.exists():
        R.err(f"{version}: missing structure json")
        return
    st = json.loads(sp.read_text(encoding="utf-8"))
    allowed = {"type", "biomes", "step", "terrain_adaptation", "spawn_overrides", "start_pool", "start_jigsaw_name",
               "size", "start_height", "use_expansion_hack", "project_start_to_heightmap", "max_distance_from_center"}
    if version != "1.20.1":
        allowed |= {"pool_aliases", "dimension_padding", "liquid_settings"}
    for k in st:
        if k not in allowed:
            R.err(f"{version}: structure field {k!r} does not exist in {version}")
    if st.get("type") != "minecraft:jigsaw":
        R.err(f"{version}: structure type {st.get('type')}")
    if st.get("step") != SPEC["step"] or st.get("terrain_adaptation") != SPEC["terrain"]:
        R.err(f"{version}: step/terrain_adaptation mismatch")
    md = st.get("max_distance_from_center")
    if not isinstance(md, int):
        R.err(f"{version}: max_distance_from_center must be an int in {version} (object form is 1.21.2+)")
    elif not 1 <= md <= 128 or md + 12 > 128:
        R.err(f"{version}: max_distance_from_center {md} + beard 12 must be <= 128")
    size_max = 7 if version == "1.20.1" else 20
    if not 0 <= int(st.get("size", -1)) <= size_max:
        R.err(f"{version}: jigsaw size {st.get('size')} out of 0..{size_max}")
    if not SPEC["start_height"](st):
        R.err(f"{version}: start height / heightmap projection mismatch")
    if version != "1.20.1" and st.get("liquid_settings") not in (None, "apply_waterlogging", "ignore_waterlogging"):
        R.err(f"{version}: bad liquid_settings")
    so = st.get("spawn_overrides", {}).get("monster")
    if not so or so.get("bounding_box") != SPEC["spawn_box"]:
        R.err(f"{version}: monster spawn override with bounding_box {SPEC['spawn_box']} missing")
    else:
        want = SPEC["spawns"]
        got = {e["type"]: (e["weight"], e["minCount"], e["maxCount"]) for e in so["spawns"]}
        if got != want:
            R.err(f"{version}: spawn table {got} != spec {want}")
    biomes = st.get("biomes", "")
    if biomes.startswith("#"):
        tag = resource_json(version, "tags/worldgen/biome", biomes[1:])
        if not tag.exists():
            R.err(f"{version}: biome tag {biomes} missing")
        else:
            vals = json.loads(tag.read_text(encoding="utf-8"))["values"]
            if set(vals) != set(SPEC["biomes"]):
                R.err(f"{version}: biome tag values {vals} != spec")
            for b in vals:
                # mod biomes (remnant_bosses:*) are checked by tools/remnants/realm/validate_data.py
                if vdata and b.startswith("minecraft:") and b not in vdata.get("biomes", []):
                    R.err(f"{version}: unknown biome {b}")
    ss = jsn("worldgen/structure_set", STRUCTURE)
    if not ss.exists():
        R.err(f"{version}: missing structure_set")
    else:
        s = json.loads(ss.read_text(encoding="utf-8"))
        pl = s["placement"]
        want = SPEC["placement"]
        got = (pl.get("type"), pl.get("spacing"), pl.get("separation"), pl.get("salt") if want[3] is not None else None)
        if got != want or not isinstance(pl.get("salt"), int):
            R.err(f"{version}: structure_set placement {pl}")
        if [e["structure"] for e in s["structures"]] != [STRUCTURE]:
            R.err(f"{version}: structure_set does not reference {STRUCTURE}")
    # pools: element schema and references
    pool_dir = data_dir(version) / "worldgen" / "template_pool"
    pools = {}
    for f in sorted((pool_dir / NAME).rglob("*.json")):
        pid = f"{NS}:" + f.relative_to(pool_dir).with_suffix("").as_posix()
        pools[pid] = json.loads(f.read_text(encoding="utf-8"))
    if st.get("start_pool") not in pools:
        R.err(f"{version}: start pool {st.get('start_pool')} missing")
    el_keys = {"element_type", "location", "processors", "projection"} | ({"override_liquid_settings"} if version != "1.20.1" else set())
    for pid, pj in pools.items():
        if "name" in pj:
            R.warn(f"{version}: pool {pid} has a legacy 'name' field")
        if pj.get("fallback") != EMPTY_POOL:
            R.warn(f"{version}: pool {pid} fallback {pj.get('fallback')}")
        for e in pj["elements"]:
            el = e["element"]
            for k in el:
                if k not in el_keys:
                    R.err(f"{version}: pool {pid} element field {k!r} not valid in {version}")
            if el["element_type"] != "minecraft:single_pool_element":
                R.err(f"{version}: pool {pid} uses {el['element_type']}")
            if el["location"] not in tpls:
                R.err(f"{version}: pool {pid} references missing template {el['location']}")
            if el.get("projection") != "rigid":
                R.err(f"{version}: pool {pid} projection {el.get('projection')}")
            proc = el.get("processors")
            if isinstance(proc, str) and not resource_json(version, "worldgen/processor_list", proc).exists():
                R.err(f"{version}: pool {pid} references missing processor list {proc}")
    # processor lists: rules must never touch protected blocks; air rule only on upper courses
    for f in sorted((data_dir(version) / "worldgen" / "processor_list").glob(f"{NAME}_*.json")):
        pl = json.loads(f.read_text(encoding="utf-8"))
        for proc in pl["processors"]:
            if proc["processor_type"] == "minecraft:rule":
                for r in proc["rules"]:
                    blk = r["input_predicate"].get("block")
                    loc = r["location_predicate"]["predicate_type"]
                    if f.stem.endswith("weathering"):
                        if blk in PROTECTED or r["input_predicate"]["predicate_type"] == "minecraft:always_true":
                            R.err(f"{version}: weathering rule touches protected input {blk}")
                        if r["output_state"]["Name"] == "minecraft:air" and blk == "minecraft:deepslate_bricks":
                            pos = r.get("position_predicate")
                            if not pos or pos.get("axis") != "y" or int(pos.get("min_dist", 0)) < 8:
                                R.err(f"{version}: air-gap rule is not limited to upper wall courses")
                    if loc not in ("minecraft:always_true", "minecraft:blockstate_match", "minecraft:block_match"):
                        R.warn(f"{version}: {f.name} uses location predicate {loc}")
    # loot tables
    loot_dir = data_dir(version) / VERSIONS[version]["loot"]
    for f in sorted(loot_dir.rglob(f"{NAME}_*.json")):
        lt = json.loads(f.read_text(encoding="utf-8"))
        if lt.get("type") != "minecraft:chest":
            R.err(f"{version}: {f.name} type {lt.get('type')}")
        for pool in lt["pools"]:
            for e in pool["entries"]:
                name = e["name"]
                if name.startswith("minecraft:") and vdata and name not in vdata.get("items", []):
                    R.err(f"{version}: {f.name} unknown item {name}")
    return pools, st


def check_jigsaws(version, R, tpls, pools):
    named = {}
    for loc, tpl in tpls.items():
        for p, s, nbt in tpl.jigsaws():
            named.setdefault(str(nbt["name"]), []).append((loc, p, s, nbt))
    pool_locs = {pid: [e["element"]["location"] for e in pj["elements"]] for pid, pj in pools.items()}
    pairs = 0
    for loc, tpl in tpls.items():
        for p, s, nbt in tpl.jigsaws():
            pool, target, name = str(nbt["pool"]), str(nbt["target"]), str(nbt["name"])
            front = parse_state(s)[1]["orientation"].split("_")[0]
            if pool != EMPTY_POOL:
                if pool not in pools:
                    R.err(f"{version} {loc} {p}: jigsaw pool {pool} missing")
                    continue
                hits = []
                for cl in pool_locs[pool]:
                    for cp, cs, cn in tpls[cl].jigsaws():
                        cfront = parse_state(cs)[1]["orientation"].split("_")[0]
                        if str(cn["name"]) != target:
                            continue
                        if (front in HORIZ) != (cfront in HORIZ) or (front not in HORIZ and cfront != OPP[front]):
                            R.err(f"{version} {loc} {p}: {name}->{target} orientation {front} cannot meet {cfront}")
                            continue
                        hits.append((cl, cp, cn))
                if not hits:
                    R.err(f"{version} {loc} {p}: no jigsaw named {target} in pool {pool}")
                else:
                    pairs += 1
                    for cl, cp, cn in hits:
                        if str(cn["target"]) != name:
                            R.warn(f"{version}: {cl} {cp} back-target {cn['target']} != {name}")
            elif target != EMPTY_POOL:
                back = [x for x in named.get(target, []) if str(x[3]["target"]) == name]
                if not back:
                    R.err(f"{version} {loc} {p}: receiving jigsaw {name} has no parent named {target}")
    R.ok(f"{version}: {pairs} jigsaw connections pair up")


def neighbours4(p):
    x, y, z = p
    return ((x + 1, y, z), (x - 1, y, z), (x, y, z + 1), (x, y, z - 1))


def check_world(version, R, meta, tpls):
    expected = {f"{NS}:{NAME}/{n}" for n in list(meta["boxes"]) + ["offerings"]}
    worlds = {}
    for rot in range(4):
        placed = assemble(version, structure_id=STRUCTURE, start_rotation=rot)
        locs = [p.location for p in placed]
        if set(locs) != expected or len(locs) != len(expected):
            R.err(f"{version} rot{rot}: assembled {sorted(locs)}, expected {sorted(expected)}")
        for i, a in enumerate(placed):
            for j, b in enumerate(placed[:i]):
                inter = not (a.box[3] < b.box[0] or b.box[3] < a.box[0] or a.box[4] < b.box[1] or b.box[4] < a.box[1]
                             or a.box[5] < b.box[2] or b.box[5] < a.box[2])
                if inter and a.parent != j:
                    R.err(f"{version} rot{rot}: {a.location} overlaps {b.location}")
        world, owner = build_world(version, placed, weathering=False)
        worlds[rot] = (placed, world)
        if rot == 0:
            for p in placed:
                name = p.location.split("/")[-1]
                if name in meta["boxes"]:
                    b = tuple(meta["boxes"][name])
                    if p.box != b:
                        R.err(f"{version}: {name} assembled at {p.box}, plan box {b}")
        # altar ring in world space
        altars = [q for q, s in world.items() if state_name(s) == f"{NS}:ancient_altar"]
        if len(altars) != 1:
            R.err(f"{version} rot{rot}: {len(altars)} altars")
            continue
        a = altars[0]
        want = SPEC["offerings"]
        for (dx, dz), block in want.items():
            ped = world.get((a[0] + dx, a[1], a[2] + dz))
            top = world.get((a[0] + dx, a[1] + 1, a[2] + dz))
            if not ped or state_name(ped) != f"{NS}:ancient_pedestal":
                R.err(f"{version} rot{rot}: no pedestal at altar{(dx, 0, dz)}")
            if not top or state_name(top) != block:
                R.err(f"{version} rot{rot}: offering at altar{(dx, 1, dz)} is {top}, ritual needs {block}")
            elif block == "minecraft:soul_lantern" and "hanging=true" in top:
                R.err(f"{version} rot{rot}: offering lantern is hanging")
            elif SPEC.get("offering_props", {}).get((dx, dz)):
                props = parse_state(top)[1]
                for k, v in SPEC["offering_props"][(dx, dz)].items():
                    if props.get(k) != v:
                        R.err(f"{version} rot{rot}: offering at altar{(dx, 1, dz)} is {top}, wants {k}={v}")
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                for dy in range(1, 6):
                    s = world.get((a[0] + dx, a[1] + dy, a[2] + dz))
                    if s is not None and s != AIR:
                        R.err(f"{version} rot{rot}: boss spawn space blocked at altar+{(dx, dy, dz)} by {s}")
        if world.get((a[0], a[1] + 1, a[2])) is None:
            R.err(f"{version} rot{rot}: cell above altar not cleared")
        # no stray markers or jigsaws left in the world
        stray = [q for q, s in world.items() if state_name(s) in ("minecraft:magenta_glazed_terracotta", "minecraft:jigsaw")]
        if stray:
            R.err(f"{version} rot{rot}: {len(stray)} marker/jigsaw blocks left after placement, e.g. {stray[:3]}")
    R.ok(f"{version}: all 4 start rotations assemble {len(expected)} pieces; offerings resolve to {SPEC['offerings_text']}")
    return worlds


def headroom(world, p, cap=12):
    n = 0
    x, y, z = p
    while n < cap and is_passable(world.get((x, y + n, z))):
        n += 1
    return n


def supported(world, p):
    below = world.get((p[0], p[1] - 1, p[2]))
    if below is None:
        return p[1] - 1 <= 0  # outside stored blocks the ground is terrain
    n = state_name(below)
    return not is_passable(below) and not n.endswith("_fence") and not (n.endswith("_wall") and "banner" not in n) \
        and "lantern" not in n and "chain" not in n


def check_round_arena(version, R, meta, world):
    """Round arena (meta 'arena_round'): solid flat floor out to the radius, clear height everywhere and toward
    the centre, and raised lens platforms due E/W/S/N of the altar with clear air above their tops."""
    ar = meta["arena_round"]
    ax, ay, az = meta["altar"]
    x0, z0, x1, z1 = meta["arena"]
    n = SPEC["arena"]
    if (x1 - x0 + 1, z1 - z0 + 1) != (n, n) or (x0 + x1) // 2 != ax or (z0 + z1) // 2 != az:
        R.err(f"{version}: arena {meta['arena']} is not {n}x{n} centred on the altar")
    rad = ar["radius"] + 0.5
    ring = {(ax, ay, az)} | {(ax + dx, ay + dy, az + dz) for dx, dz in ((3, 0), (-3, 0), (0, 3), (0, -3)) for dy in (0, 1)}
    lens_cells = {tuple(c) for c in ar["lens_cells"]}
    centre_r, centre_need = ar["centre_clear"]
    min_clear = min_centre = 99
    cells = 0
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            d = math.hypot(x - ax, z - az)
            if d > rad:
                continue
            cells += 1
            f = world.get((x, ay - 1, z))
            if f is None or is_passable(f) or "slab" in f or "stairs" in f:
                R.err(f"{version}: arena floor not solid/flat at {(x, ay - 1, z)}: {f}")
            if (x, z) in lens_cells:
                continue
            c = 0
            for y in range(ay, ay + 40):
                q = (x, y, z)
                s = world.get(q)
                if q in ring or s is None or s == AIR:
                    c += 1
                else:
                    break
            min_clear = min(min_clear, c)
            if d <= centre_r:
                min_centre = min(min_centre, c)
    if min_clear < SPEC["arena_clear"]:
        R.err(f"{version}: round arena clearance {min_clear} < {SPEC['arena_clear']}")
    if min_centre < centre_need:
        R.err(f"{version}: arena clearance within {centre_r} of the altar {min_centre} < {centre_need}")
    want = {"east": (1, 0), "west": (-1, 0), "south": (0, 1), "north": (0, -1)}
    lr = SPEC.get("lens_radius", 9)
    lens_min = 99
    for name, (lx, ly, lz) in ar["lenses"].items():
        u = want[name]
        if (lx - ax, ly - ay, lz - az) != (u[0] * lr, 0, u[1] * lr):
            R.err(f"{version}: {name} lens at {(lx, ly, lz)} is not {lr} blocks due {name} of the altar")
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                top = world.get((lx + dx, ly, lz + dz))
                if is_passable(top) or not top:
                    R.err(f"{version}: {name} lens platform missing at {(lx + dx, ly, lz + dz)}")
                k = 1
                while k <= 40 and world.get((lx + dx, ly + k, lz + dz)) in (None, AIR):
                    k += 1
                lens_min = min(lens_min, k - 1)
                if k - 1 < ar["lens_clear"]:
                    R.err(f"{version}: {name} lens has {k - 1} clear blocks above {(lx + dx, ly, lz + dz)} "
                          f"(needs {ar['lens_clear']})")
    R.ok(f"{version}: round arena r={ar['radius']} ({cells} floor cells) flat; clear height >= {min_clear} everywhere "
         f"(spec >= {SPEC['arena_clear']}), >= {min_centre} within {centre_r} of the altar (spec >= {centre_need}); "
         f"{len(ar['lenses'])} lens platforms {lr} due E/W/S/N, >= {lens_min} clear above each (spec >= {ar['lens_clear']})")


def check_arena(version, R, meta, world):
    if meta.get("arena_round"):
        return check_round_arena(version, R, meta, world)
    ax, ay, az = meta["altar"]
    x0, z0, x1, z1 = meta["arena"]
    ring = {(ax, ay, az)} | {(ax + dx, ay + dy, az + dz) for dx, dz in ((3, 0), (-3, 0), (0, 3), (0, -3)) for dy in (0, 1)}
    n = SPEC["arena"]
    if (x1 - x0 + 1, z1 - z0 + 1) != (n, n) or (x0 + x1) // 2 != ax or (z0 + z1) // 2 != az:
        R.err(f"{version}: arena {meta['arena']} is not {n}x{n} centred on the altar")
    min_clear = 99
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            f = world.get((x, ay - 1, z))
            if f is None or is_passable(f) or "slab" in f or "stairs" in f:
                R.err(f"{version}: arena floor not solid/flat at {(x, ay - 1, z)}: {f}")
            c = 0
            for y in range(ay, ay + 40):
                q = (x, y, z)
                s = world.get(q)
                if q in ring:
                    c += 1
                    continue
                if s is None or s == AIR:
                    c += 1
                else:
                    break
            min_clear = min(min_clear, c)
    if min_clear < SPEC["arena_clear"]:
        R.err(f"{version}: arena clearance {min_clear} < {SPEC['arena_clear']}")
    R.ok(f"{version}: arena {n}x{n} flat, minimum clear height above the floor {min_clear} blocks (spec >= {SPEC['arena_clear']})")


def check_chests_entities(version, R, meta, tpls):
    loot_dir = data_dir(version) / VERSIONS[version]["loot"]
    found = {}
    for loc, tpl in tpls.items():
        for p, (s, nbt) in tpl.blocks.items():
            if state_name(s) == "minecraft:chest":
                lt = str(nbt.get("LootTable", "")) if nbt is not None else ""
                if not lt:
                    R.err(f"{version} {loc} {p}: chest without LootTable")
                    continue
                ns, path = lt.split(":", 1)
                if not (OUT / version / "data" / ns / VERSIONS[version]["loot"] / f"{path}.json").exists():
                    R.err(f"{version} {loc}: chest loot table {lt} not staged")
                found.setdefault(lt, []).append(loc.split("/")[-1])
    found = {k: sorted(v) for k, v in found.items()}
    want = {}
    for c in meta["chests"].values():
        want.setdefault(c[3], []).append(piece_of(meta["boxes"], c[:3]))
    want = {k: sorted(v) for k, v in want.items()}
    if found != want:
        R.err(f"{version}: chests {found} != {want}")
    counts = {}
    for loc, tpl in tpls.items():
        for e in tpl.entities:
            eid = str(e["nbt"]["id"])
            if eid.startswith(ENTITY_NS + ":"):
                counts.setdefault(eid, []).append(loc.split("/")[-1])
    if {k: sorted(v) for k, v in counts.items()} != SPEC["fixed"]:
        R.err(f"{version}: fixed mobs {counts} != spec {SPEC['fixed']}")
    R.ok(f"{version}: chests {dict((k.split('/')[-1], v) for k, v in found.items())}; fixed mobs {counts}")


def check_headroom(version, R, meta, world, label):
    bad = 0
    for ps in meta["passages"]:
        need = ps["headroom"]
        for c in ps["cells"]:
            c = tuple(c)
            if not supported(world, c):
                R.err(f"{version} {label}: passage '{ps['name']}' cell {c} has no floor ({world.get((c[0], c[1] - 1, c[2]))})")
                bad += 1
                continue
            h = headroom(world, c)
            if h < need:
                R.err(f"{version} {label}: passage '{ps['name']}' cell {c} headroom {h} < {need}")
                bad += 1
    mins = {}
    for ps in meta["passages"]:
        mins[ps["name"]] = min(headroom(world, tuple(c)) for c in ps["cells"])
    tight = ", ".join(f"{n} {h}" for n, h in sorted(mins.items(), key=lambda kv: kv[1])[:6])
    R.ok(f"{version} {label}: {len(meta['passages'])} annotated passages/doors/stair runs, "
         f"{sum(len(p['cells']) for p in meta['passages'])} cells, {bad} below their headroom; tightest: {tight}")


def flood(world, boxes, start, solid_extra=frozenset(), passable_extra=frozenset()):
    def inside(p):
        return any(b[0] <= p[0] <= b[3] and b[1] <= p[1] <= b[4] and b[2] <= p[2] <= b[5] for b in boxes)

    def pas(q):
        if q in passable_extra:
            return True
        if q in solid_extra:
            return False
        return is_passable(world.get(q))

    def ok(p):
        if not inside(p) or not pas(p):
            return False
        b = (p[0], p[1] - 1, p[2])
        if b in passable_extra:
            return False
        if not supported(world, p) and b not in solid_extra:
            return False
        return all(pas((p[0], p[1] + k, p[2])) for k in range(3))

    seen = {start}
    dq = deque([start])
    while dq:
        p = dq.popleft()
        for (nx, ny, nz) in neighbours4(p):
            for dy in (1, 0, -1, -2, -3):
                q = (nx, p[1] + dy, nz)
                if q in seen or not ok(q):
                    continue
                if dy == 1 and not pas((p[0], p[1] + 3, p[2])):
                    step = world.get((q[0], q[1] - 1, q[2])) or ""
                    if not ("_stairs" in step or ("_slab" in step and "type=bottom" in step)):
                        continue  # a full-block jump needs a 4th free block above the mob
                seen.add(q)
                dq.append(q)
                break
    return seen


def check_reachability(version, R, meta, world):
    boxes = [tuple(b) for b in meta["boxes"].values()]
    seal = {tuple(c) for c in meta["seal"]}
    start = tuple(meta["destinations"][SPEC["start"]])
    closed = flood(world, boxes, start, solid_extra=frozenset(seal))
    opened = flood(world, boxes, start, passable_extra=frozenset(seal))
    missing = []
    for name, d in meta["destinations"].items():
        d = tuple(d)
        sealed = name in meta.get("behind_seal", [])
        reach = closed if not sealed else opened
        if d not in reach:
            missing.append(name)
            R.err(f"{version}: '{name}' {d} not reachable from the {SPEC['start']} with >= 3 headroom"
                  + (" (seal broken)" if sealed else ""))
        if sealed and d in closed:
            R.err(f"{version}: '{name}' reachable without breaking the cracked-brick seal")
    R.ok(f"{version}: reachability flood (headroom >= 3, step <= 1): {len(closed)} walkable cells from the "
         f"{SPEC['start']}; {len(meta['destinations']) - len(missing)}/{len(meta['destinations'])} destinations reached"
         + (" (vault only after breaking the seal)" if not missing and meta["seal"] else ""))


def check_guardian(version, R, meta, world):
    """A fixed miniboss (meta 'guardian') stands free and has room to fight in; reports whether it is confined."""
    g = meta.get("guardian")
    if not g:
        return
    x, y, z = g["pos"]
    half = g["width"] / 2
    xs = range(math.floor(x - half), math.floor(x + half - 1e-6) + 1)
    zs = range(math.floor(z - half), math.floor(z + half - 1e-6) + 1)
    tall = math.ceil(g["height"] - 1e-6)
    for bx in xs:
        for bz in zs:
            c = (bx, int(y), bz)
            if not supported(world, c):
                R.err(f"{version}: {g['id']} floor under {c} is not solid ({world.get((c[0], c[1] - 1, c[2]))})")
            for k in range(tall):
                s = world.get((c[0], c[1] + k, c[2]))
                if not is_passable(s):
                    R.err(f"{version}: {g['id']} spawn space blocked at {(c[0], c[1] + k, c[2])} by {s}")
    w = len(xs)
    rx0, _, rz0, rx1, _, rz1 = g["room"]

    def fits(p):  # the w x w x tall body is clear and at least one footprint cell has a floor
        return (all(is_passable(world.get((p[0] + i, p[1] + k, p[2] + j))) for i in range(w) for j in range(w) for k in range(tall))
                and any(supported(world, (p[0] + i, p[1], p[2] + j)) for i in range(w) for j in range(w)))

    start = (xs[0], int(y), zs[0])
    seen, dq = {start}, deque([start])
    while dq:
        p = dq.popleft()
        for q0 in neighbours4(p):
            for dy in (0, 1, -1):
                q = (q0[0], p[1] + dy, q0[2])
                if q in seen or abs(q[0] - start[0]) > 48 or abs(q[2] - start[2]) > 48 or not fits(q):
                    continue
                seen.add(q)
                dq.append(q)
                break
    inside = [p for p in seen if rx0 <= p[0] and p[0] + w - 1 <= rx1 and rz0 <= p[2] and p[2] + w - 1 <= rz1]
    escaped = sorted(p for p in seen if p not in set(inside))
    if len(inside) < 40:
        R.err(f"{version}: {g['id']} can only move over {len(inside)} positions of its room")
    if escaped:
        R.warn(f"{version}: {g['id']} can leave its room ({len(escaped)} positions outside, e.g. {escaped[:3]})")
    R.ok(f"{version}: {g['id']} ({g['width']} x {g['height']}) stands free at {tuple(g['pos'])}; "
         f"{len(inside)} footprint positions in its room; {'confined to it' if not escaped else 'NOT confined'}")


def check_fog_seal(version, R, meta, world):
    """Arena doorway sealed with fog gates (meta 'fog_seal'): the planned fog cells are placed, walkers that
    cannot pass fog stay in the arena, and the arena's air (floating mobs) does not leak past its walls."""
    fs = meta.get("fog_seal")
    if not fs:
        return
    fog = {q for q, s in world.items() if state_name(s) == f"{NS}:fog_gate"}
    want = {tuple(c) for c in fs["cells"]}
    if fog != want:
        R.err(f"{version}: {len(fog)} fog gates placed, {len(want)} planned "
              f"(missing {sorted(want - fog)[:3]}, extra {sorted(fog - want)[:3]})")
    boxes = [tuple(b) for b in meta["boxes"].values()]
    start = tuple(meta["destinations"][fs["inside"]])
    walk = flood(world, boxes, start, solid_extra=frozenset(fog))
    leaked = [n for n in fs["outside"] if tuple(meta["destinations"][n]) in walk]
    if leaked:
        R.err(f"{version}: from the {fs['inside']} a mob reaches {leaked} without passing a fog gate")
    cx, cz = fs["centre"]
    y0, y1 = fs["y"]
    seen, dq, out = {start}, deque([start]), []
    while dq:
        x, y, z = dq.popleft()
        for q in ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1)):
            if q in seen or not y0 <= q[1] <= y1 or q in fog or not is_passable(world.get(q)):
                continue
            seen.add(q)
            if math.hypot(q[0] - cx, q[2] - cz) > fs["radius"]:
                out.append(q)
                continue
            dq.append(q)
    if out:
        R.err(f"{version}: arena air leaks past the walls below y={y1} at {sorted(out)[:4]} (seal incomplete)")
    R.ok(f"{version}: arena door sealed by {len(fog)} fog gates; walkers from the {fs['inside']} reach {len(walk)} cells "
         f"and none of {len(fs['outside'])} outside destinations; {len(seen)} arena air cells up to y={y1} stay within "
         f"r={fs['radius']}")


def check_floating(version, R, world, label):
    """Every placed non-air block connects (6-neighbourhood) to the lowest course: nothing floats."""
    airs = ("minecraft:air", "minecraft:cave_air", "minecraft:structure_void")
    solid = {q for q, s in world.items() if s is not None and state_name(s) not in airs}
    if not solid:
        return
    ymin = min(q[1] for q in solid)
    seen = {q for q in solid if q[1] == ymin}
    stack = list(seen)
    while stack:
        x, y, z = stack.pop()
        for q in ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1)):
            if q in solid and q not in seen:
                seen.add(q)
                stack.append(q)
    lost = sorted(solid - seen)
    if lost:
        R.err(f"{version} {label}: {len(lost)} blocks float (not connected to the foundation), e.g. {lost[:4]}")
    else:
        R.ok(f"{version} {label}: all {len(solid)} placed blocks connect to the foundation course y={ymin}")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("structure", nargs="?", default="hollow_citadel", choices=[n for n in STRUCTURES if n in SPECS])
    select(ap.parse_args().structure)
    meta = json.loads((OUT / "meta" / f"{NAME}.json").read_text(encoding="utf-8"))
    R = Report()
    for version in VERSIONS:
        vdata = vanilla(version)
        if not vdata:
            R.warn(f"{version}: vanilla reports unavailable, block states only checked syntactically")
        table = block_table(version)
        load_all_json(version, R)
        tpls = check_templates(version, R, vdata, table)
        pools, _ = check_worldgen(version, R, vdata, tpls)
        check_jigsaws(version, R, tpls, pools)
        worlds = check_world(version, R, meta, tpls)
        world0 = worlds[0][1]
        check_arena(version, R, meta, world0)
        check_chests_entities(version, R, meta, tpls)
        check_headroom(version, R, meta, world0, "unweathered")
        weathered, _ = build_world(version, worlds[0][0], weathering=True)
        check_headroom(version, R, meta, weathered, "weathered")
        check_reachability(version, R, meta, world0)
        check_guardian(version, R, meta, world0)
        check_fog_seal(version, R, meta, world0)
        if SPEC.get("floating"):
            check_floating(version, R, world0, "unweathered")
            check_floating(version, R, weathered, "weathered")
        n = len(list(staged_dir(version).rglob("*.json"))) + len(list(staged_dir(version).rglob("*.nbt")))
        R.ok(f"{version}: {len(tpls)} templates, {n} staged files parsed")
    for m in R.info:
        print("OK   ", m)
    seen = set()
    for m in R.warnings:
        key = m.split(":")[0] + m.split("missing properties")[0][-40:]
        if "missing properties" in m and key in seen:
            continue
        seen.add(key)
        print("WARN ", m)
    for m in R.errors:
        print("ERROR", m)
    print(f"\n{len(R.errors)} errors, {len(R.warnings)} warnings")
    return 1 if R.errors else 0


if __name__ == "__main__":
    sys.exit(main())
