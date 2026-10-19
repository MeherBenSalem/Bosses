#!/usr/bin/env python3
"""Static validation of the Eclipsed Realm datapack in both mod resource trees (1.20.1, 1.21.1).

The game cannot boot until the realm blocks, items, particle and Remnant entity types are
registered in Java, so this checks everything that can be checked offline against the vanilla
data of each version (data generator dump cached by tools/remnants/structures/realm_data.py):

  * every JSON file under both data trees parses
  * dimension type fields and the per-version int provider format
  * dimension: generator, settings and the multi-noise parameter list, which must tile the whole
    climate space [-2, 2]^6 exactly once (proved on the grid of all box edges, then spot-checked
    with vanilla's nearest-box fitness on random points); biome shares per climate band
  * noise settings: everything except default_block / surface_rule equals the version's vanilla
    overworld settings (router, aquifers, ore veins); bedrock floor and deepslate rules are vanilla;
    every rule/condition type, block state, biome and noise referenced exists
  * biomes: schema, colours, sounds, particles, carvers, spawners against the spec table,
    spawn_costs, every feature reference, and vanilla's feature order rule (no cycles)
  * configured / placed features: every type against the version's registries, int provider format
    per version, block states and properties, block / feature references
  * tags (block, item, biome): referenced IDs exist, replace false, the vanilla tag exists
  * loot tables and recipes for the realm blocks: per-version formats, referenced items
  * structures: grace_shrine (structure, set, pool, processors, biome tag, template) and the
    realm/arrival_shrine template layout; staged copies equal the installed ones
  * what cannot be verified offline: mod registrations, reported from the Java init sources

Exit code 0 when there are no errors.
Usage: python tools/remnants/realm/validate_data.py
"""
from __future__ import annotations

import itertools
import json
import random
import re
import sys
from collections import defaultdict
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / "structures"))
import realm_data as rd  # noqa: E402
from common import (AIR, MOD_BLOCKS, NS, OUT, REPO, VERSIONS, load_template, parse_state, state_name)  # noqa: E402


class Report:
    def __init__(self):
        self.errors, self.warnings, self.info = [], [], []

    def err(self, m):
        self.errors.append(m)

    def warn(self, m):
        self.warnings.append(m)

    def ok(self, m):
        self.info.append(m)


R = Report()


class Ctx:
    def __init__(self, version):
        self.v = version
        self.vw = rd.vanilla_worldgen(version)
        self.data = rd.res_data(version)
        self.ns = self.data / NS
        regs = self.vw["registries"]
        self.reg = {k: set(v) for k, v in regs.items()}
        self.blocks = dict(self.vw["blocks"])
        self.blocks.update(MOD_BLOCKS)
        self.items = self.reg["minecraft:item"]
        self.wg = {k: set(v) for k, v in self.vw["worldgen"].items()}
        self.tags = {k: set(v) for k, v in self.vw["tags"].items()}

    def mod_ids(self, kind):
        d = self.ns / "worldgen" / kind
        return {f"{NS}:" + p.relative_to(d).with_suffix("").as_posix() for p in d.rglob("*.json")} if d.exists() else set()

    def where(self, path):
        try:
            return f"{self.v} {path.relative_to(self.data).as_posix()}"
        except ValueError:
            return f"{self.v} {path}"


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def check_state(c: Ctx, state: dict, where: str):
    name = state.get("Name")
    info = c.blocks.get(name)
    if info is None:
        R.err(f"{where}: unknown block {name}")
        return
    for k, v in state.get("Properties", {}).items():
        allowed = info["properties"].get(k)
        if allowed is None:
            R.err(f"{where}: {name} has no property {k}")
        elif v not in allowed:
            R.err(f"{where}: {name}[{k}={v}] not in {allowed}")


def check_block_id(c: Ctx, bid: str, where: str):
    if bid.startswith("#"):
        return
    if bid not in c.blocks:
        R.err(f"{where}: unknown block {bid}")


TYPE_REGISTRIES = [
    "minecraft:worldgen/feature", "minecraft:worldgen/placement_modifier_type", "minecraft:block_predicate_type",
    "minecraft:worldgen/block_state_provider_type", "minecraft:worldgen/trunk_placer_type",
    "minecraft:worldgen/foliage_placer_type", "minecraft:worldgen/feature_size_type", "minecraft:int_provider_type",
    "minecraft:height_provider_type", "minecraft:float_provider_type", "minecraft:worldgen/tree_decorator_type",
    "minecraft:worldgen/root_placer_type",
]


def walk_feature_json(c: Ctx, obj, where: str, stats):
    """Recursive checks on configured/placed feature JSON."""
    if isinstance(obj, list):
        for x in obj:
            walk_feature_json(c, x, where, stats)
        return
    if not isinstance(obj, dict):
        return
    t = obj.get("type")
    if t == "minecraft:spring_feature":  # its "state" is a fluid state (water/lava with "falling")
        cfg = dict(obj["config"])
        fluid = cfg.pop("state")
        if fluid.get("Name") not in c.reg["minecraft:fluid"]:
            R.err(f"{where}: unknown fluid {fluid.get('Name')}")
        for b in cfg.get("valid_blocks", []):
            check_block_id(c, b, where)
        walk_feature_json(c, cfg, where, stats)
        stats["types"].add(t)
        return
    if isinstance(t, str) and ":" in t:
        if not any(t in c.reg.get(r, ()) for r in TYPE_REGISTRIES):
            R.err(f"{where}: type {t} is not registered in {c.v}")
        stats["types"].add(t)
        if t == "minecraft:uniform" and ("value" in obj or isinstance(obj.get("min_inclusive"), int)):
            nested = "value" in obj
            if rd.is120(c.v) and not nested:
                R.err(f"{where}: uniform int provider must nest min/max under 'value' in 1.20.1")
            if not rd.is120(c.v) and nested:
                R.err(f"{where}: uniform int provider must be flat in {c.v}")
    if "Name" in obj and isinstance(obj["Name"], str):
        check_state(c, obj, where)
        stats["states"] += 1
    if obj.get("type") in ("minecraft:matching_blocks",):
        bl = obj.get("blocks")
        for b in (bl if isinstance(bl, list) else [bl]):
            check_block_id(c, b, where)
    # placed feature shape: {"feature": X, "placement": [...]}
    if "feature" in obj and "placement" in obj:
        f = obj["feature"]
        if isinstance(f, str):
            if f not in c.wg["configured_feature"] and f not in c.mod_ids("configured_feature"):
                R.err(f"{where}: configured feature {f} does not exist")
            stats["refs"].add(f)
    # random_selector default/features that are placed-feature references
    for key in ("default",):
        if isinstance(obj.get(key), str):
            ref = obj[key]
            if ref not in c.wg["placed_feature"] and ref not in c.mod_ids("placed_feature"):
                R.err(f"{where}: placed feature {ref} does not exist")
    for v in obj.values():
        walk_feature_json(c, v, where, stats)


