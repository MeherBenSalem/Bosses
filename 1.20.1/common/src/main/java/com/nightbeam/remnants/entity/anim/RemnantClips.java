package com.nightbeam.remnants.entity.anim;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Clip names, lengths and loop modes for every entity animation. Entities read them only from here, and
 * {@code ClipContractRegression} proves every entry exists in the matching {@code .animation.json} with
 * the same length and loop mode. Plain Java on purpose: the regression loads it without Minecraft or
 * GeckoLib.
 */
public final class RemnantClips {
	public enum Mode { LOOP, ONCE, HOLD }

	public record Clip(String model, String name, int ticks, Mode mode) {
		public String id() {
			return "animation." + model + "." + name;
		}
	}

	public static final class Sheet {
		private final String model;
		private final Map<String, Clip> clips = new LinkedHashMap<>();

		private Sheet(String model) {
			this.model = model;
		}

		private Sheet add(String name, int ticks, Mode mode) {
			if (clips.put(name, new Clip(model, name, ticks, mode)) != null) {
				throw new IllegalStateException("Duplicate clip " + model + "." + name);
			}
			return this;
		}

		Sheet loop(String name, int ticks) { return add(name, ticks, Mode.LOOP); }
		Sheet once(String name, int ticks) { return add(name, ticks, Mode.ONCE); }
		Sheet hold(String name, int ticks) { return add(name, ticks, Mode.HOLD); }

		public String model() { return model; }

		public Clip clip(String name) {
			Clip clip = clips.get(name);
			if (clip == null) throw new IllegalArgumentException("Unknown clip " + model + "." + name);
			return clip;
		}

		public boolean has(String name) { return clips.containsKey(name); }
		public int ticks(String name) { return clip(name).ticks(); }
		public String id(String name) { return clip(name).id(); }
		public Collection<Clip> all() { return Collections.unmodifiableCollection(clips.values()); }
	}

	public static final Sheet HOLLOW_SOVEREIGN = new Sheet("hollow_sovereign")
			.loop("idle", 60).loop("walk", 32).loop("run", 20)
			.once("awaken", 60).once("combo", 48).once("throne_slam", 44).once("guard", 40).once("riposte", 18)
			.once("rift_cleave", 40).once("summon", 40).once("spectral_slash", 32).once("soul_lunge", 30)
			.once("last_command", 120).once("phase_shatter", 60).once("phase_soul", 60).once("stagger", 20)
			.hold("death", 80);
	/** Also played by the spectral royal guard, which renders the sentinel model. */
	public static final Sheet HOLLOW_SENTINEL = new Sheet("hollow_sentinel")
			.loop("idle", 60).loop("walk", 30).loop("block", 20).once("bash", 20).once("strike", 36)
			.once("hurt", 8).hold("death", 40).once("rise", 30);
	/** The walk loop ships as 28 ticks; only playback reads it, no timing depends on it. */
	public static final Sheet HOLLOW_ARBALIST = new Sheet("hollow_arbalist")
			.loop("idle", 60).loop("walk", 28).once("aim", 16).once("fire", 10).once("reload", 40)
			.once("kick", 16).once("hurt", 8).hold("death", 40);
	public static final Sheet FALLEN_STANDARD_BEARER = new Sheet("fallen_standard_bearer")
			.loop("idle", 60).loop("walk", 32).once("sweep", 24).once("rally", 50).once("hurt", 8).hold("death", 50);
	public static final Sheet SOULBOUND_KNIGHT = new Sheet("soulbound_knight")
			.loop("idle", 60).loop("walk", 28).loop("run", 18).once("combo", 34).once("lunge", 30)
			.once("counter", 24).once("hurt", 8).hold("death", 46);
	public static final Sheet SOVEREIGN_PAULDRON = new Sheet("sovereign_pauldron")
			.loop("idle", 40).once("telegraph", 20).loop("dive", 10);
	public static final Sheet SPECTRAL_CRESCENT = new Sheet("spectral_crescent").loop("spin", 10);

