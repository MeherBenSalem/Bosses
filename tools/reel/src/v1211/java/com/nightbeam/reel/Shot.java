package com.nightbeam.reel;

import java.util.ArrayList;
import java.util.List;

/**
 * One capture job, read from a JSON file in {@code runs/reel/jobs/}. Times are seconds of output video from the first
 * captured frame; negative times happen during the preroll, before anything is captured.
 */
public final class Shot {
	/** "record" (default), "still" (one PNG), or "commands" (run setup only and report command output). */
	public String type = "record";
	public String name;
	public int fps = 30;
	public double duration = 3;
	/** Game seconds simulated (not rendered) before the first frame, so AI and particles are already in motion. */
	public double preroll = 0;
	/** Real seconds to wait after setup for chunks to arrive and light. */
	public double settle = 3;
	public String dimension = "remnant_bosses:eclipsed_realm";
	/** Run once the area around the player is loaded (after {@link #settle}), right before the preroll. */
	public List<String> setup = new ArrayList<>();
	/** Unless set, every non-player entity within {@link #cleanRadius} of the player is discarded before setup. */
	public boolean keepEntities = false;
	public int cleanRadius = 96;
	/** Real seconds between setup and the preroll, for new entities to reach the client. */
	public double afterSetup = 1.5;
	/** "commands" jobs: block ids to find around scanCenter (in {@link #dimension}), reported in the job's JSON. */
	public List<String> scanBlocks = new ArrayList<>();
	public double[] scanCenter;
	public int scanRadius = 64;
	public int scanMinY = -64;
	public int scanMaxY = 320;
	/** When positive, also report the surface height (no leaves) every this many blocks across the scan square. */
	public int heightmapStep = 0;
	public List<String> cleanup = new ArrayList<>();

	public int renderDistance = 12;
	public double fov = 70;
	public double gamma = 0.5;
	/** "none", "boss" (boss bars and the mod's titles only) or "full". */
	public String hud = "none";
	public boolean hand = false;
	public int guiScale = 4;
	/** GUI pixels the HUD is pushed down in "boss" mode, clear of the phone app's own top bar. */
	public int hudOffset = 52;
	public double particleClip = 1.6;
	public boolean bobView = false;
	public boolean hidePlayer = true;
	/** Scale of the hand-held drift added to path cameras, in degrees. */
	public double handheld = 0;

	/** The local player at setup: game mode, position and rotation (yaw, pitch). */
	public String gamemode = "spectator";
	public double[] playerPos;
	public double[] playerRot;
	/** Follow the camera with the (hidden) player; otherwise it waits at playerPos, or the middle of the camera path. */
	public boolean playerFollowsCamera = false;
	/** Chunks around the player that must be on the client before the first frame. */
	public int readyRadius = 8;

	/** "path": a keyframed camera; "player": first person through the player's eyes, aimed by {@link #aim}. */
	public String camera = "path";
	public String timing = "smooth";
	public List<Key> keys = new ArrayList<>();
	/** First-person aim keys (camera "player"): rot or look/lookTag, same interpolation as path keys. */
	public List<Key> aim = new ArrayList<>();
	/** Game speed over the shot: 1 is real time, 0.25 is quarter-speed slow motion. */
	public List<Value> speed = new ArrayList<>();
	public List<Event> events = new ArrayList<>();

	public static final class Key {
		public double t;
		public double[] pos;
		/** Yaw, pitch and optional roll in degrees. */
		public double[] rot;
		public double[] look;
		public String lookTag;
		public double[] lookOffset;
		public Double roll;
		public Double fov;
	}

	public static final class Value {
		public double t;
		public double v;
	}

	public static final class Event {
		public double t;
		/** A command run on the server as an operator at the player's position. */
		public String cmd;
		/** Reflective call on every entity carrying {@link #tag}: method name and arguments. */
		public String invoke;
		public List<Object> args;
		public String tag;
		/** Force a boss attack by name (bosses with startAttack(String, LivingEntity)); target tag or "@player". */
		public String attack;
		public String target;
		/** Set the tagged entity's health to this share of its maximum (0-1). */
		public Double health;
		/** Hold the tagged boss's own attack choice off (true) or let it fight freely again (false). */
		public Boolean hold;
		/** Client input: "attack", "use", "release", "jump", "forward", "sprint", "stop", "slot". */
		public String input;
		public Integer slot;
		/** Server: the player uses its held item (or {@link #useItem}, put in hand first) on this block, as a click would. */
		public double[] useOn;
		public String useItem;
		public String useFace = "up";
		/** Client: one of the mod's realm banners (translation keys) and style (AREA, REGION, DISCOVERED, FELLED, RESTED). */
		public String banner;
		public String bannerSub;
		public String bannerStyle;
		/** Client: call this method (with {@link #args}) on the open screen, e.g. the journal's "select". */
		public String screenCall;
		/** Client: close whatever screen is open (true). */
		public Boolean closeScreen;
		public Double flash;
		public Double shake;
		/** Client: aim the player at this tagged entity every tick from now on ("" stops). */
		public String face;
		public double[] faceOffset;
	}
}
