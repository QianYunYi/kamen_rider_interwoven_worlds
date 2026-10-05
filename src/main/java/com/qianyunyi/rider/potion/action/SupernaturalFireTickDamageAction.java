package com.qianyunyi.rider.potion.action;

import com.qianyunyi.rider.config.AdventureConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;

public class SupernaturalFireTickDamageAction {
	public static void execute(LivingEntity entity) {
		if (entity == null || !entity.isAlive()) return;

		// 每tick的持续视觉效果
		spawnFireParticles(entity);

		// 每秒（20 ticks）执行一次生命扣除
		if (entity.tickCount % 20 == 0) {
			// 扣除最大生命值5%（保留至少1点生命）
			float damageAmount = entity.getMaxHealth() * 0.05f;
			//最大不超过配置的伤害值
			Double maxDamage = AdventureConfig.SUPERNATURAL_FIRE_DAMAGE_MAX_PER_SECOND.get();
			damageAmount = Math.min(damageAmount, maxDamage.floatValue());
			entity.setHealth(Math.max(1.0f, entity.getHealth() - damageAmount));

			// 播放燃烧音效
			entity.playSound(SoundEvents.BLAZE_BURN,
					0.7F,
					0.5F + entity.getRandom().nextFloat() * 0.5F);
		}

		// 保持实体燃烧视觉效果
		entity.setSecondsOnFire(2);
	}

	private static void spawnFireParticles(LivingEntity entity) {
		// 基础火焰粒子
		for (int i = 0; i < 3; i++) {
			double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
			double y = entity.getY() + entity.getRandom().nextDouble() * entity.getBbHeight();
			double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
			entity.level().addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.05, 0);
		}

		// 灵魂火焰特效（每5 ticks）
		if (entity.tickCount % 5 == 0) {
			entity.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME,
					entity.getX(),
					entity.getY() + entity.getBbHeight() * 0.5,
					entity.getZ(),
					0, 0.1, 0);
		}
	}
}