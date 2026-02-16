package tn.naizo.remnants.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import tn.naizo.remnants.client.ClientBossMusicHandler;

import tn.naizo.remnants.RemnantBossesMod;

public record ClientboundBossMusicPacket(int entityId, boolean play) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundBossMusicPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(RemnantBossesMod.MODID, "boss_music"));

    public static final StreamCodec<FriendlyByteBuf, ClientboundBossMusicPacket> STREAM_CODEC = StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.INT, ClientboundBossMusicPacket::entityId,
            net.minecraft.network.codec.ByteBufCodecs.BOOL, ClientboundBossMusicPacket::play,
            ClientboundBossMusicPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientboundBossMusicPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                Class<?> handler = Class.forName("tn.naizo.remnants.client.ClientBossMusicHandler");
                java.lang.reflect.Method method = handler.getMethod("handle", ClientboundBossMusicPacket.class);
                method.invoke(null, msg);
            } catch (ClassNotFoundException e) {
                // Not on client — ignore
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }
}
