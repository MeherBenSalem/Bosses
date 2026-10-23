"""Verify all supported packaged resources/descriptors and record release hashes.

This is an artifact audit, separate from the native Minecraft parser tests run by
each workspace's :common:verifyCrafting task.
"""
import argparse
import hashlib
import json
from pathlib import Path
import tomllib
import zipfile

ROOT = Path(__file__).resolve().parents[1]
TARGETS = [('fabric', '1.20.1'), ('forge', '1.20.1'),
           ('fabric', '1.21.1'), ('neoforge', '1.21.1'),
           ('fabric', '26.2'), ('neoforge', '26.2'),
           ('fabric', '26.3'), ('neoforge', '26.3')]
# Resource and data pack formats each game version must accept.
PACK_FORMATS = {'1.20.1': [15], '1.21.1': [34, 48], '26.2': [88, 107], '26.3': [97, 121]}
RECIPES = {
    'ancient_altar_recipe': 'remnant_bosses:ancient_altar',
    'ancient_pedestal_recipe': 'remnant_bosses:ancient_pedestal',
    'skeleton_skull_recipe': 'minecraft:skeleton_skull',
    **{name: f'remnant_bosses:{name}' for name in [
        'boss_journal', 'eclipse_shard', 'eclipse_waygate', 'sovereigns_seal',
        'regalia_cuirass', 'regalia_greaves', 'regalia_sabatons', 'crypt_key',
        'memory_lens', 'archivist_hood', 'archivist_robe', 'archivist_leggings', 'archivist_boots']},
}


def same(packaged, source):
    """Packaged and source bytes match, whatever line endings the checkout uses (core.autocrlf)."""
    return packaged.replace(b'\r\n', b'\n') == source.replace(b'\r\n', b'\n')


def game_range(game):
    """The NeoForge minecraft versionRange the workspace declares (an exact version, or [26.x, 26.x+1) on 26.x)."""
    for line in (ROOT / game / 'gradle.properties').read_text().splitlines():
        if line.startswith('minecraft_version_range='):
            return line.split('=', 1)[1].strip()
    raise AssertionError(f'{game}: no minecraft_version_range')


def verify(jar, loader, game, version):
    modern = game != '1.20.1'
    unobfuscated = game.startswith('26.')
    resources = ROOT / game / 'common/src/main/resources'
    recipe_dir = 'recipe' if modern else 'recipes'
    loot_dir = 'loot_table' if modern else 'loot_tables'
    with zipfile.ZipFile(jar) as z:
        names = z.namelist()
        assert len(names) == len(set(names)), f'{jar}: duplicate entries'
        for name, output in RECIPES.items():
            entry = f'data/remnant_bosses/{recipe_dir}/{name}.json'
            raw = z.read(entry)
            assert same(raw, (resources / entry).read_bytes()), f'{jar}: stale recipe {entry}'
            recipe = json.loads(raw)
            assert recipe['result']['id' if modern else 'item'] == output
            assert recipe['result']['count'] == 1
        for name in ['ancient_altar', 'ancient_pedestal']:
            entry = f'data/remnant_bosses/{loot_dir}/blocks/{name}.json'
            assert same(z.read(entry), (resources / entry).read_bytes()), f'{jar}: stale block loot'
            loot = json.loads(z.read(entry))
            assert loot['pools'][0]['entries'][0]['name'] == f'remnant_bosses:{name}'
            assert loot['pools'][0]['rolls'] == 1
            if modern:
                assert f'data/remnant_bosses/loot_tables/blocks/{name}.json' not in names
        if modern:
            assert not any(n.startswith('data/remnant_bosses/recipes/') for n in names)
        pack = json.loads(z.read('pack.mcmeta'))['pack']
        if 'min_format' in pack:
            low, high = pack['min_format'], pack['max_format']
        else:
            formats = pack.get('supported_formats', {'min_inclusive': pack['pack_format'], 'max_inclusive': pack['pack_format']})
            low, high = formats['min_inclusive'], formats['max_inclusive']
        for fmt in PACK_FORMATS[game]:
            assert low <= fmt <= high, f'{jar}: pack format {fmt} outside [{low}, {high}]'
        expected = {'geckolib', 'jauml'}
        if loader == 'fabric':
            descriptor = json.loads(z.read('fabric.mod.json'))
            assert descriptor['id'] == 'remnant_bosses'
            assert descriptor['version'] == version
            assert descriptor['depends']['minecraft'] == (f'~{game}' if unobfuscated else game)
            assert {'geckolib', 'jauml', 'fabric-api'} <= descriptor['depends'].keys()
            expected.add('fabric-api')
        else:
            descriptor = tomllib.loads(z.read(f'META-INF/{"mods" if loader == "forge" else "neoforge.mods"}.toml').decode())
            assert descriptor['mods'][0]['modId'] == 'remnant_bosses'
            assert descriptor['mods'][0]['version'] == version
            deps = descriptor['dependencies']['remnant_bosses']
            required = {d['modId'] for d in deps if d.get('mandatory') or d.get('type') == 'required'}
            assert expected | {loader, 'minecraft'} <= required
            assert next(d['versionRange'] for d in deps if d['modId'] == 'minecraft') == game_range(game)
        # Test harnesses and fixtures must never be part of the shipped mod.
        assert not any('RecipeRegression' in n for n in names)
    data = jar.read_bytes()
    return {'file': jar.name, 'loader': loader, 'game': game, 'version': version,
            'bytes': len(data), 'sha1': hashlib.sha1(data).hexdigest(),
            'sha256': hashlib.sha256(data).hexdigest(), 'sha512': hashlib.sha512(data).hexdigest(),
            'required_dependencies': sorted(expected)}


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--version', required=True)
    parser.add_argument('--jar-dir', default='releases')
    parser.add_argument('--output')
    args = parser.parse_args()
    manifests = [verify(Path(args.jar_dir) / f'remnant_bosses-{loader}-{game}-{args.version}.jar', loader, game, args.version)
                 for loader, game in TARGETS]
    output = json.dumps(manifests, indent=2) + '\n'
    if args.output:
        Path(args.output).write_text(output)
    print(output)