def check_json_tree(c: Ctx):
    n = bad = 0
    for f in sorted(c.data.rglob("*.json")):
        n += 1
        try:
            load(f)
        except Exception as e:  # noqa: BLE001
            bad += 1
            R.err(f"{c.where(f)}: JSON parse error {e}")
    R.ok(f"{c.v}: {n} JSON files under data/ parse ({bad} errors)")


def check_dimension_type(c: Ctx):
    p = c.ns / "dimension_type" / "eclipsed_realm.json"
    if not p.exists():
        R.err(f"{c.v}: dimension_type/eclipsed_realm.json missing")
        return
    got = load(p)
    want = rd.dimension_type(c.v)
    if got != want:
        R.err(f"{c.v}: dimension type differs from the spec: {got}")
    vanilla_keys = set(c.vw["dimension_type_overworld"])
    if set(got) - {"fixed_time"} != vanilla_keys:
        R.err(f"{c.v}: dimension type keys {sorted(set(got) ^ (vanilla_keys | {'fixed_time'}))} differ from vanilla")
    light = got["monster_spawn_light_level"]
    vl = c.vw["dimension_type_overworld"]["monster_spawn_light_level"]
    if set(light) != set(vl):
        R.err(f"{c.v}: monster_spawn_light_level format {light} != vanilla format {vl}")
    checks = dict(fixed_time=12000, effects=rd.DIMENSION, bed_works=False, respawn_anchor_works=False, natural=False,
                  min_y=-64, height=384, logical_height=384, monster_spawn_block_light_limit=0,
                  infiniburn="#minecraft:infiniburn_overworld", has_raids=False, coordinate_scale=1.0, ambient_light=0.0,
                  has_skylight=True, has_ceiling=False)
    for k, v in checks.items():
        if got.get(k) != v:
            R.err(f"{c.v}: dimension type {k}={got.get(k)} != {v}")
    if "minecraft:infiniburn_overworld" not in c.tags.get(rd.VERSIONS[c.v]["block_tags"], set()):
        R.err(f"{c.v}: vanilla block tag infiniburn_overworld missing")
    R.ok(f"{c.v}: dimension type matches realm.md section 3 (fixed_time 12000, light 0-7 in the {c.v} provider format)")


AXES = ["temperature", "humidity", "continentalness", "erosion", "weirdness", "depth"]


def _iv(p):
    return (float(p), float(p)) if isinstance(p, (int, float)) else (float(p[0]), float(p[1]))


def check_dimension(c: Ctx):
    p = c.ns / "dimension" / "eclipsed_realm.json"
    if not p.exists():
        R.err(f"{c.v}: dimension/eclipsed_realm.json missing")
        return []
    d = load(p)
    if d.get("type") != rd.DIMENSION:
        R.err(f"{c.v}: dimension type ref {d.get('type')}")
    g = d.get("generator", {})
    if g.get("type") != "minecraft:noise" or g.get("settings") != rd.DIMENSION:
        R.err(f"{c.v}: generator {g.get('type')} / settings {g.get('settings')}")
    if not (c.ns / "worldgen" / "noise_settings" / "eclipsed_realm.json").exists():
        R.err(f"{c.v}: noise settings file missing")
    bs = g.get("biome_source", {})
    if bs.get("type") != "minecraft:multi_noise" or "biomes" not in bs:
        R.err(f"{c.v}: biome source must be minecraft:multi_noise with an explicit 'biomes' list")
        return []
    entries = bs["biomes"]
    boxes = []
    for i, e in enumerate(entries):
        b = e["biome"]
        if not (c.ns / "worldgen" / "biome" / f"{b.split(':')[1]}.json").exists():
            R.err(f"{c.v}: parameter entry {i} uses missing biome {b}")
        par = e["parameters"]
        if set(par) != set(AXES) | {"offset"}:
            R.err(f"{c.v}: entry {i} parameters {sorted(par)}")
            continue
        if float(par["offset"]) != 0.0:
            R.err(f"{c.v}: entry {i} offset {par['offset']} (expected 0)")
        box = []
        for a in AXES:
            lo, hi = _iv(par[a])
            if not -2.0 <= lo <= hi <= 2.0:
                R.err(f"{c.v}: entry {i} {a} {par[a]} outside the codec range [-2, 2] or inverted")
            box.append((lo, hi))
        boxes.append((b, box))
    used = {b for b, _ in boxes}
    if used != set(rd.ALL_BIOMES):
        R.err(f"{c.v}: biome source biomes {sorted(used)} != the {len(rd.ALL_BIOMES)} realm biomes")
    # exact tiling proof on the grid of all edges
    edges = [sorted({v for _, box in boxes for v in box[k]} | {-2.0, 2.0}) for k in range(len(AXES))]
    cells = [list(zip(e[:-1], e[1:])) for e in edges]
    holes = overlaps = 0
    first_hole = first_overlap = None
    n_cells = 0
    for cell in itertools.product(*cells):
        n_cells += 1
        hits = [b for b, box in boxes if all(box[k][0] <= cell[k][0] and cell[k][1] <= box[k][1] for k in range(len(AXES)))]
        if not hits:
            holes += 1
            first_hole = first_hole or cell
        elif len(hits) > 1:
            overlaps += 1
            first_overlap = first_overlap or (cell, hits)
    if holes:
        R.err(f"{c.v}: climate parameter list has {holes} uncovered grid cells, e.g. {first_hole}")
    if overlaps:
        R.err(f"{c.v}: climate parameter list has {overlaps} doubly covered grid cells, e.g. {first_overlap}")
    # nearest-box spot check with vanilla's fitness (sum of squared distances outside each interval)
    rng = random.Random(7)
    mism = 0
    for _ in range(4000):
        pt = [rng.uniform(-2.0, 2.0) for _ in AXES]

        def fit(box):
            s = 0.0
            for k, (lo, hi) in enumerate(box):
                d = lo - pt[k] if pt[k] < lo else (pt[k] - hi if pt[k] > hi else 0.0)
                s += d * d
            return s
        best = min(boxes, key=lambda bb: fit(bb[1]))
        if fit(best[1]) != 0.0:
            mism += 1
    if mism:
        R.err(f"{c.v}: {mism} random climate points have no containing box")
    if not holes and not overlaps and not mism:
        R.ok(f"{c.v}: multi-noise list: {len(entries)} entries tile [-2,2]^6 exactly ({n_cells} grid cells, no holes or "
             f"overlaps; 4000 random points all inside a box)")
    return boxes


