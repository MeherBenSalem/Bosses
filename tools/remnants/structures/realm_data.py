#!/usr/bin/env python3
"""Eclipsed Realm datapack generator.

Writes straight into both mod resource trees (1.20.1 and 1.21.1, namespace remnant_bosses):

  dimension_type/eclipsed_realm.json      fixed dusk, no beds/anchors/raids, overworld height
  dimension/eclipsed_realm.json           noise generator + explicit multi-noise parameter list
  worldgen/noise_settings/eclipsed_realm  the version's vanilla overworld settings (router, aquifers,
                                          ore veins, bedrock and deepslate kept), eclipse stone as
                                          the default block and a surface rule for the realm biomes
  worldgen/biome/<biome>.json             tarnished_fields, gracewood, gravefield_barrows,
                                          ashen_wastes, lantern_heights, stilled_sea, ossuary_depths
  worldgen/configured_feature, placed_feature
                                          graceroot trees, withered trees, gravestones, bones, ash,
                                          boulders, ruined pillars, cobwebs (vanilla feature types)
  tags/worldgen/biome                     is_eclipsed_realm, in_eclipsed_realm,
                                          has_structure/hollow_citadel (Realm biomes only)
  tags/block(s), tags/item(s)             vanilla tags the realm blocks join (merged, replace false)
  loot_table(s)/blocks, recipe(s)         realm block drops and recipes

Vanilla data (overworld noise settings, plains feature lists, registry IDs and tag lists) comes
from the version's server jar: the data generator runs once with --server --reports and the
parts used by the realm tools are cached in out/.vanilla/<version>.worldgen.json.

The grace shrine and the arrival shrine are separate (grace_shrine.py, arrival_shrine.py).
Validate everything with tools/remnants/realm/validate_data.py.

Usage: python tools/remnants/structures/realm_data.py [--version 1.20.1] [--dry-run]
"""
from __future__ import annotations

import argparse
import copy
import json
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import common  # noqa: E402
from common import ENTITY_NS, NS, OUT, REPO, VERSIONS  # noqa: E402


def rb(n):
    return f"{NS}:{n}"


def mc(n):
    return f"minecraft:{n}"


def ent(n):
    return f"{ENTITY_NS}:{n}"


DIMENSION = rb("eclipsed_realm")
FIELDS, GRACEWOOD, BARROWS, WASTES, SEA, OSSUARY = (rb(n) for n in (
    "tarnished_fields", "gracewood", "gravefield_barrows", "ashen_wastes", "stilled_sea", "ossuary_depths"))
# The Lantern Heights: inland mountains.
HEIGHTS = rb("lantern_heights")
SURFACE_LAND = [FIELDS, GRACEWOOD, BARROWS, WASTES, HEIGHTS]
ALL_BIOMES = SURFACE_LAND + [SEA, OSSUARY]

E, CE = rb("eclipse_stone"), rb("cobbled_eclipse_stone")
BR, CBR, CH = rb("eclipse_stone_bricks"), rb("cracked_eclipse_stone_bricks"), rb("chiseled_eclipse_stone_bricks")
STAIRS, SLAB, WALL = rb("eclipse_stone_brick_stairs"), rb("eclipse_stone_brick_slab"), rb("eclipse_stone_brick_wall")
LOG, PLANKS, LEAVES = rb("graceroot_log"), rb("graceroot_planks"), rb("gilded_leaves")
GRAVE, EMBER, FOG_GATE = rb("weathered_gravestone"), rb("grace_ember"), rb("fog_gate")
WAYGATE, FRAME, VEIL = rb("eclipse_waygate"), rb("waygate_frame"), rb("waygate_veil")
SHARD = rb("eclipse_shard")
GRACE_MOTE = rb("grace_mote")

# Realm blocks and the items they drop; used by loot tables and validation.
REALM_BLOCKS = [E, CE, BR, CBR, CH, STAIRS, SLAB, WALL, LOG, PLANKS, LEAVES, GRAVE, EMBER, WAYGATE, FRAME, VEIL,
                FOG_GATE]
REALM_ITEMS = [E, CE, BR, CBR, CH, STAIRS, SLAB, WALL, LOG, PLANKS, LEAVES, GRAVE, EMBER, WAYGATE, FOG_GATE, SHARD]

# Remnant entity types the biome spawners reference (registered in Java).
LEGION = [ent("hollow_sentinel"), ent("hollow_arbalist"), ent("fallen_standard_bearer"), ent("soulbound_knight")]
CRYPTBORN = [ent("gravebound_soldier"), ent("crypt_lurker"), ent("catacomb_rat")]
SOULS = [ent("lost_pilgrim"), ent("memory_wraith"), ent("soul_collector")]
# They roam the Realm ahead of their boss slices (docs/remnants/families/ashbound-and-relics.md).
ASHBOUND = [ent("ashbound_crawler"), ent("ember_servitor"), ent("rusted_guardian")]
RELICS = [ent("shardling"), ent("ancient_vessel"), ent("relic_stalker")]


def is120(version):
    return version == "1.20.1"


def res_data(version) -> Path:
    return REPO / version / "common" / "src" / "main" / "resources" / "data"


def server_jar(version) -> Path | None:
    p = Path.home() / ".gradle" / "caches" / "fabric-loom" / version / "minecraft-server.jar"
    if p.exists():
        return p
    return common._find_jar(version, "server")


# Vanilla feature JSON copied verbatim into the cache (realm copies are derived from them).
VANILLA_FEATURE_FILES = [
    "configured_feature/spring_water", "configured_feature/spring_lava_overworld", "configured_feature/glow_lichen",
    "configured_feature/lake_lava", "placed_feature/spring_water", "placed_feature/spring_lava",
    "placed_feature/glow_lichen", "placed_feature/lake_lava_underground", "placed_feature/lake_lava_surface",
]
VANILLA_TAG_FILES = ["stone_ore_replaceables", "base_stone_overworld", "overworld_carver_replaceables"]


