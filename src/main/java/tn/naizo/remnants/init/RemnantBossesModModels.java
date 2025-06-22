
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package tn.naizo.remnants.init;

import tn.naizo.remnants.client.model.Modelshuriken;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.api.distmarker.Dist;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class RemnantBossesModModels {
	@SubscribeEvent
	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(Modelshuriken.LAYER_LOCATION, Modelshuriken::createBodyLayer);
	}
}
