package com.nightbeam.remnants.init;

import com.nightbeam.remnants.entity.HollowSovereignEntity;
import com.nightbeam.remnants.entity.SickleghastEntity;
import com.nightbeam.remnants.entity.ArmoredGrubEntity;
import com.nightbeam.remnants.entity.KunaiEntity;
import com.nightbeam.remnants.entity.RemnantOssukageEntity;
import com.nightbeam.remnants.entity.OssukageRuneEffectEntity;
import com.nightbeam.remnants.entity.FallenStandardBearerEntity;
import com.nightbeam.remnants.entity.HollowArbalistEntity;
import com.nightbeam.remnants.entity.HollowSentinelEntity;
import com.nightbeam.remnants.entity.RoyalBladeMarkEntity;
import com.nightbeam.remnants.entity.SoulboundKnightEntity;
import com.nightbeam.remnants.entity.SovereignPauldronEntity;
import com.nightbeam.remnants.entity.SpectralCrescentEntity;
import com.nightbeam.remnants.entity.SpectralRoyalGuardEntity;
import com.nightbeam.remnants.entity.BurialGuardianEntity;
import com.nightbeam.remnants.entity.CatacombRatEntity;
import com.nightbeam.remnants.entity.ChainAnchorEntity;
import com.nightbeam.remnants.entity.CryptLurkerEntity;
import com.nightbeam.remnants.entity.GraveboundSoldierEntity;
import com.nightbeam.remnants.entity.GraveboundWardenEntity;
import com.nightbeam.remnants.entity.WardenChainEntity;
import com.nightbeam.remnants.entity.ForgottenWatcherEntity;
import com.nightbeam.remnants.entity.LostPilgrimEntity;
import com.nightbeam.remnants.entity.MemoryBoltEntity;
import com.nightbeam.remnants.entity.MemoryWraithEntity;
import com.nightbeam.remnants.entity.SoulCollectorEntity;
import com.nightbeam.remnants.entity.SoulWispEntity;
import com.nightbeam.remnants.entity.WatcherIllusionEntity;
import com.nightbeam.remnants.registry.RegistryHolder;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.util.RandomSource;
import net.minecraft.core.BlockPos;

public final class ModEntities {
    public static final RegistryHolder<EntityType<SickleghastEntity>> SICKLEGHAST = RegistryHolder.entity("sickleghast", () -> EntityType.Builder.<SickleghastEntity>of(SickleghastEntity::new, MobCategory.MONSTER).sized(0.9f,2.65f).clientTrackingRange(64).updateInterval(2).build("sickleghast"));
    public static final RegistryHolder<Item> SICKLEGHAST_SPAWN_EGG = new RegistryHolder<>("sickleghast_spawn_egg");
	public static final RegistryHolder<EntityType<HollowSovereignEntity>> HOLLOW_SOVEREIGN = RegistryHolder.entity("hollow_sovereign", () -> EntityType.Builder.<HollowSovereignEntity>of(HollowSovereignEntity::new, MobCategory.MONSTER).sized(1.8f,4.0f).eyeHeight(3.5f).clientTrackingRange(96).updateInterval(2).build("hollow_sovereign"));
	public static final RegistryHolder<EntityType<KunaiEntity>> KUNAI = RegistryHolder.entity("kunai",
			() -> EntityType.Builder.<KunaiEntity>of(KunaiEntity::new, MobCategory.MISC)
					.sized(0.5f, 0.5f)
					.clientTrackingRange(64)
					.updateInterval(1)
					.build("kunai"));

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

	public static final RegistryHolder<EntityType<ArmoredGrubEntity>> ARMORED_GRUB = RegistryHolder.entity("armored_grub",
			() -> EntityType.Builder.<ArmoredGrubEntity>of(ArmoredGrubEntity::new, MobCategory.CREATURE)
					.sized(0.85f, 0.95f)
					.clientTrackingRange(64)
					.updateInterval(3)
					.build("armored_grub"));

	public static final RegistryHolder<Item> REMNANT_OSSUKAGE_SPAWN_EGG = new RegistryHolder<>("remnant_ossukage_spawn_egg");
	public static final RegistryHolder<Item> ARMORED_GRUB_SPAWN_EGG = new RegistryHolder<>("armored_grub_spawn_egg");
	public static final RegistryHolder<Item> HOLLOW_SOVEREIGN_SPAWN_EGG = new RegistryHolder<>("hollow_sovereign_spawn_egg");

	// The Hollow Sovereign's helpers and the Hollow Legion.
	public static final RegistryHolder<EntityType<SovereignPauldronEntity>> SOVEREIGN_PAULDRON = RegistryHolder.entity(
			"sovereign_pauldron",
			() -> EntityType.Builder.<SovereignPauldronEntity>of(SovereignPauldronEntity::new, MobCategory.MONSTER)
					.sized(0.9f, 0.8f)
					.fireImmune()
					.clientTrackingRange(10)
					.updateInterval(1)
					.build("sovereign_pauldron"));

