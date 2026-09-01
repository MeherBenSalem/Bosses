package com.nightbeam.remnants.fabric;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.RemnantBosses;
import com.nightbeam.remnants.config.JaumlConfigBootstrap;
import com.nightbeam.remnants.config.JaumlConfigLib;
import com.nightbeam.remnants.entity.UmbrakarEntity;
import com.nightbeam.remnants.entity.UmbrakarOrbEntity;
import com.nightbeam.remnants.entity.ArmoredGrubEntity;
import com.nightbeam.remnants.entity.KotsukageEntity;
import com.nightbeam.remnants.entity.KotsukageTrapEntity;
import com.nightbeam.remnants.entity.RatEntity;
import com.nightbeam.remnants.entity.RemnantOssukageEntity;
import com.nightbeam.remnants.entity.SkeletonArcherEntity;
import com.nightbeam.remnants.entity.SkeletonMeleeEntity;
import com.nightbeam.remnants.entity.SkeletonMinionEntity;
import com.nightbeam.remnants.entity.WraithEntity;
import com.nightbeam.remnants.event.GameEvents;
import com.nightbeam.remnants.fabric.network.FabricNetwork;
import com.nightbeam.remnants.init.ModBlocks;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.init.ModItems;
import com.nightbeam.remnants.init.ModSounds;
import com.nightbeam.remnants.init.ModTabs;
import com.nightbeam.remnants.platform.Services;
import com.nightbeam.remnants.registry.RegistryHolder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.sounds.SoundEvent;
import software.bernie.geckolib.GeckoLib;

