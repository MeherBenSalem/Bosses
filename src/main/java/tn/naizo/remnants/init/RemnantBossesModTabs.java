/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package tn.naizo.remnants.init;

import tn.naizo.remnants.RemnantBossesMod;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

public class RemnantBossesModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RemnantBossesMod.MODID);
	public static final RegistryObject<CreativeModeTab> REMNANT_BOSSES_TAB = REGISTRY.register("remnant_bosses_tab",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.remnant_bosses.remnant_bosses_tab")).icon(() -> new ItemStack(RemnantBossesModBlocks.ANCIENT_ALTAR.get())).displayItems((parameters, tabData) -> {
				tabData.accept(RemnantBossesModBlocks.ANCIENT_ALTAR.get().asItem());
				tabData.accept(RemnantBossesModBlocks.ANCIENT_PEDESTAL.get().asItem());
				tabData.accept(RemnantBossesModItems.RAT_FANG.get());
				tabData.accept(RemnantBossesModItems.OLD_SKELETON_BONE.get());
				tabData.accept(RemnantBossesModItems.OLD_SKELETON_HEAD.get());
				tabData.accept(RemnantBossesModItems.SKELETON_MINION_SPAWN_EGG.get());
				tabData.accept(RemnantBossesModItems.RAT_SPAWN_EGG.get());
				tabData.accept(RemnantBossesModItems.REMNANT_OSSUKAGE_SPAWN_EGG.get());
				tabData.accept(RemnantBossesModItems.OSSUKAGE_SWORD.get());
				tabData.accept(RemnantBossesModItems.FANG_ON_A_STICK.get());
			}).build());
}