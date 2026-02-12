=============================================  
## **Changelog 2.0.0**  
=============================================  

*   **Multi-module build**: project split into `1.20.1-forge/` and `1.21.1-neoforge/` modules with a shared root build configuration. (Phase 2 complete)
*   **Neoforge upgrade**: added `1.21.1-neoforge` module and build scripts.
*   **MCreator → standalone migration**: removed MCreator artifacts and ported code to a standalone structure.
*   **Jauml config**: added safe / Jauml config support (bootstrap + thin wrapper) so config files are auto-created when the library is present — graceful fallback if absent.
*   **Gameplay & visual fixes**: animations (attack/spawn sync), client renderer/model registration fixes to prevent NPEs, improved boss/music handling and event wiring.
*   **New/ported content**: entity renderers, model layers, network packets, new procedures and resource data for neoforge.
*   **New entity — Wraith**: added `Wraith` (entity, model, renderer, spawn egg, AI & animations).
*   **Config: spawning & management**: added Jauml-config-driven spawn/management options for creature tuning and enable/disable controls (see `remnant/spawning:wraith_spawns` and `remnant/balance:wraith_stats`).

=============================================  
## **Changelog 2.0.0b**  
=============================================  

### ✨ **Major Changes**
- Fully removed **GeckoLib**; the mod now uses the **new Java model system**.  
- Updated all mob models to the new format with improved performance and animations.  
- All mobs now **float on water**.  

### ⚔️ **Remnant Ossukage Improvements**
- Phase 2 no longer makes the Remnant Ossukage **invisible**. He now **gains bonus health** on transformation.  
- Phase 2 **special dashes** now have a chance to **explode on top of the player**.  
- The Remnant Ossukage now spawns with **two skeletons** at the start of the fight.  

### 🐀 **New Mob Drops**
- Added new drops for **rats** and **skeleton minions**:  
  - **Rat Fang**  
  - **Old Skeleton Bone**  
  - **Old Skeleton Head**  

### 🛕 **New Blocks & Features**
- Added **Ancient Pedestal** and **Ancient Altar**, used to **summon bosses**.  
- Added a new **configuration option** for customizing boss summoning behavior.  
