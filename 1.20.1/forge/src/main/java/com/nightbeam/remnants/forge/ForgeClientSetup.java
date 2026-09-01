package com.nightbeam.remnants.forge;

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
import com.nightbeam.remnants.client.renderer.SkeletonMinionRenderer;
import com.nightbeam.remnants.client.renderer.WraithRenderer;
import com.nightbeam.remnants.init.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD, modid = Constants.MOD_ID)
public final class ForgeClientSetup {
	private ForgeClientSetup() {
	}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			EntityRenderers.register(ModEntities.RAT.get(), RatRenderer::new);
			EntityRenderers.register(ModEntities.SKELETON_MINION.get(), SkeletonMinionRenderer::new);
			EntityRenderers.register(ModEntities.REMNANT_OSSUKAGE.get(), RemnantOssukageRenderer::new);
			EntityRenderers.register(ModEntities.OSSUKAGE_RUNE_EFFECT.get(), OssukageRuneEffectRenderer::new);
			EntityRenderers.register(ModEntities.KUNAI.get(), KunaiRenderer::new);
			EntityRenderers.register(ModEntities.WRAITH.get(), WraithRenderer::new);
			EntityRenderers.register(ModEntities.ARMORED_GRUB.get(), ArmoredGrubRenderer::new);
			EntityRenderers.register(ModEntities.UMBRAKAR.get(), UmbrakarRenderer::new);
			EntityRenderers.register(ModEntities.UMBRAKAR_ORB.get(), UmbrakarOrbRenderer::new);
			EntityRenderers.register(ModEntities.KOTSUKAGE.get(), KotsukageRenderer::new);
			EntityRenderers.register(ModEntities.KOTSUKAGE_TRAP.get(), KotsukageTrapRenderer::new);
		});
	}

	@SubscribeEvent
	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(Modelrat.LAYER_LOCATION, Modelrat::createBodyLayer);
		event.registerLayerDefinition(Modelshuriken.LAYER_LOCATION, Modelshuriken::createBodyLayer);
		event.registerLayerDefinition(Modelskeleton_minion.LAYER_LOCATION, Modelskeleton_minion::createBodyLayer);
		event.registerLayerDefinition(Modelwraith.LAYER_LOCATION, Modelwraith::createBodyLayer);
	}
}
