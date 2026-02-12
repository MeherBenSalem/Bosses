package tn.naizo.remnants.network;

import tn.naizo.remnants.RemnantBossesMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = RemnantBossesMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class PacketHandler {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                ClientboundBossMusicPacket.TYPE,
                ClientboundBossMusicPacket.STREAM_CODEC,
                ClientboundBossMusicPacket::handle);
    }
}
