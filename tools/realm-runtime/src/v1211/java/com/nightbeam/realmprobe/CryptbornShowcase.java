package com.nightbeam.realmprobe;

import com.nightbeam.remnants.entity.CatacombRatEntity;
import com.nightbeam.remnants.entity.CryptLurkerEntity;
import com.nightbeam.remnants.init.ModBlocks;
import com.nightbeam.remnants.realm.EclipsedRealm;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Walkthrough beat for two Cryptborn behaviours that each failed for part of the population, on the creature stage above
 * the arrival shrine:
 * <ol>
 * <li>a buried Crypt Lurker is still invisible, on the server and on the client, 40 ticks after it was buried (vanilla
 * rewrites the shared invisible flag from potion effects on the first tick) while the player, briefly in survival under
 * Resistance V, stands 10 blocks away; at 4.5 blocks it emerges and is seen;</li>
 * <li>four Catacomb Rats beside a torch in a dark, roofed room all scurry into the dark. They are summoned together, so
 * their entity ids cover both parities: vanilla tries goal start conditions every other tick with a fixed parity per
 * mob, which once kept every other rat from ever fleeing.</li>
 * </ol>
 * Afterwards the player is back in creative at the arrival grace.
 */
final class CryptbornShowcase {
	/** Player eye height above the stage. */
	private static final double EYE = 1.62;
	/** The catacomb_rat flee_light_level default: rats leave any block this bright. */
	private static final int FLEE_LIGHT = 10;
	/** Only the flee-light goal moves a rat this fast (it runs at 1.4; strolling is 0.7 and chasing 1.0). */
	private static final double FLEE_SPEED = 1.3;
	/** The torch in the dark room, relative to the stage origin; the room's inside spans x -7..7, z -3..13, 4 high. */
	private static final BlockPos TORCH = new BlockPos(0, 0, 5);
	private static final int[][] BESIDE_TORCH = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

	private final List<Entity> shown = new ArrayList<>();
	/** Rat id to the ticks it took to start fleeing, and to reach a block darker than {@link #FLEE_LIGHT}. */
	private final Map<Integer, Integer> fledAfter = new LinkedHashMap<>(), darkAfter = new LinkedHashMap<>();
	private int phase, wait, watched, woke;
	private volatile String failure;
	private boolean finished, staged, captured;
	private String startLight = "";
	private BlockPos stage;
	private volatile CryptLurkerEntity lurker;
	private volatile List<CatacombRatEntity> rats = List.of();

	boolean finished() {
		return finished;
	}

	/** One client tick of the beat; {@code grace} is the arrival grace the walkthrough departs from. */
	void tick(Minecraft client, MinecraftServer server, ServerPlayer actor, BlockPos grace, Consumer<String> capture,
			Consumer<String> pass, Consumer<String> note, CreatureShowcase.Look look) {
		if (failure != null) throw new IllegalStateException("cryptborn showcase: " + failure);
		if (wait > 0) {
			wait--;
			return;
		}
		switch (phase) {
			case 0 -> {
				stage = grace.offset(0, 60, -24);
				server.execute(() -> run(() -> buildStage(server)));
				phase = 1;
				wait = 20;
			}
			case 1 -> buriedLurker(client, server, actor, capture, pass, note, look);
			case 2 -> ratsFleeLight(client, server, actor, capture, pass, note, look);
			case 3 -> {
				server.execute(() -> run(() -> {
					clear();
					darkRoom(server, false);
					actor.teleportTo(EclipsedRealm.level(server), grace.getX() + 0.5, grace.getY(), grace.getZ() + 1.5, 180, 0);
				}));
				phase = 4;
				wait = 40;
			}
			default -> finished = true;
		}
	}

