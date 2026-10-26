#!/usr/bin/env python3
"""Where points land in a 9:16 frame for a camera pose (Minecraft's vertical FOV), before spending a render on it.

  from previs import frame
  frame(cam=[20, 126, -385], look=[12.5, 128, -386.5], fov=60,
        points={"hero feet": [16.5, 125, -387.6], "hero head": [16.5, 126.9, -387.6], "boss head": [12.5, 129, -386.5]})

Prints each point's screen position: x from -1 (left edge) to 1 (right edge), y from -1 (bottom) to 1 (top).
"""
from __future__ import annotations

import math

ASPECT = 9 / 16


def _sub(a, b):
    return [a[0] - b[0], a[1] - b[1], a[2] - b[2]]


def _dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def _norm(a):
    n = math.sqrt(_dot(a, a))
    return [a[0] / n, a[1] / n, a[2] / n]


def _cross(a, b):
    return [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]


def project(cam, look, fov, point):
    f = _sub(look, cam)
    if abs(f[0]) + abs(f[2]) < 1e-6:
        return None, None, 0
    forward = _norm(f)
    right = _norm(_cross(forward, [0, 1, 0]))
    up = _cross(right, forward)
    d = _sub(point, cam)
    depth = _dot(d, forward)
    if depth <= 0:
        return None, None, depth
    t = math.tan(math.radians(fov) / 2)
    return _dot(d, right) / depth / (t * ASPECT), _dot(d, up) / depth / t, depth


def frame(cam, look, fov, points: dict) -> None:
    for name, p in points.items():
        x, y, depth = project(cam, look, fov, p)
        if x is None:
            print(f"  {name:14s} behind the camera")
            continue
        flag = "" if abs(x) <= 1 and abs(y) <= 1 else "  OFF-FRAME"
        print(f"  {name:14s} x={x:+.2f} y={y:+.2f} depth={depth:.1f}{flag}")


def solve(far, near, far_xy=(0.0, 0.25), near_xy=(-0.45, -0.45), fov=64, back=(2.0, 6.0), side=(-4.0, 4.0), height=(0.3, 4.0),
          far_h=4.0, near_h=1.9, room=None):
    """Two-shot: a camera behind `near` (feet position) looking toward `far` (feet), searched so that each subject's
    middle lands near the requested screen point. `room` = (xmin, xmax, ymin, ymax, zmin, zmax) keeps the camera inside.
    Returns (cam, look)."""
    import itertools
    ax = [far[0] - near[0], far[2] - near[2]]
    n = math.hypot(*ax)
    ax = [ax[0] / n, ax[1] / n]
    perp = [-ax[1], ax[0]]
    best = None

    def frange(lo, hi, steps):
        return [lo + (hi - lo) * i / (steps - 1) for i in range(steps)]

    for b, sd, h, lt, lh in itertools.product(frange(*back, 9), frange(*side, 17), frange(*height, 8), frange(-0.2, 1.0, 7),
                                               frange(0.0, far_h, 5)):
        cam = [near[0] - ax[0] * b + perp[0] * sd, near[1] + h, near[2] - ax[1] * b + perp[1] * sd]
        if room and not (room[0] <= cam[0] <= room[1] and room[2] <= cam[1] <= room[3] and room[4] <= cam[2] <= room[5]):
            continue
        look = [near[0] + (far[0] - near[0]) * lt, far[1] + lh, near[2] + (far[2] - near[2]) * lt]
        err = 0
        for p, h_, want in ((far, far_h, far_xy), (near, near_h, near_xy)):
            x, y, d = project(cam, look, fov, [p[0], p[1] + h_ / 2, p[2]])
            if x is None:
                err = 1e9
                break
            err += (x - want[0]) ** 2 + (y - want[1]) ** 2
            for hh in (0, h_):
                x2, y2, _ = project(cam, look, fov, [p[0], p[1] + hh, p[2]])
                if x2 is None or abs(x2) > 0.95 or abs(y2) > 0.95:
                    err += 0.5
        if best is None or err < best[0]:
            best = (err, [round(c, 2) for c in cam], [round(c, 2) for c in look])
    return best[1], best[2]
