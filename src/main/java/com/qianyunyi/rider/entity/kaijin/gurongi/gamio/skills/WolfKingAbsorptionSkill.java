package com.qianyunyi.rider.entity.kaijin.gurongi.gamio.skills;

import com.qianyunyi.rider.entity.kaijin.gurongi.gamio.NGamioZedaEntity;
import com.qianyunyi.rider.util.DelayedTask;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;

/**
 * 同类吸收（狼王标记吸收）
 * 狼王血量低于30%后释放
 * 动画wolf_howl
 * 所有附近（30x30）有狼王标记的古朗基，向着狼王靠近，
 * 所有古朗基变成一团黑雾，沿着抛物线飞向狼王，狼王恢复到100%血量
 * 如果释放的时候附近没有古朗基则不会释放成功
 */
public class WolfKingAbsorptionSkill extends AbstractGamioSkill {

	// 黑雾粒子
	private static final DustParticleOptions BLACK_FOG = new DustParticleOptions(
			new Vector3f(0.05f, 0.05f, 0.05f), 3.0f);
	private static final DustParticleOptions DARK_ENERGY = new DustParticleOptions(
			new Vector3f(0.3f, 0.0f, 0.4f), 2.5f);
	private static final DustParticleOptions BLOOD_RED = new DustParticleOptions(
			new Vector3f(0.6f, 0.0f, 0.0f), 2.0f);

	// 吸收范围
	private static final double ABSORPTION_RANGE = 30.0;

	// 生命阈值（低于30%触发）
	private static final float HP_THRESHOLD = 0.30f;

	// 飞行动画时长
	private static final int FLIGHT_DURATION_TICKS = 30; // 1.5秒飞行

	public WolfKingAbsorptionSkill(NGamioZedaEntity entity) {
		super(entity);
	}

	@Override
	public void execute() {
		// 同类吸收不需要目标，重写execute跳过目标检查
		if (!entity.isAlive() || !canExecute(entity.getTarget())) {
			return;
		}
		isExecuting = true;
		entity.setUsingSkill(true);
		onStart();
	}

	@Override
	public boolean canExecute(LivingEntity target) {
		if (!entity.isAlive()) return false;
		// 血量低于30%时才能触发
		float healthRatio = entity.getHealth() / entity.getMaxHealth();
		if (healthRatio > HP_THRESHOLD) return false;
		// 检查附近是否有狼王标记的古朗基
		return hasWolfPackMemberInRange();
	}

