package com.nightbeam.remnants.init;

import com.nightbeam.remnants.item.CryptKeyItem;
import com.nightbeam.remnants.item.CryptSigilItem;
import com.nightbeam.remnants.item.GraveboundExecutionerItem;
import com.nightbeam.remnants.item.LoreBlockItem;
import com.nightbeam.remnants.item.LoreItem;
import com.nightbeam.remnants.item.OssukageSwordItem;
import com.nightbeam.remnants.item.ArchivistArmorItem;
import com.nightbeam.remnants.item.BossJournalItem;
import com.nightbeam.remnants.item.MemoryLensItem;
import com.nightbeam.remnants.item.MemoryStaffItem;
import com.nightbeam.remnants.item.WatchersEyeItem;
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

import java.util.ArrayList;
import java.util.List;

public final class ModItems {
	public static final RegistryHolder<Item> BOSS_JOURNAL = new RegistryHolder<>("boss_journal", BossJournalItem::new);
	public static final RegistryHolder<Item> OSSUKAGE_SWORD = new RegistryHolder<>("ossukage_sword", OssukageSwordItem::new);
	public static final RegistryHolder<Item> ANCIENT_ALTAR = blockItem(ModBlocks.ANCIENT_ALTAR);
	public static final RegistryHolder<Item> ANCIENT_PEDESTAL = blockItem(ModBlocks.ANCIENT_PEDESTAL);

