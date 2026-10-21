package com.misanthropy.slashh.client;

import com.misanthropy.slashh.Slashh;
import com.misanthropy.slashh.particle.ModParticles;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Slashh.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SlashClientEvents {
    static SpriteSet slash;
    static SpriteSet crit;
    static SpriteSet droplet;
    static SpriteSet wound;

    private SlashClientEvents() {}

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) SwingSpecs::reload);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SLASH.get(), set -> {
            slash = set;
            return (type, level, x, y, z, dx, dy, dz) -> null;
        });
        event.registerSpriteSet(ModParticles.SLASH_CRIT.get(), set -> {
            crit = set;
            return (type, level, x, y, z, dx, dy, dz) -> null;
        });
        event.registerSpriteSet(ModParticles.WOUND.get(), set -> {
            wound = set;
            return (type, level, x, y, z, dx, dy, dz) -> null;
        });
        event.registerSpriteSet(ModParticles.DROPLET.get(), set -> {
            droplet = set;
            return (type, level, x, y, z, dx, dy, dz) -> null;
        });
    }
}
