"""Build every Eclipsed Realm asset into both resource trees, then render the review images.

    python tools/remnants/realm/build_assets.py            # everything
    python tools/remnants/realm/build_assets.py --no-sounds  # skip the (slower) audio synthesis
    python tools/remnants/realm/build_assets.py --no-gif     # skip the animated waygate GIF

Deterministic and re-runnable. Writes only under assets/remnant_bosses/ of
1.20.1/common/src/main/resources and 1.21.1/common/src/main/resources, plus review images in
docs/models/remnants/realm/. sounds.json gets the 8 realm events appended if (and only if) missing.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import block_models  # noqa: E402
import blocks  # noqa: E402
import contact_sheet  # noqa: E402
import grace  # noqa: E402
import icons  # noqa: E402
import particles  # noqa: E402
import preview  # noqa: E402
import realm_sounds  # noqa: E402
import sky  # noqa: E402
import validate  # noqa: E402
import waygate  # noqa: E402
from common import WRITTEN  # noqa: E402


def main(argv):
    blocks.build()
    block_models.build()
    icons.build()
    grace.build()
    particles.build()
    sky.build()
    model, geo, base, glow = waygate.build()
    print(f'waygate: {len(geo["minecraft:geometry"][0]["bones"])} bones, atlas 256x{model.atlas_h}')
    if '--no-sounds' not in argv:
        lengths, added = realm_sounds.build()
        for name, secs in lengths.items():
            print(f'sound {name}: {secs:.2f}s')
        for tree, keys in added.items():
            print(f'sounds.json {tree}: added {keys or "nothing (already present)"}')
    else:
        WRITTEN.extend(f'sounds/realm/{n}.ogg' for n in realm_sounds.EFFECTS)
    docs = [preview.waygate_sheet()] + preview.waygate_key_poses()
    if '--no-gif' not in argv:
        docs.append(preview.waygate_gif())
    docs.append(contact_sheet.build())
    errs = validate.check()
    print(f'{len(WRITTEN)} asset files written to each tree')
    for d in docs:
        print('review:', d)
    if errs:
        print('VALIDATION PROBLEMS:')
        for e in errs:
            print('  ', e)
        return 1
    print('validation: ok')
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
