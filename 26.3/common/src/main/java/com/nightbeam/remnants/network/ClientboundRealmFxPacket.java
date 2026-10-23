package com.nightbeam.remnants.network;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.client.realm.RealmClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server-driven cinematic cues for the Eclipsed Realm overlay: crossings, area titles and banners.
 * Titles and subtitles are translation keys or empty.
 */
public record ClientboundRealmFxPacket(int kind, int ticks, String title, String subtitle) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ClientboundRealmFxPacket> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "realm_fx"));
	public static final StreamCodec<FriendlyByteBuf, ClientboundRealmFxPacket> STREAM_CODEC =
			StreamCodec.of((buf, packet) -> encode(packet, buf), ClientboundRealmFxPacket::decode);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static final int CROSS_BEGIN = 0;
	public static final int ARRIVE = 1;
	public static final int GRACE_DISCOVERED = 2;
	public static final int GRACE_RESTED = 3;
	public static final int REMNANT_FELLED = 4;
	public static final int CANCEL = 5;

	public static ClientboundRealmFxPacket of(int kind) {
		return new ClientboundRealmFxPacket(kind, 0, "", "");
	}

	public static void encode(ClientboundRealmFxPacket packet, FriendlyByteBuf buf) {
		buf.writeVarInt(packet.kind);
		buf.writeVarInt(packet.ticks);
		buf.writeUtf(packet.title, 256);
		buf.writeUtf(packet.subtitle, 256);
	}

	public static ClientboundRealmFxPacket decode(FriendlyByteBuf buf) {
		return new ClientboundRealmFxPacket(buf.readVarInt(), buf.readVarInt(), buf.readUtf(256), buf.readUtf(256));
	}

	public static void handleClient(ClientboundRealmFxPacket packet) {
		RealmClient.handleFx(packet);
	}
}
