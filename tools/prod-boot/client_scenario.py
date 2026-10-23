#!/usr/bin/env python3
"""Production client walkthrough: a real 26.x server and a real installed client, driven by console commands.

  python tools/prod-boot/client_scenario.py fabric 26.2 26.2/fabric/build/libs/remnant_bosses-fabric-26.2-3.1.0.jar

The server (installed_server.py's folder) starts on a fresh world with the probe player as operator; the client
(client.py's install) joins it through Quick Play. Each step of tools/prod-boot/client_steps.txt is a console command,
a wait (`wait <seconds>`), `debug` (toggles the F3 overlay; GLFW clients only) or a capture (`shot <name>`: the game's
own F2 screenshot, posted to its window, or on SDL clients (26.3) a copy of the game window's own pixels, so nothing
else on the screen is ever recorded); captures go to .gradle/prod-boot/shots/<loader>-<mc>/.
Afterwards both logs are scanned: the client log must show no mixin, render, model, texture or resource error.
"""
from __future__ import annotations

import argparse
import ctypes
import ctypes.wintypes
import json
import re
import subprocess
import sys
import threading
import time
import uuid
from pathlib import Path

import client
from prodboot import ROOT, RUNS, prepare, props
from installed_server import JAVA, KEEP, modrinth

CLIENT_FAILURE = re.compile(
    r"Mixin apply failed|InvalidMixinException|MixinApplyError|Missing model|Unable to load model|Failed to load model|"
    r"missing texture|Missing textures|Unable to resolve texture|Failed to load texture|Couldn't load|Exception in|"
    r"Unreported exception|Rendering entity|Rendering block entity|crash-reports|Caught previously unhandled|"
    r"Failed to (load|parse|create)|Encountered an unexpected exception|Error loading|ModLoadingException|"
    r"Could not execute entrypoint|Unknown (item|block|entity|particle)|No model for|not found in atlas|"
    r"remnant_bosses.*(ERROR|WARN)|(ERROR|WARN).*remnant_bosses|(ERROR|WARN).*geckolib|NullPointerException|"
    r"IllegalStateException|IllegalArgumentException|ClassCastException|NoSuchMethodError|NoSuchFieldError|"
    r"AbstractMethodError|IncompatibleClassChangeError", re.I)
# Noise from the offline launch and the OS, not from the game or the mod.
CLIENT_IGNORED = re.compile(r"Perflib|counter names|Failed to fetch user properties|Couldn't connect to realms|"
                            r"realms|telemetry|YggdrasilUserApiService|Failed to verify authentication|profile key|"
                            r"Narrator|Missing sound for event|Unable to initialize OpenAL|GLFW error 65543|"
                            r"Failed to retrieve profile key pair|Failed to fetch Realms|oshi|Failed to get system info|Win32Exception|"
                            r"InvalidCredentialsException|Status: 401|SignedJWT|MinecraftClientHttpException|RealmsServiceException|"
                            # netty's macOS/Linux transports probed on Windows; first-run config writes on NeoForge.
                            r"Only supported on (OSX|Linux)|to run a deferred task|Connection reset|Exception caught in connection", re.I)

