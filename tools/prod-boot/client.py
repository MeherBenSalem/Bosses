#!/usr/bin/env python3
"""Install and launch a production 26.x Fabric or NeoForge client, the way the vanilla launcher does.

Dev runs (`gradlew runClient`) load the mod from class folders with dev-only launch code, so they cannot show what a
player gets. This installs the loader with its official installer into .gradle/prod-boot/client-<loader>-<mc>/
(a launcher-style folder: versions/, libraries/), resolves the installed profile (inheritsFrom, library rules,
arguments), downloads what is missing from Mojang and the loader mavens, and starts Minecraft in offline mode with
the release jar, GeckoLib, JAuml and (Fabric) Fabric API in mods/.

  python tools/prod-boot/client.py install fabric 26.2
  python tools/prod-boot/client.py launch fabric 26.2 26.2/fabric/build/libs/remnant_bosses-fabric-26.2-3.1.0.jar \
      --server localhost:25660

`--server` joins a server through Quick Play (the scenario script drives it; see client_scenario.py). The client
log is written to prod-client-<loader>-<mc>.log at the repository root.
"""
from __future__ import annotations

import argparse
import glob
import hashlib
import json
import os
import shutil
import subprocess
import sys
import urllib.request
import uuid
from pathlib import Path

from prodboot import MODULES, ROOT, RUNS, cached, props

# Java 25 runs every 26.x client; set JAVA25 to its java executable on another machine.
JAVA = os.environ.get("JAVA25", r"C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe")
MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
ASSETS = Path.home() / ".gradle" / "caches" / "neoformruntime" / "assets"
INSTALLERS = RUNS / "installers"
USERNAME = "RemnantProbe"


def fetch(url: str, dest: Path, sha1: str | None = None) -> Path:
    if dest.exists() and (sha1 is None or hashlib.sha1(dest.read_bytes()).hexdigest() == sha1):
        return dest
    dest.parent.mkdir(parents=True, exist_ok=True)
    tmp = dest.with_suffix(dest.suffix + ".part")
    with urllib.request.urlopen(url) as response, open(tmp, "wb") as out:
        shutil.copyfileobj(response, out)
    tmp.replace(dest)
    return dest


def vanilla_json(root: Path, mc: str) -> Path:
    target = root / "versions" / mc / f"{mc}.json"
    if not target.exists():
        manifest = json.load(urllib.request.urlopen(MANIFEST))
        entry = next(v for v in manifest["versions"] if v["id"] == mc)
        fetch(entry["url"], target, entry["sha1"])
    return target


def install(loader: str, mc: str) -> Path:
    p = props(mc)
    root = RUNS / f"client-{loader}-{mc}"
    root.mkdir(parents=True, exist_ok=True)
    (root / "launcher_profiles.json").write_text('{"profiles": {}}\n')
    vanilla_json(root, mc)
    if loader == "fabric":
        installer = INSTALLERS / "fabric-installer-1.1.2.jar"
        cmd = [JAVA, "-jar", str(installer), "client", "-dir", str(root), "-mcversion", mc,
               "-loader", p["fabric_loader_version"], "-noprofile"]
    else:
        installer = INSTALLERS / f"neoforge-{p['neoforge_version']}-installer.jar"
        cmd = [JAVA, "-jar", str(installer), "--installClient", str(root)]
    if not installer.exists():
        raise SystemExit(f"missing installer {installer}")
    subprocess.run(cmd, cwd=root, check=True)
    return root


def profile_id(root: Path, loader: str, mc: str) -> str:
    p = props(mc)
    wanted = f"fabric-loader-{p['fabric_loader_version']}-{mc}" if loader == "fabric" else f"neoforge-{p['neoforge_version']}"
    if not (root / "versions" / wanted / f"{wanted}.json").exists():
        raise SystemExit(f"profile {wanted} is not installed (run: client.py install {loader} {mc})")
    return wanted


def allowed(rules: list | None) -> bool:
    if not rules:
        return True
    result = False
    for rule in rules:
        os_rule = rule.get("os")
        if "features" in rule:
            matches = False
        elif os_rule:
            matches = os_rule.get("name") in (None, "windows") and os_rule.get("arch") in (None, "x86_64", "x64")
        else:
            matches = True
        if matches:
            result = rule["action"] == "allow"
    return result