def vanilla_worldgen(version) -> dict:
    """Vanilla worldgen data of one version, from the server jar's data generator (cached)."""
    cache = OUT / ".vanilla" / f"{version}.worldgen.json"
    if cache.exists():
        data = json.loads(cache.read_text(encoding="utf-8"))
        if set(VANILLA_FEATURE_FILES) <= set(data.get("feature_json", {})):
            return data
    jar, java = server_jar(version), shutil.which("java")
    if not jar or not java:
        raise SystemExit(f"[{version}] need the Minecraft server jar in the Gradle cache and java on PATH")
    print(f"[{version}] running the vanilla data generator (once) ...")
    with tempfile.TemporaryDirectory() as td:
        subprocess.run([java, "-DbundlerMainClass=net.minecraft.data.Main", "-jar", str(jar), "--server", "--reports",
                        "--output", str(Path(td) / "gen")], cwd=td, capture_output=True, timeout=1800, check=True)
        gen = Path(td) / "gen"
        mcdata = gen / "data" / "minecraft"
        wg = mcdata / "worldgen"

        def ids(d: Path):
            return sorted("minecraft:" + p.relative_to(d).with_suffix("").as_posix() for p in d.rglob("*.json"))

        tags: dict = {}
        for p in (mcdata / "tags").rglob("*.json"):
            rel = p.relative_to(mcdata / "tags")
            k = 2 if rel.parts[0] == "worldgen" else 1
            tags.setdefault("/".join(rel.parts[:k]), []).append("minecraft:" + Path(*rel.parts[k:]).with_suffix("").as_posix())
        regs = json.loads((gen / "reports" / "registries.json").read_text(encoding="utf-8"))
        blocks = json.loads((gen / "reports" / "blocks.json").read_text(encoding="utf-8"))
        data = {
            "noise_settings_overworld": json.loads((wg / "noise_settings" / "overworld.json").read_text(encoding="utf-8")),
            "dimension_type_overworld": json.loads((mcdata / "dimension_type" / "overworld.json").read_text(encoding="utf-8")),
            "plains": json.loads((wg / "biome" / "plains.json").read_text(encoding="utf-8")),
            "worldgen": {d.name: ids(d) for d in sorted(wg.iterdir()) if d.is_dir()},
            "registries": {k: sorted(v["entries"]) for k, v in regs.items()},
            "tags": {k: sorted(v) for k, v in sorted(tags.items())},
            "blocks": {n: {"properties": i.get("properties", {}),
                           "default": next((s.get("properties", {}) for s in i["states"] if s.get("default")), {})}
                       for n, i in blocks.items()},
            "feature_json": {f: json.loads((wg / f"{f}.json").read_text(encoding="utf-8")) for f in VANILLA_FEATURE_FILES},
            "block_tag_json": {t: json.loads((mcdata / "tags" / VERSIONS[version]["block_tags"] / f"{t}.json")
                                             .read_text(encoding="utf-8")) for t in VANILLA_TAG_FILES},
        }
    cache.parent.mkdir(parents=True, exist_ok=True)
    cache.write_text(json.dumps(data, separators=(",", ":")), encoding="utf-8")
    return data


def st(name, **props):
    s = {"Name": name}
    if props:
        s["Properties"] = {k: str(v).lower() if isinstance(v, bool) else str(v) for k, v in props.items()}
    return s


def simple(state):
    return {"type": "minecraft:simple_state_provider", "state": state}


def weighted(entries):
    return {"type": "minecraft:weighted_state_provider", "entries": [{"data": s, "weight": w} for s, w in entries]}


def uniform_int(version, lo, hi):
    """IntProvider uniform: nested under "value" until 1.20.4, flat from 1.20.5."""
    if is120(version):
        return {"type": "minecraft:uniform", "value": {"min_inclusive": lo, "max_inclusive": hi}}
    return {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}


def weighted_int(pairs):
    return {"type": "minecraft:weighted_list", "distribution": [{"data": d, "weight": w} for d, w in pairs]}


def matching(blocks, offset=None):
    p = {"type": "minecraft:matching_blocks", "blocks": list(blocks)}
    if offset:
        p["offset"] = list(offset)
    return p


