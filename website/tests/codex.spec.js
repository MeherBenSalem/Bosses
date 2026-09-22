import {test,expect} from '@playwright/test';

test('3D rituals load every default offering and crafting recipe',async({page})=>{
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  page.on('console',m=>{if(m.type()==='error'&&/Ritual viewer|texture|WebGL/i.test(m.text()))errors.push(m.text());});
  for(const [boss,item] of [['hollow_sovereign','Heart Of The Sea'],['ossukage','Nether Star'],['kotsukage','Wither Skeleton Skull'],['umbrakar','Echo Shard']]){
    await page.goto('./#/rituals/'+boss);
    await expect(page.locator('#ritual-stage')).toHaveAttribute('data-textured','true');
    await expect(page.locator('#ritual-stage canvas')).toHaveCount(1);
    await expect(page.locator('.default-recipe')).toContainText(item);
    await expect(page.locator('.offering-choice')).toHaveCount(4);
    await page.locator('[data-direction="one"]').click();
    await expect(page.locator('#ritual-stage')).toHaveAttribute('data-selected','one');
    await page.getByRole('button',{name:'Top view',exact:true}).click();
    await page.getByLabel('Reset ritual camera').click();
    await expect(page.locator('.recipe-grid')).toHaveCount(2);
    await expect(page.locator('.recipe-grid .ingredient')).toHaveCount(18);
    const broken=await page.locator('.recipe-grid img').evaluateAll(imgs=>imgs.filter(i=>!i.complete||!i.naturalWidth).length);expect(broken).toBe(0);
  }
  await page.locator('#ritual-stage').scrollIntoViewIfNeeded();await page.screenshot({path:'preview-ritual.png',fullPage:false});
  await page.setViewportSize({width:390,height:844});
  await expect(page.locator('#ritual-stage canvas')).toBeVisible();
  expect(await page.evaluate(()=>document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);
  await page.screenshot({path:'preview-ritual-mobile.png',fullPage:false});
  expect(errors).toEqual([]);
});

test('removed creature is absent from the archive and search',async({page})=>{
  await page.goto('./#/bestiary/marrowmaw');
  await expect(page.locator('.dossier h2')).toHaveText('Hollow Sovereign');
  await expect(page.locator('[data-specimen="marrowmaw"]')).toHaveCount(0);
  const catalog=await page.evaluate(async()=>await (await fetch('./codex.json')).text());
  expect(catalog.toLowerCase()).not.toContain('marrowmaw');
  await page.getByRole('button',{name:'Search documentation'}).click();await page.getByLabel('Search the codex').fill('marrowmaw');
  await expect(page.locator('.search-result')).toHaveCount(0);
});

test('3D model, animation switching, wireframe, pause, and scrub',async({page})=>{
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto('./#/bestiary/hollow_sovereign');await expect(page.locator('#stage')).toHaveAttribute('data-loaded','true');
  await expect(page.locator('#clip option')).toHaveCount(9);
  await page.getByLabel('Animation clip').selectOption('ground_slam');
  await page.getByLabel('Toggle wireframe').click();await expect(page.getByLabel('Toggle wireframe')).toHaveAttribute('aria-pressed','true');
  await page.getByRole('button',{name:'Pause animation',exact:true}).click();await expect(page.getByRole('button',{name:'Play animation',exact:true})).toBeVisible();
  await page.getByLabel('Animation timeline').fill('0.5');await page.getByLabel('Reset camera').click();
  await expect(page.locator('canvas')).toHaveCount(2);expect(errors).toEqual([]);
});
test('bestiary filters and native model conversion',async({page})=>{
  await page.goto('./');await page.getByRole('button',{name:'Bosses',exact:true}).click();await expect(page.locator('.creature-card')).toHaveCount(4);
  await page.getByRole('button',{name:'Monsters',exact:true}).click();await expect(page.locator('.creature-card')).toHaveCount(9);
  await page.goto('./#/bestiary/wraith');await expect(page.locator('#stage')).toHaveAttribute('data-textured','true');await expect(page.locator('#clip option')).not.toHaveCount(0);
});
test('ritual choices expose exact cardinal offerings',async({page})=>{
  await page.goto('./#/rituals/hollow_sovereign');await page.getByRole('button',{name:'East offering: Bone Block',exact:true}).click();
  await expect(page.locator('#offering-detail')).toContainText('+3, 0, 0');await expect(page.locator('#offering-detail')).toContainText('minecraft:bone_block');
  await page.getByRole('link',{name:'Umbrakar',exact:true}).click();await expect(page.getByRole('button',{name:'North offering: Sculk',exact:true})).toBeVisible();
});
test('global search finds creature and config content',async({page})=>{
  await page.goto('./#/start');await page.getByRole('button',{name:'Search documentation'}).click();await page.getByLabel('Search the codex').fill('dirge');
  await page.locator('.search-result').filter({hasText:'Dirge Lantern'}).first().click();await expect(page.locator('.dossier h2')).toHaveText('Dirge Lantern');
  await expect(page.locator('#stage')).toHaveAttribute('data-loaded','true');
});
test('config filtering and valid downloadable defaults',async({page})=>{
  await page.goto('./#/config');await page.getByLabel('Filter configuration').fill('grave_skitter');await expect(page.locator('.config-file')).toHaveCount(1);
  const downloadPromise=page.waitForEvent('download');await page.getByRole('button',{name:'Download defaults'}).click();const dl=await downloadPromise;expect(dl.suggestedFilename()).toBe('grave_skitter.json');
  const stream=await dl.createReadStream();let text='';for await(const chunk of stream)text+=chunk;expect(JSON.parse(text).max_health).toBe(24);
});
test('mobile layout and navigation remain usable',async({page})=>{
  await page.setViewportSize({width:390,height:844});await page.goto('./');await expect(page.locator('#stage')).toHaveAttribute('data-loaded','true');
  expect(await page.evaluate(()=>document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);
  await page.getByRole('button',{name:'Open navigation'}).click();await page.locator('.sidebar').getByRole('link',{name:'Configuration',exact:true}).click();
  await expect(page.locator('h1')).toContainText('Make it your world');expect(await page.evaluate(()=>document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);
  await page.screenshot({path:'preview-mobile.png',fullPage:false,animations:'disabled'});
});

test('sidebar omits version and promotional status copy',async({page})=>{
  await page.goto('./#/start');
  await expect(page.locator('.sidebar .version')).toHaveCount(0);
  await expect(page.locator('.sidebar .status')).toHaveCount(0);
  await expect(page.locator('.sidebar')).not.toContainText('A field guide to the things');
  await expect(page.locator('.sidebar')).not.toContainText('supported');
  await expect(page.locator('.sidebar').getByRole('link',{name:/Source on GitHub/})).toBeVisible();
});
test('all reference pages render without runtime errors',async({page})=>{
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  for(const route of ['start','items','reference','config','rituals']){await page.goto('./#/'+route);await expect(page.locator('h1')).toBeVisible();await expect(page.locator('footer')).toBeVisible();}
  expect(errors).toEqual([]);
});
test('reduced motion starts paused',async({page})=>{
  await page.emulateMedia({reducedMotion:'reduce'});await page.goto('./#/bestiary/grave_skitter');await expect(page.locator('#stage')).toHaveAttribute('data-loaded','true');await expect(page.getByRole('button',{name:'Play animation',exact:true})).toBeVisible();
});


test('creation section is removed, including old route and search',async({page})=>{
  await page.goto('./#/create');await expect(page.locator('.dossier')).toBeVisible();await expect(page.getByRole('link',{name:'Create a monster',exact:true})).toHaveCount(0);
  await page.getByRole('button',{name:'Search documentation'}).click();await page.getByLabel('Search the codex').fill('create a monster');await expect(page.locator('.search-result')).toHaveCount(0);
});

test('all bestiary cards have decoded textures and use one shared renderer',async({page})=>{
  const failures=[];page.on('console',m=>{if(m.type()==='error'&&/texture|blob:|GLTFLoader|Bestiary preview/i.test(m.text()))failures.push(m.text());});page.on('pageerror',e=>failures.push(e.message));
  await page.goto('./');await expect(page.locator('#stage')).toHaveAttribute('data-textured','true');
  const cards=page.locator('[data-specimen]');await expect(cards).toHaveCount(13);
  for(const card of await cards.all()){await card.scrollIntoViewIfNeeded();await expect(card).toHaveAttribute('data-textured','true');}
  await expect(page.locator('.gallery-canvas')).toHaveCount(1);await expect(page.locator('canvas')).toHaveCount(2);expect(failures).toEqual([]);
  await page.locator('#creature-grid').scrollIntoViewIfNeeded();await page.screenshot({path:'preview-gallery.png',fullPage:false,animations:'disabled'});
});
