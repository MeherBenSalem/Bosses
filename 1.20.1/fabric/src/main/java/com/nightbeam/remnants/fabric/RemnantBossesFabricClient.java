package com.nightbeam.remnants.fabric;

import com.nightbeam.remnants.client.model.BurialGuardianModel;
import com.nightbeam.remnants.client.model.GraveboundWardenModel;
import com.nightbeam.remnants.client.model.ForgottenWatcherModel;
import com.nightbeam.remnants.client.renderer.MemoryBoltRenderer;
import com.nightbeam.remnants.client.renderer.SoulWispRenderer;
import com.nightbeam.remnants.client.renderer.WatcherIllusionRenderer;
import com.nightbeam.remnants.client.journal.BossHuntsClient;
import com.nightbeam.remnants.client.model.Modelshuriken;
import com.nightbeam.remnants.client.renderer.HollowSovereignRenderer;
import com.nightbeam.remnants.client.renderer.SickleghastRenderer;
import com.nightbeam.remnants.client.renderer.ChainAnchorRenderer;
import com.nightbeam.remnants.client.renderer.ArmoredGrubRenderer;
import com.nightbeam.remnants.client.renderer.KunaiRenderer;
import com.nightbeam.remnants.client.renderer.RemnantOssukageRenderer;
import com.nightbeam.remnants.client.renderer.OssukageRuneEffectRenderer;
import com.nightbeam.remnants.client.renderer.RemnantGeoRenderer;
import com.nightbeam.remnants.client.renderer.SpectralCrescentRenderer;
import com.nightbeam.remnants.client.renderer.SpectralRoyalGuardRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import com.nightbeam.remnants.client.renderer.WardenChainRenderer;
import com.nightbeam.remnants.fabric.network.FabricNetwork;
import com.nightbeam.remnants.client.realm.RealmClient;
import com.nightbeam.remnants.client.realm.RealmParticles;
import com.nightbeam.remnants.client.realm.WaygateRenderer;
import com.nightbeam.remnants.init.ModBlockEntities;
import com.nightbeam.remnants.init.ModBlocks;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.init.ModParticles;
import com.nightbeam.remnants.realm.EclipsedRealm;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class RemnantBossesFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			BossHuntsClient.tick();
			RealmClient.tick();
		});
		BlockEntityRenderers.register(ModBlockEntities.ECLIPSE_WAYGATE.get(), WaygateRenderer::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.GRACE_MOTE.get(), RealmParticles.GraceMoteProvider::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.ECLIPSE_SPARK.get(), RealmParticles.EclipseSparkProvider::new);
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GILDED_LEAVES.get(), RenderType.cutoutMipped());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GRACE_EMBER.get(), RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.WEATHERED_GRAVESTONE.get(), RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.FOG_GATE.get(), RenderType.translucent());
		DimensionRenderingRegistry.registerDimensionEffects(EclipsedRealm.ID, RealmClient.createEffects());
		EntityRendererRegistry.register(ModEntities.REMNANT_OSSUKAGE.get(), RemnantOssukageRenderer::new);
		EntityRendererRegistry.register(ModEntities.OSSUKAGE_RUNE_EFFECT.get(), OssukageRuneEffectRenderer::new);
		EntityRendererRegistry.register(ModEntities.KUNAI.get(), KunaiRenderer::new);
		EntityRendererRegistry.register(ModEntities.ARMORED_GRUB.get(), ArmoredGrubRenderer::new);
		EntityRendererRegistry.register(ModEntities.HOLLOW_SOVEREIGN.get(), HollowSovereignRenderer::new);
		EntityRendererRegistry.register(ModEntities.SICKLEGHAST.get(), SickleghastRenderer::new);
		EntityRendererRegistry.register(ModEntities.SOVEREIGN_PAULDRON.get(), context -> new RemnantGeoRenderer<>(context, "sovereign_pauldron", 0.4f));
		EntityRendererRegistry.register(ModEntities.SPECTRAL_CRESCENT.get(), SpectralCrescentRenderer::new);
		EntityRendererRegistry.register(ModEntities.ROYAL_BLADE_MARK.get(), NoopRenderer::new);
		EntityRendererRegistry.register(ModEntities.HOLLOW_SENTINEL.get(), context -> new RemnantGeoRenderer<>(context, "hollow_sentinel", 0.5f));
		EntityRendererRegistry.register(ModEntities.SPECTRAL_ROYAL_GUARD.get(), SpectralRoyalGuardRenderer::new);
		EntityRendererRegistry.register(ModEntities.HOLLOW_ARBALIST.get(), context -> new RemnantGeoRenderer<>(context, "hollow_arbalist", 0.5f));
		EntityRendererRegistry.register(ModEntities.FALLEN_STANDARD_BEARER.get(), context -> new RemnantGeoRenderer<>(context, "fallen_standard_bearer", 0.5f));
		EntityRendererRegistry.register(ModEntities.SOULBOUND_KNIGHT.get(), context -> new RemnantGeoRenderer<>(context, "soulbound_knight", 0.55f));
		// The Gravebound Warden, its helpers and the Cryptborn.
		EntityRendererRegistry.register(ModEntities.GRAVEBOUND_WARDEN.get(), context -> new RemnantGeoRenderer<>(context, new GraveboundWardenModel(), 1.3f));
		EntityRendererRegistry.register(ModEntities.WARDEN_CHAIN.get(), WardenChainRenderer::new);
		EntityRendererRegistry.register(ModEntities.CHAIN_ANCHOR.get(), ChainAnchorRenderer::new);
		EntityRendererRegistry.register(ModEntities.GRAVEBOUND_SOLDIER.get(), context -> new RemnantGeoRenderer<>(context, "gravebound_soldier", 0.45f));
		EntityRendererRegistry.register(ModEntities.CRYPT_LURKER.get(), context -> new RemnantGeoRenderer<>(context, "crypt_lurker", 0.7f));
		EntityRendererRegistry.register(ModEntities.CATACOMB_RAT.get(), context -> new RemnantGeoRenderer<>(context, "catacomb_rat", 0.3f));
		EntityRendererRegistry.register(ModEntities.BURIAL_GUARDIAN.get(), context -> new RemnantGeoRenderer<>(context, new BurialGuardianModel(), 1.0f));
		// The Forgotten Watcher, its helpers and the Forgotten Souls.
		EntityRendererRegistry.register(ModEntities.FORGOTTEN_WATCHER.get(), context -> new RemnantGeoRenderer<>(context, new ForgottenWatcherModel(), 1.0f));
		EntityRendererRegistry.register(ModEntities.WATCHER_ILLUSION.get(), WatcherIllusionRenderer::new);
		EntityRendererRegistry.register(ModEntities.MEMORY_BOLT.get(), MemoryBoltRenderer::new);
		EntityRendererRegistry.register(ModEntities.LOST_PILGRIM.get(), context -> new RemnantGeoRenderer<>(context, "lost_pilgrim", 0.45f));
		EntityRendererRegistry.register(ModEntities.MEMORY_WRAITH.get(), context -> new RemnantGeoRenderer<>(context, "memory_wraith", 0.4f));
		EntityRendererRegistry.register(ModEntities.SOUL_COLLECTOR.get(), context -> new RemnantGeoRenderer<>(context, "soul_collector", 0.6f));
		EntityRendererRegistry.register(ModEntities.SOUL_WISP.get(), SoulWispRenderer::new);

		EntityModelLayerRegistry.registerModelLayer(Modelshuriken.LAYER_LOCATION, Modelshuriken::createBodyLayer);

		FabricNetwork.registerClient();
	}
}
