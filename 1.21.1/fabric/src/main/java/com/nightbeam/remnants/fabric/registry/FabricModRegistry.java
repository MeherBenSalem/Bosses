package com.nightbeam.remnants.fabric.registry;

import com.nightbeam.remnants.init.ModArmorMaterials;
import com.nightbeam.remnants.block.realm.EclipseWaygateBlockEntity;
import com.nightbeam.remnants.block.realm.GraceEmberBlockEntity;
import com.nightbeam.remnants.init.ModBlockEntities;
import com.nightbeam.remnants.init.ModBlocks;
import com.nightbeam.remnants.init.ModParticles;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.particles.SimpleParticleType;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.init.ModItems;
import com.nightbeam.remnants.init.ModSounds;
import com.nightbeam.remnants.init.ModTabs;
import com.nightbeam.remnants.registry.RegistryHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;

public final class FabricModRegistry {
	private FabricModRegistry() {
	}

	public static void register() {
		registerBlock(ModBlocks.ANCIENT_ALTAR);
		registerBlock(ModBlocks.ANCIENT_PEDESTAL);
		for (ModBlocks.Entry entry : ModBlocks.REALM_BLOCKS) registerBlock(entry.holder());
		ModBlockEntities.ECLIPSE_WAYGATE.bind(Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ModBlockEntities.ECLIPSE_WAYGATE.id(),
				FabricBlockEntityTypeBuilder.create(EclipseWaygateBlockEntity::new, ModBlocks.ECLIPSE_WAYGATE.get()).build()));
		ModBlockEntities.GRACE_EMBER.bind(Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ModBlockEntities.GRACE_EMBER.id(),
				FabricBlockEntityTypeBuilder.create(GraceEmberBlockEntity::new, ModBlocks.GRACE_EMBER.get()).build()));
		for (RegistryHolder<SimpleParticleType> particle : ModParticles.ALL) {
			particle.bind(Registry.register(BuiltInRegistries.PARTICLE_TYPE, particle.id(), ModParticles.create()));
		}

		// Armor items take the material's registry holder, so the material goes first.
		Holder.Reference<ArmorMaterial> regalia = Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL,
				ModArmorMaterials.REGALIA.id(), ModArmorMaterials.REGALIA.get());
		ModArmorMaterials.REGALIA.bindHolder(regalia);
		Holder.Reference<ArmorMaterial> warden = Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL,
				ModArmorMaterials.WARDEN.id(), ModArmorMaterials.WARDEN.get());
		ModArmorMaterials.WARDEN.bindHolder(warden);
		Holder.Reference<ArmorMaterial> archivist = Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL,
				ModArmorMaterials.ARCHIVIST.id(), ModArmorMaterials.ARCHIVIST.get());
		ModArmorMaterials.ARCHIVIST.bindHolder(archivist);

		for (RegistryHolder<Item> item : ModItems.ALL) registerItem(item);
		for (RegistryHolder<Item> item : ModItems.REALM_ITEMS) registerItem(item);

		registerEntity(ModEntities.KUNAI);
		registerEntity(ModEntities.REMNANT_OSSUKAGE);
		registerEntity(ModEntities.OSSUKAGE_RUNE_EFFECT);
		registerEntity(ModEntities.HOLLOW_SOVEREIGN);
		registerEntity(ModEntities.SOVEREIGN_PAULDRON);
		registerEntity(ModEntities.SPECTRAL_CRESCENT);
		registerEntity(ModEntities.ROYAL_BLADE_MARK);
		registerEntity(ModEntities.HOLLOW_SENTINEL);
		registerEntity(ModEntities.SPECTRAL_ROYAL_GUARD);
		registerEntity(ModEntities.HOLLOW_ARBALIST);
		registerEntity(ModEntities.FALLEN_STANDARD_BEARER);
		registerEntity(ModEntities.SOULBOUND_KNIGHT);
		registerEntity(ModEntities.GRAVEBOUND_WARDEN);
		registerEntity(ModEntities.WARDEN_CHAIN);
		registerEntity(ModEntities.CHAIN_ANCHOR);
		registerEntity(ModEntities.GRAVEBOUND_SOLDIER);
		registerEntity(ModEntities.CRYPT_LURKER);
		registerEntity(ModEntities.CATACOMB_RAT);
		registerEntity(ModEntities.BURIAL_GUARDIAN);
		registerEntity(ModEntities.FORGOTTEN_WATCHER);
		registerEntity(ModEntities.WATCHER_ILLUSION);
		registerEntity(ModEntities.MEMORY_BOLT);
		registerEntity(ModEntities.LOST_PILGRIM);
		registerEntity(ModEntities.MEMORY_WRAITH);
		registerEntity(ModEntities.SOUL_COLLECTOR);
		registerEntity(ModEntities.SOUL_WISP);
		registerEntity(ModEntities.ASHBOUND_CRAWLER);
		registerEntity(ModEntities.EMBER_SERVITOR);
		registerEntity(ModEntities.RUSTED_GUARDIAN);
		registerEntity(ModEntities.EMBER_BOLT);
		registerEntity(ModEntities.SHARDLING);
		registerEntity(ModEntities.ANCIENT_VESSEL);
		registerEntity(ModEntities.RELIC_STALKER);
		registerEntity(ModEntities.CRYSTAL_BOLT);

		registerSpawnEgg(ModEntities.REMNANT_OSSUKAGE_SPAWN_EGG, ModEntities.REMNANT_OSSUKAGE.get(), 0xCC0000, 0xFF0000);
		registerSpawnEgg(ModEntities.HOLLOW_SOVEREIGN_SPAWN_EGG, ModEntities.HOLLOW_SOVEREIGN.get(), ModEntities.SOVEREIGN_EGG_BASE, ModEntities.SOVEREIGN_EGG_SPOTS);
		registerSpawnEgg(ModEntities.HOLLOW_SENTINEL_SPAWN_EGG, ModEntities.HOLLOW_SENTINEL.get(), ModEntities.LEGION_EGG_BASE, ModEntities.LEGION_EGG_SPOTS);
		registerSpawnEgg(ModEntities.HOLLOW_ARBALIST_SPAWN_EGG, ModEntities.HOLLOW_ARBALIST.get(), ModEntities.LEGION_EGG_BASE, ModEntities.LEGION_EGG_SPOTS);
		registerSpawnEgg(ModEntities.FALLEN_STANDARD_BEARER_SPAWN_EGG, ModEntities.FALLEN_STANDARD_BEARER.get(), ModEntities.LEGION_EGG_BASE, ModEntities.LEGION_EGG_SPOTS);
		registerSpawnEgg(ModEntities.SOULBOUND_KNIGHT_SPAWN_EGG, ModEntities.SOULBOUND_KNIGHT.get(), ModEntities.LEGION_EGG_BASE, ModEntities.LEGION_EGG_SPOTS);
		registerSpawnEgg(ModEntities.GRAVEBOUND_WARDEN_SPAWN_EGG, ModEntities.GRAVEBOUND_WARDEN.get(), ModEntities.CRYPTBORN_EGG_BASE, ModEntities.WARDEN_EGG_SPOTS);
		registerSpawnEgg(ModEntities.GRAVEBOUND_SOLDIER_SPAWN_EGG, ModEntities.GRAVEBOUND_SOLDIER.get(), ModEntities.CRYPTBORN_EGG_BASE, ModEntities.CRYPTBORN_EGG_SPOTS);
		registerSpawnEgg(ModEntities.CRYPT_LURKER_SPAWN_EGG, ModEntities.CRYPT_LURKER.get(), ModEntities.CRYPTBORN_EGG_BASE, ModEntities.CRYPTBORN_EGG_SPOTS);
		registerSpawnEgg(ModEntities.CATACOMB_RAT_SPAWN_EGG, ModEntities.CATACOMB_RAT.get(), ModEntities.CRYPTBORN_EGG_BASE, ModEntities.CRYPTBORN_EGG_SPOTS);
		registerSpawnEgg(ModEntities.BURIAL_GUARDIAN_SPAWN_EGG, ModEntities.BURIAL_GUARDIAN.get(), ModEntities.CRYPTBORN_EGG_BASE, ModEntities.CRYPTBORN_EGG_SPOTS);
		registerSpawnEgg(ModEntities.FORGOTTEN_WATCHER_SPAWN_EGG, ModEntities.FORGOTTEN_WATCHER.get(), ModEntities.WATCHER_EGG_BASE, ModEntities.WATCHER_EGG_SPOTS);
		registerSpawnEgg(ModEntities.LOST_PILGRIM_SPAWN_EGG, ModEntities.LOST_PILGRIM.get(), ModEntities.SOULS_EGG_BASE, ModEntities.SOULS_EGG_SPOTS);
		registerSpawnEgg(ModEntities.MEMORY_WRAITH_SPAWN_EGG, ModEntities.MEMORY_WRAITH.get(), ModEntities.SOULS_EGG_BASE, ModEntities.SOULS_EGG_SPOTS);
		registerSpawnEgg(ModEntities.SOUL_COLLECTOR_SPAWN_EGG, ModEntities.SOUL_COLLECTOR.get(), ModEntities.SOULS_EGG_BASE, ModEntities.SOULS_EGG_SPOTS);
		registerSpawnEgg(ModEntities.ASHBOUND_CRAWLER_SPAWN_EGG, ModEntities.ASHBOUND_CRAWLER.get(), ModEntities.ASHBOUND_EGG_BASE, ModEntities.ASHBOUND_EGG_SPOTS);
		registerSpawnEgg(ModEntities.EMBER_SERVITOR_SPAWN_EGG, ModEntities.EMBER_SERVITOR.get(), ModEntities.ASHBOUND_EGG_BASE, ModEntities.ASHBOUND_EGG_SPOTS);
		registerSpawnEgg(ModEntities.RUSTED_GUARDIAN_SPAWN_EGG, ModEntities.RUSTED_GUARDIAN.get(), ModEntities.ASHBOUND_EGG_BASE, ModEntities.ASHBOUND_EGG_SPOTS);
		registerSpawnEgg(ModEntities.SHARDLING_SPAWN_EGG, ModEntities.SHARDLING.get(), ModEntities.RELICS_EGG_BASE, ModEntities.RELICS_EGG_SPOTS);
		registerSpawnEgg(ModEntities.ANCIENT_VESSEL_SPAWN_EGG, ModEntities.ANCIENT_VESSEL.get(), ModEntities.RELICS_EGG_BASE, ModEntities.RELICS_EGG_SPOTS);
		registerSpawnEgg(ModEntities.RELIC_STALKER_SPAWN_EGG, ModEntities.RELIC_STALKER.get(), ModEntities.RELICS_EGG_BASE, ModEntities.RELICS_EGG_SPOTS);

		for (RegistryHolder<SoundEvent> sound : ModSounds.ALL) registerSound(sound);

		CreativeModeTab tab = ModTabs.createTab();
		ModTabs.REMNANT_BOSSES_TAB.bind(Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ModTabs.REMNANT_BOSSES_TAB.id(), tab));
	}

	private static void registerBlock(RegistryHolder<Block> holder) {
		holder.bind(Registry.register(BuiltInRegistries.BLOCK, holder.id(), holder.get()));
	}

	private static void registerItem(RegistryHolder<Item> holder) {
		holder.bind(Registry.register(BuiltInRegistries.ITEM, holder.id(), holder.get()));
	}

	private static <T extends net.minecraft.world.entity.Entity> void registerEntity(RegistryHolder<EntityType<T>> holder) {
		holder.bind(Registry.register(BuiltInRegistries.ENTITY_TYPE, holder.id(), holder.get()));
	}

	private static void registerSpawnEgg(RegistryHolder<Item> holder, EntityType<? extends Mob> type, int background,
			int highlight) {
		holder.bind(Registry.register(BuiltInRegistries.ITEM, holder.id(),
				new SpawnEggItem(type, background, highlight, new Item.Properties())));
	}

	private static void registerSound(RegistryHolder<SoundEvent> holder) {
		holder.bind(Registry.register(BuiltInRegistries.SOUND_EVENT, holder.id(), holder.get()));
	}
}