	public static final RegistryHolder<EntityType<SpectralCrescentEntity>> SPECTRAL_CRESCENT = RegistryHolder.entity(
			"spectral_crescent",
			() -> EntityType.Builder.<SpectralCrescentEntity>of(SpectralCrescentEntity::new, MobCategory.MISC)
					.sized(1.6f, 0.4f)
					.fireImmune()
					.noSave()
					.clientTrackingRange(8)
					.updateInterval(2)
					.build("spectral_crescent"));

	public static final RegistryHolder<EntityType<RoyalBladeMarkEntity>> ROYAL_BLADE_MARK = RegistryHolder.entity(
			"royal_blade_mark",
			() -> EntityType.Builder.<RoyalBladeMarkEntity>of(RoyalBladeMarkEntity::new, MobCategory.MISC)
					.sized(0.5f, 0.1f)
					.fireImmune()
					.noSave()
					.noSummon()
					.clientTrackingRange(8)
					.updateInterval(20)
					.build("royal_blade_mark"));

	public static final RegistryHolder<EntityType<HollowSentinelEntity>> HOLLOW_SENTINEL = RegistryHolder.entity(
			"hollow_sentinel",
			() -> EntityType.Builder.<HollowSentinelEntity>of(HollowSentinelEntity::new, MobCategory.MONSTER)
					.sized(0.8f, 2.4f)
					.clientTrackingRange(10)
					.updateInterval(3)
					.build("hollow_sentinel"));

	public static final RegistryHolder<EntityType<SpectralRoyalGuardEntity>> SPECTRAL_ROYAL_GUARD = RegistryHolder.entity(
			"spectral_royal_guard",
			() -> EntityType.Builder.<SpectralRoyalGuardEntity>of(SpectralRoyalGuardEntity::new, MobCategory.MONSTER)
					.sized(0.8f, 2.4f)
					.noSave()
					.clientTrackingRange(10)
					.updateInterval(3)
					.build("spectral_royal_guard"));

	public static final RegistryHolder<EntityType<HollowArbalistEntity>> HOLLOW_ARBALIST = RegistryHolder.entity(
			"hollow_arbalist",
			() -> EntityType.Builder.<HollowArbalistEntity>of(HollowArbalistEntity::new, MobCategory.MONSTER)
					.sized(0.75f, 2.35f)
					.clientTrackingRange(10)
					.updateInterval(3)
					.build("hollow_arbalist"));

	public static final RegistryHolder<EntityType<FallenStandardBearerEntity>> FALLEN_STANDARD_BEARER = RegistryHolder.entity(
			"fallen_standard_bearer",
			() -> EntityType.Builder.<FallenStandardBearerEntity>of(FallenStandardBearerEntity::new, MobCategory.MONSTER)
					.sized(0.8f, 2.45f)
					.clientTrackingRange(10)
					.updateInterval(3)
					.build("fallen_standard_bearer"));

	public static final RegistryHolder<EntityType<SoulboundKnightEntity>> SOULBOUND_KNIGHT = RegistryHolder.entity(
			"soulbound_knight",
			() -> EntityType.Builder.<SoulboundKnightEntity>of(SoulboundKnightEntity::new, MobCategory.MONSTER)
					.sized(0.85f, 2.55f)
					.clientTrackingRange(10)
					.updateInterval(3)
					.build("soulbound_knight"));

	public static final RegistryHolder<Item> HOLLOW_SENTINEL_SPAWN_EGG = new RegistryHolder<>("hollow_sentinel_spawn_egg");
	public static final RegistryHolder<Item> HOLLOW_ARBALIST_SPAWN_EGG = new RegistryHolder<>("hollow_arbalist_spawn_egg");
	public static final RegistryHolder<Item> FALLEN_STANDARD_BEARER_SPAWN_EGG = new RegistryHolder<>("fallen_standard_bearer_spawn_egg");
	public static final RegistryHolder<Item> SOULBOUND_KNIGHT_SPAWN_EGG = new RegistryHolder<>("soulbound_knight_spawn_egg");

	/** Spawn-egg colours: Legion base and spots, and the Sovereign's. */
	public static final int LEGION_EGG_BASE = 0x22242C, LEGION_EGG_SPOTS = 0xB8913F;
	public static final int SOVEREIGN_EGG_BASE = 0x16171D, SOVEREIGN_EGG_SPOTS = 0x4FC9C1;

	// The Gravebound Warden, its helpers and the Cryptborn.
	public static final RegistryHolder<EntityType<GraveboundWardenEntity>> GRAVEBOUND_WARDEN = RegistryHolder.entity(
			"gravebound_warden",
			() -> EntityType.Builder.<GraveboundWardenEntity>of(GraveboundWardenEntity::new, MobCategory.MONSTER)
					.sized(2.2f, 4.6f)
					.clientTrackingRange(10)
					.updateInterval(2)
					.build("gravebound_warden"));

