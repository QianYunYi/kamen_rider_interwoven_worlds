package com.qianyunyi.rider.entity.kaijin.gurongi.gamio.skills;

import com.qianyunyi.rider.entity.kaijin.gurongi.base.Gurongi;
import com.qianyunyi.rider.entity.kaijin.gurongi.gamio.NGamioZedaEntity;
import com.qianyunyi.rider.util.DelayedTask;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 究极黑暗
 * 释放一层黑雾笼罩天空，范围内村民随机转换成拉集团以下的古朗基，
 * 被转换的古朗基获得狼王标记，属性为原版的50%，
 * 会跟随狼王移动和攻击目标，冷却30S
 */
public class UltimateDarknessSkill extends AbstractGamioSkill {

	private static final Random RANDOM = new Random();

	// 黑雾粒子
	private static final DustParticleOptions BLACK_FOG = new DustParticleOptions(
			new Vector3f(0.05f, 0.05f, 0.05f), 4.0f);
	private static final DustParticleOptions DARK_PURPLE = new DustParticleOptions(
			new Vector3f(0.15f, 0.0f, 0.2f), 3.0f);

	// 转换范围（球形半径）
	private static final double CONVERSION_RADIUS = 30.0;

	// 技能锁定时长
	private static final int SKILL_LOCK_TICKS = 40; // 2秒

	public UltimateDarknessSkill(NGamioZedaEntity entity) {
		super(entity);
	}

	@Override
	public boolean canExecute(LivingEntity target) {
		if (!entity.isAlive()) {
			KamenRiderInterwovenWorldsMod.LOGGER.debug("[究极黑暗] canExecute = false, entity not alive");
			return false;
		}
		// 周围需要有村民或灾厄村民才能执行
		if (target != null && target.isAlive() && (target instanceof Villager || target instanceof AbstractIllager)) {
			KamenRiderInterwovenWorldsMod.LOGGER.debug("[究极黑暗] canExecute = true, target is villager/illager: {} @ pos=({}, {}, {})",
					target.getStringUUID(), target.getX(), target.getY(), target.getZ());
			return true;
		}
		// 也可以检查范围内是否有村民/灾厄村民
		if (target == null) {
			KamenRiderInterwovenWorldsMod.LOGGER.debug("[究极黑暗] canExecute = false, target is null");
			return false;
		}
		// 如果目标不是村民/灾厄村民但附近有，也可执行
		boolean inRange = hasConvertibleInRange();
		KamenRiderInterwovenWorldsMod.LOGGER.debug("[究极黑暗] canExecute = {} (target not convertible, hasConvertibleInRange={})", inRange, inRange);
		return inRange;
	}

	/**
	 * 检查球形范围内是否有村民或灾厄村民
	 */
	private boolean hasConvertibleInRange() {
		if (!(entity.level() instanceof ServerLevel serverLevel)) return false;
		Vec3 center = entity.position();
		AABB area = new AABB(
				center.x - CONVERSION_RADIUS, center.y - CONVERSION_RADIUS, center.z - CONVERSION_RADIUS,
				center.x + CONVERSION_RADIUS, center.y + CONVERSION_RADIUS, center.z + CONVERSION_RADIUS
		);
		List<Villager> villagersInBox = serverLevel.getEntitiesOfClass(Villager.class, area, Villager::isAlive);
		List<AbstractIllager> illagersInBox = serverLevel.getEntitiesOfClass(AbstractIllager.class, area, AbstractIllager::isAlive);
		KamenRiderInterwovenWorldsMod.LOGGER.debug("[究极黑暗] hasConvertibleInRange: AABB found {} villagers and {} illagers in range {}", 
				villagersInBox.size(), illagersInBox.size(), CONVERSION_RADIUS);
		// 球形距离检查 - 村民
		for (Villager v : villagersInBox) {
			if (v.distanceToSqr(entity) <= CONVERSION_RADIUS * CONVERSION_RADIUS) {
				KamenRiderInterwovenWorldsMod.LOGGER.debug("[究极黑暗] hasConvertibleInRange: found villager in sphere range, returning true");
				return true;
			}
		}
		// 球形距离检查 - 灾厄村民
		for (AbstractIllager i : illagersInBox) {
			if (i.distanceToSqr(entity) <= CONVERSION_RADIUS * CONVERSION_RADIUS) {
				KamenRiderInterwovenWorldsMod.LOGGER.debug("[究极黑暗] hasConvertibleInRange: found illager in sphere range, returning true");
				return true;
			}
		}
		KamenRiderInterwovenWorldsMod.LOGGER.debug("[究极黑暗] hasConvertibleInRange: no convertible targets in sphere range, returning false");
		return false;
	}

