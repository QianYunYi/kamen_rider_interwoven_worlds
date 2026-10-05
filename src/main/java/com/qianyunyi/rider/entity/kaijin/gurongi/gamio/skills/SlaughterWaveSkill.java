package com.qianyunyi.rider.entity.kaijin.gurongi.gamio.skills;

import com.qianyunyi.rider.entity.kaijin.gurongi.gamio.NGamioZedaEntity;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.MessageSupernaturalKillingIntent;
import com.qianyunyi.rider.util.DelayedTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SlaughterWaveSkill extends AbstractGamioSkill {

	private static final double SUPPRESSION_RADIUS = 16.0;
	private static final float KILL_HEALTH_THRESHOLD_RATIO = 0.05f;
	private static final double KNOCKBACK_STRENGTH = 3.0;
	private static final int SUPPRESSION_DURATION = 400;

	public SlaughterWaveSkill(NGamioZedaEntity entity) {
		super(entity);
	}

	@Override
	public boolean canExecute(LivingEntity target) {
		return entity.isAlive() && !entity.isSupernaturalKillingIntentActive();
	}

	@Override
	public int getCooldown() {
		return 600;
	}

	@Override
	public int getPriority() {
		return 3;
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
		entity.setAnimation("supernatural", 20); // 技能锁时长=1S
		entity.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0f, 0.3f);

		DelayedTask.run(entity.level(), 10, () -> {
			try {
				executeBurst();
				activateAura();
			} finally {
				DelayedTask.run(entity.level(), 5, this::onFinish);
			}
		});
	}

	private void executeBurst() {
		if (!entity.isAlive() || entity.level().isClientSide()) {
			return;
		}

		ServerLevel serverLevel = (ServerLevel) entity.level();
		Vec3 center = entity.position();

		double range = SUPPRESSION_RADIUS;
		AABB area = new AABB(
				center.x - range, center.y - 2.0, center.z - range,
				center.x + range, center.y + 5.0, center.z + range
		);

		float entityHealth = entity.getHealth();

		for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
			if (target == entity) continue;
			if (!target.isAlive()) continue;

			double distance = target.position().distanceTo(center);
			if (distance > range) continue;

			float damage = entityHealth * 0.5f;
			float killThreshold = entityHealth * KILL_HEALTH_THRESHOLD_RATIO;

			if (target.getHealth() <= killThreshold) {
				target.hurt(entity.damageSources().mobAttack(entity), 9999.0f);
			} else {
				target.hurt(entity.damageSources().mobAttack(entity), damage);
			}

			Vec3 toTarget = target.position().subtract(center);
			double dist = toTarget.length();
			if (dist > 0.0) {
				Vec3 knockbackDir = toTarget.normalize();
				double distanceFactor = 1.0 - (dist / range);
				double finalKnockback = KNOCKBACK_STRENGTH * distanceFactor;
				target.push(knockbackDir.x * finalKnockback, 0.5, knockbackDir.z * finalKnockback);
			}
		}

		entity.playSound(SoundEvents.GENERIC_EXPLODE, 2.0f, 0.5f);
	}

	private void activateAura() {
		if (!entity.isAlive() || entity.level().isClientSide()) {
			return;
		}

		entity.activateSupernaturalKillingIntent(SUPPRESSION_DURATION);

		float gamioMaxHealth = entity.getMaxHealth();
		float healthThreshold = gamioMaxHealth * 0.15f;

		ServerLevel serverLevel = (ServerLevel) entity.level();
		for (ServerPlayer player : serverLevel.getPlayers(p -> true)) {
			double dist = player.distanceTo(entity);
			if (dist <= SUPPRESSION_RADIUS * 2 && player.getHealth() < healthThreshold) {
				NetworkHandler.sendTo(player,
						new MessageSupernaturalKillingIntent(true, SUPPRESSION_DURATION, 1.0f));
			}
		}
	}
}