def climate_shares(boxes):
    """Share of each surface biome at depth 0 over the 5x5 vanilla temperature/humidity bands, on land,
    splitting weirdness halves evenly; erosion bands weighted as vanilla's seven (the two mountain bands below
    -0.375 about 2/7, the flattest band >= 0.55 about 1/7). Points sit inland (continentalness 0.5)."""
    share = defaultdict(float)
    tb, hb = rd.T_BANDS, rd.H_BANDS
    for ti, (t0, t1) in enumerate(tb):
        for hi, (h0, h1) in enumerate(hb):
            for w, wweight in (((-1.0, -0.5), 0.5), ((0.5, 1.0), 0.5)):
                for e, eweight in ((-0.6, 2 / 7), (0.0, 4 / 7), (0.8, 1 / 7)):
                    pt = [(t0 + t1) / 2, (h0 + h1) / 2, 0.5, e, sum(w) / 2, 0.0]
                    for b, box in boxes:
                        if all(box[k][0] <= pt[k] <= box[k][1] for k in range(6)):
                            share[b.split(":")[1]] += wweight * eweight / 25
                            break
    return dict(sorted(share.items(), key=lambda kv: -kv[1]))


def walk_surface(c: Ctx, r, where, stats):
    t = r.get("type")
    if t not in c.reg["minecraft:worldgen/material_rule"]:
        R.err(f"{where}: unknown surface rule type {t}")
    if t == "minecraft:sequence":
        for s in r["sequence"]:
            walk_surface(c, s, where, stats)
    elif t == "minecraft:condition":
        walk_condition(c, r["if_true"], where, stats)
        walk_surface(c, r["then_run"], where, stats)
    elif t == "minecraft:block":
        check_state(c, r["result_state"], where)
        stats["blocks"].add(r["result_state"]["Name"])


def walk_condition(c: Ctx, cond, where, stats):
    t = cond.get("type")
    if t not in c.reg["minecraft:worldgen/material_condition"]:
        R.err(f"{where}: unknown surface condition type {t}")
    if t == "minecraft:biome":
        for b in cond["biome_is"]:
            stats["biomes"].add(b)
            if b.startswith(f"{NS}:") and not (c.ns / "worldgen" / "biome" / f"{b.split(':')[1]}.json").exists():
                R.err(f"{where}: surface rule biome {b} missing")
            elif b.startswith("minecraft:"):
                R.warn(f"{where}: surface rule references vanilla biome {b}")
    elif t == "minecraft:noise_threshold":
        if cond["noise"] not in c.wg["noise"]:
            R.err(f"{where}: unknown noise {cond['noise']}")
        stats["noises"].add(cond["noise"])
    elif t == "minecraft:not":
        walk_condition(c, cond["invert"], where, stats)


def check_noise_settings(c: Ctx):
    p = c.ns / "worldgen" / "noise_settings" / "eclipsed_realm.json"
    if not p.exists():
        return
    ns = load(p)
    van = c.vw["noise_settings_overworld"]
    for k in van:
        if k in ("default_block", "surface_rule"):
            continue
        if ns.get(k) != van[k]:
            R.err(f"{c.v}: noise settings {k} differs from the vanilla {c.v} overworld")
    if set(ns) != set(van):
        R.err(f"{c.v}: noise settings keys differ from vanilla")
    if ns.get("default_block") != {"Name": rd.E}:
        R.err(f"{c.v}: default_block {ns.get('default_block')}")
    seq = ns["surface_rule"]["sequence"]
    if seq[0] != van["surface_rule"]["sequence"][0] or seq[-1] != van["surface_rule"]["sequence"][-1]:
        R.err(f"{c.v}: bedrock floor / deepslate rules are not the vanilla ones")
    stats = {"blocks": set(), "biomes": set(), "noises": set()}
    walk_surface(c, ns["surface_rule"], f"{c.v} noise_settings surface_rule", stats)
    for b in (rd.SEA, rd.WASTES, rd.BARROWS, rd.HEIGHTS):
        if b not in stats["biomes"]:
            R.err(f"{c.v}: surface rule has no branch for {b}")
    for blk in ("minecraft:grass_block", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:gravel", "minecraft:podzol",
                "minecraft:rooted_dirt", "minecraft:sand", rd.E, "minecraft:bedrock", "minecraft:deepslate"):
        if blk not in stats["blocks"]:
            R.err(f"{c.v}: surface rule never places {blk}")
    R.ok(f"{c.v}: noise settings = vanilla {c.v} overworld (router, aquifers {ns['aquifers_enabled']}, ore veins "
         f"{ns['ore_veins_enabled']}, sea level {ns['sea_level']}) + eclipse stone default + surface rule placing "
         f"{len(stats['blocks'])} block types with noises {sorted(stats['noises'])}")


