package com.nightbeam.remnants.network;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.client.realm.RealmClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Opens the grace menu: the grace the player rests at and every grace they have discovered. */
public record ClientboundGraceMenuPacket(BlockPos grace, String name, boolean inRealm, List<Entry> entries) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ClientboundGraceMenuPacket> TYPE =
			new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "grace_menu"));
	public static final StreamCodec<FriendlyByteBuf, ClientboundGraceMenuPacket> STREAM_CODEC =
			StreamCodec.of((buf, packet) -> encode(packet, buf), ClientboundGraceMenuPacket::decode);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static final int MAX_ENTRIES = 128;

	public record Entry(ResourceLocation dimension, BlockPos pos, String name, boolean here) {
	}

	public static void encode(ClientboundGraceMenuPacket packet, FriendlyByteBuf buf) {
		buf.writeBlockPos(packet.grace);
		buf.writeUtf(packet.name, 64);
		buf.writeBoolean(packet.inRealm);
		int count = Math.min(MAX_ENTRIES, packet.entries.size());
		buf.writeVarInt(count);
		for (int i = 0; i < count; i++) {
			Entry entry = packet.entries.get(i);
			buf.writeResourceLocation(entry.dimension);
			buf.writeBlockPos(entry.pos);
			buf.writeUtf(entry.name, 64);
			buf.writeBoolean(entry.here);
		}
	}

	public static ClientboundGraceMenuPacket decode(FriendlyByteBuf buf) {
		BlockPos grace = buf.readBlockPos();
		String name = buf.readUtf(64);
		boolean inRealm = buf.readBoolean();
		int count = Math.min(MAX_ENTRIES, buf.readVarInt());
		List<Entry> entries = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			entries.add(new Entry(buf.readResourceLocation(), buf.readBlockPos(), buf.readUtf(64), buf.readBoolean()));
		}
		return new ClientboundGraceMenuPacket(grace, name, inRealm, entries);
	}

	public static void handleClient(ClientboundGraceMenuPacket packet) {
		RealmClient.openGraceMenu(packet);
	}
}
