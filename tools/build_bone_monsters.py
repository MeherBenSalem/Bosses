"""Author three non-boss GeckoLib creatures through Blockbench MCP."""
import base64
import json
import math
import shutil
from pathlib import Path
import build_hollow_sovereign as bb
from hollow_sovereign_materials import map_faces

ROOT = Path(__file__).resolve().parents[1]
SPECS = {'grave_skitter': 'Grave Skitter', 'sickleghast': 'Sickleghast', 'dirge_lantern': 'Dirge Lantern'}

def make(name):
    out = ROOT/'docs/models'/name
    out.mkdir(parents=True, exist_ok=True)
    bb.batch.clear()
    bb.call('create_project', name=name, format='geckolib_model')
    tex = out/(name+'.png')
    shutil.copyfile(ROOT/'1.21.1/common/src/main/resources/assets/remnant_bosses/textures/entities/kotsukage.png',tex)
    bb.call('create_texture',name=name+'.png',width=512,height=512,data='data:image/png;base64,'+base64.b64encode(tex.read_bytes()).decode())
    bb.call('risky_eval',code=f"Project.texture_width=512; Project.texture_height=512; Project.geometry_name='{name}';")
    g,c=bb.group,bb.cube
    g('root',[0,0,0]); g('body',[0,16,0])
    limbs=[]
    if name=='grave_skitter':
        c('body','armored_abdomen',[0,12,5],[12,9,17],0)
        c('body','dark_underside',[0,9,4],[9,4,17],4)
        for j in range(5):
            c('body',f'carapace_{j}',[0,16,10-j*3],[13-j*.9,2,2.4],1,(8,0,0))
            c('body',f'spinal_hook_{j}',[0,19,10-j*3],[1.8,5,2],1,(-25,0,0))
        g('head',[0,12,-5],'body'); g('jaw',[0,10,-9],'head')
        c('head','skull',[0,13,-8],[9,7,9],1)
        c('head','mouth_void',[0,10.5,-12.5],[7,3,1],5)
        c('jaw','jaw_bridge',[0,8.5,-10],[8,2,7],0)
        for s in [-1,1]:
            c('head',f'brow_{s}',[s*2.7,15,-12.5],[5,2,2],0,(0,0,s*15))
            for j in range(3):
                c('head',f'eye_{s}_{j}',[s*(1.5+j*1.5),13.5,-13.2],[1,.8,.7],3)
                c('jaw',f'tooth_{s}_{j}',[s*(1+j*1.2),10,-13],[.8,2,1],1)
            c('jaw',f'mandible_{s}',[s*5.5,10,-14],[2,3,9],1,(0,s*25,0))
            c('jaw',f'mandible_hook_{s}',[s*4.5,10,-18],[2,2,4],1,(0,-s*35,0))
            for j in range(3):
                leg=f'leg_{s}_{j}'; limbs.append(leg)
                g(leg,[s*5,12,8-j*6],'body')
                c(leg,'upper',[s*10,12,9-j*7],[11,2.5,3],0,(0,s*(j-1)*20,s*15))
                c(leg,'joint',[s*15,13,9-j*7],[4,4,4],1)
                c(leg,'tibia',[s*18,7,10-j*8],[2.3,14,2.5],1,(0,0,s*25))
                c(leg,'foot',[s*20,1,8-j*8],[2,2,5],4)
    elif name=='sickleghast':
        c('body','spine',[0,24,3],[3,16,4],4,(-12,0,0))
        c('body','pelvis',[0,16,2],[9,5,7],0)
        c('body','sternum',[0,28,-1],[3,9,3],1)
        c('body','soul_slit',[0,29,-2.7],[1.1,5,.7],3)
        for s in [-1,1]:
            for j in range(4):
                c('body',f'rib_{s}_{j}',[s*3.5,31-j*2.7,0],[6.5,1.6,6-j*.4],1,(0,0,s*(15+j*5)))
            leg=f'leg_{s}';limbs.append(leg);g(leg,[s*3,17,2],'body')
            c(leg,'femur',[s*3.5,12,3],[3.5,9,4],0,(-20,0,s*5))
            c(leg,'reverse_hock',[s*4,5,5],[2.5,9,3],1,(25,0,0))
            c(leg,'foot',[s*4,1,1],[4,2,8],4)
            for j in range(2): c(leg,f'toe_{j}',[s*4+(j-.5)*2,1,-3.5],[1.2,1.5,3],1)
            arm=f'arm_{s}';g(arm,[s*7,31,1],'body')
            c(arm,'shoulder',[s*8,32,1],[7,6,7],0,(0,0,s*15))
            for j in range(3): c(arm,f'shoulder_thorn_{j}',[s*(8+j*1.7),36+j*.5,1],[1.5,6,1.5],1,(0,0,-s*(15+j*15)))
            c(arm,'humerus',[s*10,25,1],[3,11,4],4,(0,0,s*10))
            c(arm,'elbow',[s*11,20,0],[4,4,5],1)
            c(arm,'blade_back',[s*12,14,-1],[3,14,5],0,(0,0,s*12))
            c(arm,'blade_edge',[s*13.5,10,-4],[1,15,6],1,(-15,0,s*12))
            c(arm,'blade_hook',[s*13.5,4,-8],[1,3,9],1,(0,15*s,0))
        g('head',[0,33,-1],'body');g('jaw',[0,32,-6],'head')
        c('head','long_skull',[0,36,-4],[7,8,10],1,(-12,0,0))
        c('head','face_void',[0,34,-9.2],[5,4,.8],5)
        c('head','forehead_crest',[0,41,-2],[2,8,5],0,(-20,0,0))
        c('jaw','hanging_jaw',[0,30,-7],[5,2,6],1,(15,0,0))
        for s in [-1,1]:
            c('head',f'eye_{s}',[s*1.6,35,-9.8],[1.4,.8,.5],3,(0,0,-s*15))
            c('head',f'horn_{s}',[s*4.2,41,-1],[2,9,2],1,(20,0,-s*20))
            c('jaw',f'fang_{s}',[s*2,32,-9],[1,3,1],1)
        for j in range(4):c('body',f'dorsal_blade_{j}',[0,22+j*3.7,6],[1.8,3,7],1,(-30,0,0))
    else:
        c('body','hollow_spine',[0,21,3],[3,19,3],4)
        c('body','cage_cap',[0,30,0],[10,3,8],0)
        c('body','cage_base',[0,13,0],[8,3,7],0)
        g('core',[0,22,0],'body')
        c('core','soul',[0,22,0],[4,7,4],3,(0,0,15))
        c('core','soul_tip',[0,27,0],[2,4,2],3,(0,0,-15))
        for s in [-1,1]:
            for j in range(4):
                c('body',f'rib_side_{s}_{j}',[s*6,27-j*3.6,0],[2,1.5,10],1,(0,0,s*12))
                c('body',f'rib_front_{s}_{j}',[s*3.8,26.5-j*3.6,-4.6],[5.5,1.4,1.8],1,(0,0,s*18))
            c('body',f'cage_strut_{s}',[s*6,21,2],[1.5,17,1.5],0,(0,0,s*6))
            g(f'wing_{s}',[s*5,29,2],'body')
            for j in range(3):
                c(f'wing_{s}',f'fan_{j}',[s*(9+j*2.8),29+j*2,3],[1.5,12-j*2,3],1,(0,0,-s*(30+j*15)))
        g('head',[0,31,-1],'body');g('jaw',[0,31,-4],'head')
        c('head','skull',[0,35,-1],[8,7,7],1)
        c('head','socket',[0,34,-4.8],[6,3,1],5)
        c('jaw','jaw',[0,30,-3],[6,2,6],1)
        for s in [-1,1]:
            c('head',f'eye_{s}',[s*1.8,34,-5.5],[1.5,1.5,.5],3)
            c('head',f'antler_base_{s}',[s*5,40,1],[2,9,2],0,(0,0,-s*20))
            c('head',f'antler_tip_{s}',[s*7,45,1],[1.3,6,1.3],1,(0,0,s*10))
            c('head',f'antler_tine_{s}',[s*8,40,1],[1.3,5,1.3],1,(0,0,-s*55))
            for j in range(2): c('jaw',f'tooth_{s}_{j}',[s*(1+j*1.4),31.5,-5],[.8,2,1],1)
        for j in range(5):
            bone=f'tendril_{j}';limbs.append(bone)
            x=(j-2)*2.3;z=2 if j%2 else -1
            g(bone,[x,14,z],'body')
            for k in range(3):c(bone,f'vertebra_{k}',[x+math.sin(j+k)*.7,11-k*3,z+k*.5],[1.7,2.4,2],0,(0,0,math.sin(j+k)*12))
            c(bone,'hook',[x,3,z],[1.2,3,3],1,(-25,0,0))
    for (bone,mat),elements in bb.batch.items():bb.call('place_cube',elements=elements,group=bone,texture=name+'.png')
    uv=map_faces(ROOT,bb.batch)
    bb.call('risky_eval',code="const r=Group.all.find(g=>g.name==='root'); for(const g of Group.all.filter(g=>g!==r && !(g.parent instanceof Group)))g.addTo(r); Project.box_uv=false; const aa="+json.dumps(uv)+"; for(const a of aa){const c=Cube.all.find(c=>c.name===a.name&&c.parent.name===a.group); c.setUVMode(false); for(const [f,u] of Object.entries(a.faces))c.faces[f].uv=u;} Canvas.updateAll();")
    def anim(clip,length,tracks,loop=False):
        bones={}
        for bone,channels in tracks.items():
            frames=[]
            for channel,points in channels.items():
                times=sorted(set([round(i/24,6) for i in range(int(length*24)+1)]+[t for t,v in points]+[length]))
                for t in times:
                    if t>length:continue
                    i=next((i for i in range(len(points)-1) if points[i][0]<=t<=points[i+1][0]),len(points)-2)
                    t0,v0=points[i];t1,v1=points[i+1];u=max(0,min(1,(t-t0)/(t1-t0)));u=u*u*(3-2*u)
                    frames.append({'time':t,channel:[round(a+(b-a)*u,5) for a,b in zip(v0,v1)]})
            bones[bone]=frames
        bb.call('create_animation',name=name+'.'+clip,animation_length=length,loop=loop,bones=bones)
    def rot(*points):return {'rotation':list(points)}
    idle={'body':{'position':[(0,[0,0,0]),(2,[0,1 if name!='dirge_lantern' else 2,0]),(4,[0,0,0])]},'jaw':rot((0,[0,0,0]),(2,[7,0,0]),(4,[0,0,0])),'head':rot((0,[0,-3,0]),(2,[2,3,0]),(4,[0,-3,0]))}
    if name=='dirge_lantern':idle['core']={'scale':[(0,[1,1,1]),(2,[1.18,1.18,1.18]),(4,[1,1,1])]}
    for i,leg in enumerate(limbs):
        if name=='dirge_lantern':idle[leg]=rot((0,[6*math.sin(i),0,-5]),(2,[-6*math.sin(i),0,5]),(4,[6*math.sin(i),0,-5]))
    anim('idle',4,idle,True)
    walk={'body':{'position':[(0,[0,0,0]),(.3,[0,.8,0]),(.6,[0,0,0]),(.9,[0,.8,0]),(1.2,[0,0,0])]}}
    for i,leg in enumerate(limbs):
        a=18*(-1 if i%2 else 1)
        walk[leg]=rot((0,[a,0,0]),(.6,[-a,0,0]),(1.2,[a,0,0]))
    anim('walk',1.2,walk,True)
    attack={'body':rot((0,[0,0,0]),(.3,[-10,-12,0]),(.5,[15,15,0]),(1,[0,0,0])),'jaw':rot((0,[0,0,0]),(.3,[35,0,0]),(.5,[0,0,0]),(1,[0,0,0]))}
    special={'body':rot((0,[0,0,0]),(.6,[-15,0,0]),(.85,[20,0,0]),(1.6,[0,0,0])),'jaw':rot((0,[0,0,0]),(.6,[40,0,0]),(1.1,[35,0,0]),(1.6,[0,0,0]))}
    if name=='sickleghast':
        for s in [-1,1]:
            attack[f'arm_{s}']=rot((0,[0,0,0]),(.3,[-85,-s*20,-s*25]),(.5,[-20,s*30,s*20]),(1,[0,0,0]))
            special[f'arm_{s}']=rot((0,[0,0,0]),(.6,[-120,0,-s*35]),(.85,[-30,0,s*25]),(1.6,[0,0,0]))
    elif name=='dirge_lantern':
        special['core']={'scale':[(0,[1,1,1]),(.6,[1.6,1.6,1.6]),(.85,[.5,.5,.5]),(1.6,[1,1,1])]}
        for s in [-1,1]:special[f'wing_{s}']=rot((0,[0,0,0]),(.6,[0,0,-s*30]),(.85,[0,0,s*10]),(1.6,[0,0,0]))
    else:
        for i,leg in enumerate(limbs):special[leg]=rot((0,[0,0,0]),(.6,[0,0,(-1 if i<3 else 1)*20]),(.85,[0,0,0]),(1.6,[0,0,0]))
    anim('attack',1,attack);anim('special',1.6,special)
    anim('hurt',.5,{'body':rot((0,[0,0,0]),(.15,[-8,0,8]),(.5,[0,0,0]))})
    anim('death',1.5,{'body':rot((0,[0,0,0]),(.45,[-8,0,10]),(1.1,[0,0,85]),(1.5,[0,0,90])),'root':{'position':[(0,[0,0,0]),(1.1,[0,-7,0]),(1.5,[0,-8,0])]}})
    bb.call('risky_eval',code="Animation.all.forEach(a=>a.snapping=120); Animation.all.find(a=>a.name.endsWith('.death')).loop='hold';")
    data=json.loads(bb.parsed(bb.call('risky_eval',code="JSON.stringify(Animator.buildFile(null, Animation.all.map(a=>a.name)))")))
    (out/(name+'.animation.json')).write_text(json.dumps(data,indent=2))
    bb.call('list_export_formats')
    for codec,suffix in [('project','.bbmodel'),('bedrock','.geo.json')]:
        data=bb.parsed(bb.call('export_model',codec_id=codec,max_content_length=2000000))['content']
        assert len(data)<2000000
        json.loads(data)
        (out/(name+suffix)).write_text(data)
    target=[0,13,0] if name=='grave_skitter' else [0,24,0]
    bb.call('set_camera_angle',position=[65,48,-90],target=target,projection='perspective')
    shot=bb.call('capture_screenshot')
    for c in shot['content']:
        if c['type']=='image':(out/'preview.png').write_bytes(base64.b64decode(c['data']))
    for version in ['1.20.1','1.21.1']:
        assets=ROOT/version/'common/src/main/resources/assets/remnant_bosses'
        for folder,suffix in [('textures/entities','.png'),('geo/entity','.geo.json'),('animations/entity','.animation.json')]:shutil.copyfile(out/(name+suffix),assets/folder/(name+suffix))
    print(name,sum(map(len,bb.batch.values())),'cubes, six clips exported',flush=True)

if __name__=='__main__':
    bb.rpc('initialize',{'protocolVersion':'2024-11-05','capabilities':{},'clientInfo':{'name':'Bone monster trio','version':'1'}})
    for name in SPECS:make(name)
