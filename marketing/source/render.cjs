// Renders the listing artwork in marketing/images/ from real in-game screenshots and the bundled OFL fonts.
// Usage (from the repository root): NODE_PATH="$(npm root -g)" node marketing/source/render.cjs [name...]
// Requires the `playwright` package with a Chromium build available.
const path = require('path');
const fs = require('fs');
const { chromium } = require('playwright');

// The release and the supported Minecraft versions. Adding a version is one line here
// (plus one row in the compatibility table of curseforge/description.md).
const RELEASE = { version: '3.1', name: 'The Realm Endures' };
const VERSIONS = [
  { minecraft: '1.20.1', loaders: ['Fabric', 'Forge'] },
  { minecraft: '1.21.1', loaders: ['Fabric', 'NeoForge'] },
];
const LOADERS = ['Fabric', 'Forge', 'NeoForge'].filter((loader) => VERSIONS.some((v) => v.loaders.includes(loader)));

const root = path.resolve(__dirname, '..', '..');
const out = path.join(root, 'marketing', 'images');
const shots = path.join(out, 'screenshots');
const fonts = path.join(__dirname, 'fonts');
const url = (file) => 'file://' + file;
const shot = (name) => url(path.join(shots, name));
const source = (name) => url(path.join(__dirname, name));

// Boss accents match the in-game custom boss bars (common/.../client/RemnantBossBars.java).
const accent = { ossukage: '#e55169', warden: '#a6623b', watcher: '#9e86e0', sovereign: '#d9b860' };

const css = `
@font-face { font-family: RbSerif; src: url('${url(path.join(fonts, 'eb-garamond.ttf'))}'); }
@font-face { font-family: RbDisplay; src: url('${url(path.join(fonts, 'cinzel.ttf'))}'); }
:root { --text:#dad3c1; --dim:#9c9584; --faint:#6e685c; --header:#e8dfc8; --gold:#d4b06a; --gold-hi:#f0d58c;
        --blue:#79a2ff; --red:#e0645a; --bg:#0b0a09; --line:rgba(216,207,184,.32); }
* { box-sizing: border-box; }
html, body { margin: 0; background: var(--bg); color: var(--text); font-family: RbSerif, serif; }
.stage { position: relative; overflow: hidden; background: radial-gradient(ellipse at 30% 40%, #1c1915 0%, #0b0a09 70%); }
.display { font-family: RbDisplay, serif; letter-spacing: .04em; }
.gold { background: linear-gradient(180deg, #fff2c8 0%, #f0d58c 35%, #d4b06a 70%, #8c7444 100%);
        -webkit-background-clip: text; background-clip: text; color: transparent; }
.glow { filter: drop-shadow(0 0 18px rgba(212,176,106,.35)); }
.kicker { font-family: RbDisplay, serif; color: var(--gold); letter-spacing: .22em; font-size: 22px; }
.rule { height: 1px; background: linear-gradient(90deg, rgba(212,176,106,0), rgba(212,176,106,.9) 20%, rgba(212,176,106,.9) 70%, rgba(212,176,106,0)); }
.diamond { display: inline-block; width: 9px; height: 9px; background: var(--gold); transform: rotate(45deg); margin: 0 12px 3px 0; }
.frame { position: relative; border: 1px solid rgba(216,207,184,.45); box-shadow: 0 0 0 1px rgba(0,0,0,.6), 0 30px 80px rgba(0,0,0,.65), 0 0 60px rgba(212,176,106,.16); background: #000; }
.frame > img { display: block; width: 100%; }
canvas.motes { position: absolute; left: 0; top: 0; width: 100%; height: 100%; pointer-events: none; }
.vignette { position: absolute; inset: 0; background: radial-gradient(ellipse at center, rgba(0,0,0,0) 45%, rgba(0,0,0,.75) 100%); }
.backdrop { position: absolute; inset: -40px; background-size: cover; background-position: center; filter: blur(16px) brightness(.42) saturate(1.1); }
.pill { display: inline-block; border: 1px solid rgba(216,207,184,.55); padding: 6px 14px 4px; margin-right: 10px;
        font-family: RbDisplay, serif; font-size: 18px; letter-spacing: .12em; color: var(--header); background: rgba(0,0,0,.35); }
.label { position: absolute; font-family: RbDisplay, serif; font-size: 20px; letter-spacing: .14em; color: var(--header);
         background: rgba(10,9,8,.86); border: 1px solid rgba(212,176,106,.8); padding: 7px 14px 5px; white-space: nowrap;
         box-shadow: 0 8px 26px rgba(0,0,0,.6); z-index: 2; }
.label small { display: block; font-family: RbSerif, serif; letter-spacing: 0; font-size: 18px; color: var(--dim); margin-top: 1px; }
ul.points { list-style: none; padding: 0; margin: 0; }
ul.points li { position: relative; padding-left: 26px; margin: 0 0 14px; font-size: 27px; line-height: 1.32; color: var(--text); }
ul.points li::before { content: ''; position: absolute; left: 2px; top: 13px; width: 8px; height: 8px; background: var(--gold); transform: rotate(45deg); }
.stats { display: flex; margin-top: 22px; border: 1px solid rgba(216,207,184,.35); background: rgba(10,9,8,.82); box-shadow: 0 18px 50px rgba(0,0,0,.55); }
.stats > div { flex: 1; padding: 14px 24px 12px; border-left: 1px solid rgba(216,207,184,.2); }
.stats > div:first-child { border-left: 0; }
.stats .t { font-family: RbDisplay, serif; font-size: 16px; letter-spacing: .18em; color: var(--dim); }
.stats .v { font-family: RbDisplay, serif; font-size: 30px; color: var(--header); margin-top: 4px; white-space: nowrap; }
b.hi { color: var(--header); font-weight: normal; }
`;

