"""Validate exported asset references and package both supported versions."""
import json
import shutil
import math
from PIL import Image
from build_hollow_sovereign import ROOT, OUT, rpc, call, base64

def main():
    animpath=OUT/'hollow_sovereign.animation.json'
    if not animpath.exists():
        raw=json.loads((OUT/'animation-export-response.json').read_text())
        data=json.loads(json.loads(raw['content'][0]['text']))
        animpath.write_text(json.dumps(data,indent=2))
    geo=json.loads((OUT/'hollow_sovereign.geo.json').read_text())['minecraft:geometry'][0]
    animations=json.loads(animpath.read_text())['animations']
    names={b['name'] for b in geo['bones']}
    atlas=Image.open(OUT/'hollow_sovereign.png').convert('RGBA')
    assert atlas.size==(geo['description']['texture_width'],geo['description']['texture_height'])
    source=ROOT/'1.21.1/common/src/main/resources/assets/remnant_bosses/textures/entities/kotsukage.png'
    assert (OUT/'hollow_sovereign.png').read_bytes()==source.read_bytes()
    assert len(names)==len(geo['bones'])
    assert len(animations)==9
    for b in geo['bones']:
        assert b.get('parent') is None or b['parent'] in names
        for c in b.get('cubes',[]):
            assert all(x>0 for x in c['size'])
            for uv in c['uv'].values():
                u,v=uv['uv']; w,h=uv['uv_size']
                assert 0<=min(u,u+w)<max(u,u+w)<=atlas.width
                assert 0<=min(v,v+h)<max(v,v+h)<=atlas.height
                region=atlas.crop((min(u,u+w),min(v,v+h),max(u,u+w),max(v,v+h)))
                assert region.getchannel('A').getextrema()==(255,255)
    for name,anim in animations.items():
        assert set(anim['bones'])<=names, name
        for channels in anim['bones'].values():
            for frames in channels.values():
                if anim.get('loop') is True:
                    ordered=sorted(frames,key=float)
                    first,last=frames[ordered[0]]['vector'],frames[ordered[-1]]['vector']
                    assert all(abs(a-b)<0.0001 for a,b in zip(first,last)), (name,'loop seam')
                for timestamp,value in frames.items():
                    assert 0<=float(timestamp)<=anim['animation_length']
                    assert all(math.isfinite(x) for x in value['vector'])
    parents={b['name']:b.get('parent') for b in geo['bones']}
    assert [n for n in names if parents[n] is None]==['root']
    for n in names:
        chain=set()
        while n is not None:
            assert n not in chain
            chain.add(n); n=parents[n]
    for version in ['1.20.1','1.21.1']:
        assetroot=ROOT/version/'common/src/main/resources/assets/remnant_bosses'
        assert (assetroot/'textures/entities/kotsukage.png').read_bytes()==source.read_bytes()
        for filename,directory in [('hollow_sovereign.geo.json','geo/entity'),('hollow_sovereign.animation.json','animations/entity'),('hollow_sovereign.png','textures/entities')]:
            dest=assetroot/directory/filename
            dest.parent.mkdir(parents=True,exist_ok=True)
            shutil.copyfile(OUT/filename,dest)
        obsolete=assetroot/'textures/entity/hollow_sovereign.png'
        if obsolete.exists(): obsolete.unlink()
    print(f'Validated {len(names)} bones and {len(animations)} clips; assets copied to both versions.')
    rpc('initialize',{'protocolVersion':'2024-11-05','capabilities':{},'clientInfo':{'name':'asset-check','version':'1'}})
    call('set_camera_angle',position=[115,90,-175],target=[0,38,0],projection='perspective')
    for clip,time in [('ground_slam',.8),('core_blast',1.3),('death',3.5)]:
        call('risky_eval',code=f"Modes.options.animate.select(); Animation.all.find(a=>a.name==='animation.hollow_sovereign.{clip}').select(); 'Selected';")
        call('animation_timeline',action='set_time',time=time)
        result=call('capture_screenshot')
        for c in result['content']:
            if c['type']=='image': (OUT/f'{clip}.png').write_bytes(base64.b64decode(c['data']))
    call('risky_eval',code="Animation.all.find(a=>a.name==='animation.hollow_sovereign.idle').select(); 'Idle selected';")
    call('animation_timeline',action='play')

if __name__=='__main__': main()
