#!/usr/bin/env python3
"""Ashbound Constructs and Corrupted Relics data, written into both resource trees (1.20.1 and 1.21.1 formats):
entity loot tables, the two family tags, the ashen_cog and relic_shard item models and textures, spawn egg item
models, the ember and crystal bolt textures (the Memory Bolt atlas recoloured, same UVs) and en_us lang entries.
Also removes the lang keys and loot of the retired armored grub and sickleghast. Idempotent: running it again
changes nothing.

The lang file is shared with other work in progress, so it is edited line by line (retired keys removed, missing
keys appended) instead of being rewritten, and keeps its line endings.

Usage: python tools/remnants/families_data.py [--dry-run]
Spec: docs/remnants/families/ashbound-and-relics.md
"""
from __future__ import annotations

import io
import json
import re
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from slice4_data import ENT, NS, ROOT, VERSIONS, Writer, is120, pool, res, tag  # noqa: E402

DOCS = ROOT / "docs" / "models" / "remnants"
ASHBOUND = [f"{ENT}:ashbound_crawler", f"{ENT}:ember_servitor", f"{ENT}:rusted_guardian"]
RELICS = [f"{ENT}:shardling", f"{ENT}:ancient_vessel", f"{ENT}:relic_stalker"]
EGGS = ["ashbound_crawler", "ember_servitor", "rusted_guardian", "shardling", "ancient_vessel", "relic_stalker"]
MATERIALS = ["ashen_cog", "relic_shard"]


def loot(version):
    v = version
    return {
        "ashbound_crawler": [
            pool(f"{NS}:ashen_cog", 0, 1, looting=(0, 1), version=v),
            pool("minecraft:coal", 0, 2, version=v),
        ],
        "ember_servitor": [
            pool(f"{NS}:ashen_cog", 0, 1, looting=(0, 1), version=v),
            pool("minecraft:redstone", 0, 2, version=v),
        ],
        "rusted_guardian": [
            pool(f"{NS}:ashen_cog", 2, 4, looting=(0, 1), version=v),
            pool("minecraft:iron_ingot", 1, 3, version=v),
        ],
        "shardling": [
            pool(f"{NS}:relic_shard", 0, 1, looting=(0, 1), version=v),
            pool("minecraft:amethyst_shard", 0, 1, version=v),
        ],
        "ancient_vessel": [
            pool(f"{NS}:relic_shard", 1, 2, looting=(0, 1), version=v),
            pool("minecraft:gold_nugget", 2, 5, version=v),
        ],
        "relic_stalker": [
            pool(f"{NS}:relic_shard", 2, 4, looting=(0, 1), version=v),
            pool("minecraft:gold_ingot", 0, 2, version=v),
            pool("minecraft:candle", 0, 1, version=v),
        ],
    }


RETIRED_LOOT = ("armored_grub", "sickleghast")

LANG = {
    "entity.remnants.ashbound_crawler": "Ashbound Crawler",
    "entity.remnants.ember_servitor": "Ember Servitor",
    "entity.remnants.rusted_guardian": "Rusted Guardian",
    "entity.remnants.ember_bolt": "Ember Bolt",
    "entity.remnants.shardling": "Shardling",
    "entity.remnants.ancient_vessel": "Ancient Vessel",
    "entity.remnants.relic_stalker": "Relic Stalker",
    "entity.remnants.crystal_bolt": "Crystal Bolt",
    "item.remnant_bosses.ashbound_crawler_spawn_egg": "Ashbound Crawler Spawn Egg",
    "item.remnant_bosses.ember_servitor_spawn_egg": "Ember Servitor Spawn Egg",
    "item.remnant_bosses.rusted_guardian_spawn_egg": "Rusted Guardian Spawn Egg",
    "item.remnant_bosses.shardling_spawn_egg": "Shardling Spawn Egg",
    "item.remnant_bosses.ancient_vessel_spawn_egg": "Ancient Vessel Spawn Egg",
    "item.remnant_bosses.relic_stalker_spawn_egg": "Relic Stalker Spawn Egg",
    "item.remnant_bosses.ashen_cog": "Ashen Cog",
    "item.remnant_bosses.ashen_cog.tooltip.0": "A soot-black cog from a Kharum construct. Its teeth are still warm.",
    "item.remnant_bosses.relic_shard": "Relic Shard",
    "item.remnant_bosses.relic_shard.tooltip.0": "A splinter of Sanctum soul glass. The Oracle's light still hums inside.",
}
RETIRED_LANG = ("entity.remnant_bosses.armored_grub", "item.remnant_bosses.armored_grub_spawn_egg",
                "subtitles.armored_grub_ambient", "subtitles.armored_grub_death", "entity.remnants.armored_grub",
                "entity.remnants.sickleghast", "entity.remnant_bosses.sickleghast", "item.remnant_bosses.sickleghast_spawn_egg")
RETIRED_SOUNDS = ("armored_grub_ambient", "armored_grub_death")


def hexc(value):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


# Memory Bolt atlas colours (Seyr violet ramp and remnant light) -> each family's ramp.
BOLT_SOURCE = [(106, 82, 176), (158, 134, 224), (210, 198, 255), (79, 201, 193), (156, 242, 230)]
BOLT_RECOLOURS = {
    # Kharum ember: #9C3412 #E0661C #FFA640, white-hot core #FFE08A.
    "ember_bolt": ["#9C3412", "#E0661C", "#FFA640", "#FFE08A", "#FFF6D6"],
    # Oracle corruption: #9C3F7E #D77BC0 #F6C9EC, pale rose core.
    "crystal_bolt": ["#9C3F7E", "#D77BC0", "#F6C9EC", "#F6C9EC", "#FFF0FA"],
}