	/**
	 * 检查范围内是否有狼王标记的古朗基
	 */
	private boolean hasWolfPackMemberInRange() {
		if (entity.level().isClientSide()) return false;
		ServerLevel serverLevel = (ServerLevel) entity.level();
		Set<UUID> packMembers = entity.getWolfPackMembers();
		if (packMembers.isEmpty()) return false;

		Vec3 center = entity.position();
		for (UUID memberId : packMembers) {
			Entity member = serverLevel.getEntity(memberId);
			if (member != null && member.isAlive() && member.distanceToSqr(entity) <= ABSORPTION_RANGE * ABSORPTION_RANGE) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int getCooldown() {
		return 40; // 最小2秒冷却，防止空转重试
	}

	@Override
	public int getPriority() {
		return 0; // 最高优先级
	}

	@Override
	public double getMaxRange() {
		return ABSORPTION_RANGE;
	}

	@Override
	public int getSkillLockDuration() {
		return FLIGHT_DURATION_TICKS + 20; // 飞行时长+收尾
	}

	@Override
	protected void onStart() {
		// 再次检查是否有可吸收的目标
		if (!hasWolfPackMemberInRange()) {
			onFinish();
			return;
		}

		// 播放狼嚎动画
		entity.setAnimation("wolf_howl", getSkillLockDuration());
		entity.playSound(SoundEvents.WOLF_GROWL, 2.0f, 0.5f);

		// 播放蓄力粒子效果
		if (entity.level() instanceof ServerLevel serverLevel) {
			spawnChargeEffect(serverLevel);
		}

		// 延迟0.5秒后执行吸收
		DelayedTask.run(entity.level(), 10, () -> {
			if (!entity.isAlive()) return;
			executeAbsorption();
		});
	}

	/**
	 * 蓄力粒子效果
	 */
	private void spawnChargeEffect(ServerLevel serverLevel) {
		Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.5, 0);

		for (int i = 0; i < 20; i++) {
			int delay = i;
			DelayedTask.run(serverLevel, delay, () -> {
				if (!entity.isAlive()) return;
				double radius = 1.0 + delay * 0.3;
				double angle = 2 * Math.PI * delay / 10;

				double x = center.x + Math.cos(angle) * radius;
				double z = center.z + Math.sin(angle) * radius;
				double y = center.y + (new Random().nextDouble() - 0.5) * 2.0;

				serverLevel.sendParticles(
						DARK_ENERGY, x, y, z,
						1, 0.1, 0.1, 0.1, 0.02
				);
			});
		}

		// 地面光环
		DelayedTask.run(serverLevel, 8, () -> {
			if (!entity.isAlive()) return;
			for (int i = 0; i < 16; i++) {
				double angle = 2 * Math.PI * i / 16;
				double radius = 3.0;
				serverLevel.sendParticles(
						BLOOD_RED,
						center.x + Math.cos(angle) * radius,
						center.y - 0.5,
						center.z + Math.sin(angle) * radius,
						1, 0.1, 0.02, 0.1, 0.01
				);
			}
		});
	}

	/**
	 * 执行吸收
	 * 所有有狼王标记的古朗基飞向狼王，狼王恢复100%血量
	 */
	private void executeAbsorption() {
		if (!entity.isAlive() || entity.level().isClientSide()) return;
		if (!(entity.level() instanceof ServerLevel serverLevel)) return;

		Set<UUID> packMembers = entity.getWolfPackMembers();
		if (packMembers.isEmpty()) return;

		Vec3 wolfKingPos = entity.position().add(0, entity.getBbHeight() * 0.3, 0);
		List<Mob> flyingMembers = new ArrayList<>();
		List<Vec3> startPositions = new ArrayList<>();

		// 收集范围内有标记的古朗基
		Iterator<UUID> iterator = packMembers.iterator();
		while (iterator.hasNext()) {
			UUID memberId = iterator.next();
			Entity member = serverLevel.getEntity(memberId);

			if (member == null || !member.isAlive()) {
				// 清除无效成员
				iterator.remove();
				continue;
			}

			double distSqr = member.distanceToSqr(entity);
			if (distSqr > ABSORPTION_RANGE * ABSORPTION_RANGE) continue;

			if (member instanceof Mob mob) {
				flyingMembers.add(mob);
				startPositions.add(mob.position());
			}
		}

		if (flyingMembers.isEmpty()) {
			// 没有可吸收的目标，取消技能
			onFinish();
			return;
		}

		// 飞行动画：每个古朗基沿抛物线飞向狼王
		for (int i = 0; i < flyingMembers.size(); i++) {
			Mob member = flyingMembers.get(i);
			Vec3 startPos = startPositions.get(i);
			int memberIndex = i;

			// 标记为飞行中（不可交互）
			member.setNoGravity(true);
			member.setInvulnerable(true);
			member.getNavigation().stop();

			// 逐帧更新位置，形成抛物线
			for (int tick = 0; tick <= FLIGHT_DURATION_TICKS; tick++) {
				final int currentTick = tick;
				DelayedTask.run(serverLevel, currentTick, () -> {
					if (!entity.isAlive() || member.isRemoved()) return;

					double progress = (double) currentTick / FLIGHT_DURATION_TICKS;
					// 抛物线：x,z线性插值，y抛物线（先升后降）
					double x = startPos.x + (wolfKingPos.x - startPos.x) * progress;
					double z = startPos.z + (wolfKingPos.z - startPos.z) * progress;
					// 抛物线高度：在中间点最高，上升8格
					double parabolaHeight = 8.0 * Math.sin(Math.PI * progress);
					double y = startPos.y + (wolfKingPos.y - startPos.y) * progress + parabolaHeight;

					member.setPos(x, y, z);

					// 生成黑雾粒子包围
					for (int p = 0; p < 3; p++) {
						double px = x + (new Random().nextDouble() - 0.5) * 1.5;
						double py = y + (new Random().nextDouble() - 0.5) * 1.5;
						double pz = z + (new Random().nextDouble() - 0.5) * 1.5;
						serverLevel.sendParticles(
								BLACK_FOG, px, py, pz,
								1, 0.2, 0.2, 0.2, 0.01
						);
						if (new Random().nextFloat() < 0.3f) {
							serverLevel.sendParticles(
									DARK_ENERGY, px, py, pz,
									1, 0.1, 0.1, 0.1, 0.01
							);
						}
					}

					// 到达终点时爆炸
					if (currentTick == FLIGHT_DURATION_TICKS) {
						// 爆炸粒子
						serverLevel.sendParticles(
								ParticleTypes.EXPLOSION,
								x, y, z,
								1, 0.3, 0.3, 0.3, 0.1
						);
						serverLevel.sendParticles(
								BLOOD_RED, x, y, z,
								10, 0.5, 0.5, 0.5, 0.05
						);
						// 移除古朗基
						member.remove(Entity.RemovalReason.DISCARDED);
					}
				});
			}
		}

		// 所有古朗基到达后，狼王恢复100%血量
		DelayedTask.run(serverLevel, FLIGHT_DURATION_TICKS, () -> {
			if (!entity.isAlive()) return;

			// 恢复100%血量
			entity.setHealth(entity.getMaxHealth());

			// 清空狼群列表（已全部吸收）
			entity.clearWolfPackMembers();

			// 治疗爆发粒子效果
			Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
			for (int i = 0; i < 20; i++) {
				double angle = 2 * Math.PI * i / 20;
				double radius = 2.0;
				serverLevel.sendParticles(
						ParticleTypes.HEART,
						center.x + Math.cos(angle) * radius,
						center.y + (new Random().nextDouble() - 0.5) * 2.0,
						center.z + Math.sin(angle) * radius,
						1, 0.1, 0.1, 0.1, 0.05
				);
			}

			entity.playSound(SoundEvents.BEACON_ACTIVATE, 2.0f, 0.7f);

			// 吸收完成，结束技能
			onFinish();
		});

		// 如果没有可吸收的古朗基（意外情况），提前结束
		if (flyingMembers.isEmpty()) {
			onFinish();
		}
	}

	@Override
	protected void onFinish() {
		super.onFinish();
	}
}