package com.nightbeam.remnants.init;

import com.nightbeam.remnants.registry.RegistryHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModTabs {
	public static final RegistryHolder<CreativeModeTab> REMNANT_BOSSES_TAB = new RegistryHolder<>("remnant_bosses_tab");

	private ModTabs() {
	}

	public static CreativeModeTab createTab() {
		return CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
				.title(Component.translatable("item_group.remnant_bosses.remnant_bosses_tab"))
				.icon(() -> new ItemStack(ModBlocks.ANCIENT_ALTAR.get()))
				.displayItems((parameters, tabData) -> {
					tabData.accept(ModItems.BOSS_JOURNAL.get());
					tabData.accept(ModBlocks.ANCIENT_ALTAR.get().asItem());
					tabData.accept(ModBlocks.ANCIENT_PEDESTAL.get().asItem());
					tabData.accept(ModItems.OSSUKAGE_SWORD.get());
					// The Hollow Sovereign's materials and rewards.
					tabData.accept(ModItems.TARNISHED_REGALIA.get());
					tabData.accept(ModItems.ROYAL_SOUL_FRAGMENT.get());
					tabData.accept(ModItems.SOVEREIGNS_SEAL.get());
					tabData.accept(ModItems.SOVEREIGNS_RUIN.get());
					tabData.accept(ModItems.SOVEREIGNS_SIGIL.get());
					tabData.accept(ModItems.HOLLOW_CROWN.get());
					tabData.accept(ModItems.REGALIA_CUIRASS.get());
					tabData.accept(ModItems.REGALIA_GREAVES.get());
					tabData.accept(ModItems.REGALIA_SABATONS.get());
					// The Gravebound Warden's materials and rewards.
					tabData.accept(ModItems.GRAVE_IRON.get());
					tabData.accept(ModItems.CRYPT_KEY.get());
					tabData.accept(ModItems.WARDENS_GRASP.get());
					tabData.accept(ModItems.GRAVEBOUND_EXECUTIONER.get());
					tabData.accept(ModItems.CRYPT_SIGIL.get());
					tabData.accept(ModItems.WARDEN_HELM.get());
					tabData.accept(ModItems.WARDEN_HAUBERK.get());
					tabData.accept(ModItems.WARDEN_TASSETS.get());
					tabData.accept(ModItems.WARDEN_BOOTS.get());
					// The Forgotten Watcher's materials and rewards.
					tabData.accept(ModItems.MEMORY_SHARD.get());
					tabData.accept(ModItems.KNOWLEDGE_FRAGMENT.get());
					tabData.accept(ModItems.MEMORY_LENS.get());
					tabData.accept(ModItems.MEMORY_STAFF.get());
					tabData.accept(ModItems.WATCHERS_EYE.get());
					tabData.accept(ModItems.ARCHIVIST_HOOD.get());
					tabData.accept(ModItems.ARCHIVIST_ROBE.get());
					tabData.accept(ModItems.ARCHIVIST_LEGGINGS.get());
					tabData.accept(ModItems.ARCHIVIST_BOOTS.get());
					// Family materials: the Ashbound Constructs and the Corrupted Relics.
					tabData.accept(ModItems.ASHEN_COG.get());
					tabData.accept(ModItems.RELIC_SHARD.get());
					// The Eclipsed Realm: the way in, then its blocks.
					for (RegistryHolder<net.minecraft.world.item.Item> item : ModItems.REALM_ITEMS) tabData.accept(item.get());
					tabData.accept(ModEntities.REMNANT_OSSUKAGE_SPAWN_EGG.get());
					tabData.accept(ModEntities.HOLLOW_SOVEREIGN_SPAWN_EGG.get());
					tabData.accept(ModEntities.HOLLOW_SENTINEL_SPAWN_EGG.get());
					tabData.accept(ModEntities.HOLLOW_ARBALIST_SPAWN_EGG.get());
					tabData.accept(ModEntities.FALLEN_STANDARD_BEARER_SPAWN_EGG.get());
					tabData.accept(ModEntities.SOULBOUND_KNIGHT_SPAWN_EGG.get());
					tabData.accept(ModEntities.GRAVEBOUND_WARDEN_SPAWN_EGG.get());
					tabData.accept(ModEntities.GRAVEBOUND_SOLDIER_SPAWN_EGG.get());
					tabData.accept(ModEntities.CRYPT_LURKER_SPAWN_EGG.get());
					tabData.accept(ModEntities.CATACOMB_RAT_SPAWN_EGG.get());
					tabData.accept(ModEntities.BURIAL_GUARDIAN_SPAWN_EGG.get());
					tabData.accept(ModEntities.FORGOTTEN_WATCHER_SPAWN_EGG.get());
					tabData.accept(ModEntities.LOST_PILGRIM_SPAWN_EGG.get());
					tabData.accept(ModEntities.MEMORY_WRAITH_SPAWN_EGG.get());
					tabData.accept(ModEntities.SOUL_COLLECTOR_SPAWN_EGG.get());
					tabData.accept(ModEntities.ASHBOUND_CRAWLER_SPAWN_EGG.get());
					tabData.accept(ModEntities.EMBER_SERVITOR_SPAWN_EGG.get());
					tabData.accept(ModEntities.RUSTED_GUARDIAN_SPAWN_EGG.get());
					tabData.accept(ModEntities.SHARDLING_SPAWN_EGG.get());
					tabData.accept(ModEntities.ANCIENT_VESSEL_SPAWN_EGG.get());
					tabData.accept(ModEntities.RELIC_STALKER_SPAWN_EGG.get());
				})
				.build();
	}
}
