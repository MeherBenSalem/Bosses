package tn.naizo.remnants.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundSource;
import tn.naizo.remnants.init.ModSounds;

import tn.naizo.remnants.RemnantBossesMod;

import java.util.HashMap;
import java.util.Map;

public record ClientboundBossMusicPacket(int entityId, boolean play) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundBossMusicPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(RemnantBossesMod.MODID, "boss_music"));

    public static final StreamCodec<FriendlyByteBuf, ClientboundBossMusicPacket> STREAM_CODEC = StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.INT, ClientboundBossMusicPacket::entityId,
            net.minecraft.network.codec.ByteBufCodecs.BOOL, ClientboundBossMusicPacket::play,
            ClientboundBossMusicPacket::new);

    // Client-side map to track playing sounds
    private static final Map<Integer, SimpleSoundInstance> playingSounds = new HashMap<>();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientboundBossMusicPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            handleClient(msg);
        });
    }

    private static void handleClient(ClientboundBossMusicPacket msg) {
        // Simple client side check to prevent server crashes if accidentally loaded
        // there (though DistExecutor usually better)
        try {
            if (tn.naizo.remnants.config.JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage",
                    "boss_music_enabled") <= 0)
                return;

            Minecraft mc = Minecraft.getInstance();
            if (msg.play) {
                if (!playingSounds.containsKey(msg.entityId)) {
                    SimpleSoundInstance sound = new SimpleSoundInstance(
                            ModSounds.SKELETONFIGHT_THEME.get().getLocation(),
                            SoundSource.RECORDS,
                            1.0f, 1.0f,
                            net.minecraft.util.RandomSource.create(),
                            false,
                            0,
                            SimpleSoundInstance.Attenuation.NONE,
                            0.0, 0.0, 0.0,
                            true);
                    mc.getSoundManager().play(sound);
                    playingSounds.put(msg.entityId, sound);
                }
            } else {
                if (playingSounds.containsKey(msg.entityId)) {
                    mc.getSoundManager().stop(playingSounds.get(msg.entityId));
                    playingSounds.remove(msg.entityId);
                }
            }
        } catch (Exception e) {
            // Ignore (likely server side or missing sound manager)
        }
    }
}
