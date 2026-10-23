package com.nightbeam.remnants.neoforge.network;

import com.nightbeam.remnants.network.ClientboundBossMusicPacket;
import com.nightbeam.remnants.network.ClientboundGraceMenuPacket;
import com.nightbeam.remnants.network.ClientboundJournalPacket;
import com.nightbeam.remnants.network.ClientboundRealmFxPacket;
import com.nightbeam.remnants.network.ServerboundGraceActionPacket;
import com.nightbeam.remnants.network.ServerboundJournalRequestPacket;
import com.nightbeam.remnants.journal.BossHunts;
import com.nightbeam.remnants.client.journal.BossHuntsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class PacketHandler {
	private PacketHandler() {
	}

	public static void register(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar("3");
		registrar.playToClient(ClientboundJournalPacket.TYPE, ClientboundJournalPacket.STREAM_CODEC, (msg, ctx) -> {
			if (FMLEnvironment.getDist() == Dist.CLIENT) Client.receive(msg, ctx);
		});
		registrar.playToServer(ServerboundJournalRequestPacket.TYPE, ServerboundJournalRequestPacket.STREAM_CODEC,
				(msg, ctx) -> ctx.enqueueWork(() -> {
					if (ctx.player() instanceof ServerPlayer player) BossHunts.request(player, msg.schemaVersion());
				}));
		// The Eclipsed Realm.
		registrar.playToClient(ClientboundRealmFxPacket.TYPE, ClientboundRealmFxPacket.STREAM_CODEC,
				(msg, ctx) -> ctx.enqueueWork(() -> ClientboundRealmFxPacket.handleClient(msg)));
		registrar.playToClient(ClientboundGraceMenuPacket.TYPE, ClientboundGraceMenuPacket.STREAM_CODEC,
				(msg, ctx) -> ctx.enqueueWork(() -> ClientboundGraceMenuPacket.handleClient(msg)));
		registrar.playToServer(ServerboundGraceActionPacket.TYPE, ServerboundGraceActionPacket.STREAM_CODEC,
				(msg, ctx) -> ctx.enqueueWork(() -> {
					if (ctx.player() instanceof ServerPlayer player) ServerboundGraceActionPacket.handleServer(msg, player);
				}));
		registrar.playToClient(
				ClientboundBossMusicPacket.TYPE,
				ClientboundBossMusicPacket.STREAM_CODEC,
				(msg, ctx) -> ctx.enqueueWork(() -> ClientboundBossMusicPacket.handleClient(msg)));
	}

	private static final class Client {
		private static void receive(ClientboundJournalPacket packet, IPayloadContext context) {
			Minecraft client = Minecraft.getInstance();
			var connection = context.connection();
			var level = client.level;
			context.enqueueWork(() -> {
				if (client.getConnection() != null && client.getConnection().getConnection() == connection
						&& client.level == level) BossHuntsClient.receive(packet);
			});
		}
	}
}
