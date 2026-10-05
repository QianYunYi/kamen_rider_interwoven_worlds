package com.qianyunyi.rider.entity.kaijin.gurongi.gamio.skills;

import com.qianyunyi.rider.entity.kaijin.gurongi.gamio.NGamioZedaEntity;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.MessageSupernaturalKillingIntent;
import com.qianyunyi.rider.util.DelayedTask;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Random;

public class LightningWaveSkill extends AbstractGamioSkill {
	private static final Random random = new Random();

	// 闪电波动颜色：外围少量绿色，主要金色
	private static final DustParticleOptions CORE_GOLDEN = new DustParticleOptions(
			new Vector3f(1.0f, 0.9f, 0.1f), 2.5f); // 亮金色核心

	private static final DustParticleOptions GOLDEN_INNER = new DustParticleOptions(
			new Vector3f(1.0f, 0.7f, 0.0f), 2.0f); // 金色内层

	private static final DustParticleOptions GREEN_EDGE = new DustParticleOptions(
			new Vector3f(0.2f, 0.8f, 0.1f), 1.5f); // 绿色边缘

	private static final DustParticleOptions GOLDEN_OUTER = new DustParticleOptions(
			new Vector3f(0.9f, 0.6f, 0.0f), 1.2f); // 暗金色外层

	private static final int CYLINDER_LENGTH_SEGMENTS = 30;
	private static final int RADIAL_SEGMENTS = 16;
	private static final float MIN_RADIUS = 0.3f;
	private static final float MAX_RADIUS = 1.5f;
	private static final float TRAVEL_SPEED = 1.5f;

	private static final int DAMAGE_HITS = 10;
	private static final int HIT_DELAY_TICKS = 1;

	public LightningWaveSkill(NGamioZedaEntity entity) {
		super(entity);
	}

	@Override
	public boolean canExecute(LivingEntity target) {
		return target != null && target.isAlive() && entity.hasLineOfSight(target);
	}

	@Override
	public int getCooldown() {
		return 600; // 30秒
	}

	@Override
	public int getPriority() {
		return 2;
	}

	@Override
	public double getMaxRange() {
		return 32.0;
	}

	@Override
	public int getSkillLockDuration() {
		return 20; // 1S
	}

	@Override
	protected void onStart() {
		entity.setAnimation("dark_surge_palm", 20); // 技能锁时长=1S

		// 播放蓄力音效
		entity.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.2f, 0.6f);

		// 显示杀意波动（和达古巴一样）
		if (!entity.isSupernaturalKillingIntentActive()) {
			entity.activateSupernaturalKillingIntent(100);
			if (!entity.level().isClientSide) {
				NetworkHandler.sendToAll(new MessageSupernaturalKillingIntent(true, 100, 1.0f));
			}
		}

		// 蓄力粒子效果
		if (entity.level() instanceof ServerLevel serverLevel) {
			for (int i = 0; i < 10; i++) {
				DelayedTask.run(serverLevel, i, () -> {
					if (!entity.isAlive()) return;
					Vec3 spawnPos = entity.position().add(0, entity.getBbHeight() * 0.75, 0);
					Vec3 offset = new Vec3(
							random.nextDouble() - 0.5,
							random.nextDouble() - 0.5,
							random.nextDouble() - 0.5
					).normalize().scale(0.5);

					// 金色闪电粒子蓄力
					serverLevel.sendParticles(
							ParticleTypes.ELECTRIC_SPARK,
							spawnPos.x + offset.x,
							spawnPos.y + offset.y,
							spawnPos.z + offset.z,
							3, 0.1, 0.1, 0.1, 0.02);
				});
			}
		}

