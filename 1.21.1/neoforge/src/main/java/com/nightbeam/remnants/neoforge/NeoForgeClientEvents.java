package com.nightbeam.remnants.neoforge;

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
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
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
		event.registerEntityRenderer(ModEntities.REMNANT_OSSUKAGE.get(), RemnantOssukageRenderer::new);
		event.registerEntityRenderer(ModEntities.OSSUKAGE_RUNE_EFFECT.get(), OssukageRuneEffectRenderer::new);
		event.registerEntityRenderer(ModEntities.KUNAI.get(), KunaiRenderer::new);
		event.registerEntityRenderer(ModEntities.ARMORED_GRUB.get(), ArmoredGrubRenderer::new);
		event.registerEntityRenderer(ModEntities.HOLLOW_SOVEREIGN.get(), HollowSovereignRenderer::new);
		event.registerEntityRenderer(ModEntities.SICKLEGHAST.get(), SickleghastRenderer::new);
		event.registerEntityRenderer(ModEntities.SOVEREIGN_PAULDRON.get(), context -> new RemnantGeoRenderer<>(context, "sovereign_pauldron", 0.4f));
		event.registerEntityRenderer(ModEntities.SPECTRAL_CRESCENT.get(), SpectralCrescentRenderer::new);
		event.registerEntityRenderer(ModEntities.ROYAL_BLADE_MARK.get(), NoopRenderer::new);
		event.registerEntityRenderer(ModEntities.HOLLOW_SENTINEL.get(), context -> new RemnantGeoRenderer<>(context, "hollow_sentinel", 0.5f));
		event.registerEntityRenderer(ModEntities.SPECTRAL_ROYAL_GUARD.get(), SpectralRoyalGuardRenderer::new);
		event.registerEntityRenderer(ModEntities.HOLLOW_ARBALIST.get(), context -> new RemnantGeoRenderer<>(context, "hollow_arbalist", 0.5f));
		event.registerEntityRenderer(ModEntities.FALLEN_STANDARD_BEARER.get(), context -> new RemnantGeoRenderer<>(context, "fallen_standard_bearer", 0.5f));
		event.registerEntityRenderer(ModEntities.SOULBOUND_KNIGHT.get(), context -> new RemnantGeoRenderer<>(context, "soulbound_knight", 0.55f));
		// The Gravebound Warden, its helpers and the Cryptborn.
		event.registerEntityRenderer(ModEntities.GRAVEBOUND_WARDEN.get(), context -> new RemnantGeoRenderer<>(context, new GraveboundWardenModel(), 1.3f));
		event.registerEntityRenderer(ModEntities.WARDEN_CHAIN.get(), WardenChainRenderer::new);
		event.registerEntityRenderer(ModEntities.CHAIN_ANCHOR.get(), ChainAnchorRenderer::new);
		event.registerEntityRenderer(ModEntities.GRAVEBOUND_SOLDIER.get(), context -> new RemnantGeoRenderer<>(context, "gravebound_soldier", 0.45f));
		event.registerEntityRenderer(ModEntities.CRYPT_LURKER.get(), context -> new RemnantGeoRenderer<>(context, "crypt_lurker", 0.7f));
		event.registerEntityRenderer(ModEntities.CATACOMB_RAT.get(), context -> new RemnantGeoRenderer<>(context, "catacomb_rat", 0.3f));
		event.registerEntityRenderer(ModEntities.BURIAL_GUARDIAN.get(), context -> new RemnantGeoRenderer<>(context, new BurialGuardianModel(), 1.0f));
		// The Forgotten Watcher, its helpers and the Forgotten Souls.
		event.registerEntityRenderer(ModEntities.FORGOTTEN_WATCHER.get(), context -> new RemnantGeoRenderer<>(context, new ForgottenWatcherModel(), 1.0f));
		event.registerEntityRenderer(ModEntities.WATCHER_ILLUSION.get(), WatcherIllusionRenderer::new);
		event.registerEntityRenderer(ModEntities.MEMORY_BOLT.get(), MemoryBoltRenderer::new);
		event.registerEntityRenderer(ModEntities.LOST_PILGRIM.get(), context -> new RemnantGeoRenderer<>(context, "lost_pilgrim", 0.45f));
		event.registerEntityRenderer(ModEntities.MEMORY_WRAITH.get(), context -> new RemnantGeoRenderer<>(context, "memory_wraith", 0.4f));
		event.registerEntityRenderer(ModEntities.SOUL_COLLECTOR.get(), context -> new RemnantGeoRenderer<>(context, "soul_collector", 0.6f));
		event.registerEntityRenderer(ModEntities.SOUL_WISP.get(), SoulWispRenderer::new);
	}

	@SubscribeEvent
	public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
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
