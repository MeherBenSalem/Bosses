
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.naizo.ossukage.init;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import com.naizo.ossukage.client.renderer.OssukageRenderer;
import com.naizo.ossukage.client.renderer.KunaiRenderer;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RemnantOssukageModEntityRenderers {
	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(RemnantOssukageModEntities.KUNAI.get(), KunaiRenderer::new);
		event.registerEntityRenderer(RemnantOssukageModEntities.OSSUKAGE.get(), OssukageRenderer::new);
	}
}
