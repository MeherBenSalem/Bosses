package com.nightbeam.remnants.neoforge.platform;

import com.nightbeam.remnants.network.ClientboundBossMusicPacket;
import com.nightbeam.remnants.network.ClientboundJournalPacket;
import com.nightbeam.remnants.network.ServerboundJournalRequestPacket;
import com.nightbeam.remnants.platform.services.INetworkHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class NeoForgeNetwork implements INetworkHelper {
	@Override
	public void sendJournal(ServerPlayer player, ClientboundJournalPacket packet) {
		PacketDistributor.sendToPlayer(player, packet);
	}

	@Override
	public void requestJournal(ServerboundJournalRequestPacket packet) {
		PacketDistributor.sendToServer(packet);
	}

	@Override
	public void sendBossMusic(ServerPlayer player, int entityId, boolean play) {
		PacketDistributor.sendToPlayer(player, new ClientboundBossMusicPacket(entityId, play));
	}
}
