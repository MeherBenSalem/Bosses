"""Write a dependency-free, textured rig preview (not a Minecraft renderer)."""
import base64
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
assets = root / '1.21.1/common/src/main/resources/assets/remnant_bosses'
data = dict(geometry=json.loads((assets / 'geo/entity/umbrakar.geo.json').read_text()),
            animations=json.loads((assets / 'animations/entity/umbrakar.animation.json').read_text())['animations'],
            texture=base64.b64encode((assets / 'textures/entities/umbrakar.png').read_bytes()).decode())
html = '''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Umbrakar — Riftmaw Colossus</title><style>
*{box-sizing:border-box}body{margin:0;background:#090f19;color:#e1eff4;font:15px system-ui}header{padding:28px 36px;border-bottom:1px solid #253345;display:flex;justify-content:space-between;align-items:center}h1{font-size:28px;margin:4px 0}small{letter-spacing:3px;color:#72d8e9}p{color:#91a6b8;margin:8px 0}main{display:grid;grid-template-columns:1fr 270px;height:calc(100vh - 134px);min-height:520px}canvas{width:100%;height:100%;touch-action:none}aside{padding:24px;border-left:1px solid #253345;background:#101925}button,select{background:#1d2e40;color:#e1eff4;border:1px solid #385065;padding:10px;border-radius:6px;width:100%;margin:6px 0;cursor:pointer}input{width:100%;accent-color:#65d5e7}label{display:block;margin-top:24px;color:#9ab0c2}#clock{font-variant-numeric:tabular-nums;color:#7fe1ef;margin:12px 0}.note{font-size:12px;line-height:1.7;color:#90a4b6}strong{color:#c5e9ee}@media(max-width:720px){main{grid-template-columns:1fr;height:auto}canvas{height:500px}aside{border:0}header{padding:20px}}
</style><header><div><small>REMNANT BOSSES / CREATURE STUDY</small><h1>Umbrakar</h1><p>Riftmaw Colossus · The broken crown</p></div><small>RIG PREVIEW</small></header>
<main><canvas id="view" aria-label="Rotatable animated Umbrakar model"></canvas><aside><strong>Animation</strong><select id="clip" aria-label="Animation clip"></select><button id="play">Pause</button><label for="time">Pose timeline</label><input id="time" type="range" min="0" max="1" step=".001"><div id="clock"></div><label for="zoom">Zoom</label><input id="zoom" type="range" min="3" max="10" value="4.6" step=".1"><button id="front">Front view</button><button id="quarter">Three-quarter view</button><p class="note">Drag the model to orbit.<br>Original texture atlas, articulated crown and blade mane, ten authored clips.</p><p class="note">This is an asset inspection viewer. Minecraft lighting, particles, combat and GeckoLib transitions require an in-game check.</p></aside></main><script>
const DATA=__DATA__;
const canvas=document.querySelector('#view'),ctx=canvas.getContext('2d'), select=document.querySelector('#clip'),slider=document.querySelector('#time');
const bones=DATA.geometry['minecraft:geometry'][0].bones, byName=Object.fromEntries(bones.map(b=>[b.name,b]));
const texture=new Image();texture.src='data:image/png;base64,'+DATA.texture;
for(const name of Object.keys(DATA.animations)){let o=document.createElement('option');o.textContent=name;o.value=name;select.append(o)}
let playing=true,time=0,last=0,yaw=-.63,pitch=.23,drag=null;
select.onchange=()=>{time=0};document.querySelector('#play').onclick=e=>{playing=!playing;e.target.textContent=playing?'Pause':'Play'};
slider.oninput=()=>{time=Number(slider.value)*DATA.animations[select.value].animation_length;playing=false;document.querySelector('#play').textContent='Play'};
document.querySelector('#front').onclick=()=>{yaw=0;pitch=.1};document.querySelector('#quarter').onclick=()=>{yaw=-.63;pitch=.23};
canvas.onpointerdown=e=>{drag=[e.clientX,e.clientY];canvas.setPointerCapture(e.pointerId)};
canvas.onpointermove=e=>{if(drag){yaw+=(e.clientX-drag[0])*.008;pitch=Math.max(-.4,Math.min(.8,pitch+(e.clientY-drag[1])*.006));drag=[e.clientX,e.clientY]}};
canvas.onpointerup=canvas.onpointercancel=()=>drag=null;
function rot(v,r){let[x,y,z]=v;let[a,b,c]=r.map((n,i)=>n*Math.PI/180*(i<2?-1:1));[y,z]=[y*Math.cos(a)-z*Math.sin(a),y*Math.sin(a)+z*Math.cos(a)];[x,z]=[x*Math.cos(b)+z*Math.sin(b),-x*Math.sin(b)+z*Math.cos(b)];return[x*Math.cos(c)-y*Math.sin(c),x*Math.sin(c)+y*Math.cos(c),z]}
function around(v,p,r){return rot(v.map((n,i)=>n-p[i]),r).map((n,i)=>n+p[i])}
function sample(track,t){if(!track)return[0,0,0];if(Array.isArray(track))return track;let keys=Object.keys(track).sort((a,b)=>Number(a)-Number(b));let i=keys.findIndex(k=>k>=t);if(i<0)i=keys.length-1;const val=j=>{let v=track[String(keys[Math.max(0,Math.min(keys.length-1,j))])];v=v.post??v;return v.vector??v};if(i===0)return val(0);let u=(t-keys[i-1])/(keys[i]-keys[i-1]),p0=val(i-2),p1=val(i-1),p2=val(i),p3=val(i+1);return p1.map((v,k)=>v+(p2[k]-v)*u)}
function transform(v,b,anim){let channels=anim.bones[b.name]??{};v=around(v,b.pivot??[0,0,0],sample(channels.rotation,time).map((n,i)=>n+(b.rotation?.[i]??0)));v=v.map((n,i)=>n+sample(channels.position,time)[i]);return b.parent?transform(v,byName[b.parent],anim):v}
const faceIndices={north:[0,1,2,3],south:[5,4,7,6],west:[4,0,3,7],east:[1,5,6,2],up:[3,2,6,7],down:[4,5,1,0]};
function triangle(dst,src){const[x0,y0]=src[0],[x1,y1]=src[1],[x2,y2]=src[2],d=x0*(y1-y2)+x1*(y2-y0)+x2*(y0-y1);if(Math.abs(d)<1e-8)return;const coeff=k=>[(dst[0][k]*(y1-y2)+dst[1][k]*(y2-y0)+dst[2][k]*(y0-y1))/d,(dst[0][k]*(x2-x1)+dst[1][k]*(x0-x2)+dst[2][k]*(x1-x0))/d,(dst[0][k]*(x1*y2-x2*y1)+dst[1][k]*(x2*y0-x0*y2)+dst[2][k]*(x0*y1-x1*y0))/d];let a=coeff(0),b=coeff(1);ctx.save();ctx.beginPath();dst.forEach((p,i)=>i?ctx.lineTo(...p):ctx.moveTo(...p));ctx.closePath();ctx.clip();ctx.transform(a[0],b[0],a[1],b[1],a[2],b[2]);ctx.drawImage(texture,0,0);ctx.restore()}
function render(now){let dt=last?(now-last)/1000:0;last=now;const anim=DATA.animations[select.value];if(playing){time+=Math.min(dt,.05);if(time>anim.animation_length)time=anim.loop?time%anim.animation_length:anim.animation_length}slider.value=time/anim.animation_length;document.querySelector('#clock').textContent=time.toFixed(2)+' / '+anim.animation_length.toFixed(2)+' s';
const w=canvas.clientWidth,h=canvas.clientHeight;if(canvas.width!==w||canvas.height!==h){canvas.width=w;canvas.height=h}ctx.clearRect(0,0,w,h);ctx.imageSmoothingEnabled=false;let scale=Number(document.querySelector('#zoom').value)*Math.min(w/1000,h/680);
const project=v=>{let[x,y,z]=v;z-=32;y-=35;let xx=x*Math.cos(yaw)+z*Math.sin(yaw),zz=-x*Math.sin(yaw)+z*Math.cos(yaw);return[w/2+xx*scale,h*.51-(y*Math.cos(pitch)+zz*Math.sin(pitch))*scale,zz*Math.cos(pitch)-y*Math.sin(pitch)]};
ctx.strokeStyle='#203044';ctx.lineWidth=1;for(let n=-100;n<=160;n+=16){for(let line of [[[-100,0,n],[100,0,n]],[[n,0,-100],[n,0,160]]]){ctx.beginPath();line.forEach((v,i)=>{let p=project(v);i?ctx.lineTo(p[0],p[1]):ctx.moveTo(p[0],p[1])});ctx.stroke()}}
let faces=[];for(let b of bones){for(let c of b.cubes??[]){let o=c.origin,s=c.size,inflate=c.inflate??0;let verts=[[0,0,0],[1,0,0],[1,1,0],[0,1,0],[0,0,1],[1,0,1],[1,1,1],[0,1,1]].map(v=>v.map((n,i)=>o[i]+n*s[i]+(n?inflate:-inflate))).map(v=>transform(around(v,c.pivot??o,c.rotation??[0,0,0]),b,anim)).map(project);for(let[name,indices]of Object.entries(faceIndices)){let uv=c.uv?.[name];if(!uv||!uv.uv_size||!uv.uv_size[0]||!uv.uv_size[1])continue;let ps=indices.map(i=>verts[i]);faces.push({ps,uv,z:ps.reduce((a,p)=>a+p[2],0)/4,name})}}}
faces.sort((a,b)=>b.z-a.z);for(let f of faces){let[x,y]=f.uv.uv,[u,v]=f.uv.uv_size,uvs=[[x,y+v],[x+u,y+v],[x+u,y],[x,y]];for(let ids of [[0,1,2],[0,2,3]])triangle(ids.map(i=>f.ps[i].slice(0,2)),ids.map(i=>uvs[i]));ctx.fillStyle=f.name==='up'?'rgba(111,215,236,.08)':'rgba(0,0,0,.08)';ctx.beginPath();f.ps.forEach((p,i)=>i?ctx.lineTo(p[0],p[1]):ctx.moveTo(p[0],p[1]));ctx.closePath();ctx.fill()}
requestAnimationFrame(render)}texture.onload=()=>requestAnimationFrame(render);
</script></html>'''
output = root / 'docs/umbrakar-preview.html'
output.parent.mkdir(exist_ok=True)
output.write_text(html.replace('__DATA__', json.dumps(data, separators=(',', ':'))), encoding='utf-8')
print(output)
