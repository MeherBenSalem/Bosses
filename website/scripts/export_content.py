"""Export public documentation and GLB views from checked-in mod resources. No secrets."""
import json, math, re, struct, shutil
from pathlib import Path
from native_models import native_model

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'website/public'
ASSETS=ROOT/'1.21.1/common/src/main/resources/assets/remnant_bosses'
JAVA=ROOT/'1.21.1/common/src/main/java/com/nightbeam/remnants'

def quat(r):
    # Bedrock rotations apply X then Y then Z, with X/Y signs opposite glTF.
    x,y,z=[math.radians(v)*(-1 if i<2 else 1)/2 for i,v in enumerate(r)];a,b,c=math.cos(x),math.cos(y),math.cos(z);d,e,f=math.sin(x),math.sin(y),math.sin(z)
    return [d*b*c-a*e*f,a*e*c+d*b*f,a*b*f-d*e*c,a*b*c+d*e*f]

def rotate(p,r):
    x,y,z,w=quat(r);a,b,c=p
    # Quaternion-vector rotation.
    tx,ty,tz=2*(y*c-z*b),2*(z*a-x*c),2*(x*b-y*a)
    return [a+w*tx+y*tz-z*ty,b+w*ty+z*tx-x*tz,c+w*tz+x*ty-y*tx]

