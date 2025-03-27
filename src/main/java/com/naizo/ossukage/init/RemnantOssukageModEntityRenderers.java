
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.naizo.ossukage.init;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.api.distmarker.Dist;

import com.naizo.ossukage.client.renderer.OssukageRenderer;
import com.naizo.ossukage.client.renderer.KunaiRenderer;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RemnantOssukageModEntityRenderers {
	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(RemnantOssukageModEntities.KUNAI.get(), KunaiRenderer::new);
		event.registerEntityRenderer(RemnantOssukageModEntities.OSSUKAGE.get(), OssukageRenderer::new);
	}
}
