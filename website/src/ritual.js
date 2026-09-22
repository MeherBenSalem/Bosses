import * as THREE from 'three';
import {OrbitControls} from 'three/addons/controls/OrbitControls.js';

export const ritualPositions = [
  {key:'four',label:'North',x:0,z:-3,offset:'0, 0, −3'},
  {key:'two',label:'West',x:-3,z:0,offset:'−3, 0, 0'},
  {key:'one',label:'East',x:3,z:0,offset:'+3, 0, 0'},
  {key:'three',label:'South',x:0,z:3,offset:'0, 0, +3'},
];
const faceNames=['east','west','up','down','south','north'];

// BoxGeometry stores four independent UV vertices for each Minecraft face.
function setUV(geometry,faces,width=16,height=16){
  const uv=geometry.attributes.uv;
  faceNames.forEach((name,i)=>{
    const face=faces[name],rect=face?.uv||[0,0,width,height];
    const [u0,v0,u1,v1]=rect;
    let corners=[[u0,v0],[u1,v0],[u0,v1],[u1,v1]];
    const order=[0,1,3,2];
    for(let turn=0;turn<(face?.rotation||0)/90;turn++){
      const prev=corners;corners=[];order.forEach((slot,j)=>corners[slot]=prev[order[(j+3)%4]]);
    }
    corners.forEach(([u,v],j)=>uv.setXY(i*4+j,u/width,1-v/height));
  });
  uv.needsUpdate=true;
}

