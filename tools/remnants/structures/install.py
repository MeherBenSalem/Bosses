#!/usr/bin/env python3
"""Copy staged structure datapacks (Hollow Citadel, Gravebound Catacombs) into the mod resources.

The staged JSON references entities and items that must be registered first (see REQUIRED; e.g.
remnants:hollow_sentinel or remnants:burial_guardian, remnant_bosses:sovereigns_seal or crypt_key).
Installing earlier would break datapack loading (spawn_overrides with unknown entity types fail the
worldgen registry).

Default is a dry run that lists what would be copied and checks the registrations by searching
the version's init/*.java sources for the registry names. Pass --apply to copy; --force skips the
registration check. --only <structure> limits the copy (and the registration check) to that
structure's files: every staged path containing the structure name (templates, worldgen JSON, its
biome tag, its chest loot tables). Without --only, everything staged for the version is copied.

Usage: python tools/remnants/structures/install.py [--apply] [--force] [--version 1.20.1] [--only gravebound_catacombs]
"""
from __future__ import annotations

import argparse
import filecmp
import shutil
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from common import NS, OUT, REPO, STRUCTURES, VERSIONS  # noqa: E402

# Registry names each structure's staged files reference (entities in spawn_overrides / template NBT,
# items in chest loot tables).
REQUIRED = {
    "hollow_citadel": {
        "entity": ["hollow_sentinel", "hollow_arbalist", "fallen_standard_bearer", "soulbound_knight"],
        "item": ["tarnished_regalia", "royal_soul_fragment", "sovereigns_seal"],
    },
    "gravebound_catacombs": {
        "entity": ["gravebound_soldier", "crypt_lurker", "catacomb_rat", "burial_guardian"],
        "item": ["crypt_key", "grave_iron"],
    },
    "forgotten_observatory": {
        "entity": ["lost_pilgrim", "memory_wraith", "soul_collector"],
        "item": ["memory_lens", "knowledge_fragment", "memory_shard"],
    },
}


def registered(version: str, structures) -> dict[str, bool]:
    src = REPO / version / "common" / "src" / "main" / "java"
    text = ""
    for f in src.rglob("init/*.java"):
        text += f.read_text(encoding="utf-8", errors="ignore")
    found = {}
    for st in structures:
        for kind, names in REQUIRED.get(st, {}).items():  # shrines have no REQUIRED entry
            for n in names:
                found[f"{kind}:{n}"] = f'"{n}"' in text
    return found


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--apply", action="store_true", help="copy files (default: dry run)")
    ap.add_argument("--force", action="store_true", help="install even if registrations are missing")
    ap.add_argument("--version", choices=list(VERSIONS), action="append", help="limit to a version (repeatable)")
    ap.add_argument("--only", choices=list(STRUCTURES), help="install only this structure's staged files")
    args = ap.parse_args()
    structures = [args.only] if args.only else list(STRUCTURES)
    status = 0
    for version in args.version or list(VERSIONS):
        src_root = OUT / version / "data"
        dst_root = REPO / version / "common" / "src" / "main" / "resources" / "data"
        if not (src_root / NS).exists():
            print(f"[{version}] nothing staged; run the structure generator first")
            status = 1
            continue
        regs = registered(version, structures)
        missing = [k for k, ok in regs.items() if not ok]
        if missing:
            print(f"[{version}] registry names not found in init/*.java: {', '.join(missing)}")
            if not args.force:
                print(f"[{version}] refusing to install (use --force to override)")
                status = 1
                continue
        files = sorted(p for p in src_root.rglob("*") if p.is_file()
                       and (not args.only or args.only in p.relative_to(src_root).as_posix()))
        if not files:
            print(f"[{version}] nothing staged for {args.only}")
            status = 1
            continue
        new = changed = same = 0
        for f in files:
            rel = f.relative_to(src_root)
            dst = dst_root / rel
            if not dst.exists():
                new += 1
                tag = "new "
            elif filecmp.cmp(f, dst, shallow=False):
                same += 1
                continue
            else:
                changed += 1
                tag = "upd "
            print(f"[{version}] {tag}{(Path('data') / rel).as_posix()}")
            if args.apply:
                dst.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(f, dst)
        verb = "copied" if args.apply else "would copy"
        print(f"[{version}] {verb} {new} new + {changed} changed files ({same} unchanged) into {dst_root}")
    if not args.apply:
        print("dry run only; pass --apply to install")
    return status


if __name__ == "__main__":
    sys.exit(main())
