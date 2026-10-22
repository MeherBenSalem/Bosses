"""Shared helpers for the Remnants datapack structure tools.

Covers: version layout, vanilla block/item/biome reports (built once from the Minecraft
jars in the Gradle cache), block-state strings, structure-template NBT IO, rotation,
a vanilla-faithful jigsaw assembler and a processor-list simulator, plus the generator
plumbing shared by every structure (block plan, seeded noise, wall/bars connection states,
block-entity NBT, cutting a plan into jigsaw pieces, staging). Used by the structure generators,
validate.py, preview.py and install.py.
"""
from __future__ import annotations

import gzip
import io
import json
import math
import re
import shutil
import subprocess
import tempfile
import zipfile
from dataclasses import dataclass, field
from pathlib import Path

import nbtlib
from nbtlib import Compound, Double, Int, List, String

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
OUT = HERE / "out"
NS = "remnant_bosses"
ENTITY_NS = "remnants"

VERSIONS = {
    "1.20.1": {"data_version": 3465, "structures": "structures", "loot": "loot_tables", "block_tags": "blocks"},
    "1.21.1": {"data_version": 3955, "structures": "structure", "loot": "loot_table", "block_tags": "block"},
}

# Mod blocks the templates may use; properties come from their blockstate JSON.
MOD_BLOCKS = {
    f"{NS}:ancient_altar": {"properties": {"facing": ["north", "south", "west", "east"]}, "default": {"facing": "north"}},
    f"{NS}:ancient_pedestal": {"properties": {"facing": ["north", "south", "west", "east"]}, "default": {"facing": "north"}},
}

# Eclipsed Realm blocks (stairs/slab/wall/leaves/log use the vanilla properties).
_H4 = ["north", "south", "west", "east"]
_TF = ["true", "false"]
_WALL_SIDE = ["none", "low", "tall"]
for _n in ("eclipse_stone", "cobbled_eclipse_stone", "eclipse_stone_bricks", "cracked_eclipse_stone_bricks",
           "chiseled_eclipse_stone_bricks", "graceroot_planks", "grace_ember", "waygate_frame", "fog_gate"):
    MOD_BLOCKS[f"{NS}:{_n}"] = {"properties": {}, "default": {}}
