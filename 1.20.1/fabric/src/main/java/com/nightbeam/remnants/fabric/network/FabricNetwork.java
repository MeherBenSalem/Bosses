package com.nightbeam.remnants.fabric.network;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.journal.BossHunts;
import com.nightbeam.remnants.client.journal.BossHuntsClient;
import com.nightbeam.remnants.network.ClientboundBossMusicPacket;
import com.nightbeam.remnants.network.ClientboundGraceMenuPacket;
import com.nightbeam.remnants.network.ClientboundJournalPacket;
import com.nightbeam.remnants.network.ClientboundRealmFxPacket;
import com.nightbeam.remnants.network.ServerboundGraceActionPacket;
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
	public static final ResourceLocation REALM_FX = new ResourceLocation(Constants.MOD_ID, "realm_fx");
	public static final ResourceLocation GRACE_MENU = new ResourceLocation(Constants.MOD_ID, "grace_menu");
	public static final ResourceLocation GRACE_ACTION = new ResourceLocation(Constants.MOD_ID, "grace_action");

	public static void registerServer() {
		ServerPlayNetworking.registerGlobalReceiver(JOURNAL_REQUEST, (server, player, handler, buf, responseSender) -> {
			ServerboundJournalRequestPacket packet = ServerboundJournalRequestPacket.decode(buf);
			server.execute(() -> BossHunts.request(player, packet.schemaVersion()));
		});
		ServerPlayNetworking.registerGlobalReceiver(GRACE_ACTION, (server, player, handler, buf, responseSender) -> {
			ServerboundGraceActionPacket packet = ServerboundGraceActionPacket.decode(buf);
			server.execute(() -> ServerboundGraceActionPacket.handleServer(packet, player));
		});
	}

	@Override
	public void sendRealmFx(ServerPlayer player, ClientboundRealmFxPacket packet) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		ClientboundRealmFxPacket.encode(packet, buf);
		ServerPlayNetworking.send(player, REALM_FX, buf);
	}

	@Override
	public void sendGraceMenu(ServerPlayer player, ClientboundGraceMenuPacket packet) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		ClientboundGraceMenuPacket.encode(packet, buf);
		ServerPlayNetworking.send(player, GRACE_MENU, buf);
	}

	@Override
	public void sendGraceAction(ServerboundGraceActionPacket packet) {
		Client.graceAction(packet);
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

		private static void graceAction(ServerboundGraceActionPacket packet) {
			FriendlyByteBuf buf = PacketByteBufs.create();
			ServerboundGraceActionPacket.encode(packet, buf);
			ClientPlayNetworking.send(GRACE_ACTION, buf);
		}

		private static void register() {
			ClientPlayNetworking.registerGlobalReceiver(REALM_FX, (client, handler, buf, responseSender) -> {
				ClientboundRealmFxPacket packet = ClientboundRealmFxPacket.decode(buf);
				client.execute(() -> ClientboundRealmFxPacket.handleClient(packet));
			});
			ClientPlayNetworking.registerGlobalReceiver(GRACE_MENU, (client, handler, buf, responseSender) -> {
				ClientboundGraceMenuPacket packet = ClientboundGraceMenuPacket.decode(buf);
				client.execute(() -> ClientboundGraceMenuPacket.handleClient(packet));
			});
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
