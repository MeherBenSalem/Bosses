# REMNANTS — Echoes of a Forgotten World

Design bible for the Remnants 3.0 overhaul. Every boss, creature, item, block and structure in the mod answers to this document. Slice specs (`docs/remnants/slices/*.md`) hold per-encounter detail; [realm.md](realm.md) holds the Eclipsed Realm, the dimension every Remnant now lives in, and the Waygate that leads there; [migration.md](migration.md) holds the registry and save-compatibility plan; [inventory.md](inventory.md) records what existed before the overhaul.

## 1. The world before

Long ago, several civilizations shared one discovery: a soul can be **poured into a vessel**. Armor, furnaces, crystals and carved stone could hold a soul, and a vessel that held one would obey, remember or endure. Historians in the Codex call this the **Age of Vessels**.

The civilizations differed in what they poured souls into:

| Civilization | Known for | What remains | Palette |
|---|---|---|---|
| **Vael**, the Hollow Crown | A kingdom of oath-bound knights and ceremonial armor | Hollow Sovereign, Eclipsed Knight, Hollow Legion, Hollow Citadel | Blackened iron, tarnished gold, faded royal blue |
| **Kharum**, the Ashen Forges | Artificers who built thinking machines around furnace hearts | Ashen Colossus, Ashbound Constructs, Ashen Foundry | Weathered bronze, soot black, ember orange |
| **Seyr**, the Lantern Archive | Scholars who stored memories inside cut crystal | Forgotten Watcher, memory constructs, Forgotten Observatory | Pale sandstone, verdigris copper, violet crystal |
| **The Ossuary Covenant** | Keepers of the dead and wardens of soul-prisons | Gravebound Warden, Cryptborn, Gravebound Catacombs | Rusted iron, old bone, grave moss |
| **The Sanctum of the Silent God** | A priesthood that served a living god through its Oracle | Shattered Oracle, Corrupted Relics, Shattered Sanctuary | Chalk marble, gilt, rose-violet soul glass |
| **The Thornwood Wardens** | Stone-shapers who refused vessels and guarded the old forests | Rootbound Titan, Rootbound creatures, Overgrown Bastion | Moss-grey stone, heartwood brown, amber rune light |

### The Hollowing

The Sanctum's god fell silent. Its **Oracle**, a divine construct of marble and soul glass forged by Kharum, refused to accept the silence. It began to rebuild its god from the only material that could hold divinity: souls.

On the night of the **Last Eclipse** the Oracle called every soul in the world to itself. Living bodies emptied where they stood. But souls already poured into vessels could not answer the call, and they stayed. That is why the world is full of armor without bodies, furnaces without stokers and crystals full of other people's memories.

- **Vael's king** saw the call coming and issued his **Last Command**: every knight of Vael was to bind their soul into their armor. The Hollow Legion is that command, still obeyed. The king bound himself last; the Hollow Sovereign is what remains.
- **Vael's champion** reached the Oracle during the eclipse and shattered it mid-ritual. The Hollowing stopped, but the champion was caught inside the moment of the eclipse. The **Eclipsed Knight** relives the final duel forever.
- **Kharum's great furnace** powered the Oracle's ritual engines. Its builders were hollowed, but the **Ashen Colossus** still burns, waiting for the next order.
- **Seyr** built the **Forgotten Watcher** to record everything before the end. It holds the memory of the Hollowing, and the truth of what caused it.
- **The Ossuary Covenant** caught the souls the Oracle dropped when it shattered and locked them in catacomb prisons. The **Gravebound Warden** still keeps the cells.
- **The Thornwood's** last guardian, the **Rootbound Titan**, stood still to wait out the eclipse. The forest grew through it while it waited.
- **The Shattered Oracle** is still trying to rebuild itself and its god, one stolen soul fragment at a time.

The player pieces this story together through boss encounters, Codex entries (the Boss Hunts journal), item tooltips and structure details. The final boss answers the question the whole mod asks: *what happened here?*