EXPECTED_SPAWNS = {  # (category, type) -> (weight, min, max)
    rd.FIELDS: {("monster", "remnants:hollow_sentinel"): (30, 1, 3), ("monster", "remnants:hollow_arbalist"): (18, 1, 2),
                ("monster", "remnants:fallen_standard_bearer"): (4, 1, 1), ("monster", "remnants:soulbound_knight"): (2, 1, 1),
                ("monster", "remnants:catacomb_rat"): (8, 2, 4), ("creature", "minecraft:rabbit"): (4, None, None),
                ("creature", "minecraft:sheep"): (6, None, None)},
    rd.GRACEWOOD: {("monster", "remnants:catacomb_rat"): (12, 2, 4), ("monster", "remnants:crypt_lurker"): (6, 1, 1),
                   ("monster", "remnants:hollow_sentinel"): (6, 1, 2), ("monster", "remnants:lost_pilgrim"): (10, 1, 2),
                   ("creature", "minecraft:sheep"): (8, None, None),
                   ("creature", "minecraft:rabbit"): (6, None, None), ("creature", "minecraft:fox"): (3, None, None),
                   ("ambient", "minecraft:bat"): (4, None, None)},
    rd.BARROWS: {("monster", "remnants:gravebound_soldier"): (30, 2, 3), ("monster", "remnants:crypt_lurker"): (20, 1, 2),
                 ("monster", "remnants:catacomb_rat"): (20, 3, 5), ("monster", "remnants:hollow_arbalist"): (6, 1, 1)},
    rd.WASTES: {("monster", "remnants:hollow_sentinel"): (20, 1, 2), ("monster", "remnants:gravebound_soldier"): (15, 1, 3),
                ("monster", "remnants:soulbound_knight"): (8, 1, 1), ("monster", "remnants:crypt_lurker"): (10, 1, 1)},
    rd.HEIGHTS: {("monster", "remnants:lost_pilgrim"): (30, 1, 3), ("monster", "remnants:memory_wraith"): (12, 1, 2),
                 ("monster", "remnants:soul_collector"): (2, 1, 1), ("creature", "minecraft:rabbit"): (4, None, None)},
    rd.SEA: {("water_creature", "minecraft:squid"): (2, None, None), ("water_ambient", "minecraft:cod"): (10, None, None)},
    rd.OSSUARY: {("monster", "remnants:crypt_lurker"): (25, 1, 2), ("monster", "remnants:catacomb_rat"): (25, 3, 5),
                 ("monster", "remnants:gravebound_soldier"): (10, 1, 2), ("ambient", "minecraft:bat"): (10, None, None)},
}
COLOR_KEYS = {"fog_color", "water_color", "water_fog_color", "sky_color", "grass_color", "foliage_color"}
EFFECT_KEYS = COLOR_KEYS | {"mood_sound", "music", "particle", "ambient_sound", "additions_sound", "grass_color_modifier"}
MOD_PARTICLES = {rd.GRACE_MOTE}


def check_biomes(c: Ctx):
    pf_mod = c.mod_ids("placed_feature")
    flat_lists = {}
    remnant_entities = set()
    for b in rd.ALL_BIOMES:
        p = c.ns / "worldgen" / "biome" / f"{b.split(':')[1]}.json"
        if not p.exists():
            R.err(f"{c.v}: biome {b} missing")
            continue
        j = load(p)
        w = c.where(p)
        for k in ("has_precipitation", "temperature", "downfall", "effects", "spawners", "spawn_costs", "carvers", "features"):
            if k not in j:
                R.err(f"{w}: missing {k}")
        if j.get("has_precipitation") is not False:
            R.err(f"{w}: has_precipitation must be false")
        if j.get("temperature", 0) < 0.15:
            R.warn(f"{w}: temperature < 0.15 lets water freeze")
        eff = j["effects"]
        for k in eff:
            if k not in EFFECT_KEYS:
                R.err(f"{w}: unknown effects key {k}")
        for k in COLOR_KEYS:
            if not isinstance(eff.get(k), int) or not 0 <= eff[k] <= 0xFFFFFF:
                R.err(f"{w}: effects.{k} missing or not an RGB int")
        sounds = [eff.get("ambient_sound"), (eff.get("mood_sound") or {}).get("sound"),
                  (eff.get("additions_sound") or {}).get("sound"), (eff.get("music") or {}).get("sound")]
        for s in filter(None, sounds):
            if s not in c.reg["minecraft:sound_event"]:
                R.err(f"{w}: unknown sound event {s}")
        if "particle" in eff:
            pt = eff["particle"]["options"]["type"]
            if pt not in c.reg["minecraft:particle_type"] and pt not in MOD_PARTICLES:
                R.err(f"{w}: unknown particle {pt}")
        for step, cv in j["carvers"].items():
            if step not in ("air", "liquid"):
                R.err(f"{w}: carver step {step}")
            for cr in cv:
                if cr not in c.wg["configured_carver"]:
                    R.err(f"{w}: unknown carver {cr}")
        cats = set(rd.SPAWN_CATEGORIES)
        got = {}
        for cat, lst in j["spawners"].items():
            if cat not in cats:
                R.err(f"{w}: unknown spawn category {cat}")
            for e in lst:
                t = e["type"]
                if t.startswith("minecraft:") and t not in c.reg["minecraft:entity_type"]:
                    R.err(f"{w}: unknown entity {t}")
                if t.startswith("remnants:"):
                    remnant_entities.add(t)
                if not (1 <= e["minCount"] <= e["maxCount"]) or e["weight"] < 1:
                    R.err(f"{w}: bad spawner {e}")
                got[(cat, t)] = (e["weight"], e["minCount"], e["maxCount"])
        want = EXPECTED_SPAWNS[b]
        if set(got) != set(want):
            R.err(f"{w}: spawner set {sorted(got)} != spec {sorted(want)}")
        for k, (wt, lo, hi) in want.items():
            g = got.get(k)
            if g and (g[0] != wt or (lo is not None and (g[1], g[2]) != (lo, hi))):
                R.err(f"{w}: spawner {k} {g} != spec {(wt, lo, hi)}")
        spawned = {t for (_, t) in got}
        for t, cost in j["spawn_costs"].items():
            if t not in spawned:
                R.err(f"{w}: spawn cost for {t} which never spawns here")
            if set(cost) != {"energy_budget", "charge"}:
                R.err(f"{w}: spawn cost {t} {cost}")
        for t in spawned:
            if t in rd.LEGION + rd.CRYPTBORN + rd.SOULS and t not in j["spawn_costs"]:
                R.err(f"{w}: {t} has no spawn cost (patrols must stay sparse)")
        feats = j["features"]
        if len(feats) != 11:
            R.err(f"{w}: features has {len(feats)} steps (11 expected)")
        for si, lst in enumerate(feats):
            for f in lst:
                if f not in c.wg["placed_feature"] and f not in pf_mod:
                    R.err(f"{w}: step {rd.STEPS[si]} feature {f} does not exist in {c.v}")
            if len(set(lst)) != len(lst):
                R.err(f"{w}: duplicate feature in step {rd.STEPS[si]}")
        flat_lists[b] = [(f, si) for si, lst in enumerate(feats) for f in lst]
    # vanilla FeatureSorter: nodes (feature, step), edges between consecutive entries of each biome
    graph = defaultdict(set)
    for lst in flat_lists.values():
        for a, bnode in zip(lst, lst[1:]):
            graph[a].add(bnode)
    state = {}

    def dfs(n):
        state[n] = 1
        for m in graph[n]:
            if state.get(m) == 1:
                return True
            if m not in state and dfs(m):
                return True
        state[n] = 2
        return False
    nodes = {n for lst in flat_lists.values() for n in lst}
    cyc = any(dfs(n) for n in sorted(nodes) if n not in state)
    if cyc:
        R.err(f"{c.v}: feature order cycle across the realm biomes (vanilla would refuse to load the dimension)")
    # every placed feature file is used by some biome
    used = {f for lst in flat_lists.values() for f, _ in lst}
    for f in sorted(pf_mod):
        if f not in used:
            R.warn(f"{c.v}: placed feature {f} is not used by any realm biome")
    # rendered fog after the fixed-dusk multiplier (about 0.86 at day time 12000)
    fogs = []
    for b in rd.SURFACE_LAND:
        fc = rd.BIOMES[b]["colors"]["fog_color"]
        fogs.append(f"{b.split(':')[1]} #{fc:06X}->#{int((fc >> 16) * 0.68):02X}{int(((fc >> 8) & 255) * 0.68):02X}"
                    f"{int((fc & 255) * 0.68):02X}")
    R.ok(f"{c.v}: {len(rd.ALL_BIOMES)} biomes: sounds, particles, carvers, spawners (= spec table), spawn costs and {len(nodes)} feature refs "
         f"resolve; no feature-order cycle; fog at dusk: {', '.join(fogs)}")
    return remnant_entities