user32 = ctypes.windll.user32
gdi32 = ctypes.windll.gdi32
WM_KEYDOWN, WM_KEYUP, VK_F2, VK_F3 = 0x0100, 0x0101, 0x71, 0x72
PW_CLIENTONLY, PW_RENDERFULLCONTENT = 1, 2
SW_SHOWNOACTIVATE, SW_SHOWMINNOACTIVE, HWND_BOTTOM = 4, 7, 1
SWP_NOSIZE, SWP_NOMOVE, SWP_NOACTIVATE = 0x1, 0x2, 0x10
for fn, res, args in [(user32.SetWindowPos, ctypes.c_bool, [ctypes.c_void_p, ctypes.c_void_p, ctypes.c_int, ctypes.c_int,
                                                             ctypes.c_int, ctypes.c_int, ctypes.c_uint]),
                      (user32.GetDC, ctypes.c_void_p, [ctypes.c_void_p]),
                      (user32.ReleaseDC, ctypes.c_int, [ctypes.c_void_p, ctypes.c_void_p]),
                      (user32.PrintWindow, ctypes.c_bool, [ctypes.c_void_p, ctypes.c_void_p, ctypes.c_uint]),
                      (gdi32.CreateCompatibleDC, ctypes.c_void_p, [ctypes.c_void_p]),
                      (gdi32.CreateCompatibleBitmap, ctypes.c_void_p, [ctypes.c_void_p, ctypes.c_int, ctypes.c_int]),
                      (gdi32.SelectObject, ctypes.c_void_p, [ctypes.c_void_p, ctypes.c_void_p]),
                      (gdi32.GetDIBits, ctypes.c_int, [ctypes.c_void_p, ctypes.c_void_p, ctypes.c_uint, ctypes.c_uint,
                                                       ctypes.c_void_p, ctypes.c_void_p, ctypes.c_uint]),
                      (gdi32.DeleteObject, ctypes.c_bool, [ctypes.c_void_p]),
                      (gdi32.DeleteDC, ctypes.c_bool, [ctypes.c_void_p])]:
    fn.restype, fn.argtypes = res, args


def find_window(pid: int) -> int | None:
    found = []

    @ctypes.WINFUNCTYPE(ctypes.c_bool, ctypes.wintypes.HWND, ctypes.wintypes.LPARAM)
    def visit(hwnd, _):
        owner = ctypes.wintypes.DWORD()
        user32.GetWindowThreadProcessId(hwnd, ctypes.byref(owner))
        if owner.value == pid and user32.IsWindowVisible(hwnd):
            length = user32.GetWindowTextLengthW(hwnd)
            buf = ctypes.create_unicode_buffer(length + 1)
            user32.GetWindowTextW(hwnd, buf, length + 1)
            if buf.value.startswith("Minecraft"):
                found.append(hwnd)
        return True

    user32.EnumWindows(visit, 0)
    return found[0] if found else None


def window_pixels(hwnd: int, path: Path) -> bool:
    """The game window's own client area through PrintWindow: that window's content only, even when covered."""
    from PIL import Image

    rect = ctypes.wintypes.RECT()
    user32.GetClientRect(hwnd, ctypes.byref(rect))
    width, height = rect.right, rect.bottom
    if width <= 0 or height <= 0:
        return False
    screen = user32.GetDC(hwnd)
    memory = gdi32.CreateCompatibleDC(screen)
    bitmap = gdi32.CreateCompatibleBitmap(screen, width, height)
    previous = gdi32.SelectObject(memory, bitmap)
    try:
        if not user32.PrintWindow(hwnd, memory, PW_CLIENTONLY | PW_RENDERFULLCONTENT):
            return False
        # BITMAPINFOHEADER: top-down (negative height), 32 bits per pixel, uncompressed.
        header = (ctypes.c_uint32 * 10)(40, width, (-height) & 0xFFFFFFFF, 1 | (32 << 16), 0, 0, 0, 0, 0, 0)
        pixels = ctypes.create_string_buffer(width * height * 4)
        if gdi32.GetDIBits(memory, bitmap, 0, height, pixels, header, 0) != height:
            return False
    finally:
        gdi32.SelectObject(memory, previous)
        gdi32.DeleteObject(bitmap)
        gdi32.DeleteDC(memory)
        user32.ReleaseDC(hwnd, screen)
    image = Image.frombuffer("RGB", (width, height), pixels, "raw", "BGRX", 0, 1)
    if image.getextrema() == ((0, 0), (0, 0), (0, 0)):
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)
    return True


