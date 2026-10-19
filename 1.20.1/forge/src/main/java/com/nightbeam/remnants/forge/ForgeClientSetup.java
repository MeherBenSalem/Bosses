package com.nightbeam.remnants.forge;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.client.model.BurialGuardianModel;
import com.nightbeam.remnants.client.model.GraveboundWardenModel;
import com.nightbeam.remnants.client.model.ForgottenWatcherModel;
import com.nightbeam.remnants.client.renderer.MemoryBoltRenderer;
import com.nightbeam.remnants.client.renderer.SoulWispRenderer;
import com.nightbeam.remnants.client.renderer.WatcherIllusionRenderer;
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
import com.nightbeam.remnants.client.realm.RealmClient;
import com.nightbeam.remnants.client.realm.RealmParticles;
import com.nightbeam.remnants.client.realm.WaygateRenderer;
import com.nightbeam.remnants.init.ModBlockEntities;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.init.ModParticles;
import com.nightbeam.remnants.realm.EclipsedRealm;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
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
			EntityRenderers.register(ModEntities.REMNANT_OSSUKAGE.get(), RemnantOssukageRenderer::new);
			EntityRenderers.register(ModEntities.OSSUKAGE_RUNE_EFFECT.get(), OssukageRuneEffectRenderer::new);
			EntityRenderers.register(ModEntities.KUNAI.get(), KunaiRenderer::new);
			EntityRenderers.register(ModEntities.ARMORED_GRUB.get(), ArmoredGrubRenderer::new);
			EntityRenderers.register(ModEntities.HOLLOW_SOVEREIGN.get(), HollowSovereignRenderer::new);
			EntityRenderers.register(ModEntities.SICKLEGHAST.get(), SickleghastRenderer::new);
			EntityRenderers.register(ModEntities.SOVEREIGN_PAULDRON.get(), context -> new RemnantGeoRenderer<>(context, "sovereign_pauldron", 0.4f));
			EntityRenderers.register(ModEntities.SPECTRAL_CRESCENT.get(), SpectralCrescentRenderer::new);
			EntityRenderers.register(ModEntities.ROYAL_BLADE_MARK.get(), NoopRenderer::new);
			EntityRenderers.register(ModEntities.HOLLOW_SENTINEL.get(), context -> new RemnantGeoRenderer<>(context, "hollow_sentinel", 0.5f));
			EntityRenderers.register(ModEntities.SPECTRAL_ROYAL_GUARD.get(), SpectralRoyalGuardRenderer::new);
			EntityRenderers.register(ModEntities.HOLLOW_ARBALIST.get(), context -> new RemnantGeoRenderer<>(context, "hollow_arbalist", 0.5f));
			EntityRenderers.register(ModEntities.FALLEN_STANDARD_BEARER.get(), context -> new RemnantGeoRenderer<>(context, "fallen_standard_bearer", 0.5f));
			EntityRenderers.register(ModEntities.SOULBOUND_KNIGHT.get(), context -> new RemnantGeoRenderer<>(context, "soulbound_knight", 0.55f));
			// The Gravebound Warden, its helpers and the Cryptborn.
			EntityRenderers.register(ModEntities.GRAVEBOUND_WARDEN.get(), context -> new RemnantGeoRenderer<>(context, new GraveboundWardenModel(), 1.3f));
			EntityRenderers.register(ModEntities.WARDEN_CHAIN.get(), WardenChainRenderer::new);
			EntityRenderers.register(ModEntities.CHAIN_ANCHOR.get(), ChainAnchorRenderer::new);
			EntityRenderers.register(ModEntities.GRAVEBOUND_SOLDIER.get(), context -> new RemnantGeoRenderer<>(context, "gravebound_soldier", 0.45f));
			EntityRenderers.register(ModEntities.CRYPT_LURKER.get(), context -> new RemnantGeoRenderer<>(context, "crypt_lurker", 0.7f));
			EntityRenderers.register(ModEntities.CATACOMB_RAT.get(), context -> new RemnantGeoRenderer<>(context, "catacomb_rat", 0.3f));
			EntityRenderers.register(ModEntities.BURIAL_GUARDIAN.get(), context -> new RemnantGeoRenderer<>(context, new BurialGuardianModel(), 1.0f));
			// The Forgotten Watcher, its helpers and the Forgotten Souls.
			EntityRenderers.register(ModEntities.FORGOTTEN_WATCHER.get(), context -> new RemnantGeoRenderer<>(context, new ForgottenWatcherModel(), 1.0f));
			EntityRenderers.register(ModEntities.WATCHER_ILLUSION.get(), WatcherIllusionRenderer::new);
			EntityRenderers.register(ModEntities.MEMORY_BOLT.get(), MemoryBoltRenderer::new);
			EntityRenderers.register(ModEntities.LOST_PILGRIM.get(), context -> new RemnantGeoRenderer<>(context, "lost_pilgrim", 0.45f));
			EntityRenderers.register(ModEntities.MEMORY_WRAITH.get(), context -> new RemnantGeoRenderer<>(context, "memory_wraith", 0.4f));
			EntityRenderers.register(ModEntities.SOUL_COLLECTOR.get(), context -> new RemnantGeoRenderer<>(context, "soul_collector", 0.6f));
			EntityRenderers.register(ModEntities.SOUL_WISP.get(), SoulWispRenderer::new);
		});
	}

	@SubscribeEvent
	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(ModBlockEntities.ECLIPSE_WAYGATE.get(), WaygateRenderer::new);
	}

	@SubscribeEvent
	public static void registerParticles(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(ModParticles.GRACE_MOTE.get(), RealmParticles.GraceMoteProvider::new);
		event.registerSpriteSet(ModParticles.ECLIPSE_SPARK.get(), RealmParticles.EclipseSparkProvider::new);
	}

	@SubscribeEvent
	public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
		event.register(EclipsedRealm.ID, RealmClient.createEffects());
	}

	@SubscribeEvent
	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(Modelshuriken.LAYER_LOCATION, Modelshuriken::createBodyLayer);
	}
}
