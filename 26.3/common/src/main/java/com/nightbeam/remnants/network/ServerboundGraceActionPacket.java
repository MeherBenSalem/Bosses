package com.nightbeam.remnants.network;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.realm.GraceRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** A choice made in the grace menu; the server re-checks everything before acting. */
public record ServerboundGraceActionPacket(BlockPos from, int action, Identifier targetDimension, BlockPos target) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ServerboundGraceActionPacket> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "grace_action"));
	public static final StreamCodec<FriendlyByteBuf, ServerboundGraceActionPacket> STREAM_CODEC =
			StreamCodec.of((buf, packet) -> encode(packet, buf), ServerboundGraceActionPacket::decode);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static final int TRAVEL = 0;
	public static final int DEPART = 1;

	public static void encode(ServerboundGraceActionPacket packet, FriendlyByteBuf buf) {
		buf.writeBlockPos(packet.from);
		buf.writeVarInt(packet.action);
		buf.writeIdentifier(packet.targetDimension);
		buf.writeBlockPos(packet.target);
	}

	public static ServerboundGraceActionPacket decode(FriendlyByteBuf buf) {
		return new ServerboundGraceActionPacket(buf.readBlockPos(), buf.readVarInt(), buf.readIdentifier(), buf.readBlockPos());
	}

	public static void handleServer(ServerboundGraceActionPacket packet, ServerPlayer player) {
		GraceRegistry.handleAction(player, packet);
	}
}
