"""Copy original ritual geometry and textures for the browser atlas."""
from pathlib import Path
import json
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'website/public/ritual'
ASSETS = ROOT / '1.21.1/common/src/main/resources/assets/remnant_bosses'


def export_ritual():
    OUT.mkdir(parents=True, exist_ok=True)
    models = {}
    for name in ('alter', 'pedestal'):
        models[name] = json.loads((ASSETS / f'models/custom/{name}.json').read_text())
    (OUT / 'models.json').write_text(json.dumps(models))
    shutil.copyfile(ASSETS / 'textures/block/alter.png', OUT / 'alter.png')
    jar = Path.home() / '.gradle/caches/fabric-loom/1.21.1/minecraft-client.jar'
    textures = {
        name: f'block/{name}' for name in (
            'bone_block_side', 'bone_block_top', 'soul_sand', 'soul_soil',
            'nether_wart_block', 'amethyst_block', 'crying_obsidian',
            'end_stone', 'sculk', 'chiseled_deepslate')
    }
    textures.update({name: f'item/{name}' for name in (
        'nether_star', 'heart_of_the_sea', 'echo_shard', 'diamond', 'emerald')})
    textures.update({
        'skeleton_skull': 'entity/skeleton/skeleton',
        'wither_skeleton_skull': 'entity/skeleton/wither_skeleton',
    })
    with zipfile.ZipFile(jar) as archive:
        for name, source in textures.items():
            (OUT / f'{name}.png').write_bytes(archive.read(f'assets/minecraft/textures/{source}.png'))
    print('Exported original altar, pedestal and 17 ritual textures.')


if __name__ == '__main__':
    export_ritual()
