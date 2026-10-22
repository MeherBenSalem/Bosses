// Renders the listing artwork in marketing/images/ from real in-game screenshots and the bundled OFL fonts.
// Usage (from the repository root): NODE_PATH="$(npm root -g)" node marketing/source/render.cjs [name...]
// Requires the `playwright` package with a Chromium build available.
const path = require('path');
const fs = require('fs');
const { chromium } = require('playwright');

const root = path.resolve(__dirname, '..', '..');
const out = path.join(root, 'marketing', 'images');
const shots = path.join(out, 'screenshots');
const fonts = path.join(__dirname, 'fonts');
const url = (file) => 'file://' + file;
const shot = (name) => url(path.join(shots, name));
const source = (name) => url(path.join(__dirname, name));

// Boss accents match the in-game custom boss bars (RemnantBossBars.java).
const accent = { ossukage: '#e55169', kotsukage: '#e6bb74', umbrakar: '#bd66ff', sovereign: '#70e6b0' };

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
.frame { border: 1px solid rgba(216,207,184,.45); box-shadow: 0 0 0 1px rgba(0,0,0,.6), 0 30px 80px rgba(0,0,0,.65), 0 0 60px rgba(212,176,106,.16); }
.frame img { display: block; width: 100%; }
canvas.motes { position: absolute; left: 0; top: 0; width: 100%; height: 100%; pointer-events: none; }
.vignette { position: absolute; inset: 0; background: radial-gradient(ellipse at center, rgba(0,0,0,0) 45%, rgba(0,0,0,.75) 100%); }
.backdrop { position: absolute; inset: -40px; background-size: cover; background-position: center; filter: blur(16px) brightness(.42) saturate(1.1); }
.pill { display: inline-block; border: 1px solid rgba(216,207,184,.55); padding: 6px 14px 4px; margin-right: 10px;
        font-family: RbDisplay, serif; font-size: 18px; letter-spacing: .12em; color: var(--header); background: rgba(0,0,0,.35); }
.label { position: absolute; font-family: RbDisplay, serif; font-size: 20px; letter-spacing: .14em; color: var(--header);
         background: rgba(10,9,8,.86); border: 1px solid rgba(212,176,106,.8); padding: 7px 14px 5px; white-space: nowrap;
         box-shadow: 0 8px 26px rgba(0,0,0,.6); }
.label small { display: block; font-family: RbSerif, serif; letter-spacing: 0; font-size: 18px; color: var(--dim); margin-top: 1px; }
ul.points { list-style: none; padding: 0; margin: 0; }
ul.points li { position: relative; padding-left: 26px; margin: 0 0 14px; font-size: 27px; line-height: 1.32; color: var(--text); }
ul.points li::before { content: ''; position: absolute; left: 2px; top: 13px; width: 8px; height: 8px; background: var(--gold); transform: rotate(45deg); }
.k { display: inline-block; border: 1px solid rgba(216,207,184,.6); padding: 0 8px; margin: 0 2px; background: rgba(0,0,0,.35); font-size: .9em; }
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

// A feature card: copy on the left, artwork on the right.
const card = ({ kicker, title, points, art, width = 1600, height = 900, artWidth = 980, backdrop = 'backdrop.jpg' }) => page(width, height, `
  <div class="backdrop" style="background-image:url('${source(backdrop)}');opacity:.55"></div>
  <div class="vignette"></div>${motes}
  <div style="position:absolute;left:80px;top:0;bottom:0;width:${width - artWidth - 150}px;display:flex;flex-direction:column;justify-content:center">
    <div class="kicker"><span class="diamond"></span>${kicker}</div>
    <div class="display gold glow" style="font-size:58px;line-height:1.08;margin:18px 0 22px">${title}</div>
    <div class="rule" style="width:80%;margin-bottom:30px"></div>
    <ul class="points">${points.map((point) => `<li>${point.replace(/ (\S+)$/, '&nbsp;$1')}</li>`).join('')}</ul>
  </div>
  <div style="position:absolute;right:60px;top:50%;transform:translateY(-50%);width:${artWidth}px">${art}</div>`);

