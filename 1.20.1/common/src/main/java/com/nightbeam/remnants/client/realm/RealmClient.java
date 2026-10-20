package com.nightbeam.remnants.client.realm;

import com.nightbeam.remnants.block.realm.GraceEmberBlock;
import com.nightbeam.remnants.config.RealmConfig;
import com.nightbeam.remnants.network.ClientboundGraceMenuPacket;
import com.nightbeam.remnants.network.ClientboundRealmFxPacket;
import com.nightbeam.remnants.realm.EclipsedRealm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

/** Client entry points for the Realm: packet handlers, the per-tick update and region titles. */
public final class RealmClient {
	private static final int REGION_INTERVAL = 20;
	private static final long REGION_REPEAT_TICKS = 20L * 60 * 10;
	private static final Map<String, Long> REGION_SHOWN = new HashMap<>();
	private static String lastBiome = "";
	private static long clientTicks;

	private RealmClient() {
	}

	/** The sky and fog for the Realm's {@code effects} id; loaders register it under {@link EclipsedRealm#ID}. */
	public static DimensionSpecialEffects createEffects() {
		return new EclipsedRealmEffects();
	}

	public static void tick() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null) {
			RealmOverlay.reset();
			ScreenShake.reset();
			lastBiome = "";
			return;
		}
		clientTicks++;
		RealmOverlay.tick(minecraft.screen instanceof ReceivingLevelScreen);
		ScreenShake.tick();
		if (clientTicks % REGION_INTERVAL == 0) regionTitle(minecraft);
	}

	private static void regionTitle(Minecraft minecraft) {
		if (!EclipsedRealm.isRealm(minecraft.level)) {
			lastBiome = "";
			return;
		}
		String biome = EclipsedRealm.biomeKey(minecraft.level, minecraft.player.blockPosition());
		if (biome.isEmpty() || biome.equals(lastBiome)) return;
		lastBiome = biome;
		Long shown = REGION_SHOWN.get(biome);
		if (shown != null && clientTicks - shown < REGION_REPEAT_TICKS) return;
		REGION_SHOWN.put(biome, clientTicks);
		if (RealmConfig.clientSwitch("realm_titles") && !RealmOverlay.showingTitle()) {
			RealmOverlay.banner(Component.translatable(biome), Component.empty(), RealmOverlay.Style.REGION, 0);
		}
	}

	public static void handleFx(ClientboundRealmFxPacket packet) {
		Minecraft minecraft = Minecraft.getInstance();
		switch (packet.kind()) {
			case ClientboundRealmFxPacket.CROSS_BEGIN -> {
				RealmOverlay.beginCrossing(packet.ticks());
				ScreenShake.add(0.35f);
			}
			case ClientboundRealmFxPacket.ARRIVE -> {
				RealmOverlay.arrive();
				ScreenShake.reset();
				Component title = packet.title().isEmpty() ? Component.empty() : Component.translatable(packet.title());
				Component subtitle = graceName(packet.subtitle());
				if (!packet.subtitle().isEmpty() && packet.subtitle().startsWith("biome.")) {
					lastBiome = packet.subtitle();
					REGION_SHOWN.put(packet.subtitle(), clientTicks);
				}
				if (packet.title().isEmpty() && !packet.subtitle().isEmpty()) {
					RealmOverlay.banner(subtitle, Component.empty(), RealmOverlay.Style.REGION, 26);
				} else if (!packet.title().isEmpty()) {
					RealmOverlay.banner(title, subtitle, RealmOverlay.Style.AREA, 22);
				}
			}
			case ClientboundRealmFxPacket.GRACE_DISCOVERED -> RealmOverlay.banner(
					Component.translatable("realm.remnant_bosses.grace_discovered"), Component.empty(), RealmOverlay.Style.DISCOVERED, 0);
			case ClientboundRealmFxPacket.GRACE_RESTED -> RealmOverlay.banner(
					Component.translatable("realm.remnant_bosses.grace_rested"), Component.empty(), RealmOverlay.Style.RESTED, 0);
			case ClientboundRealmFxPacket.REMNANT_FELLED -> {
				RealmOverlay.flash(0.45f);
				ScreenShake.add(0.4f);
				RealmOverlay.banner(Component.translatable("realm.remnant_bosses.remnant_felled"),
						packet.title().isEmpty() ? Component.empty() : Component.translatable(packet.title()), RealmOverlay.Style.FELLED, 6);
			}
			case ClientboundRealmFxPacket.CANCEL -> RealmOverlay.cancelCrossing();
			default -> {
			}
		}
		if (minecraft.player == null) RealmOverlay.reset();
	}

	public static void openGraceMenu(ClientboundGraceMenuPacket packet) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level != null) GraceEmberBlock.ensureBlockEntity(minecraft.level, packet.grace());
		minecraft.setScreen(new GraceScreen(packet));
	}

	/** "@biome.key#2" names a grace by its biome (numbered past the first); anything else is a custom name. */
	public static Component graceName(String raw) {
		if (raw == null || raw.isEmpty()) return Component.empty();
		if (raw.startsWith("biome.")) return Component.translatable(raw);
		if (!raw.startsWith("@")) return Component.literal(raw);
		int hash = raw.lastIndexOf('#');
		String key = hash > 1 ? raw.substring(1, hash) : raw.substring(1);
		int ordinal = 1;
		if (hash > 1) {
			try {
				ordinal = Integer.parseInt(raw.substring(hash + 1));
			} catch (NumberFormatException ignored) {
				ordinal = 1;
			}
		}
		Component base = key.isEmpty() ? Component.translatable("realm.remnant_bosses.grace") : Component.translatable(key);
		return ordinal > 1 ? Component.empty().append(base).append(" " + roman(ordinal)) : base;
	}

	private static String roman(int n) {
		String[] numerals = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
		return n >= 0 && n < numerals.length ? numerals[n] : Integer.toString(n);
	}
}
