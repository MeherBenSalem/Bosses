package com.nightbeam.realmprobe;

import com.nightbeam.remnants.block.realm.EclipseWaygateBlock;
import com.nightbeam.remnants.block.realm.GraceEmberBlockEntity;
import com.nightbeam.remnants.block.realm.WaygatePhase;
import com.nightbeam.remnants.block.realm.WaygateShape;
import com.nightbeam.remnants.block.realm.WaygateVeilBlock;
import com.nightbeam.remnants.client.realm.GraceScreen;
import com.nightbeam.remnants.init.ModBlocks;
import com.nightbeam.remnants.init.ModItems;
import com.nightbeam.remnants.network.ServerboundGraceActionPacket;
import com.nightbeam.remnants.realm.EclipsedRealm;
import com.nightbeam.remnants.realm.GraceRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import com.mojang.datafixers.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Development-only walkthrough of the Eclipsed Realm in a disposable world copy ("RealmValidation"): build and
 * kindle a Waygate, cross, arrive at the shrine, rest at the grace, tour the biomes and depart. Every step goes
 * through the production blocks, items and packets handlers; each beat is captured from the real framebuffer.
 */
public final class RealmRuntimeProbe implements ClientModInitializer {
	private static final Logger LOG = LoggerFactory.getLogger("RealmRuntimeTest");
	private static final String WORLD = "RealmValidation";
	private static final String[] BIOMES = {"tarnished_fields", "gracewood", "gravefield_barrows", "ashen_wastes", "lantern_heights", "stilled_sea"};