	@Override
	public int getCooldown() {
		return 600; // 30秒 = 600 ticks
	}

	@Override
	public int getPriority() {
		return 0; // 最高优先级（数字越小优先级越高）
	}

	@Override
	public double getMaxRange() {
		return CONVERSION_RADIUS;
	}

	@Override
	public int getSkillLockDuration() {
		return SKILL_LOCK_TICKS;
	}

	@Override
	protected void onStart() {
		KamenRiderInterwovenWorldsMod.LOGGER.debug("[究极黑暗] onStart called, entity=({}, {}, {}), level={}",
				entity.getX(), entity.getY(), entity.getZ(),
				entity.level().isClientSide() ? "CLIENT" : "SERVER");
		// 播放究极黑暗动画
		entity.setAnimation("teleport", SKILL_LOCK_TICKS);
		entity.playSound(SoundEvents.WITHER_SPAWN, 2.0f, 0.3f);

		// 播放黑雾笼罩天空的效果
		if (entity.level() instanceof ServerLevel serverLevel) {
			spawnBlackFogEffect(serverLevel);
		} else {
			KamenRiderInterwovenWorldsMod.LOGGER.warn("[究极黑暗] onStart: level is NOT ServerLevel, effects skipped!");
		}

		// 延迟1秒后执行转换
		KamenRiderInterwovenWorldsMod.LOGGER.debug("[究极黑暗] Scheduling conversion task (20 ticks delay)...");
		java.util.UUID taskId = DelayedTask.run(entity.level(), 20, () -> {
			try {
				if (!entity.isAlive()) {
					KamenRiderInterwovenWorldsMod.LOGGER.debug("[究极黑暗] Conversion task: entity not alive, aborting");
					return;
				}
				KamenRiderInterwovenWorldsMod.LOGGER.debug("[究极黑暗] Conversion task executing...");
				executeConversion();
			} finally {
				onFinish();
			}
		});
		KamenRiderInterwovenWorldsMod.LOGGER.debug("[究极黑暗] DelayedTask.run returned taskId={}", taskId);
	}

	/**
	 * 生成黑雾笼罩天空的粒子效果
	 */
	private void spawnBlackFogEffect(ServerLevel serverLevel) {
		Vec3 center = entity.position().add(0, 12.0, 0); // 头顶上方12格

		// 多波粒子扩散效果
		for (int wave = 0; wave < 5; wave++) {
			int delay = wave * 4;
			int finalWave = wave;
			DelayedTask.run(serverLevel, delay, () -> {
				if (!entity.isAlive()) return;
				double radius = 2.0 + finalWave * 3.0;
				// 环形黑雾
				for (int i = 0; i < 30; i++) {
					double angle = 2 * Math.PI * i / 30;
					double x = center.x + Math.cos(angle) * radius;
					double z = center.z + Math.sin(angle) * radius;
					double y = center.y + (RANDOM.nextDouble() - 0.5) * 4.0;

					serverLevel.sendParticles(
							BLACK_FOG, x, y, z,
							1, 0.5, 0.3, 0.5, 0.01
					);
					// 混杂暗紫色粒子
					if (RANDOM.nextFloat() < 0.3f) {
						serverLevel.sendParticles(
								DARK_PURPLE, x, y, z,
								1, 0.3, 0.3, 0.3, 0.01
						);
					}
				}
			});
		}

		// 黑雾从天而降的粒子
		DelayedTask.run(serverLevel, 10, () -> {
			if (!entity.isAlive()) return;
			for (int i = 0; i < 40; i++) {
				double range = CONVERSION_RADIUS * 0.8;
				Vec3 pos = center.add(
						(RANDOM.nextDouble() - 0.5) * range * 2,
						-RANDOM.nextDouble() * 12.0,
						(RANDOM.nextDouble() - 0.5) * range * 2
				);
				serverLevel.sendParticles(
						ParticleTypes.SMOKE, pos.x, pos.y, pos.z,
						2, 0.3, 0.3, 0.3, 0.02
				);
			}
		});
	}