def check_features(c: Ctx):
    stats = {"types": set(), "states": 0, "refs": set()}
    cf_dir = c.ns / "worldgen" / "configured_feature"
    pf_dir = c.ns / "worldgen" / "placed_feature"
    want_cf = {"graceroot_tree", "graceroot_tree_giant", "withered_tree", "gravestone_patch", "bone_patch", "ash_patch",
               "eclipse_boulder", "ruined_pillar"}
    have_cf = {p.stem for p in cf_dir.glob("*.json")}
    if not want_cf <= have_cf:
        R.err(f"{c.v}: configured features missing: {sorted(want_cf - have_cf)}")
    for p in sorted(cf_dir.glob("*.json")):
        j = load(p)
        if j.get("type") not in c.reg["minecraft:worldgen/feature"]:
            R.err(f"{c.where(p)}: feature type {j.get('type')}")
        walk_feature_json(c, j, c.where(p), stats)
        if j.get("type") == "minecraft:tree":
            cfg = j["config"]
            for k in ("trunk_provider", "trunk_placer", "foliage_provider", "foliage_placer", "minimum_size", "decorators",
                      "ignore_vines", "force_dirt", "dirt_provider"):
                if k not in cfg:
                    R.err(f"{c.where(p)}: tree config lacks {k}")
            if p.stem == "withered_tree" and "leaves" in json.dumps(cfg):
                R.err(f"{c.where(p)}: the withered tree must have no leaves")
            tp = cfg["trunk_placer"]
            if tp["type"] == "minecraft:cherry_trunk_placer":
                bs = tp["branch_start_offset_from_top"]
                if bs["max_inclusive"] - bs["min_inclusive"] < 1:
                    R.err(f"{c.where(p)}: cherry branch_start_offset_from_top needs a range of at least 2")
    for p in sorted(pf_dir.glob("*.json")):
        j = load(p)
        walk_feature_json(c, j, c.where(p), stats)
        mods = [m["type"] for m in j["placement"]]
        if mods[-1] != "minecraft:biome" and "minecraft:biome" not in mods:
            R.err(f"{c.where(p)}: placed feature has no biome filter")
    R.ok(f"{c.v}: {len(have_cf)} configured + {len(list(pf_dir.glob('*.json')))} placed features: {len(stats['types'])} "
         f"distinct types registered in {c.v}, {stats['states']} block states valid, int providers in {c.v} format")