def resolve(root: Path, version: str) -> dict:
    data = json.loads((root / "versions" / version / f"{version}.json").read_text(encoding="utf-8"))
    if "inheritsFrom" not in data:
        return data
    parent = resolve(root, data["inheritsFrom"])
    merged = dict(parent)
    merged["id"] = data["id"]
    merged["mainClass"] = data.get("mainClass", parent["mainClass"])
    # The child's libraries win over the parent's for the same group:artifact (Fabric and NeoForge pin ASM etc.).
    child_keys = {":".join(lib["name"].split(":")[:2]) + (":" + lib["name"].split(":")[3] if lib["name"].count(":") > 2 else "")
                  for lib in data.get("libraries", [])}
    kept = []
    for lib in parent.get("libraries", []):
        parts = lib["name"].split(":")
        key = ":".join(parts[:2]) + (":" + parts[3] if len(parts) > 3 else "")
        if key not in child_keys:
            kept.append(lib)
    merged["libraries"] = data.get("libraries", []) + kept
    args = {"game": list(parent.get("arguments", {}).get("game", [])), "jvm": list(parent.get("arguments", {}).get("jvm", []))}
    for kind in ("game", "jvm"):
        args[kind] += data.get("arguments", {}).get(kind, [])
    merged["arguments"] = args
    merged["jar"] = data.get("jar", parent.get("jar", parent["id"]))
    return merged


def library_path(lib: dict) -> tuple[Path, str | None, str | None]:
    artifact = lib.get("downloads", {}).get("artifact")
    if artifact:
        return Path(artifact["path"]), artifact.get("url") or None, artifact.get("sha1")
    group, name, version, *classifier = lib["name"].split(":")
    file = f"{name}-{version}{'-' + classifier[0] if classifier else ''}.jar"
    rel = Path(*group.split(".")) / name / version / file
    base = lib.get("url", "https://libraries.minecraft.net/")
    return rel, base.rstrip("/") + "/" + rel.as_posix(), None


def classpath(root: Path, profile: dict) -> list[Path]:
    paths = []
    for lib in profile["libraries"]:
        if not allowed(lib.get("rules")):
            continue
        rel, url, sha1 = library_path(lib)
        dest = root / "libraries" / rel
        if not dest.exists():
            if not url:
                raise SystemExit(f"library {lib['name']} is missing and has no download url")
            fetch(url, dest, sha1)
        paths.append(dest)
    client = root / "versions" / profile["jar"] / f"{profile['jar']}.jar"
    vanilla = json.loads((root / "versions" / profile["jar"] / f"{profile['jar']}.json").read_text(encoding="utf-8"))
    fetch(vanilla["downloads"]["client"]["url"], client, vanilla["downloads"]["client"]["sha1"])
    if not any(p.name == client.name for p in paths) and profile["mainClass"].startswith("net.fabricmc"):
        paths.append(client)
    elif profile["mainClass"] == "net.minecraft.client.main.Main":
        paths.append(client)
    return paths


def ensure_assets(profile: dict) -> str:
    index = profile["assetIndex"]
    path = fetch(index["url"], ASSETS / "indexes" / f"{index['id']}.json", index["sha1"])
    objects = json.loads(path.read_text(encoding="utf-8"))["objects"]
    for obj in objects.values():
        h = obj["hash"]
        dest = ASSETS / "objects" / h[:2] / h
        if not dest.exists():
            fetch(f"https://resources.download.minecraft.net/{h[:2]}/{h}", dest, h)
    return index["id"]


def substitute(arg: str, values: dict) -> str:
    for key, value in values.items():
        arg = arg.replace("${" + key + "}", value)
    return arg


def flatten(entries: list) -> list[str]:
    out = []
    for entry in entries:
        if isinstance(entry, str):
            out.append(entry)
        elif allowed(entry.get("rules")):
            value = entry["value"]
            out += value if isinstance(value, list) else [value]
    return out


