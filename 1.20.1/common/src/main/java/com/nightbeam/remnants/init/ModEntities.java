package com.nightbeam.remnants.init;

import com.nightbeam.remnants.entity.UmbrakarEntity;
import com.nightbeam.remnants.entity.UmbrakarOrbEntity;
import com.nightbeam.remnants.entity.ArmoredGrubEntity;
import com.nightbeam.remnants.entity.KunaiEntity;
import com.nightbeam.remnants.entity.RatEntity;
import com.nightbeam.remnants.entity.RemnantOssukageEntity;
import com.nightbeam.remnants.entity.SkeletonMinionEntity;
import com.nightbeam.remnants.entity.WraithEntity;
import com.nightbeam.remnants.registry.RegistryHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;

public final class ModEntities {
	public static final RegistryHolder<EntityType<KunaiEntity>> KUNAI = new RegistryHolder<>("kunai");
	public static final RegistryHolder<EntityType<RatEntity>> RAT = new RegistryHolder<>("rat");
	public static final RegistryHolder<EntityType<SkeletonMinionEntity>> SKELETON_MINION = new RegistryHolder<>("skeleton_minion");
	public static final RegistryHolder<EntityType<RemnantOssukageEntity>> REMNANT_OSSUKAGE = new RegistryHolder<>("remnant_ossukage");
	public static final RegistryHolder<EntityType<WraithEntity>> WRAITH = new RegistryHolder<>("wraith");
	public static final RegistryHolder<EntityType<ArmoredGrubEntity>> ARMORED_GRUB = new RegistryHolder<>("armored_grub");
	public static final RegistryHolder<EntityType<UmbrakarEntity>> UMBRAKAR = new RegistryHolder<>("umbrakar");
	public static final RegistryHolder<EntityType<UmbrakarOrbEntity>> UMBRAKAR_ORB = new RegistryHolder<>("umbrakar_orb");

	public static final RegistryHolder<Item> RAT_SPAWN_EGG = new RegistryHolder<>("rat_spawn_egg");
	public static final RegistryHolder<Item> SKELETON_MINION_SPAWN_EGG = new RegistryHolder<>("skeleton_minion_spawn_egg");
	public static final RegistryHolder<Item> REMNANT_OSSUKAGE_SPAWN_EGG = new RegistryHolder<>("remnant_ossukage_spawn_egg");
	public static final RegistryHolder<Item> WRAITH_SPAWN_EGG = new RegistryHolder<>("wraith_spawn_egg");
	public static final RegistryHolder<Item> ARMORED_GRUB_SPAWN_EGG = new RegistryHolder<>("armored_grub_spawn_egg");
	public static final RegistryHolder<Item> UMBRAKAR_SPAWN_EGG = new RegistryHolder<>("umbrakar_spawn_egg");

	private ModEntities() {
	}

	public static EntityType.Builder<KunaiEntity> createKunai() {
		return EntityType.Builder.<KunaiEntity>of(KunaiEntity::new, MobCategory.MISC)
				.sized(0.5f, 0.5f)
				.clientTrackingRange(64)
				.updateInterval(1);
	}

	public static EntityType.Builder<RatEntity> createRat() {
		return EntityType.Builder.<RatEntity>of(RatEntity::new, MobCategory.MONSTER)
				.sized(1.2f, 1f)
				.clientTrackingRange(64)
				.updateInterval(3);
	}

	public static EntityType.Builder<SkeletonMinionEntity> createSkeletonMinion() {
		return EntityType.Builder.<SkeletonMinionEntity>of(SkeletonMinionEntity::new, MobCategory.MONSTER)
				.sized(0.8f, 1.8f)
				.clientTrackingRange(64)
				.updateInterval(3);
	}

	public static EntityType.Builder<RemnantOssukageEntity> createRemnantOssukage() {
		return EntityType.Builder.<RemnantOssukageEntity>of(RemnantOssukageEntity::new, MobCategory.MONSTER)
				.sized(0.8f, 2.4f)
				.clientTrackingRange(128)
				.updateInterval(3);
	}

	public static EntityType.Builder<WraithEntity> createWraith() {
		return EntityType.Builder.<WraithEntity>of(WraithEntity::new, MobCategory.MONSTER)
				.sized(0.8f, 1.8f)
				.clientTrackingRange(64)
				.updateInterval(3);
	}

	public static EntityType.Builder<ArmoredGrubEntity> createArmoredGrub() {
		return EntityType.Builder.<ArmoredGrubEntity>of(ArmoredGrubEntity::new, MobCategory.CREATURE)
				.sized(0.85f, 0.95f)
				.clientTrackingRange(64)
				.updateInterval(3);
	}

	public static EntityType.Builder<UmbrakarEntity> createUmbrakar() {
		return EntityType.Builder.<UmbrakarEntity>of(UmbrakarEntity::new, MobCategory.MONSTER)
				.sized(4.2f, 3.6f)
				.clientTrackingRange(128)
				.updateInterval(3)
				.fireImmune();
	}

	public static EntityType.Builder<UmbrakarOrbEntity> createUmbrakarOrb() {
		return EntityType.Builder.<UmbrakarOrbEntity>of(UmbrakarOrbEntity::new, MobCategory.MISC)
				.sized(0.8f, 0.8f)
				.clientTrackingRange(64)
				.updateInterval(1)
				.fireImmune();
	}

	public static void initEntities() {
		RatEntity.init();
		SkeletonMinionEntity.init();
		RemnantOssukageEntity.init();
		WraithEntity.init();
		ArmoredGrubEntity.init();
		UmbrakarEntity.init();
		UmbrakarOrbEntity.init();
	}
}
