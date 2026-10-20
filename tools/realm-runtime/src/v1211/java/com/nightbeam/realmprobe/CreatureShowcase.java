package com.nightbeam.realmprobe;

import com.nightbeam.remnants.entity.LegionEntity;
import com.nightbeam.remnants.entity.RelicStalkerEntity;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Walkthrough beat for the creature families that roam the Realm ahead of their boss slices (the Ashbound Constructs
 * and the Corrupted Relics), on a stage in the open air above the arrival shrine:
 * <ol>
 * <li>each new creature is summoned still in front of the camera and captured, then all six side by side;</li>
 * <li>each one fights a stationary villager: the capture is taken on the attack's action beat and the villager's
 * health before and after is reported;</li>
 * <li>an Ember Servitor welds a damaged Ashbound Crawler: the crawler's health before and after is reported;</li>
 * <li>a hidden Relic Stalker waits unseen until the player (briefly in survival, under Resistance V) walks within
 * range, then ambushes it.</li>
 * </ol>
 * Otherwise the player stays in creative, so nothing attacks it; afterwards it returns to the arrival grace to depart.
 */
final class CreatureShowcase {
	/** Creature, its height above the stage (floaters hover), its own height, and the camera distance that frames it. */
	private record Pose(String id, double lift, double height, double distance) {
	}

	/** A fight: the creature, the gap to its dummy, and the action ages to capture (each attack's action beat). */
	private record Fight(Pose pose, double gap, Map<String, Integer> beats) {
	}

	private static final Pose CRAWLER = new Pose("ashbound_crawler", 0, 0.8, 2.6), SERVITOR = new Pose("ember_servitor", 2.5, 1.3, 4.5),
			GUARDIAN = new Pose("rusted_guardian", 0, 2.8, 5.0), SHARDLING = new Pose("shardling", 1.6, 1.0, 3.2),
			VESSEL = new Pose("ancient_vessel", 0, 1.3, 3.0), STALKER = new Pose("relic_stalker", 0, 2.6, 4.8);
	private static final List<Pose> CREATURES = List.of(CRAWLER, SERVITOR, GUARDIAN, SHARDLING, VESSEL, STALKER);
	private static final List<Fight> FIGHTS = List.of(
			new Fight(CRAWLER, 2.0, Map.of("scorch", 12)),
			new Fight(SERVITOR, 6.0, Map.of("shoot", 11)),
			new Fight(GUARDIAN, 2.6, Map.of("smash", 26, "sweep", 18)),
			new Fight(SHARDLING, 8.0, Map.of("shoot", 9)),
			new Fight(VESSEL, 3.0, Map.of("pulse", 14)),
			// Wide enough that the stalker is still 2.5-7 blocks away when its first cooldown ends: it ambushes.
			new Fight(STALKER, 9.0, Map.of("ambush", 16, "slash", 10)));
	/** Player eye height above the stage. */
	private static final double EYE = 1.62;

	interface Look {
		void at(Minecraft client, float yaw, float pitch);
	}

	private final List<Entity> shown = new ArrayList<>();
	private int phase, index, wait, watched;
	private volatile boolean staged;
	private volatile String failure;
	private boolean finished, captured;
	private String capturedAction;
	private float before;
	private BlockPos stage;
	private volatile Mob actor0;
	private volatile LivingEntity dummy;

	boolean finished() {
		return finished;
	}

