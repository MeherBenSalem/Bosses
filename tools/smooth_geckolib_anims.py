#!/usr/bin/env python3
"""Insert mid-keyframes and convert Umbrakar animations to catmullrom."""
from __future__ import annotations

import json
from pathlib import Path


def time_key(t: float) -> str:
    s = f"{t:.4f}".rstrip("0").rstrip(".")
    if s in ("-0", ""):
        return "0.0"
    if "." not in s:
        return s + ".0"
    return s


def as_vec(value) -> list[float] | None:
    if value is None:
        return None
    if isinstance(value, list):
        return [float(value[0]), float(value[1]), float(value[2] if len(value) > 2 else 0.0)]
    if isinstance(value, dict):
        if "vector" in value:
            return as_vec(value["vector"])
        if "post" in value:
            return as_vec(value["post"])
        if "pre" in value:
            return as_vec(value["pre"])
    return None


def lerp(a: list[float], b: list[float], t: float) -> list[float]:
    return [a[i] + (b[i] - a[i]) * t for i in range(3)]


def catmull(p0, p1, p2, p3, t: float) -> list[float]:
    t2 = t * t
    t3 = t2 * t
    out = []
    for i in range(3):
        out.append(0.5 * (
            2.0 * p1[i]
            + (-p0[i] + p2[i]) * t
            + (2.0 * p0[i] - 5.0 * p1[i] + 4.0 * p2[i] - p3[i]) * t2
            + (-p0[i] + 3.0 * p1[i] - 3.0 * p2[i] + p3[i]) * t3
        ))
    return out


def frame(vec: list[float]) -> dict:
    return {"post": {"vector": [round(v, 4) for v in vec]}, "lerp_mode": "catmullrom"}


def densify_channel(keys: dict) -> dict:
    parsed = []
    for raw_t, value in keys.items():
        vec = as_vec(value)
        if vec is None:
            continue
        parsed.append((float(raw_t), vec))
    parsed.sort(key=lambda item: item[0])
    if not parsed:
        return keys
    if len(parsed) == 1:
        return {time_key(parsed[0][0]): {"vector": parsed[0][1]}}

    dense: list[tuple[float, list[float]]] = []
    for i, (t1, v1) in enumerate(parsed):
        dense.append((t1, v1))
        if i == len(parsed) - 1:
            break
        t2, v2 = parsed[i + 1]
        gap = t2 - t1
        steps = 1
        if gap >= 0.55:
            steps = 3
        elif gap >= 0.28:
            steps = 2
        elif gap >= 0.12:
            steps = 1
        else:
            steps = 0
        p0 = parsed[i - 1][1] if i > 0 else v1
        p3 = parsed[i + 2][1] if i + 2 < len(parsed) else v2
        for step in range(1, steps + 1):
            u = step / (steps + 1)
            dense.append((t1 + gap * u, catmull(p0, v1, v2, p3, u)))

    out = {}
    for t, vec in dense:
        out[time_key(t)] = frame(vec)
    return out


def densify_file(path: Path) -> tuple[int, int]:
    data = json.loads(path.read_text(encoding="utf-8"))
    before = 0
    after = 0
    for anim in data.get("animations", {}).values():
        for bone in (anim.get("bones") or {}).values():
            for channel, keys in list(bone.items()):
                if not isinstance(keys, dict):
                    continue
                before += len(keys)
                dense = densify_channel(keys)
                bone[channel] = dense
                after += len(dense)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    return before, after


def main() -> None:
    roots = [
        Path("1.20.1/common/src/main/resources/assets/remnant_bosses"),
        Path("1.21.1/common/src/main/resources/assets/remnant_bosses"),
    ]
    files = [
        "animations/entity/umbrakar.animation.json",
        "animations/entity/umbrakar_orb.animation.json",
        "geckolib/animations/entity/umbrakar.animation.json",
        "geckolib/animations/entity/umbrakar_orb.animation.json",
    ]
    # Smooth from the official Blockbench export, then copy to every destination.
    source_dir = roots[0]
    for rel in files[:2]:
        src = source_dir / rel
        before, after = densify_file(src)
        print(f"{src}: {before} -> {after} keyframes")
        for root in roots:
            dest = root / rel
            if dest == src:
                continue
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_text(src.read_text(encoding="utf-8"), encoding="utf-8")
            print(f"  copied {dest}")
        geckolib = Path("1.21.1/common/src/main/resources/assets/remnant_bosses/geckolib") / rel
        if "umbrakar" in rel:
            geckolib.parent.mkdir(parents=True, exist_ok=True)
            geckolib.write_text(src.read_text(encoding="utf-8"), encoding="utf-8")
            print(f"  copied {geckolib}")


if __name__ == "__main__":
    main()
