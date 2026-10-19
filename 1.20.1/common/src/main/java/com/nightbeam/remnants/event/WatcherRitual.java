package com.nightbeam.remnants.event;

import com.nightbeam.remnants.config.RealmConfig;
import com.nightbeam.remnants.config.WatcherConfig;
import com.nightbeam.remnants.entity.ForgottenWatcherEntity;
import com.nightbeam.remnants.init.ModBlocks;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.journal.RitualInfo;
import com.nightbeam.remnants.realm.EclipsedRealm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Forgotten Watcher's altar ritual: the Lens of Memory on the Ancient Altar with an amethyst block
 * (east), calcite (west), chiseled sandstone (south) and a lectern (north) on the four pedestals. Same
 * flow as {@link WardenRitual}: everything is validated before the offering is consumed; the Watcher
 * appears 1.5 blocks above the altar and remembers it as the centre of its four lenses.
 */
public final class WatcherRitual {
	private WatcherRitual() {
	}

	public static boolean tryActivate(Player player, Level level, BlockPos altar) {
		RitualInfo ritual = RitualInfo.resolve(WatcherConfig.SUMMON);
		String item = ritual.activationItem();
		if (!BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString().equals(item)) return false;
		if (!(level instanceof ServerLevel server)) return true;
		if (player.isSpectator() || WatcherConfig.value("summoning_enabled", 0, 1) == 0 || level.getDifficulty() == Difficulty.PEACEFUL) {
			message(player, "disabled");
			return true;
		}
		if (RealmConfig.ritualsRealmOnly() && !EclipsedRealm.isRealm(level)) {
			message(player, "realm");
			return true;
		}
		BlockPos[] pedestals = {altar.offset(3, 0, 0), altar.offset(-3, 0, 0), altar.offset(0, 0, 3), altar.offset(0, 0, -3)};
		for (int i = 0; i < 4; i++) {
			ResourceLocation id = ResourceLocation.tryParse(ritual.offerings().get(i));
			if (!ritual.valid() || id == null || !BuiltInRegistries.BLOCK.containsKey(id)
					|| !level.getBlockState(pedestals[i]).is(ModBlocks.ANCIENT_PEDESTAL.get())
					|| !level.getBlockState(pedestals[i].above()).is(BuiltInRegistries.BLOCK.get(id))) {
				message(player, "pattern");
				return true;
			}
		}
		double radius = WatcherConfig.value("duplicate_radius", 16, 128);
		if (!level.getEntitiesOfClass(ForgottenWatcherEntity.class, new AABB(altar).inflate(radius), e -> e.isAlive()).isEmpty()) {
			message(player, "nearby");
			return true;
		}
		ForgottenWatcherEntity boss = ModEntities.FORGOTTEN_WATCHER.get().create(server);
		if (boss == null) return true;
		boss.moveTo(altar.getX() + .5, altar.getY() + 1.5, altar.getZ() + .5, player.getYRot() + 180, 0);
		if (!level.getWorldBorder().isWithinBounds(boss.getBoundingBox()) || !level.noCollision(boss, boss.getBoundingBox())) {
			message(player, "space");
			return true;
		}
		boss.setHome(new Vec3(altar.getX() + .5, altar.getY() + 1, altar.getZ() + .5));
		boss.setPersistenceRequired();
		if (!server.addFreshEntity(boss)) {
			message(player, "space");
			return true;
		}
		for (BlockPos pedestal : pedestals) server.setBlock(pedestal.above(), Blocks.AIR.defaultBlockState(), 3);
		if (!player.getAbilities().instabuild) player.getMainHandItem().shrink(1);
		server.playSound(null, altar, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.0f, 0.6f);
		message(player, "summoned");
		return true;
	}

	private static void message(Player player, String suffix) {
		player.displayClientMessage(Component.translatable("message.remnant_bosses.watcher." + suffix), true);
	}
}
