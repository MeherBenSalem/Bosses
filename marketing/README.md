# Marketing assets: 3.1 "The Realm Endures"

Listing copy and artwork for CurseForge and Modrinth. Nothing here ships in the mod JARs.

## Listing

| File | Use |
| --- | --- |
| [`curseforge/description.md`](curseforge/description.md) | Full project description (Markdown; also works on Modrinth) |
| [`curseforge/summary.txt`](curseforge/summary.txt) | Short project summary (231 characters) |
| [`../patchnotes/3.1.0.md`](../patchnotes/3.1.0.md) | File changelog for 3.1.0 |

Image links in the description point at `raw.githubusercontent.com/MeherBenSalem/Bosses/main/marketing/images/...`, so they work once this folder is on `main`. To host on CurseForge instead, upload the images to the project gallery and replace each URL.

The description leads with the Eclipsed Realm, then Grace Embers, the arenas, the bosses, the Ancient Altar, Boss Hunts, bars and banners, the bestiary, boss gear, configuration, everything else, downloads and compatibility, the FAQ and a gallery of 3.1.0 screenshots.

## Adding a Minecraft version

The supported versions are listed in exactly two places:

1. `VERSIONS` at the top of [`source/render.cjs`](source/render.cjs). Add one line, for example `{ minecraft: '26.2', loaders: ['Fabric', 'NeoForge'] },`. The banner's loader pills and version line are built from it. Then rerun the script for `banner`.
2. The table under **Downloads & Compatibility** in `curseforge/description.md`. Add one row.

No other copy or artwork names a Minecraft version. A new release name or number goes in `RELEASE` in `render.cjs` and in the first lines of the description.

## Images

| File | Size | Use |
| --- | --- | --- |
| `images/banner.png` | 1800×600, 1021 KB | Description header: the Gracetree over the Hollow Citadel, "3.1 · The Realm Endures", loader pills and versions |
| `images/icon.png` | 512×512, 162 KB | Project avatar: the 3.x mod logo (crowned skull under the eclipse) in a gold ring |
| `images/card-realm.png` | 1600×900, 1229 KB | The Eclipsed Realm with an eclipse inset; 7 biomes, eternal dusk, 3 arenas |
| `images/card-waygate.png` | 1600×900, 1232 KB | Four steps into the Realm; an overworld Waygate and an Arrival Shrine |
| `images/card-graces.png` | 1600×900, 991 KB | A grace beam with the Gracetree behind, plus the grace menu |
| `images/card-arenas.png` | 1600×900, 1317 KB | Hollow Citadel, Forgotten Observatory and the Catacombs' chain chapel |
| `images/card-sovereign.png` | 1600×900, 1298 KB | Hollow Sovereign in the throne hall; 720, 66% · 33%, Sovereign's Seal |
| `images/card-warden.png` | 1600×900, 1326 KB | Gravebound Warden in the chain chapel; 520, 50%, Crypt Key |
| `images/card-watcher.png` | 1600×900, 1340 KB | Forgotten Watcher under the dome; 680, 50%, Lens of Memory |
| `images/card-ossukage.png` | 1600×900, 988 KB | Ossukage at a grace shrine; 800, 50%, Nether Star |
| `images/card-altar.png` | 1600×900, 1028 KB | The Observatory altar with each pedestal's default offering labelled |
| `images/card-journal.png` | 1600×900, 685 KB | Boss Hunts journal, Defeated and Undiscovered entries |
| `images/card-credit.png` | 1600×900, 606 KB | Multiplayer defeat credit: Witness, Fight, Victory, Credit (48 / 64 blocks, 60 s) |
| `images/card-presentation.png` | 1600×900, 1372 KB | All four boss bars in game, plus the REMNANT FELLED banner |
| `images/card-families.png` | 1600×900, 1401 KB | Hollow Legion, Cryptborn and Forgotten Souls |
| `images/card-constructs.png` | 1600×900, 1266 KB | The new Ashbound Constructs and Corrupted Relics |
| `images/card-gear.png` | 1600×900, 1215 KB | Regalia, Warden and Archivist sets on armor stands, with weapons and relics |
| `images/card-config.png` | 1600×900, 585 KB | Generated `realm/realm.json` and `bosses/gravebound_warden_summon.json` |
| `images/header-*.png` | 1400×120, transparent, 84–108 KB | 14 section headings for the description |
| `images/divider.png` | 1400×40, transparent | Section divider |
| `images/gifs/waygate-kindle.gif` | 640×360, 5 s, 1871 KB | An overworld Waygate kindling and opening |
| `images/gifs/realm-crossing.gif` | 640×360, 8 s, 1297 KB | Walking through the Waygate, the eclipse closing, and the arrival title |
| `images/screenshots/*.jpg` | 1920×1080, 114–759 KB | 33 captures: gallery uploads and card art |