AIR_HERE = matching(["minecraft:air"])
STURDY_BELOW = {"type": "minecraft:has_sturdy_face", "offset": [0, -1, 0], "direction": "up"}
GROUND = ["minecraft:grass_block", "minecraft:podzol", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:rooted_dirt"]


def all_of(*preds):
    return {"type": "minecraft:all_of", "predicates": list(preds)}


def inline(feature_type, config, placement=()):
    """An inline placed feature wrapping an inline configured feature."""
    return {"feature": {"type": feature_type, "config": config}, "placement": list(placement)}


def filt(pred):
    return {"type": "minecraft:block_predicate_filter", "predicate": pred}


COUNT = lambda c: {"type": "minecraft:count", "count": c}  # noqa: E731
RARITY = lambda n: {"type": "minecraft:rarity_filter", "chance": n}  # noqa: E731
HEIGHTMAP = lambda h: {"type": "minecraft:heightmap", "heightmap": h}  # noqa: E731
OFFSET_Y = lambda y: {"type": "minecraft:random_offset", "xz_spread": 0, "y_spread": y}  # noqa: E731
IN_SQUARE = {"type": "minecraft:in_square"}
BIOME = {"type": "minecraft:biome"}
NO_WATER = {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0}
SAPLING_OK = filt({"type": "minecraft:would_survive", "state": st("minecraft:oak_sapling", stage=0)})


def dimension_type(version):
    light = (uniform_int(version, 0, 7))
    return {
        "ultrawarm": False,
        "natural": False,
        "coordinate_scale": 1.0,
        "has_skylight": True,
        "has_ceiling": False,
        "ambient_light": 0.0,
        "fixed_time": 12000,
        "monster_spawn_light_level": light,
        "monster_spawn_block_light_limit": 0,
        "piglin_safe": False,
        "bed_works": False,
        "respawn_anchor_works": False,
        "has_raids": False,
        "logical_height": 384,
        "min_y": -64,
        "height": 384,
        "infiniburn": "#minecraft:infiniburn_overworld",
        "effects": DIMENSION,
    }


# Climate map. Axes span the full codec range [-2, 2]; land is continentalness >= -0.11 (coast
# and oceans are the Stilled Sea); temperature/humidity use the vanilla band edges.
LO, HI = -2.0, 2.0
T_BANDS = [(LO, -0.45), (-0.45, -0.15), (-0.15, 0.2), (0.2, 0.55), (0.55, HI)]
H_BANDS = [(LO, -0.35), (-0.35, -0.1), (-0.1, 0.1), (0.1, 0.3), (0.3, HI)]
LAND_C, SEA_C = (-0.11, HI), (LO, -0.11)
NEAR_C, INLAND_C = (-0.11, 0.03), (0.03, HI)   # vanilla near-inland, then mid and far inland
HIGH_EROSION = 0.55                     # vanilla erosion band 6 (flattest land)
MOUNTAIN_EROSION = -0.375               # vanilla erosion bands 0-1: inland, these are the mountains
SURFACE_DEPTHS = [(LO, 0.2), (0.9, HI)]  # above and below the cave band
CAVE_DEPTH = (0.2, 0.9)
_B, _F, _G, _W, _H = "gravefield_barrows", "tarnished_fields", "gracewood", "ashen_wastes", "lantern_heights"
# rows = temperature bands (cold -> hot), columns = humidity bands (dry -> humid);
# a pair is (weirdness < 0, weirdness >= 0). Fields on high erosion become ashen flats. Inland mountains
# (continentalness >= 0.03, erosion < -0.375) are the Lantern Heights at every temperature and humidity.
CLIMATE_GRID = [
    [_B, _B, _B, _B, _G],
    [_B, (_F, _B), _F, _G, _G],
    [_F, _F, _F, (_F, _G), _G],
    [_W, _F, _F, _F, _G],
    [_W, _W, (_W, _F), _F, _G],
]


def _param(t, h, c, e, w, d):
    return {"temperature": list(t), "humidity": list(h), "continentalness": list(c), "erosion": list(e),
            "weirdness": list(w), "depth": list(d), "offset": 0.0}


def climate_boxes():
    """(biome id, parameters) boxes that tile [-2, 2]^6 (offset fixed at 0)."""
    land = []  # (t, h, w, c, e, biome)
    for ti, row in enumerate(CLIMATE_GRID):
        # merge neighbouring humidity cells that hold the same spec
        runs = []
        for hi, cell in enumerate(row):
            if runs and runs[-1][1] == cell:
                runs[-1][0][1] = H_BANDS[hi][1]
            else:
                runs.append(([H_BANDS[hi][0], H_BANDS[hi][1]], cell))
        for hrange, cell in runs:
            halves = [((LO, 0.0), cell[0]), ((0.0, HI), cell[1])] if isinstance(cell, tuple) else [((LO, HI), cell)]
            for wrange, biome in halves:
                erosions = [((LO, HIGH_EROSION), _F), ((HIGH_EROSION, HI), _W)] if biome == _F else [((LO, HI), biome)]
                for (elo, ehi), b in erosions:
                    land.append((T_BANDS[ti], tuple(hrange), wrange, NEAR_C, (elo, ehi), b))
                    # inland, the mountain erosion bands belong to the Lantern Heights (one box below)
                    land.append((T_BANDS[ti], tuple(hrange), wrange, INLAND_C, (max(elo, MOUNTAIN_EROSION), ehi), b))
    land.append(((LO, HI), (LO, HI), (LO, HI), INLAND_C, (LO, MOUNTAIN_EROSION), _H))
    out = []
    for d in SURFACE_DEPTHS:
        for t, h, w, c, e, b in land:
            out.append((rb(b), _param(t, h, c, e, w, d)))
        out.append((SEA, _param((LO, HI), (LO, HI), SEA_C, (LO, HI), (LO, HI), d)))
    out.append((OSSUARY, _param((LO, HI), (LO, HI), (LO, HI), (LO, HI), (LO, HI), CAVE_DEPTH)))
    return out


def dimension():
    return {
        "type": DIMENSION,
        "generator": {
            "type": "minecraft:noise",
            "settings": DIMENSION,
            "biome_source": {
                "type": "minecraft:multi_noise",
                "biomes": [{"biome": b, "parameters": p} for b, p in climate_boxes()],
            },
        },
    }


MAX = 1.7976931348623157e308


def r_block(state):
    return {"type": "minecraft:block", "result_state": state}


def r_seq(*rules):
    return {"type": "minecraft:sequence", "sequence": list(rules)}


def r_if(cond, rule):
    return {"type": "minecraft:condition", "if_true": cond, "then_run": rule}


def c_biome(*b):
    return {"type": "minecraft:biome", "biome_is": list(b)}


def c_noise(lo, hi, noise="minecraft:surface"):
    return {"type": "minecraft:noise_threshold", "noise": noise, "min_threshold": lo, "max_threshold": hi}


def c_depth(surface_type="floor", add_surface_depth=False, secondary=0):
    return {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": add_surface_depth,
            "secondary_depth_range": secondary, "surface_type": surface_type}


def c_water(offset, mult, add_stone_depth=False):
    return {"type": "minecraft:water", "offset": offset, "surface_depth_multiplier": mult, "add_stone_depth": add_stone_depth}


def c_y_above(y, mult, add_stone_depth=False):
    return {"type": "minecraft:y_above", "anchor": {"absolute": y}, "surface_depth_multiplier": mult,
            "add_stone_depth": add_stone_depth}


C_STEEP = {"type": "minecraft:steep"}
C_FLOOR, C_UNDER, C_CEILING = c_depth(), c_depth(add_surface_depth=True), c_depth("ceiling")
C_DRY = c_water(0, 0)                     # grass when no water directly above
C_AT_WATERLINE = c_water(-1, 0)           # top block at most one below the water surface (vanilla)
C_SHALLOW = c_water(-6, -1, True)         # not deep under water (vanilla)

B_ECLIPSE, B_DIRT, B_SAND, B_SANDSTONE = r_block(st(E)), r_block(st(mc("dirt"))), r_block(st(mc("sand"))), r_block(st(mc("sandstone")))
B_COARSE, B_GRAVEL = r_block(st(mc("coarse_dirt"))), r_block(st(mc("gravel")))
B_PODZOL, B_ROOTED = r_block(st(mc("podzol"), snowy=False)), r_block(st(mc("rooted_dirt")))
B_CALCITE = r_block(st(mc("calcite")))
GRASS = r_seq(r_if(C_DRY, r_block(st(mc("grass_block"), snowy=False))), B_DIRT)
SAFE_GRAVEL = r_seq(r_if(C_CEILING, B_ECLIPSE), B_GRAVEL)   # no gravel over a cave ceiling
SAFE_SAND = r_seq(r_if(C_CEILING, B_SANDSTONE), B_SAND)


def surface_rule(vanilla_rule):
    """Bedrock floor and deepslate gradient are the vanilla rules; the middle is ours."""
    seq = vanilla_rule["sequence"]
    bedrock, deepslate = seq[0], seq[-1]
    assert bedrock["if_true"].get("random_name") == "minecraft:bedrock_floor", bedrock
    assert deepslate["if_true"].get("random_name") == "minecraft:deepslate", deepslate
    wastes_mix = lambda last: r_seq(  # noqa: E731
        r_if(c_noise(0.32, MAX), B_ECLIPSE),       # eclipse stone outcrops
        r_if(c_noise(0.0, 0.32), SAFE_GRAVEL),     # grey ash and gravel
        r_if(c_noise(-0.3, 0.0), B_COARSE),
        last)
    top = r_seq(
        r_if(c_biome(SEA), r_seq(
            r_if(C_STEEP, B_ECLIPSE),
            r_if(c_y_above(64, 1), GRASS),                       # coastal uplands
            r_if(c_noise(-0.05, 0.05, "minecraft:gravel"), SAFE_GRAVEL),
            SAFE_SAND)),
        r_if(c_biome(HEIGHTS), r_seq(                            # pale calcite cliffs, sage grass between
            r_if(C_STEEP, B_CALCITE),
            r_if(c_noise(0.24, MAX), B_CALCITE),
            r_if(c_noise(-MAX, -0.45), B_COARSE),
            GRASS)),
        r_if(C_STEEP, B_ECLIPSE),                                # bare eclipse stone on cliffs
        r_if(c_biome(WASTES), wastes_mix(GRASS)),
        r_if(c_biome(BARROWS), r_seq(
            r_if(c_noise(0.18, MAX), B_PODZOL),
            r_if(c_noise(-MAX, -0.42), B_ROOTED),
            GRASS)),
        GRASS,                                                   # fields, gracewood
    )
    under = r_seq(
        r_if(c_biome(SEA), r_seq(r_if(c_y_above(64, 1), B_DIRT), SAFE_SAND)),
        r_if(c_biome(HEIGHTS), r_seq(r_if(C_STEEP, B_CALCITE), r_if(c_noise(0.24, MAX), B_CALCITE), B_DIRT)),
        r_if(C_STEEP, B_ECLIPSE),
        r_if(c_biome(WASTES), wastes_mix(B_COARSE)),
        r_if(c_biome(BARROWS), r_seq(r_if(c_noise(-MAX, -0.2), B_ROOTED), B_DIRT)),
        B_DIRT,
    )
    seabed = r_seq(
        r_if(c_biome(SEA), r_seq(r_if(c_noise(0.0, MAX), SAFE_SAND), SAFE_GRAVEL)),
        SAFE_GRAVEL,
    )
    surface = r_if({"type": "minecraft:above_preliminary_surface"}, r_seq(
        r_if(C_FLOOR, r_if(C_AT_WATERLINE, top)),
        r_if(C_SHALLOW, r_seq(
            r_if(C_UNDER, under),
            r_if(c_biome(SEA), r_if(c_depth(add_surface_depth=True, secondary=6), B_SANDSTONE)))),
        r_if(C_FLOOR, seabed),
    ))
    return r_seq(bedrock, surface, deepslate)


def noise_settings(version, vw):
    ns = copy.deepcopy(vw["noise_settings_overworld"])
    ns["default_block"] = st(E)
    ns["surface_rule"] = surface_rule(vw["noise_settings_overworld"]["surface_rule"])
    return ns


def tree(trunk_state, trunk_placer, foliage_state, foliage_placer, minimum_size):
    return {"type": "minecraft:tree", "config": {
        "trunk_provider": simple(trunk_state),
        "trunk_placer": trunk_placer,
        "foliage_provider": simple(foliage_state),
        "foliage_placer": foliage_placer,
        "minimum_size": minimum_size,
        "decorators": [],
        "ignore_vines": True,
        "force_dirt": False,
        "dirt_provider": simple(st(mc("dirt"))),
    }}


def _as_list(v):
    return list(v) if isinstance(v, list) else [v]


def realm_copies(vw):
    """Realm copies of vanilla features that only work on vanilla stone, derived from this version's JSON:
    springs (valid_blocks), glow lichen (can_be_placed_on) and lava lakes (stone barrier)."""
    fj = vw["feature_json"]
    cf, pf = {}, {}
    for name, src in (("spring_water", "spring_water"), ("spring_lava", "spring_lava_overworld")):
        j = copy.deepcopy(fj[f"configured_feature/{src}"])
        j["config"]["valid_blocks"] = _as_list(j["config"]["valid_blocks"]) + [E]
        cf[name] = j
    j = copy.deepcopy(fj["configured_feature/glow_lichen"])
    j["config"]["can_be_placed_on"] = _as_list(j["config"]["can_be_placed_on"]) + [E]
    cf["glow_lichen"] = j
    j = copy.deepcopy(fj["configured_feature/lake_lava"])
    j["config"]["barrier"] = simple(st(E))
    cf["lake_lava"] = j
    for placed, feature in (("spring_water", "spring_water"), ("spring_lava", "spring_lava"), ("glow_lichen", "glow_lichen"),
                            ("lake_lava_underground", "lake_lava"), ("lake_lava_surface", "lake_lava")):
        j = copy.deepcopy(fj[f"placed_feature/{placed}"])
        j["feature"] = rb(feature)
        pf[placed] = j
    return cf, pf


def configured_features(version, vw):
    log_y = st(LOG, axis="y")
    leaves = st(LEAVES, distance=7, persistent=False, waterlogged=False)
    cf = {}
    # tall, slightly gnarled tree with a broad canopy (fancy oak shape, bigger clusters)
    cf["graceroot_tree"] = tree(
        log_y, {"type": "minecraft:fancy_trunk_placer", "base_height": 6, "height_rand_a": 6, "height_rand_b": 0},
        leaves, {"type": "minecraft:fancy_foliage_placer", "radius": 3, "offset": 4, "height": 3},
        {"type": "minecraft:two_layers_feature_size", "limit": 0, "lower_size": 0, "upper_size": 0, "min_clipped_height": 4})
    # rare giant: 2x2 trunk with branches and a wide crown (mega jungle shape)
    cf["graceroot_tree_giant"] = tree(
        log_y, {"type": "minecraft:mega_jungle_trunk_placer", "base_height": 13, "height_rand_a": 2, "height_rand_b": 10},
        leaves, {"type": "minecraft:jungle_foliage_placer", "radius": 3, "offset": 0, "height": 2},
        {"type": "minecraft:two_layers_feature_size", "limit": 1, "lower_size": 1, "upper_size": 2})
    # bare trunk with one or two branches: cherry trunk, zero-radius blob foliage made of log
    # (it only fills the cell above each branch tip, so the tree has no leaves at all)
    cf["withered_tree"] = tree(
        log_y, {"type": "minecraft:cherry_trunk_placer", "base_height": 5, "height_rand_a": 2, "height_rand_b": 0,
                "branch_count": weighted_int([(1, 1), (2, 2)]),
                "branch_horizontal_length": uniform_int(version, 2, 3),
                "branch_start_offset_from_top": {"min_inclusive": -3, "max_inclusive": -2},
                "branch_end_offset_from_top": uniform_int(version, -1, 0)},
        log_y, {"type": "minecraft:blob_foliage_placer", "radius": 0, "offset": 0, "height": 0},
        {"type": "minecraft:two_layers_feature_size", "limit": 1, "lower_size": 0, "upper_size": 2})
    cf["gravestone_patch"] = {"type": "minecraft:random_patch", "config": {
        "tries": 10, "xz_spread": 5, "y_spread": 1,
        "feature": inline("minecraft:simple_block",
                          {"to_place": weighted([(st(GRAVE, facing=f), 1) for f in ("north", "south", "east", "west")])},
                          [filt(all_of(AIR_HERE, matching(GROUND, (0, -1, 0))))])}}
    bones = [(st(mc("bone_block"), axis="y"), 4), (st(mc("bone_block"), axis="x"), 2), (st(mc("bone_block"), axis="z"), 2)]
    skulls = [(st(mc("skeleton_skull"), rotation=r), 1) for r in (0, 3, 6, 9, 12)]
    cf["bone_patch"] = {"type": "minecraft:random_patch", "config": {
        "tries": 8, "xz_spread": 3, "y_spread": 2,
        "feature": inline("minecraft:simple_block", {"to_place": weighted(bones + skulls)},
                          [filt(all_of(AIR_HERE, STURDY_BELOW))])}}
    cf["ash_patch"] = {"type": "minecraft:disk", "config": {
        "state_provider": {"fallback": weighted([(st(mc("coarse_dirt")), 3), (st(mc("gravel")), 2)]), "rules": []},
        "target": matching(["minecraft:grass_block", "minecraft:dirt", "minecraft:podzol"]),
        "radius": uniform_int(version, 2, 4),
        "half_height": 1}}
    rock = lambda s: inline("minecraft:forest_rock", {"state": st(s)})  # noqa: E731
    cf["eclipse_boulder"] = {"type": "minecraft:random_selector", "config": {
        "features": [{"chance": 0.35, "feature": rock(CE)}], "default": rock(E)}}
    cf["ruined_pillar"] = {"type": "minecraft:block_column", "config": {
        "direction": "up",
        "allowed_placement": AIR_HERE,
        "prioritize_tip": False,
        "layers": [
            {"height": uniform_int(version, 1, 4), "provider": weighted([(st(BR), 3), (st(CBR), 2)])},
            {"height": weighted_int([(0, 3), (1, 1)]), "provider": simple(st(CH))},
        ]}}
    # Lantern Heights: violet crystal outcrops crowned with clusters, and broken sandstone stargazing pillars
    cf["amethyst_boulder"] = {"type": "minecraft:random_selector", "config": {
        "features": [{"chance": 0.4, "feature": rock(mc("calcite"))}], "default": rock(mc("amethyst_block"))}}
    cf["amethyst_cluster_patch"] = {"type": "minecraft:random_patch", "config": {
        "tries": 24, "xz_spread": 4, "y_spread": 3,
        "feature": inline("minecraft:simple_block",
                          {"to_place": weighted([(st(mc("amethyst_cluster"), facing="up", waterlogged=False), 1),
                                                 (st(mc("large_amethyst_bud"), facing="up", waterlogged=False), 2)])},
                          [filt(all_of(AIR_HERE, matching([mc("amethyst_block"), mc("calcite")], (0, -1, 0))))])}}
    cf["sandstone_pillar"] = {"type": "minecraft:block_column", "config": {
        "direction": "up",
        "allowed_placement": AIR_HERE,
        "prioritize_tip": False,
        "layers": [
            {"height": uniform_int(version, 2, 5),
             "provider": weighted([(st(mc("cut_sandstone")), 3), (st(mc("smooth_sandstone")), 2)])},
            {"height": weighted_int([(0, 2), (1, 1)]), "provider": simple(st(mc("chiseled_sandstone")))},
        ]}}
    cf["cobweb_patch"] = {"type": "minecraft:random_patch", "config": {
        "tries": 6, "xz_spread": 3, "y_spread": 2,
        "feature": inline("minecraft:simple_block", {"to_place": simple(st(mc("cobweb")))},
                          [filt(all_of(AIR_HERE, {"type": "minecraft:solid", "offset": [0, 1, 0]}))])}}
    cf.update(realm_copies(vw)[0])
    return cf


def _trees(feature, amount):
    return {"feature": rb(feature), "placement": [amount, IN_SQUARE, NO_WATER, HEIGHTMAP("OCEAN_FLOOR"), SAPLING_OK, BIOME]}


def _pillars(chance):
    return {"feature": rb("ruined_pillar"), "placement": [RARITY(chance), IN_SQUARE, NO_WATER, HEIGHTMAP("WORLD_SURFACE_WG"),
                                                          filt(all_of(AIR_HERE, STURDY_BELOW)), BIOME]}


def _patch(feature, amount):
    return {"feature": rb(feature), "placement": [amount, IN_SQUARE, HEIGHTMAP("WORLD_SURFACE_WG"), BIOME]}


def _ash(amount):
    return {"feature": rb("ash_patch"), "placement": [amount, IN_SQUARE, HEIGHTMAP("WORLD_SURFACE_WG"), OFFSET_Y(-1),
                                                      filt(matching(["minecraft:grass_block", "minecraft:dirt",
                                                                     "minecraft:podzol", "minecraft:coarse_dirt"])), BIOME]}


def _cave(feature, count, direction):
    scan = {"type": "minecraft:environment_scan", "direction_of_search": direction, "max_steps": 12,
            "allowed_search_condition": AIR_HERE, "target_condition": {"type": "minecraft:solid"}}
    return {"feature": rb(feature), "placement": [
        COUNT(count), IN_SQUARE,
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": -56},
                                                      "max_inclusive": {"absolute": 48}}},
        scan, OFFSET_Y(1 if direction == "down" else -1), BIOME]}


