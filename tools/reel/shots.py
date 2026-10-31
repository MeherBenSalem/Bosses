#!/usr/bin/env python3
"""Shot jobs for the Remnant Bosses reel, written as JSON into the reel director's queue.

  python3 tools/reel/shots.py list
  python3 tools/reel/shots.py queue NAME [NAME ...]     # or a prefix ending in '*'

Coordinates are in the director's world ("ReelWorld", seed 20261019), found with the director's scan jobs:
  Hollow Citadel throne hall: altar (7,125,-386), fog gate x=21 (z -388..-384)
  Forgotten Observatory dome: altar (-462,129,78), fog gate x=-475 (z 76..80)
  Gravebound Catacombs chain chapel: altar (199,-5,-11), fog gate x=181 (z -13..-9)
  Grace shrine: ember (115,77,205); a flat field south of it around (95,71,243)
  Overworld meadow plateau around (11,111,-186)
Yaw 0 faces south (+z), 90 west, 180 north, -90 east. Minecraft's FOV is vertical; a 9:16 frame is only ~45 degrees wide.
"""
from __future__ import annotations

import json
import math
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JOBS = ROOT / "1.21.1" / "fabric" / "runs" / "reel" / "reel" / "jobs"

REALM = "remnant_bosses:eclipsed_realm"
OVERWORLD = "minecraft:overworld"
RB = "remnant_bosses:"
R = "remnants:"
SOVEREIGN, WARDEN, WATCHER, OSSUKAGE = R + "hollow_sovereign", R + "gravebound_warden", R + "forgotten_watcher", R + "ossukage"

SHOTS: dict[str, dict] = {}


def shot(name: str, **job) -> None:
    job.setdefault("dimension", REALM)
    job["name"] = name
    SHOTS[name] = job


def summon(entity: str, x: float, y: float, z: float, yaw: float, tag: str = "reel_boss", ai: bool = False, extra: str = "") -> str:
    """Bosses are summoned without AI: they hold their mark and attack only when a shot tells them to."""
    nbt = f'Tags:["{tag}"],Rotation:[{yaw}f,0f],PersistenceRequired:1b'
    if not ai:
        nbt += ",NoAI:1b"
    if extra:
        nbt += "," + extra
    return f"summon {entity} {x} {y} {z} {{{nbt}}}"


def stand(x, y, z, yaw, head, chest, legs, feet, hand, tag="reel_stand") -> str:
    items = ",".join(f'{{id:"{i}",count:1}}' if i else "{}" for i in (feet, legs, chest, head))
    held = f'{{id:"{hand}",count:1}}' if hand else "{}"
    return (f'summon minecraft:armor_stand {x} {y} {z} {{Tags:["{tag}"],ShowArms:1b,NoBasePlate:1b,Rotation:[{yaw}f,0f],'
            f'ArmorItems:[{items}],HandItems:[{held},{{}}],Pose:{{RightArm:[-20f,0f,10f],LeftArm:[-10f,0f,-10f]}}}}')


def key(t, pos, look=None, tag=None, off=None, rot=None, fov=None, roll=None) -> dict:
    k = {"t": t, "pos": pos}
    for name, value in (("look", look), ("lookTag", tag), ("lookOffset", off), ("rot", rot), ("fov", fov), ("roll", roll)):
        if value is not None:
            k[name] = value
    return k


def ev(t, **kw) -> dict:
    kw["t"] = t
    return kw


def boss_ev(t, **kw) -> dict:
    kw.setdefault("tag", "reel_boss")
    return ev(t, **kw)


def arc(center, radius, a0, a1, y, n, t0, t1, look, fov, fov1=None) -> list[dict]:
    """Keys on a horizontal circular arc around center (angles in degrees, 0 = +x, 90 = +z)."""
    keys = []
    for i in range(n):
        u = i / (n - 1)
        a = math.radians(a0 + (a1 - a0) * u)
        pos = [round(center[0] + radius * math.cos(a), 3), y, round(center[2] + radius * math.sin(a), 3)]
        f = fov if fov1 is None else fov + (fov1 - fov) * u
        keys.append(key(round(t0 + (t1 - t0) * u, 3), pos, look=look, fov=round(f, 2)))
    return keys