def check_tags(c: Ctx):
    native_block = rd.VERSIONS[c.v]["block_tags"]
    native_item = "items" if rd.is120(c.v) else "item"
    n = 0
    for folder in rd.BLOCK_TAG_DIRS + rd.ITEM_TAG_DIRS:
        is_block = folder in rd.BLOCK_TAG_DIRS
        spec = rd.BLOCK_TAGS if is_block else rd.ITEM_TAGS
        for name, values in spec.items():
            p = c.data / "minecraft" / "tags" / folder / f"{name}.json"
            if not p.exists():
                R.err(f"{c.v}: tag {folder}/{name} missing")
                continue
            j = load(p)
            n += 1
            if j.get("replace") is not False:
                R.err(f"{c.where(p)}: replace must be false")
            for v in values:
                if v not in j["values"]:
                    R.err(f"{c.where(p)}: {v} missing")
            for v in j["values"]:
                if v.startswith("#"):
                    continue
                known = (c.blocks if is_block else c.items)
                if v.startswith("minecraft:") and v not in known:
                    R.err(f"{c.where(p)}: unknown {'block' if is_block else 'item'} {v}")
                elif v.startswith(f"{NS}:") and v not in (rd.REALM_BLOCKS if is_block else rd.REALM_ITEMS) and v not in known:
                    R.warn(f"{c.where(p)}: {v} is not a realm {'block' if is_block else 'item'} (another feature's entry)")
            if folder in (native_block, native_item) and f"minecraft:{name}" not in c.tags.get(folder, set()):
                R.err(f"{c.where(p)}: minecraft:{name} is not a vanilla {folder} tag in {c.v}")
    biome_dir = c.ns / "tags" / "worldgen" / "biome"
    want = dict(rd.BIOME_TAGS)
    want["has_structure/grace_shrine"] = rd.SURFACE_LAND
    for name, values in want.items():
        p = biome_dir / f"{name}.json"
        if not p.exists():
            R.err(f"{c.v}: biome tag {name} missing")
            continue
        j = load(p)
        if j["values"] != values or j.get("replace") is not False:
            R.err(f"{c.where(p)}: values {j['values']} != {values}")
        for v in j["values"]:
            if not (c.ns / "worldgen" / "biome" / f"{v.split(':')[1]}.json").exists():
                R.err(f"{c.where(p)}: biome {v} missing")
    R.ok(f"{c.v}: {n} vanilla block/item tag files (both folder spellings) carry the realm entries, replace false; "
         f"biome tags is/in_eclipsed_realm, has_structure/hollow_citadel (fields + wastes), has_structure/grace_shrine")


def check_loot(c: Ctx):
    d = c.ns / rd.VERSIONS[c.v]["loot"] / "blocks"
    tables = rd.loot_tables(c.v)
    for block in rd.REALM_BLOCKS:
        p = d / f"{block.split(':')[1]}.json"
        if not p.exists():
            R.err(f"{c.v}: loot table for {block} missing")
            continue
        j = load(p)
        if j != tables[block]:
            R.err(f"{c.where(p)}: differs from the generator")
        if j.get("type") != "minecraft:block":
            R.err(f"{c.where(p)}: type {j.get('type')}")
        text = json.dumps(j)
        for name in re.findall(r'"name": "([^"]+)"', text):
            if name.startswith("minecraft:") and name not in c.items:
                R.err(f"{c.where(p)}: unknown item {name}")
            if name.startswith(f"{NS}:") and name not in rd.REALM_ITEMS:
                R.err(f"{c.where(p)}: {name} is not a realm item")
        if "silk_touch" in text:
            if rd.is120(c.v) and '"enchantment": "minecraft:silk_touch"' not in text:
                R.err(f"{c.where(p)}: 1.20.1 silk touch predicate format")
            if not rd.is120(c.v) and '"minecraft:enchantments"' not in text:
                R.err(f"{c.where(p)}: 1.21 silk touch predicate format")
    empties = [b.split(":")[1] for b in rd.REALM_BLOCKS if not tables[b]["pools"]]
    R.ok(f"{c.v}: {len(rd.REALM_BLOCKS)} block loot tables (silk/cobbled stone, leaves->sticks, double slab, waygate item; "
         f"no drops: {', '.join(empties)})")


def check_recipes(c: Ctx):
    folder = "recipes" if rd.is120(c.v) else "recipe"
    d = c.ns / folder
    want = rd.recipes(c.v)
    serializers = c.reg["minecraft:recipe_serializer"]
    for name, r in want.items():
        p = d / f"{name}.json"
        if not p.exists():
            R.err(f"{c.v}: recipe {name} missing")
            continue
        j = load(p)
        if j != r:
            R.err(f"{c.where(p)}: differs from the generator")
        if j["type"] not in serializers:
            R.err(f"{c.where(p)}: recipe type {j['type']}")
        res = j["result"]
        if j["type"] in ("minecraft:crafting_shaped", "minecraft:crafting_shapeless"):
            key = "item" if rd.is120(c.v) else "id"
            if key not in res:
                R.err(f"{c.where(p)}: result needs '{key}' in {c.v}")
        elif j["type"] in ("minecraft:smelting", "minecraft:stonecutting"):
            if rd.is120(c.v) and not isinstance(res, str):
                R.err(f"{c.where(p)}: 1.20.1 {j['type']} result must be an item id string")
            if not rd.is120(c.v) and not (isinstance(res, dict) and "id" in res):
                R.err(f"{c.where(p)}: {c.v} {j['type']} result must be {{id, count}}")
        ids = re.findall(r'"(?:item|id)": "([^"]+)"', json.dumps(j)) + ([res] if isinstance(res, str) else [])
        for i in ids:
            if i.startswith("minecraft:") and i not in c.items:
                R.err(f"{c.where(p)}: unknown item {i}")
            if i.startswith(f"{NS}:") and i not in rd.REALM_ITEMS:
                R.err(f"{c.where(p)}: {i} is not a realm item")
    shard = load(d / "eclipse_shard.json")
    if sorted(x["item"] for x in shard["ingredients"]) != ["minecraft:amethyst_shard", "minecraft:ender_pearl",
                                                            "minecraft:gold_ingot"]:
        R.err(f"{c.v}: eclipse_shard ingredients")
    gate = load(d / "eclipse_waygate.json")
    if gate["pattern"] != ["DSD", "D D", "DGD"]:
        R.err(f"{c.v}: eclipse_waygate pattern")
    if "minecraft:planks" not in c.tags.get("items" if rd.is120(c.v) else "item", set()):
        R.err(f"{c.v}: vanilla #planks item tag missing")
    R.ok(f"{c.v}: {len(want)} recipes in the {c.v} format ({folder}/; shaped/shapeless result "
         f"'{'item' if rd.is120(c.v) else 'id'}', cooking/stonecutting result {'string' if rd.is120(c.v) else 'object'})")


def check_template_states(c: Ctx, tpl, where):
    seen = set()
    for p, (s, nbt) in tpl.blocks.items():
        if not all(0 <= p[i] < tpl.size[i] for i in range(3)):
            R.err(f"{where}: block {p} outside size {tpl.size}")
        if s in seen:
            continue
        seen.add(s)
        name, props = parse_state(s)
        check_state(c, {"Name": name, "Properties": props}, where)
        if name == "minecraft:structure_void":
            R.err(f"{where}: structure_void stored in the template (omit the cell instead)")
        info = c.blocks.get(name)
        if info and set(info["properties"]) - set(props):
            R.err(f"{where}: {name} stored without all properties")