def placed_features(version, vw):
    pf = {
        "tarnished_fields_trees": _trees("graceroot_tree", RARITY(5)),
        "tarnished_fields_pillars": _pillars(9),
        "tarnished_fields_gravestones": _patch("gravestone_patch", RARITY(6)),
        "gracewood_trees": _trees("graceroot_tree", COUNT(weighted_int([(6, 3), (8, 1)]))),
        "gracewood_giant_trees": _trees("graceroot_tree_giant", RARITY(5)),
        "gravefield_barrows_gravestones": _patch("gravestone_patch", COUNT(2)),
        "gravefield_barrows_bones": _patch("bone_patch", RARITY(2)),
        "gravefield_barrows_withered_trees": _trees("withered_tree", RARITY(3)),
        "gravefield_barrows_ash": _ash(RARITY(4)),
        "ashen_wastes_boulders": {"feature": rb("eclipse_boulder"),
                                  "placement": [COUNT(1), IN_SQUARE, HEIGHTMAP("MOTION_BLOCKING"), BIOME]},
        "ashen_wastes_ash": _ash(COUNT(2)),
        "ashen_wastes_withered_trees": _trees("withered_tree", RARITY(3)),
        "ashen_wastes_pillars": _pillars(12),
        "ashen_wastes_bones": _patch("bone_patch", RARITY(6)),
        "lantern_heights_boulders": {"feature": rb("amethyst_boulder"),
                                     "placement": [RARITY(3), IN_SQUARE, HEIGHTMAP("MOTION_BLOCKING"), BIOME]},
        "lantern_heights_crystals": {"feature": rb("amethyst_cluster_patch"),
                                     "placement": [RARITY(2), IN_SQUARE, HEIGHTMAP("MOTION_BLOCKING"), BIOME]},
        "lantern_heights_pillars": {"feature": rb("sandstone_pillar"),
                                    "placement": [RARITY(8), IN_SQUARE, NO_WATER, HEIGHTMAP("WORLD_SURFACE_WG"),
                                                  filt(all_of(AIR_HERE, STURDY_BELOW)), BIOME]},
        "lantern_heights_trees": _trees("graceroot_tree", RARITY(12)),
        "ossuary_bones": _cave("bone_patch", 16, "down"),
        "ossuary_cobwebs": _cave("cobweb_patch", 12, "up"),
    }
    pf.update(realm_copies(vw)[1])
    return pf


