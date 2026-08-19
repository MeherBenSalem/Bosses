#!/usr/bin/env python3
"""Convert Blockbench 5 free-format .bbmodel files into GeckoLib geo + animation JSON + PNG textures."""
from __future__ import annotations

import argparse
import base64
import json
import re
from pathlib import Path


def slug(name: str) -> str:
    return re.sub(r"[^a-z0-9_]+", "_", name.lower()).strip("_") or "bone"


def num(v, default=0.0):
    try:
        return float(v)
    except (TypeError, ValueError):
        return default


def face_uv(face: dict) -> dict | None:
    if not face or face.get("texture") is None:
        return None
    uv = face.get("uv") or [0, 0, 0, 0]
    u1, v1, u2, v2 = (num(uv[0]), num(uv[1]), num(uv[2]), num(uv[3]))
    return {"uv": [u1, v1], "uv_size": [u2 - u1, v2 - v1]}


def cube_from_element(el: dict) -> dict:
    fr = el.get("from") or [0, 0, 0]
    to = el.get("to") or [0, 0, 0]
    cube = {
        "origin": [num(fr[0]), num(fr[1]), num(fr[2])],
        "size": [num(to[0]) - num(fr[0]), num(to[1]) - num(fr[1]), num(to[2]) - num(fr[2])],
        "uv": {},
    }
    origin = el.get("origin")
    rot = el.get("rotation")
    if origin:
        cube["pivot"] = [num(origin[0]), num(origin[1]), num(origin[2])]
    if rot and any(abs(num(c)) > 1e-6 for c in rot):
        cube["rotation"] = [num(rot[0]), num(rot[1]), num(rot[2])]
    inflate = el.get("inflate")
    if inflate:
        cube["inflate"] = num(inflate)
    for side, face in (el.get("faces") or {}).items():
        mapped = face_uv(face)
        if mapped:
            cube["uv"][side] = mapped
    if not cube["uv"]:
        cube.pop("uv")
    return cube


def walk_outliner(nodes, parent: str | None, bones: list, elements_by_id: dict, groups_by_id: dict, used_names: dict):
    for node in nodes or []:
        if isinstance(node, str):
            el = elements_by_id.get(node)
            if el and parent:
                for bone in bones:
                    if bone["name"] == parent:
                        bone.setdefault("cubes", []).append(cube_from_element(el))
                        break
            continue
        uuid = node.get("uuid")
        group = groups_by_id.get(uuid, {})
        raw_name = group.get("name") or node.get("name") or uuid[:8]
        name = slug(raw_name)
        used_names[name] = used_names.get(name, 0) + 1
        if used_names[name] > 1:
            name = f"{name}_{used_names[name]}"
        groups_by_id.setdefault(uuid, {})["export_name"] = name
        origin = group.get("origin") or [0, 0, 0]
        bone = {
            "name": name,
            "pivot": [num(origin[0]), num(origin[1]), num(origin[2])],
        }
        if parent:
            bone["parent"] = parent
        rot = group.get("rotation")
        if rot and any(abs(num(c)) > 1e-6 for c in rot):
            bone["rotation"] = [num(rot[0]), num(rot[1]), num(rot[2])]
        bones.append(bone)
        walk_outliner(node.get("children") or [], name, bones, elements_by_id, groups_by_id, used_names)


def convert_geo(model: dict, identifier: str) -> dict:
    elements_by_id = {e["uuid"]: e for e in model.get("elements") or [] if e.get("uuid")}
    groups_by_id = {g["uuid"]: g for g in model.get("groups") or [] if isinstance(g, dict) and g.get("uuid")}
    bones: list = []
    used_names: dict = {}
    walk_outliner(model.get("outliner") or [], None, bones, elements_by_id, groups_by_id, used_names)
    res = model.get("resolution") or {}
    tw = int(res.get("width") or 64)
    th = int(res.get("height") or 64)
    vis = model.get("visible_box") or [1, 1, 0]
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [
            {
                "description": {
                    "identifier": f"geometry.{identifier}",
                    "texture_width": tw,
                    "texture_height": th,
                    "visible_bounds_width": max(4.0, float(vis[0]) * 8 if vis else 8),
                    "visible_bounds_height": max(3.0, float(vis[1]) * 4 if len(vis) > 1 else 4),
                    "visible_bounds_offset": [0, 1.5, 0],
                },
                "bones": bones,
            }
        ],
    }, groups_by_id


