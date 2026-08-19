package com.nightbeam.remnants.forge;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.RemnantBosses;
import com.nightbeam.remnants.config.JaumlConfigBootstrap;
import com.nightbeam.remnants.entity.UmbrakarEntity;
import com.nightbeam.remnants.entity.UmbrakarOrbEntity;
import com.nightbeam.remnants.entity.ArmoredGrubEntity;
import com.nightbeam.remnants.entity.RatEntity;
import com.nightbeam.remnants.entity.RemnantOssukageEntity;
import com.nightbeam.remnants.entity.SkeletonMinionEntity;
import com.nightbeam.remnants.entity.WraithEntity;
import com.nightbeam.remnants.forge.network.ForgeNetwork;
import com.nightbeam.remnants.forge.worldgen.ConfigurableSpawnBiomeModifier;
import com.nightbeam.remnants.init.ModBlocks;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.init.ModItems;
import com.nightbeam.remnants.init.ModSounds;
import com.nightbeam.remnants.init.ModTabs;
import com.nightbeam.remnants.platform.Services;
import com.nightbeam.remnants.registry.RegistryHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.GeckoLib;

import com.mojang.serialization.Codec;

import java.util.function.Supplier;

@Mod(Constants.MOD_ID)
public class RemnantBossesForge {
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Constants.MOD_ID);
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Constants.MOD_ID);
	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Constants.MOD_ID);
	public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Constants.MOD_ID);
	public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Constants.MOD_ID);
	public static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
			DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, Constants.MOD_ID);

	public RemnantBossesForge() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

		GeckoLib.initialize();
		Services.NETWORK = new ForgeNetwork();

		BLOCKS.register(modBus);
		ITEMS.register(modBus);
		ENTITIES.register(modBus);
		SOUNDS.register(modBus);
		TABS.register(modBus);
		BIOME_MODIFIER_SERIALIZERS.register(modBus);

		bindRegistries();

		modBus.addListener(this::commonSetup);
		modBus.addListener(this::registerAttributes);
		modBus.addListener(this::registerSpawnPlacements);

		MinecraftForge.EVENT_BUS.register(ForgeGameEvents.class);
		MinecraftForge.EVENT_BUS.addListener(this::onServerTick);

		RemnantBosses.init();
		Constants.LOG.info("Loaded {} on Forge", Constants.MOD_NAME);
	}

	private void bindRegistries() {
		bindBlock(ModBlocks.ANCIENT_ALTAR, ModBlocks::createAltar);
		bindBlock(ModBlocks.ANCIENT_PEDESTAL, ModBlocks::createPedestal);

		bindItem(ModItems.OSSUKAGE_SWORD, ModItems::createOssukageSword);
		bindItem(ModItems.RAT_FANG, ModItems::createRatFang);
		bindItem(ModItems.FANG_ON_A_STICK, ModItems::createFangOnAStick);
		bindItem(ModItems.OLD_SKELETON_BONE, ModItems::createOldSkeletonBone);
		bindItem(ModItems.OLD_SKELETON_HEAD, ModItems::createOldSkeletonHead);
		bindItem(ModItems.ANCIENT_ALTAR, () -> ModItems.createBlockItem(ModBlocks.ANCIENT_ALTAR));
		bindItem(ModItems.ANCIENT_PEDESTAL, () -> ModItems.createBlockItem(ModBlocks.ANCIENT_PEDESTAL));

		bindEntity(ModEntities.KUNAI, () -> ModEntities.createKunai().build(ModEntities.KUNAI.path()));
		bindEntity(ModEntities.RAT, () -> ModEntities.createRat().build(ModEntities.RAT.path()));
		bindEntity(ModEntities.SKELETON_MINION, () -> ModEntities.createSkeletonMinion().build(ModEntities.SKELETON_MINION.path()));
		bindEntity(ModEntities.REMNANT_OSSUKAGE, () -> ModEntities.createRemnantOssukage().build(ModEntities.REMNANT_OSSUKAGE.path()));
		bindEntity(ModEntities.WRAITH, () -> ModEntities.createWraith().build(ModEntities.WRAITH.path()));
		bindEntity(ModEntities.ARMORED_GRUB, () -> ModEntities.createArmoredGrub().build(ModEntities.ARMORED_GRUB.path()));
		bindEntity(ModEntities.UMBRAKAR, () -> ModEntities.createUmbrakar().build(ModEntities.UMBRAKAR.path()));
		bindEntity(ModEntities.UMBRAKAR_ORB, () -> ModEntities.createUmbrakarOrb().build(ModEntities.UMBRAKAR_ORB.path()));

		bindSpawnEgg(ModEntities.RAT_SPAWN_EGG, () -> ModEntities.RAT.get(), 0xCC666B, 0xFF0000);
		bindSpawnEgg(ModEntities.SKELETON_MINION_SPAWN_EGG, () -> ModEntities.SKELETON_MINION.get(), 0xFF8C8C, 0xFF0000);
		bindSpawnEgg(ModEntities.REMNANT_OSSUKAGE_SPAWN_EGG, () -> ModEntities.REMNANT_OSSUKAGE.get(), 0xCC0000, 0xFF0000);
		bindSpawnEgg(ModEntities.WRAITH_SPAWN_EGG, () -> ModEntities.WRAITH.get(), 0x000000, 0xFFFFFF);
		bindSpawnEgg(ModEntities.ARMORED_GRUB_SPAWN_EGG, () -> ModEntities.ARMORED_GRUB.get(), 0x4A7C00, 0x8B5E00);
		bindSpawnEgg(ModEntities.UMBRAKAR_SPAWN_EGG, () -> ModEntities.UMBRAKAR.get(), 0x3A1A4A, 0xC48CFF);

		bindSound(ModSounds.SKELETONFIGHT_THEME, ModSounds::createSkeletonFightTheme);
		bindSound(ModSounds.DASH_SFX, ModSounds::createDashSfx);
		bindSound(ModSounds.ARMORED_GRUB_AMBIENT, ModSounds::createArmoredGrubAmbient);
		bindSound(ModSounds.ARMORED_GRUB_DEATH, ModSounds::createArmoredGrubDeath);

		TABS.register(ModTabs.REMNANT_BOSSES_TAB.path(), () -> {
			CreativeModeTab tab = ModTabs.createTab();
			ModTabs.REMNANT_BOSSES_TAB.bind(tab);
			return tab;
		});

		BIOME_MODIFIER_SERIALIZERS.register("configurable_spawn", () -> ConfigurableSpawnBiomeModifier.CODEC);
	}

	private void commonSetup(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			ModEntities.initEntities();
			JaumlConfigBootstrap.initConfigs();
		});
	}

	private void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(ModEntities.RAT.get(), RatEntity.createAttributes().build());
		event.put(ModEntities.SKELETON_MINION.get(), SkeletonMinionEntity.createAttributes().build());
		event.put(ModEntities.REMNANT_OSSUKAGE.get(), RemnantOssukageEntity.createAttributes().build());
		event.put(ModEntities.WRAITH.get(), WraithEntity.createAttributes().build());
		event.put(ModEntities.ARMORED_GRUB.get(), ArmoredGrubEntity.createAttributes().build());
		event.put(ModEntities.UMBRAKAR.get(), UmbrakarEntity.createAttributes().build());
		event.put(ModEntities.UMBRAKAR_ORB.get(), UmbrakarOrbEntity.createAttributes().build());
	}

	private void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
		event.register(ModEntities.RAT.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, reason, pos, random) ->
						world.getDifficulty() != Difficulty.PEACEFUL
								&& Monster.isDarkEnoughToSpawn(world, pos, random)
								&& Mob.checkMobSpawnRules(entityType, world, reason, pos, random),
				SpawnPlacementRegisterEvent.Operation.REPLACE);

		event.register(ModEntities.SKELETON_MINION.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, reason, pos, random) ->
						world.getDifficulty() != Difficulty.PEACEFUL
								&& Monster.isDarkEnoughToSpawn(world, pos, random)
								&& Mob.checkMobSpawnRules(entityType, world, reason, pos, random),
				SpawnPlacementRegisterEvent.Operation.REPLACE);

		event.register(ModEntities.WRAITH.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, reason, pos, random) ->
						world.getDifficulty() != Difficulty.PEACEFUL
								&& Monster.isDarkEnoughToSpawn(world, pos, random)
								&& Mob.checkMobSpawnRules(entityType, world, reason, pos, random),
				SpawnPlacementRegisterEvent.Operation.REPLACE);
	}

	private void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			RemnantBosses.tickWorkQueue();
		}
	}

	private static void bindBlock(RegistryHolder<Block> holder, Supplier<Block> factory) {
		BLOCKS.register(holder.path(), () -> {
			Block value = factory.get();
			holder.bind(value);
			return value;
		});
	}

	private static void bindItem(RegistryHolder<Item> holder, Supplier<Item> factory) {
		ITEMS.register(holder.path(), () -> {
			Item value = factory.get();
			holder.bind(value);
			return value;
		});
	}

	private static <T extends net.minecraft.world.entity.Entity> void bindEntity(RegistryHolder<EntityType<T>> holder, Supplier<EntityType<T>> factory) {
		ENTITIES.register(holder.path(), () -> {
			EntityType<T> value = factory.get();
			holder.bind(value);
			return value;
		});
	}

	private static void bindSound(RegistryHolder<SoundEvent> holder, Supplier<SoundEvent> factory) {
		SOUNDS.register(holder.path(), () -> {
			SoundEvent value = factory.get();
			holder.bind(value);
			return value;
		});
	}

	private static void bindSpawnEgg(RegistryHolder<Item> holder, Supplier<? extends EntityType<? extends Mob>> type, int background, int highlight) {
		ITEMS.register(holder.path(), () -> {
			Item value = new ForgeSpawnEggItem(type, background, highlight, new Item.Properties());
			holder.bind(value);
			return value;
		});
	}
}