STEPS = ["raw_generation", "lakes", "local_modifications", "underground_structures", "surface_structures",
         "strongholds", "underground_ores", "underground_decoration", "fluid_springs", "vegetal_decoration",
         "top_layer_modification"]
SKIPPED_ORES = {"minecraft:ore_granite_upper", "minecraft:ore_granite_lower", "minecraft:ore_diorite_upper",
                "minecraft:ore_diorite_lower", "minecraft:ore_andesite_upper", "minecraft:ore_andesite_lower"}


def global_feature_order(vw):
    """One order per generation step; every biome uses a subsequence (no feature order cycles)."""
    ores = [f for f in vw["plains"]["features"][STEPS.index("underground_ores")] if f not in SKIPPED_ORES]
    return {
        "lakes": [rb("lake_lava_underground"), rb("lake_lava_surface")],
        "local_modifications": [mc("amethyst_geode"), rb("ashen_wastes_boulders"), rb("lantern_heights_boulders")],
        "underground_structures": [mc("fossil_upper"), mc("fossil_lower")],
        "underground_ores": ores + [rb("gravefield_barrows_ash"), rb("ashen_wastes_ash")],
        "underground_decoration": [rb("ossuary_bones"), rb("ossuary_cobwebs")],
        "fluid_springs": [rb("spring_water"), rb("spring_lava")],
        "vegetal_decoration": [
            rb("glow_lichen"),
            rb("tarnished_fields_pillars"), rb("ashen_wastes_pillars"), rb("lantern_heights_pillars"),
            rb("lantern_heights_crystals"),
            rb("tarnished_fields_gravestones"), rb("gravefield_barrows_gravestones"),
            rb("gravefield_barrows_bones"), rb("ashen_wastes_bones"),
            rb("gracewood_giant_trees"), rb("gracewood_trees"), rb("tarnished_fields_trees"),
            rb("gravefield_barrows_withered_trees"), rb("ashen_wastes_withered_trees"), rb("lantern_heights_trees"),
            mc("patch_tall_grass_2"), mc("patch_grass_plain"), mc("patch_grass_forest"), mc("patch_grass_taiga_2"),
            mc("patch_grass_badlands"), mc("flower_default"), mc("patch_dead_bush_2"), mc("patch_dead_bush"),
            mc("brown_mushroom_normal"), mc("red_mushroom_normal"), mc("patch_sugar_cane"), mc("seagrass_normal"),
        ],
    }


