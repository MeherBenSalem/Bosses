package tn.naizo.remnants.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundSource;
import tn.naizo.remnants.init.ModSounds;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ClientboundBossMusicPacket {
    private final int entityId; // Use entity ID to track which boss is playing the music
    private final boolean play;

    // Client-side map to track playing sounds
    // Map<EntityID, SoundInstance>
    private static final Map<Integer, SimpleSoundInstance> playingSounds = new HashMap<>();

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
                handleClient(msg);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleClient(ClientboundBossMusicPacket msg) {
        if (tn.naizo.remnants.config.JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage",
                "boss_music_enabled") <= 0)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (msg.play) {
            if (!playingSounds.containsKey(msg.entityId)) {
                // Play Music (Records category for better volume control usually, or Music)
                // Using generic SoundSource.RECORDS to allow it to be heard clearly as BGM
                SimpleSoundInstance sound = new SimpleSoundInstance(
                        ModSounds.SKELETONFIGHT_THEME.get().getLocation(),
                        SoundSource.RECORDS,
                        1.0f, 1.0f,
                        net.minecraft.util.RandomSource.create(),
                        false, // looping?
                        0,
                        SimpleSoundInstance.Attenuation.NONE, // Global sound (no attenuation)
                        0.0, 0.0, 0.0, // x, y, z (ignored if attenuation is NONE)
                        true // relative
                );

                // If it needs to loop, we might need a custom TickableSoundInstance.
                // But SimpleSoundInstance doesn't loop easily unless we use the constructor
                // correctly or wrapping.
                // Actually, for "Boss Music", usually it loops?
                // If the sound file is long (music), we assume it plays once or loops.
                // Let's assume it loops if the user wants continuous music.
                // But SimpleSoundInstance doesn't support 'looping' flag directly in this
                // constructor easily without subclassing?
                // Wait, typically 'forMusic' sets it up.
                // Let's try to enable looping if possible.
                // Actually, let's just use play for now. If it ends, we might need to restart
                // it?
                // For simplicity, we just play it.

                mc.getSoundManager().play(sound);
                playingSounds.put(msg.entityId, sound);
            }
        } else {
            if (playingSounds.containsKey(msg.entityId)) {
                mc.getSoundManager().stop(playingSounds.get(msg.entityId));
                playingSounds.remove(msg.entityId);
            }
        }
    }
}