// Slow golden motes, matching the soulslike ambience of the sibling RAS kit. Deterministic so renders are reproducible.
const motes = `<canvas class="motes"></canvas><script>
(() => { const c = document.querySelector('canvas.motes'); const r = c.getBoundingClientRect();
  c.width = r.width; c.height = r.height; const g = c.getContext('2d'); let s = 7;
  const rnd = () => (s = (s * 16807) % 2147483647) / 2147483647;
  for (let i = 0; i < 90; i++) { const x = rnd() * c.width, y = rnd() * c.height, a = .25 + rnd() * .6, z = 1 + rnd() * 2.2;
    const h = g.createRadialGradient(x, y, 0, x, y, z * 5); h.addColorStop(0, 'rgba(246,222,156,' + a * .55 + ')'); h.addColorStop(1, 'rgba(246,222,156,0)');
    g.fillStyle = h; g.beginPath(); g.arc(x, y, z * 5, 0, Math.PI * 2); g.fill();
    g.fillStyle = 'rgba(255,240,200,' + a + ')'; g.fillRect(x - z / 2, y - z / 2, z, z); } })();
</script>`;

const page = (width, height, body, transparent = false) => `<!doctype html><html><head><meta charset="utf-8"><style>${css}
body { width: ${width}px; height: ${height}px; } .stage { width: ${width}px; height: ${height}px; }
${transparent ? 'html, body, .stage { background: transparent; }' : ''}</style></head>
<body><div class="stage">${body}</div></body></html>`;

const header = (title) => page(1400, 120, `
  <div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;gap:28px;background:linear-gradient(90deg,rgba(0,0,0,0),rgba(20,18,15,.9) 30%,rgba(20,18,15,.9) 70%,rgba(0,0,0,0))">
    <div class="rule" style="width:300px;transform:scaleX(-1)"></div>
    <span class="diamond" style="margin:0"></span>
    <div class="display gold glow" style="font-size:44px;white-space:nowrap">${title}</div>
    <span class="diamond" style="margin:0"></span>
    <div class="rule" style="width:300px"></div>
  </div>`, true);

// A feature card: copy on the left, artwork on the right. Bullets never end on a one-word line.
const card = ({ kicker, title, points, art, width = 1600, height = 900, artWidth = 980 }) => page(width, height, `
  <div class="backdrop" style="background-image:url('${source('backdrop.jpg')}');opacity:.55"></div>
  <div class="vignette"></div>${motes}
  <div style="position:absolute;left:80px;top:0;bottom:0;width:${width - artWidth - 150}px;display:flex;flex-direction:column;justify-content:center">
    <div class="kicker"${kicker.length > 24 ? ' style="font-size:18px;letter-spacing:.12em"' : ''}><span class="diamond"></span>${kicker}</div>
    <div class="display gold glow" style="font-size:58px;line-height:1.08;margin:18px 0 22px">${title}</div>
    <div class="rule" style="width:80%;margin-bottom:30px"></div>
    <ul class="points">${points.map((point) => `<li>${point.replace(/ (\S+)$/, '&nbsp;$1')}</li>`).join('')}</ul>
  </div>
  <div style="position:absolute;right:60px;top:50%;transform:translateY(-50%);width:${artWidth}px">${art}</div>`);

