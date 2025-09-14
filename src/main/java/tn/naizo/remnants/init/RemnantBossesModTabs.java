
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package tn.naizo.remnants.init;

import tn.naizo.remnants.RemnantBossesMod;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class RemnantBossesModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RemnantBossesMod.MODID);
	public static final RegistryObject<CreativeModeTab> REMNANT_BOSSES_TAB = REGISTRY.register("remnant_bosses_tab",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.remnant_bosses.remnant_bosses_tab")).icon(() -> new ItemStack(RemnantBossesModBlocks.ANCIENT_ALTAR.get())).displayItems((parameters, tabData) -> {
				tabData.accept(RemnantBossesModBlocks.ANCIENT_ALTAR.get().asItem());
				tabData.accept(RemnantBossesModBlocks.ANCIENT_PEDESTAL.get().asItem());
			}).build());

	@SubscribeEvent
	public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent tabData) {
		if (tabData.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
			tabData.accept(RemnantBossesModItems.OSSUKAGE_SPAWN_EGG.get());
			tabData.accept(RemnantBossesModItems.SKELETON_MINIONS_SPAWN_EGG.get());
			tabData.accept(RemnantBossesModItems.RAT_SPAWN_EGG.get());
		} else if (tabData.getTabKey() == CreativeModeTabs.COMBAT) {
			tabData.accept(RemnantBossesModItems.OSSUKAGE_SWORD.get());
		}
	}
}
