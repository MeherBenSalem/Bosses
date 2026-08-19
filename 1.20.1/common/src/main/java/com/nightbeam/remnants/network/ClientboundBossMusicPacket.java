package com.nightbeam.remnants.network;

import com.nightbeam.remnants.client.ClientBossMusicHandler;
import net.minecraft.network.FriendlyByteBuf;

public class ClientboundBossMusicPacket {
	private final int entityId;
	private final boolean play;

	public ClientboundBossMusicPacket(int entityId, boolean play) {
		this.entityId = entityId;
		this.play = play;
	}

	public static void encode(ClientboundBossMusicPacket msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.entityId);
		buf.writeBoolean(msg.play);
	}

	public static ClientboundBossMusicPacket decode(FriendlyByteBuf buf) {
		return new ClientboundBossMusicPacket(buf.readInt(), buf.readBoolean());
	}

	public static void handleClient(ClientboundBossMusicPacket msg) {
		ClientBossMusicHandler.handle(msg);
	}

	public int getEntityId() {
		return entityId;
	}

	public boolean shouldPlay() {
		return play;
	}
}