### How the screenshots were captured

Every screenshot and both GIFs are unedited captures of the **1.21.1 Fabric development client** (`./gradlew :fabric:runClient`, Remnant Bosses 3.1.0, default configuration) with software rendering, GUI scale 3 and the HUD hidden (F1). They were taken in a fresh world, "Realm Tour", in the Realm's real generated terrain and structures. The only changes for the shots: the overworld Waygate was placed and the trees around it cleared, the chain chapel's altar and pedestals were cleared for the Cryptborn line-up, and the gear was put on armor stands in the throne hall.

- **Lighting:** the Catacombs' chain chapel, the Observatory dome and the Citadel's throne hall are lit only by soul fire and lanterns. They were captured with Night Vision, so they look brighter than in normal play.
- **Bosses** were raised with their altar rituals or spawn eggs. The creature group shots and the boss-bar line-up used `/summon`.
- **Posing:** frozen and stepped with `/tick freeze` and `/tick step`. Action shots were taken while a boss was really attacking a player in Survival with Resistance, then framed from a spectator camera.
- **HUD:** only `boss-bars.jpg` and `remnant-felled.jpg` have the HUD on. `remnant-felled.jpg` shows command output in the chat at the bottom left. The card crops it out, and the gallery leaves that shot out.
- **Journal:** captured with Minecraft's *Menu Background Blur* set to 0. At the default blur the 1.21.1 journal's title, detail page and footer render blurred. The Defeated entry is a real defeat: the Warden was killed with player-attributed damage. The Hollow Sovereign was witnessed to show Encountered.
- **GIFs:** recorded at the normal tick rate and played back at 1.5× speed. `realm-crossing.gif` cuts the loading screen between the overworld and the Realm. Other action shots were recorded at `/tick rate 4`, and single frames were picked from them.

## Regenerating the artwork

The cards, banner, icon and headers are HTML rendered with Playwright from the screenshots above and the bundled fonts:

```bash
NODE_PATH="$(npm root -g)" node marketing/source/render.cjs            # everything
NODE_PATH="$(npm root -g)" node marketing/source/render.cjs banner icon  # selected assets
```

- `source/backdrop.jpg` is the blurred background scene, a 1280×720 copy of `screenshots/realm-gracetree.jpg`.
- `source/logo.png` is the mod logo (`common/src/main/resources/logo.png`).
- `source/fonts/` holds Cinzel and EB Garamond with their SIL Open Font License 1.1 texts. The script uses only these files, so it does not depend on any other repository.

To refresh screenshots, launch a client (see the root `README.md`), replace the files in `images/screenshots/` with the same names, and rerun the script. Card crops are pixel regions of the 1920×1080 captures (`region()` and the `bossArt` crop boxes in `render.cjs`), so new captures may need their crops and label positions adjusted.

## Wording

Describe the fights as soulslike. Do not use other games' names, logos or art in listing images or titles, and call the rest points **Grace Embers** or graces. Every number in the copy and on the cards is a 3.1.0 default from the generated JAuml configuration (`config/remnant/`), a recipe or loot table, or the code. Update the copy when those defaults change.