### The Eclipsed Realm

When the champion shattered the Oracle, its call did not stop; it tore. Everything that had answered as a vessel fell with the Oracle into the instant of the eclipse: **the Eclipsed Realm**, an open world under a black sun ringed with gold, with the golden **Gracetree** burning on the horizon. Every civilization's remains, every creature family and every arena now lives there; the overworld keeps only the **Waygates** that lead in. See [realm.md](realm.md).

## 2. Design pillars

1. **Forgotten civilizations.** Every encounter implies a lost history. Name things after what they *were*, not what they do.
2. **Living relics.** Monsters are vessels: armor, machines, crystal, stone, bone. Avoid flesh-and-blood demons, generic dragons and unrelated fantasy creatures.
3. **Environmental storytelling.** Structures show what the place was for: throne rooms, forge floors, reading halls, cell blocks, chapels, groves.
4. **Memorable, readable combat.** Every damaging attack has a windup of at least 10 ticks with a pose, sound or particle tell, and every attack can be avoided by moving, jumping, blocking or breaking line of sight. Transitions never deal damage.
5. **Meaningful exploration.** Structures hold the materials that unlock their own boss, so finding the place is part of the fight.
6. **Minecraft identity.** 16 px per block, vanilla-scale silhouettes, vanilla-compatible particles, crafting-table recipes and vanilla tiers as reference points.

## 3. Art direction

### Shared rules

- Texture density: 16 px per block, the same on every entity. Boss atlases may be 128×128 or 256×128, but never a higher pixel density.
- Shading: 3 to 5 tones per material ramp, hue-shifted (shadows cooler, highlights warmer). No noise fills, no smooth gradients, no photo textures.
- Wear tells the story: chipped edges, missing rivets, a scratched-out crest, cloth torn at the hem. Detail is placed where the eye lands (head, chest, weapon), not spread evenly.
- **Remnant light**, the shared soul colour, is pale cyan (`#2E8C8C → #4FC9C1 → #9CF2E6 → #E0FFFA`). It matches vanilla soul fire so `SOUL_FIRE_FLAME`, `SOUL` and `SCULK_SOUL` particles read as the same energy. Emissive pixels are rare: eyes, cores, cracks, runes.
- **Oracle corruption** is rose-violet (`#5B2A4F → #9C3F7E → #D77BC0 → #F6C9EC`). Anything touched by the Oracle shows it.
- Silhouette first: each creature must be recognisable as a black shape at 32 px tall.
- Bosses are 3 to 5 blocks tall with one unmistakable feature (crown, furnace chest, ring halo, chained mask, antlers, eclipse halo, floating mask). Common mobs stay within 1 to 2.5 blocks.

### Material ramps

| Material | Ramp (dark → light) |
|---|---|
| Blackened iron (Vael) | `#16171D #22242C #30333E #444855 #5E6373` |
| Tarnished gold (Vael) | `#3E2F16 #5A4520 #8A6A2E #B8913F #D9B860` |
| Royal blue cloth (Vael) | `#11162A #1B2340 #2A3560 #3A4A7E` |
| Weathered bronze (Kharum) | `#2A1C12 #4A321E #6E4C2A #94683A #B98B52` |
| Ember (Kharum) | `#5A1A08 #9C3412 #E0661C #FFA640 #FFE08A` |
| Sandstone (Seyr) | `#4A4336 #6E6450 #958A6E #BDB394 #DDD5B8` |
| Verdigris (Seyr) | `#1F3B36 #2F5C52 #4A8574 #73AE98` |
| Violet crystal (Seyr) | `#2A1F4A #45337A #6A52B0 #9E86E0 #D2C6FF` |
| Rusted iron (Ossuary) | `#2B1A14 #4A2C1E #6E4029 #8C5A3A #A87A56` |
| Old bone (Ossuary) | `#5A5244 #827865 #ABA08A #D1C8B0 #EDE6D2` |
| Chalk marble (Sanctum) | `#5F5A63 #8A8590 #B5B0BA #DAD6DE #F4F2F6` |
| Moss stone (Thornwood) | `#2F332C #474D40 #626B57 #7E8A6E` + moss `#2E4A1E #46702A #6A9A3A` |
| Heartwood (Thornwood) | `#2E1D12 #4A3020 #6B4A30 #8F6A45` + amber rune `#C46A10 #F2A23A #FFD98A` |

