#!/usr/bin/env python3
"""Boot a release jar on a production 26.x Fabric or NeoForge server made by the official installer.

Install the server once into .gradle/prod-boot/<loader>-<mc>/:

  java -jar fabric-installer-1.1.2.jar server -mcversion 26.2 -loader 0.19.5 -downloadMinecraft   (in fabric-26.2/)
  java -jar neoforge-26.2.0.93-installer.jar --installServer .gradle/prod-boot/neoforge-26.2

Then, for example:

  python tools/prod-boot/installed_server.py fabric 26.2 26.2/fabric/build/libs/remnant_bosses-fabric-26.2-3.1.0.jar \
      --commands tools/prod-boot/realm_commands.txt

Each run keeps the installed launcher and libraries and starts from a fresh world and mods/ folder: the mod, GeckoLib,
JAuml and (Fabric) Fabric API, the same jars a player installs. The log goes to prod-boot-<loader>-<mc>.log at the
repository root. Exits non-zero on a mixin, datapack or startup error.
"""
from __future__ import annotations

import argparse
import glob
import os
import sys
from pathlib import Path

from prodboot import MODULES, ROOT, RUNS, boot, cached, prepare, props, read_commands

# Java 25 runs every 26.x server; set JAVA25 to its java executable on another machine.
JAVA = os.environ.get("JAVA25", r"C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe")
KEEP = ("libraries", "server.jar", "fabric-server-launch.jar", "fabric-server-launcher.properties", "versions",
        "user_jvm_args.txt", "run.bat", "run.sh", ".fabric")


def modrinth(version_id: str) -> Path:
    hits = glob.glob(str(MODULES / "maven.modrinth" / "geckolib" / version_id / "*" / f"geckolib-{version_id}.jar"))
    if not hits:
        raise SystemExit(f"GeckoLib {version_id} is not in the Gradle cache (build the workspace once first)")
    return Path(hits[0])


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("loader", choices=["fabric", "neoforge"])
    ap.add_argument("mc", choices=["26.2", "26.3"])
    ap.add_argument("jar", help="the release jar to test")
    ap.add_argument("--commands", help="console commands to run once the server is up, one per line")
    ap.add_argument("--port", type=int, default=25660)
    args = ap.parse_args()

    p = props(args.mc)
    run = RUNS / f"{args.loader}-{args.mc}"
    mods = [Path(args.jar).resolve(), modrinth(p[f"geckolib_{args.loader}_id"])]
    mods += [Path(j) for j in glob.glob(str(ROOT / args.mc / args.loader / "libs" / f"jauml-{args.loader}-*.jar"))]
    if args.loader == "fabric":
        launcher = run / "fabric-server-launch.jar"
        if not launcher.exists():
            raise SystemExit(f"install the server first (see --help); missing {launcher}")
        mods.append(cached("net.fabricmc.fabric-api", "fabric-api", p["fabric_version"]))
        cmd = [JAVA, "-Xmx3G", "-jar", "fabric-server-launch.jar", "nogui"]
    else:
        args_file = run / "libraries" / "net" / "neoforged" / "neoforge" / p["neoforge_version"] / "win_args.txt"
        if not args_file.exists():
            raise SystemExit(f"install the server first (see --help); missing {args_file}")
        cmd = [JAVA, "-Xmx3G", f"@libraries/net/neoforged/neoforge/{p['neoforge_version']}/win_args.txt", "nogui"]
    prepare(run, mods, args.port, keep=KEEP)
    return boot(cmd, run, ROOT / f"prod-boot-{args.loader}-{args.mc}.log", read_commands(args.commands))


if __name__ == "__main__":
    sys.exit(main())
