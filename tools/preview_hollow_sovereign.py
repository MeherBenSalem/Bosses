"""Record a short Blockbench MCP viewport reel. Requires ffmpeg on PATH."""
import base64
import subprocess
import tempfile
from pathlib import Path
from build_hollow_sovereign import call, rpc, OUT

rpc('initialize',{'protocolVersion':'2024-11-05','capabilities':{},'clientInfo':{'name':'animation-preview','version':'1'}})
call('animation_timeline',action='pause')
call('set_camera_angle',position=[100,72,-140],target=[0,39,0],projection='perspective')
with tempfile.TemporaryDirectory(prefix='sovereign-preview-') as directory:
    index=0
    for clip,length in [('idle',4),('claw_combo',1.8),('ground_slam',2.2),('core_blast',2.6)]:
        call('risky_eval',code=f"Modes.options.animate.select(); Animation.all.find(a=>a.name==='animation.hollow_sovereign.{clip}').select(); 'Selected';")
        for frame in range(round(length*12)):
            call('animation_timeline',action='set_time',time=frame/12)
            shot=call('capture_screenshot')
            for c in shot['content']:
                if c['type']=='image':
                    (Path(directory)/f'{index:04d}.png').write_bytes(base64.b64decode(c['data']))
            index+=1
        print('Captured '+clip,flush=True)
    subprocess.run(['ffmpeg','-y','-loglevel','error','-framerate','12','-i',str(Path(directory)/'%04d.png'),'-vf','pad=ceil(iw/2)*2:ceil(ih/2)*2','-c:v','libx264','-pix_fmt','yuv420p','-crf','20','-movflags','+faststart',str(OUT/'animation_preview.mp4')],check=True)
call('risky_eval',code="Animation.all.find(a=>a.name==='animation.hollow_sovereign.idle').select(); 'Idle selected';")
call('animation_timeline',action='play')
print('Saved animation_preview.mp4',flush=True)
