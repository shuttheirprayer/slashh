package com.misanthropy.slashh;

import com.misanthropy.slashh.client.SlashSettingsScreen;
import com.misanthropy.slashh.network.SlashNetwork;
import com.misanthropy.slashh.particle.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Slashh.MODID)
public class Slashh {
    public static final String MODID = "slashh";

    public Slashh() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SlashConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SlashClientConfig.SPEC);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> SlashSettingsScreen::register);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(SlashNetwork::register);
    }
}
