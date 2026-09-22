import {defineConfig} from '@playwright/test';
export default defineConfig({testDir:'./tests',workers:1,timeout:45000,use:{baseURL:process.env.CODEX_SITE_URL||'http://127.0.0.1:5173/remnants/',viewport:{width:1440,height:1000},launchOptions:{args:['--enable-webgl','--use-angle=swiftshader']},screenshot:'only-on-failure'},reporter:'list'});
