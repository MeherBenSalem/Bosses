#!/usr/bin/env python3
"""Forgotten Watcher and Forgotten Souls data: entity loot tables, entity tags, recipes and en_us lang
entries, written into both resource trees (1.20.1 and 1.21.1 formats). Also removes the keys and files of
the old Umbrakar, wraith and dirge lantern entities. Idempotent: running it again changes nothing.

Usage: python tools/remnants/slice4_data.py [--dry-run]
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
VERSIONS = ("1.20.1", "1.21.1")
NS, ENT = "remnant_bosses", "remnants"


def res(version):
    return ROOT / version / "common" / "src" / "main" / "resources"


def is120(version):
    return version == "1.20.1"


def pool(name, lo=1, hi=1, looting=None, chance=None, version="1.21.1"):
    entry = {"type": "minecraft:item", "name": name}
    fns = []
    if (lo, hi) != (1, 1):
        fns.append({"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}})
    if looting:
        count = {"type": "minecraft:uniform", "min": looting[0], "max": looting[1]}
        fns.append({"function": "minecraft:looting_enchant", "count": count} if is120(version)
                   else {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting", "count": count})
    if fns:
        entry["functions"] = fns
    p = {"rolls": 1, "bonus_rolls": 0, "entries": [entry]}
    if chance is not None:
        base, per_level = chance if isinstance(chance, tuple) else (chance, None)
        if per_level is None:
            p["conditions"] = [{"condition": "minecraft:random_chance", "chance": base}]
        elif is120(version):
            p["conditions"] = [{"condition": "minecraft:random_chance_with_looting", "chance": base, "looting_multiplier": per_level}]
        else:
            p["conditions"] = [{"condition": "minecraft:random_chance_with_enchanted_bonus", "enchantment": "minecraft:looting",
                                "unenchanted_chance": base,
                                "enchanted_chance": {"type": "minecraft:linear", "base": round(base + per_level, 4),
                                                     "per_level_above_first": per_level}}]
    return p


def loot(version):
    v = version
    return {
        "forgotten_watcher": [
            pool(f"{NS}:memory_staff", version=v),
            pool(f"{NS}:watchers_eye", version=v),
            pool(f"{NS}:knowledge_fragment", 2, 3, version=v),
            pool(f"{NS}:memory_shard", 6, 10, version=v),
            pool("minecraft:amethyst_shard", 4, 9, version=v),
            pool("minecraft:echo_shard", chance=0.25, version=v),
        ],
        "lost_pilgrim": [
            pool(f"{NS}:memory_shard", 0, 1, looting=(0, 1), version=v),
            pool("minecraft:candle", 0, 1, version=v),
        ],
        "memory_wraith": [
            pool(f"{NS}:memory_shard", 1, 2, looting=(0, 1), version=v),
            pool("minecraft:paper", 0, 2, version=v),
        ],
        "soul_collector": [
            pool(f"{NS}:memory_shard", 2, 4, version=v),
            pool(f"{NS}:knowledge_fragment", chance=(0.15, 0.05), version=v),
            pool("minecraft:soul_lantern", 0, 1, version=v),
        ],
    }


RETIRED_LOOT = ("umbrakar", "dirge_lantern", "wraith")

SOULS = [f"{ENT}:lost_pilgrim", f"{ENT}:memory_wraith", f"{ENT}:soul_collector", f"{ENT}:soul_wisp"]
UNDEAD_ADD = [f"{ENT}:forgotten_watcher"] + SOULS
UNDEAD_DROP = [f"{ENT}:umbrakar", f"{ENT}:wraith", f"{ENT}:dirge_lantern"]


def result(version, item, count=1):
    return {"item": item, "count": count} if is120(version) else {"id": item, "count": count}


def shaped(version, pattern, key, item, category):
    return {"type": "minecraft:crafting_shaped", "category": category, "pattern": pattern,
            "key": {k: {"item": v} for k, v in key.items()}, "result": result(version, item)}


def recipes(version):
    r = {"memory_lens": shaped(version, ["MAM", "ASA", "MAM"],
                               {"M": f"{NS}:memory_shard", "A": "minecraft:amethyst_shard", "S": "minecraft:spyglass"},
                               f"{NS}:memory_lens", "misc")}
    for piece, diamond in (("hood", "helmet"), ("robe", "chestplate"), ("leggings", "leggings"), ("boots", "boots")):
        r[f"archivist_{piece}"] = shaped(version, ["MFM", "MDM", "MMM"],
                                         {"M": f"{NS}:memory_shard", "F": f"{NS}:knowledge_fragment",
                                          "D": f"minecraft:diamond_{diamond}"},
                                         f"{NS}:archivist_{piece}", "equipment")
    return r


LANG = {
    "entity.remnants.forgotten_watcher": "The Forgotten Watcher",
    "entity.remnants.watcher_illusion": "Memory of the Watcher",
    "entity.remnants.memory_bolt": "Memory Bolt",
    "entity.remnants.lost_pilgrim": "Lost Pilgrim",
    "entity.remnants.memory_wraith": "Memory Wraith",
    "entity.remnants.soul_collector": "Soul Collector",
    "entity.remnants.soul_wisp": "Soul Wisp",
    "item.remnant_bosses.forgotten_watcher_spawn_egg": "Forgotten Watcher Spawn Egg",
    "item.remnant_bosses.lost_pilgrim_spawn_egg": "Lost Pilgrim Spawn Egg",
    "item.remnant_bosses.memory_wraith_spawn_egg": "Memory Wraith Spawn Egg",
    "item.remnant_bosses.soul_collector_spawn_egg": "Soul Collector Spawn Egg",
    "item.remnant_bosses.memory_shard": "Memory Shard",
    "item.remnant_bosses.memory_shard.tooltip.0": "A splinter of Seyr crystal. Someone's memory is still inside.",
    "item.remnant_bosses.knowledge_fragment": "Ancient Knowledge Fragment",
    "item.remnant_bosses.knowledge_fragment.tooltip.0": "A tablet shard from the Lantern Archive, its glyph still lit.",
    "item.remnant_bosses.knowledge_fragment.tooltip.1": "Repairs the Memory Staff and Archivist armor.",
    "item.remnant_bosses.memory_lens": "Lens of Memory",
    "item.remnant_bosses.memory_lens.tooltip.0": "Ground from the Observatory's broken lenses. Use it on the Ancient Altar.",
    "item.remnant_bosses.memory_lens.tooltip.offerings": "Offerings: %1$s east, %2$s west, %3$s south, %4$s north.",
    "item.remnant_bosses.memory_staff": "Memory Staff",
    "item.remnant_bosses.memory_staff.tooltip.0": "The focus Seyr's archivists used to read the crystals.",
    "item.remnant_bosses.memory_staff.tooltip.1": "Use: a homing memory bolt. Hold for 1 s, then release: a ring of 12 bolts.",
    "item.remnant_bosses.memory_staff.tooltip.2": "Repaired with Ancient Knowledge Fragments.",
    "item.remnant_bosses.watchers_eye": "Watcher's Eye",
    "item.remnant_bosses.watchers_eye.tooltip.0": "The Watcher's crystal eye. It still sees more than it should.",
    "item.remnant_bosses.watchers_eye.tooltip.1": "Off-hand: hostile creatures within 24 blocks glow every 10 s.",
    "item.remnant_bosses.watchers_eye.tooltip.2": "Use from the off-hand: blink 8 blocks toward where you look. Costs 2 hunger.",
    "item.remnant_bosses.watchers_eye.offhand": "The Eye answers only an empty off-hand… hold it in your off-hand.",
    "item.remnant_bosses.watchers_eye.hungry": "The Eye needs a fed mind (2 hunger).",
    "item.remnant_bosses.watchers_eye.blocked": "No room to blink that way.",
    "item.remnant_bosses.archivist_hood": "Archivist Hood",
    "item.remnant_bosses.archivist_hood.tooltip.0": "A scholar's deep hood. The reading-lamp soot never washed out.",
    "item.remnant_bosses.archivist_robe": "Archivist Robe",
    "item.remnant_bosses.archivist_robe.tooltip.0": "Robe over verdigris plates, clasped with a violet crystal.",
    "item.remnant_bosses.archivist_leggings": "Archivist Leggings",
    "item.remnant_bosses.archivist_leggings.tooltip.0": "Worn thin by long nights between the shelves.",
    "item.remnant_bosses.archivist_boots": "Archivist Boots",
    "item.remnant_bosses.archivist_boots.tooltip.0": "Soft soles for quiet halls.",
    "item.remnant_bosses.archivist.set.two": "Set (2): Quiet Mind. Blindness and Darkness fade at once.",
    "item.remnant_bosses.archivist.set.four": "Set (4): Archive Ward. Negates one projectile hit every 30 s.",
    "message.remnant_bosses.watcher.disabled": "The ritual is dormant (disabled or Peaceful difficulty).",
    "message.remnant_bosses.watcher.pattern": "Place four Ancient Pedestals 3 blocks from the altar, each with its offering on top. Your Boss Hunts journal names them (by default: amethyst block east, calcite west, chiseled sandstone south, lectern north).",
    "message.remnant_bosses.watcher.nearby": "A Forgotten Watcher already keeps its vigil nearby.",
    "message.remnant_bosses.watcher.space": "The Watcher needs clear space above the altar.",
    "message.remnant_bosses.watcher.summoned": "The lenses turn. The Forgotten Watcher opens its eye!",
    "message.remnant_bosses.watcher.realm": "The Watcher remembers only the sky beneath the eclipse. Perform the rite in the Eclipsed Realm.",
    "gui.remnant_bosses.journal.boss.forgotten_watcher": "The Forgotten Watcher",
    "gui.remnant_bosses.journal.clue.forgotten_watcher": "An observatory crowns the Lantern Heights of the Eclipsed Realm. Its lenses still turn toward a sky that has not moved since the Last Eclipse.",
    "gui.remnant_bosses.journal.lore.forgotten_watcher": "Seyr built the Watcher to remember everything before the end. It saw the Oracle call every soul to itself, and it remembers whose voice the Oracle was answering. It was built to remember, not to tell.",
    "biome.remnant_bosses.lantern_heights": "The Lantern Heights",
}
RETIRED_LANG = ("entity.remnant_bosses.wraith", "item.remnant_bosses.wraith_spawn_egg", "entity.remnant_bosses.umbrakar",
                "entity.remnant_bosses.umbrakar_orb", "item.remnant_bosses.umbrakar_spawn_egg", "entity.remnants.wraith",
                "entity.remnants.umbrakar", "entity.remnants.umbrakar_orb", "entity.remnants.dirge_lantern",
                "entity.remnant_bosses.dirge_lantern", "item.remnant_bosses.dirge_lantern_spawn_egg",
                "gui.remnant_bosses.journal.boss.umbrakar", "gui.remnant_bosses.journal.clue.umbrakar",
                "gui.remnant_bosses.journal.lore.umbrakar")


class Writer:
    def __init__(self, dry):
        self.dry = dry
        self.changes = []

    def json(self, path: Path, obj):
        text = json.dumps(obj, indent=2, ensure_ascii=False) + "\n"
        if path.exists() and path.read_text(encoding="utf-8") == text:
            return
        self.changes.append(f"write {path.relative_to(ROOT).as_posix()}")
        if not self.dry:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding="utf-8")

    def delete(self, path: Path):
        if not path.exists():
            return
        self.changes.append(f"delete {path.relative_to(ROOT).as_posix()}")
        if not self.dry:
            path.unlink()


def tag(w: Writer, path: Path, add, drop=()):
    cur = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"replace": False, "values": []}
    values = [v for v in cur.get("values", []) if v not in drop]
    values += [v for v in add if v not in values]
    w.json(path, {"replace": bool(cur.get("replace", False)), "values": values})


def emit(version, w: Writer):
    data = res(version) / "data"
    loot_dir = data / ENT / ("loot_tables" if is120(version) else "loot_table") / "entities"
    for name, pools in loot(version).items():
        w.json(loot_dir / f"{name}.json", {"type": "minecraft:entity", "pools": pools})
    for name in RETIRED_LOOT:
        w.delete(loot_dir / f"{name}.json")
    for folder in ("entity_type", "entity_types"):
        if (data / ENT / "tags" / folder).exists():
            tag(w, data / ENT / "tags" / folder / "forgotten_souls.json", SOULS)
        undead = data / "minecraft" / "tags" / folder / "undead.json"
        if undead.exists():
            tag(w, undead, UNDEAD_ADD, UNDEAD_DROP)
    recipe_dir = data / NS / ("recipes" if is120(version) else "recipe")
    for name, obj in recipes(version).items():
        w.json(recipe_dir / f"{name}.json", obj)
    lang_path = res(version) / "assets" / NS / "lang" / "en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    for key in RETIRED_LANG:
        lang.pop(key, None)
    lang.update(LANG)
    w.json(lang_path, lang)


def main():
    dry = "--dry-run" in sys.argv
    for version in VERSIONS:
        w = Writer(dry)
        emit(version, w)
        for line in w.changes:
            print(f"[{version}] {'would ' if dry else ''}{line}")
        print(f"[{version}] {len(w.changes)} change(s)")


if __name__ == "__main__":
    main()
