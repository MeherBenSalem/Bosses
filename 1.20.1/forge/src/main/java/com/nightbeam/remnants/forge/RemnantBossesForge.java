package com.nightbeam.remnants.forge;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.journal.BossHunts;
import com.nightbeam.remnants.RemnantBosses;
import com.nightbeam.remnants.config.JaumlConfigBootstrap;
import com.nightbeam.remnants.entity.HollowSovereignEntity;
import com.nightbeam.remnants.entity.SickleghastEntity;
import com.nightbeam.remnants.entity.ArmoredGrubEntity;
import com.nightbeam.remnants.entity.RemnantOssukageEntity;
import com.nightbeam.remnants.entity.FallenStandardBearerEntity;
import com.nightbeam.remnants.entity.HollowArbalistEntity;
import com.nightbeam.remnants.entity.HollowSentinelEntity;
import com.nightbeam.remnants.entity.SoulboundKnightEntity;
import com.nightbeam.remnants.entity.SovereignPauldronEntity;
import com.nightbeam.remnants.entity.SpectralRoyalGuardEntity;
import com.nightbeam.remnants.event.SovereignRelics;
import com.nightbeam.remnants.event.WardenRelics;
import com.nightbeam.remnants.entity.ForgottenWatcherEntity;
import com.nightbeam.remnants.entity.LostPilgrimEntity;
import com.nightbeam.remnants.entity.MemoryWraithEntity;
import com.nightbeam.remnants.entity.SoulCollectorEntity;
import com.nightbeam.remnants.entity.SoulWispEntity;
import com.nightbeam.remnants.entity.WatcherIllusionEntity;
import com.nightbeam.remnants.event.WatcherRelics;
import com.nightbeam.remnants.entity.BurialGuardianEntity;
import com.nightbeam.remnants.entity.CatacombRatEntity;
import com.nightbeam.remnants.entity.ChainAnchorEntity;
import com.nightbeam.remnants.entity.CryptLurkerEntity;
import com.nightbeam.remnants.entity.GraveboundSoldierEntity;
import com.nightbeam.remnants.entity.GraveboundWardenEntity;
import com.nightbeam.remnants.forge.network.ForgeNetwork;
import com.nightbeam.remnants.block.realm.EclipseWaygateBlockEntity;
import com.nightbeam.remnants.init.ModBlockEntities;
import com.nightbeam.remnants.init.ModBlocks;
import com.nightbeam.remnants.init.ModParticles;
import com.nightbeam.remnants.realm.RealmTravel;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.init.ModItems;
import com.nightbeam.remnants.init.ModSounds;
import com.nightbeam.remnants.init.ModTabs;
import com.nightbeam.remnants.platform.Services;
import com.nightbeam.remnants.registry.RegistryHolder;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;
import software.bernie.geckolib.GeckoLib;

import java.util.function.Supplier;