	public static final Sheet GRAVEBOUND_WARDEN = new Sheet("gravebound_warden")
			.loop("idle", 80).loop("walk", 40)
			.once("sweep", 48).once("chop", 52).once("chain_grab", 44).once("eruption", 56).once("summon", 50)
			.once("chain_wall", 60).once("cell_break", 60).once("stagger", 24)
			.hold("death", 100);
	public static final Sheet GRAVEBOUND_SOLDIER = new Sheet("gravebound_soldier")
			.loop("idle", 60).loop("walk", 28).once("thrust", 24).once("sweep", 28).once("hurt", 8).once("rise", 30)
			.hold("death", 32);
	public static final Sheet CRYPT_LURKER = new Sheet("crypt_lurker")
			.loop("idle", 48).loop("walk", 12).once("pounce", 32).once("bite", 16).once("emerge", 24).once("hurt", 6)
			.hold("death", 30);
	public static final Sheet BURIAL_GUARDIAN = new Sheet("burial_guardian")
			.loop("idle", 80).loop("walk", 48).once("slam", 48).once("sweep", 36).once("unseal", 52).once("hurt", 8)
			.hold("death", 48);
	public static final Sheet CATACOMB_RAT = new Sheet("catacomb_rat")
			.loop("idle", 40).loop("walk", 10).once("attack", 12).once("hurt", 6).hold("death", 20);

	// The Watcher floats: idle also plays while drifting.
	public static final Sheet FORGOTTEN_WATCHER = new Sheet("forgotten_watcher")
			.loop("idle", 80).once("volley", 36).once("beam", 88).once("teleport", 32).once("shield", 24)
			.loop("shielded", 80).once("summon", 40).once("awaken_memory", 60).once("hurt", 8)
			.hold("death", 60);
	public static final Sheet LOST_PILGRIM = new Sheet("lost_pilgrim")
			.loop("idle", 80).loop("walk", 40).once("disturbed", 30).once("strike", 24).once("hurt", 8).hold("death", 40);
	public static final Sheet MEMORY_WRAITH = new Sheet("memory_wraith")
			.loop("idle", 60).loop("drift", 24).once("cast", 24).once("blink", 12).once("hurt", 8).hold("death", 32);
	public static final Sheet SOUL_COLLECTOR = new Sheet("soul_collector")
			.loop("idle", 60).once("mark", 32).once("harvest", 32).once("summon", 40).once("hurt", 8).hold("death", 40);
	/** Also spun by the ember and crystal bolts, which render this model in their own colours. */
	public static final Sheet MEMORY_BOLT = new Sheet("memory_bolt").loop("spin", 10);
	public static final Sheet SOUL_WISP = new Sheet("soul_wisp").loop("float", 20);

	// The Ashbound Constructs (docs/remnants/families/ashbound-and-relics.md).
	public static final Sheet ASHBOUND_CRAWLER = new Sheet("ashbound_crawler")
			.loop("idle", 40).loop("walk", 8).once("scorch", 24).once("hurt", 6).hold("death", 20);
	public static final Sheet EMBER_SERVITOR = new Sheet("ember_servitor")
			.loop("idle", 40).loop("fly", 20).once("shoot", 20).loop("repair", 32).once("hurt", 6).hold("death", 28);
	public static final Sheet RUSTED_GUARDIAN = new Sheet("rusted_guardian")
			.loop("idle", 60).loop("walk", 36).once("smash", 52).once("sweep", 40).once("hurt", 8).hold("death", 48);
	// The Corrupted Relics. The Shardling floats: idle also plays while it drifts.
	public static final Sheet SHARDLING = new Sheet("shardling")
			.loop("idle", 40).once("shoot", 16).once("hurt", 6).hold("death", 12);
	public static final Sheet ANCIENT_VESSEL = new Sheet("ancient_vessel")
			.loop("idle", 60).loop("walk", 16).once("pulse", 28).once("hurt", 6).hold("death", 24);
	public static final Sheet RELIC_STALKER = new Sheet("relic_stalker")
			.loop("idle", 60).loop("walk", 14).once("ambush", 32).once("slash", 20).once("hurt", 6).hold("death", 36);

	public static final List<Sheet> ALL = List.of(HOLLOW_SOVEREIGN, HOLLOW_SENTINEL, HOLLOW_ARBALIST,
			FALLEN_STANDARD_BEARER, SOULBOUND_KNIGHT, SOVEREIGN_PAULDRON, SPECTRAL_CRESCENT,
			GRAVEBOUND_WARDEN, GRAVEBOUND_SOLDIER, CRYPT_LURKER, BURIAL_GUARDIAN, CATACOMB_RAT,
			FORGOTTEN_WATCHER, LOST_PILGRIM, MEMORY_WRAITH, SOUL_COLLECTOR, MEMORY_BOLT, SOUL_WISP,
			ASHBOUND_CRAWLER, EMBER_SERVITOR, RUSTED_GUARDIAN, SHARDLING, ANCIENT_VESSEL, RELIC_STALKER);

	private RemnantClips() {
	}
}
