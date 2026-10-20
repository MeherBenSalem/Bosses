package com.nightbeam.remnants.realm;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.config.RealmConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Realm does not change (realm.md section 8): survival and adventure players can neither break nor place
 * blocks there, use items on blocks (except those in #remnant_bosses:realm_usable) or empty buckets, and
 * explosions leave the terrain whole. Creative and spectator players are exempt. Blocks can still be used:
 * graces, Waygates, doors and chests work as usual.
 */
public final class RealmProtection {
	public static final TagKey<Item> USABLE = TagKey.create(Registries.ITEM, new ResourceLocation(Constants.MOD_ID, "realm_usable"));
	private static final long WARNING_INTERVAL_MS = 2000;
	private static final Map<UUID, Long> LAST_WARNING = new ConcurrentHashMap<>();

	private RealmProtection() {
	}

	public static boolean active(@Nullable Level level) {
		return EclipsedRealm.isRealm(level) && RealmConfig.buildProtection();
	}

	public static boolean denies(@Nullable Player player, @Nullable Level level) {
		return player != null && !player.isCreative() && !player.isSpectator() && active(level);
	}

	/** An action-bar reminder from the server side, at most every two seconds per player. */
	public static void warn(Player player) {
		if (player.level().isClientSide) return;
		long now = System.currentTimeMillis();
		Long last = LAST_WARNING.get(player.getUUID());
		if (last != null && now - last < WARNING_INTERVAL_MS) return;
		LAST_WARNING.put(player.getUUID(), now);
		player.displayClientMessage(Component.translatable("message.remnant_bosses.realm.protected"), true);
	}
}