MOD_BLOCKS.update({
    f"{NS}:eclipse_stone_brick_stairs": {
        "properties": {"facing": _H4, "half": ["top", "bottom"],
                       "shape": ["straight", "inner_left", "inner_right", "outer_left", "outer_right"], "waterlogged": _TF},
        "default": {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"}},
    f"{NS}:eclipse_stone_brick_slab": {"properties": {"type": ["top", "bottom", "double"], "waterlogged": _TF},
                                       "default": {"type": "bottom", "waterlogged": "false"}},
    f"{NS}:eclipse_stone_brick_wall": {
        "properties": {"east": _WALL_SIDE, "north": _WALL_SIDE, "south": _WALL_SIDE, "west": _WALL_SIDE, "up": _TF,
                       "waterlogged": _TF},
        "default": {"east": "none", "north": "none", "south": "none", "west": "none", "up": "true", "waterlogged": "false"}},
    f"{NS}:graceroot_log": {"properties": {"axis": ["x", "y", "z"]}, "default": {"axis": "y"}},
    f"{NS}:gilded_leaves": {"properties": {"distance": [str(i) for i in range(1, 8)], "persistent": _TF, "waterlogged": _TF},
                            "default": {"distance": "7", "persistent": "false", "waterlogged": "false"}},
    f"{NS}:weathered_gravestone": {"properties": {"facing": _H4}, "default": {"facing": "north"}},
    f"{NS}:eclipse_waygate": {"properties": {"facing": _H4, "phase": ["dormant", "kindling", "open"]},
                              "default": {"facing": "north", "phase": "dormant"}},
    f"{NS}:waygate_veil": {"properties": {"open": _TF}, "default": {"open": "false"}},
})

AIR = "minecraft:air"
EMPTY_POOL = "minecraft:empty"

# Structures this toolchain generates (name -> generator script); validate/preview/install take one of these.
STRUCTURES = {
    "hollow_citadel": "hollow_citadel.py",
    "gravebound_catacombs": "gravebound_catacombs.py",
    "grace_shrine": "grace_shrine.py",
    "arrival_shrine": "arrival_shrine.py",
    "forgotten_observatory": "forgotten_observatory.py",
}


def staged_root(version: str) -> Path:
    return OUT / version


def data_dir(version: str) -> Path:
    return staged_root(version) / "data" / NS


def structure_files(version: str, name: str) -> list[Path]:
    """Every staged file that belongs to one structure (all its paths contain the structure name)."""
    root = staged_root(version) / "data"
    if not root.exists():
        return []
    return sorted(p for p in root.rglob("*") if p.is_file() and name in p.relative_to(root).as_posix())


def clean_staged(version: str, name: str) -> None:
    """Remove one structure's staged files (other structures staged next to it are kept)."""
    for p in structure_files(version, name):
        p.unlink()
    root = data_dir(version)
    if root.exists():
        for d in sorted((q for q in root.rglob("*") if q.is_dir()), key=lambda q: -len(q.parts)):
            if not any(d.iterdir()):
                d.rmdir()


def _find_jar(version: str, kind: str) -> Path | None:
    base = Path.home() / ".gradle" / "caches"
    direct = base / "neoformruntime" / "artifacts" / f"minecraft_{version}_{kind}.jar"
    if direct.exists():
        return direct
    for p in base.glob(f"**/minecraft*{version}*{kind}*.jar"):
        if "sources" not in p.name and "mapping" not in p.name:
            return p
    return None


def vanilla(version: str) -> dict:
    """Block properties, items, entity types, biomes and block tags for a Minecraft version.

    Built once by running the vanilla data generator (``--reports``) from the server jar in
    the Gradle cache, then cached in ``out/.vanilla/<version>.json``. Returns ``{}`` when
    neither the cache nor a jar + Java is available (callers fall back to syntax checks).
    """
    cache = OUT / ".vanilla" / f"{version}.json"
    if cache.exists():
        return json.loads(cache.read_text(encoding="utf-8"))
    server, client = _find_jar(version, "server"), _find_jar(version, "client")
    java = shutil.which("java")
    if not server or not client or not java:
        return {}
    data: dict = {}
    with tempfile.TemporaryDirectory() as td:
        subprocess.run([java, "-DbundlerMainClass=net.minecraft.data.Main", "-jar", str(server),
                        "--reports", "--output", str(Path(td) / "gen")],
                       cwd=td, capture_output=True, timeout=900, check=True)
        reports = Path(td) / "gen" / "reports"
        blocks = json.loads((reports / "blocks.json").read_text(encoding="utf-8"))
        regs = json.loads((reports / "registries.json").read_text(encoding="utf-8"))
    data["blocks"] = {}
    for name, info in blocks.items():
        default = next((s.get("properties", {}) for s in info["states"] if s.get("default")), {})
        data["blocks"][name] = {"properties": info.get("properties", {}), "default": default}
    data["items"] = sorted(regs["minecraft:item"]["entries"])
    data["entity_types"] = sorted(regs["minecraft:entity_type"]["entries"])
    with zipfile.ZipFile(client) as z:
        names = z.namelist()
    data["biomes"] = sorted("minecraft:" + Path(n).stem for n in names
                            if n.startswith("data/minecraft/worldgen/biome/") and n.endswith(".json"))
    tag_dir = f"data/minecraft/tags/{VERSIONS[version]['block_tags']}/"
    data["block_tags"] = sorted("minecraft:" + n[len(tag_dir):-5] for n in names
                                if n.startswith(tag_dir) and n.endswith(".json"))
    cache.parent.mkdir(parents=True, exist_ok=True)
    cache.write_text(json.dumps(data, separators=(",", ":")), encoding="utf-8")
    return data


def block_table(version: str) -> dict:
    table = dict(vanilla(version).get("blocks", {}))
    table.update(MOD_BLOCKS)
    return table


_STATE_RE = re.compile(r"^([a-z0-9_.-]+:[a-z0-9_/.-]+)(?:\[([a-z0-9_=,]*)\])?$")


def parse_state(s: str) -> tuple[str, dict]:
    m = _STATE_RE.match(s)
    if not m:
        raise ValueError(f"bad block state {s!r}")
    props = {}
    if m.group(2):
        for kv in m.group(2).split(","):
            k, v = kv.split("=")
            props[k] = v
    return m.group(1), props


def fmt_state(name: str, props: dict | None = None) -> str:
    if not props:
        return name
    return name + "[" + ",".join(f"{k}={props[k]}" for k in sorted(props)) + "]"


def full_state(s: str, table: dict) -> str:
    """Complete a state string with the block's default properties (vanilla templates store all)."""
    name, props = parse_state(s)
    info = table.get(name)
    if not info:
        return fmt_state(name, props)
    full = dict(info["default"])
    full.update(props)
    return fmt_state(name, full)


def state_name(s: str) -> str:
    return s.split("[", 1)[0]


HORIZ = ["north", "east", "south", "west"]  # clockwise
DIRV = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0),
        "up": (0, 1, 0), "down": (0, -1, 0)}
OPP = {"north": "south", "south": "north", "east": "west", "west": "east", "up": "down", "down": "up"}


def rot_dir(d: str, r: int) -> str:
    if d in HORIZ:
        return HORIZ[(HORIZ.index(d) + r) % 4]
    return d


def rot_pos(p, r: int):
    """Vanilla StructureTemplate.transform around pivot 0 (r = clockwise quarter turns)."""
    x, y, z = p
    r %= 4
    if r == 0:
        return (x, y, z)
    if r == 1:
        return (-z, y, x)
    if r == 2:
        return (-x, y, -z)
    return (z, y, -x)


