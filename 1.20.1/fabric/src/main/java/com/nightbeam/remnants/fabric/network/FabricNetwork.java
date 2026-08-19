package com.nightbeam.remnants.fabric.network;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.network.ClientboundBossMusicPacket;
import com.nightbeam.remnants.platform.services.INetworkHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class FabricNetwork implements INetworkHelper {
	public static final ResourceLocation BOSS_MUSIC = new ResourceLocation(Constants.MOD_ID, "boss_music");

	@Override
	public void sendBossMusic(ServerPlayer player, int entityId, boolean play) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		ClientboundBossMusicPacket.encode(new ClientboundBossMusicPacket(entityId, play), buf);
		ServerPlayNetworking.send(player, BOSS_MUSIC, buf);
	}

	public static void registerClient() {
		ClientPlayNetworking.registerGlobalReceiver(BOSS_MUSIC, (client, handler, buf, responseSender) -> {
			ClientboundBossMusicPacket packet = ClientboundBossMusicPacket.decode(buf);
			client.execute(() -> ClientboundBossMusicPacket.handleClient(packet));
		});
	}
}
