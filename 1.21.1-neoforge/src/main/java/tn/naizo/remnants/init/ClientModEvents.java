package tn.naizo.remnants.init;

import tn.naizo.remnants.RemnantBossesMod;
import tn.naizo.remnants.client.model.Modelrat;
import tn.naizo.remnants.client.model.Modelskeleton_minion;
import tn.naizo.remnants.client.model.Modelskeleton_ninja;
import tn.naizo.remnants.client.model.Modelshuriken;
import tn.naizo.remnants.client.model.Modelwraith;
import tn.naizo.remnants.client.renderer.KunaiRenderer;
import tn.naizo.remnants.client.renderer.RatRenderer;
import tn.naizo.remnants.client.renderer.RemnantOssukageRenderer;
import tn.naizo.remnants.client.renderer.SkeletonMinionRenderer;
import tn.naizo.remnants.client.renderer.WraithRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = RemnantBossesMod.MODID, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.RAT.get(), RatRenderer::new);
        event.registerEntityRenderer(ModEntities.SKELETON_MINION.get(), SkeletonMinionRenderer::new);
        event.registerEntityRenderer(ModEntities.REMNANT_OSSUKAGE.get(), RemnantOssukageRenderer::new);
        event.registerEntityRenderer(ModEntities.KUNAI.get(), KunaiRenderer::new);
        event.registerEntityRenderer(ModEntities.WRAITH.get(), WraithRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(Modelrat.LAYER_LOCATION, Modelrat::createBodyLayer);
        event.registerLayerDefinition(Modelskeleton_minion.LAYER_LOCATION, Modelskeleton_minion::createBodyLayer);
        event.registerLayerDefinition(Modelskeleton_ninja.LAYER_LOCATION, Modelskeleton_ninja::createBodyLayer);
        event.registerLayerDefinition(Modelshuriken.LAYER_LOCATION, Modelshuriken::createBodyLayer);
        event.registerLayerDefinition(Modelwraith.LAYER_LOCATION, Modelwraith::createBodyLayer);
    }
}
