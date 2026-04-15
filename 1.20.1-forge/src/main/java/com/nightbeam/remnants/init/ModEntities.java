package com.nightbeam.remnants.init;

import com.nightbeam.remnants.entity.SkeletonMinionEntity;
import com.nightbeam.remnants.entity.RemnantOssukageEntity;
import com.nightbeam.remnants.entity.RatEntity;
import com.nightbeam.remnants.entity.KunaiEntity;
import com.nightbeam.remnants.entity.WraithEntity;
import com.nightbeam.remnants.entity.ArmoredGrubEntity;
import com.nightbeam.remnants.RemnantBossesMod;

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
import net.minecraft.world.item.Item;

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

	public static final RegistryObject<EntityType<ArmoredGrubEntity>> ARMORED_GRUB = registerEntity("armored_grub",
			EntityType.Builder.<ArmoredGrubEntity>of(ArmoredGrubEntity::new, MobCategory.CREATURE).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(ArmoredGrubEntity::new)
					.sized(0.85f, 0.95f));

	// Spawn eggs (registered as items)
	public static final RegistryObject<Item> RAT_SPAWN_EGG = SPAWN_EGGS.register("rat_spawn_egg",
			() -> new ForgeSpawnEggItem(RAT, 0xCC666B, 0xFF0000, new Item.Properties()));

	public static final RegistryObject<Item> SKELETON_MINION_SPAWN_EGG = SPAWN_EGGS.register("skeleton_minion_spawn_egg",
			() -> new ForgeSpawnEggItem(SKELETON_MINION, 0xFF8C8C, 0xFF0000, new Item.Properties()));

	public static final RegistryObject<Item> REMNANT_OSSUKAGE_SPAWN_EGG = SPAWN_EGGS.register("remnant_ossukage_spawn_egg",
			() -> new ForgeSpawnEggItem(REMNANT_OSSUKAGE, 0xCC0000, 0xFF0000, new Item.Properties()));

	public static final RegistryObject<Item> WRAITH_SPAWN_EGG = SPAWN_EGGS.register("wraith_spawn_egg",
			() -> new ForgeSpawnEggItem(WRAITH, 0x000000, 0xFFFFFF, new Item.Properties()));

	public static final RegistryObject<Item> ARMORED_GRUB_SPAWN_EGG = SPAWN_EGGS.register("armored_grub_spawn_egg",
			() -> new ForgeSpawnEggItem(ARMORED_GRUB, 0x4A7C00, 0x8B5E00, new Item.Properties()));

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
			ArmoredGrubEntity.init();
		});
	}

	@SubscribeEvent
	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(RAT.get(), RatEntity.createAttributes().build());
		event.put(SKELETON_MINION.get(), SkeletonMinionEntity.createAttributes().build());
		event.put(REMNANT_OSSUKAGE.get(), RemnantOssukageEntity.createAttributes().build());
		event.put(WRAITH.get(), WraithEntity.createAttributes().build());
		event.put(ARMORED_GRUB.get(), ArmoredGrubEntity.createAttributes().build());
	}
}