def rot_state(s: str, r: int) -> str:
    r %= 4
    if r == 0:
        return s
    name, props = parse_state(s)
    if not props:
        return s
    out = dict(props)
    if props.get("facing") in HORIZ:
        out["facing"] = rot_dir(props["facing"], r)
    if "axis" in props and r % 2 == 1 and props["axis"] in ("x", "z"):
        out["axis"] = "z" if props["axis"] == "x" else "x"
    if "orientation" in props:  # jigsaw FrontAndTop
        front, top = props["orientation"].split("_")
        out["orientation"] = f"{rot_dir(front, r)}_{rot_dir(top, r)}"
    if "rotation" in props and props["rotation"].isdigit():
        out["rotation"] = str((int(props["rotation"]) + 4 * r) % 16)
    sides = [d for d in HORIZ if d in props]
    if len(sides) == 4:
        for d in HORIZ:
            out[rot_dir(d, r)] = props[d]
    return fmt_state(name, out)


def mirror_x_state(s: str) -> str:
    """Mirror east<->west (used by the generator to build left/right twins)."""
    name, props = parse_state(s)
    if not props:
        return s
    out = dict(props)
    sw = {"east": "west", "west": "east"}
    if props.get("facing") in sw:
        out["facing"] = sw[props["facing"]]
    if "east" in props and "west" in props:
        out["east"], out["west"] = props["west"], props["east"]
    if "shape" in props:
        out["shape"] = {"inner_left": "inner_right", "inner_right": "inner_left",
                        "outer_left": "outer_right", "outer_right": "outer_left"}.get(props["shape"], props["shape"])
    if "rotation" in props and props["rotation"].isdigit():
        out["rotation"] = str((16 - int(props["rotation"])) % 16)
    if "orientation" in props:
        front, top = props["orientation"].split("_")
        out["orientation"] = f"{sw.get(front, front)}_{sw.get(top, top)}"
    return fmt_state(name, out)


@dataclass
class Template:
    size: tuple
    blocks: dict = field(default_factory=dict)      # (x,y,z) -> (state, nbt Compound | None)
    entities: list = field(default_factory=list)    # dicts with pos, blockPos, nbt

    def jigsaws(self):
        for p, (s, nbt) in sorted(self.blocks.items()):
            if state_name(s) == "minecraft:jigsaw":
                yield p, s, nbt


def save_template(path: Path, tpl: Template, data_version: int, table: dict) -> None:
    palette: list[str] = []
    index: dict[str, int] = {}
    blocks = []
    for p in sorted(tpl.blocks, key=lambda q: (q[1], q[2], q[0])):
        s, nbt = tpl.blocks[p]
        s = full_state(s, table)
        if s not in index:
            index[s] = len(palette)
            palette.append(s)
        entry = {"pos": List[Int]([Int(c) for c in p]), "state": Int(index[s])}
        if nbt is not None:
            entry["nbt"] = nbt
        blocks.append(Compound(entry))
    pal = []
    for s in palette:
        name, props = parse_state(s)
        c = {"Name": String(name)}
        if props:
            c["Properties"] = Compound({k: String(v) for k, v in props.items()})
        pal.append(Compound(c))
    ents = []
    for e in tpl.entities:
        ents.append(Compound({
            "pos": List[Double]([Double(c) for c in e["pos"]]),
            "blockPos": List[Int]([Int(math.floor(c)) for c in e["pos"]]),
            "nbt": e["nbt"],
        }))
    root = Compound({
        "DataVersion": Int(data_version),
        "size": List[Int]([Int(c) for c in tpl.size]),
        "palette": List[Compound](pal),
        "blocks": List[Compound](blocks),
        "entities": List[Compound](ents),
    })
    path.parent.mkdir(parents=True, exist_ok=True)
    buf = io.BytesIO()
    nbtlib.File(root).write(buf)
    path.write_bytes(gzip.compress(buf.getvalue(), compresslevel=9, mtime=0))  # byte-stable output


def load_template(path: Path) -> tuple[Template, Compound]:
    root = nbtlib.load(str(path))
    pal_tag = root["palette"] if "palette" in root else root["palettes"][0]
    palette = []
    for c in pal_tag:
        props = {k: str(v) for k, v in c.get("Properties", {}).items()}
        palette.append(fmt_state(str(c["Name"]), props))
    tpl = Template(size=tuple(int(v) for v in root["size"]))
    for b in root["blocks"]:
        p = tuple(int(v) for v in b["pos"])
        tpl.blocks[p] = (palette[int(b["state"])], b.get("nbt"))
    for e in root.get("entities", []):
        tpl.entities.append({"pos": tuple(float(v) for v in e["pos"]),
                             "blockPos": tuple(int(v) for v in e["blockPos"]), "nbt": e["nbt"]})
    return tpl, root


def template_path(version: str, location: str) -> Path:
    ns, path = location.split(":", 1)
    return staged_root(version) / "data" / ns / VERSIONS[version]["structures"] / f"{path}.nbt"


def resource_json(version: str, kind: str, rid: str) -> Path:
    ns, path = rid.split(":", 1)
    return staged_root(version) / "data" / ns / kind / f"{path}.json"


@dataclass
class Placed:
    location: str
    pool: str
    processors: str | dict
    origin: tuple
    rotation: int
    box: tuple          # x0,y0,z0,x1,y1,z1 inclusive
    depth: int
    template: Template
    parent: int | None = None


