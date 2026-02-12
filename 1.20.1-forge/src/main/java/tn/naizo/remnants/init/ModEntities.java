package tn.naizo.remnants.init;

import tn.naizo.remnants.entity.SkeletonMinionEntity;
import tn.naizo.remnants.entity.RemnantOssukageEntity;
import tn.naizo.remnants.entity.RatEntity;
import tn.naizo.remnants.entity.KunaiEntity;
import tn.naizo.remnants.entity.WraithEntity;
import tn.naizo.remnants.RemnantBossesMod;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.common.ForgeSpawnEggItem;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.Difficulty;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntities {
	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RemnantBossesMod.MODID);
	public static final DeferredRegister<Item> SPAWN_EGGS = DeferredRegister.create(ForgeRegistries.ITEMS, RemnantBossesMod.MODID);

	// Entity types
	public static final RegistryObject<EntityType<KunaiEntity>> KUNAI = registerEntity("kunai",
			EntityType.Builder.<KunaiEntity>of(KunaiEntity::new, MobCategory.MISC).setCustomClientFactory(KunaiEntity::new).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));

	public static final RegistryObject<EntityType<RatEntity>> RAT = registerEntity("rat",
			EntityType.Builder.<RatEntity>of(RatEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(RatEntity::new)
					.sized(1.2f, 1f));

	public static final RegistryObject<EntityType<SkeletonMinionEntity>> SKELETON_MINION = registerEntity("skeleton_minion",
			EntityType.Builder.<SkeletonMinionEntity>of(SkeletonMinionEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(SkeletonMinionEntity::new)
					.sized(0.8f, 1.8f));

	public static final RegistryObject<EntityType<RemnantOssukageEntity>> REMNANT_OSSUKAGE = registerEntity("remnant_ossukage",
			EntityType.Builder.<RemnantOssukageEntity>of(RemnantOssukageEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(3).setCustomClientFactory(RemnantOssukageEntity::new)
					.sized(0.8f, 2.4f));

	public static final RegistryObject<EntityType<WraithEntity>> WRAITH = registerEntity("wraith",
			EntityType.Builder.<WraithEntity>of(WraithEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(WraithEntity::new)
					.sized(0.8f, 1.8f));

	// Spawn eggs (registered as items)
	public static final RegistryObject<Item> RAT_SPAWN_EGG = SPAWN_EGGS.register("rat_spawn_egg",
			() -> new ForgeSpawnEggItem(RAT, 0xCC666B, 0xFF0000, new Item.Properties()));

	public static final RegistryObject<Item> SKELETON_MINION_SPAWN_EGG = SPAWN_EGGS.register("skeleton_minion_spawn_egg",
			() -> new ForgeSpawnEggItem(SKELETON_MINION, 0xFF8C8C, 0xFF0000, new Item.Properties()));

	public static final RegistryObject<Item> REMNANT_OSSUKAGE_SPAWN_EGG = SPAWN_EGGS.register("remnant_ossukage_spawn_egg",
			() -> new ForgeSpawnEggItem(REMNANT_OSSUKAGE, 0xCC0000, 0xFF0000, new Item.Properties()));

	public static final RegistryObject<Item> WRAITH_SPAWN_EGG = SPAWN_EGGS.register("wraith_spawn_egg",
			() -> new ForgeSpawnEggItem(WRAITH, 0x000000, 0xFFFFFF, new Item.Properties()));

	private static <T extends Entity> RegistryObject<EntityType<T>> registerEntity(String registryname, EntityType.Builder<T> entityTypeBuilder) {
		return ENTITIES.register(registryname, () -> (EntityType<T>) entityTypeBuilder.build(registryname));
	}

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			RatEntity.init();
			SkeletonMinionEntity.init();
			RemnantOssukageEntity.init();
			WraithEntity.init();

			// Register spawn placements
			SpawnPlacements.register(RAT.get(), SpawnPlacements.Type.MONSTER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					(entityType, world, reason, pos,
							random) -> (world.getDifficulty() != Difficulty.PEACEFUL
									&& Monster.isDarkEnoughToSpawn(world, pos, random)
									&& Mob.checkMobSpawnRules(entityType, world, reason, pos, random)));
			SpawnPlacements.register(SKELETON_MINION.get(), SpawnPlacements.Type.MONSTER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					(entityType, world, reason, pos,
							random) -> (world.getDifficulty() != Difficulty.PEACEFUL
									&& Monster.isDarkEnoughToSpawn(world, pos, random)
									&& Mob.checkMobSpawnRules(entityType, world, reason, pos, random)));
			SpawnPlacements.register(REMNANT_OSSUKAGE.get(), SpawnPlacements.Type.MONSTER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					(entityType, world, reason, pos,
							random) -> (world.getDifficulty() != Difficulty.PEACEFUL
									&& Monster.isDarkEnoughToSpawn(world, pos, random)
									&& Mob.checkMobSpawnRules(entityType, world, reason, pos, random)));
			SpawnPlacements.register(WRAITH.get(), SpawnPlacements.Type.MONSTER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					(entityType, world, reason, pos,
							random) -> (world.getDifficulty() != Difficulty.PEACEFUL
									&& Monster.isDarkEnoughToSpawn(world, pos, random)
									&& Mob.checkMobSpawnRules(entityType, world, reason, pos, random)));
		});
	}

	@SubscribeEvent
	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(RAT.get(), RatEntity.createAttributes().build());
		event.put(SKELETON_MINION.get(), SkeletonMinionEntity.createAttributes().build());
		event.put(REMNANT_OSSUKAGE.get(), RemnantOssukageEntity.createAttributes().build());
		event.put(WRAITH.get(), WraithEntity.createAttributes().build());
	}
}