	/** Chain Grab and Warden's Grasp projectile; never saved. */
	public static final RegistryHolder<EntityType<WardenChainEntity>> WARDEN_CHAIN = RegistryHolder.entity(
			"warden_chain",
			() -> EntityType.Builder.<WardenChainEntity>of(WardenChainEntity::new, MobCategory.MISC)
					.sized(0.5f, 0.5f)
					.fireImmune()
					.noSave()
					.noSummon()
					.clientTrackingRange(8)
					.updateInterval(1)
					.build("warden_chain"));

	/** Chain Wall post; never saved. */
	public static final RegistryHolder<EntityType<ChainAnchorEntity>> CHAIN_ANCHOR = RegistryHolder.entity(
			"chain_anchor",
			() -> EntityType.Builder.<ChainAnchorEntity>of(ChainAnchorEntity::new, MobCategory.MISC)
					.sized(1.0f, 2.0f)
					.fireImmune()
					.noSave()
					.noSummon()
					.clientTrackingRange(10)
					.updateInterval(3)
					.build("chain_anchor"));

	public static final RegistryHolder<EntityType<GraveboundSoldierEntity>> GRAVEBOUND_SOLDIER = RegistryHolder.entity(
			"gravebound_soldier",
			() -> EntityType.Builder.<GraveboundSoldierEntity>of(GraveboundSoldierEntity::new, MobCategory.MONSTER)
					.sized(0.7f, 2.1f)
					.clientTrackingRange(10)
					.updateInterval(3)
					.build("gravebound_soldier"));

	public static final RegistryHolder<EntityType<CryptLurkerEntity>> CRYPT_LURKER = RegistryHolder.entity(
			"crypt_lurker",
			() -> EntityType.Builder.<CryptLurkerEntity>of(CryptLurkerEntity::new, MobCategory.MONSTER)
					.sized(1.4f, 0.8f)
					.clientTrackingRange(10)
					.updateInterval(3)
					.build("crypt_lurker"));

	public static final RegistryHolder<EntityType<CatacombRatEntity>> CATACOMB_RAT = RegistryHolder.entity(
			"catacomb_rat",
			() -> EntityType.Builder.<CatacombRatEntity>of(CatacombRatEntity::new, MobCategory.MONSTER)
					.sized(0.6f, 0.6f)
					.clientTrackingRange(8)
					.updateInterval(3)
					.build("catacomb_rat"));

	public static final RegistryHolder<EntityType<BurialGuardianEntity>> BURIAL_GUARDIAN = RegistryHolder.entity(
			"burial_guardian",
			() -> EntityType.Builder.<BurialGuardianEntity>of(BurialGuardianEntity::new, MobCategory.MONSTER)
					.sized(1.6f, 3.3f)
					.clientTrackingRange(10)
					.updateInterval(3)
					.build("burial_guardian"));

	public static final RegistryHolder<Item> GRAVEBOUND_WARDEN_SPAWN_EGG = new RegistryHolder<>("gravebound_warden_spawn_egg");
	public static final RegistryHolder<Item> GRAVEBOUND_SOLDIER_SPAWN_EGG = new RegistryHolder<>("gravebound_soldier_spawn_egg");
	public static final RegistryHolder<Item> CRYPT_LURKER_SPAWN_EGG = new RegistryHolder<>("crypt_lurker_spawn_egg");
	public static final RegistryHolder<Item> CATACOMB_RAT_SPAWN_EGG = new RegistryHolder<>("catacomb_rat_spawn_egg");
	public static final RegistryHolder<Item> BURIAL_GUARDIAN_SPAWN_EGG = new RegistryHolder<>("burial_guardian_spawn_egg");

	/** Spawn-egg colours: Cryptborn base and spots; the Warden keeps the base with remnant-light spots. */
	public static final int CRYPTBORN_EGG_BASE = 0x271C17, CRYPTBORN_EGG_SPOTS = 0xC3B99F, WARDEN_EGG_SPOTS = 0x4FC9C1;

	// The Forgotten Watcher, its helpers and the Forgotten Souls.
	public static final RegistryHolder<EntityType<ForgottenWatcherEntity>> FORGOTTEN_WATCHER = RegistryHolder.entity(
			"forgotten_watcher",
			() -> EntityType.Builder.<ForgottenWatcherEntity>of(ForgottenWatcherEntity::new, MobCategory.MONSTER)
					.sized(1.6f, 3.2f)
					.eyeHeight((float) ForgottenWatcherEntity.EYE)
					.fireImmune()
					.clientTrackingRange(10)
					.updateInterval(2)
					.build("forgotten_watcher"));