def export_glb(name):
    src=ASSETS/f'geo/entity/{name}.geo.json'; tex=ASSETS/f'textures/entities/{name}.png'
    if name=='skeleton_minion':tex=ASSETS/'textures/entities/java_skeleton.png'
    native=None if src.exists() else native_model(JAVA,name)
    if not tex.exists() or (not src.exists() and not native):return None
    geo=native[0] if native else json.loads(src.read_text())['minecraft:geometry'][0]; bones=geo['bones'];tw=geo['description']['texture_width'];th=geo['description']['texture_height']
    doc={'asset':{'version':'2.0','generator':'Remnant Codex source exporter'},'scene':0,'scenes':[{'nodes':[]}],'nodes':[],'meshes':[],'bufferViews':[],'accessors':[],
         'materials':[{'pbrMetallicRoughness':{'baseColorTexture':{'index':0},'metallicFactor':0,'roughnessFactor':.9},'alphaMode':'MASK','alphaCutoff':.1,'doubleSided':True}],
         'textures':[{'sampler':0,'source':0}],'samplers':[{'magFilter':9728,'minFilter':9728,'wrapS':33071,'wrapT':33071}], 'animations':[]}
    blob=bytearray()
    def view(data):
        while len(blob)%4:blob.append(0)
        start=len(blob);blob.extend(data);doc['bufferViews'].append({'buffer':0,'byteOffset':start,'byteLength':len(data)})
        return len(doc['bufferViews'])-1
    def access(rows,typ):
        width={'SCALAR':1,'VEC2':2,'VEC3':3,'VEC4':4}[typ]
        flat=[v for r in rows for v in (r if isinstance(r,list) else [r])]
        idx=view(struct.pack('<'+'f'*len(flat),*flat))
        d={'bufferView':idx,'componentType':5126,'count':len(rows),'type':typ,'min':[min(flat[i::width]) for i in range(width)],'max':[max(flat[i::width]) for i in range(width)]}
        doc['accessors'].append(d);return len(doc['accessors'])-1
    mapping={b['name']:i for i,b in enumerate(bones)}
    for b in bones:
        p=b.get('pivot',[0,0,0]);parent=next((x.get('pivot',[0,0,0]) for x in bones if x['name']==b.get('parent')),[0,0,0])
        doc['nodes'].append({'name':b['name'],'translation':[(v-u)/16 for v,u in zip(p,parent)],'rotation':quat(b.get('rotation',[0,0,0]))})
    for i,b in enumerate(bones):
        if b.get('parent') in mapping:doc['nodes'][mapping[b['parent']]].setdefault('children',[]).append(i)
        else:doc['scenes'][0]['nodes'].append(i)
        pos=[];uvs=[];norm=[]
        for cube in b.get('cubes',[]):
            x,y,z=cube['origin'];sx,sy,sz=cube['size'];inf=cube.get('inflate',0);x-=inf;y-=inf;z-=inf;sx+=2*inf;sy+=2*inf;sz+=2*inf
            points={'north':[[x,y,z],[x+sx,y,z],[x+sx,y+sy,z],[x,y+sy,z]],'south':[[x+sx,y,z+sz],[x,y,z+sz],[x,y+sy,z+sz],[x+sx,y+sy,z+sz]],'east':[[x+sx,y,z],[x+sx,y,z+sz],[x+sx,y+sy,z+sz],[x+sx,y+sy,z]],'west':[[x,y,z+sz],[x,y,z],[x,y+sy,z],[x,y+sy,z+sz]],'up':[[x,y+sy,z],[x+sx,y+sy,z],[x+sx,y+sy,z+sz],[x,y+sy,z+sz]],'down':[[x,y,z+sz],[x+sx,y,z+sz],[x+sx,y,z],[x,y,z]]}
            uv=cube.get('uv',[0,0])
            if isinstance(uv,list):
                u,v=uv;uv={f:{'uv':a,'uv_size':s} for f,a,s in [('north',[u+sz,v+sz],[sx,sy]),('south',[u+sz*2+sx,v+sz],[sx,sy]),('west',[u,v+sz],[sz,sy]),('east',[u+sz+sx,v+sz],[sz,sy]),('up',[u+sz,v],[sx,sz]),('down',[u+sz+sx,v],[sx,sz])]}
            for face,pts in points.items():
                if face not in uv:continue
                u,v=uv[face]['uv'];w,h=uv[face].get('uv_size',[sx,sy]);coords=[[u/tw,(v+h)/th],[(u+w)/tw,(v+h)/th],[(u+w)/tw,v/th],[u/tw,v/th]]
                if cube.get('mirror'):coords=[coords[j] for j in [1,0,3,2]]
                cp=cube.get('pivot',[0,0,0]);bp=b.get('pivot',[0,0,0]);rot=cube.get('rotation',[0,0,0])
                pts=[[(v+a-b)/16 for v,a,b in zip(rotate([v-a for v,a in zip(pt,cp)],rot),cp,bp)] for pt in pts]
                # Winding chosen outward; GLB uses top-left texture coordinates.
                a=[pts[2][j]-pts[0][j] for j in range(3)];c=[pts[1][j]-pts[0][j] for j in range(3)]
                n=[a[1]*c[2]-a[2]*c[1],a[2]*c[0]-a[0]*c[2],a[0]*c[1]-a[1]*c[0]];length=math.sqrt(sum(v*v for v in n)) or 1;n=[v/length for v in n]
                for k in [0,2,1,0,3,2]:pos.append(pts[k]);uvs.append(coords[k]);norm.append(n)
        if pos:
            doc['nodes'][i]['mesh']=len(doc['meshes']);doc['meshes'].append({'primitives':[{'attributes':{'POSITION':access(pos,'VEC3'),'TEXCOORD_0':access(uvs,'VEC2'),'NORMAL':access(norm,'VEC3')},'material':0}]})
    animfile=ASSETS/f'animations/entity/{name}.animation.json';clips=[]
    if animfile.exists() or native:
        for key,animation in (native[1] if native else json.loads(animfile.read_text()).get('animations',{})).items():
            a={'name':key.split('.')[-1],'channels':[],'samplers':[]}
            for bone,channels in animation.get('bones',{}).items():
                if bone not in mapping:continue
                for ch,frames in channels.items():
                    if ch not in ['rotation','position','scale']:continue
                    if isinstance(frames,list):frames={'0':frames,'1':frames}
                    if not isinstance(frames,dict):continue
                    pairs=[]
                    for t,v in frames.items():
                        try:
                            t=float(t)
                            while isinstance(v,dict):v=v.get('vector',v.get('post',v.get('pre')))
                            if isinstance(v,(int,float)):v=[v]*3
                            if not isinstance(v,list) or len(v)!=3:continue
                            v=[float(x) for x in v]
                            if not all(math.isfinite(x) for x in v):continue
                            base=doc['nodes'][mapping[bone]]['translation']
                            rest=bones[mapping[bone]].get('rotation',[0,0,0])
                            v=quat([x+y for x,y in zip(v,rest)]) if ch=='rotation' else [x/16+y for x,y in zip(v,base)] if ch=='position' else v
                            pairs.append((t,v))
                        except (ValueError,TypeError):continue
                    pairs.sort()
                    if len(pairs)<2:continue
                    a['channels'].append({'sampler':len(a['samplers']),'target':{'node':mapping[bone],'path':{'position':'translation','rotation':'rotation','scale':'scale'}[ch]}})
                    a['samplers'].append({'input':access([p[0] for p in pairs],'SCALAR'),'output':access([p[1] for p in pairs],'VEC4' if ch=='rotation' else 'VEC3'),'interpolation':'LINEAR'})
            if a['channels']:doc['animations'].append(a);clips.append(a['name'])
    doc['images']=[{'bufferView':view(tex.read_bytes()),'mimeType':'image/png'}]
    while len(blob)%4:blob.append(0)
    doc['buffers']=[{'byteLength':len(blob)}];raw=json.dumps(doc,separators=(',',':')).encode()
    raw+=b' '*((-len(raw))%4)
    target=OUT/f'models/{name}.glb';target.write_bytes(struct.pack('<III',0x46546c67,2,12+8+len(raw)+8+len(blob))+struct.pack('<II',len(raw),0x4e4f534a)+raw+struct.pack('<II',len(blob),0x004e4942)+blob)
    return {'url':f'models/{name}.glb','clips':clips,'bones':len(bones),'cubes':sum(len(b.get('cubes',[])) for b in bones)}