// An exact region (x, y, w, h in 1920x1080 pixels) of a capture, scaled to W pixels wide.
const region = (file, x, y, w, h, W) => `<div style="position:relative;width:${W}px;height:${Math.round(W * h / w)}px;overflow:hidden">
    <img src="${shot(file)}" style="position:absolute;max-width:none;width:${W * 1920 / w}px;left:${-x * W / w}px;top:${-y * W / w}px"></div>`;
const framed = (inner, style = '', color = '') => `<div class="frame" style="${color ? `border-color:${color}99;` : ''}${style}">${inner}</div>`;
const label = (text, style, small = '') => `<div class="label" style="${style}">${text}${small ? `<small>${small}</small>` : ''}</div>`;
const stats = (items, color) => `<div class="stats"${color ? ` style="border-top:2px solid ${color}"` : ''}>${items.map(([t, v, flex]) =>
  `<div${flex ? ` style="flex:${flex}"` : ''}><div class="t">${t}</div><div class="v">${v}</div></div>`).join('')}</div>`;
// Two overlapping captures, like the RAS Status & Records card.
const pair = (first, second, firstLabel, secondLabel, firstSpot = 'left:20px;top:20px', height = 700) => `<div style="position:relative;height:${height}px">
    <div class="frame" style="position:absolute;left:0;top:0;width:74%"><img src="${shot(first)}" style="display:block;width:100%"></div>
    <div class="frame" style="position:absolute;right:0;bottom:0;width:74%"><img src="${shot(second)}" style="display:block;width:100%"></div>
    ${label(firstLabel, firstSpot)}${label(secondLabel, 'right:20px;bottom:20px')}</div>`;
// A boss card's art: the capture in its bar colour with default stats from config/remnant/bosses/*.json.
const bossArt = (file, color, items, crop = [100, 50, 50]) => framed(region(file, ...cropBox(crop), 978), '', color) + stats(items, color);
const cropBox = ([w, x, y]) => { const cw = 1920 * 100 / w, ch = 1080 * 100 / w; return [(1920 - cw) * x / 100, (1080 - ch) * y / 100, cw, ch]; };

const versionText = VERSIONS.map((v) => v.minecraft).join(' · ');

