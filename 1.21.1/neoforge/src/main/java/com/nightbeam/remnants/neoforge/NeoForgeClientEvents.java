package com.nightbeam.remnants.neoforge;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.client.model.Modelrat;
import com.nightbeam.remnants.client.model.Modelshuriken;
import com.nightbeam.remnants.client.model.Modelskeleton_minion;
import com.nightbeam.remnants.client.model.Modelwraith;
import com.nightbeam.remnants.client.renderer.UmbrakarOrbRenderer;
import com.nightbeam.remnants.client.renderer.UmbrakarRenderer;
import com.nightbeam.remnants.client.renderer.KotsukageRenderer;
import com.nightbeam.remnants.client.renderer.KotsukageTrapRenderer;
import com.nightbeam.remnants.client.renderer.ArmoredGrubRenderer;
import com.nightbeam.remnants.client.renderer.KunaiRenderer;
import com.nightbeam.remnants.client.renderer.RatRenderer;
import com.nightbeam.remnants.client.renderer.RemnantOssukageRenderer;
import com.nightbeam.remnants.client.renderer.OssukageRuneEffectRenderer;
import com.nightbeam.remnants.client.renderer.SkeletonArcherRenderer;
import com.nightbeam.remnants.client.renderer.SkeletonMeleeRenderer;
import com.nightbeam.remnants.client.renderer.SkeletonMinionRenderer;
import com.nightbeam.remnants.client.renderer.WraithRenderer;
import com.nightbeam.remnants.init.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public final class NeoForgeClientEvents {
	private NeoForgeClientEvents() {
	}

	@SubscribeEvent
	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(ModEntities.RAT.get(), RatRenderer::new);
		event.registerEntityRenderer(ModEntities.SKELETON_MINION.get(), SkeletonMinionRenderer::new);
		event.registerEntityRenderer(ModEntities.REMNANT_OSSUKAGE.get(), RemnantOssukageRenderer::new);
		event.registerEntityRenderer(ModEntities.OSSUKAGE_RUNE_EFFECT.get(), OssukageRuneEffectRenderer::new);
		event.registerEntityRenderer(ModEntities.KUNAI.get(), KunaiRenderer::new);
		event.registerEntityRenderer(ModEntities.WRAITH.get(), WraithRenderer::new);
		event.registerEntityRenderer(ModEntities.ARMORED_GRUB.get(), ArmoredGrubRenderer::new);
		event.registerEntityRenderer(ModEntities.UMBRAKAR.get(), UmbrakarRenderer::new);
		event.registerEntityRenderer(ModEntities.UMBRAKAR_ORB.get(), UmbrakarOrbRenderer::new);
		event.registerEntityRenderer(ModEntities.KOTSUKAGE.get(), KotsukageRenderer::new);
		event.registerEntityRenderer(ModEntities.KOTSUKAGE_TRAP.get(), KotsukageTrapRenderer::new);
		event.registerEntityRenderer(ModEntities.SKELETON_MELEE.get(), SkeletonMeleeRenderer::new);
		event.registerEntityRenderer(ModEntities.SKELETON_ARCHER.get(), SkeletonArcherRenderer::new);
	}

	@SubscribeEvent
	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(Modelrat.LAYER_LOCATION, Modelrat::createBodyLayer);
		event.registerLayerDefinition(Modelskeleton_minion.LAYER_LOCATION, Modelskeleton_minion::createBodyLayer);
		event.registerLayerDefinition(Modelshuriken.LAYER_LOCATION, Modelshuriken::createBodyLayer);
		event.registerLayerDefinition(Modelwraith.LAYER_LOCATION, Modelwraith::createBodyLayer);
	}
}