catalog=[
('hollow_sovereign','Hollow Sovereign','The crown awakens','Boss','A towering bone monarch with a suspended crown and a volatile soul core.','Claw combo|Ground slam|Core blast|Roar|Enrage','SovereignConfig.java'),
('ossukage','Ossukage','Oni Shogun','Boss','A skeletal warrior whose blade swings and dash attacks punish careless approaches.','Blade swings|Attack dash|Skeleton reinforcements',''),
('kotsukage','Kotsukage','Bone Sovereign','Boss','A heavy bone predator mixing alternating swipes, stomps, poison breath, and a roar.','Right and left swipe|Alternating stomps|Poison breath|Roar',''),
('umbrakar','Umbrakar','Riftmaw Colossus','Boss','A crowned colossus with a blade mane, crushing limbs, and a tail that casts homing orbs.','Bite|Front slam|Tail slam|Homing orb|Roar shockwave|Rift claw',''),
('grave_skitter','Grave Skitter','The cemetery crawls','Monster','Six jointed legs carry a low carapace and a skull with hooked mandibles. Watch for the crouch before its pounce.','Melee bite|Telegraphed pounce',''),
('sickleghast','Sickleghast','A harvest of bone','Monster','A reverse-jointed ghoul with enormous scythe forearms. Its frontal cleave rewards stepping out of the attack arc.','Paired strike|Frontal cleave',''),
('dirge_lantern','Dirge Lantern','A light you should not follow','Monster','A flying ribcage carries a trapped soul. It marks a location before releasing a soul burst; move away from the tell.','Flight|Marked soul burst',''),
('skeleton_melee','Skeleton Warrior','The restless front line','Monster','An armored skeleton built for close combat, with a heavier attack alongside its regular strike.','Melee strike|Heavy attack',''),
('skeleton_archer','Skeleton Archer','Death at a distance','Monster','A skeletal ranged enemy with regular arrows and a charged shot.','Arrow shot|Charged shot',''),
('armored_grub','Armored Grub','Small, heavily plated','Creature','An armored creature registered in the creature category. Available through its spawn egg.','Armored body',''),
('wraith','Wraith','The lingering dead','Monster','A hostile spirit that pursues players with melee attacks and can rise toward targets above it.','Melee pursuit|Vertical pursuit',''),
('rat','Rat','A bite in the dark','Monster','A small hostile mob with configurable natural spawning, found through the mod’s spawn system.','Melee attack',''),
('skeleton_minion','Skeleton Minion','Called from the grave','Monster','A remnant minion that can accompany the Ossukage ritual. Its balance configuration includes leap and trap settings.','Minion combat',''),
]

