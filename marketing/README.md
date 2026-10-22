# Marketing assets: 2.7 "Boss Hunts"

Listing copy and artwork for CurseForge and Modrinth. Nothing here ships in the mod JARs.

## Listing

| File | Use |
| --- | --- |
| [`curseforge/description.md`](curseforge/description.md) | Full project description (Markdown; also works on Modrinth) |
| [`curseforge/summary.txt`](curseforge/summary.txt) | Short project summary (216 characters) |
| [`../patchnotes/2.7.0.md`](../patchnotes/2.7.0.md) | File changelog for 2.7.0 |

Image links in the description point at `raw.githubusercontent.com/MeherBenSalem/Bosses/main/marketing/images/...`, so they work once this folder is on `main`. To host on CurseForge instead, upload the images to the project gallery and replace each URL.

## Images

| File | Size | Use |
| --- | --- | --- |
| `images/banner.png` | 1800×600 | Description header (Hollow Sovereign, Unbound Crown) |
| `images/icon.png` | 512×512 | Project avatar (the mod's skull logo) |
| `images/card-ossukage.png` | 1600×900 | Ossukage moveset, default health, phase and summon item |
| `images/card-kotsukage.png` | 1600×900 | Kotsukage moveset, bone traps and minions |
| `images/card-umbrakar.png` | 1600×900 | Umbrakar mid Rift Claw |
| `images/card-sovereign.png` | 1600×900 | Hollow Sovereign halos and seal |
| `images/card-altar.png` | 1600×900 | Altar layout with labelled offerings (Umbrakar's rite) |
| `images/card-journal.png` | 1600×900 | Boss Hunts journal, Undiscovered and Encountered |
| `images/card-credit.png` | 1600×900 | Multiplayer defeat credit, step by step |
| `images/card-bars.png` | 1600×900 | Custom boss bars in game, with a magnified inset |
| `images/card-bestiary.png` | 1600×900 | Bone trio, skeletons and smaller creatures |
| `images/card-config.png` | 1600×900 | Generated `umbrakar_summon.json` and `hollow_sovereign.json` |
| `images/header-*.png`, `images/divider.png` | 1400 wide, transparent | Section headings for the description |
| `images/screenshots/*.jpg` | 1920×1080 | Gallery uploads and card art |
| `images/screenshots/journal-*.png` | 1280×960 | Journal captures (see below) |
| `images/gifs/ritual.gif` | 720×405, 5.5 s | Ossukage ritual: lightning on the offerings, then Ossukage rising |
| `images/gifs/sovereign-awakening.gif` | 720×405, 3.9 s | Hollow Sovereign ritual and awakening |

### How the screenshots were captured

All 1920×1080 screenshots and both GIFs are unedited captures of the 1.21.1 Fabric development client (`./gradlew :fabric:runClient`, Remnant Bosses 2.7.0, default configuration) with software rendering, GUI scale 3, FOV 60 and the HUD hidden (F1). The scene is a creative world: a deepslate arena built in a cherry grove at dusk (`/time set 12400`). Bosses were summoned with the Ancient Altar, spawn eggs or `/summon`, and posed with `/tick freeze` and `/tick step`. Action shots were taken while a boss was really attacking a player, then viewed from a spectator camera.

- `boss-bars.jpg` is the only shot with the HUD on, taken in spectator mode so the hotbar and hand are hidden.
- `ritual-lightning.jpg` is a frame from the Umbrakar ritual recording.
- `hollow-sovereign.jpg` shows the Unbound Crown phase change, which uses the same halos and seal as the awakening.
- The GIFs were recorded at `/tick rate 4` and played back 5× faster, so they run at real game speed. `ritual.gif` cuts most of the four-second wait between the offering strikes and the boss's arrival.
- `journal-*.png` are the 1.20.1 Fabric captures from [`docs/boss-hunts/screenshots/`](../docs/boss-hunts/screenshots/). They are used because, on the 1.21.1 build, the menu blur draws over the journal's title and detail text.

## Regenerating the artwork

The cards, banner, icon and headers are HTML rendered with Playwright from the screenshots above and the bundled fonts:

```bash
NODE_PATH="$(npm root -g)" node marketing/source/render.cjs            # everything
NODE_PATH="$(npm root -g)" node marketing/source/render.cjs banner icon  # selected assets
```

- `source/backdrop.jpg` is the blurred background scene, a downscaled copy of `screenshots/bosses.jpg`.
- `source/skull.png` is the mod logo (`common/src/main/resources/logo.png`).
- `source/fonts/` holds Cinzel and EB Garamond with their SIL Open Font License 1.1 texts. The script uses only these files, so it does not depend on any other repository.

To refresh screenshots, launch a client (see the root `README.md`), replace the files in `images/screenshots/` with the same names, and rerun the script. Card crops and label positions are percentages, so new captures may need their focal points adjusted in `render.cjs`.

## Wording

Describe the fights as soulslike. Do not use other games' names, logos or art in listing images or titles. Every number in the copy and on the cards is a default from the generated JAuml configuration (`config/remnant/`) or the docs. Update the copy when those defaults change.
