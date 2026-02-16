package tn.naizo.remnants.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundSource;
import tn.naizo.remnants.init.ModSounds;
import tn.naizo.remnants.network.ClientboundBossMusicPacket;

import java.util.HashMap;
import java.util.Map;

public final class ClientBossMusicHandler {
    private static final Map<Integer, SimpleSoundInstance> playingSounds = new HashMap<>();

    private ClientBossMusicHandler() {
    }

    public static void handle(ClientboundBossMusicPacket msg) {
        if (tn.naizo.remnants.config.JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage",
                "boss_music_enabled") <= 0) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        int entityId = msg.entityId();

        if (msg.play()) {
            if (!playingSounds.containsKey(entityId)) {
                SimpleSoundInstance sound = new SimpleSoundInstance(
                        ModSounds.SKELETONFIGHT_THEME.get().getLocation(),
                        SoundSource.RECORDS,
                        1.0f,
                        1.0f,
                        net.minecraft.util.RandomSource.create(),
                        true,
                        0,
                        SimpleSoundInstance.Attenuation.NONE,
                        0.0,
                        0.0,
                        0.0,
                        true
                );
                mc.getSoundManager().play(sound);
                playingSounds.put(entityId, sound);
            }
        } else {
            SimpleSoundInstance sound = playingSounds.remove(entityId);
            if (sound != null) {
                mc.getSoundManager().stop(sound);
            }
        }
    }
}
