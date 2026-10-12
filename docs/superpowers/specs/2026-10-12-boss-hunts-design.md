# Boss Hunts design

Approved in chat on 2026-10-12. Base: `3a47ea949e71deef157c47ab88cf8c0c5a44b6e2`, latest GitHub main at fetch.

## Player experience

Add a craftable Boss Hunts journal and a sneak, empty-hand interaction with an Ancient Altar. The item is an access tool; it contains no progress. A four-entry journal shows undiscovered, encountered, and defeated states. Undiscovered entries offer short ritual clues; discovered entries show the effective server activation item and east/west/south/north offerings. Pedestals are three blocks from the altar and offerings one block above them. No coordinates or natural boss locations are invented.

The screen uses a restrained dark parchment/purple palette, clear list/detail hierarchy, item icons, wrapped translated text, scrolling, keyboard entry navigation, focus narration and Escape/back. It refreshes safely on repeated opens and clears client state between connections/worlds.

## Persistence and discovery

Use schema-versioned overworld SavedData keyed by player UUID and canonical boss ID: `remnants:ossukage`, `remnants:kotsukage`, `remnants:umbrakar`, `remnants:hollow_sovereign`. States advance monotonically. Unknown boss records and future schemas must be handled without silently destroying data. Death, dimension changes, reconnects, item trading and restarts do not reset progress.

Discovery checks living bosses every ten ticks, within 48 blocks, in the same dimension, with server line of sight. Spectators are excluded. Network tracking is not treated as discovery; periodic checks cover players arriving later or moving into sight.

## Contribution and accepted death

Record actual accepted positive damage, resolving projectile and tame-pet ownership. Contributors must be active non-spectator players, in the boss dimension, within 64 blocks at the hit. At accepted death, credit the final-hit player and contributors who damaged the boss during the last 1,200 server ticks (60 seconds), with the same dimension/radius rule. Online players use their current position; disconnected players use their last recorded position/dimension, refreshed at logout. Do not credit teams or uninvolved bystanders.

Use the accepted LivingEntity death flag after `die(DamageSource)` completes, so cancelled Forge/NeoForge death does not award progress. Preserve DamageSource in 1.21.1 loader adapters. Removal and lingering death animations are not kill signals. Persist a processed marker on boss entities to make repeated death callbacks idempotent. Optional first-kill rewards are deferred.

Temporary encounters, contributor maps and request cooldowns are bounded, expire, and are cleared at shutdown. Persistent state never uses queueServerWork.

## Configuration and protocol

Resolve ritual descriptions from the same effective JAuml wrapper used by gameplay, including fallback behavior. Validate resource syntax and registry membership; invalid configured values display an unavailable ritual instead of a misleading default. Share validated resolution with ritual execution where necessary to prevent invalid configuration crashes.

Extend INetworkHelper and each active loader. A bounded, versioned S2C snapshot contains only the receiving player's four states and server ritual descriptors. An optional C2S refresh request carries only schema version, never target UUID, kill/discovery data or rewards. Derive player from transport context, validate version/alive/spectator state, and rate-limit to one request per second. Dedicated servers must not initialize UI classes.

## Compatibility and acceptance

Support Minecraft 1.20.1 Fabric/Forge, Java 17; Minecraft 1.21.1 Fabric/NeoForge, Java 21. Keep shared gameplay in common modules and adapters thin. Preserve combat, music, bars, recipes and ordinary drops.

Automated regression coverage: real NBT round-trips, corrupt/future schema handling, monotonic/idempotent state, contributor boundaries and ownership, discovery eligibility, cancellation guards and request validation/packet bounds. Run full buildAll, verifyCrafting, existing publisher/recipe checks, independent review, dedicated startup and client checks where tooling permits. Record passed, failed and unrun checks separately. In-game screenshots must be actual captures; generated previews do not establish runtime quality.

Deliver source, documentation, changelog, artifacts and exact-version test report. Prepare a draft PR if the repository workflow permits. Do not merge or publish releases.