### Animation rules

- 24 fps keyframes, eased in and out; loops share first and last pose.
- Every attack clip has three readable beats: **anticipation** (windup pose, the telegraph), **action** (fast, 2 to 6 frames), **recovery** (the punish window).
- Damage frames in Java must match the clip's action beat. Each slice spec lists the shared tick timings.
- Death clips hold their final pose; the entity is removed only after the clip ends.

## 4. Bosses

| # | Boss | Civilization | Archetype | Tier | Replaces | Arena |
|---|---|---|---|---|---|---|
| 1 | **The Gravebound Warden** | Ossuary | Summoner and arena control | I (iron) | Kotsukage | Gravebound Catacombs |
| 2 | **The Rootbound Titan** | Thornwood | Environmental, exposed weak points | I–II | *new* | Overgrown Bastion |
| 3 | **The Hollow Sovereign** | Vael | Heavy melee to summoner, three phases | II (diamond) | Hollow Sovereign (old bone caster) | Hollow Citadel |
| 4 | **The Ashen Colossus** | Kharum | Mechanical, destructible plating, overheat | II | *new* | Ashen Foundry (Nether) |
| 5 | **The Forgotten Watcher** | Seyr | Magical ranged, patterns, teleport | III | Umbrakar | Forgotten Observatory |
| 6 | **The Eclipsed Knight** | Vael | Fast duelist: parries, dodges, afterimages | III (netherite) | Ossukage | Eclipse Field |
| 7 | **The Shattered Oracle** | Sanctum | Multi-stage final boss | Final | *new* | Shattered Sanctuary (End) |

Every boss gets: an original model, texture and full clip set; a dedicated combat controller with at least two phases; a boss bar and music cue; an arena structure; a summoning item crafted from that structure's materials; a unique weapon or shield; one armor set or armor piece; and one relic.

### Boss summaries

**The Hollow Sovereign** — *The Last King of Vael.* Broken royal armor with floating pauldrons, a shattered crown and a hollow chest holding his soul. Phase 1, *Fallen Monarch*: three-hit sword combos, a throne slam ring, a guarded counter-stance and a forward ground shockwave. Phase 2, *Shattered Throne*: his armor tears loose; spectral royal guards answer him; his pauldrons fly and dive independently; attacks speed up. Phase 3, *The Last Command*: the chest breaks open and his soul is visible; spectral crescent slashes and a soul lunge join his kit; at 10% he kneels and issues the Last Command, raining marked spectral blades. Rewards: Sovereign's Ruin, Hollow Crown, Royal Soul Fragment, Sovereign's Sigil relic, Regalia armor.

**The Ashen Colossus** — *The Furnace That Never Died.* Stone-and-bronze guardian with a cracked furnace chest and piston arms. Heat builds with every attack; at full heat it vents (damaging steam ring) and then overheats, opening its core as a weak point. Bronze plates on the arms and back can be broken off, removing the attacks they power. Final phase: plates gone, it runs hot permanently and throws molten volleys. Rewards: Ashen Warhammer, Colossus Core relic, Ancient Furnace Plates, Furnace Guardian armor.

**The Forgotten Watcher** — *Guardian of the Last Memory.* Floating stone body inside rotating rune rings around a violet crystal eye. Ring-patterned projectile volleys, a sweeping focused beam, teleports between four arena lenses, memory illusions (decoy Watchers that shatter when hit) and rotating shield phases where only the open ring segment can be hit. Rewards: Memory Staff, Watcher's Eye relic, Ancient Knowledge Fragments, Archivist armor.

