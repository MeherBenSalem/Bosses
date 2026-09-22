import {defineConfig} from 'vite';
export default defineConfig({base:'/remnants/',server:{host:'127.0.0.1'},build:{rollupOptions:{output:{manualChunks(id){if(id.includes('/node_modules/three/'))return 'three';}}}}});
