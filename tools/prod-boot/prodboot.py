"""Shared runner for the production boot checks: start a server, wait for "Done", run console commands, stop it,
and fail on any mixin, datapack or startup error in the log."""
from __future__ import annotations

import glob
import re
import shutil
import subprocess
import threading
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CACHE = Path.home() / ".gradle" / "caches"
MODULES = CACHE / "modules-2" / "files-2.1"
RUNS = ROOT / ".gradle" / "prod-boot"

FAILURE = re.compile(r"Mixin apply failed|InvalidMixinException|MixinApplyError|was not located|Failed to load datapacks|"
                     r"Error loading registry data|Encountered an unexpected exception|Exception in server tick loop|"
                     r"Could not execute entrypoint|Incompatible mods found|Missing or unsupported mandatory dependencies|"
                     r"crash-reports|ModLoadingException|Failed to create mod instance", re.I)


def props(mc: str) -> dict:
    out = {}
    for line in (ROOT / mc / "gradle.properties").read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.startswith("#"):
            k, v = line.split("=", 1)
            out[k.strip()] = v.strip()
    return out


def cached(group: str, artifact: str, version: str, classifier: str = "") -> Path:
    name = f"{artifact}-{version}-{classifier}.jar" if classifier else f"{artifact}-{version}.jar"
    hits = [Path(p) for p in glob.glob(str(MODULES / group / artifact / version / "*" / name))]
    if not hits:
        raise SystemExit(f"not in the Gradle cache: {group}:{artifact}:{version} (build the workspace once first)")
    return hits[0]


def java(major: int) -> str:
    if major == 17:
        jdks = sorted(glob.glob(str(Path.home() / ".gradle" / "jdks" / "eclipse_adoptium-17*" / "bin" / "java.exe")))
        if jdks:
            return jdks[0]
    return "java"


def prepare(run: Path, mods: list[Path], port: int, keep: tuple = ()) -> None:
    """Fresh mods/, world and config in the server folder; anything named in keep survives."""
    for child in list(run.iterdir()) if run.exists() else []:
        if child.name in keep:
            continue
        shutil.rmtree(child) if child.is_dir() else child.unlink()
    (run / "mods").mkdir(parents=True, exist_ok=True)
    for jar in mods:
        shutil.copy2(jar, run / "mods" / jar.name)
    (run / "eula.txt").write_text("eula=true\n")
    (run / "server.properties").write_text(
        f"server-port={port}\nonline-mode=false\nlevel-seed=20261019\nview-distance=4\nsimulation-distance=4\n"
        # 26.3 servers whitelist by default; the probe client has to be able to join.
        "spawn-protection=0\nmax-tick-time=-1\nwhite-list=false\n")


def boot(cmd: list[str], run: Path, log_path: Path, commands: list[str], timeout: int = 900) -> int:
    print("mods:", ", ".join(sorted(p.name for p in (run / "mods").iterdir())))
    proc = subprocess.Popen(cmd, cwd=run, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            text=True, encoding="utf-8", errors="replace")
    lines: list[str] = []
    done = threading.Event()

    def pump():
        with open(log_path, "w", encoding="utf-8") as log:
            for line in proc.stdout:
                lines.append(line)
                log.write(line)
                log.flush()
                if "Done (" in line:
                    done.set()
        done.set()

    threading.Thread(target=pump, daemon=True).start()
    started = time.time()
    while not done.wait(1):
        if time.time() - started > timeout:
            proc.kill()
            print("FAIL: server did not finish starting in time")
            return 1
    up = any("Done (" in line for line in lines)
    if up:
        for command in commands:
            proc.stdin.write(command + "\n")
            proc.stdin.flush()
            time.sleep(3)
        time.sleep(5)
        proc.stdin.write("stop\n")
        proc.stdin.flush()
    try:
        proc.wait(timeout=180)
    except subprocess.TimeoutExpired:
        proc.kill()
    failures = [line.rstrip() for line in lines if FAILURE.search(line)]
    print(f"log: {log_path}")
    print(f"started: {up}; exit code {proc.returncode}; failure lines: {len(failures)}")
    for line in failures[:15]:
        print("  " + line)
    return 0 if up and not failures else 1


def read_commands(path: str | None) -> list[str]:
    if not path:
        return []
    return [c.strip() for c in Path(path).read_text(encoding="utf-8").splitlines() if c.strip() and not c.startswith("#")]
