#!/usr/bin/env python3
"""Make marketing/modrinth/{description.md,summary.txt} from the CurseForge copy.

Modrinth takes the same Markdown, but screenshots belong in the project's Gallery tab, so the CurseForge
"Gallery" section (from its header image up to the next "---" rule) becomes one line pointing there.
Usage: python3 make_modrinth_md.py <repo root>
"""
import pathlib, re, sys

root = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else ".")
cf = root / "marketing/curseforge"
mr = root / "marketing/modrinth"
text = (cf / "description.md").read_text(encoding="utf-8")
lines = text.split("\n")

start = next((i for i, l in enumerate(lines) if re.match(r"!\[[^\]]*\]\([^)]*header-gallery\.png\)", l)), None)
if start is not None:
    end = next((i for i in range(start + 1, len(lines)) if lines[i].strip() == "---"), len(lines))
    lines[start:end] = ["More screenshots are in this project's **Gallery** tab.", ""]
out = "\n".join(lines)
mr.mkdir(parents=True, exist_ok=True)
(mr / "description.md").write_text(out, encoding="utf-8")
summary = (cf / "summary.txt").read_text(encoding="utf-8").strip()
if len(summary) > 256:
    raise SystemExit(f"summary is {len(summary)} characters; Modrinth allows 256")
(mr / "summary.txt").write_text(summary + "\n", encoding="utf-8")
print(f"wrote {mr}/description.md ({'gallery moved' if start is not None else 'no gallery section'}) and summary.txt")
