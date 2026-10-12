# Boss Hunts

Boss Hunts records personal progress for Ossukage, Kotsukage, Umbrakar and Hollow Sovereign. These are altar bosses: the journal never invents naturally generated locations or tracks coordinates.

## Opening the journal

Combine a Book, Bone and Ink Sac in a crafting grid to make the Boss Hunts Journal. Use it from either hand. It is also in the Remnant Bosses creative tab. Alternatively, sneak and interact with an Ancient Altar with an empty main hand.

Arrow keys select entries. Tab moves between controls. Page Up/Page Down and the mouse wheel scroll the detail page; Home/End jump to its ends. Escape or Back closes the journal. Selected entries and controls support Minecraft narration. The layout adapts to GUI scale, and long translated text wraps or has a tooltip.

## Discovery and rituals

Each entry begins **Undiscovered**, with a short ritual clue. Every ten server ticks, a live boss can be encountered by a living, non-spectator player within 48 blocks in the same dimension and with line of sight. Merely receiving the boss entity over the network does not discover it. Entering range later or walking around an obstruction can reveal it.

An **Encountered** or **Defeated** entry shows the effective server JAuml activation item and four offering blocks. Place Ancient Pedestals three blocks from the altar's center in each cardinal direction, leaving two intervening blocks. Each offering is a placed block one block above its pedestal. The order is east, west, south, north; altar facing does not change this.

Invalid, missing, oversized, unknown or air activation/offering IDs make the ritual unavailable. The journal never substitutes a default registry air value. Ritual checks share this validation, including the 1.21.1 checks that previously threw on malformed IDs. Hollow Sovereign's summoning-enabled setting is also respected. Other gameplay restrictions, such as Sovereign space/difficulty/nearby-boss checks, still apply.

Values are read through the server's effective JAuml wrapper. Bootstrap defaults and fallback defaults can differ; the journal does not hardcode either set. No new configuration file is required. In particular, existing Kotsukage fallback omissions and the versioned Ossukage fallback difference are not silently replaced.

## Personal defeat credit

Progress advances only after Minecraft accepts the boss's death. Forge/NeoForge cancelled death events do not award defeat. Boss removal, chunk unload and the end of a death animation are not kill signals. Each boss persists a processed marker, and player states only advance, making callbacks and repeated kills idempotent.

The final damaging player and qualifying contributors receive credit. Damage must actually reduce the boss's health; cancelled hits and absorbed-only hits do not count. A qualifying hit comes from a living, non-spectator player in the boss dimension within 64 blocks. Projectile owners are resolved explicitly, and tame-animal attacks use the owner's player UUID. An offline owner cannot begin a new contribution.

At completion, qualifying damage must be within the last 1,200 server ticks, nominally 60 seconds. Online contributors must be non-spectators in the same dimension within 64 blocks of the boss. Disconnected contributors use their position/dimension recorded at logout, or at their last qualifying hit if no logout callback was available. No credit is given to an uninvolved player, an entire team or the whole server. A contributor who leaves the dimension or radius before logging out does not qualify.

Temporary combat contributions are not persisted through a server restart or boss unload. Awarded journal states are persisted. Contributors are bounded to 128 per encounter, keeping the most recent; temporary encounters are bounded to 1,024 and expire after the contribution window. Optional first-kill rewards are deferred; ordinary boss loot is unchanged.

## Persistence and networking

Progress is stored in the world's overworld SavedData file `data/remnant_bosses_hunts.dat`, schema version 1, keyed by player UUID and canonical entity IDs `remnants:ossukage`, `remnants:kotsukage`, `remnants:umbrakar` and `remnants:hollow_sovereign`. Player death, dimension changes, reconnects and restart preserve awarded progress. Different worlds have different journals. Journal item ownership has no effect.

Unknown records are preserved. Unsupported or malformed schema versions are opened read-only to avoid overwriting data from a newer version; restore a compatible backup or use a compatible mod version instead of deleting the original. Temporary encounter and request data lives with the world's SavedData instance and is cleared at server shutdown; no journal state uses the static ritual work queue.

The server sends only a receiving player's four-entry snapshot. C2S refresh requests contain a schema version, never a target UUID, kill or discovery state. The transport supplies the authenticated player, the server excludes dead/spectator requests, and requests are limited to once every 20 ticks. Packet sizes, boss IDs, states, duplicates and strings are validated. Forge/NeoForge protocol 2 rejects peers lacking the journal packets. Client journal screens clear on connection/world changes.

## Verification

Both common modules provide `:common:verifyJournal`, wired into `check`. It exercises NBT persistence, monotonic states, cancellation/discovery policies, time/distance/dimension credit boundaries, disconnected contributors, bounded temporary maps, request validation, real packet codecs, configuration registry validation and layout bounds. These tests do not replace actual loader and world checks. See [the test report](boss-hunts-test-report.md) for exact passed, failed and unrun scenarios.
