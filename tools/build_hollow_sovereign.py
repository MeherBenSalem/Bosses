"""Author Hollow Sovereign through the running Blockbench MCP plugin."""
import base64
import json
import math
from pathlib import Path
import urllib.request
import shutil
from hollow_sovereign_materials import map_faces
from hollow_sovereign_motion import smooth_motion

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/models/hollow_sovereign'
OUT.mkdir(parents=True, exist_ok=True)
URL = 'http://localhost:3000/bb-mcp'
session = None

def rpc(method, params):
    global session
    headers = {'Content-Type': 'application/json', 'Accept': 'application/json, text/event-stream'}
    if session:
        headers['mcp-session-id'] = session
    req = urllib.request.Request(URL, json.dumps({'jsonrpc':'2.0','id':1,'method':method,'params':params}).encode(), headers)
    with urllib.request.urlopen(req, timeout=45) as res:
        session = res.headers.get('mcp-session-id', session)
        value = json.load(res)
    if 'error' in value:
        raise RuntimeError(value)
    return value['result']

def call(tool_name, **args):
    result = rpc('tools/call', {'name':tool_name, 'arguments':args})
    if result.get('isError'):
        raise RuntimeError(result)
    return result

def parsed(result):
    return json.loads(next(c['text'] for c in result['content'] if c['type']=='text'))

def group(name, pivot, parent='root'):
    call('add_group', name=name, origin=pivot, parent=parent)

batch = {}
def cube(bone, name, center, size, material=0, rot=(0,0,0)):
    entry = {'name':name, 'from':[a-b/2 for a,b in zip(center,size)], 'to':[a+b/2 for a,b in zip(center,size)], 'origin':center, 'rotation':list(rot)}
    batch.setdefault((bone,material), []).append(entry)

def keys(channel, values):
    return [{'time':t, channel:v} for t,v in values]

