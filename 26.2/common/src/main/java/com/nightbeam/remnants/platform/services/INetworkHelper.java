package com.nightbeam.remnants.platform.services;

import com.nightbeam.remnants.network.ClientboundGraceMenuPacket;
import com.nightbeam.remnants.network.ClientboundRealmFxPacket;
import com.nightbeam.remnants.network.ServerboundGraceActionPacket;
import com.nightbeam.remnants.network.ServerboundJournalRequestPacket;
import com.nightbeam.remnants.network.ClientboundJournalPacket;
import net.minecraft.server.level.ServerPlayer;

public interface INetworkHelper {
	void sendBossMusic(ServerPlayer player, int entityId, boolean play);
	void sendJournal(ServerPlayer player, ClientboundJournalPacket packet);
	void requestJournal(ServerboundJournalRequestPacket packet);

	void sendRealmFx(ServerPlayer player, ClientboundRealmFxPacket packet);
	void sendGraceMenu(ServerPlayer player, ClientboundGraceMenuPacket packet);
	void sendGraceAction(ServerboundGraceActionPacket packet);
}
