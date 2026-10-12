package com.nightbeam.remnants.network;

import com.nightbeam.remnants.journal.BossHunts;
import com.nightbeam.remnants.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ServerboundJournalRequestPacket(int schemaVersion) implements CustomPacketPayload {
	public static final Type<ServerboundJournalRequestPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "boss_journal_request"));
	public static final StreamCodec<FriendlyByteBuf, ServerboundJournalRequestPacket> STREAM_CODEC = StreamCodec.of(
			(buf, packet) -> encode(packet, buf), ServerboundJournalRequestPacket::decode);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public ServerboundJournalRequestPacket {
		if (schemaVersion != BossHunts.SCHEMA_VERSION) throw new IllegalArgumentException("Unsupported journal schema");
	}

	public static void encode(ServerboundJournalRequestPacket packet, FriendlyByteBuf buf) {
		buf.writeVarInt(packet.schemaVersion());
	}

	public static ServerboundJournalRequestPacket decode(FriendlyByteBuf buf) {
		ServerboundJournalRequestPacket packet = new ServerboundJournalRequestPacket(buf.readVarInt());
		if (buf.readableBytes() != 0) throw new IllegalArgumentException("Trailing journal request data");
		return packet;
	}
}
