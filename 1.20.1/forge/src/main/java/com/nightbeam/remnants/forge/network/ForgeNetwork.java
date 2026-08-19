package com.nightbeam.remnants.forge.network;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.network.ClientboundBossMusicPacket;
import com.nightbeam.remnants.platform.services.INetworkHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class ForgeNetwork implements INetworkHelper {
	private static final String PROTOCOL = "1";
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
			new ResourceLocation(Constants.MOD_ID, "main"),
			() -> PROTOCOL,
			PROTOCOL::equals,
			PROTOCOL::equals);

	public ForgeNetwork() {
		CHANNEL.registerMessage(0, ClientboundBossMusicPacket.class,
				ClientboundBossMusicPacket::encode,
				ClientboundBossMusicPacket::decode,
				(msg, ctx) -> {
					ctx.get().enqueueWork(() -> {
						if (ctx.get().getDirection().getReceptionSide().isClient()) {
							DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientboundBossMusicPacket.handleClient(msg));
						}
					});
					ctx.get().setPacketHandled(true);
				},
				Optional.of(NetworkDirection.PLAY_TO_CLIENT));
	}

	@Override
	public void sendBossMusic(ServerPlayer player, int entityId, boolean play) {
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ClientboundBossMusicPacket(entityId, play));
	}
}
