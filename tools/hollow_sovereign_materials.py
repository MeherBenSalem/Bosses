"""Map Sovereign faces onto opaque regions of the unchanged Kotsukage atlas."""
import json
import math
from PIL import Image

def map_faces(root, batches):
    assets=root/'1.21.1/common/src/main/resources/assets/remnant_bosses'
    atlas=Image.open(assets/'textures/entities/kotsukage.png').convert('RGBA')
    reference=json.loads((assets/'geo/entity/kotsukage.geo.json').read_text())['minecraft:geometry'][0]
    patches=[]
    seen=set()
    for bone in reference['bones']:
        for cube in bone.get('cubes',[]):
            for face in cube['uv'].values():
                u,v=face['uv']; w,h=face['uv_size']
                box=tuple(map(int,(min(u,u+w),min(v,v+h),max(u,u+w),max(v,v+h))))
                if box in seen or box[0]==box[2] or box[1]==box[3]: continue
                seen.add(box)
                pixels=list(atlas.crop(box).get_flattened_data())
                if any(p[3]!=255 for p in pixels): continue
                color=[sum(p[i] for p in pixels)/len(pixels) for i in range(3)]
                patches.append((box,color))
    # Exact existing accent/occlusion pixels; the atlas is never painted or resampled.
    pixels=[(atlas.getpixel((x,y)),x,y) for y in range(atlas.height) for x in range(atlas.width) if atlas.getpixel((x,y))[3]==255]
    teal=max(pixels,key=lambda p:p[0][1]-p[0][0]-.15*abs(p[0][1]-p[0][2]))
    shadow=min(pixels,key=lambda p:sum(p[0][:3]))
    assignments=[]
    for (group,material),cubes in batches.items():
        for cube in cubes:
            sx,sy,sz=[b-a for a,b in zip(cube['from'],cube['to'])]
            faces={}
            for face,(w,h) in {'north':(sx,sy),'south':(sx,sy),'east':(sz,sy),'west':(sz,sy),'up':(sx,sz),'down':(sx,sz)}.items():
                if material in (2,3,5):
                    _,u,v=shadow if material==5 else teal
                    uv=[u,v,u+1,v+1]
                else:
                    # Bone surfaces retain the source's authored cracks and edge shading.
                    target={0:145,1:164,4:96}[material]
                    choices=[p for p in patches if p[1][0]>p[1][1]*1.12 and abs(p[1][0]-target)<35]
                    def score(p):
                        u,v,u2,v2=p[0]; pw,ph=u2-u,v2-v
                        return 3*abs(math.log((pw/ph)/(w/h)))+abs(math.log(pw/w))+abs(math.log(ph/h))+abs(p[1][0]-target)/40
                    uv=list(min(choices,key=score)[0])
                faces[face]=uv
            assignments.append({'group':group,'name':cube['name'],'faces':faces})
    return assignments
