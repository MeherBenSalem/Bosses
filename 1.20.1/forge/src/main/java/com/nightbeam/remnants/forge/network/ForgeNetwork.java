package com.nightbeam.remnants.forge.network;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.journal.BossHunts;
import com.nightbeam.remnants.client.journal.BossHuntsClient;
import com.nightbeam.remnants.network.ClientboundBossMusicPacket;
import com.nightbeam.remnants.network.ClientboundJournalPacket;
import com.nightbeam.remnants.network.ServerboundJournalRequestPacket;
import net.minecraft.client.Minecraft;
import com.nightbeam.remnants.platform.services.INetworkHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class ForgeNetwork implements INetworkHelper {
	private static final String PROTOCOL = "2";
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
			new ResourceLocation(Constants.MOD_ID, "main"),
			() -> PROTOCOL,
			PROTOCOL::equals,
			PROTOCOL::equals);

	public ForgeNetwork() {
		CHANNEL.registerMessage(1, ClientboundJournalPacket.class,
				ClientboundJournalPacket::encode, ClientboundJournalPacket::decode,
				(msg, ctx) -> {
					DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.receive(msg, ctx.get()));
					ctx.get().setPacketHandled(true);
				}, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
		CHANNEL.registerMessage(2, ServerboundJournalRequestPacket.class,
				ServerboundJournalRequestPacket::encode, ServerboundJournalRequestPacket::decode,
				(msg, ctx) -> {
					ServerPlayer player = ctx.get().getSender();
					if (player != null) ctx.get().enqueueWork(() -> BossHunts.request(player, msg.schemaVersion()));
					ctx.get().setPacketHandled(true);
				}, Optional.of(NetworkDirection.PLAY_TO_SERVER));
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

	@Override
	public void sendJournal(ServerPlayer player, ClientboundJournalPacket packet) {
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
	}

	@Override
	public void requestJournal(ServerboundJournalRequestPacket packet) {
		CHANNEL.sendToServer(packet);
	}

	private static final class Client {
		private static void receive(ClientboundJournalPacket packet, NetworkEvent.Context context) {
			Minecraft client = Minecraft.getInstance();
			var level = client.level;
			context.enqueueWork(() -> {
				if (client.getConnection() != null && client.getConnection().getConnection() == context.getNetworkManager()
						&& client.level == level) BossHuntsClient.receive(packet);
			});
		}
	}
}