COMMON_FEATURES = {rb("lake_lava_underground"), mc("amethyst_geode"), rb("spring_water"), rb("spring_lava"),
                   rb("glow_lichen"), "ORES"}


def spawn(t, w, lo, hi):
    return {"type": t, "weight": w, "minCount": lo, "maxCount": hi}


LEGION_COST = {"energy_budget": 0.15, "charge": 0.7}
ELITE_COST = {"energy_budget": 0.12, "charge": 1.0}
RAT_COST = {"energy_budget": 0.2, "charge": 0.5}
COSTS = {ent("hollow_sentinel"): LEGION_COST, ent("hollow_arbalist"): LEGION_COST,
         ent("fallen_standard_bearer"): ELITE_COST, ent("soulbound_knight"): ELITE_COST,
         ent("gravebound_soldier"): LEGION_COST, ent("crypt_lurker"): LEGION_COST, ent("catacomb_rat"): RAT_COST,
         ent("lost_pilgrim"): LEGION_COST, ent("memory_wraith"): LEGION_COST, ent("soul_collector"): ELITE_COST,
         ent("ashbound_crawler"): LEGION_COST, ent("ember_servitor"): LEGION_COST, ent("rusted_guardian"): ELITE_COST,
         ent("shardling"): RAT_COST, ent("ancient_vessel"): ELITE_COST, ent("relic_stalker"): ELITE_COST}