def staged_equal(c: Ctx, rel: Path):
    a = OUT / c.v / "data" / rel
    b = c.data / rel
    if not a.exists() or not b.exists() or a.read_bytes() != b.read_bytes():
        R.err(f"{c.v}: installed data/{rel.as_posix()} differs from the staged copy (re-run the generator with --install)")


def check_arrival_shrine(c: Ctx):
    rel = Path(NS) / rd.VERSIONS[c.v]["structures"] / "realm" / "arrival_shrine.nbt"
    p = c.data / rel
    if not p.exists():
        R.err(f"{c.v}: {rel.as_posix()} missing")
        return
    staged_equal(c, rel)
    tpl, root = load_template(p)
    w = f"{c.v} realm/arrival_shrine"
    if int(root["DataVersion"]) != rd.VERSIONS[c.v]["data_version"]:
        R.err(f"{w}: DataVersion {int(root['DataVersion'])}")
    if tpl.size != (11, 9, 11):
        R.err(f"{w}: size {tpl.size} != (11, 9, 11)")
    check_template_states(c, tpl, w)
    b = {q: s for q, (s, _) in tpl.blocks.items()}
    nbt = {q: n for q, (_, n) in tpl.blocks.items() if n is not None}

    def name(q):
        return state_name(b[q]) if q in b else None

    def props(q):
        return parse_state(b[q])[1] if q in b else {}

    core = (5, 1, 2)
    if name(core) != f"{NS}:eclipse_waygate" or props(core).get("facing") != "south" or props(core).get("phase") != "open":
        R.err(f"{w}: core at {core} is {b.get(core)}")
    if set(nbt) != {core} or dict(nbt.get(core, {})) != {"id": f"{NS}:eclipse_waygate"}:
        R.err(f"{w}: block entities {nbt} (want only an empty eclipse_waygate compound at {core})")
    frame = [(4, y, 2) for y in range(1, 5)] + [(6, y, 2) for y in range(1, 5)] + [(5, 4, 2)]
    for q in frame:
        if name(q) != f"{NS}:waygate_frame":
            R.err(f"{w}: frame missing at {q} ({b.get(q)})")
    for q in ((5, 2, 2), (5, 3, 2)):
        if name(q) != f"{NS}:waygate_veil" or props(q).get("open") != "true":
            R.err(f"{w}: veil at {q} is {b.get(q)}")
    if name((5, 1, 7)) != f"{NS}:grace_ember":
        R.err(f"{w}: grace ember missing at (5,1,7)")
    for q in ((5, 1, 4), (5, 2, 4), (5, 1, 3), (5, 2, 3), (5, 1, 5), (5, 1, 6)):
        if b.get(q) != AIR:
            R.err(f"{w}: landing / path cell {q} is {b.get(q)} (must be air)")
    floor = [(x, 0, z) for x in range(11) for z in range(11)]
    missing = [q for q in floor if q not in b or b[q] == AIR]
    if missing:
        R.err(f"{w}: y=0 floor has {len(missing)} empty cells, e.g. {missing[:4]}")
    unstored = [(x, y, z) for x in range(11) for y in range(1, 9) for z in range(11) if (x, y, z) not in b]
    if unstored:
        R.err(f"{w}: {len(unstored)} cells above the floor are not stored (terrain would stay), e.g. {unstored[:3]}")
    solid_up = [q for q, s in b.items() if q[1] >= 1 and s != AIR]
    floating = [q for q in solid_up if q[1] >= 1 and b.get((q[0], q[1] - 1, q[2]), AIR) == AIR
                and name(q) not in (f"{NS}:waygate_veil",)]
    if floating:
        R.err(f"{w}: unsupported blocks {floating}")
    ring = [state_name(b[(5 + dx, 0, 5 + dz)]) for dx, dz in ((3, 0), (-3, 0), (0, 3), (0, -3), (2, 2), (-2, -2))]
    if any(r != f"{NS}:chiseled_eclipse_stone_bricks" for r in ring) or name((5, 0, 5)) != f"{NS}:chiseled_eclipse_stone_bricks":
        R.err(f"{w}: floor ring pattern missing")
    R.ok(f"{c.v}: realm/arrival_shrine 11x9x11 DataVersion {int(root['DataVersion'])}: open south-facing Waygate at (5,1,2) "
         f"(empty BE), frame x4..6 y1..4, veils (5,2..3,2), grace (5,1,7), landing (5,1,4) clear, solid 11x11 floor at y=0, "
         f"all {11 * 8 * 11 - len(solid_up)} other cells above it stored as air, {len(solid_up)} placed blocks, none floating")


