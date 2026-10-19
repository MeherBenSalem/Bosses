package com.nightbeam.remnants.forge.network;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.journal.BossHunts;
import com.nightbeam.remnants.client.journal.BossHuntsClient;
import com.nightbeam.remnants.network.ClientboundBossMusicPacket;
import com.nightbeam.remnants.network.ClientboundGraceMenuPacket;
import com.nightbeam.remnants.network.ClientboundJournalPacket;
import com.nightbeam.remnants.network.ClientboundRealmFxPacket;
import com.nightbeam.remnants.network.ServerboundGraceActionPacket;
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
	private static final String PROTOCOL = "3";
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
		// The Eclipsed Realm.
		CHANNEL.registerMessage(3, ClientboundRealmFxPacket.class,
				ClientboundRealmFxPacket::encode, ClientboundRealmFxPacket::decode,
				(msg, ctx) -> {
					ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientboundRealmFxPacket.handleClient(msg)));
					ctx.get().setPacketHandled(true);
				}, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
		CHANNEL.registerMessage(4, ClientboundGraceMenuPacket.class,
				ClientboundGraceMenuPacket::encode, ClientboundGraceMenuPacket::decode,
				(msg, ctx) -> {
					ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientboundGraceMenuPacket.handleClient(msg)));
					ctx.get().setPacketHandled(true);
				}, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
		CHANNEL.registerMessage(5, ServerboundGraceActionPacket.class,
				ServerboundGraceActionPacket::encode, ServerboundGraceActionPacket::decode,
				(msg, ctx) -> {
					ServerPlayer player = ctx.get().getSender();
					if (player != null) ctx.get().enqueueWork(() -> ServerboundGraceActionPacket.handleServer(msg, player));
					ctx.get().setPacketHandled(true);
				}, Optional.of(NetworkDirection.PLAY_TO_SERVER));
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

	@Override
	public void sendRealmFx(ServerPlayer player, ClientboundRealmFxPacket packet) {
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
	}

	@Override
	public void sendGraceMenu(ServerPlayer player, ClientboundGraceMenuPacket packet) {
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
	}

	@Override
	public void sendGraceAction(ServerboundGraceActionPacket packet) {
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
