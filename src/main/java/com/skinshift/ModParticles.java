package com.skinshift;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, SkinShiftMod.MODID);

    /** Кастомный тип частицы: жёлтая искра, которая крутится вокруг игрока. */
    public static final RegistryObject<SimpleParticleType> VORTEX =
            PARTICLES.register("vortex", () -> new SimpleParticleType(true));

    private ModParticles() {}
}