**The Gravebound Warden** — *Keeper of the Forgotten Dead.* Executioner armor wrapped in chains, a rusted iron mask and a chained headsman's blade. Chain grabs that pull you in (break line of sight or strafe), wide sweeps, rows of grave eruptions, Gravebound Soldier summons, and chain walls that seal the arena into smaller cells. Destroying the four anchor chains ends the cell phase. Rewards: Warden's Grasp, Gravebound Executioner, Crypt Sigil relic, Warden armor.

**The Rootbound Titan** — *The Guardian Reclaimed by Nature.* A colossal stone guardian with a broken mask and branch antlers, grown through with roots. Root snares erupt under the player, bramble walls reshape the arena, corrupted sapling minions sprout, and wide sweeping arms clear space. Its rune heart is buried under bark; burning or cutting the bark plates exposes it. Rewards: Rootbound Greatshield, Titan Heart relic, Titan Heartwood, Thornwarden armor.

**The Eclipsed Knight** — *Champion of the Final War.* Sleek damaged knight armor, a broken ceremonial helm, a torn cloak and spectral afterimages. A true duel: fast combos, a readable parry stance, sidestep dodges, counterattacks, and afterimages that replay his last combo a beat later. At half health the eclipse deepens: afterimages strike independently. Rewards: Eclipsed Blade, Eclipsed Oath relic, Duelist armor, Forgotten Champion Sigil.

**The Shattered Oracle** — *The Voice of a Dead God.* A floating ceremonial mask over a broken stone body, crystalline limbs held in place by soul light, and a fragmented halo. Stage 1 is projectile formations behind fragment shields. Stage 2 adds illusions and soul absorption: it drains nearby souls, including the player's summons, to heal unless interrupted. Stage 3, *Reconstruction*: it rebuilds its body from fragments and uses every attack the player has seen from the other six bosses, in Oracle colours. Summoning requires a relic fragment from each other boss. Rewards: Oracle's Staff, Shattered Halo, Divine Remnant relic, the ending Codex entry.

## 5. Creature families

Difficulty tiers: **common** (fodder, packs), **uncommon** (changes how you fight), **elite** (dangerous alone), **miniboss** (structure-guarding, boss bar, no ritual).

| Family | Creature | Tier | Role | Replaces |
|---|---|---|---|---|
| Hollow Legion (Vael) | Hollow Sentinel | common | Shield guard; frontal block, slow heavy strike | skeleton_melee |
| | Hollow Arbalist | common | Heavy crossbow; slow, hard-hitting bolts | skeleton_archer |
| | Fallen Standard Bearer | uncommon | Banner aura buffs nearby Legion; kill it first | *new* |
| | Soulbound Knight | elite | Sword combos, occasional counter | *new* |
| | Spectral Royal Guard | boss summon | Sovereign phase 2 | *new* |
| Ashbound Constructs (Kharum) | Ashbound Crawler | common | Fast furnace bug; flame breath that sets you burning, ember burst on death | armored_grub |
| | Ember Servitor | common | Maintenance drone; short-range ember shots, repairs constructs | *new* |
| | Rusted Guardian | elite | Heavy soldier; slow turn, big recovery | *new* |
| Forgotten Souls | Lost Pilgrim | common | Neutral wanderer; hostile when struck | *new* |
| | Memory Wraith | uncommon | Evasive flyer; soul bolts | wraith |
| | Soul Collector | elite | Harvests souls, summons wisps, buffs spectres | dirge_lantern |
| Cryptborn (Ossuary) | Crypt Lurker | common | Low crawler; ambush leap | grave_skitter |
| | Gravebound Soldier | common | Rusted-weapon guard; fights in groups | skeleton_minion |
| | Burial Guardian | miniboss | Stone tomb guardian; slow crush, high defence | *new* |
| | Catacomb Rat | common (ambient) | Scavenger swarm; harmless alone | rat |
| Corrupted Relics (Sanctum) | Shardling | common | Fragile floating crystal; ranged bolts in groups | *new* |
| | Ancient Vessel | uncommon | Slow living urn; soul pulse ring, unstable burst when broken | *new* |
| | Relic Stalker | elite | Assembled artifact-limbs; skittering ambush | sickleghast |

