import * as THREE from 'three';
import {GLTFLoader} from 'three/addons/loaders/GLTFLoader.js';
import {OrbitControls} from 'three/addons/controls/OrbitControls.js';

export function assertTextured(root){
  let meshes=0;
  root.traverse(o=>{if(!o.isMesh)return;meshes++;for(const m of Array.isArray(o.material)?o.material:[o.material]){if(!m.map?.image?.width||!m.map?.image?.height)throw new Error('Model texture failed to decode');}});
  if(!meshes)throw new Error('Model contains no meshes');
}
export function disposeModel(root){root?.traverse(o=>{o.geometry?.dispose();for(const m of o.material?(Array.isArray(o.material)?o.material:[o.material]):[]){m.map?.dispose();m.dispose();}});}

export class CreatureViewer {
  constructor(host,{onLoad,onError}={}) {
    this.host=host;this.onLoad=onLoad;this.onError=onError;this.disposed=false;this.serial=0;this.playing=!matchMedia('(prefers-reduced-motion: reduce)').matches;this.speed=1;
    this.renderer=new THREE.WebGLRenderer({antialias:true,alpha:true,powerPreference:'low-power'});
    this.renderer.setPixelRatio(Math.min(devicePixelRatio,1.6));this.renderer.outputColorSpace=THREE.SRGBColorSpace;this.renderer.toneMapping=THREE.ACESFilmicToneMapping;this.renderer.toneMappingExposure=1.35;
    this.renderer.domElement.setAttribute('aria-label','Interactive 3D creature. Drag to orbit; scroll to zoom.');this.renderer.domElement.setAttribute('role','img');host.append(this.renderer.domElement);
    this.scene=new THREE.Scene();this.camera=new THREE.PerspectiveCamera(35,1,.05,100);this.camera.position.set(6,3.2,-9);
    this.controls=new OrbitControls(this.camera,this.renderer.domElement);this.controls.target.set(0,1.8,0);this.controls.enableDamping=true;this.controls.enablePan=false;this.controls.minDistance=4;this.controls.maxDistance=15;this.controls.maxPolarAngle=Math.PI*.57;this.controls.autoRotate=this.playing;this.controls.autoRotateSpeed=.35;
    this.scene.add(new THREE.HemisphereLight(0xd5eee1,0x48413a,2.2));
    const key=new THREE.DirectionalLight(0xffeed2,3.2);key.position.set(-4,6,-5);this.scene.add(key);
    const rim=new THREE.DirectionalLight(0x93d6a0,3.5);rim.position.set(4,3,3);this.scene.add(rim);
    this.stage=new THREE.Group();this.scene.add(this.stage);
    const base=new THREE.Mesh(new THREE.CylinderGeometry(2.7,2.85,.16,80),new THREE.MeshStandardMaterial({color:0x1b2520,roughness:.85}));base.position.y=-.15;this.stage.add(base);
    for(const r of [2.3,2.5,2.78]) {
      const ring=new THREE.Mesh(new THREE.RingGeometry(r,r+.012,96),new THREE.MeshBasicMaterial({color:0x667957,transparent:true,opacity:.55,side:THREE.DoubleSide}));ring.rotation.x=-Math.PI/2;ring.position.y=-.059;this.stage.add(ring);
    }
    for(let i=0;i<32;i++) {
      const mark=new THREE.Mesh(new THREE.BoxGeometry(.025,.012,i%4===0?.18:.07),new THREE.MeshBasicMaterial({color:i%4===0?0xa7c286:0x47533f}));mark.position.set(Math.sin(i*Math.PI/16)*2.6,-.05,Math.cos(i*Math.PI/16)*2.6);mark.rotation.y=i*Math.PI/16;this.stage.add(mark);
    }
    this.clock=new THREE.Clock();this.resize=new ResizeObserver(()=>this.fit());this.resize.observe(host);this.visible=true;this.intersection=new IntersectionObserver(entries=>{this.visible=entries[0].isIntersecting;});this.intersection.observe(host);
    this.contextLost=e=>{e.preventDefault();this.onError?.('3D rendering paused. Reload the page to restore the viewer.');};this.renderer.domElement.addEventListener('webglcontextlost',this.contextLost);
    this.frame=()=>{if(this.disposed)return;this.raf=requestAnimationFrame(this.frame);const dt=Math.min(this.clock.getDelta(),.05);if(document.hidden||!this.visible)return;if(this.playing)this.mixer?.update(dt*this.speed);this.controls.update();this.renderer.render(this.scene,this.camera);};this.frame();
  }
  fit(){const {width,height}=this.host.getBoundingClientRect();if(!width||!height)return;this.renderer.setSize(width,height);this.camera.aspect=width/height;this.camera.updateProjectionMatrix();}
  release(root){disposeModel(root);}
  async load(url){
    const serial=++this.serial;
    try{
      const gltf=await new GLTFLoader().loadAsync(url);
      if(this.disposed||serial!==this.serial){this.release(gltf.scene);return;}
      assertTextured(gltf.scene);this.host.dataset.textured='true';
      if(this.model){this.mixer?.stopAllAction();this.scene.remove(this.model);this.release(this.model);}
      this.model=new THREE.Group();const asset=gltf.scene;this.model.add(asset);
      const box=new THREE.Box3().setFromObject(asset),size=box.getSize(new THREE.Vector3()),center=box.getCenter(new THREE.Vector3());
      const scale=4/Math.max(size.x,size.y,size.z);asset.scale.setScalar(scale);asset.position.set(-center.x*scale,-box.min.y*scale,-center.z*scale);
      this.scene.add(this.model);this.mixer=new THREE.AnimationMixer(asset);this.clips=gltf.animations;this.play(this.clips.find(c=>/idle/i.test(c.name))?.name||this.clips[0]?.name);
      this.reset();this.onLoad?.(this.clips);
    }catch(error){if(!this.disposed&&serial===this.serial)this.onError?.('The 3D model could not load. The field notes below are still available.');console.error(error);}
  }
  play(name){if(!this.mixer)return;this.mixer.stopAllAction();const clip=this.clips.find(c=>c.name===name);if(!clip)return;this.action=this.mixer.clipAction(clip);this.action.reset();this.action.play();}
  reset(){this.camera.position.set(6,3.2,-9);this.controls.target.set(0,1.8,0);this.controls.update();}
  setWireframe(value){this.model?.traverse(o=>{if(o.isMesh)o.material.wireframe=value;});}
  scrub(value){if(this.action)this.mixer.setTime(this.action.getClip().duration*value);}
  dispose(){this.disposed=true;cancelAnimationFrame(this.raf);this.resize.disconnect();this.intersection.disconnect();this.controls.dispose();this.release(this.scene);this.renderer.domElement.removeEventListener('webglcontextlost',this.contextLost);this.renderer.dispose();this.renderer.forceContextLoss();this.renderer.domElement.remove();}
}