MOOD = {"sound": mc("ambient.cave"), "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0}

BIOMES = {
    FIELDS: dict(
        temperature=0.7, downfall=0.3,
        colors=dict(sky_color=0x5D6A84, fog_color=0xCDB68C, water_color=0x2E5A62, water_fog_color=0x12262B,
                    grass_color=0xB5A866, foliage_color=0xA6994F),
        music=mc("music.overworld.old_growth_taiga"),
        spawners={"monster": [spawn(ent("hollow_sentinel"), 30, 1, 3), spawn(ent("hollow_arbalist"), 18, 1, 2),
                              spawn(ent("fallen_standard_bearer"), 4, 1, 1), spawn(ent("soulbound_knight"), 2, 1, 1),
                              spawn(ent("catacomb_rat"), 8, 2, 4), spawn(ent("shardling"), 6, 2, 3),
                              spawn(ent("ancient_vessel"), 4, 1, 1), spawn(ent("rusted_guardian"), 2, 1, 1)],
                  "creature": [spawn(mc("rabbit"), 4, 2, 3), spawn(mc("sheep"), 6, 2, 4)]},
        features={rb("tarnished_fields_pillars"), rb("tarnished_fields_gravestones"), rb("tarnished_fields_trees"),
                  mc("patch_tall_grass_2"), mc("patch_grass_plain"), mc("flower_default"), mc("brown_mushroom_normal"),
                  mc("red_mushroom_normal"), mc("patch_sugar_cane")}),
    GRACEWOOD: dict(
        temperature=0.6, downfall=0.8,
        colors=dict(sky_color=0x66687E, fog_color=0xD8BD7C, water_color=0x2F5E5E, water_fog_color=0x132A2A,
                    grass_color=0xA3B24A, foliage_color=0x98A83E),
        music=mc("music.overworld.cherry_grove"),
        particle=(GRACE_MOTE, 0.004),
        additions=(mc("block.amethyst_block.chime"), 0.004),
        spawners={"monster": [spawn(ent("catacomb_rat"), 12, 2, 4), spawn(ent("crypt_lurker"), 6, 1, 1),
                              spawn(ent("hollow_sentinel"), 6, 1, 2), spawn(ent("lost_pilgrim"), 10, 1, 2),
                              spawn(ent("shardling"), 12, 3, 4)],
                  "creature": [spawn(mc("sheep"), 8, 2, 4), spawn(mc("rabbit"), 6, 2, 3), spawn(mc("fox"), 3, 2, 4)],
                  "ambient": [spawn(mc("bat"), 4, 8, 8)]},
        features={rb("gracewood_giant_trees"), rb("gracewood_trees"), mc("patch_grass_forest"), mc("patch_tall_grass_2"),
                  mc("flower_default"), mc("brown_mushroom_normal"), mc("red_mushroom_normal"), mc("patch_sugar_cane")}),
    BARROWS: dict(
        temperature=0.4, downfall=0.2,
        colors=dict(sky_color=0x56606F, fog_color=0xAAA894, water_color=0x2A4F58, water_fog_color=0x102228,
                    grass_color=0x7F8B6A, foliage_color=0x707B5C),
        music=mc("music.overworld.swamp"),
        particle=(mc("white_ash"), 0.012),
        additions=(mc("ambient.soul_sand_valley.additions"), 0.0055),
        spawners={"monster": [spawn(ent("gravebound_soldier"), 30, 2, 3), spawn(ent("crypt_lurker"), 20, 1, 2),
                              spawn(ent("catacomb_rat"), 20, 3, 5), spawn(ent("hollow_arbalist"), 6, 1, 1),
                              spawn(ent("relic_stalker"), 5, 1, 1), spawn(ent("ancient_vessel"), 6, 1, 1)]},
        features={rb("gravefield_barrows_ash"), rb("gravefield_barrows_gravestones"), rb("gravefield_barrows_bones"),
                  rb("gravefield_barrows_withered_trees"), mc("patch_grass_taiga_2"), mc("patch_dead_bush"),
                  mc("brown_mushroom_normal"), mc("red_mushroom_normal")}),
    WASTES: dict(
        temperature=1.2, downfall=0.0,
        colors=dict(sky_color=0x6A6562, fog_color=0xBCAE98, water_color=0x34535A, water_fog_color=0x172628,
                    grass_color=0x9C927B, foliage_color=0x8E846F),
        music=mc("music.nether.soul_sand_valley"),
        particle=(mc("white_ash"), 0.06),
        ambient=mc("ambient.soul_sand_valley.loop"),
        additions=(mc("ambient.soul_sand_valley.additions"), 0.0111),
        mood=mc("ambient.soul_sand_valley.mood"),
        spawners={"monster": [spawn(ent("hollow_sentinel"), 20, 1, 2), spawn(ent("gravebound_soldier"), 15, 1, 3),
                              spawn(ent("soulbound_knight"), 8, 1, 1), spawn(ent("crypt_lurker"), 10, 1, 1),
                              spawn(ent("ashbound_crawler"), 25, 2, 4), spawn(ent("ember_servitor"), 12, 1, 2),
                              spawn(ent("rusted_guardian"), 4, 1, 1)]},
        features={rb("lake_lava_surface"), rb("ashen_wastes_boulders"), rb("ashen_wastes_ash"), rb("ashen_wastes_pillars"),
                  rb("ashen_wastes_bones"), rb("ashen_wastes_withered_trees"), mc("patch_grass_badlands"),
                  mc("patch_dead_bush_2")}),
    HEIGHTS: dict(
        temperature=0.5, downfall=0.3,
        colors=dict(sky_color=0x5E5F86, fog_color=0xC8B8C4, water_color=0x3A4F70, water_fog_color=0x141C2C,
                    grass_color=0x9AA88C, foliage_color=0x8D9A7E),
        music=mc("music.overworld.jagged_peaks"),
        additions=(mc("block.amethyst_block.chime"), 0.006),
        spawners={"monster": [spawn(ent("lost_pilgrim"), 30, 1, 3), spawn(ent("memory_wraith"), 12, 1, 2),
                              spawn(ent("soul_collector"), 2, 1, 1)],
                  "creature": [spawn(mc("rabbit"), 4, 1, 3)]},
        features={rb("lantern_heights_boulders"), rb("lantern_heights_crystals"), rb("lantern_heights_pillars"),
                  rb("lantern_heights_trees"), mc("patch_grass_plain"), mc("flower_default")}),
    SEA: dict(
        temperature=0.5, downfall=0.5,
        colors=dict(sky_color=0x50647E, fog_color=0xBAB59C, water_color=0x284C57, water_fog_color=0x0E2026,
                    grass_color=0xADA36A, foliage_color=0xA0975D),
        music=mc("music.overworld.frozen_peaks"),
        spawners={"water_creature": [spawn(mc("squid"), 2, 1, 4)], "water_ambient": [spawn(mc("cod"), 10, 3, 6)]},
        features={mc("seagrass_normal")}),
    OSSUARY: dict(
        temperature=0.5, downfall=0.4,
        colors=dict(sky_color=0x5A6478, fog_color=0x9E9584, water_color=0x2A4A50, water_fog_color=0x0F1F24,
                    grass_color=0x8C8A70, foliage_color=0x7E7C64),
        music=mc("music.overworld.deep_dark"),
        additions=(mc("ambient.soul_sand_valley.additions"), 0.004),
        spawners={"monster": [spawn(ent("crypt_lurker"), 25, 1, 2), spawn(ent("catacomb_rat"), 25, 3, 5),
                              spawn(ent("gravebound_soldier"), 10, 1, 2), spawn(ent("ashbound_crawler"), 10, 1, 3),
                              spawn(ent("relic_stalker"), 6, 1, 1), spawn(ent("ancient_vessel"), 6, 1, 1)],
                  "ambient": [spawn(mc("bat"), 10, 8, 8)]},
        features={mc("fossil_upper"), mc("fossil_lower"), rb("ossuary_bones"), rb("ossuary_cobwebs")}),
}
SPAWN_CATEGORIES = ["monster", "creature", "ambient", "axolotls", "underground_water_creature", "water_creature",
                    "water_ambient", "misc"]
CARVERS = [mc("cave"), mc("cave_extra_underground"), mc("canyon")]


def biome_json(biome, vw):
    spec = BIOMES[biome]
    order = global_feature_order(vw)
    ores = set(order["underground_ores"]) - {rb("gravefield_barrows_ash"), rb("ashen_wastes_ash")}
    wanted = set(spec["features"]) | (COMMON_FEATURES - {"ORES"}) | ores
    features = [[f for f in order.get(step, []) if f in wanted] for step in STEPS]
    effects = dict(spec["colors"])
    effects["mood_sound"] = dict(MOOD, sound=spec.get("mood", MOOD["sound"]))
    if spec.get("ambient"):
        effects["ambient_sound"] = spec["ambient"]
    if spec.get("additions"):
        effects["additions_sound"] = {"sound": spec["additions"][0], "tick_chance": spec["additions"][1]}
    if spec.get("particle"):
        effects["particle"] = {"options": {"type": spec["particle"][0]}, "probability": spec["particle"][1]}
    effects["music"] = {"sound": spec["music"], "min_delay": 12000, "max_delay": 24000, "replace_current_music": False}
    spawners = {c: spec["spawners"].get(c, []) for c in SPAWN_CATEGORIES}
    costs = {}
    for entries in spawners.values():
        for e in entries:
            if e["type"] in COSTS:
                costs[e["type"]] = dict(COSTS[e["type"]])
    return {
        "has_precipitation": False,
        "temperature": spec["temperature"],
        "downfall": spec["downfall"],
        "effects": effects,
        "spawners": spawners,
        "spawn_costs": costs,
        "carvers": {"air": list(CARVERS)},
        "features": features,
    }


STONE_FAMILY = [E, CE, BR, CBR, CH, STAIRS, SLAB, WALL]
BLOCK_TAGS = {
    "mineable/pickaxe": STONE_FAMILY + [WAYGATE, FRAME, GRAVE],
    "mineable/axe": [LOG, PLANKS],
    "mineable/hoe": [LEAVES],
    "leaves": [LEAVES],
    "logs": [LOG],
    "logs_that_burn": [LOG],
    "planks": [PLANKS],
    "stairs": [STAIRS],
    "slabs": [SLAB],
    "walls": [WALL],
    "stone_ore_replaceables": [E],
    "base_stone_overworld": [E],
    "overworld_carver_replaceables": [E],
    "dragon_immune": [EMBER, FOG_GATE, WAYGATE, FRAME, VEIL],
    "wither_immune": [EMBER, FOG_GATE, WAYGATE, FRAME, VEIL],
}
ITEM_TAGS = {
    "logs": [LOG],
    "logs_that_burn": [LOG],
    "planks": [PLANKS],
    "stone_crafting_materials": [CE],
    "stone_tool_materials": [CE],
    "leaves": [LEAVES],
    "stairs": [STAIRS],
    "slabs": [SLAB],
    "walls": [WALL],
}
# The repo keeps both the singular (1.21) and plural (1.20) tag folders in both trees; follow that.
BLOCK_TAG_DIRS = ["block", "blocks"]
ITEM_TAG_DIRS = ["item", "items"]

BIOME_TAGS = {
    "is_eclipsed_realm": SURFACE_LAND + [OSSUARY],
    "in_eclipsed_realm": ALL_BIOMES,
    "has_structure/hollow_citadel": [FIELDS, WASTES],
}


def silk_touch(version):
    if is120(version):
        return {"condition": "minecraft:match_tool",
                "predicate": {"enchantments": [{"enchantment": "minecraft:silk_touch", "levels": {"min": 1}}]}}
    return {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
        {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}


def shears(version):
    return {"condition": "minecraft:match_tool",
            "predicate": {"items": ["minecraft:shears"] if is120(version) else "minecraft:shears"}}


def _table(block, pools):
    return {"type": "minecraft:block", "pools": pools, "random_sequence": f"{NS}:blocks/{block.split(':')[1]}"}


def loot_tables(version):
    survives = [{"condition": "minecraft:survives_explosion"}]

    def drop(block, item=None):
        return _table(block, [{"rolls": 1.0, "bonus_rolls": 0.0, "conditions": survives,
                               "entries": [{"type": "minecraft:item", "name": item or block}]}])

    out = {}
    for b in (CE, BR, CBR, CH, STAIRS, WALL, LOG, PLANKS, GRAVE):
        out[b] = drop(b)
    out[WAYGATE] = drop(WAYGATE, WAYGATE)
    for b in (FRAME, VEIL, EMBER, FOG_GATE):
        out[b] = _table(b, [])
    out[E] = _table(E, [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "name": E, "conditions": [silk_touch(version)]},
        {"type": "minecraft:item", "name": CE, "conditions": survives}]}]}])
    out[SLAB] = _table(SLAB, [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{
        "type": "minecraft:item", "name": SLAB, "functions": [
            {"function": "minecraft:set_count", "add": False, "count": 2.0,
             "conditions": [{"condition": "minecraft:block_state_property", "block": SLAB, "properties": {"type": "double"}}]},
            {"function": "minecraft:explosion_decay"}]}]}])
    out[LEAVES] = _table(LEAVES, [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "name": LEAVES,
         "conditions": [{"condition": "minecraft:any_of", "terms": [shears(version), silk_touch(version)]}]},
        {"type": "minecraft:item", "name": "minecraft:stick",
         "conditions": [{"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune",
                         "chances": [0.02, 0.022222223, 0.025, 0.033333335, 0.1]}],
         "functions": [{"function": "minecraft:set_count", "add": False,
                        "count": {"type": "minecraft:uniform", "min": 1.0, "max": 2.0}},
                       {"function": "minecraft:explosion_decay"}]}]}]}])
    return out


