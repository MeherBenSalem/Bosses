"""Record actual Blockbench animation previews for the new trio."""
import base64
import subprocess
import tempfile
from pathlib import Path
from build_bone_monsters import ROOT, SPECS
from build_hollow_sovereign import rpc,call

rpc('initialize',{'protocolVersion':'2024-11-05','capabilities':{},'clientInfo':{'name':'trio-preview','version':'1'}})
for name in SPECS:
    out=ROOT/'docs/models'/name
    result=call('risky_eval',code=f"ModelProject.all.find(p=>p.name==='{name}').select(); Modes.options.animate.select();")
    assert 'Error executing code' not in str(result),result
    call('animation_timeline',action='pause')
    call('set_camera_angle',position=[65,48,-90],target=[0,13 if name=='grave_skitter' else 24,0],projection='perspective')
    with tempfile.TemporaryDirectory(prefix='bone-monster-preview-') as d:
        index=0
        for clip,length in [('idle',4),('walk',1.2),('attack',1),('special',1.6),('death',1.5)]:
            call('risky_eval',code=f"Animation.all.find(a=>a.name==='animation.{name}.{clip}').select();")
            for frame in range(round(length*12)):
                call('animation_timeline',action='set_time',time=frame/12)
                shot=call('capture_screenshot')
                for c in shot['content']:
                    if c['type']=='image':
                        data=base64.b64decode(c['data'])
                        (Path(d)/f'{index:04d}.png').write_bytes(data)
                        if clip=='special' and frame==7:(out/'special.png').write_bytes(data)
                index+=1
        subprocess.run(['ffmpeg','-y','-loglevel','error','-framerate','12','-i',str(Path(d)/'%04d.png'),'-vf','pad=ceil(iw/2)*2:ceil(ih/2)*2','-c:v','libx264','-pix_fmt','yuv420p','-crf','20','-movflags','+faststart',str(out/'animation_preview.mp4')],check=True)
    call('risky_eval',code=f"Animation.all.find(a=>a.name==='animation.{name}.idle').select();")
    print(name,'animation preview saved',flush=True)
call('animation_timeline',action='play')
