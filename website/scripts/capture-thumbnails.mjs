import {chromium} from '@playwright/test';
import fs from 'node:fs/promises';
const data=JSON.parse(await fs.readFile('public/codex.json','utf8'));
const browser=await chromium.launch({headless:true,args:['--enable-webgl','--use-angle=swiftshader']});
const page=await browser.newPage({viewport:{width:1440,height:1000},reducedMotion:'reduce'});
await fs.mkdir('public/thumbnails',{recursive:true});
for(const c of data.creatures.filter(c=>c.model)){
  await page.goto(`http://127.0.0.1:5173/remnants/#/bestiary/${c.id}`);
  await page.locator('#stage[data-loaded=true]').waitFor({timeout:30000});
  await page.addStyleTag({content:'.specimen-top,.viewer-note{visibility:hidden}'});
  await page.locator('canvas').screenshot({path:`public/thumbnails/${c.id}.png`});
  c.thumbnail=`thumbnails/${c.id}.png`;
  console.log(c.id);
}
await fs.writeFile('public/codex.json',JSON.stringify(data,null,2));await browser.close();