def dolly(cam, look, fov, dur, push=0.6, side=0.0, rise=0.0, fov1=None) -> list[dict]:
    """Two keys: the pose, then the camera pushed `push` blocks toward the look point and slid `side` blocks right."""
    d = [look[i] - cam[i] for i in range(3)]
    n = math.sqrt(sum(c * c for c in d))
    f = [c / n for c in d]
    rn = math.hypot(f[2], f[0]) or 1
    r = [-f[2] / rn, 0, f[0] / rn]
    end = [round(cam[i] + f[i] * push + r[i] * side + (rise if i == 1 else 0), 3) for i in range(3)]
    return [key(0, list(cam), look=list(look), fov=fov), key(dur, end, look=list(look), fov=fov if fov1 is None else fov1)]


SETS = {
    "regalia": (RB + "hollow_crown", RB + "regalia_cuirass", RB + "regalia_greaves", RB + "regalia_sabatons", RB + "sovereigns_ruin"),
    "warden": (RB + "warden_helm", RB + "warden_hauberk", RB + "warden_tassets", RB + "warden_boots", RB + "gravebound_executioner"),
    "archivist": (RB + "archivist_hood", RB + "archivist_robe", RB + "archivist_leggings", RB + "archivist_boots", RB + "memory_staff"),
}
CAMERA_ONLY = ["effect give @p night_vision infinite 0 true", "effect give @p resistance infinite 4 true"]


def hero(kit: str, knockback: bool = False) -> list[str]:
    """The player as the hero: unhurt by anything, in one of the boss armour sets, holding that set's weapon."""
    head, chest, legs, feet, weapon = SETS[kit]
    return ["effect give @p night_vision infinite 0 true", "effect give @p resistance infinite 4 true",
            "effect give @p regeneration infinite 4 true", "effect give @p saturation infinite 0 true",
            "attribute @p minecraft:generic.knockback_resistance base set " + ("0" if knockback else "1"),
            f"item replace entity @p weapon.mainhand with {weapon}", f"item replace entity @p armor.head with {head}",
            f"item replace entity @p armor.chest with {chest}", f"item replace entity @p armor.legs with {legs}",
            f"item replace entity @p armor.feet with {feet}"]


# The hero faces the boss throughout; the boss chooses no attack of its own ("hold") but targets the hero.
FACE_BOSS = [ev(-20, face="reel_boss", faceOffset=[0, 0.4, 0]), boss_ev(-20, hold=True),
             boss_ev(-19.5, invoke="setTarget", args=["@player"])]

# ---------------------------------------------------------------- the Hollow Sovereign (throne hall)

HALL_BOSS = (12.5, 125, -386.5)
SOV = dict(settle=5, renderDistance=10, hud="boss", hidePlayer=False, gamemode="survival", playerRot=[90, 0])
SOV_SETUP = hero("archivist") + [summon(SOVEREIGN, *HALL_BOSS, -90)]

# The hook: over the hero's shoulder, the throne slam lands in slow motion.
shot("sov1_slam", duration=3.4, preroll=3.2, timing="linear", playerPos=[16.6, 125, -385.0], **SOV, setup=SOV_SETUP,
     events=FACE_BOSS + [boss_ev(0.15, attack="throne_slam")],
     speed=[{"t": 0, "v": 1}, {"t": 1.0, "v": 1}, {"t": 1.35, "v": 0.28}, {"t": 2.1, "v": 0.28}, {"t": 2.6, "v": 1}],
     keys=[key(0, [19.9, 126.1, -382.3], look=[12.0, 128.2, -387.2], fov=64),
           key(3.4, [19.3, 126.0, -382.8], look=[12.0, 128.5, -387.2], fov=56)])
