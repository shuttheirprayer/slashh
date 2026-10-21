package com.misanthropy.slashh.network;

import com.misanthropy.slashh.client.SlashClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SlashEffectPacket(double x, double y, double z, float rollDeg, float size, int rgb, int flags, int attacker,
                                float dx, float dy, float dz, String animation, int target) {
    public static final int CRIT = 1;
    public static final int FINISHER = 2;
    public static final int SWEEP = 4;
    public static final int SPRAY = 8;
    public static final int OFFHAND = 16;

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }

    public static void encode(SlashEffectPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeFloat(msg.rollDeg);
        buf.writeFloat(msg.size);
        buf.writeInt(msg.rgb);
        buf.writeByte(msg.flags);
        buf.writeVarInt(msg.attacker + 1);
        buf.writeFloat(msg.dx);
        buf.writeFloat(msg.dy);
        buf.writeFloat(msg.dz);
        buf.writeUtf(msg.animation, 128);
        buf.writeVarInt(msg.target + 1);
    }

    public static SlashEffectPacket decode(FriendlyByteBuf buf) {
        return new SlashEffectPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat(),
                buf.readInt(), buf.readByte(), buf.readVarInt() - 1, buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readUtf(128), buf.readVarInt() - 1);
    }

    public static void handle(SlashEffectPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SlashClientHandler.slash(msg)));
        context.setPacketHandled(true);
    }
}
