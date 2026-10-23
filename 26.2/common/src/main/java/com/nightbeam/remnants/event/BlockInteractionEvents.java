package com.nightbeam.remnants.event;

import com.nightbeam.remnants.RemnantBosses;
import com.nightbeam.remnants.entity.Equipment;
import net.minecraft.world.entity.EntityTypes;
import com.nightbeam.remnants.block.AncientAltarBlock;
import com.nightbeam.remnants.config.JaumlConfigLib;
import com.nightbeam.remnants.entity.GraveboundSoldierEntity;
import com.nightbeam.remnants.init.ModBlocks;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.journal.BossHunts;
import com.nightbeam.remnants.journal.RitualInfo;
import com.nightbeam.remnants.procedures.OssukageOnInitialEntitySpawnProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class BlockInteractionEvents {
	private BlockInteractionEvents() {
	}

	public static boolean onRightClickBlock(Player player, Level level, BlockPos pos) {
		if (level.isClientSide()) {
			return false;
		}
		if (!(level.getBlockState(pos).getBlock() instanceof AncientAltarBlock)) {
			return false;
		}
		handleAncientAltarClick(player, pos, level);
		return true;
	}

	private static void handleAncientAltarClick(Player player, BlockPos pos, Level level) {
		if (level.isClientSide()) {
			return;
		}
		if (player.isShiftKeyDown() && player.getMainHandItem().isEmpty() && player instanceof ServerPlayer serverPlayer) {
			BossHunts.open(serverPlayer);
			return;
		}

		if (SovereignRitual.tryActivate(player, level, pos)) return;
		if (WardenRitual.tryActivate(player, level, pos)) return;
		if (WatcherRitual.tryActivate(player, level, pos)) return;
		String heldKey = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString();
		if (!heldKey.equalsIgnoreCase(JaumlConfigLib.getStringValue("remnant/bosses", "ossukage_summon",
				"portal_activation_item"))) {
			return;
		}

		if (level.getBlockState(pos.offset(3, 0, 0)).getBlock() != ModBlocks.ANCIENT_PEDESTAL.get()
				|| level.getBlockState(pos.offset(-3, 0, 0)).getBlock() != ModBlocks.ANCIENT_PEDESTAL.get()
				|| level.getBlockState(pos.offset(0, 0, 3)).getBlock() != ModBlocks.ANCIENT_PEDESTAL.get()
				|| level.getBlockState(pos.offset(0, 0, -3)).getBlock() != ModBlocks.ANCIENT_PEDESTAL.get()) {
			return;
		}

		if (!RitualInfo.resolve("ossukage_summon").matchesTops(level, pos)) return;

		if (level instanceof ServerLevel serverLevel) {
			// Dawn of a new day, as setDayTime(0) did before world clocks.
			serverLevel.dimensionType().defaultClock().ifPresent(clock -> serverLevel.clockManager().setTotalTicks(clock, 0));
			for (BlockPos bp : List.of(pos.offset(3, 1, 0), pos.offset(-3, 1, 0), pos.offset(0, 1, 3),
					pos.offset(0, 1, -3))) {
				LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.TRIGGERED);
				if (bolt != null) {
					bolt.snapTo(Vec3.atBottomCenterOf(bp));
					bolt.setVisualOnly(true);
					serverLevel.addFreshEntity(bolt);
				}
				serverLevel.setBlock(bp, Blocks.AIR.defaultBlockState(), 3);
			}
			serverLevel.playSound(null, pos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.NEUTRAL, 1f, 1f);
		}

		Vec3 center = new Vec3(pos.getX(), pos.getY(), pos.getZ());
		List<Entity> ents = level.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(70d / 2d), e -> true)
				.stream().toList();
		for (Entity entity : ents) {
			if (entity instanceof ServerPlayer serverPlayer) {
				if (player.getInventory().contains(player.getMainHandItem())) {
					if (BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString()
							.equalsIgnoreCase(JaumlConfigLib.getStringValue("remnant/bosses", "ossukage_summon",
									"portal_activation_item"))) {
						if (!serverPlayer.getAbilities().instabuild) {
							player.getMainHandItem().shrink(1);
						}
					}
				}

				serverPlayer.sendSystemMessage(Component.literal(
						"\u00A76The \u00A7cRemnant Warriors \u00A76rise from the \u00A78shadows\u00A76!"));
				try {
					Equipment.playTo(serverPlayer, 
							SoundEvent.createVariableRangeEvent(Identifier.parse("remnant_bosses:skeletonfight_theme")),
							SoundSource.MUSIC, 1f, 1f);
				} catch (Exception ignored) {
				}
			}
		}

		queueBossSpawn(level, pos, ModEntities.REMNANT_OSSUKAGE.get(),
				(int) JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage", "on_spawn_skeletons"), true);
	}

	private static void queueBossSpawn(Level level, BlockPos pos, EntityType<?> bossType, int minions, boolean ossukageInit) {
		RemnantBosses.queueServerWork(80, () -> {
			if (!(level instanceof ServerLevel serverLevel)) {
				return;
			}
			LightningBolt lightning = EntityTypes.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.TRIGGERED);
			if (lightning != null) {
				lightning.snapTo(Vec3.atBottomCenterOf(pos));
				lightning.setVisualOnly(true);
				serverLevel.addFreshEntity(lightning);
			}
			Entity boss = bossType.spawn(serverLevel, pos, EntitySpawnReason.MOB_SUMMONED);
			if (boss != null) {
				boss.setDeltaMovement(0, 0, 0);
				if (ossukageInit) {
					OssukageOnInitialEntitySpawnProcedure.execute(serverLevel, boss);
				}
			}
			Mob owner = boss instanceof Mob mob ? mob : null;
			for (int i = 0; i < minions; i++) {
				BlockPos spot = pos.offset(Mth.nextInt(RandomSource.create(), -1, 1), 1, Mth.nextInt(RandomSource.create(), -1, 1));
				GraveboundSoldierEntity.raise(serverLevel, owner, Vec3.atBottomCenterOf(spot));
			}
		});
	}
}
