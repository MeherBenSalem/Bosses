package com.nightbeam.reel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.nightbeam.reel.mixin.MinecraftInvoker;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Development-only cinematic capture for the promotional reel. Runs a client that opens a fixed world ("ReelWorld"),
 * then takes shot jobs (JSON, {@link Shot}) from {@code runs/reel/reel/jobs/}: it stages each scene with server
 * commands, simulates a preroll, then renders the shot frame by frame under virtual time with the integrated server
 * in lockstep, streaming the frames to ffmpeg and logging every sound the game plays. Nothing here ships.
 */
public final class ReelDirector implements ClientModInitializer {
	private static final Logger LOG = LoggerFactory.getLogger("ReelDirector");
	private static final String WORLD = "ReelWorld";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private enum Phase { BOOT, IDLE, SETTLE, PREROLL, RECORD }

	private static Phase phase = Phase.BOOT;
	private static boolean worldRequested;
	private static int bootFrames;
	private static Path root, jobs, out;
	private static long phaseStart, lastPoll;

	private static Shot shot;
	private static Path jobFile;
	private static List<Shot.Key> keys = List.of(), aim = List.of();
	private static final Map<Integer, List<Shot.Event>> serverEvents = new ConcurrentHashMap<>();
	private static final Map<Integer, List<Shot.Event>> clientEvents = new HashMap<>();
	private static final Map<String, Integer> tagIds = new ConcurrentHashMap<>();
	private static final Set<String> heldTags = ConcurrentHashMap.newKeySet();
	private static int nextTick, frame, totalFrames, holds, totalHolds, warmFrames;
	private static boolean setupDone;
	private static long readySince, setupAt;
	private static double[] steps;
	private static boolean holdNext;
	private static ReelCapture capture;
	private static String faceTag;
	private static double[] faceOffset;
	private static int releaseJumpAt = Integer.MIN_VALUE;
	private static final List<String> commandLog = Collections.synchronizedList(new ArrayList<>());
	private static long recordStartNanos;
	/** Visible sections still waiting for light after the last compile (set by the LevelRenderer mixin). */
	public static volatile int unlitSections;

	@Override
	public void onInitializeClient() {
		LOG.info("DEV ONLY: reel director armed for world {}", WORLD);
	}

	// ---------------------------------------------------------------- frame loop

	/** Head of every frame (render thread), before the timer advances. */
	public static void frameStart(Minecraft mc) {
		try {
			switch (phase) {
				case BOOT -> boot(mc);
				case IDLE -> idle(mc);
				case SETTLE -> settle(mc);
				case PREROLL -> {
					double remaining = -ReelClock.gameTicks;
					if (remaining <= 1e-9) {
						startRecording(mc);
					} else {
						ReelClock.ticksThisFrame = Math.min(10, remaining);
					}
				}
				case RECORD -> {
					if (holdNext || "stills".equals(shot.type)) {
						ReelClock.ticksThisFrame = 0;
					} else {
						ReelClock.ticksThisFrame = steps[frame];
					}
					ReelSounds.videoTime = videoTime();
				}
			}
		} catch (Throwable error) {
			fail(mc, error);
		}
	}

	/** After the frame is drawn into the main target, before it is presented. */
	public static void frameRendered(Minecraft mc) {
		if (phase != Phase.RECORD) return;
		try {
			mc.getToasts().clear();
			if (warmFrames > 0) {
				warmFrames--;
				holdNext = true;
				return;
			}
			if (unlitSections > 0 && holds < 60) {
				holds++;
				totalHolds++;
				holdNext = true;
				return;
			}
			holds = 0;
			holdNext = false;
			capture.grab(mc.getMainRenderTarget());
			frame++;
			if (frame % 15 == 0) status("recording " + shot.name + " frame " + frame + "/" + totalFrames);
			if (frame >= totalFrames) finish(mc, null);
		} catch (Throwable error) {
			fail(mc, error);
		}
	}

	private static double videoTime() {
		return frame / (double) shot.fps;
	}

	// ---------------------------------------------------------------- boot and jobs

