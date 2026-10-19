package com.nightbeam.remnants.init;

import com.nightbeam.remnants.item.CryptKeyItem;
import com.nightbeam.remnants.item.CryptSigilItem;
import com.nightbeam.remnants.item.GraveboundExecutionerItem;
import com.nightbeam.remnants.item.OssukageSwordItem;
import com.nightbeam.remnants.item.ArchivistArmorItem;
import com.nightbeam.remnants.item.BossJournalItem;
import com.nightbeam.remnants.item.MemoryLensItem;
import com.nightbeam.remnants.item.MemoryStaffItem;
import com.nightbeam.remnants.item.WatchersEyeItem;
import com.nightbeam.remnants.item.LoreBlockItem;
import com.nightbeam.remnants.item.LoreItem;
import com.nightbeam.remnants.item.RegaliaArmorItem;
import com.nightbeam.remnants.item.SovereignsRuinItem;
import com.nightbeam.remnants.item.SovereignsSealItem;
import com.nightbeam.remnants.item.SovereignsSigilItem;
import com.nightbeam.remnants.item.WardenArmorItem;
import com.nightbeam.remnants.item.WardensGraspItem;
import com.nightbeam.remnants.registry.RegistryHolder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ModItems {
	public static final RegistryHolder<Item> BOSS_JOURNAL = new RegistryHolder<>("boss_journal");
	public static final RegistryHolder<Item> OSSUKAGE_SWORD = new RegistryHolder<>("ossukage_sword");
	public static final RegistryHolder<Item> ANCIENT_ALTAR = new RegistryHolder<>("ancient_altar");
	public static final RegistryHolder<Item> ANCIENT_PEDESTAL = new RegistryHolder<>("ancient_pedestal");

	// The Hollow Sovereign and the Hollow Legion.
	public static final RegistryHolder<Item> TARNISHED_REGALIA = new RegistryHolder<>("tarnished_regalia");
	public static final RegistryHolder<Item> ROYAL_SOUL_FRAGMENT = new RegistryHolder<>("royal_soul_fragment");
	public static final RegistryHolder<Item> SOVEREIGNS_SEAL = new RegistryHolder<>("sovereigns_seal");
	public static final RegistryHolder<Item> SOVEREIGNS_RUIN = new RegistryHolder<>("sovereigns_ruin");
	public static final RegistryHolder<Item> SOVEREIGNS_SIGIL = new RegistryHolder<>("sovereigns_sigil");
	public static final RegistryHolder<Item> HOLLOW_CROWN = new RegistryHolder<>("hollow_crown");
	public static final RegistryHolder<Item> REGALIA_CUIRASS = new RegistryHolder<>("regalia_cuirass");
	public static final RegistryHolder<Item> REGALIA_GREAVES = new RegistryHolder<>("regalia_greaves");
	public static final RegistryHolder<Item> REGALIA_SABATONS = new RegistryHolder<>("regalia_sabatons");

	/**
	 * An item and its factory; loaders register {@link #SOVEREIGN_ITEMS}, {@link #WARDEN_ITEMS}, {@link #WATCHER_ITEMS}
	 * and {@link #REALM_ITEMS} in order.
	 */
	public record Entry(RegistryHolder<Item> holder, Supplier<Item> factory) {
	}

	/** Hollow Sovereign items in registration (and creative tab) order; spawn eggs live in ModEntities. */
	public static final List<Entry> SOVEREIGN_ITEMS = List.of(
			new Entry(TARNISHED_REGALIA, () -> new LoreItem(new Item.Properties(), 1)),
			new Entry(ROYAL_SOUL_FRAGMENT, () -> new LoreItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 2)),
			new Entry(SOVEREIGNS_SEAL, () -> new SovereignsSealItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON))),
			new Entry(SOVEREIGNS_RUIN, SovereignsRuinItem::new),
			new Entry(SOVEREIGNS_SIGIL, () -> new SovereignsSigilItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))),
			new Entry(HOLLOW_CROWN, () -> new RegaliaArmorItem(ArmorItem.Type.HELMET, Rarity.EPIC)),
			new Entry(REGALIA_CUIRASS, () -> new RegaliaArmorItem(ArmorItem.Type.CHESTPLATE, Rarity.RARE)),
			new Entry(REGALIA_GREAVES, () -> new RegaliaArmorItem(ArmorItem.Type.LEGGINGS, Rarity.RARE)),
			new Entry(REGALIA_SABATONS, () -> new RegaliaArmorItem(ArmorItem.Type.BOOTS, Rarity.RARE)));

	// The Gravebound Warden and the Cryptborn.
	public static final RegistryHolder<Item> GRAVE_IRON = new RegistryHolder<>("grave_iron");
	public static final RegistryHolder<Item> CRYPT_KEY = new RegistryHolder<>("crypt_key");
	public static final RegistryHolder<Item> WARDENS_GRASP = new RegistryHolder<>("wardens_grasp");
	public static final RegistryHolder<Item> GRAVEBOUND_EXECUTIONER = new RegistryHolder<>("gravebound_executioner");
	public static final RegistryHolder<Item> CRYPT_SIGIL = new RegistryHolder<>("crypt_sigil");
	public static final RegistryHolder<Item> WARDEN_HELM = new RegistryHolder<>("warden_helm");
	public static final RegistryHolder<Item> WARDEN_HAUBERK = new RegistryHolder<>("warden_hauberk");
	public static final RegistryHolder<Item> WARDEN_TASSETS = new RegistryHolder<>("warden_tassets");
	public static final RegistryHolder<Item> WARDEN_BOOTS = new RegistryHolder<>("warden_boots");

	/** Gravebound Warden items in registration (and creative tab) order; spawn eggs live in ModEntities. */
	public static final List<Entry> WARDEN_ITEMS = List.of(
			new Entry(GRAVE_IRON, () -> new LoreItem(new Item.Properties(), 1)),
			new Entry(CRYPT_KEY, () -> new CryptKeyItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON))),
			new Entry(WARDENS_GRASP, WardensGraspItem::new),
			new Entry(GRAVEBOUND_EXECUTIONER, GraveboundExecutionerItem::new),
			new Entry(CRYPT_SIGIL, () -> new CryptSigilItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))),
			new Entry(WARDEN_HELM, () -> new WardenArmorItem(ArmorItem.Type.HELMET, Rarity.RARE)),
			new Entry(WARDEN_HAUBERK, () -> new WardenArmorItem(ArmorItem.Type.CHESTPLATE, Rarity.RARE)),
			new Entry(WARDEN_TASSETS, () -> new WardenArmorItem(ArmorItem.Type.LEGGINGS, Rarity.RARE)),
			new Entry(WARDEN_BOOTS, () -> new WardenArmorItem(ArmorItem.Type.BOOTS, Rarity.RARE)));

	// The Forgotten Watcher and the Forgotten Souls.
	public static final RegistryHolder<Item> MEMORY_SHARD = new RegistryHolder<>("memory_shard");
	public static final RegistryHolder<Item> KNOWLEDGE_FRAGMENT = new RegistryHolder<>("knowledge_fragment");
	public static final RegistryHolder<Item> MEMORY_LENS = new RegistryHolder<>("memory_lens");
	public static final RegistryHolder<Item> MEMORY_STAFF = new RegistryHolder<>("memory_staff");
	public static final RegistryHolder<Item> WATCHERS_EYE = new RegistryHolder<>("watchers_eye");
	public static final RegistryHolder<Item> ARCHIVIST_HOOD = new RegistryHolder<>("archivist_hood");
	public static final RegistryHolder<Item> ARCHIVIST_ROBE = new RegistryHolder<>("archivist_robe");
	public static final RegistryHolder<Item> ARCHIVIST_LEGGINGS = new RegistryHolder<>("archivist_leggings");
	public static final RegistryHolder<Item> ARCHIVIST_BOOTS = new RegistryHolder<>("archivist_boots");

	/** Forgotten Watcher items in registration (and creative tab) order; spawn eggs live in ModEntities. */
	public static final List<Entry> WATCHER_ITEMS = List.of(
			new Entry(MEMORY_SHARD, () -> new LoreItem(new Item.Properties(), 1)),
			new Entry(KNOWLEDGE_FRAGMENT, () -> new LoreItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 2)),
			new Entry(MEMORY_LENS, () -> new MemoryLensItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON))),
			new Entry(MEMORY_STAFF, MemoryStaffItem::new),
			new Entry(WATCHERS_EYE, () -> new WatchersEyeItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))),
			new Entry(ARCHIVIST_HOOD, () -> new ArchivistArmorItem(ArmorItem.Type.HELMET, Rarity.RARE)),
			new Entry(ARCHIVIST_ROBE, () -> new ArchivistArmorItem(ArmorItem.Type.CHESTPLATE, Rarity.RARE)),
			new Entry(ARCHIVIST_LEGGINGS, () -> new ArchivistArmorItem(ArmorItem.Type.LEGGINGS, Rarity.RARE)),
			new Entry(ARCHIVIST_BOOTS, () -> new ArchivistArmorItem(ArmorItem.Type.BOOTS, Rarity.RARE)));

	// The Eclipsed Realm: the shard that kindles a Waygate, and the Realm's block items.
	public static final RegistryHolder<Item> ECLIPSE_SHARD = new RegistryHolder<>("eclipse_shard");

	/** Realm items in registration (and creative tab) order: the shard, then one item per Realm block that has one. */
	public static final List<Entry> REALM_ITEMS = realmItems();

	private static List<Entry> realmItems() {
		List<Entry> items = new ArrayList<>();
		items.add(new Entry(ECLIPSE_SHARD, () -> new LoreItem(new Item.Properties().rarity(Rarity.UNCOMMON), 2)));
		for (ModBlocks.Entry block : ModBlocks.REALM_BLOCKS) {
			if (block.itemRarity() == null) continue;
			items.add(new Entry(new RegistryHolder<>(block.holder().path()),
					() -> new LoreBlockItem(block.holder().get(), new Item.Properties().rarity(block.itemRarity()), block.loreLines())));
		}
		return List.copyOf(items);
	}

	private ModItems() {
	}

	public static BossJournalItem createBossJournal() {
		return new BossJournalItem();
	}

	public static OssukageSwordItem createOssukageSword() {
		return new OssukageSwordItem();
	}

	public static BlockItem createBlockItem(RegistryHolder<Block> block) {
		return new BlockItem(block.get(), new Item.Properties());
	}
}
