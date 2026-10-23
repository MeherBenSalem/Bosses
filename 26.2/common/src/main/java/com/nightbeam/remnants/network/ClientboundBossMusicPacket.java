package com.nightbeam.remnants.network;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.client.ClientBossMusicHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ClientboundBossMusicPacket(int entityId, boolean play) implements CustomPacketPayload {
	public static final Identifier ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "boss_music");
	public static final CustomPacketPayload.Type<ClientboundBossMusicPacket> TYPE = new CustomPacketPayload.Type<>(ID);

	public static final StreamCodec<FriendlyByteBuf, ClientboundBossMusicPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT, ClientboundBossMusicPacket::entityId,
			ByteBufCodecs.BOOL, ClientboundBossMusicPacket::play,
			ClientboundBossMusicPacket::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeInt(entityId);
		buf.writeBoolean(play);
	}

	public static ClientboundBossMusicPacket decode(FriendlyByteBuf buf) {
		return new ClientboundBossMusicPacket(buf.readInt(), buf.readBoolean());
	}

	public static void handleClient(ClientboundBossMusicPacket packet) {
		ClientBossMusicHandler.handle(packet);
	}
}