	/** Memory illusion decoy; never saved. */
	public static final RegistryHolder<EntityType<WatcherIllusionEntity>> WATCHER_ILLUSION = RegistryHolder.entity(
			"watcher_illusion",
			() -> EntityType.Builder.<WatcherIllusionEntity>of(WatcherIllusionEntity::new, MobCategory.MISC)
					.sized(1.6f, 3.2f)
					.eyeHeight((float) ForgottenWatcherEntity.EYE)
					.fireImmune()
					.noSave()
					.noSummon()
					.clientTrackingRange(10)
					.updateInterval(3)
					.build("watcher_illusion"));

	/** Ring Volley, soul bolt and Memory Staff projectile; never saved. */
	public static final RegistryHolder<EntityType<MemoryBoltEntity>> MEMORY_BOLT = RegistryHolder.entity(
			"memory_bolt",
			() -> EntityType.Builder.<MemoryBoltEntity>of(MemoryBoltEntity::new, MobCategory.MISC)
					.sized(0.5f, 0.5f)
					.fireImmune()
					.noSave()
					.noSummon()
					.clientTrackingRange(8)
					.updateInterval(1)
					.build("memory_bolt"));

	public static final RegistryHolder<EntityType<LostPilgrimEntity>> LOST_PILGRIM = RegistryHolder.entity(
			"lost_pilgrim",
			() -> EntityType.Builder.<LostPilgrimEntity>of(LostPilgrimEntity::new, MobCategory.MONSTER)
					.sized(0.7f, 2.1f)
					.clientTrackingRange(10)
					.updateInterval(3)
					.build("lost_pilgrim"));

	public static final RegistryHolder<EntityType<MemoryWraithEntity>> MEMORY_WRAITH = RegistryHolder.entity(
			"memory_wraith",
			() -> EntityType.Builder.<MemoryWraithEntity>of(MemoryWraithEntity::new, MobCategory.MONSTER)
					.sized(0.8f, 2.2f)
					.clientTrackingRange(10)
					.updateInterval(2)
					.build("memory_wraith"));

	public static final RegistryHolder<EntityType<SoulCollectorEntity>> SOUL_COLLECTOR = RegistryHolder.entity(
			"soul_collector",
			() -> EntityType.Builder.<SoulCollectorEntity>of(SoulCollectorEntity::new, MobCategory.MONSTER)
					.sized(1.1f, 2.4f)
					.clientTrackingRange(10)
					.updateInterval(3)
					.build("soul_collector"));

	/** The Soul Collector's wisps; never saved. */
	public static final RegistryHolder<EntityType<SoulWispEntity>> SOUL_WISP = RegistryHolder.entity(
			"soul_wisp",
			() -> EntityType.Builder.<SoulWispEntity>of(SoulWispEntity::new, MobCategory.MISC)
					.sized(0.4f, 0.4f)
					.fireImmune()
					.noSave()
					.noSummon()
					.clientTrackingRange(8)
					.updateInterval(2)
					.build("soul_wisp"));

	public static final RegistryHolder<Item> FORGOTTEN_WATCHER_SPAWN_EGG = new RegistryHolder<>("forgotten_watcher_spawn_egg");
	public static final RegistryHolder<Item> LOST_PILGRIM_SPAWN_EGG = new RegistryHolder<>("lost_pilgrim_spawn_egg");
	public static final RegistryHolder<Item> MEMORY_WRAITH_SPAWN_EGG = new RegistryHolder<>("memory_wraith_spawn_egg");
	public static final RegistryHolder<Item> SOUL_COLLECTOR_SPAWN_EGG = new RegistryHolder<>("soul_collector_spawn_egg");

	/** Spawn-egg colours: Forgotten Souls base and spots; the Watcher's sandstone and violet crystal. */
	public static final int SOULS_EGG_BASE = 0x22344A, SOULS_EGG_SPOTS = 0xA3C4D6, WATCHER_EGG_BASE = 0x4A4336, WATCHER_EGG_SPOTS = 0x9E86E0;

	private ModEntities() {
	}

	public static void init() {
		RemnantOssukageEntity.init();
		ArmoredGrubEntity.init();
	}

	public static boolean canMonsterSpawn(EntityType<? extends Mob> entityType, ServerLevelAccessor world,
			net.minecraft.world.entity.MobSpawnType reason, BlockPos pos, RandomSource random) {
		if (world.getDifficulty() == Difficulty.PEACEFUL) return false;
		// Under the eclipse the Realm keeps its own light rule.
		Boolean realm = com.nightbeam.remnants.realm.RealmEvents.realmSpawnLight(world, reason, pos, random);
		return (realm != null ? realm : Monster.isDarkEnoughToSpawn(world, pos, random))
				&& Mob.checkMobSpawnRules(entityType, world, reason, pos, random);
	}
}
