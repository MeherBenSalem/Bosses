"""Convert the mod's native Java cuboid rigs and numeric animation channels for web previews."""
import math
import re

def numbers(text):
    return [float(n.strip().removesuffix('F').removesuffix('f')) for n in text.split(',')]

def native_model(java,name):
    path=java/f'client/model/Model{name}.java'
    if not path.exists():return None
    text=path.read_text(encoding='utf-8')
    width,height=map(int,re.search(r'LayerDefinition.create\(meshdefinition,\s*(\d+),\s*(\d+)\)',text).groups())
    bones=[];variables={'partdefinition':{'name':'__native_root__','pivot':[0,24,0]}}
    bones.append({'name':'__native_root__','pivot':[0,24,0]})
    pattern=r'PartDefinition\s+(\w+)\s*=\s*(\w+)\.addOrReplaceChild\("([^"]+)",\s*(.*?),\s*PartPose\.(offsetAndRotation|offset)\(([^)]*)\)\);'
    for variable,parent,name_,cubes,kind,pose in re.findall(pattern,text,re.S):
        values=numbers(pose);p=variables[parent]['pivot'];x,y,z=values[:3];pivot=[p[0]+x,p[1]-y,p[2]+z]
        rotation=values[3:] if kind=='offsetAndRotation' else [0,0,0]
        bone={'name':name_,'parent':variables[parent]['name'],'pivot':pivot,'rotation':[math.degrees(v)*(1 if i==0 else -1) for i,v in enumerate(rotation)],'cubes':[]}
        for u,v,mirror,box,inflation in re.findall(r'\.texOffs\((-?\d+),\s*(-?\d+)\)\s*(\.mirror\(\))?\s*\.addBox\((.*?),\s*new CubeDeformation\(([^)]+)\)\)',cubes,re.S):
            a,b,c,sx,sy,sz=numbers(box)
            bone['cubes'].append({'origin':[pivot[0]+a,pivot[1]-b-sy,pivot[2]+c],'size':[sx,sy,sz],'uv':[int(u),int(v)],'inflate':numbers(inflation)[0],'mirror':bool(mirror)})
        variables[variable]=bone;bones.append(bone)
    assert len(bones)>1 and any(b.get('cubes') for b in bones),name
    assert sum(len(b.get('cubes',[])) for b in bones)==text.count('.addBox('),(name,'unparsed native cubes')
    assert len({b['name'] for b in bones})==len(bones),(name,'duplicate bones')
    animations={}
    animpath=java/f'client/model/animations/{name}Animation.java'
    if animpath.exists():
        animtext=animpath.read_text(encoding='utf-8')
        for clip,length,body in re.findall(r'AnimationDefinition\s+(\w+)\s*=\s*AnimationDefinition.Builder.withLength\(([^)]+)\)(.*?)\.build\(\);',animtext,re.S):
            animation={'animation_length':numbers(length)[0],'loop':'.looping()' in body,'bones':{}}
            for bone,target,frames in re.findall(r'\.addAnimation\("([^"]+)",\s*new AnimationChannel\(AnimationChannel.Targets\.(\w+),(.*?)(?=\.addAnimation|$)',body,re.S):
                channel={'ROTATION':'rotation','POSITION':'position','SCALE':'scale'}[target];keys={}
                for time,vector in re.findall(r'new Keyframe\(([^,]+),\s*KeyframeAnimations\.\w+\(([^)]+)\)',frames):
                    v=numbers(vector)
                    if channel=='rotation':v=[v[0],-v[1],-v[2]]
                    keys[str(numbers(time)[0])]={'vector':v}
                if keys:animation['bones'].setdefault(bone,{})[channel]=keys
            if animation['bones']:animations[clip]=animation
    return {'description':{'texture_width':width,'texture_height':height},'bones':bones},animations
