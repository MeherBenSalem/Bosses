"""Validate geometry, animation seams/timing, mirrored assets, and loader bindings."""
import json
import math
import zipfile
from pathlib import Path
from PIL import Image
from build_bone_monsters import ROOT, SPECS

CLASSES=['GraveSkitter','Sickleghast','DirgeLantern']
for (name,title),cls in zip(SPECS.items(),CLASSES):
    out=ROOT/'docs/models'/name
    geo=json.loads((out/(name+'.geo.json')).read_text())['minecraft:geometry'][0]
    animations=json.loads((out/(name+'.animation.json')).read_text())['animations']
    parents={b['name']:b.get('parent') for b in geo['bones']}
    assert len(parents)==len(geo['bones'])
    assert [b for b,p in parents.items() if p is None]==['root']
    for bone in parents:
        seen=set()
        while bone is not None:
            assert bone not in seen
            seen.add(bone);bone=parents[bone]
    atlas=Image.open(out/(name+'.png')).convert('RGBA')
    assert atlas.size==(geo['description']['texture_width'],geo['description']['texture_height'])
    for bone in geo['bones']:
        for cube in bone.get('cubes',[]):
            assert all(v>0 for v in cube['size'])
            for face in cube['uv'].values():
                u,v=face['uv'];w,h=face['uv_size']
                box=(min(u,u+w),min(v,v+h),max(u,u+w),max(v,v+h))
                assert 0<=box[0]<box[2]<=512 and 0<=box[1]<box[3]<=512
                assert atlas.crop(box).getchannel('A').getextrema()==(255,255)
    assert set(animations)=={f'animation.{name}.{clip}' for clip in ['idle','walk','attack','special','hurt','death']}
    count=0
    for clip,a in animations.items():
        assert set(a['bones'])<=parents.keys()
        for channels in a['bones'].values():
            for frames in channels.values():
                times=sorted(frames,key=float)
                assert float(times[0])==0 and abs(float(times[-1])-a['animation_length'])<.00001,(clip,times[-1])
                assert all(0<=float(t)<=a['animation_length'] for t in times)
                assert max(float(b)-float(a) for a,b in zip(times,times[1:]))<=1/24+.001
                for f in frames.values():assert all(math.isfinite(x) for x in f['vector'])
                if a.get('loop') is True:assert frames[times[0]]['vector']==frames[times[-1]]['vector'],clip
                count+=len(times)
    for version,loaders in [('1.20.1',['fabric','forge']),('1.21.1',['fabric','neoforge'])]:
        common=ROOT/version/'common/src/main'
        resources=common/'resources/assets/remnant_bosses'
        for folder,suffix in [('geo/entity','.geo.json'),('animations/entity','.animation.json'),('textures/entities','.png')]:
            assert (resources/folder/(name+suffix)).read_bytes()==(out/(name+suffix)).read_bytes()
        assert (resources/'textures/entities/kotsukage.png').read_bytes()==(out/(name+'.png')).read_bytes()
        for loader in loaders:
            files=list((ROOT/version/loader/'src/main/java').rglob('*.java'))
            code='\n'.join(p.read_text() for p in files)
            assert name.upper()+'_SPAWN_EGG' in code
            assert cls+'Entity.createAttributes()' in code
            assert cls+'Renderer::new' in code
    print(f'{title}: valid bone hierarchy, opaque UVs, six clips, {count} sampled channel frames, four loader bindings.')

if __name__=='__main__':
    for version,loaders in [('1.20.1',['fabric','forge']),('1.21.1',['fabric','neoforge'])]:
        for loader in loaders:
            jars=[p for p in (ROOT/version/loader/'build/libs').glob('*.jar') if not any(x in p.name for x in ['sources','javadoc','dev','shadow'])]
            jar=max(jars,key=lambda p:p.stat().st_mtime)
            with zipfile.ZipFile(jar) as z:
                for name,cls in zip(SPECS,CLASSES):
                    for path in [f'com/nightbeam/remnants/entity/{cls}Entity.class',f'com/nightbeam/remnants/client/renderer/{cls}Renderer.class',f'assets/remnant_bosses/geo/entity/{name}.geo.json',f'assets/remnant_bosses/animations/entity/{name}.animation.json',f'assets/remnant_bosses/textures/entities/{name}.png',f'assets/remnant_bosses/models/item/{name}_spawn_egg.json']:
                        assert path in z.namelist(),(jar,path)
            print('Packaged:',jar.name)
