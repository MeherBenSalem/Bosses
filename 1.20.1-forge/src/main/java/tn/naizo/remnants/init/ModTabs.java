package tn.naizo.remnants.init;

import tn.naizo.remnants.RemnantBossesMod;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

public class ModTabs {
	public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RemnantBossesMod.MODID);

	public static final RegistryObject<CreativeModeTab> REMNANT_BOSSES_TAB = TABS.register("remnant_bosses_tab",
			() -> CreativeModeTab.builder()
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
				})
				.build());
}