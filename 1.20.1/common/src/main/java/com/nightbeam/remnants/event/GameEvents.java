package com.nightbeam.remnants.event;

import com.nightbeam.remnants.RemnantBosses;
import com.nightbeam.remnants.block.AncientAltarBlock;
import com.nightbeam.remnants.config.JaumlConfigLib;
import com.nightbeam.remnants.entity.RatEntity;
import com.nightbeam.remnants.entity.RemnantOssukageEntity;
import com.nightbeam.remnants.entity.SkeletonMinionEntity;
import com.nightbeam.remnants.entity.WraithEntity;
import com.nightbeam.remnants.init.ModBlocks;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.item.OssukageSwordItem;
import com.nightbeam.remnants.procedures.NinjaSkeletonEntityIsHurtProcedure;
import com.nightbeam.remnants.procedures.NinjaSkeletonOnEntityTickUpdateProcedure;
import com.nightbeam.remnants.procedures.OssukageOnInitialEntitySpawnProcedure;
import com.nightbeam.remnants.procedures.ThrowKunaisProcedureProcedure;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

public final class GameEvents {
	private GameEvents() {
	}

	public static void updateRatAnimations(RatEntity entity) {
		int tickCount = entity.tickCount;
		boolean isAttacking = entity.swinging || entity.getAttackAnim(0.0f) > 0.0f;

		if (isAttacking) {
			entity.animationState2.startIfStopped(tickCount);
			entity.animationState0.stop();
		} else {
			entity.animationState0.startIfStopped(tickCount);
			entity.animationState2.stop();
		}
	}

	public static void updateOssukageAnimations(RemnantOssukageEntity entity) {
		int tickCount = entity.tickCount;
		String state = entity.getEntityState();

		boolean isAttacking = entity.swinging || entity.getAttackAnim(0.0f) > 0.0f;
		boolean isTransforming = entity.isTransformed();
		boolean isSpawning = tickCount < 120;
		boolean isLeaping = state.equals("leap");
		boolean isIdle = !isAttacking && (state.equals("idle") || state.isEmpty());

		if (isSpawning) {
			entity.animationState5.startIfStopped(tickCount);
		} else {
			entity.animationState5.stop();
		}

		if (isTransforming) {
			entity.animationState4.startIfStopped(tickCount);
		} else {
			entity.animationState4.stop();
		}

		if (isLeaping) {
			entity.animationState3.startIfStopped(tickCount);
		} else {
			entity.animationState3.stop();
		}

		if (isAttacking) {
			entity.animationState2.startIfStopped(tickCount);
			entity.animationState0.stop();
		} else if (isIdle) {
			entity.animationState0.startIfStopped(tickCount);
			entity.animationState2.stop();
		} else {
			entity.animationState0.stop();
			entity.animationState2.stop();
		}
	}

	public static void updateSkeletonMinionAnimations(SkeletonMinionEntity entity) {
		int tickCount = entity.tickCount;
		boolean isAttacking = entity.swinging || entity.getAttackAnim(0.0f) > 0.0f;
		boolean isSpawning = tickCount < 120;

		if (isSpawning) {
			entity.animationState3.startIfStopped(tickCount);
		} else {
			entity.animationState3.stop();
		}

		if (isAttacking) {
			entity.animationState2.startIfStopped(tickCount);
			entity.animationState0.stop();
		} else {
			entity.animationState0.startIfStopped(tickCount);
			entity.animationState2.stop();
		}
	}

	public static void updateWraithAnimations(WraithEntity entity) {
		int tickCount = entity.tickCount;
		boolean isDead = entity.isDeadOrDying();
		boolean isAttacking = entity.swinging || entity.getAttackAnim(0.0f) > 0.0f;

		if (isDead) {
			entity.animationState3.startIfStopped(tickCount);
			entity.animationState0.stop();
			entity.animationState2.stop();
		} else {
			entity.animationState3.stop();
			if (isAttacking) {
				entity.animationState2.startIfStopped(tickCount);
				entity.animationState0.stop();
			} else {
				entity.animationState0.startIfStopped(tickCount);
				entity.animationState2.stop();
			}
		}
	}

	public static void updateOssukageServerTick(RemnantOssukageEntity entity) {
		NinjaSkeletonOnEntityTickUpdateProcedure.execute(entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity);
	}