public class RemnantBossesFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		GeckoLib.initialize();
		Services.NETWORK = new FabricNetwork();

		registerContent();
		registerAttributes();
		registerSpawnPlacements();
		registerBiomeSpawns();
		registerEvents();

		ModEntities.initEntities();
		JaumlConfigBootstrap.initConfigs();
		RemnantBosses.init();
		Constants.LOG.info("Loaded {} on Fabric", Constants.MOD_NAME);
	}

	private void registerContent() {
		registerBlock(ModBlocks.ANCIENT_ALTAR, ModBlocks.createAltar());
		registerBlock(ModBlocks.ANCIENT_PEDESTAL, ModBlocks.createPedestal());

		registerItem(ModItems.OSSUKAGE_SWORD, ModItems.createOssukageSword());
		registerItem(ModItems.RAT_FANG, ModItems.createRatFang());
		registerItem(ModItems.FANG_ON_A_STICK, ModItems.createFangOnAStick());
		registerItem(ModItems.OLD_SKELETON_BONE, ModItems.createOldSkeletonBone());
		registerItem(ModItems.OLD_SKELETON_HEAD, ModItems.createOldSkeletonHead());
		registerItem(ModItems.ANCIENT_ALTAR, ModItems.createBlockItem(ModBlocks.ANCIENT_ALTAR));
		registerItem(ModItems.ANCIENT_PEDESTAL, ModItems.createBlockItem(ModBlocks.ANCIENT_PEDESTAL));

		registerEntity(ModEntities.KUNAI, ModEntities.createKunai().build(ModEntities.KUNAI.path()));
		registerEntity(ModEntities.RAT, ModEntities.createRat().build(ModEntities.RAT.path()));
		registerEntity(ModEntities.SKELETON_MINION, ModEntities.createSkeletonMinion().build(ModEntities.SKELETON_MINION.path()));
		registerEntity(ModEntities.REMNANT_OSSUKAGE, ModEntities.createRemnantOssukage().build(ModEntities.REMNANT_OSSUKAGE.path()));
		registerEntity(ModEntities.OSSUKAGE_RUNE_EFFECT, ModEntities.createOssukageRuneEffect().build(ModEntities.OSSUKAGE_RUNE_EFFECT.path()));
		registerEntity(ModEntities.WRAITH, ModEntities.createWraith().build(ModEntities.WRAITH.path()));
		registerEntity(ModEntities.ARMORED_GRUB, ModEntities.createArmoredGrub().build(ModEntities.ARMORED_GRUB.path()));
		registerEntity(ModEntities.UMBRAKAR, ModEntities.createUmbrakar().build(ModEntities.UMBRAKAR.path()));
		registerEntity(ModEntities.UMBRAKAR_ORB, ModEntities.createUmbrakarOrb().build(ModEntities.UMBRAKAR_ORB.path()));
		registerEntity(ModEntities.KOTSUKAGE, ModEntities.createKotsukage().build(ModEntities.KOTSUKAGE.path()));
		registerEntity(ModEntities.KOTSUKAGE_TRAP, ModEntities.createKotsukageTrap().build(ModEntities.KOTSUKAGE_TRAP.path()));
		registerEntity(ModEntities.SKELETON_MELEE, ModEntities.createSkeletonMelee().build(ModEntities.SKELETON_MELEE.path()));
		registerEntity(ModEntities.SKELETON_ARCHER, ModEntities.createSkeletonArcher().build(ModEntities.SKELETON_ARCHER.path()));

		registerItem(ModEntities.RAT_SPAWN_EGG, new SpawnEggItem(ModEntities.RAT.get(), 0xCC666B, 0xFF0000, new Item.Properties()));
		registerItem(ModEntities.SKELETON_MINION_SPAWN_EGG, new SpawnEggItem(ModEntities.SKELETON_MINION.get(), 0xFF8C8C, 0xFF0000, new Item.Properties()));
		registerItem(ModEntities.REMNANT_OSSUKAGE_SPAWN_EGG, new SpawnEggItem(ModEntities.REMNANT_OSSUKAGE.get(), 0xCC0000, 0xFF0000, new Item.Properties()));
		registerItem(ModEntities.WRAITH_SPAWN_EGG, new SpawnEggItem(ModEntities.WRAITH.get(), 0x000000, 0xFFFFFF, new Item.Properties()));
		registerItem(ModEntities.ARMORED_GRUB_SPAWN_EGG, new SpawnEggItem(ModEntities.ARMORED_GRUB.get(), 0x4A7C00, 0x8B5E00, new Item.Properties()));
		registerItem(ModEntities.UMBRAKAR_SPAWN_EGG, new SpawnEggItem(ModEntities.UMBRAKAR.get(), 0x3A1A4A, 0xC48CFF, new Item.Properties()));
		registerItem(ModEntities.KOTSUKAGE_SPAWN_EGG, new SpawnEggItem(ModEntities.KOTSUKAGE.get(), 0xC4B59A, 0x3A7A3A, new Item.Properties()));
		registerItem(ModEntities.SKELETON_MELEE_SPAWN_EGG, new SpawnEggItem(ModEntities.SKELETON_MELEE.get(), 0xC8B89A, 0x5A2020, new Item.Properties()));
		registerItem(ModEntities.SKELETON_ARCHER_SPAWN_EGG, new SpawnEggItem(ModEntities.SKELETON_ARCHER.get(), 0xC8B89A, 0x6B4A2A, new Item.Properties()));

		registerSound(ModSounds.SKELETONFIGHT_THEME, ModSounds.createSkeletonFightTheme());
		registerSound(ModSounds.DASH_SFX, ModSounds.createDashSfx());
		registerSound(ModSounds.ARMORED_GRUB_AMBIENT, ModSounds.createArmoredGrubAmbient());
		registerSound(ModSounds.ARMORED_GRUB_DEATH, ModSounds.createArmoredGrubDeath());

		CreativeModeTab tab = ModTabs.createTab();
		ModTabs.REMNANT_BOSSES_TAB.bind(Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ModTabs.REMNANT_BOSSES_TAB.id(), tab));
	}

	private void registerAttributes() {
		FabricDefaultAttributeRegistry.register(ModEntities.RAT.get(), RatEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.SKELETON_MINION.get(), SkeletonMinionEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.REMNANT_OSSUKAGE.get(), RemnantOssukageEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.WRAITH.get(), WraithEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.ARMORED_GRUB.get(), ArmoredGrubEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.UMBRAKAR.get(), UmbrakarEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.UMBRAKAR_ORB.get(), UmbrakarOrbEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.KOTSUKAGE.get(), KotsukageEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.KOTSUKAGE_TRAP.get(), KotsukageTrapEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.SKELETON_MELEE.get(), SkeletonMeleeEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.SKELETON_ARCHER.get(), SkeletonArcherEntity.createAttributes());
	}

	private void registerSpawnPlacements() {
		SpawnPlacements.register(ModEntities.RAT.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, reason, pos, random) ->
						world.getDifficulty() != Difficulty.PEACEFUL
								&& Monster.isDarkEnoughToSpawn(world, pos, random)
								&& Mob.checkMobSpawnRules(entityType, world, reason, pos, random));
		SpawnPlacements.register(ModEntities.SKELETON_MINION.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, reason, pos, random) ->
						world.getDifficulty() != Difficulty.PEACEFUL
								&& Monster.isDarkEnoughToSpawn(world, pos, random)
								&& Mob.checkMobSpawnRules(entityType, world, reason, pos, random));
		SpawnPlacements.register(ModEntities.WRAITH.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, reason, pos, random) ->
						world.getDifficulty() != Difficulty.PEACEFUL
								&& Monster.isDarkEnoughToSpawn(world, pos, random)
								&& Mob.checkMobSpawnRules(entityType, world, reason, pos, random));
		SpawnPlacements.register(ModEntities.SKELETON_MELEE.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, reason, pos, random) ->
						world.getDifficulty() != Difficulty.PEACEFUL
								&& Monster.isDarkEnoughToSpawn(world, pos, random)
								&& Mob.checkMobSpawnRules(entityType, world, reason, pos, random));
		SpawnPlacements.register(ModEntities.SKELETON_ARCHER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, reason, pos, random) ->
						world.getDifficulty() != Difficulty.PEACEFUL
								&& Monster.isDarkEnoughToSpawn(world, pos, random)
								&& Mob.checkMobSpawnRules(entityType, world, reason, pos, random));
	}

	private void registerBiomeSpawns() {
		addConfigurableSpawn("rat_spawns", ModEntities.RAT.get());
		addConfigurableSpawn("wraith_spawns", ModEntities.WRAITH.get());
	}

	private void addConfigurableSpawn(String configFile, EntityType<? extends Mob> type) {
		if (JaumlConfigLib.getNumberValue("remnant/spawning", configFile, "enable_natural_spawning") <= 0) {
			return;
		}

		int weight = (int) JaumlConfigLib.getNumberValue("remnant/spawning", configFile, "spawn_weight");
		int min = (int) JaumlConfigLib.getNumberValue("remnant/spawning", configFile, "min_group_size");
		int max = (int) JaumlConfigLib.getNumberValue("remnant/spawning", configFile, "max_group_size");

		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld().and(context -> {
					String biomeKey = context.getBiomeRegistryEntry().unwrapKey()
							.map(k -> k.location().toString())
							.orElse("");
					return !JaumlConfigLib.getStringListValue("remnant/spawning", configFile, "biome_blacklist").contains(biomeKey);
				}),
				MobCategory.MONSTER,
				type,
				weight,
				min,
				max);
	}

	private void registerEvents() {
		ServerTickEvents.END_SERVER_TICK.register(server -> RemnantBosses.tickWorkQueue());

		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (!GameEvents.onEntityJoin(entity, world)) {
				entity.discard();
			}
		});

		ServerLivingEntityEvents.AFTER_DEATH.register(GameEvents::onLivingDeath);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			GameEvents.onLivingHurt(entity, source, amount);
			return true;
		});

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (GameEvents.onRightClickBlock(player, world, hitResult.getBlockPos(), world.getBlockState(hitResult.getBlockPos()).getBlock())) {
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});
	}

	private static void registerBlock(RegistryHolder<Block> holder, Block value) {
		holder.bind(Registry.register(BuiltInRegistries.BLOCK, holder.id(), value));
	}

	private static void registerItem(RegistryHolder<Item> holder, Item value) {
		holder.bind(Registry.register(BuiltInRegistries.ITEM, holder.id(), value));
	}

	private static <T extends net.minecraft.world.entity.Entity> void registerEntity(RegistryHolder<EntityType<T>> holder, EntityType<T> value) {
		holder.bind(Registry.register(BuiltInRegistries.ENTITY_TYPE, holder.id(), value));
	}

	private static void registerSound(RegistryHolder<SoundEvent> holder, SoundEvent value) {
		holder.bind(Registry.register(BuiltInRegistries.SOUND_EVENT, holder.id(), value));
	}
}