@Mod(Constants.MOD_ID)
public class RemnantBossesForge {
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Constants.MOD_ID);
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Constants.MOD_ID);
	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Constants.ENTITY_NAMESPACE);
	public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Constants.MOD_ID);
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Constants.MOD_ID);
	public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Constants.MOD_ID);
	public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Constants.MOD_ID);

	public RemnantBossesForge() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

		GeckoLib.initialize();
		Services.NETWORK = new ForgeNetwork();

		BLOCKS.register(modBus);
		ITEMS.register(modBus);
		ENTITIES.register(modBus);
		SOUNDS.register(modBus);
		BLOCK_ENTITIES.register(modBus);
		PARTICLES.register(modBus);
		TABS.register(modBus);

		bindRegistries();

		modBus.addListener(this::commonSetup);
		modBus.addListener(this::registerAttributes);
		modBus.addListener(this::registerSpawnPlacements);

		MinecraftForge.EVENT_BUS.register(ForgeGameEvents.class);
		MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, MissingMappingsEvent.class, ForgeRegistryAliases::onMissingMappings);
		MinecraftForge.EVENT_BUS.addListener(this::onServerTick);

		RemnantBosses.init();
		Constants.LOG.info("Loaded {} on Forge", Constants.MOD_NAME);
	}

	private void bindRegistries() {
		bindBlock(ModBlocks.ANCIENT_ALTAR, ModBlocks::createAltar);
		bindBlock(ModBlocks.ANCIENT_PEDESTAL, ModBlocks::createPedestal);
		for (ModBlocks.Entry entry : ModBlocks.REALM_BLOCKS) bindBlock(entry.holder(), entry.factory());
		BLOCK_ENTITIES.register(ModBlockEntities.ECLIPSE_WAYGATE.path(), () -> {
			BlockEntityType<EclipseWaygateBlockEntity> type = BlockEntityType.Builder.of(EclipseWaygateBlockEntity::new,
					ModBlocks.ECLIPSE_WAYGATE.get()).build(null);
			ModBlockEntities.ECLIPSE_WAYGATE.bind(type);
			return type;
		});
		for (RegistryHolder<SimpleParticleType> particle : ModParticles.ALL) {
			PARTICLES.register(particle.path(), () -> {
				SimpleParticleType type = ModParticles.create();
				particle.bind(type);
				return type;
			});
		}

		bindItem(ModItems.OSSUKAGE_SWORD, ModItems::createOssukageSword);
		bindItem(ModItems.BOSS_JOURNAL, ModItems::createBossJournal);
		bindItem(ModItems.ANCIENT_ALTAR, () -> ModItems.createBlockItem(ModBlocks.ANCIENT_ALTAR));
		bindItem(ModItems.ANCIENT_PEDESTAL, () -> ModItems.createBlockItem(ModBlocks.ANCIENT_PEDESTAL));
		for (ModItems.Entry entry : ModItems.SOVEREIGN_ITEMS) bindItem(entry.holder(), entry.factory());
		for (ModItems.Entry entry : ModItems.WARDEN_ITEMS) bindItem(entry.holder(), entry.factory());
		for (ModItems.Entry entry : ModItems.WATCHER_ITEMS) bindItem(entry.holder(), entry.factory());
		for (ModItems.Entry entry : ModItems.REALM_ITEMS) bindItem(entry.holder(), entry.factory());

		bindEntity(ModEntities.KUNAI, () -> ModEntities.createKunai().build(ModEntities.KUNAI.path()));
		bindEntity(ModEntities.REMNANT_OSSUKAGE, () -> ModEntities.createRemnantOssukage().build(ModEntities.REMNANT_OSSUKAGE.path()));
		bindEntity(ModEntities.OSSUKAGE_RUNE_EFFECT, () -> ModEntities.createOssukageRuneEffect().build(ModEntities.OSSUKAGE_RUNE_EFFECT.path()));
		bindEntity(ModEntities.ARMORED_GRUB, () -> ModEntities.createArmoredGrub().build(ModEntities.ARMORED_GRUB.path()));
		bindEntity(ModEntities.HOLLOW_SOVEREIGN, () -> ModEntities.createHollowSovereign().build(ModEntities.HOLLOW_SOVEREIGN.path()));
		bindEntity(ModEntities.SICKLEGHAST, () -> ModEntities.createSickleghast().build(ModEntities.SICKLEGHAST.path()));
		bindEntity(ModEntities.SOVEREIGN_PAULDRON, () -> ModEntities.createSovereignPauldron().build(ModEntities.SOVEREIGN_PAULDRON.path()));
		bindEntity(ModEntities.SPECTRAL_CRESCENT, () -> ModEntities.createSpectralCrescent().build(ModEntities.SPECTRAL_CRESCENT.path()));
		bindEntity(ModEntities.ROYAL_BLADE_MARK, () -> ModEntities.createRoyalBladeMark().build(ModEntities.ROYAL_BLADE_MARK.path()));
		bindEntity(ModEntities.HOLLOW_SENTINEL, () -> ModEntities.createHollowSentinel().build(ModEntities.HOLLOW_SENTINEL.path()));
		bindEntity(ModEntities.SPECTRAL_ROYAL_GUARD, () -> ModEntities.createSpectralRoyalGuard().build(ModEntities.SPECTRAL_ROYAL_GUARD.path()));
		bindEntity(ModEntities.HOLLOW_ARBALIST, () -> ModEntities.createHollowArbalist().build(ModEntities.HOLLOW_ARBALIST.path()));
		bindEntity(ModEntities.FALLEN_STANDARD_BEARER, () -> ModEntities.createFallenStandardBearer().build(ModEntities.FALLEN_STANDARD_BEARER.path()));
		bindEntity(ModEntities.SOULBOUND_KNIGHT, () -> ModEntities.createSoulboundKnight().build(ModEntities.SOULBOUND_KNIGHT.path()));
		bindEntity(ModEntities.GRAVEBOUND_WARDEN, () -> ModEntities.createGraveboundWarden().build(ModEntities.GRAVEBOUND_WARDEN.path()));
		bindEntity(ModEntities.WARDEN_CHAIN, () -> ModEntities.createWardenChain().build(ModEntities.WARDEN_CHAIN.path()));
		bindEntity(ModEntities.CHAIN_ANCHOR, () -> ModEntities.createChainAnchor().build(ModEntities.CHAIN_ANCHOR.path()));
		bindEntity(ModEntities.GRAVEBOUND_SOLDIER, () -> ModEntities.createGraveboundSoldier().build(ModEntities.GRAVEBOUND_SOLDIER.path()));
		bindEntity(ModEntities.CRYPT_LURKER, () -> ModEntities.createCryptLurker().build(ModEntities.CRYPT_LURKER.path()));
		bindEntity(ModEntities.CATACOMB_RAT, () -> ModEntities.createCatacombRat().build(ModEntities.CATACOMB_RAT.path()));
		bindEntity(ModEntities.BURIAL_GUARDIAN, () -> ModEntities.createBurialGuardian().build(ModEntities.BURIAL_GUARDIAN.path()));
		bindEntity(ModEntities.FORGOTTEN_WATCHER, () -> ModEntities.createForgottenWatcher().build(ModEntities.FORGOTTEN_WATCHER.path()));
		bindEntity(ModEntities.WATCHER_ILLUSION, () -> ModEntities.createWatcherIllusion().build(ModEntities.WATCHER_ILLUSION.path()));
		bindEntity(ModEntities.MEMORY_BOLT, () -> ModEntities.createMemoryBolt().build(ModEntities.MEMORY_BOLT.path()));
		bindEntity(ModEntities.LOST_PILGRIM, () -> ModEntities.createLostPilgrim().build(ModEntities.LOST_PILGRIM.path()));
		bindEntity(ModEntities.MEMORY_WRAITH, () -> ModEntities.createMemoryWraith().build(ModEntities.MEMORY_WRAITH.path()));
		bindEntity(ModEntities.SOUL_COLLECTOR, () -> ModEntities.createSoulCollector().build(ModEntities.SOUL_COLLECTOR.path()));
		bindEntity(ModEntities.SOUL_WISP, () -> ModEntities.createSoulWisp().build(ModEntities.SOUL_WISP.path()));

		bindSpawnEgg(ModEntities.REMNANT_OSSUKAGE_SPAWN_EGG, () -> ModEntities.REMNANT_OSSUKAGE.get(), 0xCC0000, 0xFF0000);
		bindSpawnEgg(ModEntities.ARMORED_GRUB_SPAWN_EGG, () -> ModEntities.ARMORED_GRUB.get(), 0x4A7C00, 0x8B5E00);
		bindSpawnEgg(ModEntities.HOLLOW_SOVEREIGN_SPAWN_EGG, () -> ModEntities.HOLLOW_SOVEREIGN.get(), ModEntities.SOVEREIGN_EGG_BASE, ModEntities.SOVEREIGN_EGG_SPOTS);
		bindSpawnEgg(ModEntities.SICKLEGHAST_SPAWN_EGG, () -> ModEntities.SICKLEGHAST.get(), 0x947360, 0x64CE8A);
		bindSpawnEgg(ModEntities.HOLLOW_SENTINEL_SPAWN_EGG, () -> ModEntities.HOLLOW_SENTINEL.get(), ModEntities.LEGION_EGG_BASE, ModEntities.LEGION_EGG_SPOTS);
		bindSpawnEgg(ModEntities.HOLLOW_ARBALIST_SPAWN_EGG, () -> ModEntities.HOLLOW_ARBALIST.get(), ModEntities.LEGION_EGG_BASE, ModEntities.LEGION_EGG_SPOTS);
		bindSpawnEgg(ModEntities.FALLEN_STANDARD_BEARER_SPAWN_EGG, () -> ModEntities.FALLEN_STANDARD_BEARER.get(), ModEntities.LEGION_EGG_BASE, ModEntities.LEGION_EGG_SPOTS);
		bindSpawnEgg(ModEntities.SOULBOUND_KNIGHT_SPAWN_EGG, () -> ModEntities.SOULBOUND_KNIGHT.get(), ModEntities.LEGION_EGG_BASE, ModEntities.LEGION_EGG_SPOTS);
		bindSpawnEgg(ModEntities.GRAVEBOUND_WARDEN_SPAWN_EGG, () -> ModEntities.GRAVEBOUND_WARDEN.get(), ModEntities.CRYPTBORN_EGG_BASE, ModEntities.WARDEN_EGG_SPOTS);
		bindSpawnEgg(ModEntities.GRAVEBOUND_SOLDIER_SPAWN_EGG, () -> ModEntities.GRAVEBOUND_SOLDIER.get(), ModEntities.CRYPTBORN_EGG_BASE, ModEntities.CRYPTBORN_EGG_SPOTS);
		bindSpawnEgg(ModEntities.CRYPT_LURKER_SPAWN_EGG, () -> ModEntities.CRYPT_LURKER.get(), ModEntities.CRYPTBORN_EGG_BASE, ModEntities.CRYPTBORN_EGG_SPOTS);
		bindSpawnEgg(ModEntities.CATACOMB_RAT_SPAWN_EGG, () -> ModEntities.CATACOMB_RAT.get(), ModEntities.CRYPTBORN_EGG_BASE, ModEntities.CRYPTBORN_EGG_SPOTS);
		bindSpawnEgg(ModEntities.BURIAL_GUARDIAN_SPAWN_EGG, () -> ModEntities.BURIAL_GUARDIAN.get(), ModEntities.CRYPTBORN_EGG_BASE, ModEntities.CRYPTBORN_EGG_SPOTS);
		bindSpawnEgg(ModEntities.FORGOTTEN_WATCHER_SPAWN_EGG, () -> ModEntities.FORGOTTEN_WATCHER.get(), ModEntities.WATCHER_EGG_BASE, ModEntities.WATCHER_EGG_SPOTS);
		bindSpawnEgg(ModEntities.LOST_PILGRIM_SPAWN_EGG, () -> ModEntities.LOST_PILGRIM.get(), ModEntities.SOULS_EGG_BASE, ModEntities.SOULS_EGG_SPOTS);
		bindSpawnEgg(ModEntities.MEMORY_WRAITH_SPAWN_EGG, () -> ModEntities.MEMORY_WRAITH.get(), ModEntities.SOULS_EGG_BASE, ModEntities.SOULS_EGG_SPOTS);
		bindSpawnEgg(ModEntities.SOUL_COLLECTOR_SPAWN_EGG, () -> ModEntities.SOUL_COLLECTOR.get(), ModEntities.SOULS_EGG_BASE, ModEntities.SOULS_EGG_SPOTS);

		bindSound(ModSounds.SKELETONFIGHT_THEME, ModSounds::createSkeletonFightTheme);
		bindSound(ModSounds.DASH_SFX, ModSounds::createDashSfx);
		bindSound(ModSounds.ARMORED_GRUB_AMBIENT, ModSounds::createArmoredGrubAmbient);
		bindSound(ModSounds.ARMORED_GRUB_DEATH, ModSounds::createArmoredGrubDeath);
		for (RegistryHolder<SoundEvent> sound : ModSounds.SOVEREIGN_SOUNDS) bindSound(sound, () -> ModSounds.create(sound));
		for (RegistryHolder<SoundEvent> sound : ModSounds.REALM_SOUNDS) bindSound(sound, () -> ModSounds.create(sound));

		TABS.register(ModTabs.REMNANT_BOSSES_TAB.path(), () -> {
			CreativeModeTab tab = ModTabs.createTab();
			ModTabs.REMNANT_BOSSES_TAB.bind(tab);
			return tab;
		});
	}

	private void commonSetup(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			ModEntities.initEntities();
			JaumlConfigBootstrap.initConfigs();
		});
	}

	private void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(ModEntities.REMNANT_OSSUKAGE.get(), RemnantOssukageEntity.createAttributes().build());
		event.put(ModEntities.ARMORED_GRUB.get(), ArmoredGrubEntity.createAttributes().build());
		event.put(ModEntities.HOLLOW_SOVEREIGN.get(), HollowSovereignEntity.createAttributes().build());
		event.put(ModEntities.SICKLEGHAST.get(), SickleghastEntity.createAttributes().build());
		event.put(ModEntities.SOVEREIGN_PAULDRON.get(), SovereignPauldronEntity.createAttributes().build());
		event.put(ModEntities.HOLLOW_SENTINEL.get(), HollowSentinelEntity.createAttributes().build());
		event.put(ModEntities.SPECTRAL_ROYAL_GUARD.get(), SpectralRoyalGuardEntity.createAttributes().build());
		event.put(ModEntities.HOLLOW_ARBALIST.get(), HollowArbalistEntity.createAttributes().build());
		event.put(ModEntities.FALLEN_STANDARD_BEARER.get(), FallenStandardBearerEntity.createAttributes().build());
		event.put(ModEntities.SOULBOUND_KNIGHT.get(), SoulboundKnightEntity.createAttributes().build());
		event.put(ModEntities.GRAVEBOUND_WARDEN.get(), GraveboundWardenEntity.createAttributes().build());
		event.put(ModEntities.CHAIN_ANCHOR.get(), ChainAnchorEntity.createAttributes().build());
		event.put(ModEntities.GRAVEBOUND_SOLDIER.get(), GraveboundSoldierEntity.createAttributes().build());
		event.put(ModEntities.CRYPT_LURKER.get(), CryptLurkerEntity.createAttributes().build());
		event.put(ModEntities.CATACOMB_RAT.get(), CatacombRatEntity.createAttributes().build());
		event.put(ModEntities.BURIAL_GUARDIAN.get(), BurialGuardianEntity.createAttributes().build());
		event.put(ModEntities.FORGOTTEN_WATCHER.get(), ForgottenWatcherEntity.createAttributes().build());
		event.put(ModEntities.WATCHER_ILLUSION.get(), WatcherIllusionEntity.createAttributes().build());
		event.put(ModEntities.LOST_PILGRIM.get(), LostPilgrimEntity.createAttributes().build());
		event.put(ModEntities.MEMORY_WRAITH.get(), MemoryWraithEntity.createAttributes().build());
		event.put(ModEntities.SOUL_COLLECTOR.get(), SoulCollectorEntity.createAttributes().build());
		event.put(ModEntities.SOUL_WISP.get(), SoulWispEntity.createAttributes().build());
	}

	private void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
		// The Hollow Legion has no biome spawns; these placements let the Hollow Citadel spawn_overrides work.
		event.register(ModEntities.HOLLOW_SENTINEL.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
		event.register(ModEntities.HOLLOW_ARBALIST.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
		event.register(ModEntities.FALLEN_STANDARD_BEARER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
		event.register(ModEntities.SOULBOUND_KNIGHT.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
		// The Cryptborn have no overworld biome spawns; placements let the Gravebound Catacombs'
		// spawn_overrides and the Eclipsed Realm's biome spawners place them.
		event.register(ModEntities.GRAVEBOUND_SOLDIER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
		event.register(ModEntities.CRYPT_LURKER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
		event.register(ModEntities.CATACOMB_RAT.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
		event.register(ModEntities.BURIAL_GUARDIAN.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
		// The Forgotten Souls: the Lantern Heights' and Gracewood's spawners and the Forgotten Observatory.
		event.register(ModEntities.LOST_PILGRIM.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
		event.register(ModEntities.MEMORY_WRAITH.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
		event.register(ModEntities.SOUL_COLLECTOR.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				ModEntities::canMonsterSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
	}

	private void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			RemnantBosses.tickWorkQueue();
			BossHunts.tick(event.getServer());
			SovereignRelics.serverTick(event.getServer());
			WardenRelics.serverTick(event.getServer());
			WatcherRelics.serverTick(event.getServer());
			RealmTravel.tick(event.getServer());
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