# Phase two: the pauldrons tear loose. Slow low orbit.
shot("sov2_shatter", duration=4.5, preroll=3.2, timing="smooth", playerPos=[18.5, 125, -386.5], **SOV,
     setup=hero("warden") + [summon(SOVEREIGN, *HALL_BOSS, -90)], events=FACE_BOSS + [boss_ev(0.1, health=0.6)],
     keys=arc(HALL_BOSS, 6.6, 58, 22, 126.2, 5, 0, 4.5, [12.5, 128.4, -386.5], 66, 60))
# Phase three: the soul bursts from the breastplate.
shot("sov3_soul", duration=4.5, preroll=7.5, timing="smooth", playerPos=[17.5, 125, -386.5], **SOV, setup=SOV_SETUP,
     events=FACE_BOSS + [boss_ev(-4.0, health=0.6), boss_ev(-0.2, health=0.3)],
     keys=dolly([20.0, 127.5, -385.0], [13.0, 127.6, -386.5], 64, 4.5, push=0.9, side=0.4, fov1=58))
# The Last Command: he kneels, his guards rise and blades of light fall around the hero.
shot("sov4_last_command", duration=6.5, preroll=10.0, timing="smooth", playerPos=[16.0, 125, -386.5], **SOV, setup=SOV_SETUP,
     events=FACE_BOSS + [boss_ev(-9.5, health=0.6), boss_ev(-6.3, health=0.3), boss_ev(-0.3, health=0.08)],
     keys=dolly([18.0, 130.0, -386.9], [13.9, 126.0, -386.5], 72, 6.5, push=0.8, rise=-0.6, fov1=68))

# ---------------------------------------------------------------- the Gravebound Warden (chain chapel)

W_BOSS = [193.5, -5, -11.5]
WAR = dict(settle=5, renderDistance=8, hud="boss", hidePlayer=False, gamemode="survival", playerPos=[187.5, -5, -11.5],
           playerRot=[-90, 0])
WAR_SETUP = hero("regalia") + [summon(WARDEN, *W_BOSS, 90)]

shot("war1_grab", duration=4.0, preroll=3.0, timing="smooth", **WAR,
     setup=hero("regalia", knockback=True) + [summon(WARDEN, *W_BOSS, 90)],
     events=FACE_BOSS + [boss_ev(0.3, attack="chain_grab")],
     keys=dolly([184.15, -2.8, -12.5], [192.3, -3.85, -11.5], 64, 4.0, push=0.6, side=0.3))
shot("war2_eruption", duration=4.0, preroll=3.0, timing="smooth", **WAR, setup=WAR_SETUP,
     events=FACE_BOSS + [boss_ev(0.2, attack="eruption")],
     keys=dolly([184.0, -2.5, -10.5], [191.1, -3.85, -11.5], 62, 4.0, push=0.6, side=0.3))
shot("war4_walls", duration=6.0, preroll=4.5, timing="smooth", **WAR, setup=WAR_SETUP,
     events=FACE_BOSS + [boss_ev(-0.2, health=0.45), boss_ev(3.2, attack="chain_wall")],
     keys=dolly([184.75, -0.29, -11.25], [193.5, -4.0, -11.5], 76, 6.0, push=1.2, side=0.5))

# ---------------------------------------------------------------- the Forgotten Watcher (observatory dome)

X_BOSS = [-462.5, 130.5, 78.5]
WAT = dict(settle=5, renderDistance=8, hud="boss", hidePlayer=False, gamemode="survival", playerPos=[-470.5, 129, 78.5],
           playerRot=[-90, 0])
WAT_SETUP = hero("regalia") + [summon(WATCHER, *X_BOSS, 90)]

shot("wat1_volley", duration=3.5, preroll=3.0, timing="smooth", **WAT, setup=WAT_SETUP,
     events=FACE_BOSS + [boss_ev(0.25, attack="volley")],
     keys=dolly([-457.6, 130.3, 74.2], [-462.5, 132.0, 78.5], 72, 3.5, push=0.5, side=0.5, fov1=68))
shot("wat2_beam", duration=4.5, preroll=3.0, timing="smooth", **WAT, setup=WAT_SETUP,
     events=FACE_BOSS + [boss_ev(0.2, attack="beam")],
     keys=dolly([-456.6, 133.6, 80.8], [-470.5, 129.6, 78.5], 64, 4.5, push=0.4, side=-0.5))