// Boss card art: a framed 16:9 capture (optionally zoomed on a focal point) with a stat strip in the boss bar's colour.
// Values are the defaults the mod writes to config/remnant/bosses/*.json.
const zoom = (file, w = 100, x = 50, y = 50) => `<div style="position:relative;aspect-ratio:16/9;overflow:hidden">
    <img src="${shot(file)}" style="position:absolute;width:${w}%;max-width:none;left:${-(w - 100) * x / 100}%;top:${-(w - 100) * y / 100}%"></div>`;
const bossArt = (file, color, stats, [w, x, y] = [100, 50, 50]) => `<div class="frame" style="border-color:${color}99">${zoom(file, w, x, y)}</div>
  <div class="stats" style="border-top:2px solid ${color}">${stats.map(([t, v]) => `<div${t === 'Summon' ? ' style="flex:1.9"' : ''}><div class="t">${t}</div><div class="v">${v}</div></div>`).join('')}</div>`;

const assets = {
  banner: () => page(1800, 600, `
    <img src="${shot('hollow-sovereign.jpg')}" style="position:absolute;right:-40px;top:-150px;width:1440px;filter:saturate(1.05);-webkit-mask-image:linear-gradient(90deg,transparent 0%,#000 38%);mask-image:linear-gradient(90deg,transparent 0%,#000 38%)">
    <div style="position:absolute;inset:0;background:linear-gradient(90deg,rgba(8,7,6,.96) 0%,rgba(8,7,6,.9) 30%,rgba(8,7,6,.45) 55%,rgba(8,7,6,0) 75%)"></div>
    <div style="position:absolute;inset:0;background:linear-gradient(0deg,rgba(8,7,6,.7) 0%,rgba(8,7,6,0) 30%)"></div>
    <div class="vignette"></div>${motes}
    <div style="position:absolute;left:90px;top:92px;width:900px">
      <div class="kicker"><span class="diamond"></span>2.7 · Boss Hunts</div>
      <div class="display gold glow" style="font-size:84px;line-height:1.04;margin:22px 0 26px;white-space:nowrap">Remnant<br>Bosses</div>
      <div class="rule" style="width:600px;margin-bottom:26px"></div>
      <div style="font-size:34px;color:var(--header);line-height:1.3">Raise the rite. Face the remnant.<br>Record the hunt.</div>
      <div style="margin-top:36px"><span class="pill">Fabric</span><span class="pill">Forge</span><span class="pill">NeoForge</span>
        <span style="font-size:24px;color:var(--dim);margin-left:8px">1.20.1 · 1.21.1</span></div>
    </div>`),

  icon: () => page(512, 512, `
    <div style="position:absolute;inset:0;background:radial-gradient(circle at 50% 45%,#2a241c 0%,#120f0c 55%,#070605 100%)"></div>${motes}
    <div style="position:absolute;left:56px;top:56px;width:400px;height:400px;border-radius:50%;border:3px solid var(--gold);box-shadow:0 0 40px rgba(212,176,106,.45), inset 0 0 40px rgba(212,176,106,.25)"></div>
    <div style="position:absolute;left:78px;top:78px;width:356px;height:356px;border-radius:50%;border:1px solid rgba(212,176,106,.55)"></div>
    ${[[256, 56], [456, 256], [256, 456], [56, 256]].map(([x, y]) => `<div style="position:absolute;left:${x - 12}px;top:${y - 12}px;width:24px;height:24px;background:var(--gold);transform:rotate(45deg);box-shadow:0 0 16px rgba(240,213,140,.8)"></div>`).join('')}
    <div style="position:absolute;left:116px;top:96px;width:280px;height:280px;background:radial-gradient(circle,rgba(240,213,140,.30),rgba(240,213,140,0) 65%)"></div>
    <img src="${source('skull.png')}" style="position:absolute;left:136px;top:104px;width:240px;height:240px;image-rendering:pixelated;filter:drop-shadow(0 10px 24px rgba(0,0,0,.7))">
    <div class="display gold" style="position:absolute;left:0;right:0;top:356px;text-align:center;font-size:40px;letter-spacing:.16em">REMNANT</div>`),

  'card-ossukage': () => card({
    kicker: 'The Oni Shogun', title: 'Ossukage',
    points: ['Rises from a rune circle, untouchable until the rite ends', 'A three-strike sword chain up close, kunai at range',
      'At half health: five skeletons, Absorption, more speed and damage', 'Phase two dashes in, with an explosion, Darkness or more skeletons',
      'Drops the <b class="hi">Ossukage Blade</b>'],
    art: bossArt('ossukage.jpg', accent.ossukage, [['Health', '800'], ['Phase two', '50%'], ['Summon', 'Nether Star']]) }),

  'card-kotsukage': () => card({
    kicker: 'The Bone Sovereign', title: 'Kotsukage',
    points: ['Left and right claw swipes up close', 'Stomps that hit everything within 5&nbsp;blocks', 'Poison breath across a cone in front of it',
      'A roar that raises bone traps and calls minions', 'Below 45%: faster, harder, one more trap and minion per roar'],
    art: bossArt('kotsukage.jpg', accent.kotsukage, [['Health', '700'], ['Phase two', '45%'], ['Summon', 'Wither Skeleton Skull']], [135, 50, 30]) }),

  'card-umbrakar': () => card({
    kicker: 'The Riftmaw Colossus', title: 'Umbrakar',
    points: ['Bite, front slam, and a tail slam for anyone behind it', 'Rift Claw: five eruptions marching up to 15 blocks',
      'A roar shockwave across a 16-block arena', 'A homing orb cast from its tail', 'Windup markers lock in place, so stepping out works'],
    art: bossArt('umbrakar.jpg', accent.umbrakar, [['Health', '750'], ['Phase two', '40%'], ['Summon', 'Echo Shard']]) }),

  'card-sovereign': () => card({
    kicker: 'The Bone Monarch', title: 'Hollow<br>Sovereign',
    points: ['Awakens in four soul halos and a six&#8209;block seal', 'Twin Reaping claws and Grave Tide shockwaves',
      'Hollow Judgment: a core beam that walls can stop', 'Crown&rsquo;s Edict marks your spot, then erupts', 'At 45%: +30% damage, +20% speed, wider eruptions'],
    art: bossArt('hollow-sovereign.jpg', accent.sovereign, [['Health', '950'], ['Phase two', '45%'], ['Summon', 'Heart of the Sea']], [125, 50, 40]) }),

  'card-altar': () => card({
    kicker: 'Summoning', title: 'Raise the rite',
    points: ['One Ancient Altar, four Ancient Pedestals', 'Pedestals 3 blocks out: east, west, south and north', 'An offering on top of each pedestal',
      'Right-click the altar with the activation item', 'Every item and offering is configurable'],
    art: `<div style="position:relative"><div class="frame"><img src="${shot('altar.jpg')}"></div>
      ${[['West', 'Crying Obsidian', 40, 25, 'translate(-50%,-100%)'], ['North', 'Sculk', 62.5, 24, 'translate(-50%,-100%)'],
         ['South', 'End Stone', 24, 58, 'translate(-100%,-50%)'], ['East', 'Amethyst Block', 75, 84, 'translate(-50%,0)'],
         ['Altar', 'Echo Shard in hand', 52, 66, 'translate(-50%,0)']]
        .map(([t, s, x, y, tr]) => `<div class="label" style="left:${x}%;top:${y}%;transform:${tr}">${t}<small>${s}</small></div>`).join('')}
      <div class="label" style="left:24px;top:24px;font-size:18px">Umbrakar&rsquo;s rite · default offerings</div></div>` }),

  'card-journal': () => card({
    kicker: 'Boss Hunts', title: 'A journal of<br>your hunts',
    points: ['Craft it from a Book, a Bone and an Ink Sac', 'Undiscovered hunts whisper a ritual clue', 'Witness a boss to reveal its real rite',
      'Defeat it and the hunt is recorded', 'Progress stays with you, not the book'],
    art: `<div style="position:relative;height:720px">
      <div class="frame" style="position:absolute;left:0;top:0;width:70%"><img src="${shot('journal-undiscovered.png')}" style="image-rendering:pixelated"></div>
      <div class="frame" style="position:absolute;right:0;bottom:0;width:70%"><img src="${shot('journal-encountered.png')}" style="image-rendering:pixelated"></div>
      <div class="label" style="left:68%;top:0;transform:translate(-100%,-50%)">Undiscovered</div>
      <div class="label" style="right:2%;top:206px;transform:translateY(-50%)">Encountered</div></div>` }),

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

  'card-bars': () => card({
    kicker: 'Boss presentation', title: 'Bars worth<br>fighting under',
    points: ['A pixel-art bar for each boss', 'Other bosses keep the vanilla bar', 'A fight theme for Ossukage and Kotsukage',
      'Particle density from 0 to 1 on each client'],
    art: `<div style="position:relative;height:790px">
      <div class="frame" style="position:absolute;left:0;top:0;width:100%">${zoom('boss-bars.jpg', 118, 15, 0)}</div>
      <div class="frame" style="position:absolute;left:0;bottom:0;width:540px;height:356px;overflow:hidden;background:#000">
        <img src="${shot('boss-bars.jpg')}" style="position:absolute;width:1571px;max-width:none;left:-515px;top:0;image-rendering:pixelated"></div></div>` }),

  'card-bestiary': () => card({
    kicker: 'The bestiary', title: 'Remnants<br>in the dark',
    points: ['<b class="hi">Grave Skitter</b>: a six-legged crawler that pounces', '<b class="hi">Sickleghast</b>: paired scythes and a wide cleave',
      '<b class="hi">Dirge Lantern</b>: marks the ground, then bursts', 'Skeleton warriors, archers and minions', 'Rats, Wraiths and Armored Grubs'],
    art: `<div style="display:flex;flex-direction:column;gap:20px">
      <div class="frame" style="position:relative;height:430px;overflow:hidden">
        <img src="${shot('bestiary-trio.jpg')}" style="position:absolute;width:100%;top:-12%">
        <div class="label" style="left:18px;bottom:18px">Grave Skitter · Sickleghast · Dirge Lantern</div></div>
      <div style="display:flex;gap:20px">
        <div class="frame" style="position:relative;flex:1;height:270px;overflow:hidden"><img src="${shot('bestiary-skeletons.jpg')}" style="position:absolute;width:118%;left:-9%;top:-12%">
          <div class="label" style="left:14px;bottom:14px;font-size:18px">Skeleton Warrior · Archer</div></div>
        <div class="frame" style="position:relative;flex:1;height:270px;overflow:hidden"><img src="${shot('bestiary-creatures.jpg')}" style="position:absolute;width:110%;left:-5%;top:-15%">
          <div class="label" style="left:14px;bottom:14px;font-size:18px">Wraith · Rat · Armored Grub</div></div></div></div>` }),

  'card-config': () => card({
    kicker: 'Make it yours', title: 'Every number<br>is yours',
    points: ['Health, damage, phases and every attack', 'Activation items and all four offerings', 'Monsters, minions and the Ossukage Blade',
      'Missing keys are added, your values are kept'],
    art: `<div style="display:flex;flex-direction:column;gap:24px">${[
      ['config/remnant/bosses/umbrakar_summon.json', [['portal_activation_item', '"minecraft:echo_shard"'], ['pedestal_one_activation_block', '"minecraft:amethyst_block"'],
        ['pedestal_two_activation_block', '"minecraft:crying_obsidian"'], ['pedestal_three_activation_block', '"minecraft:end_stone"'], ['pedestal_four_activation_block', '"minecraft:sculk"']], false],
      ['config/remnant/bosses/hollow_sovereign.json', [['max_health', '950.0'], ['phase_two_percent', '45.0'], ['phase_two_damage_multiplier', '1.3'],
        ['beam_damage', '24.0'], ['wave_radius', '12.0']], true]]
      .map(([file, rows, more]) => `<div class="frame" style="background:rgba(12,11,9,.92);padding:26px 38px;font-family:monospace;font-size:24px;line-height:1.5;color:var(--text)">
        <div style="color:var(--dim)">${file}</div>
        <pre style="margin:10px 0 0;font-family:inherit">{
${rows.map(([k, v], i) => `  <span style="color:var(--gold)">"${k}"</span>: <span style="color:var(--blue)">${v}</span>${i < rows.length - 1 || more ? ',' : ''}`).join('\n')}${more ? '\n  <span style="color:var(--dim)">…</span>' : ''}
}</pre></div>`).join('')}</div>` }),
};

const headers = { 'header-bosses': 'The Four Bosses', 'header-altar': 'The Ancient Altar', 'header-hunts': 'Boss Hunts',
  'header-presentation': 'Bars & Music', 'header-bestiary': 'The Bestiary', 'header-config': 'Configuration',
  'header-everything': 'Everything Else', 'header-compatibility': 'Downloads & Compatibility', 'header-faq': 'Questions',
  'header-gallery': 'Gallery' };
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
