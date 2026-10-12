package com.nightbeam.remnants.platform.services;

import com.nightbeam.remnants.network.ServerboundJournalRequestPacket;
import com.nightbeam.remnants.network.ClientboundJournalPacket;
import net.minecraft.server.level.ServerPlayer;

public interface INetworkHelper {
	void sendBossMusic(ServerPlayer player, int entityId, boolean play);
	void sendJournal(ServerPlayer player, ClientboundJournalPacket packet);
	void requestJournal(ServerboundJournalRequestPacket packet);
}
