package com.nightbeam.remnants.event;

import com.nightbeam.remnants.RemnantBosses;
import com.nightbeam.remnants.block.AncientAltarBlock;
import com.nightbeam.remnants.config.JaumlConfigLib;
import com.nightbeam.remnants.init.ModBlocks;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.procedures.OssukageOnInitialEntitySpawnProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

public final class BlockInteractionEvents {
	private BlockInteractionEvents() {
	}

	public static boolean onRightClickBlock(Player player, Level level, BlockPos pos) {
		if (level.isClientSide) {
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

		String heldKey = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString();
		if (heldKey.equalsIgnoreCase(JaumlConfigLib.getStringValue("remnant/bosses", "umbrakar_summon",
				"portal_activation_item"))) {
			trySummon(player, pos, level, "umbrakar_summon",
					"\u00A75The \u00A7dRiftmaw Colossus \u00A75tears through the altar!",
					ModEntities.UMBRAKAR.get(), 0, SoundEvents.WARDEN_SONIC_BOOM);
			return;
		}
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

		boolean topsValid = true;
		Locale locale = Locale.ENGLISH;
		ResourceLocation cfgOne = ResourceLocation.parse(JaumlConfigLib
				.getStringValue("remnant/bosses", "ossukage_summon", "pedestal_one_activation_block")
				.toLowerCase(locale));
		ResourceLocation cfgTwo = ResourceLocation.parse(JaumlConfigLib
				.getStringValue("remnant/bosses", "ossukage_summon", "pedestal_two_activation_block")
				.toLowerCase(locale));
		ResourceLocation cfgThree = ResourceLocation.parse(JaumlConfigLib
				.getStringValue("remnant/bosses", "ossukage_summon", "pedestal_three_activation_block")
				.toLowerCase(locale));
		ResourceLocation cfgFour = ResourceLocation.parse(JaumlConfigLib
				.getStringValue("remnant/bosses", "ossukage_summon", "pedestal_four_activation_block")
				.toLowerCase(locale));

		if (BuiltInRegistries.BLOCK.get(cfgOne) != level.getBlockState(pos.offset(3, 1, 0)).getBlock()) {
			topsValid = false;
		}
		if (BuiltInRegistries.BLOCK.get(cfgTwo) != level.getBlockState(pos.offset(-3, 1, 0)).getBlock()) {
			topsValid = false;
		}
		if (BuiltInRegistries.BLOCK.get(cfgThree) != level.getBlockState(pos.offset(0, 1, 3)).getBlock()) {
			topsValid = false;
		}
		if (BuiltInRegistries.BLOCK.get(cfgFour) != level.getBlockState(pos.offset(0, 1, -3)).getBlock()) {
			topsValid = false;
		}
		if (!topsValid) {
			return;
		}

		if (level instanceof ServerLevel serverLevel) {
			serverLevel.setDayTime(0);
			for (BlockPos bp : List.of(pos.offset(3, 1, 0), pos.offset(-3, 1, 0), pos.offset(0, 1, 3),
					pos.offset(0, 1, -3))) {
				LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
				if (bolt != null) {
					bolt.moveTo(Vec3.atBottomCenterOf(bp));
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

				serverPlayer.displayClientMessage(Component.literal(
						"\u00A76The \u00A7cRemnant Warriors \u00A76rise from the \u00A78shadows\u00A76!"), false);
				try {
					serverPlayer.playNotifySound(
							SoundEvent.createVariableRangeEvent(ResourceLocation.parse("remnant_bosses:skeletonfight_theme")),
							SoundSource.MUSIC, 1f, 1f);
				} catch (Exception ignored) {
				}
			}
		}

		queueBossSpawn(level, pos, ModEntities.REMNANT_OSSUKAGE.get(),
				(int) JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage", "on_spawn_skeletons"), true);
	}

	private static void trySummon(Player player, BlockPos pos, Level level, String summonFile, String message,
			EntityType<?> bossType, int minions, SoundEvent sound) {
		if (level.getBlockState(pos.offset(3, 0, 0)).getBlock() != ModBlocks.ANCIENT_PEDESTAL.get()
				|| level.getBlockState(pos.offset(-3, 0, 0)).getBlock() != ModBlocks.ANCIENT_PEDESTAL.get()
				|| level.getBlockState(pos.offset(0, 0, 3)).getBlock() != ModBlocks.ANCIENT_PEDESTAL.get()
				|| level.getBlockState(pos.offset(0, 0, -3)).getBlock() != ModBlocks.ANCIENT_PEDESTAL.get()) {
			return;
		}
		Locale locale = Locale.ENGLISH;
		ResourceLocation cfgOne = ResourceLocation.parse(JaumlConfigLib.getStringValue("remnant/bosses", summonFile, "pedestal_one_activation_block").toLowerCase(locale));
		ResourceLocation cfgTwo = ResourceLocation.parse(JaumlConfigLib.getStringValue("remnant/bosses", summonFile, "pedestal_two_activation_block").toLowerCase(locale));
		ResourceLocation cfgThree = ResourceLocation.parse(JaumlConfigLib.getStringValue("remnant/bosses", summonFile, "pedestal_three_activation_block").toLowerCase(locale));
		ResourceLocation cfgFour = ResourceLocation.parse(JaumlConfigLib.getStringValue("remnant/bosses", summonFile, "pedestal_four_activation_block").toLowerCase(locale));
		if (BuiltInRegistries.BLOCK.get(cfgOne) != level.getBlockState(pos.offset(3, 1, 0)).getBlock()
				|| BuiltInRegistries.BLOCK.get(cfgTwo) != level.getBlockState(pos.offset(-3, 1, 0)).getBlock()
				|| BuiltInRegistries.BLOCK.get(cfgThree) != level.getBlockState(pos.offset(0, 1, 3)).getBlock()
				|| BuiltInRegistries.BLOCK.get(cfgFour) != level.getBlockState(pos.offset(0, 1, -3)).getBlock()) {
			return;
		}
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.setDayTime(18000);
			for (BlockPos bp : List.of(pos.offset(3, 1, 0), pos.offset(-3, 1, 0), pos.offset(0, 1, 3), pos.offset(0, 1, -3))) {
				LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
				if (bolt != null) {
					bolt.moveTo(Vec3.atBottomCenterOf(bp));
					bolt.setVisualOnly(true);
					serverLevel.addFreshEntity(bolt);
				}
				serverLevel.setBlock(bp, Blocks.AIR.defaultBlockState(), 3);
			}
			serverLevel.playSound(null, pos, sound, SoundSource.NEUTRAL, 1f, 0.65f);
		}
		for (Entity entity : level.getEntitiesOfClass(Entity.class, new AABB(Vec3.atCenterOf(pos), Vec3.atCenterOf(pos)).inflate(35d), e -> true)) {
			if (entity instanceof ServerPlayer serverPlayer) {
				if (!serverPlayer.getAbilities().instabuild && heldMatches(player, summonFile)) {
					player.getMainHandItem().shrink(1);
				}
				serverPlayer.displayClientMessage(Component.literal(message), false);
			}
		}
		queueBossSpawn(level, pos, bossType, minions, false);
	}

	private static boolean heldMatches(Player player, String summonFile) {
		return BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString()
				.equalsIgnoreCase(JaumlConfigLib.getStringValue("remnant/bosses", summonFile, "portal_activation_item"));
	}

	private static void queueBossSpawn(Level level, BlockPos pos, EntityType<?> bossType, int minions, boolean ossukageInit) {
		RemnantBosses.queueServerWork(80, () -> {
			if (!(level instanceof ServerLevel serverLevel)) {
				return;
			}
			LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
			if (lightning != null) {
				lightning.moveTo(Vec3.atBottomCenterOf(pos));
				lightning.setVisualOnly(true);
				serverLevel.addFreshEntity(lightning);
			}
			Entity boss = bossType.spawn(serverLevel, pos, MobSpawnType.MOB_SUMMONED);
			if (boss != null) {
				boss.setDeltaMovement(0, 0, 0);
				if (ossukageInit) {
					OssukageOnInitialEntitySpawnProcedure.execute(serverLevel, boss);
				}
			}
			for (int i = 0; i < minions; i++) {
				Entity minion = ModEntities.SKELETON_MINION.get().spawn(serverLevel,
						pos.offset(Mth.nextInt(RandomSource.create(), -1, 1), 1,
								Mth.nextInt(RandomSource.create(), -1, 1)),
						MobSpawnType.MOB_SUMMONED);
				if (minion != null) {
					minion.setDeltaMovement(0, 0, 0);
				}
			}
		});
	}
}