def unminimize(hwnd: int) -> bool:
    """A minimized game draws nothing. Show it again behind every other window, without focus; True if it was minimized."""
    if not user32.IsIconic(hwnd):
        return False
    user32.SetWindowPos(hwnd, HWND_BOTTOM, 0, 0, 0, 0, SWP_NOSIZE | SWP_NOMOVE | SWP_NOACTIVATE)
    user32.ShowWindow(hwnd, SW_SHOWNOACTIVATE)
    user32.SetWindowPos(hwnd, HWND_BOTTOM, 0, 0, 0, 0, SWP_NOSIZE | SWP_NOMOVE | SWP_NOACTIVATE)
    time.sleep(3)  # chunk meshes and the frame catch up
    return True


def capture(pid: int, screenshots: Path, path: Path, sdl: bool) -> bool:
    """The game's own F2 screenshot, posted straight to its window: only the game framebuffer, whatever is on screen.

    Since 26.3 the game reads input through SDL, which ignores keys posted to a window without keyboard focus (and
    focusing it would take the player's mouse). There the capture copies the game window's own pixels instead. A window
    someone minimized is shown behind everything else for the capture and minimized again afterwards.
    """
    hwnd = find_window(pid)
    if not hwnd:
        print("  no client window to capture")
        return False
    was_minimized = unminimize(hwnd)
    try:
        if not sdl:
            before = set(screenshots.glob("*.png")) if screenshots.exists() else set()
            scan = user32.MapVirtualKeyW(VK_F2, 0)
            user32.PostMessageW(hwnd, WM_KEYDOWN, VK_F2, 1 | (scan << 16))
            time.sleep(0.15)
            user32.PostMessageW(hwnd, WM_KEYUP, VK_F2, 1 | (scan << 16) | (0xC0 << 24))
            for _ in range(40):
                time.sleep(0.25)
                new = set(screenshots.glob("*.png")) - before if screenshots.exists() else set()
                if new:
                    time.sleep(0.5)
                    path.parent.mkdir(parents=True, exist_ok=True)
                    path.write_bytes(sorted(new)[-1].read_bytes())
                    print("  captured", path.name)
                    return True
        if window_pixels(hwnd, path):
            print("  captured", path.name, "(game window)")
            return True
        print("  the game wrote no screenshot for", path.name)
        return False
    finally:
        if was_minimized:
            user32.ShowWindow(hwnd, SW_SHOWMINNOACTIVE)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("loader", choices=["fabric", "neoforge"])
    ap.add_argument("mc", choices=["26.2", "26.3"])
    ap.add_argument("jar")
    ap.add_argument("--steps", default=str(ROOT / "tools" / "prod-boot" / "client_steps.txt"))
    ap.add_argument("--port", type=int, default=25670)
    args = ap.parse_args()

    p = props(args.mc)
    run = RUNS / f"{args.loader}-{args.mc}"
    mods = client.mods_for(args.loader, args.mc, args.jar)
    prepare(run, mods, args.port, keep=KEEP)
    player_uuid = str(uuid.uuid3(uuid.NAMESPACE_OID, "OfflinePlayer:" + client.USERNAME))
    (run / "ops.json").write_text(json.dumps([{"uuid": player_uuid, "name": client.USERNAME, "level": 4,
                                               "bypassesPlayerLimit": False}]))
    if args.loader == "fabric":
        cmd = [JAVA, "-Xmx3G", "-jar", "fabric-server-launch.jar", "nogui"]
    else:
        cmd = [JAVA, "-Xmx3G", f"@libraries/net/neoforged/neoforge/{p['neoforge_version']}/win_args.txt", "nogui"]
    server_log = ROOT / f"prod-scenario-server-{args.loader}-{args.mc}.log"
    server = subprocess.Popen(cmd, cwd=run, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                              text=True, encoding="utf-8", errors="replace")
    lines: list[str] = []
    ready, joined = threading.Event(), threading.Event()

    def pump():
        with open(server_log, "w", encoding="utf-8") as log:
            for line in server.stdout:
                lines.append(line)
                log.write(line)
                log.flush()
                if "Done (" in line:
                    ready.set()
                if f"{client.USERNAME} joined the game" in line:
                    joined.set()
        ready.set()

    threading.Thread(target=pump, daemon=True).start()
    if not ready.wait(900) or server.poll() is not None:
        print("FAIL: server did not start")
        return 1
    print("server up; launching client")
    game = client.launch(args.loader, args.mc, args.jar, f"localhost:{args.port}", 1280, 720)
    game_dir = RUNS / f"client-{args.loader}-{args.mc}" / "game"
    shots = RUNS / "shots" / f"{args.loader}-{args.mc}"
    for old in shots.glob("*.png") if shots.exists() else []:
        old.unlink()
    sdl = args.mc != "26.2"  # 26.3 replaced GLFW with SDL
    ok = True
    if not joined.wait(600):
        print("FAIL: the client never joined the server")
        ok = False
    else:
        print("client joined; running steps")
        hwnd = find_window(game.pid)
        if hwnd:  # out of the way: behind every other window, still drawing
            user32.SetWindowPos(hwnd, HWND_BOTTOM, 0, 0, 0, 0, SWP_NOSIZE | SWP_NOMOVE | SWP_NOACTIVATE)
        time.sleep(8)
        for raw in Path(args.steps).read_text(encoding="utf-8").splitlines():
            step = raw.strip()
            if not step or step.startswith("#"):
                continue
            if game.poll() is not None:
                print("FAIL: the client exited during the walkthrough")
                ok = False
                break
            if step.startswith("wait "):
                time.sleep(float(step.split()[1]))
            elif step == "debug":
                hwnd = find_window(game.pid)
                if hwnd:
                    scan = user32.MapVirtualKeyW(VK_F3, 0)
                    user32.PostMessageW(hwnd, WM_KEYDOWN, VK_F3, 1 | (scan << 16))
                    time.sleep(0.15)
                    user32.PostMessageW(hwnd, WM_KEYUP, VK_F3, 1 | (scan << 16) | (0xC0 << 24))
            elif step.startswith("shot "):
                capture(game.pid, game_dir / "screenshots", shots / f"{step.split()[1]}.png", sdl)
            else:
                server.stdin.write(step.replace("@probe", client.USERNAME) + "\n")
                server.stdin.flush()
                time.sleep(1.5)
    alive = game.poll() is None
    if alive:
        game.terminate()
        try:
            game.wait(30)
        except subprocess.TimeoutExpired:
            game.kill()
    server.stdin.write("stop\n")
    server.stdin.flush()
    try:
        server.wait(180)
    except subprocess.TimeoutExpired:
        server.kill()

    client_log = (ROOT / f"prod-client-{args.loader}-{args.mc}.log").read_text(encoding="utf-8", errors="replace").splitlines()
    problems = [l for l in client_log if CLIENT_FAILURE.search(l) and not CLIENT_IGNORED.search(l)]
    # Besides OS and network noise: vanilla 26.2 itself reports its deep dark sculk patches near this seed's spawn.
    server_problems = [l.rstrip() for l in lines if re.search(r"ERROR|Exception|Mixin apply failed", l)
                       and not re.search(r"Perflib|counter|Win32Exception|oshi|kqueue|epoll|Appender DebugFile|"
                                 r"Only supported on (OSX|Linux)|Connection reset|Exception caught in connection|"
                                 r"unsafe terrain read.*minecraft:sculk_patch_deep_dark\]", l)]
    print(f"client alive at the end: {alive}; client problem lines: {len(problems)}; server problem lines: {len(server_problems)}")
    for l in problems[:40]:
        print("  client:", l[:300])
    for l in server_problems[:20]:
        print("  server:", l[:300])
    print("captures:", ", ".join(sorted(s.name for s in shots.glob("*.png"))) if shots.exists() else "none")
    return 0 if ok and alive and not problems and not server_problems else 1


if __name__ == "__main__":
    sys.exit(main())