shot("wat3_aegis", duration=5.0, preroll=4.5, timing="smooth", **WAT, setup=WAT_SETUP,
     events=FACE_BOSS + [boss_ev(-4.0, health=0.45), boss_ev(0.3, attack="shield")],
     keys=arc(X_BOSS, 5.6, -50, 20, 131.4, 5, 0, 5.0, [-462.5, 132.0, 78.5], 70, 66))

# ---------------------------------------------------------------- the Waygate (an overworld meadow at sunset)

MEADOW_FLAT = ["fill 0 110 -200 22 110 -175 minecraft:grass_block", "fill 0 111 -200 22 118 -175 minecraft:air",
               "fill 0 109 -200 22 109 -175 minecraft:dirt"]
# Placed by the real item, kindled with an Eclipse Shard, opening onto the eclipse.
shot("gate1_kindle", dimension=OVERWORLD, duration=9.0, preroll=0.5, settle=6, renderDistance=12, hud="none", hidePlayer=False,
     gamemode="survival", playerPos=[11.5, 111, -189.5], playerRot=[180, 0], timing="smooth",
     setup=["time set 12650", "weather clear"] + MEADOW_FLAT + hero("regalia"),
     events=[ev(-0.4, face=""), ev(0.2, useOn=[11, 110, -195], useItem=RB + "eclipse_waygate"),
             ev(1.2, useOn=[11, 112, -195], useItem=RB + "eclipse_shard", useFace="south")],
     keys=[key(0, [15.6, 111.6, -186.4], look=[11.0, 113.2, -195.0], fov=62),
           key(9.0, [13.4, 111.9, -189.2], look=[11.0, 113.0, -195.0], fov=58)])

# ---------------------------------------------------------------- the Eclipsed Realm

shot("realm1_gracetree", duration=6.0, settle=10, renderDistance=16, hud="none", gamemode="spectator", timing="smoother",
     setup=CAMERA_ONLY, keys=[key(0, [33.0, 131.0, -334.0], look=[18.0, 136.0, -386.0], fov=70),
                              key(6.0, [32.0, 153.0, -302.0], rot=[180, -14], fov=72)])
shot("realm2_eclipse", duration=5.0, settle=8, renderDistance=14, hud="none", gamemode="spectator", timing="smoother",
     setup=CAMERA_ONLY, keys=[key(0, [40.0, 146.0, -305.0], rot=[10, 12], fov=70), key(5.0, [40.0, 147.5, -300.0], rot=[2, -52], fov=66)])
shot("realm3_flyover", duration=4.5, settle=10, renderDistance=16, hud="none", gamemode="spectator", timing="linear",
     setup=CAMERA_ONLY, keys=[key(0, [44.0, 142.0, -262.0], look=[20.0, 142.0, -386.0], fov=74),
                              key(4.5, [30.0, 139.0, -318.0], look=[18.0, 141.0, -386.0], fov=70)])
# The hero rests at the grace; the mod's own GRACE DISCOVERED banner.
shot("grace1_rest", duration=5.0, settle=8, renderDistance=12, hud="boss", hudOffset=0, hidePlayer=False, gamemode="survival",
     playerPos=[115.5, 77, 208.5], playerRot=[180, 30], timing="smooth",
     setup=hero("regalia") + ["item replace entity @p weapon.mainhand with minecraft:air"],
     events=[ev(-0.5, face=""), ev(0.6, useOn=[115, 77, 205], useFace="south"), ev(0.75, closeScreen=True),
             ev(0.8, banner="realm.remnant_bosses.grace_discovered", bannerStyle="DISCOVERED")],
     keys=[key(0, [121.5, 79.2, 215.0], look=[115.0, 80.5, 205.0], fov=70),
           key(5.0, [119.0, 80.6, 216.5], look=[115.0, 82.0, 205.0], fov=68)])

# ---------------------------------------------------------------- Ossukage, on the field below the Gracetree