	/** One client tick of the showcase; {@code grace} is the arrival grace the walkthrough departs from. */
	void tick(Minecraft client, MinecraftServer server, ServerPlayer actor, BlockPos grace, Consumer<String> capture,
			Consumer<String> pass, Consumer<String> note, Look look) {
		if (failure != null) throw new IllegalStateException("creature showcase: " + failure);
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
			case 1 -> portraits(client, server, actor, capture, pass, look);
			case 2 -> fights(client, server, actor, capture, pass, note, look);
			case 3 -> repair(client, server, actor, capture, pass, note, look);
			case 4 -> hiddenStalker(client, server, actor, capture, pass, note, look);
			case 5 -> {
				server.execute(() -> run(() -> {
					clear();
					actor.teleportTo(EclipsedRealm.level(server), grace.getX() + 0.5, grace.getY(), grace.getZ() + 1.5, 180, 0);
				}));
				phase = 6;
				wait = 40;
			}
			default -> finished = true;
		}
	}

	/** Each creature still, facing the camera, then all six in a row. */
	private void portraits(Minecraft client, MinecraftServer server, ServerPlayer actor, Consumer<String> capture, Consumer<String> pass,
			Look look) {
		boolean lineup = index == CREATURES.size();
		if (!staged) {
			List<Pose> poses = lineup ? CREATURES : List.of(CREATURES.get(index));
			double distance = lineup ? 10 : CREATURES.get(index).distance();
			double aim = lineup ? 1.2 : CREATURES.get(index).lift() + CREATURES.get(index).height() * 0.5;
			float pitch = pitch(aim, distance);
			server.execute(() -> run(() -> {
				for (int i = 0; i < poses.size(); i++) {
					double x = stage.getX() + 0.5 + (i - (poses.size() - 1) / 2.0) * 2.6;
					spawn(server, poses.get(i), x, stage.getZ() + 0.5, 0, false);
				}
				camera(server, actor, stage.getX() + 0.5, distance, pitch);
			}));
			look.at(client, 180, pitch);
			staged = true;
			wait = 45;
			return;
		}
		String name = lineup ? "families-lineup" : CREATURES.get(index).id();
		capture.accept("realm-12-" + name);
		pass.accept(lineup ? "showcase: all six Ashbound and Relic creatures side by side"
				: "showcase: " + name + " summoned in the Realm and rendered");
		server.execute(() -> run(this::clear));
		staged = false;
		wait = 8;
		if (++index > CREATURES.size()) {
			phase = 2;
			index = 0;
		}
	}

	/** Each creature, with its AI, against a stationary villager; captured on the action beat. */
	private void fights(Minecraft client, MinecraftServer server, ServerPlayer actor, Consumer<String> capture, Consumer<String> pass,
			Consumer<String> note, Look look) {
		if (index >= FIGHTS.size()) {
			phase = 3;
			index = 0;
			return;
		}
		Fight fight = FIGHTS.get(index);
		if (!staged) {
			double reach = fight.gap() / 2;
			float pitch = pitch(fight.pose().lift() + fight.pose().height() * 0.5, fight.pose().distance() + reach + 1.5);
			server.execute(() -> run(() -> {
				Mob mob = (Mob) spawn(server, fight.pose(), stage.getX() + 0.5 - reach, stage.getZ() + 0.5, -90, true);
				LivingEntity villager = (LivingEntity) spawn(server, new Pose("minecraft:villager", 0, 1.9, 0),
						stage.getX() + 0.5 + reach, stage.getZ() + 0.5, 90, false);
				mob.setTarget(villager);
				actor0 = mob;
				dummy = villager;
				before = villager.getHealth();
				camera(server, actor, stage.getX() + 0.5, fight.pose().distance() + reach + 1.5, pitch);
			}));
			look.at(client, 180, pitch);
			staged = true;
			captured = false;
			watched = 0;
			wait = 4;
			return;
		}
		Mob mob = actor0;
		LivingEntity villager = dummy;
		if (mob == null || villager == null) return;
		watched++;
		if (!captured) {
			String action = mob instanceof LegionEntity legion ? legion.action() : "";
			int age = mob instanceof LegionEntity legion ? legion.actionAge() : 0;
			boolean beat = fight.beats().containsKey(action) && age >= fight.beats().get(action);
			if (beat || watched > 300) {
				capture.accept("realm-13-" + fight.pose().id() + "-attack");
				captured = true;
				capturedAction = beat ? action + " at age " + age : "no attack within 300 ticks";
				watched = 0;
			}
			return;
		}
		if (watched < 30) return;
		float after = villager.isAlive() ? villager.getHealth() : 0;
		String line = "COMBAT " + fight.pose().id() + ": captured " + capturedAction + "; villager " + before + " -> " + after;
		if (after < before) pass.accept(line);
		else note.accept("NO HIT " + line);
		server.execute(() -> run(this::clear));
		staged = false;
		wait = 6;
		index++;
	}

	/** An Ember Servitor finds a damaged Ashbound Crawler and welds it. */
	private void repair(Minecraft client, MinecraftServer server, ServerPlayer actor, Consumer<String> capture, Consumer<String> pass,
			Consumer<String> note, Look look) {
		if (!staged) {
			float pitch = pitch(1.4, 6.5);
			server.execute(() -> run(() -> {
				Mob crawler = (Mob) spawn(server, CRAWLER, stage.getX() + 0.5 + 1.5, stage.getZ() + 0.5, 90, false);
				Mob servitor = (Mob) spawn(server, SERVITOR, stage.getX() + 0.5 - 3.5, stage.getZ() + 0.5, -90, true);
				actor0 = servitor;
				dummy = crawler;
				camera(server, actor, stage.getX() + 0.5, 6.5, pitch);
			}));
			look.at(client, 180, pitch);
			staged = true;
			captured = false;
			watched = 0;
			wait = 4;
			return;
		}
		Mob servitor = actor0;
		LivingEntity crawler = dummy;
		if (servitor == null || crawler == null) return;
		watched++;
		if (watched == 5) {
			// The Legion body restores full health when it first configures itself: hurt the crawler after that.
			server.execute(() -> run(() -> {
				crawler.setHealth(crawler.getMaxHealth() * 0.4f);
				before = crawler.getHealth();
			}));
			return;
		}
		if (watched < 8) return;
		if (!captured) {
			boolean welding = servitor instanceof LegionEntity legion && "repair".equals(legion.action()) && legion.actionAge() >= 8;
			if (welding || watched > 400) {
				capture.accept("realm-14-ember_servitor-repair");
				captured = true;
				capturedAction = welding ? "repair loop" : "no repair within 400 ticks";
				wait = 80;
			}
			return;
		}
		float after = crawler.getHealth();
		String line = "REPAIR ember_servitor on ashbound_crawler: captured " + capturedAction + "; crawler " + before + " -> " + after;
		if (after > before) pass.accept(line);
		else note.accept("NO REPAIR " + line);
		server.execute(() -> run(this::clear));
		staged = false;
		phase = 4;
	}

	/**
	 * A hidden stalker stays unseen while the player stands 10 blocks away; at 5.5 blocks it wakes and ambushes. The
	 * player is in survival for this beat (hidden stalkers ignore creative players) under Resistance V, so nothing hurts it.
	 */
	private void hiddenStalker(Minecraft client, MinecraftServer server, ServerPlayer actor, Consumer<String> capture,
			Consumer<String> pass, Consumer<String> note, Look look) {
		if (!staged) {
			float pitch = pitch(1.3, 10);
			server.execute(() -> run(() -> {
				RelicStalkerEntity stalker = (RelicStalkerEntity) spawn(server, STALKER, stage.getX() + 0.5, stage.getZ() + 0.5, 0, true);
				stalker.setHidden(true);
				actor0 = stalker;
				actor.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 4, false, false));
				actor.getAbilities().flying = false;
				actor.setGameMode(GameType.SURVIVAL);
				camera(server, actor, stage.getX() + 0.5, 10, pitch);
			}));
			look.at(client, 180, pitch);
			staged = true;
			captured = false;
			watched = 0;
			wait = 40;
			return;
		}
		if (!(actor0 instanceof RelicStalkerEntity stalker)) return;
		watched++;
		if (watched == 1) {
			boolean unseen = stalker.isHidden() && stalker.isInvisible() && stalker.getTarget() == null;
			capture.accept("realm-15-relic_stalker-hidden");
			if (unseen) pass.accept("AMBUSH relic_stalker stays hidden and invisible with a survival player 10 blocks away");
			else note.accept("NOT HIDDEN relic_stalker hidden=" + stalker.isHidden() + " target=" + stalker.getTarget());
			float pitch = pitch(1.3, 5.5);
			server.execute(() -> run(() -> camera(server, actor, stage.getX() + 0.5, 5.5, pitch)));
			look.at(client, 180, pitch);
			return;
		}
		if (!captured) {
			boolean ambush = "ambush".equals(stalker.action()) && stalker.actionAge() >= 16;
			if (ambush || watched > 200) {
				capture.accept("realm-15-relic_stalker-ambush");
				captured = true;
				String line = "AMBUSH relic_stalker woke at 5.5 blocks: hidden=" + stalker.isHidden() + ", action " + stalker.action()
						+ " age " + stalker.actionAge() + ", target " + (stalker.getTarget() == actor ? "the player" : String.valueOf(stalker.getTarget()));
				if (ambush && !stalker.isHidden() && stalker.getTarget() == actor) pass.accept(line);
				else note.accept("NO AMBUSH " + line);
			}
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
		phase = 5;
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

	/** A 17 x 17 slab of eclipse bricks in the open air, so the dusk sky frames every creature. */
	private void buildStage(MinecraftServer server) {
		ServerLevel realm = EclipsedRealm.level(server);
		for (int x = -8; x <= 8; x++) {
			for (int z = -4; z <= 14; z++) {
				realm.setBlock(stage.offset(x, -1, z), ModBlocks.ECLIPSE_STONE_BRICKS.get().defaultBlockState(), 2);
				for (int y = 0; y < 6; y++) realm.setBlock(stage.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
			}
		}
	}

	/** Summons {@code pose} (a remnants: id, or a full id) at x/z on the stage facing {@code yaw}; still unless {@code ai}. */
	private Entity spawn(MinecraftServer server, Pose pose, double x, double z, float yaw, boolean ai) {
		ServerLevel realm = EclipsedRealm.level(server);
		ResourceLocation id = pose.id().contains(":") ? ResourceLocation.parse(pose.id())
				: ResourceLocation.fromNamespaceAndPath("remnants", pose.id());
		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
		Entity entity = type.create(realm);
		if (entity == null) throw new IllegalStateException("cannot create " + id);
		entity.moveTo(x, stage.getY() + pose.lift(), z, yaw, 0);
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

	/** Stands the player {@code distance} south of the stage line at x, looking north. */
	private void camera(MinecraftServer server, ServerPlayer actor, double x, double distance, float pitch) {
		actor.teleportTo(EclipsedRealm.level(server), x, stage.getY(), stage.getZ() + 0.5 + distance, 180, pitch);
	}

	private void clear() {
		for (Entity entity : shown) entity.discard();
		shown.clear();
		actor0 = null;
		dummy = null;
	}
}