		// 延迟后发射波动
		DelayedTask.run(entity.level(), 20, () -> {
			if (target != null && target.isAlive() && entity.isAlive()) {
				createLightningWaveBeam();
			} else {
				onFinish();
			}
		});
	}

	/**
	 * 创建闪电波动光束
	 */
	private void createLightningWaveBeam() {
		if (!(entity.level() instanceof ServerLevel serverLevel) || target == null) {
			onFinish();
			return;
		}

		Vec3 startPos = entity.position().add(0, entity.getBbHeight() * 0.75, 0);
		Vec3 endPos = target.position().add(0, target.getBbHeight() * 0.5, 0);

		Vec3 direction = endPos.subtract(startPos).normalize();
		double distance = entity.distanceTo(target);
		double segmentLength = distance / CYLINDER_LENGTH_SEGMENTS;

		entity.playSound(SoundEvents.WITHER_SHOOT, 1.5f, 0.7f);

		// 生成光束
		for (int segment = 0; segment <= CYLINDER_LENGTH_SEGMENTS; segment++) {
			int delay = (int) (segment * segmentLength / TRAVEL_SPEED);
			int currentSegment = segment;

			DelayedTask.run(serverLevel, delay, () -> {
				if (!entity.isAlive()) return;

				double progress = (double) currentSegment / CYLINDER_LENGTH_SEGMENTS;
				Vec3 currentPos = startPos.add(direction.scale(distance * progress));

				float radiusFactor = 1.0f;
				if (progress < 0.3f) {
					radiusFactor = (float) (progress / 0.3f);
				} else if (progress > 0.7f) {
					radiusFactor = (float) (1.0f - (progress - 0.7f) / 0.3f);
				}

				float currentRadius = MIN_RADIUS + (MAX_RADIUS - MIN_RADIUS) * radiusFactor;

				for (int radial = 0; radial < RADIAL_SEGMENTS; radial++) {
					double angle = 2 * Math.PI * radial / RADIAL_SEGMENTS;

					Vec3 right = getPerpendicularVector(direction).normalize();
					Vec3 up = direction.cross(right).normalize();

					float radiusRatio = (float) radial / RADIAL_SEGMENTS;
					float particleRadius = currentRadius * radiusRatio;

					Vec3 radialOffset = right.scale(Math.cos(angle) * particleRadius)
							.add(up.scale(Math.sin(angle) * particleRadius));

					Vec3 particlePos = currentPos.add(radialOffset);

					// 颜色分布：核心金色 -> 外层金色 -> 边缘绿色
					DustParticleOptions particleType;
					if (radiusRatio < 0.25f) {
						particleType = CORE_GOLDEN;
					} else if (radiusRatio < 0.5f) {
						particleType = GOLDEN_INNER;
					} else if (radiusRatio < 0.75f) {
						particleType = GOLDEN_OUTER;
					} else {
						particleType = GREEN_EDGE;
					}

					float randomOffset = 0.1f;
					Vec3 finalPos = particlePos.add(
							(random.nextFloat() - 0.5f) * randomOffset,
							(random.nextFloat() - 0.5f) * randomOffset,
							(random.nextFloat() - 0.5f) * randomOffset);

					serverLevel.sendParticles(
							particleType, finalPos.x, finalPos.y, finalPos.z,
							1, 0, 0, 0, 0);

					// 额外闪电火花
					if (radiusRatio > 0.5f && random.nextFloat() < 0.15f) {
						serverLevel.sendParticles(
								ParticleTypes.ELECTRIC_SPARK,
								finalPos.x, finalPos.y, finalPos.z,
								1, 0.05, 0.05, 0.05, 0.01);
					}
				}

				// 核心闪电粒子
				if (currentSegment % 2 == 0) {
					Vec3 coreOffset = new Vec3(
							(random.nextFloat() - 0.5f) * 0.2,
							(random.nextFloat() - 0.5f) * 0.2,
							(random.nextFloat() - 0.5f) * 0.2);

					serverLevel.sendParticles(
							ParticleTypes.ELECTRIC_SPARK,
							currentPos.x + coreOffset.x,
							currentPos.y + coreOffset.y,
							currentPos.z + coreOffset.z,
							3, 0, 0, 0, 0.05);
				}

				// 到达终点时产生冲击
				if (currentSegment == CYLINDER_LENGTH_SEGMENTS) {
					createImpactEffect(serverLevel, endPos);
				}
			});
		}

		// 最终延迟后结束技能
		int totalDelay = (int) (distance / TRAVEL_SPEED) + 30;
		DelayedTask.run(serverLevel, totalDelay, this::onFinish);
	}

	/**
	 * 命中目标时的冲击效果
	 */
	private void createImpactEffect(ServerLevel serverLevel, Vec3 impactPos) {
		serverLevel.playSound(null, impactPos.x, impactPos.y, impactPos.z,
				SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0f, 0.7f);

		// 爆炸粒子
		for (int i = 0; i < 30; i++) {
			Vec3 offset = new Vec3(
					(random.nextDouble() - 0.5) * 2.0,
					(random.nextDouble() - 0.5) * 2.0,
					(random.nextDouble() - 0.5) * 2.0);

			DustParticleOptions particle = random.nextFloat() < 0.2f ? GREEN_EDGE : CORE_GOLDEN;
			serverLevel.sendParticles(particle,
					impactPos.x + offset.x,
					impactPos.y + offset.y,
					impactPos.z + offset.z,
					1, 0, 0, 0, 0.2);
		}

		// 扩散闪电环
		for (int ring = 0; ring < 3; ring++) {
			int currentRing = ring;
			DelayedTask.run(serverLevel, currentRing * 3, () -> {
				float radius = 1.0f + currentRing * 1.5f;
				for (int i = 0; i < 20; i++) {
					double angle = 2 * Math.PI * i / 20;
					double x = Math.cos(angle) * radius;
					double z = Math.sin(angle) * radius;

					DustParticleOptions particle;
					if (currentRing == 0) {
						particle = CORE_GOLDEN;
					} else if (currentRing == 1) {
						particle = GOLDEN_INNER;
					} else {
						particle = GREEN_EDGE;
					}

					serverLevel.sendParticles(particle,
							impactPos.x + x, impactPos.y + 0.5, impactPos.z + z,
							2, 0, 0.1, 0, 0.1);
				}
			});
		}

		// 应用范围伤害
		applyAreaDamage(impactPos);
	}

	/**
	 * 范围伤害
	 */
	private void applyAreaDamage(Vec3 center) {
		float explosionRadius = 4.0f;

		AABB aabb = new AABB(
				center.x - explosionRadius, center.y - explosionRadius, center.z - explosionRadius,
				center.x + explosionRadius, center.y + explosionRadius, center.z + explosionRadius);

		List<LivingEntity> entities = entity.level().getEntitiesOfClass(LivingEntity.class, aabb);

		float baseDamage = (float) entity.getAttributeValue(Attributes.ATTACK_DAMAGE);
		float healthBonus = entity.getHealth() * 0.015f;
		float maxDamage = baseDamage * 4.0f + healthBonus;

		for (LivingEntity targetEntity : entities) {
			if (targetEntity == entity) continue;
			if (!targetEntity.isAlive()) continue;

			double distance = targetEntity.distanceToSqr(center.x, center.y, center.z);
			if (distance > explosionRadius * explosionRadius) continue;

			float distanceFactor = 1.0f - (float) Math.sqrt(distance) / explosionRadius;
			float damage = maxDamage * distanceFactor;

			if (damage < 1) damage = 1;

			applyMultiHitDamage(targetEntity, damage);

			Vec3 knockbackDir = targetEntity.position().subtract(center).normalize();
			float knockbackStrength = 1.5f + distanceFactor * 1.5f;
			targetEntity.push(
					knockbackDir.x * knockbackStrength,
					0.3 + distanceFactor * 0.5,
					knockbackDir.z * knockbackStrength);

			if (entity.level() instanceof ServerLevel serverLevel) {
				serverLevel.sendParticles(
						ParticleTypes.ELECTRIC_SPARK,
						targetEntity.getX(),
						targetEntity.getY() + targetEntity.getBbHeight() * 0.5,
						targetEntity.getZ(),
						10, 0.3, 0.3, 0.3, 0.2);
			}
		}

		entity.playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.0f, 0.5f);
	}

	private void applyMultiHitDamage(LivingEntity target, float totalDamage) {
		float damagePerHit = totalDamage * 0.1f;

		for (int i = 0; i < DAMAGE_HITS; i++) {
			if (!target.isAlive()) break;

			DelayedTask.run(entity.level(), HIT_DELAY_TICKS * i, () -> {
				if (!target.isAlive()) return;

				target.invulnerableTime = 0;
				target.hurt(entity.damageSources().mobAttack(entity), damagePerHit);

				entity.level().playSound(null, target.getX(), target.getY(), target.getZ(),
						SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS,
						0.5f, 0.9f + random.nextFloat() * 0.2f);
			});
		}
	}

	private Vec3 getPerpendicularVector(Vec3 direction) {
		Vec3 up = new Vec3(0, 1, 0);
		Vec3 perpendicular = direction.cross(up);

		if (perpendicular.lengthSqr() < 0.0001) {
			perpendicular = direction.cross(new Vec3(1, 0, 0));
		}

		return perpendicular;
	}
}