def _box(origin, size, r):
    corners = [rot_pos((x, y, z), r) for x in (0, size[0] - 1) for y in (0, size[1] - 1) for z in (0, size[2] - 1)]
    xs = [origin[0] + c[0] for c in corners]
    ys = [origin[1] + c[1] for c in corners]
    zs = [origin[2] + c[2] for c in corners]
    return (min(xs), min(ys), min(zs), max(xs), max(ys), max(zs))


def _inside(p, b):
    return b[0] <= p[0] <= b[3] and b[1] <= p[1] <= b[4] and b[2] <= p[2] <= b[5]


def _box_in(inner, outer):
    return all(outer[i] <= inner[i] for i in range(3)) and all(inner[i] <= outer[i] for i in range(3, 6))


def _box_hit(a, b):
    return not (a[3] < b[0] or b[3] < a[0] or a[4] < b[1] or b[4] < a[1] or a[5] < b[2] or b[5] < a[2])


def jigsaw_front_top(state: str):
    _, props = parse_state(state)
    front, top = props["orientation"].split("_")
    return front, top


def can_attach(parent_state, parent_nbt, child_state, child_nbt) -> bool:
    """JigsawBlock.canAttach for already rotated states."""
    f1, t1 = jigsaw_front_top(parent_state)
    f2, t2 = jigsaw_front_top(child_state)
    joint = str(parent_nbt.get("joint", "aligned" if f1 in HORIZ else "rollable"))
    if joint not in ("aligned", "rollable"):
        joint = "aligned" if f1 in HORIZ else "rollable"
    rollable = joint == "rollable"
    return f1 == OPP[f2] and (rollable or t1 == t2) and str(parent_nbt["target"]) == str(child_nbt["name"])


def load_pool(version: str, pool: str) -> dict:
    p = resource_json(version, "worldgen/template_pool", pool)
    return json.loads(p.read_text(encoding="utf-8"))


def assemble(version: str, structure_id: str = f"{NS}:hollow_citadel", start_rotation: int = 0,
             templates: dict | None = None) -> list[Placed]:
    """Deterministic re-implementation of JigsawPlacement.addPieces for rigid single elements.

    Deviations from vanilla, all made for determinism: start element = first pool element,
    jigsaws iterated in position order, child rotations tried in the order 0..3 and pool
    elements in file order (vanilla shuffles all of these with the world seed).
    """
    sj = json.loads(resource_json(version, "worldgen/structure", structure_id).read_text(encoding="utf-8"))
    max_depth = int(sj["size"])
    max_dist = sj["max_distance_from_center"]
    if isinstance(max_dist, dict):
        max_dist_h, max_dist_v = int(max_dist["horizontal"]), int(max_dist.get("vertical", max_dist["horizontal"]))
    else:
        max_dist_h = max_dist_v = int(max_dist)
    templates = templates if templates is not None else {}

    def tpl(loc):
        if loc not in templates:
            templates[loc] = load_template(template_path(version, loc))[0]
        return templates[loc]

    start_pool = sj["start_pool"]
    el = load_pool(version, start_pool)["elements"][0]["element"]
    t0 = tpl(el["location"])
    start = Placed(el["location"], start_pool, el.get("processors"), (0, 0, 0), start_rotation % 4,
                   _box((0, 0, 0), t0.size, start_rotation), 0, t0)
    placed = [start]
    cx = (start.box[0] + start.box[3]) // 2
    cz = (start.box[2] + start.box[5]) // 2
    ground = start.box[1] + 1
    limit = (cx - max_dist_h, ground - max_dist_v, cz - max_dist_h,
             cx + max_dist_h, ground + max_dist_v, cz + max_dist_h)
    internal: dict[int, list] = {}
    queue = [0]
    while queue:
        pi = queue.pop(0)
        par = placed[pi]
        for lp, s, nbt in par.template.jigsaws():
            pool = str(nbt.get("pool", EMPTY_POOL))
            if pool == EMPTY_POOL:
                continue
            ws = rot_state(s, par.rotation)
            wp = tuple(par.origin[i] + rot_pos(lp, par.rotation)[i] for i in range(3))
            front, _ = jigsaw_front_top(ws)
            tgt = tuple(wp[i] + DIRV[front][i] for i in range(3))
            is_internal = _inside(tgt, par.box)
            if par.depth >= max_depth:
                continue  # vanilla only tries the fallback pool here (minecraft:empty)
            pj = load_pool(version, pool)
            done = False
            for entry in pj["elements"]:
                elem = entry["element"]
                if elem.get("element_type") not in ("minecraft:single_pool_element", "minecraft:legacy_single_pool_element"):
                    continue
                ct = tpl(elem["location"])
                for r in range(4):
                    for clp, cs, cnbt in ct.jigsaws():
                        rcs = rot_state(cs, r)
                        if not can_attach(ws, nbt, rcs, cnbt):
                            continue
                        rel = rot_pos(clp, r)
                        origin = tuple(tgt[i] - rel[i] for i in range(3))
                        box = _box(origin, ct.size, r)
                        if is_internal:
                            ok = _box_in(box, par.box) and not any(_box_hit(box, b) for b in internal.get(pi, []))
                        else:
                            ok = _box_in(box, limit) and not any(_box_hit(box, q.box) for q in placed)
                        if not ok:
                            continue
                        child = Placed(elem["location"], pool, elem.get("processors"), origin, r, box,
                                       par.depth + 1, ct, pi)
                        placed.append(child)
                        if is_internal:
                            internal.setdefault(pi, []).append(box)
                        if child.depth <= max_depth:
                            queue.append(len(placed) - 1)
                        done = True
                        break
                    if done:
                        break
                if done:
                    break
    return placed


