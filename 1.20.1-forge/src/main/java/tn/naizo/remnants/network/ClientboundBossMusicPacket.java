package tn.naizo.remnants.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import tn.naizo.remnants.client.ClientBossMusicHandler;

import java.util.function.Supplier;

public class ClientboundBossMusicPacket {
    private final int entityId; // Use entity ID to track which boss is playing the music
    private final boolean play;

    public ClientboundBossMusicPacket(int entityId, boolean play) {
        this.entityId = entityId;
        this.play = play;
    }

    public static void encode(ClientboundBossMusicPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeBoolean(msg.play);
    }

    public static ClientboundBossMusicPacket decode(FriendlyByteBuf buf) {
        return new ClientboundBossMusicPacket(buf.readInt(), buf.readBoolean());
    }

    public static void handle(ClientboundBossMusicPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Ensure we are on client
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBossMusicHandler.handle(msg));
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public int getEntityId() {
        return entityId;
    }

    public boolean shouldPlay() {
        return play;
    }
}
