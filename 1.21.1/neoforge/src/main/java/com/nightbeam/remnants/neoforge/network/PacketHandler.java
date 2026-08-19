package com.nightbeam.remnants.neoforge.network;

import com.nightbeam.remnants.network.ClientboundBossMusicPacket;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class PacketHandler {
	private PacketHandler() {
	}

	public static void register(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar("1");
		registrar.playToClient(
				ClientboundBossMusicPacket.TYPE,
				ClientboundBossMusicPacket.STREAM_CODEC,
				(msg, ctx) -> ctx.enqueueWork(() -> ClientboundBossMusicPacket.handleClient(msg)));
	}
}