def _hrand(p, salt=0):
    x, y, z = p
    n = (x * 73856093) ^ (y * 19349663) ^ (z * 83492791) ^ (salt * 2654435761)
    n &= 0xFFFFFFFFFFFF
    n = (n ^ (n >> 17)) * 0x5BD1E995 & 0xFFFFFFFFFFFF
    n ^= n >> 15
    return (n & 0xFFFFFF) / float(0x1000000)


class _Rand:
    def __init__(self, p):
        self.p, self.i = p, 0

    def next(self):
        self.i += 1
        return _hrand(self.p, self.i)


def _state_from_json(obj, table) -> str:
    return full_state(fmt_state(obj["Name"], obj.get("Properties")), table)


def _test_block(pred, state, rnd, table) -> bool:
    t = pred["predicate_type"]
    if t == "minecraft:always_true":
        return True
    if t == "minecraft:block_match":
        return state_name(state) == pred["block"]
    if t == "minecraft:random_block_match":
        return state_name(state) == pred["block"] and rnd.next() < float(pred["probability"])
    if t == "minecraft:blockstate_match":
        return full_state(state, table) == _state_from_json(pred["block_state"], table)
    if t == "minecraft:random_blockstate_match":
        return full_state(state, table) == _state_from_json(pred["block_state"], table) and rnd.next() < float(pred["probability"])
    raise ValueError(f"unsupported rule test {t}")


def _test_pos(pred, world_pos, pivot, rnd) -> bool:
    if not pred or pred["predicate_type"] == "minecraft:always_true":
        return True
    if pred["predicate_type"] == "minecraft:axis_aligned_linear_pos":
        axis = pred.get("axis", "y")
        i = {"x": 0, "y": 1, "z": 2}[axis]
        d = abs(world_pos[i] - pivot[i])
        lo, hi = int(pred.get("min_dist", 0)), int(pred.get("max_dist", 0))
        cmin, cmax = float(pred.get("min_chance", 0)), float(pred.get("max_chance", 0))
        t = 0.0 if hi == lo else (d - lo) / (hi - lo)
        t = max(0.0, min(1.0, t))
        return rnd.next() <= cmin + (cmax - cmin) * t
    raise ValueError(f"unsupported pos test {pred['predicate_type']}")


def load_processors(version: str, ref) -> list:
    if ref is None:
        return []
    if isinstance(ref, dict):
        return ref.get("processors", [])
    p = resource_json(version, "worldgen/processor_list", ref)
    return json.loads(p.read_text(encoding="utf-8"))["processors"]


def run_processors(procs, state, world, world_pos, pivot, table, weathering=True):
    """Apply a processor list to one (unrotated) template state; returns the new state or None."""
    rnd = _Rand(world_pos)
    for proc in procs:
        kind = proc["processor_type"]
        if kind == "minecraft:rule":
            existing = world.get(world_pos, AIR)
            for rule in proc["rules"]:
                if not weathering and rule["location_predicate"]["predicate_type"] == "minecraft:always_true":
                    continue  # previews without weathering skip the random rules
                if (_test_block(rule["input_predicate"], state, rnd, table)
                        and _test_block(rule["location_predicate"], existing, rnd, table)
                        and _test_pos(rule.get("position_predicate"), world_pos, pivot, rnd)):
                    state = _state_from_json(rule["output_state"], table)
                    break
        elif kind == "minecraft:block_rot":
            if weathering and rnd.next() > float(proc.get("integrity", 1.0)):
                return None
        elif kind in ("minecraft:protected_blocks", "minecraft:nop"):
            continue
        else:
            raise ValueError(f"unsupported processor {kind}")
    return state


