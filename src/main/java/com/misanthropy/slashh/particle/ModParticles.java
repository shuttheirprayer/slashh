package com.misanthropy.slashh.particle;

import com.misanthropy.slashh.Slashh;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Slashh.MODID);

    public static final RegistryObject<SimpleParticleType> SLASH = PARTICLE_TYPES.register("slash", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> SLASH_CRIT = PARTICLE_TYPES.register("slash_crit", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> WOUND = PARTICLE_TYPES.register("wound", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> DROPLET = PARTICLE_TYPES.register("droplet", () -> new SimpleParticleType(false));

    private ModParticles() {}
}