const assets = {
  banner: () => page(1800, 600, `
    <img src="${shot('realm-gracetree.jpg')}" style="position:absolute;right:-60px;top:-110px;width:1440px;-webkit-mask-image:linear-gradient(90deg,transparent 0%,#000 38%);mask-image:linear-gradient(90deg,transparent 0%,#000 38%)">
    <div style="position:absolute;inset:0;background:linear-gradient(90deg,rgba(8,7,6,.96) 0%,rgba(8,7,6,.9) 30%,rgba(8,7,6,.4) 55%,rgba(8,7,6,0) 75%)"></div>
    <div style="position:absolute;inset:0;background:linear-gradient(0deg,rgba(8,7,6,.7) 0%,rgba(8,7,6,0) 30%)"></div>
    <div class="vignette"></div>${motes}
    <div style="position:absolute;left:90px;top:88px;width:900px">
      <div class="kicker"><span class="diamond"></span>${RELEASE.version} · ${RELEASE.name}</div>
      <div class="display gold glow" style="font-size:84px;line-height:1.04;margin:22px 0 26px;white-space:nowrap">Remnant<br>Bosses</div>
      <div class="rule" style="width:600px;margin-bottom:26px"></div>
      <div style="font-size:34px;color:var(--header);line-height:1.3">Cross into the eclipse. Rest at grace.<br>Fell the Remnants.</div>
      <div style="margin-top:36px">${LOADERS.map((l) => `<span class="pill">${l}</span>`).join('')}
        <span style="font-size:24px;color:var(--dim);margin-left:8px">${versionText}</span></div>
    </div>`),

  icon: () => page(512, 512, `
    <div style="position:absolute;inset:0;background:radial-gradient(circle at 50% 45%,#2a241c 0%,#120f0c 55%,#070605 100%)"></div>${motes}
    <div style="position:absolute;left:78px;top:78px;width:356px;height:356px;border-radius:50%;overflow:hidden;box-shadow:inset 0 0 30px rgba(0,0,0,.8)">
      <img src="${source('logo.png')}" style="position:absolute;left:-14px;top:-2px;width:384px;height:384px;image-rendering:pixelated"></div>
    <div style="position:absolute;left:56px;top:56px;width:400px;height:400px;border-radius:50%;border:3px solid var(--gold);box-shadow:0 0 40px rgba(212,176,106,.45), inset 0 0 40px rgba(212,176,106,.25)"></div>
    <div style="position:absolute;left:78px;top:78px;width:356px;height:356px;border-radius:50%;border:1px solid rgba(212,176,106,.55)"></div>
    ${[[256, 56], [456, 256], [256, 456], [56, 256]].map(([x, y]) => `<div style="position:absolute;left:${x - 12}px;top:${y - 12}px;width:24px;height:24px;background:var(--gold);transform:rotate(45deg);box-shadow:0 0 16px rgba(240,213,140,.8)"></div>`).join('')}`),

  'card-realm': () => card({
    kicker: 'A dimension of its own', title: 'The Eclipsed<br>Realm',
    points: ['A black eclipse sun over a permanent golden dusk', 'The Gracetree burning on the northern horizon',
      'Seven biomes, from the Gracewood to the Ossuary Depths', 'Remnant creatures spawn here, never in the overworld',
      'The Realm will not change: Survival players cannot build or break'],
    art: `<div style="position:relative">${framed(`<img src="${shot('realm-gracetree.jpg')}">`)}
      <div class="frame" style="position:absolute;right:-24px;top:-40px;width:330px">${region('eclipse.jpg', 700, 150, 520, 330, 330)}</div></div>
      ${stats([['Biomes', '7'], ['Sky', 'Eternal dusk'], ['Arenas', '3']])}` }),

  'card-waygate': () => card({
    kicker: 'Entering the Realm', title: 'Cross the<br>Waygate',
    points: ['<b class="hi">1</b> Craft an Eclipse Shard: amethyst shard, ender pearl and gold', '<b class="hi">2</b> Craft a Waygate and place its 3 × 4 arch',
      '<b class="hi">3</b> Use an Eclipse Shard on it to kindle it for six seconds', '<b class="hi">4</b> Walk through to an Arrival Shrine with a Waygate home'],
    art: pair('waygate.jpg', 'arrival-shrine.jpg', 'Overworld', 'Arrival Shrine') }),

  'card-graces': () => card({
    kicker: 'Grace Embers', title: 'Rest at<br>grace',
    points: ['Heals you, clears harmful effects and sets your respawn', 'Golden motes point to the nearest arena',
      'Travel to every grace you have found, nearest first', 'Depart the Realm from any grace', 'A beam of light marks each grace from afar'],
    art: `<div style="position:relative;height:680px">${framed(`<img src="${shot('grace-beam.jpg')}">`, 'position:absolute;left:0;top:0;width:100%')}
      <div class="frame" style="position:absolute;right:-24px;bottom:0;width:400px">${region('grace-menu.jpg', 560, 175, 800, 735, 400)}</div></div>` }),

  'card-arenas': () => card({
    kicker: 'Three arenas', title: 'Behind the<br>fog gates',
    points: ['<b class="hi">Hollow Citadel</b>: Tarnished Fields and Ashen Wastes', '<b class="hi">Gravebound Catacombs</b>: deep under the land biomes',
      '<b class="hi">Forgotten Observatory</b>: on the Lantern Heights', 'Players pass the fog gates. Bosses cannot', 'Each arena altar already holds its offerings'],
    art: `<div style="display:flex;flex-direction:column;gap:20px">
      ${framed(region('hollow-citadel.jpg', 0, 40, 1920, 830, 978) + label('Hollow Citadel', 'left:18px;bottom:18px'))}
      <div style="display:flex;gap:20px">
        ${framed(region('forgotten-observatory.jpg', 0, 0, 1920, 1080, 478) + label('Forgotten Observatory', 'left:14px;bottom:14px;font-size:17px'), 'flex:1')}
        ${framed(region('catacomb-chapel.jpg', 0, 0, 1920, 1080, 478) + label('Gravebound Catacombs', 'left:14px;bottom:14px;font-size:17px'), 'flex:1')}</div></div>` }),

  'card-sovereign': () => card({
    kicker: 'The Last King of Vael', title: 'Hollow<br>Sovereign',
    points: ['Sword combos, a throne slam and a guard an axe breaks', 'Phase two: his pauldrons dive and spectral guards rise',
      'Phase three: spectral crescents and a soul lunge', 'At 10% he kneels for the Last Command', 'Raise him at the throne-hall altar of the Hollow Citadel'],
    art: bossArt('hollow-sovereign.jpg', accent.sovereign, [['Health', '720'], ['Phases', '66% · 33%'], ['Summon', 'Sovereign’s Seal', 1.9]], [120, 50, 35]) }),

  'card-warden': () => card({
    kicker: 'Keeper of the Forgotten Dead', title: 'Gravebound<br>Warden',
    points: ['Executioner sweeps and a chop that wedges his blade', 'Chain grabs that drag you in', 'Rows of grave eruptions and rising soldiers',
      'Below half health, chain walls split the arena', 'Break the four anchors to stagger him'],
    art: bossArt('gravebound-warden.jpg', accent.warden, [['Health', '520'], ['Phase two', '50%'], ['Summon', 'Crypt Key', 1.9]], [118, 50, 30]) }),

  'card-watcher': () => card({
    kicker: 'Guardian of the Last Memory', title: 'Forgotten<br>Watcher',
    points: ['Ring volleys of memory bolts and a sweeping beam', 'Teleports between the four lenses of its dome',
      'Below half health, memory illusions join the fight', 'A rotating crystal aegis: strike through the gap', 'Raise it at the altar beneath the Observatory dome'],
    art: bossArt('forgotten-watcher.jpg', accent.watcher, [['Health', '680'], ['Phase two', '50%'], ['Summon', 'Lens of Memory', 1.9]], [112, 40, 30]) }),

  'card-ossukage': () => card({
    kicker: 'The Oni Shogun', title: 'Ossukage',
    points: ['Rises from a rune circle, untouchable until the rite ends', 'A three-strike sword chain up close, kunai at range',
      'At half health it transforms and raises Gravebound Soldiers', 'Phase two dashes in, with an explosion, Darkness or more soldiers',
      'Drops the <b class="hi">Ossukage Blade</b>'],
    art: bossArt('ossukage.jpg', accent.ossukage, [['Health', '800'], ['Phase two', '50%'], ['Summon', 'Nether Star', 1.9]], [110, 55, 20]) }),

  'card-altar': () => card({
    kicker: 'Summoning', title: 'Raise the rite',
    points: ['One Ancient Altar, four Ancient Pedestals', 'Pedestals 3 blocks out: east, west, south and north', 'An offering on top of each pedestal',
      'Use the summon item on the altar', 'Realm bosses answer only inside the Realm'],
    art: `<div style="position:relative">${framed(`<img src="${shot('observatory-altar.jpg')}">`)}
      ${[['East', 'Amethyst Block', 50, 70, 'translate(-50%,0)'], ['West', 'Calcite', 50, 33, 'translate(-50%,-100%)'],
         ['South', 'Chiseled Sandstone', 32, 47, 'translate(-100%,-50%)'], ['North', 'Lectern', 67, 48, 'translate(0,-50%)']]
        .map(([t, s, x, y, tr]) => label(t, `left:${x}%;top:${y}%;transform:${tr}`, s)).join('')}
      ${label('The Forgotten Watcher’s rite · default offerings', 'left:20px;top:20px;font-size:17px')}</div>` }),

  'card-journal': () => card({
    kicker: 'Boss Hunts', title: 'A journal of<br>your hunts',
    points: ['Craft it from a Book, a Bone and an Ink Sac', 'Undiscovered hunts whisper a ritual clue', 'Witness a boss to reveal its real rite',
      'Defeat it and the hunt is recorded', 'Progress stays with you, not the book'],
    art: pair('journal-defeated.jpg', 'journal-undiscovered.jpg', 'Defeated', 'Undiscovered', 'left:74%;top:20px;transform:translateX(calc(-100% - 20px))') }),

  'card-credit': () => card({
    kicker: 'Multiplayer', title: 'Credit where<br>it is due',
    points: ['Everyone who really fought gets the defeat', 'Arrows count for the shooter, pets for their owner', 'Spectators and bystanders get nothing',
      'Saved per player, in each world'],
    art: `<div style="display:flex;flex-direction:column;gap:26px">
      ${[['Witness', 'within 48 blocks · line of sight · not a spectator'], ['Fight', 'only hits that lower its health count'],
         ['Victory', 'Minecraft accepts the death · cancelled deaths award nothing'], ['Credit', 'final hit + damage in the last 60 s, within 64 blocks']]
        .map(([t, s], i) => `<div class="frame" style="background:rgba(12,11,9,.9);padding:26px 34px;display:flex;align-items:center;gap:26px">
          <div class="display gold" style="font-size:52px;width:60px">${i + 1}</div>
          <div><div class="display" style="font-size:32px;color:var(--header)">${t}</div><div style="font-size:26px;color:var(--dim);margin-top:4px">${s}</div></div></div>`).join('')}</div>` }),

  'card-presentation': () => card({
    kicker: 'Boss presentation', title: 'Bars, banners<br>and music',
    points: ['A pixel-art bar for every boss', 'REMNANT FELLED for everyone within 64 blocks', 'Area titles as you cross into the Realm',
      'The boss fight theme within 64 blocks of a boss'],
    art: `<div style="position:relative;height:760px">${framed(region('boss-bars.jpg', 420, 0, 1080, 608, 978), 'position:absolute;left:0;top:0')}
      <div class="frame" style="position:absolute;left:-20px;bottom:0;width:620px">${region('remnant-felled.jpg', 380, 330, 1160, 330, 620)}</div></div>` }),

  'card-families': () => card({
    kicker: 'Creature families', title: 'The Realm’s<br>defenders',
    points: ['<b class="hi">Hollow Legion</b>: sentinels, arbalists, standard bearers and knights', '<b class="hi">Cryptborn</b>: catacomb rats, crypt lurkers and soldiers',
      '<b class="hi">Burial Guardian</b>: a miniboss with its own bar', '<b class="hi">Forgotten Souls</b>: pilgrims, memory wraiths and soul collectors'],
    art: `<div style="display:flex;flex-direction:column;gap:20px">
      ${framed(region('hollow-legion.jpg', 0, 160, 1920, 830, 978) + label('Hollow Legion', 'left:18px;bottom:18px'))}
      <div style="display:flex;gap:20px">
        ${framed(region('cryptborn.jpg', 0, 0, 1920, 1080, 478) + label('Cryptborn', 'left:14px;bottom:14px;font-size:17px'), 'flex:1')}
        ${framed(region('forgotten-souls.jpg', 0, 0, 1920, 1080, 478) + label('Forgotten Souls', 'left:14px;bottom:14px;font-size:17px'), 'flex:1')}</div></div>` }),

  'card-constructs': () => card({
    kicker: 'New in 3.1', title: 'Constructs<br>and relics',
    points: ['<b class="hi">Ashbound Crawler</b>: rears up and breathes flame', '<b class="hi">Ember Servitor</b>: burning bolts, and it welds constructs',
      '<b class="hi">Rusted Guardian</b>: a smash you can jump and a wide sweep', '<b class="hi">Shardlings</b>, <b class="hi">Ancient Vessels</b> and the hidden <b class="hi">Relic Stalker</b>',
      'They only hurt creatures and never touch the terrain'],
    art: pair('ashbound-constructs.jpg', 'corrupted-relics.jpg', 'Ashbound Constructs', 'Corrupted Relics') }),

  'card-gear': () => card({
    kicker: 'Spoils of the hunt', title: 'Boss gear',
    points: ['<b class="hi">Sovereign’s Ruin</b>: charge and release a spectral crescent', '<b class="hi">Warden’s Grasp</b>: a chain hook that pulls',
      '<b class="hi">Gravebound Executioner</b>: +40% against slowed or chained foes', '<b class="hi">Memory Staff</b>: a homing bolt, or a ring of twelve',
      'Three armor sets and three <span style="white-space:nowrap">off-hand relics</span>'],
    art: `<div style="position:relative">${framed(`<img src="${shot('gear.jpg')}">`)}
      ${[['Regalia', 'Hollow Sovereign', 25.5], ['Warden', 'Gravebound Warden', 52.6], ['Archivist', 'Forgotten Watcher', 76]]
        .map(([t, s, x]) => label(t, `left:${x}%;bottom:22px;transform:translateX(-50%);text-align:center`, s)).join('')}</div>` }),

  'card-config': () => card({
    kicker: 'Make it yours', title: 'Every number<br>is yours',
    points: ['Health, damage, phases and every attack', 'Summon items and all four offerings', 'Realm travel, spawns and build protection',
      'Missing keys are added, your values are kept'],
    art: `<div style="display:flex;flex-direction:column;gap:24px">${[
      ['config/remnant/realm/realm.json', [['kindle_ticks', '120.0'], ['crossing_cooldown_ticks', '100.0'], ['rituals_realm_only', '1.0'],
        ['guidance_radius_chunks', '48.0'], ['build_protection', '1.0']], true],
      ['config/remnant/bosses/gravebound_warden_summon.json', [['portal_activation_item', '"remnant_bosses:crypt_key"'], ['pedestal_one_activation_block', '"minecraft:bone_block"'],
        ['pedestal_two_activation_block', '"minecraft:soul_soil"'], ['pedestal_three_activation_block', '"minecraft:chain"'], ['pedestal_four_activation_block', '"minecraft:iron_bars"']], true]]
      .map(([file, rows, more]) => `<div class="frame" style="background:rgba(12,11,9,.92);padding:24px 36px;font-family:monospace;font-size:23px;line-height:1.5;color:var(--text)">
        <div style="color:var(--dim)">${file}</div>
        <pre style="margin:10px 0 0;font-family:inherit">{
${rows.map(([k, v], i) => `  <span style="color:var(--gold)">"${k}"</span>: <span style="color:var(--blue)">${v}</span>${i < rows.length - 1 || more ? ',' : ''}`).join('\n')}${more ? '\n  <span style="color:var(--dim)">…</span>' : ''}
}</pre></div>`).join('')}</div>` }),
};

