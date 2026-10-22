"""Copy built entity assets from docs/models/remnants/<id>/ into both Minecraft resource trees.

  python tools/remnants/install_assets.py hollow_sovereign hollow_sentinel ...
  python tools/remnants/install_assets.py --all

Copies <id>.geo.json, <id>.animation.json, <id>.png, <id>_glowmask.png and any extra texture
variants named <id>_*.png (e.g. hollow_sentinel_spectral.png) into
<version>/common/src/main/resources/assets/remnant_bosses/{geo/entity,animations/entity,textures/entities}.
"""
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / 'docs/models/remnants'
VERSIONS = ('1.20.1', '1.21.1')


def install(model_id):
    folder = SRC / model_id
    if not folder.is_dir():
        raise SystemExit(f'no built model at {folder}')
    copied = []
    for version in VERSIONS:
        assets = ROOT / version / 'common/src/main/resources/assets/remnant_bosses'
        plan = [(folder / f'{model_id}.geo.json', assets / 'geo/entity'),
                (folder / f'{model_id}.animation.json', assets / 'animations/entity')]
        for png in sorted(folder.glob(f'{model_id}*.png')):
            if png.name.startswith(('preview_', 'pose_')):
                continue
            plan.append((png, assets / 'textures/entities'))
        for src, dest_dir in plan:
            if not src.exists():
                raise SystemExit(f'missing {src}')
            dest_dir.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(src, dest_dir / src.name)
            copied.append(f'{version}: {dest_dir.relative_to(ROOT / version)}/{src.name}')
    return copied


if __name__ == '__main__':
    ids = sys.argv[1:]
    if ids == ['--all']:
        ids = sorted(p.name for p in SRC.iterdir() if (p / f'{p.name}.geo.json').exists())
    for model_id in ids:
        for line in install(model_id):
            print(line)
