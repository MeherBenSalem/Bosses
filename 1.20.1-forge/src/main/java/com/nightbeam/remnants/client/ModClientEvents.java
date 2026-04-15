package com.nightbeam.remnants.client;

import com.nightbeam.remnants.RemnantBossesMod;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.client.model.Modelrat;
import com.nightbeam.remnants.client.model.Modelshuriken;
import com.nightbeam.remnants.client.model.Modelskeleton_minion;
import com.nightbeam.remnants.client.model.Modelskeleton_ninja;
import com.nightbeam.remnants.client.model.Modelwraith;
import com.nightbeam.remnants.client.renderer.RatRenderer;
import com.nightbeam.remnants.client.renderer.KunaiRenderer;
import com.nightbeam.remnants.client.renderer.SkeletonMinionRenderer;
import com.nightbeam.remnants.client.renderer.RemnantOssukageRenderer;
import com.nightbeam.remnants.client.renderer.WraithRenderer;
import com.nightbeam.remnants.client.renderer.ArmoredGrubRenderer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD, modid = RemnantBossesMod.MODID)
public class ModClientEvents {
	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		// Register entity renderers (model layers are baked by the renderer's Context)
		EntityRenderers.register(ModEntities.RAT.get(), RatRenderer::new);
		EntityRenderers.register(ModEntities.SKELETON_MINION.get(), SkeletonMinionRenderer::new);
		EntityRenderers.register(ModEntities.REMNANT_OSSUKAGE.get(), RemnantOssukageRenderer::new);
		EntityRenderers.register(ModEntities.KUNAI.get(), KunaiRenderer::new);
		EntityRenderers.register(ModEntities.WRAITH.get(), WraithRenderer::new);
		EntityRenderers.register(ModEntities.ARMORED_GRUB.get(), ArmoredGrubRenderer::new);
	}

	@SubscribeEvent
	public static void registerLayerDefinitions(net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
		// Register model layer definitions used by the renderers
		event.registerLayerDefinition(Modelrat.LAYER_LOCATION, Modelrat::createBodyLayer);
		event.registerLayerDefinition(Modelshuriken.LAYER_LOCATION, Modelshuriken::createBodyLayer);
		event.registerLayerDefinition(Modelskeleton_minion.LAYER_LOCATION, Modelskeleton_minion::createBodyLayer);
		event.registerLayerDefinition(Modelskeleton_ninja.LAYER_LOCATION, Modelskeleton_ninja::createBodyLayer);
		event.registerLayerDefinition(Modelwraith.LAYER_LOCATION, Modelwraith::createBodyLayer);
	}
}
