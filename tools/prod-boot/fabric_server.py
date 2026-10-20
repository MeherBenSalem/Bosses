#!/usr/bin/env python3
"""Boot a release jar on a real production Fabric server, assembled offline from the Gradle cache.

Dev runs (`gradlew runServer`) use Mojang names and load the mod from class folders, so they cannot catch
production-only failures such as unmapped mixin members. This builds the same layout a Fabric server install
uses: the Knot launcher, intermediary mappings, the vanilla bundler server jar and the mod jars in mods/.

  python tools/prod-boot/fabric_server.py 1.20.1 1.20.1/fabric/build/libs/remnant_bosses-fabric-1.20.1-3.1.0.jar \
      --commands tools/prod-boot/realm_commands.txt

The server lives in .gradle/prod-boot/fabric-<mc>/ (disposable; the EULA is accepted only there) and the log in
prod-boot-fabric-<mc>.log at the repository root. Exits non-zero on a mixin, datapack or startup error.
"""
from __future__ import annotations

import argparse
import glob
import json
import os
import sys
import zipfile
from pathlib import Path

from prodboot import CACHE, ROOT, RUNS, boot, cached, java, prepare, props, read_commands


def loader_classpath(loader_jar: Path, mc: str) -> list[Path]:
    meta = json.loads(zipfile.ZipFile(loader_jar).read("fabric-installer.json"))
    cp = [loader_jar, cached("net.fabricmc", "intermediary", mc, "v2")]
    for lib in meta["libraries"]["common"] + meta["libraries"].get("server", []):
        group, artifact, version = lib["name"].split(":")
        cp.append(cached(group, artifact, version))
    return cp


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("mc", choices=["1.20.1", "1.21.1"])
    ap.add_argument("jar", help="the release jar to test")
    ap.add_argument("--commands", help="console commands to run once the server is up, one per line")
    ap.add_argument("--port", type=int, default=25640)
    args = ap.parse_args()

    p = props(args.mc)
    loader = cached("net.fabricmc", "fabric-loader", p["fabric_loader_version"])
    server_jar = CACHE / "fabric-loom" / args.mc / "minecraft-server.jar"
    mods = [Path(args.jar).resolve(),
            cached("net.fabricmc.fabric-api", "fabric-api", p["fabric_version"]),
            cached("software.bernie.geckolib", f"geckolib-fabric-{args.mc}", p["geckolib_version"])]
    mods += [Path(j) for j in glob.glob(str(ROOT / args.mc / "fabric" / "libs" / "jauml-fabric-*.jar"))]

    run = RUNS / f"fabric-{args.mc}"
    run.mkdir(parents=True, exist_ok=True)
    prepare(run, mods, args.port)
    cp = os.pathsep.join(str(j) for j in loader_classpath(loader, args.mc))
    cmd = [java(17 if args.mc == "1.20.1" else 21), "-Xmx3G", f"-Dfabric.gameJarPath={server_jar}", "-cp", cp,
           "net.fabricmc.loader.impl.launch.knot.KnotServer", "nogui"]
    return boot(cmd, run, ROOT / f"prod-boot-fabric-{args.mc}.log", read_commands(args.commands))


if __name__ == "__main__":
    sys.exit(main())
