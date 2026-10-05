
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.qianyunyi.rider.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleType;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;

public class KamenRiderInterwovenWorldsModParticleTypes {
	public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, KamenRiderInterwovenWorldsMod.MODID);
	public static final RegistryObject<SimpleParticleType> SUPPRESS_ENERGY_PARTICLE = REGISTRY.register("suppress_energy_particle", () -> new SimpleParticleType(false));
	public static final RegistryObject<SimpleParticleType> LIGHTNING = REGISTRY.register("lightning", () -> new SimpleParticleType(false));
	public static final RegistryObject<SimpleParticleType> DARK_LIGHTNING = REGISTRY.register("dark_lightning", () -> new SimpleParticleType(false));
	public static final RegistryObject<SimpleParticleType> SPECIAL_EFFECTS_EXPLOSION = REGISTRY.register("special_effects_explosion", () -> new SimpleParticleType(false));
}
