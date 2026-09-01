package com.nightbeam.remnants.init;

import com.nightbeam.remnants.entity.UmbrakarEntity;
import com.nightbeam.remnants.entity.UmbrakarOrbEntity;
import com.nightbeam.remnants.entity.ArmoredGrubEntity;
import com.nightbeam.remnants.entity.KotsukageEntity;
import com.nightbeam.remnants.entity.KotsukageTrapEntity;
import com.nightbeam.remnants.entity.KunaiEntity;
import com.nightbeam.remnants.entity.RatEntity;
import com.nightbeam.remnants.entity.RemnantOssukageEntity;
import com.nightbeam.remnants.entity.OssukageRuneEffectEntity;
import com.nightbeam.remnants.entity.SkeletonMinionEntity;
import com.nightbeam.remnants.entity.WraithEntity;
import com.nightbeam.remnants.registry.RegistryHolder;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.util.RandomSource;
import net.minecraft.core.BlockPos;

public final class ModEntities {
	public static final RegistryHolder<EntityType<KunaiEntity>> KUNAI = RegistryHolder.entity("kunai",
			() -> EntityType.Builder.<KunaiEntity>of(KunaiEntity::new, MobCategory.MISC)
					.sized(0.5f, 0.5f)
					.clientTrackingRange(64)
					.updateInterval(1)
					.build("kunai"));

	public static final RegistryHolder<EntityType<RatEntity>> RAT = RegistryHolder.entity("rat",
			() -> EntityType.Builder.<RatEntity>of(RatEntity::new, MobCategory.MONSTER)
					.sized(1.2f, 1f)
					.clientTrackingRange(64)
					.updateInterval(3)
					.build("rat"));

	public static final RegistryHolder<EntityType<SkeletonMinionEntity>> SKELETON_MINION = RegistryHolder.entity(
			"skeleton_minion",
			() -> EntityType.Builder.<SkeletonMinionEntity>of(SkeletonMinionEntity::new, MobCategory.MONSTER)
					.sized(0.8f, 1.8f)
					.clientTrackingRange(64)
					.updateInterval(3)
					.build("skeleton_minion"));

	public static final RegistryHolder<EntityType<RemnantOssukageEntity>> REMNANT_OSSUKAGE = RegistryHolder.entity(
			"ossukage",
			() -> EntityType.Builder.<RemnantOssukageEntity>of(RemnantOssukageEntity::new, MobCategory.MONSTER)
					.sized(0.8f, 2.4f)
					.clientTrackingRange(128)
					.updateInterval(3)
					.build("ossukage"));

	public static final RegistryHolder<EntityType<OssukageRuneEffectEntity>> OSSUKAGE_RUNE_EFFECT = RegistryHolder.entity(
			"ossukage_rune_effect",
			() -> EntityType.Builder.<OssukageRuneEffectEntity>of(OssukageRuneEffectEntity::new, MobCategory.MISC)
					.sized(0.1f, 0.1f)
					.clientTrackingRange(128)
					.updateInterval(1)
					.noSave()
					.noSummon()
					.build("ossukage_rune_effect"));

	public static final RegistryHolder<EntityType<WraithEntity>> WRAITH = RegistryHolder.entity("wraith",
			() -> EntityType.Builder.<WraithEntity>of(WraithEntity::new, MobCategory.MONSTER)
					.sized(0.8f, 1.8f)
					.clientTrackingRange(64)
					.updateInterval(3)
					.build("wraith"));

	public static final RegistryHolder<EntityType<ArmoredGrubEntity>> ARMORED_GRUB = RegistryHolder.entity("armored_grub",
			() -> EntityType.Builder.<ArmoredGrubEntity>of(ArmoredGrubEntity::new, MobCategory.CREATURE)
					.sized(0.85f, 0.95f)
					.clientTrackingRange(64)
					.updateInterval(3)
					.build("armored_grub"));

	public static final RegistryHolder<EntityType<UmbrakarEntity>> UMBRAKAR = RegistryHolder.entity("umbrakar",
			() -> EntityType.Builder.<UmbrakarEntity>of(UmbrakarEntity::new, MobCategory.MONSTER)
					.sized(4.2f, 3.6f)
					.clientTrackingRange(128)
					.updateInterval(3)
					.fireImmune()
					.build("umbrakar"));

	public static final RegistryHolder<EntityType<UmbrakarOrbEntity>> UMBRAKAR_ORB = RegistryHolder.entity("umbrakar_orb",
			() -> EntityType.Builder.<UmbrakarOrbEntity>of(UmbrakarOrbEntity::new, MobCategory.MISC)
					.sized(0.8f, 0.8f)
					.clientTrackingRange(64)
					.updateInterval(1)
					.fireImmune()
					.build("umbrakar_orb"));

	public static final RegistryHolder<Item> RAT_SPAWN_EGG = new RegistryHolder<>("rat_spawn_egg");
	public static final RegistryHolder<Item> SKELETON_MINION_SPAWN_EGG = new RegistryHolder<>("skeleton_minion_spawn_egg");
	public static final RegistryHolder<Item> REMNANT_OSSUKAGE_SPAWN_EGG = new RegistryHolder<>("remnant_ossukage_spawn_egg");
	public static final RegistryHolder<Item> WRAITH_SPAWN_EGG = new RegistryHolder<>("wraith_spawn_egg");
	public static final RegistryHolder<Item> ARMORED_GRUB_SPAWN_EGG = new RegistryHolder<>("armored_grub_spawn_egg");
	public static final RegistryHolder<EntityType<KotsukageEntity>> KOTSUKAGE = RegistryHolder.entity("kotsukage",
			() -> EntityType.Builder.<KotsukageEntity>of(KotsukageEntity::new, MobCategory.MONSTER)
					.sized(1.6f, 3.8f)
					.clientTrackingRange(128)
					.updateInterval(3)
					.build("kotsukage"));

	public static final RegistryHolder<EntityType<KotsukageTrapEntity>> KOTSUKAGE_TRAP = RegistryHolder.entity(
			"kotsukage_trap",
			() -> EntityType.Builder.<KotsukageTrapEntity>of(KotsukageTrapEntity::new, MobCategory.MISC)
					.sized(1.8f, 1.2f)
					.clientTrackingRange(64)
					.updateInterval(1)
					.build("kotsukage_trap"));

	public static final RegistryHolder<Item> UMBRAKAR_SPAWN_EGG = new RegistryHolder<>("umbrakar_spawn_egg");
	public static final RegistryHolder<Item> KOTSUKAGE_SPAWN_EGG = new RegistryHolder<>("kotsukage_spawn_egg");

	private ModEntities() {
	}

	public static void init() {
		RatEntity.init();
		SkeletonMinionEntity.init();
		RemnantOssukageEntity.init();
		WraithEntity.init();
		ArmoredGrubEntity.init();
		UmbrakarEntity.init();
		UmbrakarOrbEntity.init();
		KotsukageEntity.init();
		KotsukageTrapEntity.init();
	}

	public static boolean canMonsterSpawn(EntityType<? extends Mob> entityType, ServerLevelAccessor world,
			net.minecraft.world.entity.MobSpawnType reason, BlockPos pos, RandomSource random) {
		return world.getDifficulty() != Difficulty.PEACEFUL
				&& Monster.isDarkEnoughToSpawn(world, pos, random)
				&& Mob.checkMobSpawnRules(entityType, world, reason, pos, random);
	}
}
