package com.misanthropy.slashh.network;

import com.misanthropy.slashh.Slashh;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class SlashNetwork {
    private static final String PROTOCOL_VERSION = "5";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Slashh.MODID, "main"),
            () -> PROTOCOL_VERSION,

            v -> PROTOCOL_VERSION.equals(v) || NetworkRegistry.ABSENT.version().equals(v),
            PROTOCOL_VERSION::equals
    );

    private SlashNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(0, SlashEffectPacket.class, SlashEffectPacket::encode, SlashEffectPacket::decode, SlashEffectPacket::handle);
    }
}
