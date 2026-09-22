# Remnant Codex

A static, interactive documentation site for Remnant Bosses, built with Vite and Three.js. Intended address: **https://docs.nightbeam.dev/remnants/**.

## Included

- Four boss dossiers and regular creature guides: 13 playable catalog entries total.
- Thirteen textured GLB model studies with animation selection, pause/play, speed, timeline scrubbing, wireframe, and orbit controls.
- Search across creatures, guides, items, and configuration files.
- Textured Three.js ritual layouts and default offering recipes for all four bosses.
- Seven items/blocks, five source-derived recipes, 21 configuration files with downloads, and a registry index.
- A live Three.js bestiary grid rendered with one shared WebGL context and lazy-loaded, animated models.
- Responsive mobile navigation, keyboard search, reduced-motion support, WebGL fallback messages, and self-hosted fonts.

## Run and update

```powershell
cd website
npm ci
python scripts/export_content.py
npm run dev
```

Open `http://127.0.0.1:5173/remnants/`. To regenerate model thumbnails while the dev server is running:

```powershell
node scripts/capture-thumbnails.mjs
npm run build
npm run preview -- --host 127.0.0.1 --port 4173
```

Tests against the production preview:

```powershell
$env:CODEX_SITE_URL='http://127.0.0.1:4173/remnants/'
npm test
```

The ten browser tests cover the viewer controls, creature filters, native-model conversion, ritual offerings, search, config downloads, mobile layout/navigation, reference pages, and reduced motion. All ten pass against the production build. The fourteen model studies and desktop/mobile screenshots were inspected. Exported animations are an inspection approximation; numeric Bedrock channels are supported, while runtime Molang expressions and Minecraft particles are not reproduced.

## Deployment

The built `dist/` folder is deployed to `/var/www/remnant-codex/releases/<timestamp>/`; an atomic `current` symlink selects the active release. Nginx uses a dedicated `docs.nightbeam.dev` vhost. Other sites are not modified. The deployment script checks Nginx syntax before reload and keeps a backup of its existing vhost. Credentials stay in the external connection note and are never copied into the website.

```powershell
python scripts/deploy.py --connection-file "PATH_TO_YOUR_SSH_NOTE"
```

**Live:** https://docs.nightbeam.dev/remnants/ was deployed and verified on 2026-09-22. Cloudflare proxies the `docs` hostname to the VPS. The origin has its own Let's Encrypt certificate, initially valid until 2026-12-21. The Certbot timer is active, and a domain-specific deploy hook reloads Nginx after renewal. All ten browser tests also pass against the public HTTPS URL.

To provision or check the certificate on a future deployment:

```powershell
python scripts/deploy.py --connection-file "PATH_TO_YOUR_SSH_NOTE" --https
```

This uses the VPS's existing Certbot account to issue a certificate, then enables HTTPS and an HTTP redirect. Future ordinary deployments preserve HTTPS. Verify the public page and rerun the browser tests with `CODEX_SITE_URL=https://docs.nightbeam.dev/remnants/`.

For rollback, select a verified previous release directory under `/var/www/remnant-codex/releases/` and atomically replace `current` with a symlink to that directory. Do not delete the existing release until its replacement is verified.

## Content provenance

`scripts/export_content.py` reads only the mod's public entity geometry, animations, textures, registry files, recipe files, translations, and config defaults. It embeds unchanged texture PNGs in GLB files and preserves the source bone hierarchy. The site does not connect to or control a Minecraft server. Config downloads show source defaults, not a live server's settings. Refresh the export and rebuild whenever the mod changes.

## Texture loading

GLB textures are embedded PNGs decoded via blob URLs. The Nginx Content Security Policy permits `blob:` in `connect-src` as well as `img-src`. Both viewers verify decoded texture dimensions before marking a model ready. The live gallery regression test checks all 14 entries and fails on GLTF texture or blob-policy errors. Rat, Wraith, and Skeleton Minion are converted from the existing Java model and animation definitions. The monster-creation page and its navigation/search entries have been removed.
