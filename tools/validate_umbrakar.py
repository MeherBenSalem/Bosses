"""Validate cross-port rig references, UVs, animation endpoints and combat timing."""
import json
import math
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
relative = Path('common/src/main/resources/assets/remnant_bosses')
previous = None
for version in ('1.20.1', '1.21.1'):
    base = root / version / relative
    geo = json.loads((base / 'geo/entity/umbrakar.geo.json').read_text())
    animations = json.loads((base / 'animations/entity/umbrakar.animation.json').read_text())['animations']
    model = geo['minecraft:geometry'][0]
    bones = {b['name']: b for b in model['bones']}
    assert len(bones) == len(model['bones']), 'Duplicate bone names'
    for name, bone in bones.items():
        seen = {name}
        parent = bone.get('parent')
        while parent:
            assert parent in bones and parent not in seen, f'Broken rig ancestry: {name}'
            seen.add(parent)
            parent = bones[parent].get('parent')
        for cube in bone.get('cubes', []):
            assert all(math.isfinite(x) for key in ('origin', 'size') for x in cube[key])
            for face in cube['uv'].values():
                for i, limit in enumerate([model['description']['texture_width'], model['description']['texture_height']]):
                    a, b = face['uv'][i], face['uv'][i] + face['uv_size'][i]
                    assert 0 <= min(a, b) <= max(a, b) <= limit, f'UV outside texture: {name}'
    for name, animation in animations.items():
        assert animation['animation_length'] > 0
        for bone, channels in animation['bones'].items():
            assert bone in bones, f'Unknown animated bone: {bone}'
            for channel, frames in channels.items():
                times = list(map(float, frames))
                assert times == sorted(set(times)), f'Unordered keyframes: {name}/{bone}'
                assert times[0] == 0 and times[-1] == animation['animation_length']
                for frame in frames.values():
                    assert len(frame['vector']) == 3 and all(math.isfinite(v) for v in frame['vector'])
                values = list(frames.values())
                if animation['loop']:
                    assert values[0] == values[-1], f'Loop seam: {name}/{bone}/{channel}'
    java = (root / version / 'common/src/main/java/com/nightbeam/remnants/entity/UmbrakarEntity.java').read_text()
    references = re.findall(r'then(?:Loop|Play|PlayAndHold)\("([^"]+)"\)', java)
    assert set(references) == set(animations), 'Java / animation clip mismatch'
    for name, duration, impact in re.findall(r'queueAttack\("([^"]+)", (\d+), (\d+),', java):
        assert abs(animations[name]['animation_length'] * 20 - int(duration)) < .001
        assert 0 < int(impact) < int(duration)
        assert str(round(int(impact) / 20, 5)) in animations[name]['bones']['body']['rotation'], name
    assert int(re.search(r'deathTime >= (\d+)', java)[1]) == animations['death']['animation_length'] * 20
    pair = (geo, animations)
    if previous:
        assert pair == previous, 'Cross-port assets differ'
    previous = pair
    print(f'{version}: {len(bones)} bones, {sum(len(b.get("cubes", [])) for b in bones.values())} cubes, '
          f'{len(animations)} clips; rig, UVs, loops and combat timing passed.')
