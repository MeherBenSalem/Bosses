package com.nightbeam.remnants.platform.services;

import net.minecraft.server.level.ServerPlayer;

public interface INetworkHelper {
	void sendBossMusic(ServerPlayer player, int entityId, boolean play);
}