	private final List<String> report = new ArrayList<>();
	private final List<Path> captures = new ArrayList<>();
	private final AtomicInteger pendingCaptures = new AtomicInteger();
	private int ticks;
	private int step;
	private int waitUntil;
	private int stepStart;
	private boolean finished;
	private String finalResult;
	private volatile String serverFailure;
	private volatile BlockPos core;
	private volatile BlockPos realmGrace;
	private volatile BlockPos freshGrace;
	private volatile boolean serverDone;
	private int biome;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
		LOG.info("DEV ONLY: armed for exact world folder {}", WORLD);
	}

	private void tick(Minecraft client) {
		if (finished) return;
		ticks++;
		client.options.pauseOnLostFocus = false;
		try {
			if (finalResult != null) {
				if (pendingCaptures.get() == 0 || ticks > waitUntil + 600) finishNow(client);
				return;
			}
			if (serverFailure != null) throw new IllegalStateException(serverFailure);
			if (ticks > 20 * 60 * 12) throw new IllegalStateException("timed out at step " + step);
			MinecraftServer server = client.getSingleplayerServer();
			if (server == null || client.player == null || ticks < waitUntil) return;
			ServerPlayer actor = server.getPlayerList().getPlayers().isEmpty() ? null : server.getPlayerList().getPlayers().get(0);
			if (actor == null) return;
			run(client, server, actor);
		} catch (Throwable error) {
			LOG.error("Realm probe failure", error);
			finish("FAIL step " + step + ": " + error);
		}
	}

	private void run(Minecraft client, MinecraftServer server, ServerPlayer actor) {
		switch (step) {
			case 0 -> {
				String folder = server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString();
				require(folder.equals(WORLD), "refusing to touch world folder " + folder);
				onServer(server, () -> setUp(server, actor));
				next(40);
			}
			case 1 -> {
				require(serverDone, "set-up and Waygate placement finished on the server");
				BlockState state = server.overworld().getBlockState(core);
				require(state.getBlock() instanceof EclipseWaygateBlock, "Waygate core placed by the real item at " + core);
				for (BlockPos part : WaygateShape.frame(core, state.getValue(EclipseWaygateBlock.FACING))) {
					require(server.overworld().getBlockState(part).is(ModBlocks.WAYGATE_FRAME.get()), "frame part at " + part);
				}
				pass("Waygate item raised the full 3x4 arch, facing " + state.getValue(EclipseWaygateBlock.FACING));
				capture(client, "realm-01-waygate-dormant");
				onServer(server, () -> kindle(server, actor));
				next(70);
			}
			case 2 -> {
				require(server.overworld().getBlockState(core).getValue(EclipseWaygateBlock.PHASE) == WaygatePhase.KINDLING,
						"Eclipse Shard kindles the Waygate");
				pass("Eclipse Shard kindled the Waygate through the real block use path");
				capture(client, "realm-02-waygate-kindling");
				next(70);
			}
			case 3 -> {
				BlockState state = server.overworld().getBlockState(core);
				require(state.getValue(EclipseWaygateBlock.PHASE) == WaygatePhase.OPEN, "Waygate opens after the kindle");
				for (BlockPos veil : WaygateShape.veil(core, state.getValue(EclipseWaygateBlock.FACING))) {
					require(server.overworld().getBlockState(veil).getValue(WaygateVeilBlock.OPEN), "veil opened at " + veil);
				}
				pass("kindle completed; the arch is open");
				capture(client, "realm-03-waygate-open");
				onServer(server, () -> actor.teleportTo(server.overworld(), core.getX() + 0.5, core.getY() + 0.2, core.getZ() + 0.5, 0, 0));
				next(22);
			}
			case 4 -> {
				capture(client, "realm-04-crossing");
				stepStart = ticks;
				next(1);
			}
			case 5 -> {
				if (!EclipsedRealm.isRealm(actor.level())) {
					require(ticks - stepStart < 400, "player crossed into the Eclipsed Realm");
					return;
				}
				pass("crossing landed in " + actor.level().dimension().location() + " at " + actor.blockPosition());
				next(70);
			}
			case 6 -> {
				capture(client, "realm-05-arrival");
				onServer(server, () -> findShrine(server, actor));
				look(client, 0, -48);
				next(20);
			}
			case 7 -> {
				require(realmGrace != null, "Arrival Shrine has a Grace Ember near the landing spot");
				pass("Arrival Shrine built with grace at " + realmGrace);
				capture(client, "realm-06-eclipse-sky");
				// Rise above the shrine to see the Gracetree on the northern horizon.
				onServer(server, () -> {
					actor.getAbilities().flying = true;
					actor.onUpdateAbilities();
					actor.teleportTo(EclipsedRealm.level(server), actor.getX(), actor.getY() + 40, actor.getZ(), 180, -4);
				});
				look(client, 180, -4);
				next(60);
			}
			case 8 -> {
				capture(client, "realm-07-gracetree");
				// Close on the redesigned grace before resting at it.
				onServer(server, () -> actor.teleportTo(EclipsedRealm.level(server), realmGrace.getX() + 0.5, realmGrace.getY() + 0.6,
						realmGrace.getZ() + 4.5, 180, 20));
				look(client, 180, 20);
				next(60);
			}
			case 9 -> {
				capture(client, "realm-07b-grace-ember");
				onServer(server, () -> discoverGraces(server, actor));
				next(50);
			}
			case 10 -> {
				require(serverDone, "extra graces discovered on the server");
				require(client.screen instanceof GraceScreen, "grace menu opened from the real packet");
				require(EclipsedRealm.LEVEL.equals(actor.getRespawnDimension()) && actor.getRespawnPosition() != null,
						"resting set the respawn point in the Realm");
				pass("rested: respawn " + actor.getRespawnPosition() + ", grace menu shown with distances");
				capture(client, "realm-08-grace-menu");
				next(10);
			}
			case 11 -> {
				client.setScreen(null);
				onServer(server, () -> protect(server, actor));
				look(client, 180, 30);
				next(15);
			}
			case 12 -> {
				require(serverDone, "Realm protection checks finished on the server");
				require(client.gameMode.getPlayerMode() == GameType.SURVIVAL
						&& client.player.blockActionRestricted(client.level, realmGrace.below(), GameType.SURVIVAL),
						"the client refuses breaking for a survival player too");
				pass("Realm protection: survival break, place, bucket and explosion refused; creative still builds");
				capture(client, "realm-08b-protected");
				onServer(server, () -> {
					actor.setGameMode(GameType.CREATIVE);
					actor.getAbilities().flying = true;
					actor.onUpdateAbilities();
					actor.teleportTo(EclipsedRealm.level(server), realmGrace.getX() + 0.5, realmGrace.getY() + 0.6, realmGrace.getZ() + 4.5, 180, 20);
				});
				look(client, 180, 20);
				next(40);
			}
			case 13 -> {
				// Resting added the renderer's block entity on the client even for a grace saved before it existed.
				require(client.level.getChunkAt(realmGrace).getBlockEntity(realmGrace, LevelChunk.EntityCreationType.CHECK)
						instanceof GraceEmberBlockEntity, "the arrival grace has its glow block entity on the client");
				pass("arrival grace renders its glow (block entity present on the client)");
				capture(client, "realm-08c-grace-glow");
				onServer(server, () -> actor.teleportTo(EclipsedRealm.level(server), freshGrace.getX() + 0.5, freshGrace.getY() + 5,
						freshGrace.getZ() + 16.5, 180, 14));
				look(client, 180, 14);
				next(100);
			}
			case 14 -> {
				require(client.level.getChunkAt(freshGrace).getBlockEntity(freshGrace, LevelChunk.EntityCreationType.CHECK)
						instanceof GraceEmberBlockEntity, "a newly placed grace arrives on the client with its block entity");
				pass("newly placed grace at " + freshGrace + " arrived with its glow block entity");
				capture(client, "realm-08d-grace-afar");
				next(1);
			}
			case 15 -> {
				client.setScreen(null);
				if (biome >= BIOMES.length) {
					next(1);
					step = 17;
					return;
				}
				serverDone = false;
				String id = BIOMES[biome];
				onServer(server, () -> visitBiome(server, actor, id));
				next(140);
				step = 16;
			}
			case 16 -> {
				capture(client, "realm-09-" + BIOMES[biome]);
				biome++;
				step = 15;
				waitUntil = ticks + 2;
			}
			case 17 -> {
				onServer(server, () -> visitCitadel(server, actor));
				next(160);
			}
			case 18 -> {
				capture(client, "realm-10-hollow-citadel");
				onServer(server, () -> visitObservatory(server, actor));
				next(160);
			}
			case 19 -> {
				capture(client, "realm-10b-forgotten-observatory");
				onServer(server, () -> {
					ServerLevel realm = EclipsedRealm.level(server);
					actor.teleportTo(realm, realmGrace.getX() + 0.5, realmGrace.getY(), realmGrace.getZ() + 1.5, 180, 0);
				});
				next(40);
			}
			case 20 -> {
				onServer(server, () -> GraceRegistry.handleAction(actor, new ServerboundGraceActionPacket(realmGrace,
						ServerboundGraceActionPacket.DEPART, new ResourceLocation("minecraft", "overworld"), realmGrace)));
				stepStart = ticks;
				next(1);
			}
			case 21 -> {
				if (EclipsedRealm.isRealm(actor.level())) {
					require(ticks - stepStart < 400, "Depart returns the player to the overworld");
					return;
				}
				pass("departed to " + actor.level().dimension().location() + " at " + actor.blockPosition()
						+ " (Waygate at " + core + ")");
				next(60);
			}
			case 22 -> {
				capture(client, "realm-11-returned");
				finish("PASS realm runtime walkthrough");
			}
			default -> {
			}
		}
	}

	private void setUp(MinecraftServer server, ServerPlayer actor) {
		try {
			ServerLevel overworld = server.overworld();
			actor.setGameMode(GameType.CREATIVE);
			actor.getAbilities().flying = true;
			actor.onUpdateAbilities();
			overworld.setDayTime(6000);
			for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
				overworld.setBlock(new BlockPos(x, 199, z), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
				for (int y = 200; y < 210; y++) overworld.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
			}
			// Stand south of the target, facing north: the arch's front turns toward the player.
			actor.teleportTo(overworld, 0.5, 200, 6.5, 180, 0);
			actor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.get(ModBlocks.ECLIPSE_WAYGATE.id())));
			BlockPos target = new BlockPos(0, 199, 0);
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target).add(0, 0.5, 0), Direction.UP, target, false);
			actor.getMainHandItem().useOn(new UseOnContext(actor, InteractionHand.MAIN_HAND, hit));
			core = target.above();
			actor.teleportTo(overworld, 0.5, 200, 7.5, 180, -8);
			serverDone = true;
		} catch (Throwable e) {
			serverFailure = "set-up: " + e;
		}
	}

	private void kindle(MinecraftServer server, ServerPlayer actor) {
		try {
			actor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ECLIPSE_SHARD.get()));
			ServerLevel overworld = server.overworld();
			BlockPos veil = core.above();
			overworld.getBlockState(veil).use(overworld, actor, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(veil), Direction.SOUTH, veil, false));
			actor.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		} catch (Throwable e) {
			serverFailure = "kindle: " + e;
		}
	}

	private void findShrine(MinecraftServer server, ServerPlayer actor) {
		ServerLevel realm = EclipsedRealm.level(server);
		BlockPos around = actor.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(around.offset(-8, -3, -8), around.offset(8, 3, 8))) {
			if (realm.getBlockState(pos).is(ModBlocks.GRACE_EMBER.get())) {
				realmGrace = pos.immutable();
				return;
			}
		}
	}

	private void useGrace(MinecraftServer server, ServerPlayer actor) {
		try {
			ServerLevel realm = EclipsedRealm.level(server);
			actor.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			realm.getBlockState(realmGrace).use(realm, actor, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(realmGrace), Direction.UP, realmGrace, false));
		} catch (Throwable e) {
			serverFailure = "grace: " + e;
		}
	}

	/**
	 * Two more Realm graces and one in the overworld, so the menu shows meters and "another world"; the arrival grace
	 * is clicked last, through the real block-use path, so its menu is the one on screen.
	 */
	private void discoverGraces(MinecraftServer server, ServerPlayer actor) {
		try {
			ServerLevel realm = EclipsedRealm.level(server);
			actor.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			for (int[] offset : new int[][] {{64, -90}, {-150, 260}}) {
				BlockPos grace = surfaceGrace(realm, realmGrace.getX() + offset[0], realmGrace.getZ() + offset[1]);
				GraceRegistry.use(actor, realm, grace, InteractionHand.MAIN_HAND);
				if (freshGrace == null) freshGrace = grace;
			}
			BlockPos home = new BlockPos(4, 200, 3);
			server.overworld().setBlock(home, ModBlocks.GRACE_EMBER.get().defaultBlockState(), 3);
			GraceRegistry.use(actor, server.overworld(), home, InteractionHand.MAIN_HAND);
			useGrace(server, actor);
			serverDone = serverFailure == null;
		} catch (Throwable e) {
			serverFailure = "graces: " + e;
		}
	}

	private static BlockPos surfaceGrace(ServerLevel level, int x, int z) {
		level.getChunk(x >> 4, z >> 4);
		BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
		level.setBlock(pos, ModBlocks.GRACE_EMBER.get().defaultBlockState(), 3);
		return pos;
	}

	/**
	 * The Realm does not change for a survival player: breaking, placing, buckets and explosions all leave it whole.
	 * The same placement then succeeds in creative, so the refusal comes from the protection and nothing else.
	 */
	private void protect(MinecraftServer server, ServerPlayer actor) {
		try {
			ServerLevel realm = EclipsedRealm.level(server);
			BlockPos spot = GraceRegistry.standSpot(realm, realmGrace);
			actor.teleportTo(realm, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 180, 30);
			actor.setGameMode(GameType.SURVIVAL);
			actor.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

			BlockPos floor = spot.below();
			BlockState ground = realm.getBlockState(floor);
			require(!actor.gameMode.destroyBlock(floor) && realm.getBlockState(floor).equals(ground),
					"survival breaking is refused (" + ground + " at " + floor + ")");

			BlockPos free = null;
			for (BlockPos pos : BlockPos.betweenClosed(spot.offset(-3, -1, -3), spot.offset(3, 1, 3))) {
				if (!pos.equals(spot) && !pos.equals(spot.above()) && realm.getBlockState(pos).isAir()
						&& realm.getBlockState(pos.below()).isSolidRender(realm, pos.below())) {
					free = pos.immutable();
					break;
				}
			}
			require(free != null, "an open floor tile near the grace to try building on");
			BlockHitResult face = new BlockHitResult(Vec3.atCenterOf(free.below()).add(0, 0.5, 0), Direction.UP, free.below(), false);
			ItemStack stone = new ItemStack(Items.COBBLESTONE, 8);
			actor.setItemInHand(InteractionHand.MAIN_HAND, stone);
			InteractionResult placed = stone.useOn(new UseOnContext(actor, InteractionHand.MAIN_HAND, face));
			require(!placed.consumesAction() && realm.getBlockState(free).isAir(), "survival placing is refused (" + placed + ")");

			ItemStack bucket = new ItemStack(Items.WATER_BUCKET);
			actor.setItemInHand(InteractionHand.MAIN_HAND, bucket);
			require(!bucket.use(realm, actor, InteractionHand.MAIN_HAND).getResult().consumesAction(), "survival buckets are refused");

			BlockPos blast = realm.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spot.offset(12, 0, 0)).below();
			List<BlockState> before = new ArrayList<>();
			for (BlockPos pos : BlockPos.betweenClosed(blast.offset(-3, -3, -3), blast.offset(3, 3, 3))) before.add(realm.getBlockState(pos));
			realm.explode(null, blast.getX() + 0.5, blast.getY() + 1.0, blast.getZ() + 0.5, 3.0f, Level.ExplosionInteraction.TNT);
			List<BlockState> after = new ArrayList<>();
			for (BlockPos pos : BlockPos.betweenClosed(blast.offset(-3, -3, -3), blast.offset(3, 3, 3))) after.add(realm.getBlockState(pos));
			require(before.equals(after), "a TNT-strength explosion at " + blast + " leaves the terrain whole");

			actor.setGameMode(GameType.CREATIVE);
			ItemStack control = new ItemStack(Items.COBBLESTONE);
			actor.setItemInHand(InteractionHand.MAIN_HAND, control);
			require(control.useOn(new UseOnContext(actor, InteractionHand.MAIN_HAND, face)).consumesAction()
					&& realm.getBlockState(free).is(Blocks.COBBLESTONE), "creative players can still build at " + free);
			require(actor.gameMode.destroyBlock(free) && realm.getBlockState(free).isAir(), "creative players can still break");
			report.add("PROTECTION floor " + floor + ", build tile " + free + ", blast " + blast);

			actor.setGameMode(GameType.SURVIVAL);
			actor.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			actor.teleportTo(realm, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 180, 30);
			// One more refused break so the action-bar warning is fresh for the capture.
			actor.gameMode.destroyBlock(floor);
			serverDone = true;
		} catch (Throwable e) {
			serverFailure = "protection: " + e;
		}
	}

	private void visitBiome(MinecraftServer server, ServerPlayer actor, String id) {
		ServerLevel realm = EclipsedRealm.level(server);
		ResourceLocation key = new ResourceLocation("remnant_bosses", id);
		Pair<BlockPos, Holder<Biome>> found = realm.findClosestBiome3d(holder -> holder.is(key), actor.blockPosition(), 6400, 32, 64);
		if (found == null) {
			report.add("UNFOUND biome " + id + " within 6400 blocks");
			LOG.warn("UNFOUND biome {}", id);
			return;
		}
		BlockPos pos = found.getFirst();
		realm.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
		int y = realm.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
		actor.teleportTo(realm, pos.getX() + 0.5, y + 6, pos.getZ() + 0.5, 30, 12);
		report.add("BIOME " + id + " at " + pos.getX() + " " + y + " " + pos.getZ());
	}

	private void visitCitadel(MinecraftServer server, ServerPlayer actor) {
		ServerLevel realm = EclipsedRealm.level(server);
		// The arena tag also holds the underground Catacombs; look for the Citadel itself.
		Holder<Structure> structure = realm.registryAccess().registryOrThrow(Registries.STRUCTURE)
				.getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE, new ResourceLocation("remnant_bosses", "hollow_citadel")));
		var located = realm.getChunkSource().getGenerator().findNearestMapStructure(realm, HolderSet.direct(structure),
				actor.blockPosition(), 64, false);
		if (located == null) {
			report.add("UNFOUND Hollow Citadel within 64 chunks");
			return;
		}
		BlockPos citadel = located.getFirst();
		for (int cx = -6; cx <= 6; cx++) for (int cz = -6; cz <= 6; cz++) realm.getChunk((citadel.getX() >> 4) + cx, (citadel.getZ() >> 4) + cz);
		int y = realm.getHeight(Heightmap.Types.MOTION_BLOCKING, citadel.getX(), citadel.getZ());
		// The throne hall's altar marks the arena; frame the keep from above its south side.
		BlockPos altar = null;
		for (BlockPos pos : BlockPos.betweenClosed(citadel.offset(-90, y - citadel.getY() - 30, -90), citadel.offset(90, y - citadel.getY() + 40, 90))) {
			if (realm.getBlockState(pos).is(ModBlocks.ANCIENT_ALTAR.get())) {
				altar = pos.immutable();
				break;
			}
		}
		BlockPos focus = altar != null ? altar : new BlockPos(citadel.getX(), y, citadel.getZ());
		report.add(altar != null ? "ALTAR (throne arena) at " + altar : "NO ALTAR found within 90 blocks of the Citadel start");
		actor.getAbilities().flying = true;
		actor.onUpdateAbilities();
		actor.teleportTo(realm, focus.getX() + 0.5, focus.getY() + 26, focus.getZ() - 34.5, 0, 34);
		int mobs = 0;
		for (Entity entity : realm.getEntities((Entity) null, new AABB(citadel).inflate(96), e -> true)) {
			if (BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals("remnants")) mobs++;
		}
		report.add("CITADEL at " + citadel + "; Remnant creatures within 96 blocks: " + mobs);
	}

	/** The Forgotten Observatory in the Lantern Heights, framed from north of its dome altar. */
	private void visitObservatory(MinecraftServer server, ServerPlayer actor) {
		ServerLevel realm = EclipsedRealm.level(server);
		Holder<Structure> structure = realm.registryAccess().registryOrThrow(Registries.STRUCTURE)
				.getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE, new ResourceLocation("remnant_bosses", "forgotten_observatory")));
		var located = realm.getChunkSource().getGenerator().findNearestMapStructure(realm, HolderSet.direct(structure),
				actor.blockPosition(), 100, false);
		if (located == null) {
			report.add("UNFOUND Forgotten Observatory within 100 chunks");
			return;
		}
		BlockPos observatory = located.getFirst();
		for (int cx = -5; cx <= 5; cx++) for (int cz = -5; cz <= 5; cz++) realm.getChunk((observatory.getX() >> 4) + cx, (observatory.getZ() >> 4) + cz);
		int y = realm.getHeight(Heightmap.Types.MOTION_BLOCKING, observatory.getX(), observatory.getZ());
		BlockPos altar = null;
		for (BlockPos pos : BlockPos.betweenClosed(observatory.offset(-60, y - observatory.getY() - 30, -60), observatory.offset(60, y - observatory.getY() + 30, 60))) {
			if (realm.getBlockState(pos).is(ModBlocks.ANCIENT_ALTAR.get())) {
				altar = pos.immutable();
				break;
			}
		}
		BlockPos focus = altar != null ? altar : new BlockPos(observatory.getX(), y, observatory.getZ());
		report.add(altar != null ? "ALTAR (dome arena) at " + altar : "NO ALTAR found within 60 blocks of the Observatory start");
		actor.getAbilities().flying = true;
		actor.onUpdateAbilities();
		actor.teleportTo(realm, focus.getX() + 0.5, focus.getY() + 24, focus.getZ() - 32.5, 0, 34);
		int mobs = 0;
		for (Entity entity : realm.getEntities((Entity) null, new AABB(observatory).inflate(80), e -> true)) {
			if (BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals("remnants")) mobs++;
		}
		report.add("OBSERVATORY at " + observatory + "; Remnant creatures within 80 blocks: " + mobs);
	}

	private void look(Minecraft client, float yaw, float pitch) {
		if (client.player == null) return;
		client.player.setYRot(yaw);
		client.player.setXRot(pitch);
		client.player.yRotO = yaw;
		client.player.xRotO = pitch;
	}

	private void onServer(MinecraftServer server, Runnable task) {
		serverDone = false;
		server.execute(task);
	}

	private void next(int wait) {
		step++;
		waitUntil = ticks + wait;
	}

	private void pass(String message) {
		report.add("PASS " + message);
		LOG.info("PASS {}", message);
	}

	private static void require(boolean condition, String message) {
		if (!condition) throw new IllegalStateException(message);
	}

	private void capture(Minecraft client, String name) {
		String file = name + ".png";
		captures.add(client.gameDirectory.toPath().resolve("screenshots").resolve(file));
		pendingCaptures.incrementAndGet();
		Screenshot.grab(client.gameDirectory, file, client.getMainRenderTarget(), message -> {
			LOG.info("SCREENSHOT {} {}", file, message.getString());
			pendingCaptures.decrementAndGet();
		});
	}

	private void finish(String result) {
		finalResult = result;
		waitUntil = ticks;
	}

	private void finishNow(Minecraft client) {
		finished = true;
		try {
			StringBuilder out = new StringBuilder(finalResult).append('\n');
			for (String line : report) out.append(line).append('\n');
			for (Path path : captures) {
				boolean present = Files.isRegularFile(path) && Files.size(path) > 0;
				out.append(present ? "CAPTURE " : "MISSING ").append(path.toAbsolutePath()).append('\n');
			}
			Path file = client.gameDirectory.toPath().resolve("realm-runtime-report.txt");
			Files.writeString(file, out);
			LOG.info("RUNTIME REPORT {}\n{}", file.toAbsolutePath(), out);
		} catch (Exception error) {
			LOG.error("Could not write runtime report", error);
		}
		client.stop();
	}
}