def recolour(src: Path, ramp) -> bytes:
    im = Image.open(src).convert("RGBA")
    lut = {s: hexc(d) for s, d in zip(BOLT_SOURCE, ramp)}
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a and (r, g, b) in lut:
                px[x, y] = lut[(r, g, b)] + (a,)
            elif a:
                raise SystemExit(f"{src.name}: unmapped colour {(r, g, b)} at {x},{y}")
    out = io.BytesIO()
    im.save(out, "PNG")
    return out.getvalue()


class KeepWriter(Writer):
    """slice4_data's Writer, but files keep their line endings (new files get LF)."""

    def json(self, path: Path, obj):
        text = json.dumps(obj, indent=2, ensure_ascii=False) + "\n"
        if path.exists() and b"\r\n" in path.read_bytes():
            text = text.replace("\n", "\r\n")
        write_bytes(self, path, text.encode("utf-8"))


def write_bytes(w: Writer, path: Path, data: bytes):
    if path.exists() and path.read_bytes() == data:
        return
    w.changes.append(f"write {path.relative_to(ROOT).as_posix()}")
    if not w.dry:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)


def bolt_textures(w: Writer):
    """Recoloured Memory Bolt atlases, kept beside the other built models for review."""
    out = {}
    for bolt, ramp in BOLT_RECOLOURS.items():
        for suffix in ("", "_glowmask"):
            data = recolour(DOCS / "memory_bolt" / f"memory_bolt{suffix}.png", ramp)
            write_bytes(w, DOCS / bolt / f"{bolt}{suffix}.png", data)
            out[f"{bolt}{suffix}.png"] = data
    return out


def edit_lang(w: Writer, path: Path):
    raw = path.read_bytes()
    crlf = b"\r\n" in raw
    text = raw.decode("utf-8").replace("\r\n", "\n")
    lines = text.split("\n")
    key_of = re.compile(r'^\s*"((?:[^"\\]|\\.)*)"\s*:')
    present = set()
    kept = []
    for line in lines:
        m = key_of.match(line)
        if m and m.group(1) in RETIRED_LANG:
            continue
        if m:
            present.add(m.group(1))
        kept.append(line)
    missing = [k for k in LANG if k not in present]
    close = max(i for i, line in enumerate(kept) if line.strip() == "}")
    last = max(i for i in range(close) if key_of.match(kept[i]))
    if missing:
        kept[last] = kept[last].rstrip().rstrip(",") + ","
        new = [f"  {json.dumps(k, ensure_ascii=False)}: {json.dumps(LANG[k], ensure_ascii=False)}" for k in missing]
        kept[last + 1:last + 1] = [line + ("," if i < len(new) - 1 else "") for i, line in enumerate(new)]
    else:
        kept[last] = kept[last].rstrip().rstrip(",")
    out = "\n".join(kept)
    json.loads(out)  # still valid JSON
    if crlf:
        out = out.replace("\n", "\r\n")
    write_bytes(w, path, out.encode("utf-8"))


def emit(version, w: Writer, bolts):
    data = res(version) / "data"
    assets = res(version) / "assets" / NS
    loot_dir = data / ENT / ("loot_tables" if is120(version) else "loot_table") / "entities"
    for name, pools in loot(version).items():
        w.json(loot_dir / f"{name}.json", {"type": "minecraft:entity", "pools": pools})
    for name in RETIRED_LOOT:
        w.delete(loot_dir / f"{name}.json")
    folder = "entity_types" if is120(version) else "entity_type"
    tag(w, data / ENT / "tags" / folder / "ashbound.json", ASHBOUND)
    tag(w, data / ENT / "tags" / folder / "corrupted_relics.json", RELICS)
    for name in MATERIALS:
        write_bytes(w, assets / "textures" / "item" / f"{name}.png", (DOCS / "items" / f"{name}.png").read_bytes())
        w.json(assets / "models" / "item" / f"{name}.json",
               {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})
    for name in EGGS:
        w.json(assets / "models" / "item" / f"{name}_spawn_egg.json", {"parent": "item/template_spawn_egg"})
    for name, png in bolts.items():
        write_bytes(w, assets / "textures" / "entities" / name, png)
    sounds_path = assets / "sounds.json"
    sounds = json.loads(sounds_path.read_text(encoding="utf-8"))
    if any(s in sounds for s in RETIRED_SOUNDS):
        for s in RETIRED_SOUNDS:
            sounds.pop(s, None)
        w.json(sounds_path, sounds)
    edit_lang(w, assets / "lang" / "en_us.json")


def main():
    dry = "--dry-run" in sys.argv
    w0 = KeepWriter(dry)
    bolts = bolt_textures(w0)
    for line in w0.changes:
        print(f"[docs] {'would ' if dry else ''}{line}")
    for version in VERSIONS:
        w = KeepWriter(dry)
        emit(version, w, bolts)
        for line in w.changes:
            print(f"[{version}] {'would ' if dry else ''}{line}")
        print(f"[{version}] {len(w.changes)} change(s)")


if __name__ == "__main__":
    main()