	/**
	 * 执行村民转换
	 * 球形范围内村民随机转换成拉集团以下的古朗基
	 */
	private void executeConversion() {
		if (!entity.isAlive() || entity.level().isClientSide()) {
			KamenRiderInterwovenWorldsMod.LOGGER.debug("[究极黑暗] executeConversion: entity not alive or client side");
			return;
		}
		if (!(entity.level() instanceof ServerLevel serverLevel)) {
			KamenRiderInterwovenWorldsMod.LOGGER.warn("[究极黑暗] executeConversion: level is not ServerLevel!");
			return;
		}

		Vec3 center = entity.position();

		// 先用AABB粗筛（全半径范围），再用球形距离精筛
		AABB area = new AABB(
				center.x - CONVERSION_RADIUS, center.y - CONVERSION_RADIUS, center.z - CONVERSION_RADIUS,
				center.x + CONVERSION_RADIUS, center.y + CONVERSION_RADIUS, center.z + CONVERSION_RADIUS
		);

		// 收集所有可转换目标（村民 + 灾厄村民）
		List<LivingEntity> convertibleTargets = new ArrayList<>();

		List<Villager> allVillagers = serverLevel.getEntitiesOfClass(Villager.class, area, Villager::isAlive);
		List<AbstractIllager> allIllagers = serverLevel.getEntitiesOfClass(AbstractIllager.class, area, AbstractIllager::isAlive);
		KamenRiderInterwovenWorldsMod.LOGGER.debug("[究极黑暗] executeConversion: found {} villagers and {} illagers in AABB range {}",
				allVillagers.size(), allIllagers.size(), CONVERSION_RADIUS);

		// 球形距离过滤
		double radiusSqr = CONVERSION_RADIUS * CONVERSION_RADIUS;
		List<Vec3> targetPositions = new ArrayList<>();

		for (Villager v : allVillagers) {
			if (v.distanceToSqr(entity) <= radiusSqr) {
				convertibleTargets.add(v);
			} else {
				KamenRiderInterwovenWorldsMod.LOGGER.debug("[究极黑暗] Villager {} is in AABB but out of sphere range: distSqr={} > {}", 
						v.getStringUUID(), v.distanceToSqr(entity), radiusSqr);
			}
		}
		for (AbstractIllager i : allIllagers) {
			if (i.distanceToSqr(entity) <= radiusSqr) {
				convertibleTargets.add(i);
			} else {
				KamenRiderInterwovenWorldsMod.LOGGER.debug("[究极黑暗] Illager {} is in AABB but out of sphere range: distSqr={} > {}", 
						i.getStringUUID(), i.distanceToSqr(entity), radiusSqr);
			}
		}

		KamenRiderInterwovenWorldsMod.LOGGER.debug("[究极黑暗] executeConversion: {} convertible targets in spherical range (radius={})",
				convertibleTargets.size(), CONVERSION_RADIUS);

		if (convertibleTargets.isEmpty()) {
			KamenRiderInterwovenWorldsMod.LOGGER.debug("[究极黑暗] No convertible targets in range, aborting conversion");
			return;
		}

		// 可转换的古朗基类型（拉集团以下）
		List<EntityType<? extends Gurongi>> availableTypes = getAvailableGurongiTypes();
		KamenRiderInterwovenWorldsMod.LOGGER.debug("[究极黑暗] Available gurongi types: {}", availableTypes.size());
		if (availableTypes.isEmpty()) {
			KamenRiderInterwovenWorldsMod.LOGGER.warn("[究极黑暗] No available gurongi types to convert to!");
			return;
		}

		int convertedCount = 0;

		for (LivingEntity target : convertibleTargets) {
			// 随机选择一种古朗基类型
			EntityType<? extends Gurongi> gurongiType = availableTypes.get(RANDOM.nextInt(availableTypes.size()));
			KamenRiderInterwovenWorldsMod.LOGGER.debug("[究极黑暗] Converting {} {} -> gurongi type {}",
					target instanceof Villager ? "villager" : "illager",
					target.getStringUUID(), gurongiType.getDescriptionId());

			// 生成古朗基实体
			Gurongi gurongi = gurongiType.create(serverLevel);
			if (gurongi == null) {
				KamenRiderInterwovenWorldsMod.LOGGER.warn("[究极黑暗] Failed to create gurongi entity from type {}",
						gurongiType.getDescriptionId());
				continue;
			}

			// 记录目标位置
			targetPositions.add(new Vec3(target.getX(), target.getY(), target.getZ()));

			// 设置位置到目标的位置
			gurongi.setPos(target.getX(), target.getY(), target.getZ());

			// 属性调整为原版的50%
			double originalMaxHealth = gurongi.getAttributeBaseValue(Attributes.MAX_HEALTH);
			double originalAttackDamage = gurongi.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
			double originalArmor = gurongi.getAttributeBaseValue(Attributes.ARMOR);
			double originalSpeed = gurongi.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);

			gurongi.getAttribute(Attributes.MAX_HEALTH).setBaseValue(originalMaxHealth * 0.5);
			gurongi.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(originalAttackDamage * 0.5);
			gurongi.getAttribute(Attributes.ARMOR).setBaseValue(originalArmor * 0.5);
			gurongi.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(originalSpeed * 0.5);

			// 设置生命值为最大值（满血生成）
			gurongi.setHealth((float) (originalMaxHealth * 0.5));

			// 设置狼王标记（持久化数据）
			gurongi.getPersistentData().putString("wolfKingId", entity.getStringUUID());

			// 添加到狼王的管理列表
			entity.addWolfPackMember(gurongi.getUUID());

			// 生成到世界
			serverLevel.addFreshEntity(gurongi);

			// 移除原始目标
			target.remove(Entity.RemovalReason.DISCARDED);

			convertedCount++;
		}

		// 播放转换音效
		if (convertedCount > 0) {
			KamenRiderInterwovenWorldsMod.LOGGER.debug("[究极黑暗] Successfully converted {} villagers to gurongi!", convertedCount);
			serverLevel.playSound(null, center.x, center.y, center.z,
					SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.HOSTILE, 1.5f, 0.3f);

			// 每个被转换的目标位置生成爆炸粒子
			for (Vec3 pos : targetPositions) {
				serverLevel.sendParticles(
						ParticleTypes.LARGE_SMOKE,
						pos.x, pos.y + 1.0, pos.z,
						15, 0.5, 0.5, 0.5, 0.05
				);
			}
		}
	}

	/**
	 * 获取可转换的古朗基类型（拉集团以下 = NU, GO, ME, ZU, BE）
	 * 目前仅实现了ZU集团，后续可扩展
	 */
	private List<EntityType<? extends Gurongi>> getAvailableGurongiTypes() {
		return NGamioZedaEntity.getAvailableGurongiTypes();
	}
}