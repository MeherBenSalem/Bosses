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
					tabData.accept(ModBlocks.ANCIENT_ALTAR.get().asItem());
					tabData.accept(ModBlocks.ANCIENT_PEDESTAL.get().asItem());
					tabData.accept(ModItems.RAT_FANG.get());
					tabData.accept(ModItems.OLD_SKELETON_BONE.get());
					tabData.accept(ModItems.OLD_SKELETON_HEAD.get());
					tabData.accept(ModItems.OSSUKAGE_SWORD.get());
					tabData.accept(ModItems.FANG_ON_A_STICK.get());
					tabData.accept(ModEntities.RAT_SPAWN_EGG.get());
					tabData.accept(ModEntities.SKELETON_MINION_SPAWN_EGG.get());
					tabData.accept(ModEntities.REMNANT_OSSUKAGE_SPAWN_EGG.get());
					tabData.accept(ModEntities.WRAITH_SPAWN_EGG.get());
					tabData.accept(ModEntities.ARMORED_GRUB_SPAWN_EGG.get());
					tabData.accept(ModEntities.UMBRAKAR_SPAWN_EGG.get());
					tabData.accept(ModEntities.KOTSUKAGE_SPAWN_EGG.get());
				})
				.build();
	}
}