def check_grace_shrine(c: Ctx):
    import grace_shrine as gs  # noqa: PLC0415
    rels = [Path(NS) / rd.VERSIONS[c.v]["structures"] / "grace_shrine" / "shrine.nbt",
            Path(NS) / "worldgen" / "structure" / "grace_shrine.json",
            Path(NS) / "worldgen" / "structure_set" / "grace_shrine.json",
            Path(NS) / "worldgen" / "template_pool" / "grace_shrine" / "start.json",
            Path(NS) / "worldgen" / "processor_list" / "grace_shrine_weathering.json",
            Path(NS) / "tags" / "worldgen" / "biome" / "has_structure" / "grace_shrine.json"]
    for rel in rels:
        if not (c.data / rel).exists():
            R.err(f"{c.v}: data/{rel.as_posix()} missing")
            return
        staged_equal(c, rel)
    st = load(c.data / rels[1])
    allowed = {"type", "biomes", "step", "terrain_adaptation", "spawn_overrides", "start_pool", "start_jigsaw_name",
               "size", "start_height", "use_expansion_hack", "project_start_to_heightmap", "max_distance_from_center"}
    if not rd.is120(c.v):
        allowed |= {"pool_aliases", "dimension_padding", "liquid_settings"}
    for k in st:
        if k not in allowed:
            R.err(f"{c.v}: grace_shrine structure field {k!r} does not exist in {c.v}")
    exp = {"type": "minecraft:jigsaw", "step": "surface_structures", "terrain_adaptation": "beard_thin",
           "biomes": f"#{NS}:has_structure/grace_shrine", "start_pool": gs.POOL,
           "project_start_to_heightmap": "WORLD_SURFACE_WG", "start_height": {"absolute": 0}}
    for k, v in exp.items():
        if st.get(k) != v:
            R.err(f"{c.v}: grace_shrine {k}={st.get(k)} != {v}")
    if not isinstance(st.get("max_distance_from_center"), int):
        R.err(f"{c.v}: grace_shrine max_distance_from_center must be an int")
    ss = load(c.data / rels[2])
    pl = ss["placement"]
    if (pl["type"], pl["spacing"], pl["separation"]) != ("minecraft:random_spread", 24, 8):
        R.err(f"{c.v}: grace_shrine placement {pl}")
    salts = {}
    for f in (c.ns / "worldgen" / "structure_set").glob("*.json"):
        s = load(f)["placement"].get("salt")
        salts.setdefault(s, []).append(f.stem)
    if len(salts.get(pl["salt"], [])) != 1:
        R.err(f"{c.v}: grace_shrine salt {pl['salt']} shared with {salts.get(pl['salt'])}")
    pool = load(c.data / rels[3])
    el = pool["elements"][0]["element"]
    if el["location"] != gs.TEMPLATE or el["projection"] != "rigid" or el["processors"] != gs.WEATHERING:
        R.err(f"{c.v}: grace_shrine pool element {el}")
    for proc in load(c.data / rels[4])["processors"]:
        if proc["processor_type"] not in c.reg["minecraft:worldgen/structure_processor"]:
            R.err(f"{c.v}: processor {proc['processor_type']}")
        for r in proc.get("rules", []):
            for k in ("block",):
                check_block_id(c, r["input_predicate"][k], f"{c.v} grace_shrine_weathering")
            check_state(c, r["output_state"], f"{c.v} grace_shrine_weathering")
            if r["input_predicate"]["block"] in (f"{NS}:grace_ember", f"{NS}:gilded_leaves"):
                R.err(f"{c.v}: weathering touches {r['input_predicate']['block']}")
    tpl, root = load_template(c.data / rels[0])
    w = f"{c.v} grace_shrine/shrine"
    if int(root["DataVersion"]) != rd.VERSIONS[c.v]["data_version"]:
        R.err(f"{w}: DataVersion")
    check_template_states(c, tpl, w)
    if any(s != c2 for s, c2 in [(tpl.size, (7, 6, 7))]):
        R.err(f"{w}: size {tpl.size}")
    embers = [q for q, (s, _) in tpl.blocks.items() if state_name(s) == f"{NS}:grace_ember"]
    if embers != [(3, 1, 3)]:
        R.err(f"{w}: grace embers at {embers}")
    leaves = [s for s, _ in tpl.blocks.values() if state_name(s) == f"{NS}:gilded_leaves"]
    if any("persistent=true" not in s for s in leaves):
        R.err(f"{w}: gilded leaves in the template must be persistent")
    if any(state_name(s) == "minecraft:jigsaw" for s, _ in tpl.blocks.values()):
        R.err(f"{w}: unexpected jigsaw block")
    from common import assemble, build_world  # noqa: PLC0415  (staged copy, verified equal above)
    for rot in range(4):
        placed = assemble(c.v, structure_id=gs.STRUCTURE, start_rotation=rot)
        world, _ = build_world(c.v, placed, weathering=True)
        embers = [q for q, s in world.items() if state_name(s) == f"{NS}:grace_ember"]
        if len(placed) != 1 or len(embers) != 1:
            R.err(f"{w}: start rotation {rot} assembles {len(placed)} pieces with {len(embers)} embers")
    R.ok(f"{c.v}: grace_shrine 7x6x7 single-piece jigsaw (surface, beard_thin, random_spread 24/8 salt {pl['salt']} unique), "
         f"ember at (3,1,3), {len(leaves)} persistent gilded leaves, weathering touches bricks only")


def registrations(version, remnant_entities):
    src = REPO / version / "common" / "src" / "main" / "java"
    text = ""
    for f in src.rglob("*.java"):
        if "init" in f.parts or f.name.startswith("Mod"):
            text += f.read_text(encoding="utf-8", errors="ignore")
    names = [b.split(":")[1] for b in rd.REALM_BLOCKS] + ["eclipse_shard", "grace_mote"] + \
            [e.split(":")[1] for e in sorted(remnant_entities)] + ["eclipsed_realm"]
    missing = [n for n in names if f'"{n}"' not in text]
    return names, missing


def main():
    for version in VERSIONS:
        c = Ctx(version)
        check_json_tree(c)
        check_dimension_type(c)
        boxes = check_dimension(c)
        if version == "1.20.1" and boxes:
            R.ok(f"climate shares at the surface (5x5 temperature/humidity bands, land): {climate_shares(boxes)}")
        check_noise_settings(c)
        ents = check_biomes(c)
        check_features(c)
        check_tags(c)
        check_loot(c)
        check_recipes(c)
        check_arrival_shrine(c)
        check_grace_shrine(c)
        names, missing = registrations(version, ents)
        if missing:
            R.warn(f"{version}: not found in the Java sources yet (the datapack cannot load until they are registered): "
                   f"{', '.join(missing)}")
        else:
            R.ok(f"{version}: all {len(names)} realm registry names appear in the Java sources")
    for m in R.info:
        print("OK   ", m)
    for m in R.warnings:
        print("WARN ", m)
    for m in R.errors:
        print("ERROR", m)
    print(f"\n{len(R.errors)} errors, {len(R.warnings)} warnings")
    print("Not verifiable offline: Java registration of the realm blocks/items, remnant_bosses:grace_mote, the "
          "remnants:* entity types and the remnant_bosses:eclipsed_realm sky effects; in-game terrain look; spawn rates.")
    return 1 if R.errors else 0


if __name__ == "__main__":
    sys.exit(main())
