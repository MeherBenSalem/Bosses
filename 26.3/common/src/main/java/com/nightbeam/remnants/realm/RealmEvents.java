package com.nightbeam.remnants.realm;

import com.nightbeam.remnants.config.RealmConfig;
import com.nightbeam.remnants.entity.Equipment;
import com.nightbeam.remnants.init.ModSounds;
import com.nightbeam.remnants.journal.BossHunt;
import com.nightbeam.remnants.network.ClientboundRealmFxPacket;
import com.nightbeam.remnants.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;

/** Realm-wide hooks: the REMNANT FELLED banner and the Realm's own spawn rule. */
public final class RealmEvents {
	private RealmEvents() {
	}

	public static void onLivingDeath(LivingEntity entity) {
		if (!(entity.level() instanceof ServerLevel level)) return;
		if (BossHunt.fromId(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString()) == null) return;
		double radius = RealmConfig.value("felled_banner_radius", 0, 512);
		ClientboundRealmFxPacket banner = new ClientboundRealmFxPacket(ClientboundRealmFxPacket.REMNANT_FELLED, 0,
				entity.getType().getDescriptionId(), "");
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(entity) > radius * radius) continue;
			Services.NETWORK.sendRealmFx(player, banner);
			Equipment.playTo(player, ModSounds.REMNANT_FELLED.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
		}
	}

	/**
	 * In the Realm, Remnant creatures ignore daylight (it is always dusk under the eclipse): a natural spawn needs
	 * block light at or below spawn_block_light_max and passes a spawn_thinning roll. Elsewhere the vanilla
	 * darkness check applies. Returns null outside the Realm so the caller keeps its own rule.
	 */
	public static Boolean realmSpawnLight(ServerLevelAccessor world, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		if (!EclipsedRealm.isRealm(world.getLevel())) return null;
		if (world.getBrightness(LightLayer.BLOCK, pos) > (int) RealmConfig.value("spawn_block_light_max", 0, 15)) return false;
		if (reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION) {
			return random.nextDouble() < RealmConfig.value("spawn_thinning", 0, 1);
		}
		return true;
	}
}
