import * as THREE from 'three';
import {GLTFLoader} from 'three/addons/loaders/GLTFLoader.js';
import {assertTextured,disposeModel} from './viewer.js';

/** One WebGL context renders all visible bestiary cards using scissored viewports. */
export class BestiaryGallery {
  constructor(host,creatures,base){
    this.host=host;this.entries=[];this.disposed=false;this.reduced=matchMedia('(prefers-reduced-motion: reduce)').matches;
    this.renderer=new THREE.WebGLRenderer({alpha:true,antialias:true,powerPreference:'low-power'});
    this.renderer.setPixelRatio(Math.min(devicePixelRatio,1.25));this.renderer.setClearColor(0,0);this.renderer.toneMapping=THREE.ACESFilmicToneMapping;this.renderer.toneMappingExposure=1.2;
    this.renderer.domElement.className='gallery-canvas';this.renderer.domElement.setAttribute('aria-hidden','true');host.append(this.renderer.domElement);
    this.resize=new ResizeObserver(()=>{this.renderer.setSize(host.clientWidth,host.clientHeight,false);});this.resize.observe(host);
    this.observer=new IntersectionObserver(entries=>{for(const item of entries){const entry=this.entries.find(e=>e.host===item.target);if(entry&&item.isIntersecting&&!entry.started)this.load(entry);}}, {rootMargin:'160px'});
    for(const creature of creatures){
      if(!creature.model)continue;
      const element=host.querySelector(`[data-specimen="${creature.id}"]`);if(!element)continue;
      const scene=new THREE.Scene();scene.add(new THREE.HemisphereLight(0xdde8da,0x494138,2));
      const light=new THREE.DirectionalLight(0xffeed8,3);light.position.set(-3,5,-6);scene.add(light);
      const rim=new THREE.DirectionalLight(0xb8dba7,2);rim.position.set(4,3,3);scene.add(rim);
      const camera=new THREE.PerspectiveCamera(32,1,.05,50);camera.position.set(5,2.5,-8);camera.lookAt(0,1.6,0);
      const entry={host:element,url:base+creature.model.url,scene,camera,started:false,hovered:false};this.entries.push(entry);
      element.parentElement.addEventListener('pointerenter',()=>entry.hovered=true);element.parentElement.addEventListener('pointerleave',()=>entry.hovered=false);
      this.observer.observe(element);
    }
    this.last=performance.now();
    this.frame=now=>{
      if(this.disposed)return;this.raf=requestAnimationFrame(this.frame);if(now-this.last<40)return;
      const dt=Math.min((now-this.last)/1000,.08);this.last=now;if(document.hidden)return;
      const bounds=host.getBoundingClientRect();if(bounds.bottom<0||bounds.top>innerHeight)return;
      this.renderer.setScissorTest(false);this.renderer.clear();this.renderer.setScissorTest(true);
      for(const entry of this.entries){
        const rect=entry.host.getBoundingClientRect();if(!entry.root||rect.bottom<0||rect.top>innerHeight||!rect.width||!rect.height)continue;
        const x=rect.left-bounds.left,y=bounds.bottom-rect.bottom;
        entry.camera.aspect=rect.width/rect.height;entry.camera.updateProjectionMatrix();
        if(!this.reduced){entry.mixer.update(dt);entry.root.rotation.y+=dt*(entry.hovered?.5:.08);}
        this.renderer.setViewport(x,y,rect.width,rect.height);this.renderer.setScissor(x,y,rect.width,rect.height);this.renderer.render(entry.scene,entry.camera);
      }
    };this.raf=requestAnimationFrame(this.frame);
  }
  async load(entry){
    entry.started=true;
    try{
      const gltf=await new GLTFLoader().loadAsync(entry.url);
      if(this.disposed){disposeModel(gltf.scene);return;}
      assertTextured(gltf.scene);
      const asset=gltf.scene,box=new THREE.Box3().setFromObject(asset),size=box.getSize(new THREE.Vector3()),center=box.getCenter(new THREE.Vector3());
      const scale=3.5/Math.max(size.x,size.y,size.z);asset.scale.setScalar(scale);asset.position.set(-center.x*scale,-box.min.y*scale,-center.z*scale);
      entry.root=new THREE.Group();entry.root.add(asset);entry.scene.add(entry.root);entry.mixer=new THREE.AnimationMixer(asset);
      const idle=gltf.animations.find(c=>/idle/i.test(c.name))||gltf.animations[0];if(idle)entry.mixer.clipAction(idle).play();
      entry.host.dataset.loaded='true';entry.host.dataset.textured='true';entry.host.querySelector('.card-loading')?.remove();
    }catch(error){if(!this.disposed){entry.host.dataset.error='true';entry.host.querySelector('.card-loading').textContent='Preview unavailable';}console.error('Bestiary preview failed',error);}
  }
  dispose(){this.disposed=true;cancelAnimationFrame(this.raf);this.observer.disconnect();this.resize.disconnect();for(const e of this.entries){e.mixer?.stopAllAction();disposeModel(e.scene);}this.renderer.dispose();this.renderer.forceContextLoss();this.renderer.domElement.remove();}
}
