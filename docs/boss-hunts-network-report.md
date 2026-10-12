Final verification outcomes are consolidated in [the test report](boss-hunts-test-report.md). The implementation-stage evidence below is retained as a record.

# Boss Hunts network implementation

Scope: Minecraft 1.20.1 Fabric/Forge and 1.21.1 Fabric/NeoForge.

## Implemented

- Both common modules have immutable journal snapshot and version-only request records, plus network service methods.
- Codecs validate schema 1, zero to four unique canonical bosses, known states, strings up to 256 characters, exactly four offerings, truncated buffers, and trailing data. Constructors enforce the same writer bounds.
- All four transports send snapshots only to the intended server player and authenticate refresh requests using their transport context. Directions are registered separately. Work is scheduled on the appropriate game thread.
- Client adapters check connection/world identity before queued delivery and call the client cache tick only on the physical client. Dedicated adapters reach journal UI only through nested client classes or client entrypoints.
- Server tick, logout, and stop lifecycle adapters call the common journal service. Minecraft 1.21.1 death adapters preserve DamageSource.
- Journal items are included in all four loader registrations. Boss music registrations remain present.
- Forge/NeoForge transport protocol changed from 1 to 2 because journal packets add a required protocol capability.

## Regression coverage and checks

JournalPacketRegression.java in each common test tree exercises real FriendlyByteBuf/Netty buffers: snapshot/request round trips; immutable snapshot copying; future/negative schemas; excessive/negative entry counts; duplicate/unknown boss IDs; invalid states; 257-character rejection and 256-character acceptance; missing/excess offerings; truncation; trailing request identity data; and constructor writer bounds. The common verifyJournal harness invokes these checks.

Passed: git diff --check (2026-10-12); source ownership/API consistency checks.

Expected initial failure: after tests were written and before packet implementation, a PowerShell missing-implementation preflight failed with "JournalPacketRegression cannot run because ClientboundJournalPacket implementation is absent." This preflight is not a Java regression execution.

Deferred to the coordinating root agent: both actual verifyJournal tasks, all four loader compilation tasks, dedicated server startup, and client/server runtime networking. This worker ran no Gradle commands to avoid colliding with the centralized baseline/integration builds. Until those checks finish, compilation and runtime success are not claimed.

## Acceptance mapping

Done in source: bounded protocol; no client target identity/progress fields; authenticated personal delivery; thread scheduling; lifecycle/cache tick adapters; dedicated client guards; preserved death source/music; loader item registration.

Pending verification: actual packet regression execution and four-loader compilation/runtime scenarios.

Centralized verification update: coordinating agent reported Minecraft 1.20.1 common compileJava passed. Its compileTestJava found three test-only chains where inherited writeBoolean returns ByteBuf; split those writes in both version fixtures. The targeted diff check passes; centralized rerun is pending. No Gradle commands were launched by this worker.

Modern transport source check: NeoForge's 1.21.1 [CustomPacketPayload patch](https://github.com/neoforged/NeoForge/blob/1.21.1/patches/net/minecraft/network/protocol/common/custom/CustomPacketPayload.java.patch) passes the remaining custom packet buffer directly to the selected codec after its payload ID. RegistryFriendlyByteBuf adds registry access and does not change that framing. The existing trailing-data rejection is therefore intended for the remainder of this packet. Full transport runtime verification remains pending. Its [IPayloadContext source](https://github.com/neoforged/NeoForge/blob/1.21.1/src/main/java/net/neoforged/neoforge/network/handling/IPayloadContext.java) also confirms context.connection(), now used to authenticate queued client delivery against the originating connection.