def build_world(version: str, placed: list[Placed], weathering: bool = False) -> tuple[dict, dict]:
    """Place assembled pieces in order like StructureStart.placeInChunk.

    Returns (world, owner): world maps position -> rotated state, owner maps position -> piece index.
    Jigsaw blocks become their final_state; pool processors run per block with the world as
    placed so far (so location predicates behave as in game).
    """
    table = block_table(version)
    world: dict = {}
    owner: dict = {}
    start = placed[0]
    pivot = ((start.box[0] + start.box[3]) // 2, start.box[1], (start.box[2] + start.box[5]) // 2)
    for idx, pc in enumerate(placed):
        procs = load_processors(version, pc.processors)
        staged = []
        for lp, (s, nbt) in pc.template.blocks.items():
            wp = tuple(pc.origin[i] + rot_pos(lp, pc.rotation)[i] for i in range(3))
            if state_name(s) == "minecraft:jigsaw":
                s = str(nbt.get("final_state", AIR)) if nbt is not None else AIR
                if state_name(s) == "minecraft:structure_void":
                    continue
            if state_name(s) == "minecraft:structure_void":
                continue
            s2 = run_processors(procs, s, world, wp, pivot, table, weathering)
            if s2 is None:
                continue
            staged.append((wp, rot_state(s2, pc.rotation)))
        for wp, s in staged:  # processBlockInfos runs before placement within one piece
            world[wp] = s
            owner[wp] = idx
    return world, owner


NON_SOLID_HINTS = ("slab", "stairs", "_wall", "fence", "bars", "pane", "carpet", "lantern", "chain", "banner",
                   "air", "cobweb", "candle", "chest", "anvil", "_bed", "button", "trapdoor", "door", "torch", "vine",
                   "ladder", "skull", "head", "campfire", "grindstone", "lectern", "decorated_pot", "rail", "sign",
                   "water", "lava", "pressure_plate", "ancient_altar", "ancient_pedestal", "flower_pot", "lever",
                   "jigsaw_never")


def is_full_solid(s: str | None) -> bool:
    if s is None:
        return False
    n = state_name(s)
    if n.endswith("_wall") or n.endswith("_wall_banner"):
        return False
    return not any(h in n for h in NON_SOLID_HINTS)


def is_passable(s: str | None) -> bool:
    """Cells a 2.5-block-tall mob can occupy (no collision box). Fog gates count: players walk through them,
    and the reachability checks are about challengers reaching the arena (bosses stay behind them in game)."""
    if s is None:
        return True
    n = state_name(s)
    return (n in ("minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:moss_carpet",
                  "minecraft:vine", "minecraft:glow_lichen", "minecraft:light", f"{NS}:fog_gate")
            or n.endswith("_carpet") or n.endswith("_banner") or n.endswith("_wall_banner")
            or n.endswith("_pressure_plate"))


WEB = "minecraft:cobweb"
BARS = "minecraft:iron_bars"


def make_noise(seed: int):
    """Seeded integer hash h3(x, y, z, salt) in [0, 1) and smooth 2D value noise vnoise(x, z, scale, salt)."""
    def h3(x, y, z, salt=0):
        n = (x * 73856093) ^ (y * 19349663) ^ (z * 83492791) ^ ((salt + seed) * 2654435761)
        n &= 0xFFFFFFFFFFFF
        n = (n ^ (n >> 17)) * 0x5BD1E995 & 0xFFFFFFFFFFFF
        n ^= n >> 15
        return (n & 0xFFFFFF) / float(0x1000000)

    def vnoise(x, z, scale, salt=0):
        fx, fz = x / scale, z / scale
        x0, z0 = math.floor(fx), math.floor(fz)
        tx, tz = fx - x0, fz - z0
        tx, tz = tx * tx * (3 - 2 * tx), tz * tz * (3 - 2 * tz)
        a, b = h3(x0, 0, z0, salt), h3(x0 + 1, 0, z0, salt)
        c, d = h3(x0, 0, z0 + 1, salt), h3(x0 + 1, 0, z0 + 1, salt)
        return (a * (1 - tx) + b * tx) * (1 - tz) + (c * (1 - tx) + d * tx) * tz

    return h3, vnoise


def pick(weighted, r):
    total = sum(w for _, w in weighted)
    t = r * total
    for v, w in weighted:
        t -= w
        if t <= 0:
            return v
    return weighted[-1][0]


class Plan:
    """Global block plan of a structure in the assembled frame (start rotation 0)."""

    def __init__(self):
        self.b: dict = {}          # (x,y,z) -> state ; AIR is stored explicitly (clears terrain)
        self.be: dict = {}         # (x,y,z) -> block entity spec
        self.ents: list = []       # entity specs in plan coordinates
        self.locked: set = set()
        self.clear_zones: list = []  # boxes where decoration/rubble may not add solids
        self.passages: list = []     # (name, min_headroom, [(x,y,z) standing cells])
        self.boxes: dict = {}

    def put(self, x, y, z, s, be=None, force=False):
        p = (x, y, z)
        if p in self.locked and not force:
            return
        if s is None:
            self.b.pop(p, None)
            self.be.pop(p, None)
            return
        self.b[p] = s
        if be is not None:
            self.be[p] = be
        else:
            self.be.pop(p, None)

    def get(self, x, y, z):
        return self.b.get((x, y, z))

    def fill(self, x0, y0, z0, x1, y1, z1, s):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.put(x, y, z, s(x, y, z) if callable(s) else s)

    def air(self, x0, y0, z0, x1, y1, z1):
        self.fill(x0, y0, z0, x1, y1, z1, AIR)

    def lock(self, *ps):
        self.locked.update(ps)

    def is_empty(self, x, y, z):
        s = self.b.get((x, y, z))
        return s is None or s == AIR

    def in_box(self, x, y, z):
        return any(b[0] <= x <= b[3] and b[1] <= y <= b[4] and b[2] <= z <= b[5] for b in self.boxes.values())

    def in_clear(self, x, y, z):
        return any(b[0] <= x <= b[3] and b[1] <= y <= b[4] and b[2] <= z <= b[5] for b in self.clear_zones)

    def ground(self, x, z, top):
        """First standing y at column (x, z) scanning down from top (carpets/air are skipped)."""
        for y in range(top, -1, -1):
            s = self.b.get((x, y, z))
            if s is None or s == AIR or state_name(s).endswith("carpet") or s == WEB:
                continue
            return y + 1
        return 1

    def passage(self, name, cells, headroom=3):
        self.passages.append((name, headroom, sorted(set(cells))))
        xs = [c[0] for c in cells]
        ys = [c[1] for c in cells]
        zs = [c[2] for c in cells]
        self.clear_zones.append((min(xs), min(ys), min(zs), max(xs), max(ys) + headroom - 1, max(zs)))

    def entity(self, kind, pos, yaw=0.0, **kw):
        self.ents.append(dict(kind=kind, pos=pos, yaw=yaw, **kw))

    def deco(self, x, y, z, s, be=None):
        """Place decoration only into empty cells outside clear zones."""
        if self.is_empty(x, y, z) and not self.in_clear(x, y, z) and self.in_box(x, y, z):
            self.put(x, y, z, s, be)
            return True
        return False


def _solid_face(s):
    if s is None or s == AIR:
        return False
    n = state_name(s)
    bad = ("slab", "stairs", "carpet", "lantern", "chain", "banner", "cobweb", "candle", "chest", "anvil", "fence",
           "_wall", "bars", "pane", "campfire", "grindstone", "water", "ancient_", "jigsaw_x", "barrel_x", "air")
    if n.endswith("_wall"):
        return False
    return not any(b in n for b in bad)


def fix_connections(P):
    """Compute wall / bars / pane / fence side states (templates are placed with known shapes)."""
    dirs = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0)}
    for p, s in list(P.b.items()):
        n, props = parse_state(s)
        x, y, z = p
        if n.endswith("_wall") and not n.endswith("wall_banner"):
            conn = {}
            above = P.b.get((x, y + 1, z))
            for d, v in dirs.items():
                t = P.b.get((x + v[0], y + v[1], z + v[2]))
                tn = state_name(t) if t else ""
                c = _solid_face(t) or (tn.endswith("_wall") and not tn.endswith("wall_banner")) or tn == BARS or "fence_gate" in tn
                conn[d] = c
            sides = {}
            for d, c in conn.items():
                sides[d] = ("tall" if _solid_face(above) else "low") if c else "none"
            ns, ss, es, ws = sides["north"], sides["south"], sides["east"], sides["west"]
            above_wall_up = above is not None and state_name(above).endswith("_wall") and not state_name(above).endswith("wall_banner")
            if above_wall_up:
                up = True
            elif (ns == "none" and ss == "none" and es == "none" and ws == "none") or ((ns == "none") != (ss == "none")) or ((es == "none") != (ws == "none")):
                up = True
            elif (ns == "tall" and ss == "tall") or (es == "tall" and ws == "tall"):
                up = False
            else:
                up = above is not None and ("lantern" in above or "banner" in above or _solid_face(above))
            P.b[p] = fmt_state(n, dict(sides, up="true" if up else "false"))
        elif n in ("minecraft:iron_bars",) or n.endswith("_pane"):
            out = {}
            for d, v in dirs.items():
                t = P.b.get((x + v[0], y + v[1], z + v[2]))
                tn = state_name(t) if t else ""
                out[d] = "true" if (_solid_face(t) or tn == BARS or tn.endswith("_pane") or (tn.endswith("_wall") and "banner" not in tn)) else "false"
            P.b[p] = fmt_state(n, out)
        elif n.endswith("_fence"):
            out = {}
            for d, v in dirs.items():
                t = P.b.get((x + v[0], y + v[1], z + v[2]))
                tn = state_name(t) if t else ""
                out[d] = "true" if (_solid_face(t) or tn.endswith("_fence")) else "false"
            P.b[p] = fmt_state(n, out)