Each family shares a palette and a drop material so the player learns one visual language per civilization. Structures spawn their family; a reduced set spawns naturally (see §7).

**Ahead of their bosses.** The Ashbound Constructs and the Corrupted Relics already roam the Eclipsed Realm (Ashen Wastes, Ossuary Depths, Gracewood, Tarnished Fields, Gravefield Barrows) and drop their materials, Ashen Cog and Relic Shard, before slices 6 and 8 bring the Ashen Colossus and the Shattered Oracle. The armored grub and the sickleghast are retired already. Stats, clips, attacks, spawns and loot: [families/ashbound-and-relics.md](families/ashbound-and-relics.md).

## 6. Items, equipment and relics

### Family materials

| Material | Dropped by | Used for |
|---|---|---|
| Tarnished Regalia | Hollow Legion | Sovereign's Seal, Regalia armor |
| Ashen Cog | Ashbound Constructs | Colossus summon, Furnace armor |
| Memory Shard | Forgotten Souls, Seyr ruins | Watcher summon, Archivist armor, Memory Staff upgrades |
| Grave Iron | Cryptborn | Warden summon, Warden armor |
| Relic Shard | Corrupted Relics | Oracle summon, relic repair |
| Heartwood Sliver | Rootbound saplings | Titan summon, Thornwarden armor |

### Weapons (one signature weapon per boss)

| Weapon | Boss | Identity |
|---|---|---|
| Sovereign's Ruin | Hollow Sovereign | Greatsword. Hold use to charge; release for a piercing spectral crescent slash. |
| Ashen Warhammer | Ashen Colossus | Hammer. Hits build heat; at full heat the next charged slam makes a fire shockwave. |
| Memory Staff | Forgotten Watcher | Focus. Fires homing memory bolts; charged use releases a ring burst. |
| Warden's Grasp | Gravebound Warden | Chain weapon. Use hooks and pulls a target; sweep hits slow. |
| Gravebound Executioner | Gravebound Warden | Axe. Bonus damage to slowed or chained targets. |
| Rootbound Greatshield | Rootbound Titan | Shield. Block stance roots nearby attackers briefly. |
| Eclipsed Blade | Eclipsed Knight | Fast sword. Blocking an attack in the last 0.3 s with a shield in the off-hand empowers the next hit with an afterimage strike. |
| Oracle's Staff | Shattered Oracle | Focus. Steerable projectile formation; charged area strike. |

### Armor

Six sets (Regalia, Furnace Guardian, Archivist, Warden, Thornwarden, Duelist), each between diamond and netherite in value at its tier. Each has a two-piece and a four-piece bonus. Bonuses reward a playstyle and have cooldowns; none grant permanent flight, invulnerability or flat damage multipliers above 15%.

### Relics

A relic is an ancient artifact that works **from the off-hand**. Choosing a relic means giving up a shield, totem or torch. Each relic has a passive effect while held and an active ability on use with a cooldown and a **cost**:

| Relic | Passive | Active | Cost |
|---|---|---|---|
| Sovereign's Sigil | Nearby tamed allies deal +10% damage | Summon two spectral royal guards for 20 s | Lose 4 max health until they fade |
| Colossus Core | Immune to fire while above 50% health | Vent heat: knockback ring that ignites | Slowness II for 4 s afterward |
| Watcher's Eye | Highlights hostile mobs within 24 blocks every 10 s | Blink 8 blocks toward where you look | 2 hunger points |
| Crypt Sigil | Undead deal 10% less damage to you | Chain the nearest enemy in place for 3 s | Wither I for 3 s on yourself |
| Titan Heart | Slow natural regeneration on grass or moss | Root yourself: Resistance II, cannot move, 5 s | Cannot move |
| Eclipsed Oath | Next hit after a successful block crits | Become intangible to projectiles for 2 s | Weakness I for 5 s afterward |
| Divine Remnant | — | Copies the active of the last relic you used | Its cost doubled |