export class RitualViewer {
  constructor(host,config,onSelect){
    this.host=host;this.config=config;this.onSelect=onSelect;this.dead=false;
    this.textures=new Map();this.pickables=[];
    this.scene=new THREE.Scene();this.scene.background=new THREE.Color('#101914');
    this.camera=new THREE.PerspectiveCamera(40,1,.1,80);
    this.renderer=new THREE.WebGLRenderer({antialias:true});
    this.renderer.setPixelRatio(Math.min(devicePixelRatio,2));
    this.renderer.outputColorSpace=THREE.SRGBColorSpace;
    host.append(this.renderer.domElement);
    this.renderer.domElement.setAttribute('aria-label','Interactive 3D summoning altar and four pedestal offerings');
    this.controls=new OrbitControls(this.camera,this.renderer.domElement);
    this.controls.enablePan=false;this.controls.minDistance=7;this.controls.maxDistance=23;
    this.controls.maxPolarAngle=Math.PI/2-.08;
    this.controls.addEventListener('change',()=>this.render());
    this.scene.add(new THREE.HemisphereLight(0xe1f8e9,0x364238,2.7));
    const sun=new THREE.DirectionalLight(0xffe4ba,3);sun.position.set(3,9,5);this.scene.add(sun);
    const floor=new THREE.Mesh(new THREE.BoxGeometry(9,.16,9),new THREE.MeshStandardMaterial({color:0x24332b,roughness:1}));
    floor.position.y=-.1;this.scene.add(floor);
    const grid=new THREE.GridHelper(9,9,0x60735f,0x344a3d);grid.position.y=-.01;this.scene.add(grid);
    this.ring=new THREE.Mesh(new THREE.RingGeometry(.65,.72,48),new THREE.MeshBasicMaterial({color:0xb3d597,side:THREE.DoubleSide}));
    this.ring.rotation.x=-Math.PI/2;this.ring.position.y=.02;this.ring.visible=false;this.scene.add(this.ring);
    ritualPositions.forEach(p=>this.label(p.label.toUpperCase(),p.x*1.35,p.z*1.35));
    this.resize=new ResizeObserver(()=>{
      const w=host.clientWidth,h=host.clientHeight;
      if(!w||!h)return;
      this.renderer.setSize(w,h);this.camera.aspect=w/h;this.camera.updateProjectionMatrix();this.render();
    });this.resize.observe(host);
    this.pointerDown=e=>{this.down=[e.clientX,e.clientY];};
    this.pointerUp=e=>{
      if(!this.down||Math.hypot(e.clientX-this.down[0],e.clientY-this.down[1])>5)return;
      const r=this.renderer.domElement.getBoundingClientRect(),ray=new THREE.Raycaster();
      ray.setFromCamera(new THREE.Vector2((e.clientX-r.left)/r.width*2-1,-(e.clientY-r.top)/r.height*2+1),this.camera);
      const hit=ray.intersectObjects(this.pickables,true)[0];
      if(hit){let obj=hit.object;while(obj&&!obj.userData.direction)obj=obj.parent;if(obj){this.select(obj.userData.direction);onSelect(obj.userData.direction);}}
    };
    host.addEventListener('pointerdown',this.pointerDown);host.addEventListener('pointerup',this.pointerUp);
    this.reset();
    this.load().catch(error=>{if(!this.dead){host.dataset.loaded='error';const note=document.createElement('p');note.className='ritual-error';note.textContent='The 3D layout could not load. The exact recipe and offsets are listed below.';host.append(note);console.error('Ritual viewer:',error);}});
  }
  async texture(name){
    if(!this.textures.has(name))this.textures.set(name,new THREE.TextureLoader().loadAsync(import.meta.env.BASE_URL+`ritual/${name}.png`).then(t=>{
      t.colorSpace=THREE.SRGBColorSpace;t.magFilter=THREE.NearestFilter;t.minFilter=THREE.NearestFilter;
      // Vanilla animated block PNGs stack square frames vertically; show frame zero.
      if(!name.endsWith('_skull')&&t.image.height>t.image.width){t.repeat.y=t.image.width/t.image.height;t.offset.y=1-t.repeat.y;}
      if(this.dead)t.dispose();return t;
    }));
    return this.textures.get(name);
  }
  block(model,material){
    const group=new THREE.Group();
    for(const element of model.elements){
      const size=element.to.map((v,i)=>(v-element.from[i])/16);
      const geometry=new THREE.BoxGeometry(...size);setUV(geometry,element.faces);
      const mesh=new THREE.Mesh(geometry,material);
      mesh.position.set(...element.to.map((v,i)=>(v+element.from[i])/32-(i===1?0:.5)));
      if(element.rotation){
        const pivot=new THREE.Group(),rotation=element.rotation;
        pivot.position.set(...rotation.origin.map((v,i)=>v/16-(i===1?0:.5)));
        mesh.position.sub(pivot.position);pivot.rotation[rotation.axis]=THREE.MathUtils.degToRad(rotation.angle);
        pivot.add(mesh);group.add(pivot);
      }else group.add(mesh);
    }
    return group;
  }
  async offering(id){
    const name=id.split(':')[1],skull=name.endsWith('_skull');
    const names=name==='bone_block'?['bone_block_side','bone_block_side','bone_block_top','bone_block_top','bone_block_side','bone_block_side']:Array(6).fill(name);
    const textures=await Promise.all(names.map(n=>this.texture(n)));
    if(this.dead)return null;
    const geometry=new THREE.BoxGeometry(skull?.5:1,skull?.5:1,skull?.5:1);
    if(skull)setUV(geometry,{east:{uv:[0,8,8,16]},west:{uv:[16,8,24,16]},up:{uv:[8,0,16,8]},down:{uv:[16,0,24,8]},south:{uv:[24,8,32,16]},north:{uv:[8,8,16,16]}},textures[0].image.width,textures[0].image.height);
    const mesh=new THREE.Mesh(geometry,textures.map(map=>new THREE.MeshStandardMaterial({map,roughness:1})));
    mesh.position.y=skull?1.25:1.5;return mesh;
  }
  async load(){
    const [response,atlas]=await Promise.all([fetch(import.meta.env.BASE_URL+'ritual/models.json'),this.texture('alter')]);
    if(!response.ok)throw Error('Ritual geometry unavailable');
    const models=await response.json();if(this.dead)return;
    const material=new THREE.MeshStandardMaterial({map:atlas,roughness:1});
    this.scene.add(this.block(models.alter,material));
    await Promise.all(ritualPositions.map(async p=>{
      const offering=await this.offering(this.config[`pedestal_${p.key}_activation_block`]);if(this.dead)return;
      const pedestal=this.block(models.pedestal,material);pedestal.position.set(p.x,0,p.z);
      pedestal.userData.direction=p.key;pedestal.add(offering);this.pickables.push(pedestal);this.scene.add(pedestal);
    }));
    if(this.dead){material.dispose();return;}
    this.host.dataset.loaded='true';this.host.dataset.textured='true';this.render();
  }
  label(text,x,z){
    const canvas=document.createElement('canvas');canvas.width=256;canvas.height=64;
    const context=canvas.getContext('2d');context.fillStyle='#c0d1bf';context.textAlign='center';context.font='500 27px sans-serif';context.fillText(text,128,40);
    const texture=new THREE.CanvasTexture(canvas),sprite=new THREE.Sprite(new THREE.SpriteMaterial({map:texture,depthTest:false}));
    sprite.scale.set(1.65,.4125,1);sprite.position.set(x,.18,z);this.scene.add(sprite);
  }
  reset(top=false){this.camera.position.set(...(top?[0,16,.01]:[10,11,13]));this.controls.target.set(0,.4,0);this.controls.update();this.render();}
  select(key){const p=ritualPositions.find(p=>p.key===key);this.ring.position.set(p.x,.02,p.z);this.ring.visible=true;this.host.dataset.selected=key;this.render();}
  render(){if(!this.dead)this.renderer.render(this.scene,this.camera);}
  dispose(){
    this.dead=true;this.resize.disconnect();this.controls.dispose();
    this.host.removeEventListener('pointerdown',this.pointerDown);this.host.removeEventListener('pointerup',this.pointerUp);
    const materials=new Set(),textures=new Set();
    this.scene.traverse(o=>{o.geometry?.dispose();if(o.material)(Array.isArray(o.material)?o.material:[o.material]).forEach(m=>materials.add(m));});
    materials.forEach(m=>{if(m.map)textures.add(m.map);m.dispose();});textures.forEach(t=>t.dispose());
    this.textures.forEach(p=>p.then(t=>t.dispose()).catch(()=>{}));this.renderer.dispose();this.renderer.domElement.remove();
  }
}
