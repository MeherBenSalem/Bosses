#!/usr/bin/env python3
"""Boot a release jar on a real production Forge 1.20.1 or NeoForge 1.21.1 server.

Needs a server installed once by the official installer into .gradle/prod-boot/<loader>-<mc>/:

  java -jar forge-1.20.1-47.4.20-installer.jar --installServer .gradle/prod-boot/forge-1.20.1
  java -jar neoforge-21.1.219-installer.jar --installServer .gradle/prod-boot/neoforge-1.21.1

Then, for example:

  python tools/prod-boot/forge_server.py forge 1.20.1/forge/build/libs/remnant_bosses-forge-1.20.1-3.1.0.jar \
      --commands tools/prod-boot/realm_commands.txt

Each run keeps the installed libraries and starts from a fresh world and mods/ folder (the mod, GeckoLib and
JAuml). The log goes to prod-boot-<loader>-<mc>.log at the repository root.
"""
from __future__ import annotations

import argparse
import glob
import sys
from pathlib import Path

from prodboot import ROOT, RUNS, boot, cached, java, prepare, props, read_commands

LOADERS = {
    "forge": ("1.20.1", "net/minecraftforge/forge", lambda p: f"{p['minecraft_version']}-{p['forge_version']}", 17),
    "neoforge": ("1.21.1", "net/neoforged/neoforge", lambda p: p["neoforge_version"], 21),
}


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("loader", choices=sorted(LOADERS))
    ap.add_argument("jar", help="the release jar to test")
    ap.add_argument("--commands", help="console commands to run once the server is up, one per line")
    ap.add_argument("--port", type=int, default=25650)
    args = ap.parse_args()

    mc, path, version_of, java_major = LOADERS[args.loader]
    p = props(mc)
    run = RUNS / f"{args.loader}-{mc}"
    args_file = run / "libraries" / path / version_of(p) / "win_args.txt"
    if not args_file.exists():
        raise SystemExit(f"install the server first (see --help); missing {args_file}")
    mods = [Path(args.jar).resolve(),
            cached("software.bernie.geckolib", f"geckolib-{args.loader}-{mc}", p["geckolib_version"])]
    mods += [Path(j) for j in glob.glob(str(ROOT / mc / args.loader / "libs" / f"jauml-{args.loader}-*.jar"))]
    prepare(run, mods, args.port, keep=("libraries", "user_jvm_args.txt", "run.bat", "run.sh", "install.log"))
    (run / "user_jvm_args.txt").write_text("-Xmx3G\n")
    cmd = [java(java_major), "@user_jvm_args.txt", f"@libraries/{path}/{version_of(p)}/win_args.txt", "nogui"]
    return boot(cmd, run, ROOT / f"prod-boot-{args.loader}-{mc}.log", read_commands(args.commands))


if __name__ == "__main__":
    sys.exit(main())
