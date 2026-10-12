Final verification outcomes are consolidated in [the test report](boss-hunts-test-report.md). The implementation-stage evidence below is retained as a record.

# Boss Hunts client journal verification

Scope: Minecraft 1.20.1 (Fabric/Forge, Java 17) and 1.21.1 (Fabric/NeoForge, Java 21). Common client journal source, English language additions, and standalone layout regressions were implemented in both version workspaces.

## Implemented behavior

- Four translated hunt entries, selected/focused borders, state badges, existing registered spawn-egg icons, and a restrained dark parchment/purple/gold page.
- Split navigation/details at normal sizes; compact four-entry navigation on narrow or short screens. Headings and entry labels are elided with full-name tooltips; detail text wraps in a clipped viewport.
- Compact buttons use short translated boss names and omit icons below 100 GUI pixels per entry, keeping full registered names in headings, tooltips and narration. Normal split-page list icons are preserved.
- Undiscovered hunts show translated clues only. Encountered hunts show the server's effective activation item and registered block names/icons, ordered east/west/south/north, with three-block center spacing and offerings one block above pedestals. Defeated hunts show completion. Invalid or missing registry values show translated ritual unavailability, never a default air item.
- Mouse-wheel and button scrolling, Page Up/Down and Home/End, arrow entry selection, Tab focus, Escape/back, selected-state and detail narration.
- Repeated snapshots update the current screen while preserving selection and clamping scroll. Client tick closes the screen on world/connection change or missing active player. Loader transports must guard queued callbacks with the original connection and world identities.

## Automated checks executed

Initial missing-feature check (expected failure: missing `JournalLayout`):

```powershell
javac -d "$env:TEMP\boss-journal-layout-red" 1.20.1/common/src/test/java/com/nightbeam/remnants/JournalLayoutRegression.java
```

An additional boundary regression at 440×252 failed with `entry covers footer controls` before adaptive split-row heights were implemented, then passed after the fix.

Passing standalone checks:

```powershell
javac --release 17 -d "$env:TEMP\boss-journal-layout-green" 1.20.1/common/src/main/java/com/nightbeam/remnants/client/journal/JournalLayout.java 1.20.1/common/src/test/java/com/nightbeam/remnants/JournalLayoutRegression.java
java -cp "$env:TEMP\boss-journal-layout-green" com.nightbeam.remnants.JournalLayoutRegression
javac --release 21 -d "$env:TEMP\boss-journal-layout-121" 1.21.1/common/src/main/java/com/nightbeam/remnants/client/journal/JournalLayout.java 1.21.1/common/src/test/java/com/nightbeam/remnants/JournalLayoutRegression.java
java -cp "$env:TEMP\boss-journal-layout-121" com.nightbeam.remnants.JournalLayoutRegression
```

Both print `Journal layout regression passed`. Fixtures cover 160×100, 240×135, 320×180, 440×252, 640×360 and 1920×1080; entries/details/footer remain on screen and do not overlap. Scroll checks cover negative input, final-content boundary, short content and viewport resizing. The core regression owner integrates `JournalLayoutRegression.run()` into `verifyJournal`.

Both edited language JSON files parsed successfully through PowerShell `ConvertFrom-Json`; `git diff --check` passed (repository line-ending conversion warnings only).

## Checks pending central integration or runtime

No parallel Gradle builds were started by the UI worker. Full Minecraft compilation and `verifyJournal` are run centrally after all workers' shared interfaces are present.

The UI worker inspected actual Minecraft 1.20.1 Fabric captures `boss-hunts-encountered-scale2-ossukage-top.png`, `boss-hunts-undiscovered-scale3-hollow_sovereign-top.png` and `boss-hunts-encountered-scale4-hollow_sovereign-bottom.png` under `1.20.1/fabric/run/screenshots`. The normal split page rendered cleanly; compact names were overly abbreviated at high GUI scale. The short-name/icon refinement above addresses that finding without changing layout geometry. The central owner will rebuild/relaunch for new captures to confirm the refinement.

Focus narration listening, long translated name checks, repeated network opens and disconnect/reconnect/dimension-change checks still require central runtime verification. Source/layout tests alone do not establish rendered game quality. No generated image or mockup is claimed as a runtime screenshot.