PATTERN_CODES = {"gradient_up": "gru", "gradient": "gra", "rhombus": "mr", "triangles_bottom": "bts",
                 "border": "bo", "stripe_top": "ts", "circle": "mc", "triangle_top": "tt"}
DYE_IDS = {"white": 0, "orange": 1, "magenta": 2, "light_blue": 3, "yellow": 4, "lime": 5, "pink": 6, "gray": 7,
           "light_gray": 8, "cyan": 9, "purple": 10, "blue": 11, "brown": 12, "green": 13, "red": 14, "black": 15}


def be_nbt(spec, version):
    """Block-entity NBT for chests (loot table), banners (per-version pattern format) and jigsaws."""
    k = spec["kind"]
    if k == "chest":
        return Compound({"id": String("minecraft:chest"), "LootTable": String(spec["loot"])})
    if k == "banner":
        if version == "1.20.1":
            pats = [Compound({"Pattern": String(PATTERN_CODES[p]), "Color": Int(DYE_IDS[c])}) for p, c in spec["patterns"]]
            return Compound({"id": String("minecraft:banner"), "Patterns": List[Compound](pats)})
        pats = [Compound({"pattern": String("minecraft:" + p), "color": String(c)}) for p, c in spec["patterns"]]
        return Compound({"id": String("minecraft:banner"), "patterns": List[Compound](pats)})
    if k == "jigsaw":
        c = {"id": String("minecraft:jigsaw"), "name": String(spec["name"]), "target": String(spec["target"]),
             "pool": String(spec["pool"]), "joint": String(spec["joint"]), "final_state": String(spec["final_state"])}
        if version != "1.20.1":
            c["selection_priority"] = Int(0)
            c["placement_priority"] = Int(0)
        return Compound(c)
    raise ValueError(k)


