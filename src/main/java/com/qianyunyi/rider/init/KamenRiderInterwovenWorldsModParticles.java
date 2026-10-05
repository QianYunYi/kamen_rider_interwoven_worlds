
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.qianyunyi.rider.init;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.api.distmarker.Dist;

import com.qianyunyi.rider.client.particle.SuppressEnergyParticleParticle;
import com.qianyunyi.rider.client.particle.SpecialEffectsExplosionParticle;
import com.qianyunyi.rider.client.particle.LIGHTNINGParticle;
import com.qianyunyi.rider.client.particle.DarkLightningParticle;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KamenRiderInterwovenWorldsModParticles {
	@SubscribeEvent
	public static void registerParticles(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(KamenRiderInterwovenWorldsModParticleTypes.SUPPRESS_ENERGY_PARTICLE.get(), SuppressEnergyParticleParticle::provider);
		event.registerSpriteSet(KamenRiderInterwovenWorldsModParticleTypes.LIGHTNING.get(), LIGHTNINGParticle::provider);
		event.registerSpriteSet(KamenRiderInterwovenWorldsModParticleTypes.DARK_LIGHTNING.get(), DarkLightningParticle::provider);
		event.registerSpriteSet(KamenRiderInterwovenWorldsModParticleTypes.SPECIAL_EFFECTS_EXPLOSION.get(), SpecialEffectsExplosionParticle::provider);
	}
}