	private static void boot(Minecraft mc) throws IOException {
		mc.options.pauseOnLostFocus = false;
		mc.options.onboardAccessibility = false;
		if (mc.level != null && mc.player != null && mc.getSingleplayerServer() != null) {
			MinecraftServer server = mc.getSingleplayerServer();
			String folder = server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString();
			if (!folder.equals(WORLD)) throw new IllegalStateException("refusing to direct world folder " + folder);
			root = mc.gameDirectory.toPath().resolve("reel");
			jobs = root.resolve("jobs");
			out = root.resolve("out");
			Files.createDirectories(jobs.resolve("done"));
			Files.createDirectories(out);
			applyBaseOptions(mc);
			phase = Phase.IDLE;
			status("idle");
			LOG.info("Reel director ready; jobs in {}", jobs.toAbsolutePath());
			return;
		}
		// Wait out the startup resource load: world loading only runs main-thread tasks once the overlay is gone.
		if (worldRequested || mc.level != null || mc.getOverlay() != null || mc.screen == null) return;
		if (++bootFrames < 20) return;
		worldRequested = true;
		if (Files.isDirectory(mc.gameDirectory.toPath().resolve("saves").resolve(WORLD))) {
			LOG.info("Opening {}", WORLD);
			mc.createWorldOpenFlows().openWorld(WORLD, () -> LOG.error("Could not open {}", WORLD));
		} else {
			LOG.info("Creating {}", WORLD);
			GameRules rules = new GameRules();
			rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
			rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
			rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
			rules.getRule(GameRules.RULE_KEEPINVENTORY).set(true, null);
			LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.NORMAL, true, rules,
					WorldDataConfiguration.DEFAULT);
			mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(20261019L, true, false),
					WorldPresets::createNormalWorldDimensions, mc.screen);
		}
	}

	private static void applyBaseOptions(Minecraft mc) {
		Options o = mc.options;
		o.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
		mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
		mc.getToasts().clear();
		o.framerateLimit().set(260);
		o.enableVsync().set(false);
		o.graphicsMode().set(GraphicsStatus.FANCY);
		o.particles().set(ParticleStatus.ALL);
		o.entityShadows().set(true);
		o.ambientOcclusion().set(true);
		o.cloudStatus().set(CloudStatus.FANCY);
		o.entityDistanceScaling().set(2.5);
		o.menuBackgroundBlurriness().set(0);
		o.hideGui = false;
		o.save();
	}

	private static void idle(Minecraft mc) throws IOException {
		long now = System.nanoTime();
		if (now - lastPoll < 400_000_000L) return;
		lastPoll = now;
		Path next;
		try (Stream<Path> files = Files.list(jobs)) {
			next = files.filter(p -> p.toString().endsWith(".json")).sorted().findFirst().orElse(null);
		}
		if (next == null) return;
		jobFile = next;
		shot = GSON.fromJson(Files.readString(next), Shot.class);
		if (shot.name == null) shot.name = next.getFileName().toString().replace(".json", "");
		LOG.info("Job {} ({})", shot.name, shot.type);
		begin(mc);
	}

	/** Stages the shot in real time: options, player, setup commands. */
	private static void begin(Minecraft mc) {
		MinecraftServer server = mc.getSingleplayerServer();
		// A screen a previous shot opened (the journal, the grace menu) would be drawn over this one.
		if (mc.screen != null) mc.setScreen(null);
		commandLog.clear();
		tagIds.clear();
		heldTags.clear();
		serverEvents.clear();
		clientEvents.clear();
		faceTag = null;
		Options o = mc.options;
		o.renderDistance().set(shot.renderDistance);
		o.simulationDistance().set(Math.min(12, Math.max(5, shot.renderDistance)));
		o.gamma().set(shot.gamma);
		o.bobView().set(shot.bobView);
		o.fov().set((int) Math.round(Math.max(30, Math.min(110, shot.fov))));

		keys = shot.keys;
		aim = shot.aim;
		ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(shot.dimension));
		Vec3 start = anchor();
		float[] rot = shot.playerRot != null ? new float[]{(float) shot.playerRot[0], (float) shot.playerRot[1]} : new float[]{0, 0};
		GameType mode = GameType.byName(shot.gamemode, GameType.SPECTATOR);
		server.submit(() -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			ServerLevel level = server.getLevel(dimension);
			if (level == null) throw new IllegalStateException("no dimension " + shot.dimension);
			player.setGameMode(mode);
			if (start != null) player.teleportTo(level, start.x, start.y, start.z, rot[0], rot[1]);
			return null;
		}).join();
		for (Shot.Event event : shot.events) {
			int tick = eventTick(event.t);
			boolean client = event.input != null || event.slot != null || event.face != null || event.banner != null
					|| event.flash != null || event.shake != null || event.closeScreen != null || event.screenCall != null;
			(client ? clientEvents : serverEvents).computeIfAbsent(tick, k -> new ArrayList<>()).add(event);
		}
		if ("commands".equals(shot.type)) {
			for (String cmd : shot.setup) runCommandBlocking(server, cmd);
			if (!shot.scanBlocks.isEmpty() || shot.heightmapStep > 0) server.submit(() -> scan(server.getLevel(dimension))).join();
			finish(mc, null);
			return;
		}
		setupDone = false;
		readySince = 0;
		ReelCamera.hud = shot.hud;
		ReelCamera.hand = shot.hand;
		ReelCamera.hidePlayer = shot.hidePlayer;
		ReelCamera.hudOffset = "boss".equals(shot.hud) ? shot.hudOffset : 0;
		ReelCamera.particleClip = shot.particleClip;
		if (mc.options.guiScale().get() != shot.guiScale) {
			mc.options.guiScale().set(shot.guiScale);
			mc.resizeDisplay();
		}
		ReelCamera.detached = true;
		ReelCamera.active = "path".equals(shot.camera) && !keys.isEmpty();
		ReelCamera.fov = Double.NaN;
		phase = Phase.SETTLE;
		phaseStart = System.nanoTime();
		status("settling " + shot.name);
	}

	/** Where the hidden player waits: playerPos, else the middle of the camera path. */
	private static Vec3 anchor() {
		if (shot.playerPos != null) return new Vec3(shot.playerPos[0], shot.playerPos[1], shot.playerPos[2]);
		double x = 0, y = 0, z = 0;
		int n = 0;
		for (Shot.Key k : keys) {
			if (k.pos == null) continue;
			x += k.pos[0];
			y += k.pos[1];
			z += k.pos[2];
			n++;
		}
		return n == 0 ? null : new Vec3(x / n, y / n, z / n);
	}

	private static boolean chunksReady(Minecraft mc) {
		if (mc.level == null || mc.player == null) return false;
		int r = Math.max(1, Math.min(shot.readyRadius, shot.renderDistance - 1));
		int cx = mc.player.getBlockX() >> 4, cz = mc.player.getBlockZ() >> 4;
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (dx * dx + dz * dz > r * r) continue;
				if (!mc.level.getChunkSource().hasChunk(cx + dx, cz + dz)) return false;
			}
		}
		return true;
	}

	private static void settle(Minecraft mc) {
		ReelSounds.videoTime = 0;
		mc.getToasts().clear();
		followCamera(mc, 0);
		long now = System.nanoTime();
		double waited = (now - phaseStart) / 1e9;
		boolean ready = mc.levelRenderer.hasRenderedAllSections() && chunksReady(mc);
		if (!setupDone) {
			if (waited < shot.settle || (!ready && waited < shot.settle + 180)) return;
			if (!ready) LOG.warn("{}: chunks still loading after {} s; staging anyway", shot.name, Math.round(waited));
			// Entities load after their chunks; give them a moment so the clean-up sees them.
			if (readySince == 0) readySince = now;
			if (now - readySince < 2_000_000_000L) return;
			MinecraftServer server = mc.getSingleplayerServer();
			if (!shot.keepEntities) runCommandBlocking(server, "!discard @e[type=!player,distance=.." + shot.cleanRadius + "]");
			for (String cmd : shot.setup) runCommandBlocking(server, cmd);
			setupDone = true;
			setupAt = now;
			return;
		}
		if (now - setupAt < shot.afterSetup * 1e9) return;
		refreshTags(mc.getSingleplayerServer());
		double prerollTicks = Math.round(Math.max(0, shot.preroll) * 20);
		ReelClock.reset();
		ReelClock.gameTicks = -prerollTicks;
		nextTick = (int) -prerollTicks;
		ReelClock.ticksThisFrame = 0;
		ReelClock.timeControl = true;
		ReelClock.lockstep = true;
		if (prerollTicks > 0) {
			mc.noRender = true;
			phase = Phase.PREROLL;
			status("preroll " + shot.name);
		} else {
			startRecording(mc);
		}
	}

	private static void startRecording(Minecraft mc) {
		try {
			mc.noRender = false;
			ReelClock.gameTicks = 0;
			boolean still = "still".equals(shot.type) || "stills".equals(shot.type);
			totalFrames = "stills".equals(shot.type) ? keys.size() : still ? 1 : Math.max(1, (int) Math.round(shot.duration * shot.fps));
			steps = new double[totalFrames];
			for (int f = 0; f < totalFrames; f++) {
				steps[f] = 20.0 / shot.fps * ReelCamera.value(shot.speed, (f + 0.5) / shot.fps, 1.0);
			}
			frame = 0;
			holds = totalHolds = 0;
			// A few frames at time zero first, so every section and entity is in place for frame one.
			warmFrames = 4;
			holdNext = true;
			var target = mc.getMainRenderTarget();
			Path video = out.resolve(shot.name + ("stills".equals(shot.type) ? "_%02d.png" : still ? ".png" : ".mp4"));
			capture = new ReelCapture(video, target.width, target.height, shot.fps, still, out.resolve(shot.name + ".ffmpeg.log"));
			ReelSounds.open(out.resolve(shot.name + ".sounds.jsonl"));
			ReelSounds.recording = true;
			ReelSounds.videoTime = 0;
			ReelClock.ticksThisFrame = 0;
			recordStartNanos = System.nanoTime();
			phase = Phase.RECORD;
			status("recording " + shot.name);
		} catch (IOException e) {
			fail(mc, e);
		}
	}

	private static void finish(Minecraft mc, Throwable error) {
		ReelSounds.recording = false;
		ReelSounds.close();
		ReelClock.release();
		ReelClock.timeControl = false;
		mc.noRender = false;
		ReelCamera.off();
		releaseKeys(mc);
		Path closing = out == null ? null : out.resolve(shot.name);
		try {
			if (capture != null) capture.close();
		} catch (IOException e) {
			LOG.error("Closing capture", e);
		}
		capture = null;
		MinecraftServer server = mc.getSingleplayerServer();
		if (server != null) {
			for (String cmd : shot.cleanup) runCommandBlocking(server, cmd);
		}
		try {
			JsonObject meta = new JsonObject();
			meta.addProperty("name", shot.name);
			meta.addProperty("frames", frame);
			meta.addProperty("fps", shot.fps);
			meta.addProperty("holds", totalHolds);
			meta.addProperty("seconds", (System.nanoTime() - recordStartNanos) / 1e9);
			meta.add("commands", GSON.toJsonTree(new ArrayList<>(commandLog)));
			if (error != null) {
				StringWriter trace = new StringWriter();
				error.printStackTrace(new PrintWriter(trace));
				meta.addProperty("error", trace.toString());
			}
			Files.writeString(out.resolve(shot.name + ".json"), GSON.toJson(meta));
			Files.writeString(out.resolve(shot.name + (error == null ? ".done" : ".failed")), "");
			Files.move(jobFile, jobs.resolve("done").resolve(jobFile.getFileName()), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			LOG.error("Writing results for {}", closing, e);
		}
		LOG.info("Job {} {} ({} frames)", shot.name, error == null ? "done" : "FAILED", frame);
		phase = Phase.IDLE;
		status("idle");
	}

	private static void fail(Minecraft mc, Throwable error) {
		LOG.error("Reel failure", error);
		if (shot == null || out == null) {
			phase = phase == Phase.BOOT ? Phase.BOOT : Phase.IDLE;
			return;
		}
		finish(mc, error);
	}

	private static void status(String text) {
		if (root == null) return;
		try {
			Files.writeString(root.resolve("status.txt"), text + "\n");
		} catch (IOException ignored) {
		}
	}

	// ---------------------------------------------------------------- ticks

	/** Record-relative tick index at which an event at video time {@code t} fires. */
	private static int eventTick(double t) {
		if (t < 0) return (int) Math.floor(t * 20);
		double ticks = 0;
		int frames = (int) Math.floor(t * shot.fps);
		for (int f = 0; f < frames; f++) ticks += 20.0 / shot.fps * ReelCamera.value(shot.speed, (f + 0.5) / shot.fps, 1.0);
		ticks += (t * shot.fps - frames) * 20.0 / shot.fps * ReelCamera.value(shot.speed, t, 1.0);
		return (int) Math.floor(ticks + 1e-6);
	}

	/** Head of every client tick: in lockstep the server runs this tick first, then client events fire. */
	public static void clientTickStart(Minecraft mc) {
		if (!ReelClock.lockstep || (phase != Phase.PREROLL && phase != Phase.RECORD)) return;
		int tick = nextTick++;
		ReelClock.stepServer(mc, tick);
		try {
			if (tick == releaseJumpAt) mc.options.keyJump.setDown(false);
			List<Shot.Event> due = clientEvents.remove(tick);
			if (due != null) for (Shot.Event event : due) clientEvent(mc, event, tick);
			followCamera(mc, phase == Phase.RECORD ? videoTime() : 0);
		} catch (Throwable error) {
			LOG.error("Client event failed at tick {}", tick, error);
		}
	}

	private static void clientEvent(Minecraft mc, Shot.Event event, int tick) {
		LocalPlayer player = mc.player;
		if (player == null) return;
		if (event.slot != null) player.getInventory().selected = event.slot;
		if (event.face != null) {
			faceTag = event.face.isEmpty() ? null : event.face;
			faceOffset = event.faceOffset;
		}
		if (event.banner != null) {
			com.nightbeam.remnants.client.realm.RealmOverlay.banner(Component.translatable(event.banner),
					event.bannerSub == null ? Component.empty() : Component.translatable(event.bannerSub),
					com.nightbeam.remnants.client.realm.RealmOverlay.Style.valueOf(event.bannerStyle == null ? "AREA" : event.bannerStyle), 0);
		}
		if (Boolean.TRUE.equals(event.closeScreen) && mc.screen != null) mc.setScreen(null);
		if (event.screenCall != null && mc.screen != null) {
			try {
				call(mc.screen, event.screenCall, event.args == null ? List.of() : event.args);
			} catch (ReflectiveOperationException e) {
				LOG.warn("Screen call {} failed", event.screenCall, e);
			}
		}
		if (event.flash != null) com.nightbeam.remnants.client.realm.RealmOverlay.flash(event.flash.floatValue());
		if (event.shake != null) com.nightbeam.remnants.client.realm.ScreenShake.add(event.shake.floatValue());
		if (event.input == null) return;
		Options o = mc.options;
		switch (event.input) {
			case "attack" -> ((MinecraftInvoker) mc).reel$startAttack();
			case "use" -> o.keyUse.setDown(true);
			case "release" -> o.keyUse.setDown(false);
			case "tapuse" -> ((MinecraftInvoker) mc).reel$startUseItem();
			case "jump" -> {
				o.keyJump.setDown(true);
				releaseJumpAt = tick + 2;
			}
			case "forward" -> o.keyUp.setDown(true);
			case "back" -> o.keyDown.setDown(true);
			case "left" -> o.keyLeft.setDown(true);
			case "right" -> o.keyRight.setDown(true);
			case "sprint" -> o.keySprint.setDown(true);
			case "sneak" -> o.keyShift.setDown(true);
			case "stop" -> releaseKeys(mc);
			default -> LOG.warn("Unknown input {}", event.input);
		}
	}

	private static void releaseKeys(Minecraft mc) {
		Options o = mc.options;
		for (var key : List.of(o.keyUp, o.keyDown, o.keyLeft, o.keyRight, o.keySprint, o.keyShift, o.keyJump, o.keyUse, o.keyAttack)) {
			key.setDown(false);
		}
	}

	/** Server thread, head of every tick (after the lockstep permit). */
	public static void serverTick(MinecraftServer server) {
		if (!ReelClock.lockstep || shot == null) return;
		int tick = ReelClock.serverTickIndex;
		try {
			List<Shot.Event> due = serverEvents.remove(tick);
			if (due != null) {
				for (Shot.Event event : due) serverEvent(server, event);
			}
			for (String tag : heldTags) {
				for (Entity entity : tagged(server, tag)) setField(entity, "cooldown", 200);
			}
			if (tick % 10 == 0 || due != null) refreshTags(server);
		} catch (Throwable error) {
			LOG.error("Server event failed at tick {}", tick, error);
		}
	}

	private static void serverEvent(MinecraftServer server, Shot.Event event) throws ReflectiveOperationException {
		if (event.cmd != null) runCommand(server, event.cmd);
		if (event.useOn != null) {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			if (event.useItem != null) {
				var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(event.useItem));
				player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(item));
			}
			var pos = net.minecraft.core.BlockPos.containing(event.useOn[0], event.useOn[1], event.useOn[2]);
			var face = net.minecraft.core.Direction.byName(event.useFace);
			var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos).relative(face, 0.5), face, pos, false);
			var result = player.gameMode.useItemOn(player, player.serverLevel(), player.getMainHandItem(),
					net.minecraft.world.InteractionHand.MAIN_HAND, hit);
			commandLog.add("use " + player.getMainHandItem() + " on " + pos + " -> " + result);
		}
		if (event.tag == null) return;
		ServerPlayer player = server.getPlayerList().getPlayers().get(0);
		for (Entity entity : tagged(server, event.tag)) {
			if (event.hold != null) {
				if (event.hold) heldTags.add(event.tag);
				else heldTags.remove(event.tag);
			}
			if (event.health != null && entity instanceof LivingEntity living) {
				living.setHealth((float) (living.getMaxHealth() * event.health));
			}
			if (event.attack != null) {
				Entity target = event.target == null || "@player".equals(event.target) ? player : first(server, event.target);
				if (target == null) throw new IllegalStateException("no attack target " + event.target);
				if (target instanceof LivingEntity living && entity instanceof net.minecraft.world.entity.Mob mob) mob.setTarget(living);
				call(entity, "startAttack", List.of(event.attack, target));
			}
			if (event.invoke != null) {
				List<Object> args = new ArrayList<>();
				for (Object arg : event.args == null ? List.of() : event.args) {
					if ("@player".equals(arg)) args.add(player);
					else if (arg instanceof String s && s.startsWith("@tag:")) args.add(first(server, s.substring(5)));
					else args.add(arg);
				}
				call(entity, event.invoke, args);
			}
		}
	}

	// ---------------------------------------------------------------- camera

	/** Camera.setup head: in first-person shots, aims the player along the aim keys or at the faced entity. */
	public static void beforeCamera(float partialTick) {
		if (phase != Phase.RECORD && phase != Phase.SETTLE && phase != Phase.PREROLL || shot == null) return;
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) return;
		Vec3 eye = player.getEyePosition(partialTick);
		Float yaw = null, pitch = null;
		if (!aim.isEmpty() && !"path".equals(shot.camera)) {
			List<Shot.Key> resolved = new ArrayList<>();
			for (Shot.Key k : aim) {
				Shot.Key copy = new Shot.Key();
				copy.t = k.t;
				copy.pos = new double[]{eye.x, eye.y, eye.z};
				copy.rot = k.rot;
				copy.look = k.look;
				copy.lookTag = k.lookTag;
				copy.lookOffset = k.lookOffset;
				copy.fov = k.fov;
				copy.roll = k.roll;
				resolved.add(copy);
			}
			ReelCamera.Pose pose = ReelCamera.evaluate(shot, resolved, phase == Phase.RECORD ? videoTime() : 0, k -> lookAt(k.lookTag, k.lookOffset, partialTick));
			yaw = pose.yaw();
			pitch = pose.pitch();
			ReelCamera.fov = pose.fov();
			ReelCamera.roll = pose.roll();
		} else if (faceTag != null) {
			Vec3 at = lookAt(faceTag, faceOffset, partialTick);
			if (at != null) {
				Vec3 d = at.subtract(eye);
				yaw = ReelCamera.yawOf(d);
				pitch = ReelCamera.pitchOf(d);
			}
		}
		if (yaw != null) {
			float y = yaw;
			// Keep the player's yaw continuous so its body does not spin through a wrap.
			float current = player.getYRot();
			y = current + net.minecraft.util.Mth.wrapDegrees(y - current);
			player.setYRot(y);
			player.yRotO = y;
			player.setXRot(pitch);
			player.xRotO = pitch;
			player.setYHeadRot(y);
			player.yHeadRotO = y;
		}
	}

	/** Camera.setup tail: the path camera's pose for this frame. */
	public static void placeCamera(float partialTick) {
		if (shot == null || keys.isEmpty()) return;
		double t = phase == Phase.RECORD ? videoTime() : 0;
		List<Shot.Key> path = "stills".equals(shot.type) ? List.of(keys.get(Math.min(frame, keys.size() - 1))) : keys;
		ReelCamera.Pose pose = ReelCamera.evaluate(shot, path, t, k -> lookAt(k.lookTag, k.lookOffset, partialTick));
		float[] drift = ReelCamera.handheld(shot.handheld, t);
		ReelCamera.x = pose.pos().x;
		ReelCamera.y = pose.pos().y;
		ReelCamera.z = pose.pos().z;
		ReelCamera.yaw = pose.yaw() + drift[0];
		ReelCamera.pitch = pose.pitch() + drift[1];
		ReelCamera.roll = pose.roll() + drift[2];
		ReelCamera.fov = pose.fov();
	}

	private static Vec3 lookAt(String tag, double[] offset, float partialTick) {
		if (tag == null) return null;
		Minecraft mc = Minecraft.getInstance();
		Integer id = tagIds.get(tag);
		Entity entity = id == null || mc.level == null ? null : mc.level.getEntity(id);
		if (entity == null) return null;
		Vec3 base = entity.getPosition(partialTick).add(0, entity.getBbHeight() * 0.6, 0);
		return offset == null ? base : base.add(offset[0], offset[1], offset[2]);
	}

	/** Keeps the hidden camera operator at the camera so chunks and entities around the shot stay loaded. */
	private static void followCamera(Minecraft mc, double t) {
		if (!shot.playerFollowsCamera || !"path".equals(shot.camera) || keys.isEmpty() || mc.player == null) return;
		if (!"spectator".equals(shot.gamemode)) return;
		ReelCamera.Pose pose = ReelCamera.evaluate(shot, keys, t, k -> null);
		mc.player.setPos(pose.pos().x, pose.pos().y - 1.62, pose.pos().z);
	}

	// ---------------------------------------------------------------- server helpers

	/** Lists the positions of the requested blocks in a square around scanCenter (server thread). */
	private static void scan(ServerLevel level) {
		Set<String> wanted = Set.copyOf(shot.scanBlocks);
		int cx = (int) Math.floor(shot.scanCenter[0]), cz = (int) Math.floor(shot.scanCenter[2]), r = shot.scanRadius;
		int minY = Math.max(level.getMinBuildHeight(), shot.scanMinY), maxY = Math.min(level.getMaxBuildHeight() - 1, shot.scanMaxY);
		Map<String, Integer> counts = new HashMap<>();
		net.minecraft.core.BlockPos.MutableBlockPos pos = new net.minecraft.core.BlockPos.MutableBlockPos();
		for (int x = cx - r; x <= cx + r; x++) {
			for (int z = cz - r; z <= cz + r; z++) {
				level.getChunk(x >> 4, z >> 4);
				for (int y = minY; y <= maxY; y++) {
					pos.set(x, y, z);
					String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
					if (!wanted.contains(id)) continue;
					int seen = counts.merge(id, 1, Integer::sum);
					if (seen <= 40) commandLog.add("scan " + id + " " + x + " " + y + " " + z);
				}
			}
		}
		counts.forEach((id, n) -> commandLog.add("scan total " + id + " " + n));
		if (shot.heightmapStep > 0) {
			for (int z = cz - r; z <= cz + r; z += shot.heightmapStep) {
				StringBuilder row = new StringBuilder("hm " + z + " " + (cx - r));
				for (int x = cx - r; x <= cx + r; x += shot.heightmapStep) {
					level.getChunk(x >> 4, z >> 4);
					row.append(' ').append(level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z));
				}
				commandLog.add(row.toString());
			}
		}
	}

	private static void refreshTags(MinecraftServer server) {
		if (server == null) return;
		Runnable scan = () -> {
			for (ServerLevel level : server.getAllLevels()) {
				for (Entity entity : level.getAllEntities()) {
					for (String tag : entity.getTags()) {
						if (tag.startsWith("reel_")) tagIds.put(tag, entity.getId());
					}
				}
			}
		};
		if (server.isSameThread()) scan.run();
		else server.submit(scan).join();
	}

	private static List<Entity> tagged(MinecraftServer server, String tag) {
		List<Entity> found = new ArrayList<>();
		for (ServerLevel level : server.getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				if (entity.getTags().contains(tag)) found.add(entity);
			}
		}
		return found;
	}

	private static Entity first(MinecraftServer server, String tag) {
		List<Entity> found = tagged(server, tag);
		return found.isEmpty() ? null : found.get(0);
	}

	private static void runCommandBlocking(MinecraftServer server, String cmd) {
		server.submit(() -> runCommand(server, cmd)).join();
	}

	private static void runCommand(MinecraftServer server, String cmd) {
		ServerPlayer player = server.getPlayerList().getPlayers().get(0);
		if (cmd.startsWith("!discard ")) {
			// Removes entities without a death: no animation, loot or REMNANT FELLED banner left in the next shot.
			try {
				var selector = net.minecraft.commands.arguments.EntityArgument.entities()
						.parse(new com.mojang.brigadier.StringReader(cmd.substring(9)));
				List<? extends Entity> found = selector.findEntities(player.createCommandSourceStack().withPermission(4).withSuppressedOutput());
				for (Entity entity : found) entity.discard();
				commandLog.add(cmd + " -> discarded " + found.size());
			} catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
				commandLog.add(cmd + " -> " + e.getMessage());
			}
			return;
		}
		CommandSource sink = new CommandSource() {
			@Override
			public void sendSystemMessage(Component component) {
				commandLog.add(cmd + " -> " + component.getString());
			}

			@Override
			public boolean acceptsSuccess() {
				return true;
			}

			@Override
			public boolean acceptsFailure() {
				return true;
			}

			@Override
			public boolean shouldInformAdmins() {
				return false;
			}
		};
		CommandSourceStack source = new CommandSourceStack(sink, player.position(), new Vec2(player.getXRot(), player.getYRot()),
				player.serverLevel(), 4, "Reel", Component.literal("Reel"), server, player);
		server.getCommands().performPrefixedCommand(source, cmd);
	}

	private static void call(Object target, String name, List<Object> args) throws ReflectiveOperationException {
		for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
			for (Method method : type.getDeclaredMethods()) {
				if (!method.getName().equals(name) || method.getParameterCount() != args.size()) continue;
				Object[] values = new Object[args.size()];
				Class<?>[] params = method.getParameterTypes();
				boolean ok = true;
				for (int i = 0; i < params.length && ok; i++) {
					values[i] = coerce(args.get(i), params[i]);
					ok = values[i] != null || !params[i].isPrimitive();
				}
				if (!ok) continue;
				method.setAccessible(true);
				method.invoke(target, values);
				return;
			}
		}
		throw new NoSuchMethodException(target.getClass().getName() + "." + name + "/" + args.size());
	}

	private static Object coerce(Object value, Class<?> type) {
		if (value == null) return null;
		if (type.isInstance(value)) return value;
		if (value instanceof Number n) {
			if (type == int.class || type == Integer.class) return n.intValue();
			if (type == float.class || type == Float.class) return n.floatValue();
			if (type == double.class || type == Double.class) return n.doubleValue();
			if (type == long.class || type == Long.class) return n.longValue();
		}
		if (type == String.class) return String.valueOf(value);
		if (type.isEnum() && value instanceof String name) {
			for (Object constant : type.getEnumConstants()) if (((Enum<?>) constant).name().equals(name)) return constant;
		}
		if (type == boolean.class && value instanceof Boolean b) return b;
		if (type == Vec3.class) {
			if (value instanceof Entity e) return e.position();
			if (value instanceof List<?> l && l.size() == 3) {
				return new Vec3(((Number) l.get(0)).doubleValue(), ((Number) l.get(1)).doubleValue(), ((Number) l.get(2)).doubleValue());
			}
		}
		return null;
	}

	private static void setField(Object target, String name, int value) {
		for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
			try {
				Field field = type.getDeclaredField(name);
				field.setAccessible(true);
				if (field.getType() == int.class) field.setInt(target, value);
				return;
			} catch (NoSuchFieldException ignored) {
			} catch (IllegalAccessException e) {
				return;
			}
		}
	}
}