## 7. Structures and world generation

All Remnant structures generate in the Eclipsed Realm ([realm.md](realm.md) §3 and §9), whose six biomes stand in for the overworld, Nether and End biomes originally planned below. The Realm biome each structure uses is listed first; the original placement is kept for reference.

| Structure | Dimension and biomes | Contents |
|---|---|---|
| Hollow Citadel | Realm: Tarnished Fields, Ashen Wastes (was overworld plains, meadow, taiga, snowy plains) | Ruined keep, barracks with Legion spawners, royal vault, throne arena with altar |
| Ashen Foundry | Nether: basalt deltas, crimson forest edge | Forge halls, conveyor ruins, construct pens, furnace-heart arena |
| Forgotten Observatory | Realm: Lantern Heights, the inland mountains (was overworld jagged/frozen peaks, stony peaks) | Reading halls, lens platforms, memory vault, dome arena |
| Gravebound Catacombs | Realm underground (Y −20 to 20), any land biome (was overworld underground) | Cell blocks, ossuary niches, chain chapel arena |
| Shattered Sanctuary | End: outer islands | Floating chapel fragments, relic chambers, halo arena |
| Overgrown Bastion | Overworld: dark forest, old growth taiga, jungle | Root-split fortress, grove courtyard arena |
| Eclipse Field | Overworld: savanna plateau, windswept hills | Battlefield ruins, broken siege engines, duel circle |

Structures are datapack worldgen (jigsaw or single-template) and only appear in **newly generated chunks**. Each arena has a pre-built altar ring; the summoning item comes from that structure's family drops and loot, so discovery leads to the fight. The classic altar ritual still works anywhere unless disabled in config.

Natural spawning happens only in the Eclipsed Realm, from each Realm biome's own spawn list (patrols of the Hollow Legion in the Tarnished Fields, Cryptborn in the Gravefield Barrows and the Ossuary Depths, Forgotten Souls in the Lantern Heights, and so on), thinned so the open world stays readable. Remnant creatures never spawn naturally in the overworld. Arena structures keep their own spawn lists.

## 8. Progression

```
Iron ──► Gravebound Warden ──► Rootbound Titan
              │                       │
Diamond ─► Hollow Sovereign ──► Ashen Colossus (Nether)
              │                       │
Netherite ► Forgotten Watcher ──► Eclipsed Knight
                          │
End ─────────────► Shattered Oracle (needs a relic fragment from each of the six)
```

The Boss Hunts journal becomes the **Codex of Remnants**: witnessing a boss records its rite; defeating it unlocks its lore entry and the next lead (which structure to look for).

## 9. Delivery order

Work ships in vertical slices. Each slice delivers its boss, family, gear, relic, arena and recipes on all four loaders, built and smoke-tested, before the next starts.

1. **Foundation**: inventory, this bible, migration plan, alias layer.
1b. **The Eclipsed Realm**: dimension, biomes, Waygate entry, graces, fog gates, banners ([realm.md](realm.md)). Every later slice places its arena and spawns there.
2. **Hollow Sovereign + Hollow Legion + Hollow Citadel.**
3. **Gravebound Warden + Cryptborn + Catacombs** (retires Kotsukage, skeleton minion, grave skitter, rat).
4. **Forgotten Watcher + Forgotten Souls + Observatory** (retires Umbrakar, wraith, dirge lantern).
5. **Eclipsed Knight + Eclipse Field** (retires Ossukage, kunai, ossukage sword).
6. **Ashen Colossus + Ashbound Constructs + Foundry** (the constructs and the armored grub's retirement shipped early; see §5).
7. **Rootbound Titan + Overgrown Bastion.**
8. **Shattered Oracle + Corrupted Relics + Sanctuary** and the ending (the relics and the sickleghast's retirement shipped early; see §5).
