package com.nightbeam.remnants.network;

import com.nightbeam.remnants.realm.GraceRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** A choice made in the grace menu; the server re-checks everything before acting. */
public record ServerboundGraceActionPacket(BlockPos from, int action, ResourceLocation targetDimension, BlockPos target) {
	public static final int TRAVEL = 0;
	public static final int DEPART = 1;

	public static void encode(ServerboundGraceActionPacket packet, FriendlyByteBuf buf) {
		buf.writeBlockPos(packet.from);
		buf.writeVarInt(packet.action);
		buf.writeResourceLocation(packet.targetDimension);
		buf.writeBlockPos(packet.target);
	}

	public static ServerboundGraceActionPacket decode(FriendlyByteBuf buf) {
		return new ServerboundGraceActionPacket(buf.readBlockPos(), buf.readVarInt(), buf.readResourceLocation(), buf.readBlockPos());
	}

	public static void handleServer(ServerboundGraceActionPacket packet, ServerPlayer player) {
		GraceRegistry.handleAction(player, packet);
	}
}
