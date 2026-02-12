package tn.naizo.remnants.network;

import tn.naizo.remnants.RemnantBossesMod;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;

public class ThrowShurikenKeyBindMessage {
	int type, pressedms;

	public ThrowShurikenKeyBindMessage(int type, int pressedms) {
		this.type = type;
		this.pressedms = pressedms;
	}

	public ThrowShurikenKeyBindMessage(FriendlyByteBuf buffer) {
		this.type = buffer.readInt();
		this.pressedms = buffer.readInt();
	}

	public static void buffer(ThrowShurikenKeyBindMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.type);
		buffer.writeInt(message.pressedms);
	}

	public static void pressAction(Player entity, int type, int pressedms) {
		Level world = entity.level();
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(entity.blockPosition()))
			return;
		if (type == 0) {
			// Procedure call removed - will be handled by event system
		}
	}
}