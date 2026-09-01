package com.nightbeam.remnants.fabric;

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
import com.nightbeam.remnants.fabric.network.FabricNetwork;
import com.nightbeam.remnants.init.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class RemnantBossesFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.RAT.get(), RatRenderer::new);
		EntityRendererRegistry.register(ModEntities.SKELETON_MINION.get(), SkeletonMinionRenderer::new);
		EntityRendererRegistry.register(ModEntities.REMNANT_OSSUKAGE.get(), RemnantOssukageRenderer::new);
		EntityRendererRegistry.register(ModEntities.OSSUKAGE_RUNE_EFFECT.get(), OssukageRuneEffectRenderer::new);
		EntityRendererRegistry.register(ModEntities.KUNAI.get(), KunaiRenderer::new);
		EntityRendererRegistry.register(ModEntities.WRAITH.get(), WraithRenderer::new);
		EntityRendererRegistry.register(ModEntities.ARMORED_GRUB.get(), ArmoredGrubRenderer::new);
		EntityRendererRegistry.register(ModEntities.UMBRAKAR.get(), UmbrakarRenderer::new);
		EntityRendererRegistry.register(ModEntities.UMBRAKAR_ORB.get(), UmbrakarOrbRenderer::new);
		EntityRendererRegistry.register(ModEntities.KOTSUKAGE.get(), KotsukageRenderer::new);
		EntityRendererRegistry.register(ModEntities.KOTSUKAGE_TRAP.get(), KotsukageTrapRenderer::new);
		EntityRendererRegistry.register(ModEntities.SKELETON_MELEE.get(), SkeletonMeleeRenderer::new);
		EntityRendererRegistry.register(ModEntities.SKELETON_ARCHER.get(), SkeletonArcherRenderer::new);

		EntityModelLayerRegistry.registerModelLayer(Modelrat.LAYER_LOCATION, Modelrat::createBodyLayer);
		EntityModelLayerRegistry.registerModelLayer(Modelshuriken.LAYER_LOCATION, Modelshuriken::createBodyLayer);
		EntityModelLayerRegistry.registerModelLayer(Modelskeleton_minion.LAYER_LOCATION, Modelskeleton_minion::createBodyLayer);
		EntityModelLayerRegistry.registerModelLayer(Modelwraith.LAYER_LOCATION, Modelwraith::createBodyLayer);

		FabricNetwork.registerClient();
	}
}
