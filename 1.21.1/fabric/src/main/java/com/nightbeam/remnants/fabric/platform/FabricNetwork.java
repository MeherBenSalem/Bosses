package com.nightbeam.remnants.fabric.platform;

import com.nightbeam.remnants.network.ClientboundBossMusicPacket;
import com.nightbeam.remnants.platform.services.INetworkHelper;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public class FabricNetwork implements INetworkHelper {
	@Override
	public void sendBossMusic(ServerPlayer player, int entityId, boolean play) {
		ServerPlayNetworking.send(player, new ClientboundBossMusicPacket(entityId, play));
	}
}