	public static void onLivingHurt(LivingEntity entity, DamageSource source, float amount) {
		if (entity instanceof RemnantOssukageEntity ossukage) {
			NinjaSkeletonEntityIsHurtProcedure.execute(ossukage.level(), ossukage.getX(), ossukage.getY(), ossukage.getZ(), ossukage);
		}

		if (entity.level().isClientSide) {
			return;
		}

		Entity attacker = source.getEntity();
		if (!(attacker instanceof Player player)) {
			return;
		}

		ItemStack itemStack = player.getMainHandItem();
		if (itemStack.getItem() instanceof OssukageSwordItem) {
			handleOssukageSwordHit(entity, player, itemStack, entity.level());
		}
	}

	public static boolean onEntityJoin(Entity entity, Level level) {
		if (level.isClientSide) {
			return true;
		}

		if (entity instanceof RatEntity || entity instanceof RemnantOssukageEntity
				|| entity instanceof SkeletonMinionEntity || entity instanceof WraithEntity) {
			if (!isDimensionAllowed(level)) {
				return false;
			}
		}

		if (entity instanceof RemnantOssukageEntity ossukage) {
			OssukageOnInitialEntitySpawnProcedure.execute((LevelAccessor) ossukage.level(), ossukage);
		}

		if (entity instanceof RatEntity rat) {
			int skinVariant = rat.getRandom().nextInt(4);
			rat.setSkinVariant(skinVariant);
		}

		if (entity instanceof SkeletonMinionEntity skeleton) {
			skeleton.setSpawned(true);
		}

		return true;
	}

	public static void onLivingDeath(LivingEntity entity, DamageSource source) {
		Level level = entity.level();
		if (level.isClientSide) {
			return;
		}

		if (entity instanceof RemnantOssukageEntity ossukage) {
			handleOssukageDeath(ossukage, level);
		}
	}

	public static boolean onRightClickBlock(Player player, Level level, BlockPos pos, Block block) {
		if (level.isClientSide) {
			return false;
		}

		if (block instanceof AncientAltarBlock) {
			handleAncientAltarClick(player, pos, level);
			return true;
		}
		return false;
	}

	private static boolean isDimensionAllowed(Level level) {
		String dimensionKey = level.dimension().location().toString();

		List<String> whitelist = JaumlConfigLib.getStringListValue("remnant/spawning", "rat_spawns", "dimension_whitelist");
		if (!whitelist.isEmpty() && !whitelist.contains(dimensionKey)) {
			return false;
		}

		List<String> blacklist = JaumlConfigLib.getStringListValue("remnant/spawning", "rat_spawns", "dimension_blacklist");
		return !blacklist.contains(dimensionKey);
	}

	private static void handleOssukageDeath(RemnantOssukageEntity entity, Level level) {
		if (!level.isClientSide() && level.getServer() != null) {
			level.getServer().getCommands().performPrefixedCommand(
					new CommandSourceStack(CommandSource.NULL, entity.position(), entity.getRotationVector(),
							level instanceof ServerLevel ? (ServerLevel) level : null, 4, entity.getName().getString(),
							entity.getDisplayName(), level.getServer(), entity),
					"stopsound @p music remnant_bosses:skeletonfight_theme");
		}
	}

	private static void handleOssukageSwordHit(LivingEntity target, Player attacker, ItemStack itemStack, Level level) {
	}

	private static void handleAncientAltarClick(Player player, BlockPos pos, Level level) {
		if (level.isClientSide()) {
			return;
		}

		String heldKey = String.valueOf(BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()));
		if (heldKey.equalsIgnoreCase(JaumlConfigLib.getStringValue("remnant/bosses", "umbrakar_summon", "portal_activation_item"))) {
			trySummonBoss(player, pos, level, "umbrakar_summon", "umbrakar",
					"\u00A75The \u00A7dRiftmaw Colossus \u00A75tears through the altar!",
					ModEntities.UMBRAKAR.get(), 0);
			return;
		}
		if (heldKey.equalsIgnoreCase(JaumlConfigLib.getStringValue("remnant/bosses", "ossukage_summon", "portal_activation_item"))) {
			trySummonBoss(player, pos, level, "ossukage_summon", "ossukage",
					"\u00A76The \u00A7cRemnant Warriors \u00A76rise from the \u00A78shadows\u00A76!",
					ModEntities.REMNANT_OSSUKAGE.get(),
					(int) JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage", "on_spawn_skeletons"));
		}
	}