def mods_for(loader: str, mc: str, jar: str) -> list[Path]:
    p = props(mc)
    gl = glob.glob(str(MODULES / "maven.modrinth" / "geckolib" / p[f"geckolib_{loader}_id"] / "*" / "*.jar"))
    mods = [Path(jar).resolve(), Path(gl[0])]
    mods += [Path(j) for j in glob.glob(str(ROOT / mc / loader / "libs" / f"jauml-{loader}-*.jar"))]
    if loader == "fabric":
        mods.append(cached("net.fabricmc.fabric-api", "fabric-api", p["fabric_version"]))
    return mods


def launch(loader: str, mc: str, jar: str, server: str | None, width: int, height: int) -> subprocess.Popen:
    root = RUNS / f"client-{loader}-{mc}"
    version = profile_id(root, loader, mc)
    profile = resolve(root, version)
    game = root / "game"
    mods = game / "mods"
    for stale in (mods, game / "screenshots"):
        if stale.exists():
            shutil.rmtree(stale)
    mods.mkdir(parents=True)
    for m in mods_for(loader, mc, jar):
        shutil.copy2(m, mods / m.name)
    # Skip the first-run screens (accessibility onboarding, narrator) and keep the window small and unpaused.
    (game / "options.txt").write_text(f"onboardAccessibility:false\nnarrator:0\npauseOnLostFocus:false\nrenderDistance:8\n"
                                      f"guiScale:2\noverrideWidth:{width}\noverrideHeight:{height}\nskipMultiplayerWarning:true\n"
                                      "joinedFirstServer:true\ntutorialStep:none\nsoundCategory_master:0.0\n")
    cp = classpath(root, profile)
    asset_index = ensure_assets(profile)
    natives = root / "natives"
    natives.mkdir(exist_ok=True)
    values = {
        "auth_player_name": USERNAME, "version_name": version, "game_directory": str(game),
        "assets_root": str(ASSETS), "assets_index_name": asset_index,
        "auth_uuid": str(uuid.uuid3(uuid.NAMESPACE_OID, "OfflinePlayer:" + USERNAME)).replace("-", ""),
        "auth_access_token": "0", "clientid": "", "auth_xuid": "", "user_type": "msa", "version_type": "release",
        "natives_directory": str(natives), "launcher_name": "remnant-prod-boot", "launcher_version": "1",
        "classpath": os.pathsep.join(str(p) for p in cp), "library_directory": str(root / "libraries"),
        "classpath_separator": os.pathsep, "resolution_width": str(width), "resolution_height": str(height),
        "quickPlayMultiplayer": server or "", "user_properties": "{}",
    }
    jvm = [substitute(a, values) for a in flatten(profile["arguments"]["jvm"])]
    game_args = [substitute(a, values) for a in flatten(profile["arguments"]["game"])]
    game_args += ["--width", str(width), "--height", str(height)]
    if server:
        game_args += ["--quickPlayMultiplayer", server]
    cmd = [JAVA, "-Xmx4G"] + jvm + [profile["mainClass"]] + game_args
    log = open(ROOT / f"prod-client-{loader}-{mc}.log", "w", encoding="utf-8")
    print("client mods:", ", ".join(sorted(p.name for p in mods.iterdir())))
    return subprocess.Popen(cmd, cwd=game, stdout=log, stderr=subprocess.STDOUT)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="action", required=True)
    i = sub.add_parser("install")
    i.add_argument("loader", choices=["fabric", "neoforge"])
    i.add_argument("mc", choices=["26.2", "26.3"])
    l = sub.add_parser("launch")
    l.add_argument("loader", choices=["fabric", "neoforge"])
    l.add_argument("mc", choices=["26.2", "26.3"])
    l.add_argument("jar")
    l.add_argument("--server")
    l.add_argument("--width", type=int, default=1280)
    l.add_argument("--height", type=int, default=720)
    args = ap.parse_args()
    if args.action == "install":
        print("installed into", install(args.loader, args.mc))
        return 0
    proc = launch(args.loader, args.mc, args.jar, args.server, args.width, args.height)
    return proc.wait()


if __name__ == "__main__":
    sys.exit(main())
