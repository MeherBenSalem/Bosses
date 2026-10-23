package com.nightbeam.remnants.fabric.platform;

import com.nightbeam.remnants.network.ClientboundBossMusicPacket;
import com.nightbeam.remnants.network.ClientboundGraceMenuPacket;
import com.nightbeam.remnants.network.ClientboundJournalPacket;
import com.nightbeam.remnants.network.ClientboundRealmFxPacket;
import com.nightbeam.remnants.network.ServerboundGraceActionPacket;
import com.nightbeam.remnants.network.ServerboundJournalRequestPacket;
import com.nightbeam.remnants.platform.services.INetworkHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public class FabricNetwork implements INetworkHelper {
	@Override
	public void sendJournal(ServerPlayer player, ClientboundJournalPacket packet) {
		ServerPlayNetworking.send(player, packet);
	}

	@Override
	public void requestJournal(ServerboundJournalRequestPacket packet) {
		Client.request(packet);
	}

	private static final class Client {
		private static void request(ServerboundJournalRequestPacket packet) {
			ClientPlayNetworking.send(packet);
		}

		private static void graceAction(ServerboundGraceActionPacket packet) {
			ClientPlayNetworking.send(packet);
		}
	}

	@Override
	public void sendRealmFx(ServerPlayer player, ClientboundRealmFxPacket packet) {
		ServerPlayNetworking.send(player, packet);
	}

	@Override
	public void sendGraceMenu(ServerPlayer player, ClientboundGraceMenuPacket packet) {
		ServerPlayNetworking.send(player, packet);
	}

	@Override
	public void sendGraceAction(ServerboundGraceActionPacket packet) {
		Client.graceAction(packet);
	}

	@Override
	public void sendBossMusic(ServerPlayer player, int entityId, boolean play) {
		ServerPlayNetworking.send(player, new ClientboundBossMusicPacket(entityId, play));
	}
}