def _result(version, item, count=1):
    if is120(version):
        return {"item": item, "count": count}
    return {"id": item, "count": count}


def _ing(i):
    return {"tag": i[1:]} if i.startswith("#") else {"item": i}


def shaped(version, pattern, key, item, count=1, category="building", group=None):
    r = {"type": "minecraft:crafting_shaped", "category": category, "pattern": pattern,
         "key": {k: _ing(v) for k, v in key.items()}, "result": _result(version, item, count)}
    if group:
        r["group"] = group
    return r


def shapeless(version, ingredients, item, count=1, category="misc", group=None):
    r = {"type": "minecraft:crafting_shapeless", "category": category,
         "ingredients": [_ing(i) for i in ingredients], "result": _result(version, item, count)}
    if group:
        r["group"] = group
    return r


def smelting(version, ingredient, item, xp=0.1):
    return {"type": "minecraft:smelting", "category": "blocks", "cookingtime": 200, "experience": xp,
            "ingredient": _ing(ingredient), "result": item if is120(version) else {"id": item}}


def stonecutting(version, ingredient, item, count=1):
    if is120(version):
        return {"type": "minecraft:stonecutting", "ingredient": _ing(ingredient), "result": item, "count": count}
    return {"type": "minecraft:stonecutting", "ingredient": _ing(ingredient), "result": {"id": item, "count": count}}


def recipes(version):
    v = version
    r = {
        "eclipse_shard": shapeless(v, [mc("amethyst_shard"), mc("ender_pearl"), mc("gold_ingot")], SHARD),
        "eclipse_waygate": shaped(v, ["DSD", "D D", "DGD"], {"D": mc("polished_deepslate"), "S": SHARD, "G": mc("gold_ingot")},
                                  WAYGATE, category="misc"),
        "eclipse_stone_from_smelting": smelting(v, CE, E),
        "eclipse_stone_bricks": shaped(v, ["##", "##"], {"#": E}, BR, 4),
        "cracked_eclipse_stone_bricks": smelting(v, BR, CBR),
        "chiseled_eclipse_stone_bricks": shaped(v, ["#", "#"], {"#": SLAB}, CH),
        "eclipse_stone_brick_stairs": shaped(v, ["#  ", "## ", "###"], {"#": BR}, STAIRS, 4),
        "eclipse_stone_brick_slab": shaped(v, ["###"], {"#": BR}, SLAB, 6),
        "eclipse_stone_brick_wall": shaped(v, ["###", "###"], {"#": BR}, WALL, 6, category="misc"),
        "graceroot_planks": shapeless(v, [LOG], PLANKS, 4, category="building", group="planks"),
    }
    for src_name, src in (("eclipse_stone", E), ("eclipse_stone_bricks", BR)):
        r[f"eclipse_stone_brick_stairs_from_{src_name}_stonecutting"] = stonecutting(v, src, STAIRS)
        r[f"eclipse_stone_brick_slab_from_{src_name}_stonecutting"] = stonecutting(v, src, SLAB, 2)
        r[f"eclipse_stone_brick_wall_from_{src_name}_stonecutting"] = stonecutting(v, src, WALL)
        r[f"chiseled_eclipse_stone_bricks_from_{src_name}_stonecutting"] = stonecutting(v, src, CH)
    r["eclipse_stone_bricks_from_eclipse_stone_stonecutting"] = stonecutting(v, E, BR)
    return r


class Writer:
    def __init__(self, dry_run):
        self.dry_run = dry_run
        self.new = self.changed = self.same = 0
        self.written: list[Path] = []

    def json(self, path: Path, obj):
        text = json.dumps(obj, indent=2) + "\n"
        old = path.read_text(encoding="utf-8") if path.exists() else None
        if old == text:
            self.same += 1
            return
        if old is None:
            self.new += 1
        else:
            self.changed += 1
        self.written.append(path)
        if not self.dry_run:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding="utf-8")

    def tag(self, path: Path, values):
        """Merge values into a tag file (re-read right before writing; other tools may share it)."""
        cur = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
        existing = list(cur.get("values", []))
        merged = existing + [v for v in values if v not in existing]
        self.json(path, {"replace": bool(cur.get("replace", False)), "values": merged})


def emit(version, w: Writer):
    vw = vanilla_worldgen(version)
    root = res_data(version)
    ns = root / NS
    info = VERSIONS[version]
    w.json(ns / "dimension_type" / "eclipsed_realm.json", dimension_type(version))
    w.json(ns / "dimension" / "eclipsed_realm.json", dimension())
    w.json(ns / "worldgen" / "noise_settings" / "eclipsed_realm.json", noise_settings(version, vw))
    for biome in ALL_BIOMES:
        w.json(ns / "worldgen" / "biome" / f"{biome.split(':')[1]}.json", biome_json(biome, vw))
    for name, obj in configured_features(version, vw).items():
        w.json(ns / "worldgen" / "configured_feature" / f"{name}.json", obj)
    for name, obj in placed_features(version, vw).items():
        w.json(ns / "worldgen" / "placed_feature" / f"{name}.json", obj)
    for name, values in BIOME_TAGS.items():
        w.json(ns / "tags" / "worldgen" / "biome" / f"{name}.json", {"replace": False, "values": values})
    for folder in BLOCK_TAG_DIRS:
        for name, values in BLOCK_TAGS.items():
            w.tag(root / "minecraft" / "tags" / folder / f"{name}.json", values)
    for folder in ITEM_TAG_DIRS:
        for name, values in ITEM_TAGS.items():
            w.tag(root / "minecraft" / "tags" / folder / f"{name}.json", values)
    for block, table in loot_tables(version).items():
        w.json(ns / info["loot"] / "blocks" / f"{block.split(':')[1]}.json", table)
    recipe_dir = "recipes" if is120(version) else "recipe"
    for name, obj in recipes(version).items():
        w.json(ns / recipe_dir / f"{name}.json", obj)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--version", choices=list(VERSIONS), action="append", help="limit to a version (repeatable)")
    ap.add_argument("--dry-run", action="store_true", help="report what would change, write nothing")
    args = ap.parse_args()
    for version in args.version or list(VERSIONS):
        w = Writer(args.dry_run)
        emit(version, w)
        verb = "would write" if args.dry_run else "wrote"
        for p in w.written:
            print(f"[{version}] {verb} {p.relative_to(REPO).as_posix()}")
        print(f"[{version}] {w.new} new, {w.changed} changed, {w.same} unchanged")


if __name__ == "__main__":
    main()
