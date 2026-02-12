package tn.naizo.remnants.network;

import tn.naizo.remnants.RemnantBossesMod;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.NetworkDirection;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RemnantBossesMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static void init() {
        int id = 0;
        CHANNEL.registerMessage(id++, ClientboundBossMusicPacket.class,
                ClientboundBossMusicPacket::encode,
                ClientboundBossMusicPacket::decode,
                ClientboundBossMusicPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
