import tn.naizo.remnants.entity.SkeletonMinionEntity;
import tn.naizo.remnants.entity.RemnantOssukageEntity;
import tn.naizo.remnants.entity.RatEntity;
import tn.naizo.remnants.entity.KunaiEntity;
import tn.naizo.remnants.RemnantBossesMod;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.core.registries.BuiltInRegistries;

public class ModEntities {
	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPES, RemnantBossesMod.MODID);
	public static final DeferredRegister<Item> SPAWN_EGGS = DeferredRegister.create(BuiltInRegistries.ITEMS, RemnantBossesMod.MODID);

	// Entity types
	public static final DeferredHolder<EntityType<KunaiEntity>> KUNAI = registerEntity("kunai",
			EntityType.Builder.<KunaiEntity>of(KunaiEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));

	public static final DeferredHolder<EntityType<RatEntity>> RAT = registerEntity("rat",
			EntityType.Builder.<RatEntity>of(RatEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)
					.sized(1.2f, 1f));

	public static final DeferredHolder<EntityType<SkeletonMinionEntity>> SKELETON_MINION = registerEntity("skeleton_minion",
			EntityType.Builder.<SkeletonMinionEntity>of(SkeletonMinionEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)
					.sized(0.8f, 1.8f));

	public static final DeferredHolder<EntityType<RemnantOssukageEntity>> REMNANT_OSSUKAGE = registerEntity("remnant_ossukage",
			EntityType.Builder.<RemnantOssukageEntity>of(RemnantOssukageEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(128).setUpdateInterval(3)
					.sized(0.8f, 2.4f));

	// Spawn eggs (registered as items)
	public static final DeferredHolder<Item> RAT_SPAWN_EGG = SPAWN_EGGS.register("rat_spawn_egg",
			() -> new SpawnEggItem(RAT, 0xCC666B, 0xFF0000, new Item.Properties()));

	public static final DeferredHolder<Item> SKELETON_MINION_SPAWN_EGG = SPAWN_EGGS.register("skeleton_minion_spawn_egg",
			() -> new SpawnEggItem(SKELETON_MINION, 0xFF8C8C, 0xFF0000, new Item.Properties()));

	public static final DeferredHolder<Item> REMNANT_OSSUKAGE_SPAWN_EGG = SPAWN_EGGS.register("remnant_ossukage_spawn_egg",
			() -> new SpawnEggItem(REMNANT_OSSUKAGE, 0xCC0000, 0xFF0000, new Item.Properties()));

	private static <T extends Entity> DeferredHolder<EntityType<T>> registerEntity(String registryname, EntityType.Builder<T> entityTypeBuilder) {
		return ENTITIES.register(registryname, () -> (EntityType<T>) entityTypeBuilder.build(registryname));
	}

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			RatEntity.init();
			SkeletonMinionEntity.init();
			RemnantOssukageEntity.init();
		});
	}

	@SubscribeEvent
	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(RAT.get(), RatEntity.createAttributes().build());
		event.put(SKELETON_MINION.get(), SkeletonMinionEntity.createAttributes().build());
		event.put(REMNANT_OSSUKAGE.get(), RemnantOssukageEntity.createAttributes().build());
	}
}