def write_json(path: Path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def piece_of(boxes: dict, p):
    for name, b in boxes.items():
        if b[0] <= p[0] <= b[3] and b[1] <= p[1] <= b[4] and b[2] <= p[2] <= b[5]:
            return name
    return None


_BACK = {"north": "south", "south": "north", "east": "west", "west": "east"}
_STEP = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0)}


def cut_pieces(P, version, boxes, connections, piece_pool, jig_name, pool_id, anchors=(), entity_nbt=None):
    """Cut a plan into one template per box and add the floor-level jigsaw pairs.

    connections: (parent, child, parent jigsaw pos, parent front, connection name); the child jigsaw is the
    neighbouring cell in the child box, both keep the plan block there as final_state.
    anchors: (piece, pos, orientation, name, target, pool, final_state) extra jigsaws (e.g. the offerings anchor).
    """
    tpls = {n: Template(size=(b[3] - b[0] + 1, b[4] - b[1] + 1, b[5] - b[2] + 1)) for n, b in boxes.items()}
    dropped = 0
    for p, s in P.b.items():
        n = piece_of(boxes, p)
        if n is None:
            dropped += 1
            continue
        b = boxes[n]
        be = P.be.get(p)
        tpls[n].blocks[(p[0] - b[0], p[1] - b[1], p[2] - b[2])] = (s, be_nbt(be, version) if be else None)
    for parent, child, pos, front, cname in connections:
        back, step = _BACK[front], _STEP[front]
        cpos = (pos[0] + step[0], pos[1] + step[1], pos[2] + step[2])
        assert piece_of(boxes, pos) == parent and piece_of(boxes, cpos) == child, (cname, piece_of(boxes, pos), piece_of(boxes, cpos))
        pfin = P.b.get(pos, AIR)
        cfin = P.b.get(cpos, AIR)
        pj = {"kind": "jigsaw", "name": jig_name(f"{cname}_socket"), "target": jig_name(f"{cname}_plug"),
              "pool": pool_id(piece_pool[child]), "joint": "aligned", "final_state": pfin}
        cj = {"kind": "jigsaw", "name": jig_name(f"{cname}_plug"), "target": jig_name(f"{cname}_socket"),
              "pool": EMPTY_POOL, "joint": "aligned", "final_state": cfin}
        for (pc, pp, fr, spec) in ((parent, pos, front, pj), (child, cpos, back, cj)):
            b = boxes[pc]
            tpls[pc].blocks[(pp[0] - b[0], pp[1] - b[1], pp[2] - b[2])] = (f"minecraft:jigsaw[orientation={fr}_up]", be_nbt(spec, version))
    for piece, pos, orientation, name, target, pool, final in anchors:
        b = boxes[piece]
        spec = {"kind": "jigsaw", "name": name, "target": target, "pool": pool, "joint": "aligned", "final_state": final}
        tpls[piece].blocks[(pos[0] - b[0], pos[1] - b[1], pos[2] - b[2])] = (f"minecraft:jigsaw[orientation={orientation}]", be_nbt(spec, version))
    for i, e in enumerate(P.ents):
        p = tuple(math.floor(c) for c in e["pos"])
        n = piece_of(boxes, p)
        assert n, e
        b = boxes[n]
        tpls[n].entities.append({"pos": (e["pos"][0] - b[0], e["pos"][1] - b[1], e["pos"][2] - b[2]),
                                 "nbt": entity_nbt(e, version, i * 17)})
    return tpls, dropped


def offerings_template(version, ring_name, anchor_name, placeholder, radius=3):
    """Flat (2r+1)x1x(2r+1) piece: an up-facing jigsaw in the centre and one placeholder per ring cell.

    It is attached under a down-facing anchor above the altar; its processor list swaps each placeholder
    for the offering that the direction marker below it (rotated with the parent piece) asks for.
    """
    d = 2 * radius + 1
    tpl = Template(size=(d, 1, d))
    jig = {"kind": "jigsaw", "name": ring_name, "target": anchor_name, "pool": EMPTY_POOL, "joint": "aligned", "final_state": AIR}
    tpl.blocks[(radius, 0, radius)] = ("minecraft:jigsaw[orientation=up_north]", be_nbt(jig, version))
    for p in ((d - 1, 0, radius), (0, 0, radius), (radius, 0, d - 1), (radius, 0, 0)):
        tpl.blocks[p] = (placeholder, None)
    return tpl
