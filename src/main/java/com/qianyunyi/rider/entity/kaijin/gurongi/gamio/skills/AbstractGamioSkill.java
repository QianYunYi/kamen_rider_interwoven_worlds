package com.qianyunyi.rider.entity.kaijin.gurongi.gamio.skills;

import com.qianyunyi.rider.entity.kaijin.gurongi.gamio.NGamioZedaEntity;
import net.minecraft.world.entity.LivingEntity;

public abstract class AbstractGamioSkill {
	protected final NGamioZedaEntity entity;
	protected LivingEntity target;
	protected boolean isExecuting;

	protected AbstractGamioSkill(NGamioZedaEntity entity) {
		this.entity = entity;
	}

	public boolean canExecute(LivingEntity target) {
		return true;
	}

	public abstract int getCooldown();

	public abstract int getPriority();

	public abstract double getMaxRange();

	public boolean requiresFacingTarget() {
		return false;
	}

	/**
	 * 技能释放期间封锁普攻和其他技能的时长 (ticks)
	 * 默认 1S (20 ticks)
	 */
	public int getSkillLockDuration() {
		return 20;
	}

	public void execute() {
		this.target = entity.getTarget();
		if (target == null || !canExecute(target)) {
			return;
		}
		if (target != null) {
			rotateToTarget(target);
		}
		isExecuting = true;
		entity.setUsingSkill(true);
		onStart();
	}

	private void rotateToTarget(LivingEntity target) {
		entity.getLookControl().setLookAt(target, 60.0F, 60.0F);
		double dx = target.getX() - entity.getX();
		double dz = target.getZ() - entity.getZ();
		float yaw = (float) (Math.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
		entity.setYRot(yaw);
		entity.yBodyRot = yaw;
		entity.yBodyRotO = yaw;
	}

	protected abstract void onStart();

	protected void onFinish() {
		isExecuting = false;
		entity.setUsingSkill(false);
		// 重置动画，让移动控制器接管
		entity.setAnimation(NGamioZedaEntity.ANIM_NONE);
	}

	public boolean isExecuting() {
		return isExecuting;
	}
}