package com.nightbeam.remnants.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class PacketHandler {
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                ClientboundBossMusicPacket.TYPE,
                ClientboundBossMusicPacket.STREAM_CODEC,
                ClientboundBossMusicPacket::handle);
    }
}
