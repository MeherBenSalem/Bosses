package com.nightbeam.remnants.network;

import com.nightbeam.remnants.journal.BossHunts;
import net.minecraft.network.FriendlyByteBuf;

public record ServerboundJournalRequestPacket(int schemaVersion) {
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
