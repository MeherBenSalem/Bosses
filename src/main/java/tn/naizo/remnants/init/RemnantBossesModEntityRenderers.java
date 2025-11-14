/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package tn.naizo.remnants.init;

import tn.naizo.remnants.client.renderer.SkeletonMinionRenderer;
import tn.naizo.remnants.client.renderer.RemnantOssukageRenderer;
import tn.naizo.remnants.client.renderer.RatRenderer;
import tn.naizo.remnants.client.renderer.KunaiRenderer;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.api.distmarker.Dist;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RemnantBossesModEntityRenderers {
	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(RemnantBossesModEntities.KUNAI.get(), KunaiRenderer::new);
		event.registerEntityRenderer(RemnantBossesModEntities.RAT.get(), RatRenderer::new);
		event.registerEntityRenderer(RemnantBossesModEntities.SKELETON_MINION.get(), SkeletonMinionRenderer::new);
		event.registerEntityRenderer(RemnantBossesModEntities.REMNANT_OSSUKAGE.get(), RemnantOssukageRenderer::new);
	}
}