def main():
    batch.clear()
    rpc('initialize', {'protocolVersion':'2024-11-05','capabilities':{},'clientInfo':{'name':'Hollow Sovereign asset author','version':'1'}})
    print('Connected', flush=True)
    call('create_project', name='hollow_sovereign', format='geckolib_model')
    texture=OUT/'hollow_sovereign.png'
    shutil.copyfile(ROOT/'1.21.1/common/src/main/resources/assets/remnant_bosses/textures/entities/kotsukage.png',texture)
    call('create_texture',name='hollow_sovereign.png',width=512,height=512,data='data:image/png;base64,'+base64.b64encode(texture.read_bytes()).decode())
    call('risky_eval', code="Project.texture_width=512; Project.texture_height=512; Project.geometry_name='hollow_sovereign'; 'Configured original Kotsukage atlas';")
    group('root',[0,0,0])
    group('body',[0,27,0])
    group('chest',[0,31,0],'body')
    group('core',[0,38,-5],'chest')
    group('head',[0,47,0],'chest')
    group('jaw',[0,49,-3],'head')
    group('crown',[0,60,1],'head')
    cube('body','pelvis',[0,26,1],[13,8,10])
    cube('body','belt',[0,29,0],[13,2,10],4)
    cube('chest','spinal_column',[0,39,3],[5,22,6],1)
    cube('chest','upper_mantle',[0,45,1],[21,5,10])
    cube('chest','abdominal_shadow',[0,34,1],[9,12,8],5)
    cube('core','void_heart',[0,39,-5.7],[4,6,2],5)
    cube('core','heart_fire',[0,39,-6.8],[1.5,4,.4],3)
    for s in [-1,1]:
        side='left' if s==1 else 'right'
        for i in range(4):
            cube('chest',f'{side}_rib_{i}',[s*(6.5-i*.6),43-i*3.3,-5.5],[8-i*.5,1.8,3],1,(0,s*14,s*(22-i*5)))
        group(side+'_arm',[s*16,44,0],'chest')
        group(side+'_forearm',[s*21,32,-1],side+'_arm')
        group(side+'_hand',[s*23,20,-3],side+'_forearm')
        cube(side+'_arm','shoulder',[s*16,45,0],[10,7,10],0,(0,0,s*12))
        cube(side+'_arm','pauldron_trim',[s*17,47,-1],[9,2,10],1,(0,0,s*12))
        cube(side+'_arm','upper_arm',[s*19,37,0],[7,13,8],1,(0,0,s*14))
        cube(side+'_forearm','forearm_armor',[s*22,28,-2],[7,11,8],0,(0,0,s*8))
        cube(side+'_forearm','wrist_bone',[s*23,21,-2],[5,5,6],1)
        cube(side+'_forearm','forearm_rune',[s*22,28,-6.1],[1,6,.3],4)
        cube(side+'_hand','palm',[s*23,18,-3],[9,6,8])
        for j in range(3):
            cube(side+'_hand',f'talon_{j}',[s*(20+j*3),12,-5],[2,9,2.5],1,(10,0,s*(j-1)*8))
            cube(side+'_hand',f'talon_hook_{j}',[s*(20+j*3),8,-7],[1.5,3,5],1,(-25,0,0))
        cube(side+'_hand','thumb',[s*17.8,17,-4],[2.5,7,3],1,(0,0,s*-35))
        for j in range(2):
            cube(side+'_arm',f'shoulder_spike_{j}',[s*(13+j*5),51+j,1],[2.5,7+j*2,2.5],1,(0,0,-s*(15+j*15)))
        group(side+'_leg',[s*6,25,1])
        group(side+'_shin',[s*7,14,1],side+'_leg')
        cube(side+'_leg','thigh',[s*6,21,1],[7,12,8],0,(0,0,-s*4))
        cube(side+'_shin','shin',[s*7,8,1],[6,12,7],0)
        cube(side+'_shin','kneecap',[s*7,14,-3],[7,5,4],1)
        cube(side+'_shin','greave_stripe',[s*7,7,-2.8],[2,8,1],1)
        cube(side+'_shin','foot',[s*7,2,-2],[8,4,12])
        for j in range(3):
            cube(side+'_shin',f'foot_claw_{j}',[s*7+(j-1)*2.5,1.5,-9],[1.7,2,5],1)
        cube('head',side+'_cheek',[s*4,51,-3],[4,8,7],1,(0,0,s*-8))
        cube('head',side+'_eye_socket',[s*3.2,53,-6.7],[4,3,1],5,(0,0,s*12))
        cube('head',side+'_eye',[s*3.3,53,-7.3],[2.8,.9,.5],3,(0,0,s*12))
        cube('head',side+'_horn_base',[s*6,58,1],[3,8,3],1,(0,0,-s*25))
        cube('head',side+'_horn_tip',[s*8,63,1],[1.5,5,1.5],1,(0,0,s*12))
        cube('jaw',side+'_mandible',[s*3.3,47,-4],[3,5,7],1,(0,0,s*12))
        for j in range(3):
            cube('jaw',f'{side}_lower_tooth_{j}',[s*(1.5+j*1.5),49,-7],[.9,2.5,1],1)
    cube('head','skull',[0,55,0],[10,8,9],1)
    cube('head','brow',[0,55,-5],[9,2,3],0)
    cube('head','nasal_cavity',[0,51.5,-6],[1.5,3,1],5)
    cube('jaw','chin',[0,45,-4],[7,2,7],1)
    for i in range(5):
        angle=i*2*math.pi/5
        x,z=math.sin(angle)*10,math.cos(angle)*7+1
        group(f'crown_shard_{i}',[x,67,z],'crown')
        cube(f'crown_shard_{i}',f'floating_crown_{i}',[x,67,z],[2.5,5+(i%2)*2,2.5],0,(0,0,(-1)**i*8))
        cube(f'crown_shard_{i}',f'crown_tip_{i}',[x,71+(i%2),z],[1,3,1],1)
    for i in range(5):
        cube('chest',f'dorsal_spike_{i}',[0,30+i*4,7],[3,3,9],1,(-25,0,0))
    for s in [-1,1]:
        group(f'tasset_{s}',[s*6,27,0],'body')
        for j in range(3):
            cube(f'tasset_{s}',f'skirt_plate_{j}',[s*(7+j*.7),23-j*3,0],[3,6,10-j],0,(0,0,s*15))
    for (bone,mat),elements in batch.items():
        u,v=(mat%4)*32,(mat//4)*32
        faces=[{'face':f,'uv':[u+2,v+2,u+30,v+30]} for f in ['north','south','east','west','up','down']]
        call('place_cube',elements=elements,group=bone,texture='hollow_sovereign.png',faces=faces)
    print(f'Model: {sum(map(len,batch.values()))} cubes',flush=True)
    call('risky_eval',code="const rootBone=Group.all.find(g=>g.name==='root'); for(const g of Group.all.filter(g=>g!==rootBone && !(g.parent instanceof Group))){g.addTo(rootBone);} Canvas.updateAll(); 'Root hierarchy repaired';")
    uv_assignments=map_faces(ROOT,batch)
    call('risky_eval',code="Project.box_uv=false; const assignments="+json.dumps(uv_assignments)+"; for(const a of assignments){const c=Cube.all.find(c=>c.name===a.name && c.parent.name===a.group); c.setUVMode(false); for(const [f,uv] of Object.entries(a.faces)){c.faces[f].uv=uv;}} Canvas.updateAll(); 'Original bone atlas mapped per face';")
    animations={}
    def anim(name,length,bones,loop=False):
        bones=smooth_motion(name,length,bones,loop)
        call('create_animation',name='hollow_sovereign.'+name,animation_length=length,loop=loop,bones=bones)
        animations[name]={'length':length,'loop':loop}
    anim('idle',4,{'chest':keys('rotation',[(0,[0,0,0]),(2,[-2,0,0]),(4,[0,0,0])]),'core':keys('scale',[(0,[1,1,1]),(2,[1.12,1.12,1.12]),(4,[1,1,1])]),'crown':keys('position',[(0,[0,0,0]),(2,[0,1.5,0]),(4,[0,0,0])]),'jaw':keys('rotation',[(0,[0,0,0]),(2,[5,0,0]),(4,[0,0,0])])},True)
    walk={}
    for s,offset in [('left',1),('right',-1)]:
        walk[s+'_leg']=keys('rotation',[(0,[22*offset,0,0]),(.6,[-22*offset,0,0]),(1.2,[22*offset,0,0])])
        walk[s+'_arm']=keys('rotation',[(0,[-12*offset,0,0]),(.6,[12*offset,0,0]),(1.2,[-12*offset,0,0])])
        walk[s+'_shin']=keys('rotation',[(0,[0,0,0]),(.3 if offset==1 else .9,[-25,0,0]),(1.2,[0,0,0])])
    walk['body']=keys('position',[(0,[0,0,0]),(.3,[0,1,0]),(.6,[0,0,0]),(.9,[0,1,0]),(1.2,[0,0,0])])
    anim('walk',1.2,walk,True)
    anim('awaken',3,{'root':keys('position',[(0,[0,-8,0]),(1,[0,-8,0]),(2.3,[0,1,0]),(3,[0,0,0])]),'chest':keys('rotation',[(0,[35,0,0]),(1,[35,0,0]),(2.2,[-12,0,0]),(3,[0,0,0])]),'head':keys('rotation',[(0,[30,0,0]),(1.5,[30,0,0]),(2.2,[-20,0,0]),(3,[0,0,0])]),'jaw':keys('rotation',[(0,[0,0,0]),(1.6,[0,0,0]),(2.2,[32,0,0]),(3,[0,0,0])])})
    anim('roar',2,{'chest':keys('rotation',[(0,[0,0,0]),(.4,[-12,0,0]),(1.5,[-8,0,0]),(2,[0,0,0])]),'jaw':keys('rotation',[(0,[0,0,0]),(.35,[35,0,0]),(1.5,[32,0,0]),(2,[0,0,0])]),'crown':keys('position',[(0,[0,0,0]),(.4,[0,4,0]),(1.5,[0,4,0]),(2,[0,0,0])])})
    anim('claw_combo',1.8,{'chest':keys('rotation',[(0,[0,0,0]),(.35,[0,-25,0]),(.55,[10,30,0]),(.95,[0,25,0]),(1.15,[10,-30,0]),(1.8,[0,0,0])]),'right_arm':keys('rotation',[(0,[0,0,0]),(.35,[-85,0,-25]),(.55,[-20,0,30]),(1.8,[0,0,0])]),'left_arm':keys('rotation',[(0,[0,0,0]),(.85,[-85,0,25]),(1.15,[-20,0,-30]),(1.8,[0,0,0])])})
    slam={'chest':keys('rotation',[(0,[0,0,0]),(.8,[-18,0,0]),(1.05,[35,0,0]),(1.35,[35,0,0]),(2.2,[0,0,0])])}
    for side in ['left','right']:
        slam[side+'_arm']=keys('rotation',[(0,[0,0,0]),(.8,[-150,0,0]),(1.05,[-35,0,0]),(1.35,[-35,0,0]),(2.2,[0,0,0])])
    anim('ground_slam',2.2,slam)
    anim('core_blast',2.6,{'core':keys('scale',[(0,[1,1,1]),(1.3,[1.6,1.6,1.6]),(1.45,[.8,.8,.8]),(2.6,[1,1,1])]),'chest':keys('rotation',[(0,[0,0,0]),(1.3,[-15,0,0]),(1.45,[10,0,0]),(2.6,[0,0,0])]),'left_arm':keys('rotation',[(0,[0,0,0]),(.8,[0,0,-55]),(1.6,[0,0,-55]),(2.6,[0,0,0])]),'right_arm':keys('rotation',[(0,[0,0,0]),(.8,[0,0,55]),(1.6,[0,0,55]),(2.6,[0,0,0])])})
    anim('enrage',3,{'crown':keys('rotation',[(0,[0,0,0]),(1.5,[0,180,0]),(3,[0,360,0])])+keys('position',[(0,[0,0,0]),(1,[0,7,0]),(2,[0,7,0]),(3,[0,0,0])]),'core':keys('scale',[(0,[1,1,1]),(1,[1.4,1.4,1.4]),(1.3,[1,1,1]),(1.6,[1.5,1.5,1.5]),(3,[1,1,1])]),'jaw':keys('rotation',[(0,[0,0,0]),(.7,[38,0,0]),(2.4,[38,0,0]),(3,[0,0,0])])})
    anim('death',3.5,{'root':keys('rotation',[(0,[0,0,0]),(1,[0,0,8]),(1.9,[0,0,-6]),(2.6,[80,0,0]),(3.5,[90,0,0])])+keys('position',[(0,[0,0,0]),(1,[0,-4,0]),(2,[0,-13,0]),(3.5,[0,0,0])]),'core':keys('scale',[(0,[1,1,1]),(1,[1.3,1.3,1.3]),(2,[.1,.1,.1]),(3.5,[0,0,0])]),'crown':keys('position',[(0,[0,0,0]),(1,[0,4,0]),(3.5,[0,-10,0])])})
    (OUT/'clips.json').write_text(json.dumps(animations,indent=2))
    print('Created nine animation clips',flush=True)
    call('risky_eval',code="Animation.all.forEach(a=>a.snapping=120); Animation.all.find(a=>a.name==='animation.hollow_sovereign.death').loop='hold'; '24 FPS motion on a 120 FPS timing grid preserves clip endpoints';")
    result=call('risky_eval',code="JSON.stringify(Animator.buildFile(null, Animation.all.map(a=>a.name)))")
    exported=json.loads(parsed(result))
    (OUT/'hollow_sovereign.animation.json').write_text(json.dumps(exported,indent=2),encoding='utf-8')
    call('set_camera_angle',position=[100,75,-140],target=[0,35,0],projection='perspective')
    for codec,filename in [('project','hollow_sovereign.bbmodel'),('bedrock','hollow_sovereign.geo.json')]:
        data=parsed(call('export_model',codec_id=codec,max_content_length=2000000))
        if len(data['content'])>=2000000:
            raise RuntimeError('Export exceeds content limit; do not package truncated assets')
        (OUT/filename).write_text(data['content'],encoding='utf-8')
    shot=call('capture_screenshot')
    for c in shot['content']:
        if c['type']=='image': (OUT/'preview.png').write_bytes(base64.b64decode(c['data']))
    print('Exported to '+str(OUT),flush=True)

if __name__=='__main__': main()