# The field's short grass stands as tall as a low camera; the rite's altar and pedestals stay behind after the rite.
FIELD_CLEAR = ["fill 75 71 223 115 73 263 minecraft:air replace minecraft:short_grass"]
RITE_CLEAR = ["fill 91 71 239 99 72 247 minecraft:air"]
SKULLS = ["minecraft:skeleton_skull[rotation=4]", "minecraft:skeleton_skull[rotation=12]",
          "minecraft:skeleton_skull[rotation=8]", "minecraft:skeleton_skull[rotation=0]"]


def ritual(x, y, z, tops) -> list[str]:
    """An Ancient Altar with its four pedestals three blocks out and an offering on each (east, west, south, north)."""
    cmds = [f"setblock {x} {y} {z} {RB}ancient_altar"]
    for (dx, dz), block in zip(((3, 0), (-3, 0), (0, 3), (0, -3)), tops):
        cmds += [f"setblock {x + dx} {y} {z + dz} {RB}ancient_pedestal", f"setblock {x + dx} {y + 1} {z + dz} {block}"]
    return cmds


# The rite (a Nether Star on the altar): lightning on every skull, then Ossukage rises with his soldiers. The rite
# resets the overworld clock to dawn, so it is staged in the Realm.
shot("oss1_ritual", duration=9.0, preroll=0.5, settle=6, renderDistance=12, hud="boss", hidePlayer=False,
     gamemode="survival", playerPos=[95.5, 71, 249.5], playerRot=[180, 15], timing="smooth",
     setup=FIELD_CLEAR + hero("regalia") + ritual(95, 71, 243, SKULLS),
     events=[ev(-0.4, face=""), ev(0.3, useOn=[95, 71, 243], useItem="minecraft:nether_star")],
     keys=arc([95.5, 0, 243.5], 7.2, 62, 112, 72.3, 6, 0, 9.0, [95.5, 72.6, 243.5], 68, 62))
shot("oss2_fight", duration=5.0, preroll=5.0, settle=6, renderDistance=12, hud="boss", hidePlayer=False,
     gamemode="survival", playerPos=[95.5, 71, 249.5], playerRot=[180, 0], timing="smooth",
     setup=FIELD_CLEAR + hero("regalia") + [summon(OSSUKAGE, 95.5, 72, 242.5, 0, ai=True)],
     events=[ev(-4.5, face="reel_boss")],
     keys=[key(0, [100.5, 72.4, 252.0], tag="reel_boss", off=[0, 0.2, 0], fov=62),
           key(5.0, [99.0, 72.6, 253.0], tag="reel_boss", off=[0, 0.2, 0], fov=58)])


# ---------------------------------------------------------------- the bestiary: one family per shot, the Gracetree behind


def family(name, centre, left, right, dist=7.4, look_y=72.7, lift=(0, 0, 0)) -> None:
    """The camera stands south of the group looking north; the three creatures face it. Floaters get NoGravity."""
    ents = [(centre, 95.5, 246.6, lift[0]), (left, 93.6, 248.0, lift[1]), (right, 97.4, 248.0, lift[2])]
    setup = [summon(e, x, 71 + dy, z, 0, tag="reel_mob", extra="NoGravity:1b" if dy else "") for e, x, z, dy in ents]
    cam_z = 247.2 + dist
    shot(name, duration=2.6, preroll=1.0, settle=6, renderDistance=10, hud="none", hidePlayer=True, gamemode="spectator",
         playerPos=[95.5, 74, 251.5], timing="smooth", setup=FIELD_CLEAR + RITE_CLEAR + CAMERA_ONLY + setup,
         keys=[key(0, [95.5, 71.9, cam_z], look=[95.5, look_y, 247.2], fov=62),
               key(2.6, [95.5, 71.95, cam_z - 1.0], look=[95.5, look_y - 0.05, 247.2], fov=58)])