def interp_name(value: str | None) -> str:
    if not value:
        return "linear"
    value = str(value).lower()
    if value in ("catmullrom", "smooth"):
        return "catmullrom"
    if value == "step":
        return "step"
    return "linear"


def convert_animations(model: dict, groups_by_id: dict) -> dict:
    uuid_to_name = {uid: g.get("export_name") or slug(g.get("name") or uid[:8]) for uid, g in groups_by_id.items()}
    out = {}
    for anim in model.get("animations") or []:
        name = anim.get("name") or "animation"
        loop = anim.get("loop")
        entry = {
            "loop": True if loop in (True, "loop") else False,
            "animation_length": num(anim.get("length"), 1.0),
            "bones": {},
        }
        if loop == "hold":
            entry["loop"] = "hold_on_last_frame"
        for uuid, animator in (anim.get("animators") or {}).items():
            if not isinstance(animator, dict) or animator.get("type") not in (None, "bone"):
                continue
            bone_name = uuid_to_name.get(uuid) or slug(animator.get("name") or uuid[:8])
            channels: dict = {}
            for kf in animator.get("keyframes") or []:
                channel = kf.get("channel")
                if channel not in ("position", "rotation", "scale"):
                    continue
                pts = kf.get("data_points") or [{}]
                pt = pts[0]
                vec = [num(pt.get("x")), num(pt.get("y")), num(pt.get("z"), 1.0 if channel == "scale" else 0.0)]
                time = f"{num(kf.get('time')):.4f}".rstrip("0").rstrip(".")
                if time == "-0":
                    time = "0"
                channels.setdefault(channel, {})
                item = {"vector": vec, "easing": interp_name(kf.get("interpolation"))}
                if len(pts) > 1 and kf.get("interpolation") == "bezier":
                    item["easing"] = "linear"
                channels[channel][time] = item
            if channels:
                entry["bones"][bone_name] = channels
        out[name] = entry
    return {"format_version": "1.8.0", "animations": out}


def write_textures(model: dict, dest_dir: Path, rename: dict[str, str] | None = None) -> list[Path]:
    dest_dir.mkdir(parents=True, exist_ok=True)
    written = []
    rename = rename or {}
    for tex in model.get("textures") or []:
        src = tex.get("source") or ""
        if "," in src:
            src = src.split(",", 1)[1]
        if not src:
            continue
        raw_name = tex.get("name") or "texture.png"
        name = rename.get(raw_name, raw_name)
        if not name.endswith(".png"):
            name += ".png"
        path = dest_dir / name
        path.write_bytes(base64.b64decode(src))
        written.append(path)
    return written


def convert_file(src: Path, geo_path: Path, anim_path: Path, tex_dir: Path, identifier: str, tex_rename: dict[str, str] | None = None):
    model = json.loads(src.read_text(encoding="utf-8"))
    geo, groups = convert_geo(model, identifier)
    anims = convert_animations(model, groups)
    geo_path.parent.mkdir(parents=True, exist_ok=True)
    anim_path.parent.mkdir(parents=True, exist_ok=True)
    geo_path.write_text(json.dumps(geo, indent=2), encoding="utf-8")
    anim_path.write_text(json.dumps(anims, indent=2), encoding="utf-8")
    textures = write_textures(model, tex_dir, tex_rename)
    print(f"{src.name}: {len(geo['minecraft:geometry'][0]['bones'])} bones, {len(anims['animations'])} anims, {len(textures)} textures")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("src")
    parser.add_argument("--id", required=True)
    parser.add_argument("--geo", required=True)
    parser.add_argument("--anim", required=True)
    parser.add_argument("--tex", required=True)
    parser.add_argument("--rename-tex", action="append", default=[])
    args = parser.parse_args()
    rename = {}
    for item in args.rename_tex:
        old, new = item.split("=", 1)
        rename[old] = new
    convert_file(Path(args.src), Path(args.geo), Path(args.anim), Path(args.tex), args.id, rename)


if __name__ == "__main__":
    main()