def main():
    from export_ritual import export_ritual
    export_ritual()
    OUT.mkdir(exist_ok=True);(OUT/'models').mkdir(exist_ok=True)
    configs={}
    source=(JAVA/'config/JaumlConfigBootstrap.java').read_text()
    for kind,path,file,key,value in re.findall(r'api\.set(Number|String)Value\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*("[^"]*"|[-\d.]+)\s*\)',source):
        configs.setdefault(path+'/'+file,{})[key]=json.loads(value)
    s=(JAVA/'config/SovereignConfig.java').read_text();configs['remnant/bosses/hollow_sovereign']={k:float(v) for k,v in re.findall(r'Map.entry\("([^"]+)",\s*([\d.]+)\)',s)}
    configs['remnant/bosses/hollow_sovereign_summon']={'portal_activation_item':'minecraft:heart_of_the_sea','pedestal_one_activation_block':'minecraft:bone_block','pedestal_two_activation_block':'minecraft:bone_block','pedestal_three_activation_block':'minecraft:skeleton_skull','pedestal_four_activation_block':'minecraft:skeleton_skull'}
    for name,values in re.findall(r'"(grave_skitter|sickleghast|dirge_lantern)",stats\(([^)]+)\)',(JAVA/'config/BoneMonsterConfig.java').read_text()):
        configs['remnant/monsters/'+name]=dict(zip(['max_health','attack_damage','armor','movement_speed','attack_cooldown_ticks','special_damage','special_range','xp_reward'],[float(v) for v in values.split(',')]))
    configs['remnant/client/presentation']={'custom_boss_bars':1,'vfx_density':1}
    creatures=[]
    for name,title,subtitle,category,description,abilities,_ in catalog:
        model=export_glb(name);cfg='remnant/bosses/'+name if category=='Boss' else 'remnant/monsters/'+name
        stats=configs.get(cfg,configs.get('remnant/balance/'+name+'_stats',{}))
        hp=next((v for k,v in stats.items() if k in ['max_health','max_health_phase_1',name+'_health']),None)
        creatures.append(dict(id=name,name=title,subtitle=subtitle,category=category,description=description,abilities=abilities.split('|'),model=model,health=hp,config=cfg if cfg in configs else 'remnant/balance/'+name+'_stats',playable=category!='Asset study'))
        if (OUT/f'thumbnails/{name}.png').exists():creatures[-1]['thumbnail']=f'thumbnails/{name}.png'
        folder=ROOT/'docs/models'/name
        if (folder/(name+'.bbmodel')).exists():
            (OUT/'downloads').mkdir(exist_ok=True);shutil.copyfile(folder/(name+'.bbmodel'),OUT/f'downloads/{name}.bbmodel')
    lang=json.loads((ASSETS/'lang/en_us.json').read_text())
    items=[]
    for file in ['ModItems.java','ModBlocks.java']:
        for id in re.findall(r'new RegistryHolder<>\("([^"]+)"',(JAVA/'init'/file).read_text()):
            if id in [i['id'] for i in items]:continue
            items.append({'id':id,'name':lang.get('item.remnant_bosses.'+id,lang.get('block.remnant_bosses.'+id,id.replace('_',' ').title())),'category':'Block' if file=='ModBlocks.java' else 'Item'})
    recipes=[]
    for p in (ROOT/'1.21.1/common/src/main/resources/data/remnant_bosses/recipes').glob('*.json'):
        d=json.loads(p.read_text());recipes.append({'id':p.stem,**d})
    entities=re.findall(r'RegistryHolder.entity\(\s*"([^"]+)"',(JAVA/'init/ModEntities.java').read_text())
    data={'version':'2.6.0','creatures':creatures,'configs':configs,'items':items,'recipes':recipes,'entityIds':entities}
    (OUT/'codex.json').write_text(json.dumps(data,indent=2))
    print('Exported',len(creatures),'creatures,',len(configs),'config files,',len(recipes),'recipes.')

if __name__=='__main__':main()