	/**
	 * A buried lurker stays unseen while the player stands 10 blocks away; at 4.5 blocks it emerges. The player is in
	 * survival for this beat (buried lurkers ignore creative players) under Resistance V, so nothing hurts it.
	 */
	private void buriedLurker(Minecraft client, MinecraftServer server, ServerPlayer actor, Consumer<String> capture,
			Consumer<String> pass, Consumer<String> note, CreatureShowcase.Look look) {
		if (!staged) {
			float pitch = pitch(0.4, 10);
			server.execute(() -> run(() -> {
				CryptLurkerEntity mob = (CryptLurkerEntity) spawn(server, "crypt_lurker", stage.getX() + 0.5, stage.getZ() + 0.5, 0, true);
				mob.setBuried(true);
				lurker = mob;
				actor.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 4, false, false));
				actor.getAbilities().flying = false;
				actor.setGameMode(GameType.SURVIVAL);
				camera(server, actor, 10, pitch);
			}));
			look.at(client, 180, pitch);
			staged = true;
			captured = false;
			watched = 0;
			wait = 40;
			return;
		}
		CryptLurkerEntity mob = lurker;
		if (mob == null) return;
		watched++;
		Entity copy = client.level == null ? null : client.level.getEntity(mob.getId());
		if (watched == 1) {
			boolean unseen = mob.isBuried() && mob.isInvisible() && copy != null && copy.isInvisible() && mob.getTarget() == null;
			capture.accept("realm-16-crypt_lurker-buried");
			String line = "crypt_lurker " + mob.tickCount + " ticks after burial with a survival player 10 blocks away: buried="
					+ mob.isBuried() + ", invisible on the server=" + mob.isInvisible() + ", on the client="
					+ (copy == null ? "no client copy" : copy.isInvisible()) + ", target=" + mob.getTarget();
			if (unseen) pass.accept("BURIED " + line);
			else note.accept("NOT HIDDEN " + line);
			float pitch = pitch(0.4, 4.5);
			server.execute(() -> run(() -> camera(server, actor, 4.5, pitch)));
			look.at(client, 180, pitch);
			return;
		}
		if (!captured) {
			boolean emerged = "emerge".equals(mob.action()) && mob.actionAge() >= 12 || "pounce".equals(mob.action());
			if (!emerged && watched <= 200) return;
			capture.accept("realm-16-crypt_lurker-emerge");
			captured = true;
			boolean seen = copy != null && !copy.isInvisible();
			String line = "crypt_lurker woke at 4.5 blocks: buried=" + mob.isBuried() + ", action " + mob.action() + " age "
					+ mob.actionAge() + ", visible on the client=" + seen + ", target "
					+ (mob.getTarget() == actor ? "the player" : String.valueOf(mob.getTarget()));
			if (emerged && !mob.isBuried() && seen && mob.getTarget() == actor) pass.accept("EMERGE " + line);
			else note.accept("NO EMERGE " + line);
			return;
		}
		server.execute(() -> run(() -> {
			clear();
			actor.removeEffect(MobEffects.DAMAGE_RESISTANCE);
			actor.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
			actor.setGameMode(GameType.CREATIVE);
			actor.getAbilities().flying = true;
			actor.onUpdateAbilities();
		}));
		staged = false;
		phase = 2;
		wait = 10;
	}

	/**
	 * Four still rats are summoned beside a torch in a roofed room; once captured they are given their AI and each one
	 * must run off at flee speed and reach a block darker than light {@value #FLEE_LIGHT} within 300 ticks. Reaching the
	 * dark alone is not enough: a rat whose flee goal never starts still strolls into it sooner or later. Night Vision
	 * keeps the room readable.
	 */
	private void ratsFleeLight(Minecraft client, MinecraftServer server, ServerPlayer actor, Consumer<String> capture,
			Consumer<String> pass, Consumer<String> note, CreatureShowcase.Look look) {
		ServerLevel realm = EclipsedRealm.level(server);
		if (!staged) {
			float pitch = pitch(0.3, 7.5);
			server.execute(() -> run(() -> {
				darkRoom(server, true);
				actor.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0, false, false));
				camera(server, actor, TORCH.getZ() + 7, pitch);
			}));
			look.at(client, 180, pitch);
			staged = true;
			captured = false;
			watched = 0;
			fledAfter.clear();
			darkAfter.clear();
			// Lets the roof shut the sky light out before anything is measured.
			wait = 60;
			return;
		}
		watched++;
		if (watched == 1) {
			server.execute(() -> run(() -> {
				List<CatacombRatEntity> group = new ArrayList<>();
				for (int[] offset : BESIDE_TORCH) {
					group.add((CatacombRatEntity) spawn(server, "catacomb_rat", stage.getX() + TORCH.getX() + offset[0] + 0.5,
							stage.getZ() + TORCH.getZ() + offset[1] + 0.5, 180, false));
				}
				rats = group;
			}));
			wait = 20;
			return;
		}
		List<CatacombRatEntity> group = rats;
		if (group.isEmpty()) return;
		if (!captured) {
			capture.accept("realm-17-catacomb_rat-torch");
			captured = true;
			StringBuilder light = new StringBuilder();
			for (CatacombRatEntity rat : group) light.append(light.isEmpty() ? "" : ", ").append(realm.getMaxLocalRawBrightness(rat.blockPosition()));
			startLight = light + " (room corner " + realm.getMaxLocalRawBrightness(stage.offset(-6, 0, -2)) + ")";
			server.execute(() -> run(() -> {
				for (CatacombRatEntity rat : group) rat.setNoAi(false);
			}));
			woke = watched;
			return;
		}
		for (CatacombRatEntity rat : group) {
			if (!fledAfter.containsKey(rat.getId()) && rat.getMoveControl().getSpeedModifier() > FLEE_SPEED) {
				fledAfter.put(rat.getId(), watched - woke);
			}
			if (!darkAfter.containsKey(rat.getId()) && realm.getMaxLocalRawBrightness(rat.blockPosition()) < FLEE_LIGHT) {
				darkAfter.put(rat.getId(), watched - woke);
			}
		}
		if ((fledAfter.size() < group.size() || darkAfter.size() < group.size()) && watched - woke < 300) return;
		capture.accept("realm-17-catacomb_rat-fled");
		StringBuilder rows = new StringBuilder();
		int even = 0;
		for (CatacombRatEntity rat : group) {
			if (rat.getId() % 2 == 0) even++;
			Integer fled = fledAfter.get(rat.getId()), dark = darkAfter.get(rat.getId());
			rows.append(rows.isEmpty() ? "" : "; ").append("id ").append(rat.getId())
					.append(fled == null ? " never ran at flee speed" : " fled after " + fled + " ticks")
					.append(dark == null ? ", stayed in light " + realm.getMaxLocalRawBrightness(rat.blockPosition()) : ", dark after " + dark + " ticks");
		}
		String line = "catacomb_rat x" + group.size() + " (" + even + " even ids, " + (group.size() - even) + " odd) beside a torch, light "
				+ startLight + ": " + rows;
		boolean bothParities = even > 0 && even < group.size();
		if (fledAfter.size() == group.size() && darkAfter.size() == group.size() && bothParities) pass.accept("FLEE LIGHT " + line);
		else note.accept("NO FLEE " + line);
		server.execute(() -> run(() -> {
			clear();
			darkRoom(server, false);
			actor.removeEffect(MobEffects.NIGHT_VISION);
		}));
		staged = false;
		phase = 3;
		wait = 10;
	}

	private static float pitch(double aimHeight, double distance) {
		return (float) Math.toDegrees(Math.atan2(EYE - aimHeight, distance));
	}

	private void run(Runnable task) {
		try {
			task.run();
		} catch (Throwable e) {
			failure = e.toString();
		}
	}

	/** The creature stage's 17 x 19 slab of eclipse bricks in the open air (CreatureShowcase builds the same one). */
	private void buildStage(MinecraftServer server) {
		ServerLevel realm = EclipsedRealm.level(server);
		for (int x = -8; x <= 8; x++) {
			for (int z = -4; z <= 14; z++) {
				realm.setBlock(stage.offset(x, -1, z), ModBlocks.ECLIPSE_STONE_BRICKS.get().defaultBlockState(), 2);
				for (int y = 0; y < 6; y++) realm.setBlock(stage.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
			}
		}
	}

	/** Walls the stage in under a roof with one torch inside ({@code build}), or takes all of it down again. */
	private void darkRoom(MinecraftServer server, boolean build) {
		ServerLevel realm = EclipsedRealm.level(server);
		BlockState brick = build ? ModBlocks.ECLIPSE_STONE_BRICKS.get().defaultBlockState() : Blocks.AIR.defaultBlockState();
		for (int x = -8; x <= 8; x++) {
			for (int z = -4; z <= 14; z++) {
				boolean wall = x == -8 || x == 8 || z == -4 || z == 14;
				for (int y = 0; y <= 4; y++) {
					if (wall || y == 4) realm.setBlock(stage.offset(x, y, z), brick, 2);
				}
			}
		}
		realm.setBlock(stage.offset(TORCH), build ? Blocks.TORCH.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
	}

	/** Summons a remnants: creature at x/z on the stage facing {@code yaw}; still unless {@code ai}. */
	private Entity spawn(MinecraftServer server, String path, double x, double z, float yaw, boolean ai) {
		ServerLevel realm = EclipsedRealm.level(server);
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath("remnants", path);
		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
		Entity entity = type.create(realm);
		if (entity == null) throw new IllegalStateException("cannot create " + id);
		entity.moveTo(x, stage.getY(), z, yaw, 0);
		entity.setYHeadRot(yaw);
		if (entity instanceof Mob mob) {
			mob.setYBodyRot(yaw);
			mob.setNoAi(!ai);
			mob.setPersistenceRequired();
		}
		realm.addFreshEntity(entity);
		shown.add(entity);
		return entity;
	}

	/** Stands the player {@code distance} south of the stage origin, looking north. */
	private void camera(MinecraftServer server, ServerPlayer actor, double distance, float pitch) {
		actor.teleportTo(EclipsedRealm.level(server), stage.getX() + 0.5, stage.getY(), stage.getZ() + 0.5 + distance, 180, pitch);
	}

	private void clear() {
		for (Entity entity : shown) entity.discard();
		shown.clear();
		lurker = null;
		rats = List.of();
	}
}
