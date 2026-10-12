package com.nightbeam.remnants.fabric.network;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.journal.BossHunts;
import com.nightbeam.remnants.client.journal.BossHuntsClient;
import com.nightbeam.remnants.network.ClientboundBossMusicPacket;
import com.nightbeam.remnants.network.ClientboundJournalPacket;
import com.nightbeam.remnants.network.ServerboundJournalRequestPacket;
import com.nightbeam.remnants.platform.services.INetworkHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class FabricNetwork implements INetworkHelper {
	public static final ResourceLocation BOSS_MUSIC = new ResourceLocation(Constants.MOD_ID, "boss_music");
	public static final ResourceLocation BOSS_JOURNAL = new ResourceLocation(Constants.MOD_ID, "boss_journal");
	public static final ResourceLocation JOURNAL_REQUEST = new ResourceLocation(Constants.MOD_ID, "boss_journal_request");

	public static void registerServer() {
		ServerPlayNetworking.registerGlobalReceiver(JOURNAL_REQUEST, (server, player, handler, buf, responseSender) -> {
			ServerboundJournalRequestPacket packet = ServerboundJournalRequestPacket.decode(buf);
			server.execute(() -> BossHunts.request(player, packet.schemaVersion()));
		});
	}

	@Override
	public void sendJournal(ServerPlayer player, ClientboundJournalPacket packet) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		ClientboundJournalPacket.encode(packet, buf);
		ServerPlayNetworking.send(player, BOSS_JOURNAL, buf);
	}

	@Override
	public void requestJournal(ServerboundJournalRequestPacket packet) {
		Client.request(packet);
	}

	@Override
	public void sendBossMusic(ServerPlayer player, int entityId, boolean play) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		ClientboundBossMusicPacket.encode(new ClientboundBossMusicPacket(entityId, play), buf);
		ServerPlayNetworking.send(player, BOSS_MUSIC, buf);
	}

	public static void registerClient() {
		Client.register();
	}

	private static final class Client {
		private static void request(ServerboundJournalRequestPacket packet) {
			FriendlyByteBuf buf = PacketByteBufs.create();
			ServerboundJournalRequestPacket.encode(packet, buf);
			ClientPlayNetworking.send(JOURNAL_REQUEST, buf);
		}

		private static void register() {
			ClientPlayNetworking.registerGlobalReceiver(BOSS_JOURNAL, (client, handler, buf, responseSender) -> {
				ClientboundJournalPacket packet = ClientboundJournalPacket.decode(buf);
				var level = client.level;
				client.execute(() -> {
					if (client.getConnection() == handler && client.level == level) BossHuntsClient.receive(packet);
				});
			});
			ClientPlayNetworking.registerGlobalReceiver(BOSS_MUSIC, (client, handler, buf, responseSender) -> {
				ClientboundBossMusicPacket packet = ClientboundBossMusicPacket.decode(buf);
				client.execute(() -> ClientboundBossMusicPacket.handleClient(packet));
			});
		}
	}
}
