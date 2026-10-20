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
					for (ModItems.Entry entry : ModItems.SOVEREIGN_ITEMS) tabData.accept(entry.holder().get());
					// The Gravebound Warden's materials and rewards.
					for (ModItems.Entry entry : ModItems.WARDEN_ITEMS) tabData.accept(entry.holder().get());
					// The Forgotten Watcher's materials and rewards.
					for (ModItems.Entry entry : ModItems.WATCHER_ITEMS) tabData.accept(entry.holder().get());
					// Family materials: the Ashbound Constructs and the Corrupted Relics.
					for (ModItems.Entry entry : ModItems.FAMILY_ITEMS) tabData.accept(entry.holder().get());
					// The Eclipsed Realm: the way in, then its blocks.
					for (ModItems.Entry entry : ModItems.REALM_ITEMS) tabData.accept(entry.holder().get());
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
