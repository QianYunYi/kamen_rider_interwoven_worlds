package com.qianyunyi.rider.entity.kaijin.gurongi.gamio;

import com.qianyunyi.rider.entity.kaijin.gurongi.gamio.skills.AbstractGamioSkill;
import com.qianyunyi.rider.entity.kaijin.gurongi.gamio.skills.UltimateDarknessSkill;
import com.qianyunyi.rider.entity.projectile.TelekineticWaveProjectile;
import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.util.DelayedTask;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class NGamioZedaAttackGoal extends Goal {
	private final NGamioZedaEntity entity;

	// === 距离常量 ===
	private static final double GROUND_ATTACK_DISTANCE = 3.0;     // 地面近战攻击距离
	private static final double AIR_SAFE_DISTANCE = 10.0;         // 空中环绕半径
	private static final double AIR_ATTACK_MIN_DISTANCE = 5.0;    // 空中最小距离（太近就远离）
	private static final double AIR_ATTACK_MAX_DISTANCE = 30.0;   // 空中最大有效射程
	private static final double MAX_HEIGHT_ABOVE_TARGET = 10.0;   // 浮空最大高度（不能超过目标上方10格）
	private static final double RISE_SPEED = 0.05;                // 高度修正系数（优先水平移动）
	private static final double MAX_Y_SPEED = 0.5;                // Y轴最大速度（防止大误差时失控）
	private static final double Y_DEADBAND = 0.3;                 // 高度死区

	// === 冷却常量 ===
	private static final int SKILL_CHECK_INTERVAL = 30;     // 技能检查间隔 (1.5秒)
	private static final int AIR_ATTACK_COOLDOWN = 15;      // 空中攻击间隔 (0.75秒)
	private static final int PROJECTILE_SPEED = 1;          // 念力波速度
	private static final int PROJECTILE_FIRE_DELAY = 10;    // 发射投掷物延迟 (0.5秒)
	private static final int GROUND_ATTACK_COOLDOWN = 30;   // 地面攻击间隔 (1.5秒)

	// === 战斗锁 ===
	// 战斗锁期间不能发动任何攻击或技能
	private static final int GROUND_ATTACK_LOCK = 10;   // 地面普攻锁定时长 (0.5S)
	private static final int AIR_ATTACK_LOCK = 15;       // 空中普攻锁定时长 (0.75S)

	// === 状态变量 ===
	private int skillCheckCooldown = 0;
	private int airAttackCooldown = 0;
	private int groundAttackCooldown = 0;
	private int combatLock = 0; // 战斗锁：>0 时禁止攻击和技能

	// 空中环绕角度
	private float orbitAngle = 0;

	public NGamioZedaAttackGoal(NGamioZedaEntity entity) {
		this.entity = entity;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (entity.isStunned()) return false;
		LivingEntity target = entity.getTarget();
		return target != null && target.isAlive();
	}

	@Override
	public boolean canContinueToUse() {
		if (entity.isStunned()) return false;
		LivingEntity target = entity.getTarget();
		if (target == null || !target.isAlive()) return false;
		return true;
	}

	@Override
	public void start() {
		skillCheckCooldown = 0;
		airAttackCooldown = 0;
		groundAttackCooldown = 0;
		combatLock = 0;
		orbitAngle = entity.getRandom().nextFloat() * 360;
		entity.setBossState(NGamioZedaEntity.BossState.COMBAT);

		// 进入战斗时自动浮空（除非被击落冷却中）
		if (!entity.isFloating() && entity.getTelekineticCooldown() <= 0) {
			entity.setFloating(true);
		}
	}

	@Override
	public void stop() {
		entity.getNavigation().stop();
	}

	@Override
	public void tick() {
		if (entity.isStunned() || entity.level() == null) return;

		LivingEntity target = entity.getTarget();
		if (target == null || !target.isAlive()) return;

		// 锁定旋转朝向目标
		updateRotationToTarget(target);

		double distanceSqr = entity.distanceToSqr(target);
		double distance = Math.sqrt(distanceSqr);

		// ===== 技能施放中 — 只移动不攻击（使用技能专属锁定时长） =====
		if (entity.isCastingSkill()) {
			if (combatLock <= 0) {
				combatLock = entity.getSkillManager().getCurrentSkillLockDuration();
			}
			combatLock--;
			moveTowardTarget(target);
			return;
		}

		// ===== 冷却递减 =====
		if (skillCheckCooldown > 0) skillCheckCooldown--;
		if (airAttackCooldown > 0) airAttackCooldown--;
		if (groundAttackCooldown > 0) groundAttackCooldown--;
		if (combatLock > 0) combatLock--;

		// ===== 战斗锁 — 不能攻击也不能使用技能 =====
		if (combatLock > 0) {
			moveTowardTarget(target);
			return;
		}

		// ===== 如果目标是村民或灾厄村民，优先释放究极黑暗 =====
		if ((target instanceof Villager || target instanceof AbstractIllager) && skillCheckCooldown <= 0) {
			KamenRiderInterwovenWorldsMod.LOGGER.debug("[狼王AI] Villager/Illager targeted! distance={}, skillCheckCooldown={}",
					distance, skillCheckCooldown);
			// 用究极黑暗技能的射程作为距离判断
			double ultimateDarknessRange = 30.0;
			if (distance <= ultimateDarknessRange) {
				skillCheckCooldown = SKILL_CHECK_INTERVAL;
				// 尝试执行究极黑暗技能
				java.util.List<AbstractGamioSkill> availableSkills = entity.getSkillManager().getAvailableSkills(target);
				KamenRiderInterwovenWorldsMod.LOGGER.debug("[狼王AI] Available skills for villager target: {} skills", availableSkills.size());
				for (AbstractGamioSkill skill : availableSkills) {
					if (skill instanceof UltimateDarknessSkill && skill.canExecute(target)) {
						KamenRiderInterwovenWorldsMod.LOGGER.debug("[狼王AI] Attempting to execute UltimateDarknessSkill...");
						boolean executed = entity.getSkillManager().executeSkill(skill);
						KamenRiderInterwovenWorldsMod.LOGGER.debug("[狼王AI] executeSkill returned: {}", executed);
						if (executed) {
							return;
						}
					}
				}
				KamenRiderInterwovenWorldsMod.LOGGER.debug("[狼王AI] UltimateDarknessSkill not available/executable for villager target");
			} else {
				KamenRiderInterwovenWorldsMod.LOGGER.debug("[狼王AI] Villager too far: distance={} > range={}", distance, ultimateDarknessRange);
			}
		}

		// ===== 分隔地面和空中逻辑 =====
		if (entity.isFloating()) {
			handleAirCombat(target, distance);
		} else {
			handleGroundCombat(target, distance);
		}

		// ===== 技能检查（仅当未锁定） =====
		if (combatLock <= 0 && skillCheckCooldown <= 0 && distance <= 24.0) {
			skillCheckCooldown = SKILL_CHECK_INTERVAL;
			if (entity.getSkillManager().tryAutoExecute(target, distance)) {
				return;
			}
		}
	}

	// ========================
	//      空中战斗逻辑
	// ========================

	/**
	 * 空中战斗：以水平移动为主，高度保持在目标上方5格左右，最高不超过10格
	 */
	private void handleAirCombat(LivingEntity target, double distance) {
		// 1. 高度控制：目标高度为目标上方5格，但不超过最大限制10格
		double targetY = target.getY() + 5.0;
		double maxY = target.getY() + MAX_HEIGHT_ABOVE_TARGET;
		Vec3 motion = entity.getDeltaMovement();

		if (entity.getY() > maxY) {
			// 超过最大高度限制 → 只降不升，缓慢下降
			entity.setDeltaMovement(motion.x, -RISE_SPEED * 2, motion.z);
		} else {
			double dyError = targetY - entity.getY();
			if (Math.abs(dyError) > Y_DEADBAND) {
				// 在死区外，缓慢修正高度（RISE_SPEED已降低，优先水平移动）
				double dy = dyError * RISE_SPEED;
				dy = Math.max(-MAX_Y_SPEED, Math.min(MAX_Y_SPEED, dy));
				entity.setDeltaMovement(motion.x, dy, motion.z);
			} else {
				// 死区内锁定Y轴速度为零，彻底消除振荡
				entity.setDeltaMovement(motion.x, 0, motion.z);
			}
		}

		// 2. 距离控制
		if (distance < AIR_ATTACK_MIN_DISTANCE) {
			// 太近了，后退
			Vec3 away = entity.position().subtract(target.position()).normalize();
			double speed = 0.4 * (1.0 - distance / AIR_ATTACK_MIN_DISTANCE);
			entity.setDeltaMovement(away.x * speed, entity.getDeltaMovement().y, away.z * speed);
		} else if (distance > AIR_ATTACK_MAX_DISTANCE) {
			// 太远了，靠近（加速接近以便尽快进入射程）
			Vec3 toTarget = target.position().subtract(entity.position()).normalize();
			entity.setDeltaMovement(toTarget.x * 0.5, entity.getDeltaMovement().y, toTarget.z * 0.5);
		} else {
			// 安全范围内，环绕飞行
			orbitAngle += 1.5f;
			if (orbitAngle > 360) orbitAngle -= 360;

			double rad = Math.toRadians(orbitAngle);
			double targetX = target.getX() + Math.cos(rad) * AIR_SAFE_DISTANCE;
			double targetZ = target.getZ() + Math.sin(rad) * AIR_SAFE_DISTANCE;

			Vec3 targetPos = new Vec3(targetX, targetY, targetZ);
			Vec3 toTarget = targetPos.subtract(entity.position()).normalize();
			entity.setDeltaMovement(toTarget.x * 0.25, entity.getDeltaMovement().y, toTarget.z * 0.25);
		}

		// 3. 发射念力波：只要有目标且在最小射程外就发射
		// 空中战斗以远程攻击为主，距离>5格就射击
		if (airAttackCooldown <= 0 && distance > AIR_ATTACK_MIN_DISTANCE) {
			fireTelekineticWave(target);
			airAttackCooldown = AIR_ATTACK_COOLDOWN;
		}
	}

	/**
	 * 发射念力波弹射物
	 * 锁定战斗 0.75S (15 ticks)，期间禁止技能和攻击
	 */
	private void fireTelekineticWave(LivingEntity target) {
		if (entity.level().isClientSide) return;

		// 随机选择attack1或attack2动画
		int attackIndex = entity.getNextFloatingAttackIndex();
		String attackAnim = attackIndex == 0 ? "attack1" : "attack2";
		entity.setAnimation(attackAnim, AIR_ATTACK_LOCK + 3);

		// 设置战斗锁，期间禁止技能和攻击
		combatLock = AIR_ATTACK_LOCK;

		// 延迟后发射，配合动画（0.5秒时发射）
		// 使用entity.getTarget()而不是捕获的target，确保目标始终是最新的
		DelayedTask.run(entity.level(), PROJECTILE_FIRE_DELAY, () -> {
			if (!entity.isAlive() || entity.isRemoved()) return;
			if (!(entity.level() instanceof ServerLevel serverLevel)) return;

			LivingEntity currentTarget = entity.getTarget();
			if (currentTarget == null || !currentTarget.isAlive()) return;

			// 创建念力波弹射物
			TelekineticWaveProjectile wave = new TelekineticWaveProjectile(entity.level(), entity);
			Vec3 entityPos = entity.position().add(0, entity.getEyeHeight() * 0.8, 0);

			// 瞄准目标中心（50%身高）
			Vec3 targetPos = currentTarget.position().add(0, currentTarget.getBbHeight() * 0.5, 0);

			// 计算方向向量
			Vec3 direction = targetPos.subtract(entityPos).normalize();

			// 设置弹射物位置和速度（无重力直线飞行，不放逸散）
			wave.setPos(entityPos.x, entityPos.y, entityPos.z);
			wave.shoot(direction.x, direction.y, direction.z, PROJECTILE_SPEED * 1.5f, 0.0f);

			// 设置伤害（80%近战伤害）
			float damage = (float) entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.8f;
			wave.setBaseDamage(damage);

			serverLevel.addFreshEntity(wave);

			// 发射音效
			entity.playSound(SoundEvents.BLAZE_SHOOT, 0.8f, 0.5f);
		});
	}

	// ========================
	//      地面战斗逻辑
	// ========================

	/**
	 * 地面战斗：使用导航系统接近目标进行近战攻击
	 */
	private void handleGroundCombat(LivingEntity target, double distance) {
		// 非攻击距离时，使用导航系统接近
		if (distance > GROUND_ATTACK_DISTANCE) {
			moveTowardTarget(target);
		}

		// 近战攻击
		if (distance <= GROUND_ATTACK_DISTANCE && groundAttackCooldown <= 0) {
			entity.getNavigation().stop();
			executeGroundAttack(target);
			groundAttackCooldown = GROUND_ATTACK_COOLDOWN;
		}
	}

	/**
	 * 使用导航系统向目标移动（地面/技能封锁时通用）
	 */
	private void moveTowardTarget(LivingEntity target) {
		entity.getNavigation().moveTo(target, 1.0);
	}

	/**
	 * 地面攻击：播放attack动画，显示tail，0.5S和1S各造成一次伤害
	 * 锁定战斗 0.5S (10 ticks)，期间禁止技能和攻击
	 */
	private void executeGroundAttack(LivingEntity target) {
		entity.setAnimation("attack", GROUND_ATTACK_LOCK + 3);
		entity.showTailTemporary();
		entity.setDeltaMovement(Vec3.ZERO);

		// 设置战斗锁，期间禁止技能和攻击
		combatLock = GROUND_ATTACK_LOCK;

		// 0.5S (10 ticks) 第一次伤害
		DelayedTask.run(entity.level(), 10, () -> {
			if (isTargetValid(target)) {
				updateRotationToTarget(target);
				target.hurt(entity.damageSources().mobAttack(entity),
						(float) entity.getAttributeValue(Attributes.ATTACK_DAMAGE));
				playStrikeSound();
				addParticleEffect(ParticleTypes.CRIT, target);
			}
		});

		// 1.0S (20 ticks) 第二次伤害
		DelayedTask.run(entity.level(), 20, () -> {
			if (isTargetValid(target)) {
				updateRotationToTarget(target);
				target.hurt(entity.damageSources().mobAttack(entity),
						(float) entity.getAttributeValue(Attributes.ATTACK_DAMAGE));
				playStrikeSound();
				addParticleEffect(ParticleTypes.CRIT, target);
			}
		});
	}

	// ========== 辅助方法 ==========

	private boolean isTargetValid(LivingEntity target) {
		return target != null && target.isAlive() && !entity.isRemoved();
	}

	private void updateRotationToTarget(LivingEntity target) {
		entity.getLookControl().setLookAt(target, 60.0F, 60.0F);
		double dx = target.getX() - entity.getX();
		double dz = target.getZ() - entity.getZ();
		float yaw = (float) (Math.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
		entity.setYRot(yaw);
		entity.yBodyRot = yaw;
		entity.yBodyRotO = yaw;
	}

	private void playStrikeSound() {
		entity.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
	}

	private void addParticleEffect(net.minecraft.core.particles.SimpleParticleType particleType, LivingEntity target) {
		if (entity.level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(particleType,
					target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
					10, 0.3, 0.3, 0.3, 0.1);
		}
	}
}