family("fam1_legion", R + "fallen_standard_bearer", R + "hollow_sentinel", R + "soulbound_knight")
family("fam2_cryptborn", R + "burial_guardian", R + "gravebound_soldier", R + "crypt_lurker", dist=8.2, look_y=72.9)
family("fam3_souls", R + "soul_collector", R + "memory_wraith", R + "lost_pilgrim", lift=(0, 0.6, 0))
family("fam4_ashbound", R + "rusted_guardian", R + "ember_servitor", R + "ashbound_crawler", dist=8.0, look_y=72.9, lift=(0, 1.6, 0))
family("fam5_relics", R + "relic_stalker", R + "shardling", R + "ancient_vessel", dist=8.0, look_y=72.9, lift=(0, 1.4, 0))

# ---------------------------------------------------------------- the gear

shot("gear1_sets", duration=5.0, settle=5, renderDistance=10, hud="none", gamemode="spectator", playerPos=[18, 126, -386.5],
     timing="smooth",
     setup=CAMERA_ONLY + [stand(12.5, 125, -388.5 + 2 * i, -90, *SETS[n]) for i, n in enumerate(["warden", "regalia", "archivist"])],
     keys=arc([12.5, 0, -386.5], 5.4, 20, -20, 126.3, 5, 0, 5.0, [12.5, 126.1, -386.5], 64, 60))
# First person: hold use with the Sovereign's Ruin, release a spectral crescent through three sentinels.
shot("gear2_crescent", duration=3.5, preroll=1.0, settle=5, renderDistance=10, hud="none", hand=True, hidePlayer=False,
     gamemode="survival", camera="player", playerPos=[95.5, 71, 236.5], playerRot=[0, 4], timing="smooth",
     setup=FIELD_CLEAR + RITE_CLEAR + hero("regalia") +
     [summon(R + "hollow_sentinel", 93.7 + 1.8 * i, 71, 244.5, 180, tag="reel_mob") for i in range(3)],
     events=[ev(0.4, input="use"), ev(1.6, input="release")])

# ---------------------------------------------------------------- the end of a hunt

shot("felled1_warden", duration=6.0, preroll=4.0, settle=5, renderDistance=8, hud="boss", hidePlayer=False, gamemode="survival",
     playerPos=[190.6, -5, -11.5], playerRot=[-90, 0], timing="smooth", setup=WAR_SETUP,
     events=FACE_BOSS + [boss_ev(-3.5, health=0.45), boss_ev(0.0, health=0.004), ev(0.6, input="attack"), ev(1.1, input="attack")],
     keys=dolly([187.2, -3.2, -7.4], [193.0, -2.8, -12.0], 66, 6.0, push=0.8, side=0.6))
# The Boss Hunts journal, opened with the real item; the Warden's entry reads Defeated after felled1_warden.
shot("journal1", duration=4.5, preroll=0.5, settle=5, renderDistance=8, hud="none", hand=True, hidePlayer=False, gamemode="survival",
     camera="player", playerPos=[188.5, -5, -11.5], playerRot=[-90, 10],
     setup=hero("regalia") + ["item replace entity @p weapon.mainhand with remnant_bosses:boss_journal"],
     events=[ev(0.2, input="tapuse"), ev(1.6, screenCall="select", args=["GRAVEBOUND_WARDEN"]),
             ev(2.8, screenCall="select", args=["HOLLOW_SOVEREIGN"])])


def queue(names: list[str]) -> None:
    JOBS.mkdir(parents=True, exist_ok=True)
    chosen = []
    for pattern in names:
        if pattern.endswith("*"):
            chosen += [n for n in SHOTS if n.startswith(pattern[:-1])]
        elif pattern in SHOTS:
            chosen.append(pattern)
        else:
            raise SystemExit(f"unknown shot {pattern}")
    batch = int(time.time())  # later batches sort after earlier ones
    for i, name in enumerate(chosen):
        (JOBS / f"{batch}_{i:03d}_{name}.json").write_text(json.dumps(SHOTS[name], indent=1))
        print("queued", name)


if __name__ == "__main__":
    if len(sys.argv) < 2 or sys.argv[1] == "list":
        print("\n".join(SHOTS))
    elif sys.argv[1] == "queue":
        queue(sys.argv[2:])