	private static void trySummonBoss(Player player, BlockPos pos, Level level, String summonFile, String unused,
			String message, EntityType<?> bossType, int minions) {
		if (level.getBlockState(pos.offset(3, 0, 0)).getBlock() != ModBlocks.ANCIENT_PEDESTAL.get()
				|| level.getBlockState(pos.offset(-3, 0, 0)).getBlock() != ModBlocks.ANCIENT_PEDESTAL.get()
				|| level.getBlockState(pos.offset(0, 0, 3)).getBlock() != ModBlocks.ANCIENT_PEDESTAL.get()
				|| level.getBlockState(pos.offset(0, 0, -3)).getBlock() != ModBlocks.ANCIENT_PEDESTAL.get()) {
			return;
		}
		if (!pedestalTopsMatch(level, pos, summonFile)) {
			return;
		}
		playRitualEffects(level, pos);
		finishRitual(player, pos, level, summonFile, message, bossType, minions);
	}

	private static boolean pedestalTopsMatch(Level level, BlockPos pos, String summonFile) {
		ResourceLocation cfgOne = ResourceLocation.tryParse(JaumlConfigLib.getStringValue("remnant/bosses", summonFile, "pedestal_one_activation_block").toLowerCase(Locale.ENGLISH));
		ResourceLocation cfgTwo = ResourceLocation.tryParse(JaumlConfigLib.getStringValue("remnant/bosses", summonFile, "pedestal_two_activation_block").toLowerCase(Locale.ENGLISH));
		ResourceLocation cfgThree = ResourceLocation.tryParse(JaumlConfigLib.getStringValue("remnant/bosses", summonFile, "pedestal_three_activation_block").toLowerCase(Locale.ENGLISH));
		ResourceLocation cfgFour = ResourceLocation.tryParse(JaumlConfigLib.getStringValue("remnant/bosses", summonFile, "pedestal_four_activation_block").toLowerCase(Locale.ENGLISH));
		return cfgOne != null && BuiltInRegistries.BLOCK.get(cfgOne) == level.getBlockState(pos.offset(3, 1, 0)).getBlock()
				&& cfgTwo != null && BuiltInRegistries.BLOCK.get(cfgTwo) == level.getBlockState(pos.offset(-3, 1, 0)).getBlock()
				&& cfgThree != null && BuiltInRegistries.BLOCK.get(cfgThree) == level.getBlockState(pos.offset(0, 1, 3)).getBlock()
				&& cfgFour != null && BuiltInRegistries.BLOCK.get(cfgFour) == level.getBlockState(pos.offset(0, 1, -3)).getBlock();
	}

	private static void playRitualEffects(Level level, BlockPos pos) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
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
		serverLevel.playSound(null, pos, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.tryParse("entity.warden.sonic_boom")), SoundSource.NEUTRAL, 1f, 0.6f);
	}

	private static void finishRitual(Player player, BlockPos pos, Level level, String summonFile, String message,
			EntityType<?> bossType, int minions) {
		final Vec3 center = new Vec3(pos.getX(), pos.getY(), pos.getZ());
		List<Entity> ents = level.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(35d), e -> true);
		for (Entity e : ents) {
			if (e instanceof ServerPlayer serverPlayer) {
				ResourceLocation activationId = ResourceLocation.tryParse(JaumlConfigLib.getStringValue("remnant/bosses", summonFile, "portal_activation_item").toLowerCase(Locale.ENGLISH));
				if (activationId != null) {
					player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == BuiltInRegistries.ITEM.get(activationId), 1, player.inventoryMenu.getCraftSlots());
				}
				serverPlayer.displayClientMessage(Component.literal(message), false);
			}
		}

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
			}
			for (int i = 0; i < minions; i++) {
				Entity minion = ModEntities.SKELETON_MINION.get().spawn(serverLevel,
						pos.offset(Mth.nextInt(RandomSource.create(), -1, 1), 1, Mth.nextInt(RandomSource.create(), -1, 1)),
						MobSpawnType.MOB_SUMMONED);
				if (minion != null) {
					minion.setDeltaMovement(0, 0, 0);
				}
			}
		});
	}

	public static void handleOssukageSwordRightClick(Player player, ItemStack itemStack, Level level) {
		if (level.isClientSide()) {
			return;
		}

		ThrowKunaisProcedureProcedure.execute(player);
		level.playSound(null, player.blockPosition(),
				BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.tryParse("entity.arrow.shoot")),
				SoundSource.PLAYERS, 1f, 1f);
		player.getCooldowns().addCooldown(itemStack.getItem(),
				(int) JaumlConfigLib.getNumberValue("remnant/items", "ossukage_sword", "shuriken_timer"));
	}
}