	// The Hollow Sovereign and the Hollow Legion.
	public static final RegistryHolder<Item> TARNISHED_REGALIA = new RegistryHolder<>("tarnished_regalia",
			() -> new LoreItem(new Item.Properties(), 1));
	public static final RegistryHolder<Item> ROYAL_SOUL_FRAGMENT = new RegistryHolder<>("royal_soul_fragment",
			() -> new LoreItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 2));
	public static final RegistryHolder<Item> SOVEREIGNS_SEAL = new RegistryHolder<>("sovereigns_seal",
			() -> new SovereignsSealItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
	public static final RegistryHolder<Item> SOVEREIGNS_RUIN = new RegistryHolder<>("sovereigns_ruin", SovereignsRuinItem::new);
	public static final RegistryHolder<Item> SOVEREIGNS_SIGIL = new RegistryHolder<>("sovereigns_sigil",
			() -> new SovereignsSigilItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));
	public static final RegistryHolder<Item> HOLLOW_CROWN = new RegistryHolder<>("hollow_crown",
			() -> new RegaliaArmorItem(ArmorItem.Type.HELMET, Rarity.EPIC));
	public static final RegistryHolder<Item> REGALIA_CUIRASS = new RegistryHolder<>("regalia_cuirass",
			() -> new RegaliaArmorItem(ArmorItem.Type.CHESTPLATE, Rarity.RARE));
	public static final RegistryHolder<Item> REGALIA_GREAVES = new RegistryHolder<>("regalia_greaves",
			() -> new RegaliaArmorItem(ArmorItem.Type.LEGGINGS, Rarity.RARE));
	public static final RegistryHolder<Item> REGALIA_SABATONS = new RegistryHolder<>("regalia_sabatons",
			() -> new RegaliaArmorItem(ArmorItem.Type.BOOTS, Rarity.RARE));

	// The Gravebound Warden and the Cryptborn.
	public static final RegistryHolder<Item> GRAVE_IRON = new RegistryHolder<>("grave_iron",
			() -> new LoreItem(new Item.Properties(), 1));
	public static final RegistryHolder<Item> CRYPT_KEY = new RegistryHolder<>("crypt_key",
			() -> new CryptKeyItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
	public static final RegistryHolder<Item> WARDENS_GRASP = new RegistryHolder<>("wardens_grasp", WardensGraspItem::new);
	public static final RegistryHolder<Item> GRAVEBOUND_EXECUTIONER = new RegistryHolder<>("gravebound_executioner", GraveboundExecutionerItem::new);
	public static final RegistryHolder<Item> CRYPT_SIGIL = new RegistryHolder<>("crypt_sigil",
			() -> new CryptSigilItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
	public static final RegistryHolder<Item> WARDEN_HELM = new RegistryHolder<>("warden_helm",
			() -> new WardenArmorItem(ArmorItem.Type.HELMET, Rarity.RARE));
	public static final RegistryHolder<Item> WARDEN_HAUBERK = new RegistryHolder<>("warden_hauberk",
			() -> new WardenArmorItem(ArmorItem.Type.CHESTPLATE, Rarity.RARE));
	public static final RegistryHolder<Item> WARDEN_TASSETS = new RegistryHolder<>("warden_tassets",
			() -> new WardenArmorItem(ArmorItem.Type.LEGGINGS, Rarity.RARE));
	public static final RegistryHolder<Item> WARDEN_BOOTS = new RegistryHolder<>("warden_boots",
			() -> new WardenArmorItem(ArmorItem.Type.BOOTS, Rarity.RARE));

	// The Forgotten Watcher and the Forgotten Souls.
	public static final RegistryHolder<Item> MEMORY_SHARD = new RegistryHolder<>("memory_shard",
			() -> new LoreItem(new Item.Properties(), 1));
	public static final RegistryHolder<Item> KNOWLEDGE_FRAGMENT = new RegistryHolder<>("knowledge_fragment",
			() -> new LoreItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 2));
	public static final RegistryHolder<Item> MEMORY_LENS = new RegistryHolder<>("memory_lens",
			() -> new MemoryLensItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
	public static final RegistryHolder<Item> MEMORY_STAFF = new RegistryHolder<>("memory_staff", MemoryStaffItem::new);
	public static final RegistryHolder<Item> WATCHERS_EYE = new RegistryHolder<>("watchers_eye",
			() -> new WatchersEyeItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));
	public static final RegistryHolder<Item> ARCHIVIST_HOOD = new RegistryHolder<>("archivist_hood",
			() -> new ArchivistArmorItem(ArmorItem.Type.HELMET, Rarity.RARE));
	public static final RegistryHolder<Item> ARCHIVIST_ROBE = new RegistryHolder<>("archivist_robe",
			() -> new ArchivistArmorItem(ArmorItem.Type.CHESTPLATE, Rarity.RARE));
	public static final RegistryHolder<Item> ARCHIVIST_LEGGINGS = new RegistryHolder<>("archivist_leggings",
			() -> new ArchivistArmorItem(ArmorItem.Type.LEGGINGS, Rarity.RARE));
	public static final RegistryHolder<Item> ARCHIVIST_BOOTS = new RegistryHolder<>("archivist_boots",
			() -> new ArchivistArmorItem(ArmorItem.Type.BOOTS, Rarity.RARE));

	/** Every plain item above, in registration (and creative tab) order; spawn eggs live in ModEntities. */
	public static final List<RegistryHolder<Item>> ALL = List.of(BOSS_JOURNAL, OSSUKAGE_SWORD, ANCIENT_ALTAR, ANCIENT_PEDESTAL,
			TARNISHED_REGALIA, ROYAL_SOUL_FRAGMENT, SOVEREIGNS_SEAL, SOVEREIGNS_RUIN, SOVEREIGNS_SIGIL,
			HOLLOW_CROWN, REGALIA_CUIRASS, REGALIA_GREAVES, REGALIA_SABATONS,
			GRAVE_IRON, CRYPT_KEY, WARDENS_GRASP, GRAVEBOUND_EXECUTIONER, CRYPT_SIGIL,
			WARDEN_HELM, WARDEN_HAUBERK, WARDEN_TASSETS, WARDEN_BOOTS,
			MEMORY_SHARD, KNOWLEDGE_FRAGMENT, MEMORY_LENS, MEMORY_STAFF, WATCHERS_EYE,
			ARCHIVIST_HOOD, ARCHIVIST_ROBE, ARCHIVIST_LEGGINGS, ARCHIVIST_BOOTS);

	// The Eclipsed Realm: the shard that kindles a Waygate, and the Realm's block items.
	public static final RegistryHolder<Item> ECLIPSE_SHARD = new RegistryHolder<>("eclipse_shard",
			() -> new LoreItem(new Item.Properties().rarity(Rarity.UNCOMMON), 2));

	/** Realm items in registration (and creative tab) order: the shard, then one item per Realm block that has one. */
	public static final List<RegistryHolder<Item>> REALM_ITEMS = realmItems();

	private static List<RegistryHolder<Item>> realmItems() {
		List<RegistryHolder<Item>> items = new ArrayList<>();
		items.add(ECLIPSE_SHARD);
		for (ModBlocks.Entry block : ModBlocks.REALM_BLOCKS) {
			if (block.itemRarity() == null) continue;
			items.add(new RegistryHolder<>(block.holder().path(), () -> new LoreBlockItem(block.holder().get(),
					new Item.Properties().rarity(block.itemRarity()), block.loreLines())));
		}
		return List.copyOf(items);
	}

	private ModItems() {
	}

	private static RegistryHolder<Item> blockItem(RegistryHolder<net.minecraft.world.level.block.Block> block) {
		return new RegistryHolder<>(block.path(), () -> new BlockItem(block.get(), new Item.Properties()));
	}
}