const headers = { 'header-realm': 'The Eclipsed Realm', 'header-graces': 'Grace Embers', 'header-arenas': 'The Arenas',
  'header-bosses': 'The Bosses', 'header-altar': 'The Ancient Altar', 'header-hunts': 'Boss Hunts', 'header-presentation': 'Bars & Banners',
  'header-bestiary': 'The Bestiary', 'header-gear': 'Boss Gear', 'header-config': 'Configuration', 'header-everything': 'Everything Else',
  'header-compatibility': 'Downloads & Compatibility', 'header-faq': 'Questions', 'header-gallery': 'Gallery' };
for (const [name, title] of Object.entries(headers)) assets[name] = () => header(title);
assets.divider = () => page(1400, 40, `<div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;gap:18px">
  <div class="rule" style="width:520px;transform:scaleX(-1)"></div><span class="diamond" style="margin:0"></span><div class="rule" style="width:520px"></div></div>`, true);

(async () => {
  const wanted = process.argv.slice(2);
  const browser = await chromium.launch();
  fs.mkdirSync(out, { recursive: true });
  for (const [name, build] of Object.entries(assets)) {
    if (wanted.length && !wanted.includes(name)) continue;
    const html = build();
    const size = html.match(/body \{ width: (\d+)px; height: (\d+)px; \}/);
    const tab = await browser.newPage({ viewport: { width: +size[1], height: +size[2] }, deviceScaleFactor: 1 });
    const file = path.join(__dirname, `.render-${name}.html`);
    fs.writeFileSync(file, html);
    await tab.goto(url(file));
    await tab.evaluate(() => document.fonts.ready);
    await tab.waitForLoadState('networkidle');
    await tab.screenshot({ path: path.join(out, `${name}.png`), omitBackground: name === 'divider' || name.startsWith('header-') });
    fs.unlinkSync(file);
    await tab.close();
    console.log('rendered', name);
  }
  await browser.close();
})().catch((error) => { console.error(error); process.exit(1); });
