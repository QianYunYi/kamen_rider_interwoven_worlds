package com.qianyunyi.rider.entity.kaijin.gurongi.gamio.skills;

import com.qianyunyi.rider.entity.kaijin.gurongi.gamio.NGamioZedaEntity;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModParticleTypes;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModParticles;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.YellowLightningSyncMessage;
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
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.List;
import java.util.Random;

public class ThunderExplosionSkill extends AbstractGamioSkill {
	private static final Random random = new Random();

	// ===== 粒子定义 =====
	// 核心粒子（金色）
	private static final DustParticleOptions BEAM_CORE = new DustParticleOptions(
			new Vector3f(1.0f, 0.95f, 0.45f), 2.5f);
	// 内层高能粒子（亮白金色）
	private static final DustParticleOptions BEAM_HIGH = new DustParticleOptions(
			new Vector3f(1.0f, 1.0f, 0.7f), 3.5f);
	// 外层散射粒子（金色）
	private static final DustParticleOptions BEAM_OUTER = new DustParticleOptions(
			new Vector3f(1.0f, 0.7f, 0.0f), 1.8f);
	// 绿色闪电粒子
	private static final DustParticleOptions BEAM_GREEN = new DustParticleOptions(
			new Vector3f(0.2f, 0.9f, 0.2f), 2.0f);

	private static final int BEAM_LENGTH = 24;       // 粒子束最长距离
	private static final int CONE_ANGLE_DEG = 25;     // 锥角（度）
	private static final int BEAM_PARTICLES = 80;     // 每波粒子数
	private static final int DAMAGE_HIT_COUNT = 5;    // 穿透伤害次数

	public ThunderExplosionSkill(NGamioZedaEntity entity) {
		super(entity);
	}

	@Override
	public boolean canExecute(LivingEntity target) {
		return target != null && target.isAlive() && entity.hasLineOfSight(target);
	}

	@Override
	public int getCooldown() {
		return 300;
	}

	@Override
	public int getPriority() {
		return 1;
	}

	@Override
	public double getMaxRange() {
		return 24.0;
	}

	@Override
	public int getSkillLockDuration() {
		return 40; // 2S
	}

	@Override
	protected void onStart() {
		boolean isFloating = entity.isFloating();
		String animName = isFloating ? "thunder_explosion" : "thunder_explosion2";
		entity.setAnimation(animName, 40); // 技能锁时长=40 ticks

		entity.showGreenLightningTemporary();
		entity.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.5f, 0.6f);

		// 第1步：施法前摇 — 触发CHARGING_RING绿色环身闪电渲染
		sendLightningSync(3);

		// 等待动画播放到合适时机（1.5S后发射粒子束）
		DelayedTask.run(entity.level(), 30, () -> {
			if (!entity.isAlive()) {
				sendLightningSync(4); // 清理
				onFinish();
				return;
			}

			if (entity.level() instanceof ServerLevel serverLevel) {
				// 计算发射方向（与粒子束方向一致）
				Vec3 dir = getBeamDirection();

				// 第2步：爆炸触发 — 发送带方向的FLYING闪电渲染
				sendLightningSyncWithDir(1, dir);

				// 锥形粒子束（内部包含穿透伤害）
				createThunderExplosion(serverLevel);
			}

			entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
					SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0f, 0.5f);

			// 第3步：清理闪电渲染
			DelayedTask.run(entity.level(), 30, () -> sendLightningSync(4));

			onFinish();
		});
	}

	/**
	 * 计算光束发射方向
	 */
	private Vec3 getBeamDirection() {
		LivingEntity target = entity.getTarget();
		if (target != null) {
			return target.position()
					.add(0, target.getBbHeight() * 0.3, 0)
					.subtract(entity.position().add(0, entity.getEyeHeight() * 0.5, 0))
					.normalize();
		}
		return entity.getLookAngle();
	}

	/**
	 * 发送闪电渲染同步消息到所有追踪此实体的客户端
	 */
	private void sendLightningSync(int phase) {
		Vec3 pos = entity.position();
		YellowLightningSyncMessage msg = new YellowLightningSyncMessage(
				entity.getUUID(),
				phase,
				pos.x, pos.y, pos.z,
				0, 0, 0,
				0,
				entity.level().getGameTime()
		);
		NetworkHandler.CHANNEL.send(
				PacketDistributor.TRACKING_ENTITY.with(() -> entity),
				msg
		);
	}

	/**
	 * 发送带方向的FLYING闪电同步消息
	 */
	private void sendLightningSyncWithDir(int phase, Vec3 direction) {
		Vec3 pos = entity.position();
		YellowLightningSyncMessage msg = new YellowLightningSyncMessage(
				entity.getUUID(),
				phase,
				pos.x, pos.y, pos.z,
				direction.x, direction.y, direction.z,
				0,
				entity.level().getGameTime()
		);
		NetworkHandler.CHANNEL.send(
				PacketDistributor.TRACKING_ENTITY.with(() -> entity),
				msg
		);
	}

	/**
	 * 创建锥形粒子束（雷爆核心效果）
	 * 从实体发射锥形粒子光束，向前穿透，粒子从密集到扩散
	 */
	private void createThunderExplosion(ServerLevel serverLevel) {
		// 确定发射方向：朝向目标
		Vec3 direction = getBeamDirection();

		Vec3 origin = entity.position().add(0, entity.getBbHeight() * 0.4, 0);
		double coneRad = Math.toRadians(CONE_ANGLE_DEG);

		// 构建锥体基向量：forward、up、right
		Vec3 forward = direction;
		Vec3 upBase;
		if (Math.abs(forward.y) > 0.99) {
			upBase = new Vec3(1, 0, 0);
		} else {
			upBase = new Vec3(0, 1, 0);
		}
		Vec3 right = forward.cross(upBase).normalize();
		Vec3 up = right.cross(forward).normalize();

		// ===== 第1波：中心密集核心光束（先发射，速度快）=====
		// 密集的直线粒子束，从原点向前延伸
		spawnCoreBeam(serverLevel, origin, forward, up, right);

		// ===== 第2波：锥形扩散粒子束（延迟，锥形展开）=====
		DelayedTask.run(serverLevel, 2, () -> {
			if (!serverLevel.isLoaded(entity.blockPosition())) return;
			spawnConeBeam(serverLevel, origin, forward, up, right, coneRad);
		});

		// ===== 第3波：第二次锥形扩散（更大角度）=====
		DelayedTask.run(serverLevel, 5, () -> {
			if (!serverLevel.isLoaded(entity.blockPosition())) return;
			spawnConeBeam(serverLevel, origin, forward, up, right, coneRad * 1.6);
		});

		// ===== 第4波：大扩散闪电散射（延迟最高）=====
		DelayedTask.run(serverLevel, 8, () -> {
			if (!serverLevel.isLoaded(entity.blockPosition())) return;
			spawnScatterLightning(serverLevel, origin, forward, up, right);
		});

		// ===== 穿透伤害 =====
		applyBeamDamage(serverLevel, origin, forward, BEAM_LENGTH);
	}

	/**
	 * 核心光束：密集直线核心粒子，向前穿透
	 */
	private void spawnCoreBeam(ServerLevel serverLevel, Vec3 origin,
							   Vec3 forward, Vec3 up, Vec3 right) {
		for (int i = 0; i < 40; i++) {
			// 沿直线分布，越远越稀疏
			double t = random.nextDouble() * 0.9;
			double dist = t * BEAM_LENGTH;
			// 微小的随机偏移（核心束很集中）
			double spread = 0.15 + t * 0.4;
			double offX = (random.nextDouble() - 0.5) * spread;
			double offY = (random.nextDouble() - 0.5) * spread;
			Vec3 pos = origin.add(forward.scale(dist))
					.add(right.scale(offX))
					.add(up.scale(offY));

			if (!serverLevel.isLoaded(entity.blockPosition())) return;

			// 核心高亮粒子
			boolean highEnergy = random.nextFloat() < 0.3f;
			serverLevel.sendParticles(
					highEnergy ? BEAM_HIGH : BEAM_CORE,
					pos.x, pos.y, pos.z,
					1, 0.02, 0.02, 0.02, 0.005);

			// 沿路径生成电弧
			if (random.nextFloat() < 0.3f) {
				Vec3 arcEnd = pos.add(
						(random.nextDouble() - 0.5) * 1.5,
						(random.nextDouble() - 0.5) * 1.5,
						(random.nextDouble() - 0.5) * 1.5);
				spawnLightningArc(serverLevel, pos, arcEnd);
			}
		}
	}

	/**
	 * 锥形扩散光束：粒子沿锥体分布，越远越扩散
	 */
	private void spawnConeBeam(ServerLevel serverLevel, Vec3 origin,
							   Vec3 forward, Vec3 up, Vec3 right, double coneAngle) {
		for (int i = 0; i < BEAM_PARTICLES; i++) {
			// 距离：从近到远，但远距离密度降低
			double dist;
			if (random.nextFloat() < 0.5f) {
				// 一半粒子集中在近-中距离 (0~60%)
				dist = random.nextDouble() * 0.6 * BEAM_LENGTH;
			} else {
				dist = random.nextDouble() * BEAM_LENGTH;
			}

			// 锥形扩散：距离越远，偏移半径越大
			double coneRadius = Math.tan(coneAngle) * dist;
			// 在锥形截面圆内随机分布（偏向边缘让锥体更明显）
			double r = coneRadius * Math.sqrt(random.nextDouble());
			double theta = random.nextDouble() * 2 * Math.PI;

			double offX = r * Math.cos(theta);
			double offY = r * Math.sin(theta) * 0.7; // 垂直方向略压缩

			Vec3 pos = origin
					.add(forward.scale(dist))
					.add(right.scale(offX))
					.add(up.scale(offY));

			if (!serverLevel.isLoaded(entity.blockPosition())) return;

			// 根据在锥体中的位置选择粒子类型
			// 中心区域高能，边缘散射（绿色闪电粒子由客户端渲染）
			double normalizedR = coneRadius > 0.01 ? r / coneRadius : 0;
			DustParticleOptions particle;
			if (normalizedR < 0.3) {
				particle = random.nextFloat() < 0.3f ? BEAM_HIGH : BEAM_CORE;
			} else if (normalizedR < 0.7) {
				particle = BEAM_CORE;
			} else {
				particle = random.nextFloat() < 0.3f ? BEAM_CORE : BEAM_OUTER;
			}

			serverLevel.sendParticles(particle,
					pos.x, pos.y, pos.z,
					1, 0.03, 0.03, 0.03, 0.005);

			// 远距离粒子更大
			if (dist > BEAM_LENGTH * 0.5 && random.nextFloat() < 0.2f) {
				serverLevel.sendParticles(particle,
						pos.x + (random.nextDouble() - 0.5) * 0.3,
						pos.y + (random.nextDouble() - 0.5) * 0.3,
						pos.z + (random.nextDouble() - 0.5) * 0.3,
						2, 0.05, 0.05, 0.05, 0.01);
			}
		}
	}

	/**
	 * 大扩散闪电散射：锥体周围的闪电弧线和散落粒子
	 */
	private void spawnScatterLightning(ServerLevel serverLevel, Vec3 origin,
									   Vec3 forward, Vec3 up, Vec3 right) {
		double wideAngle = Math.toRadians(CONE_ANGLE_DEG * 2.5);
		for (int i = 0; i < 30; i++) {
			double dist = 3.0 + random.nextDouble() * BEAM_LENGTH * 0.8;
			double coneRadius = Math.tan(wideAngle) * dist;
			double r = coneRadius * Math.sqrt(random.nextDouble());
			double theta = random.nextDouble() * 2 * Math.PI;

			Vec3 scatterPos = origin
					.add(forward.scale(dist))
					.add(right.scale(r * Math.cos(theta)))
					.add(up.scale(r * Math.sin(theta) * 0.7));

			if (!serverLevel.isLoaded(entity.blockPosition())) return;

			// 闪电电弧
			Vec3 arcEnd = scatterPos.add(
					(random.nextDouble() - 0.5) * 4.0,
					(random.nextDouble() - 0.5) * 3.0,
					(random.nextDouble() - 0.5) * 4.0);
			spawnLightningArc(serverLevel, scatterPos, arcEnd);

			// 散射粒子
			serverLevel.sendParticles(
					random.nextFloat() < 0.3f ? ParticleTypes.ELECTRIC_SPARK : BEAM_OUTER,
					scatterPos.x, scatterPos.y, scatterPos.z,
					2, 0.15, 0.15, 0.15, 0.02);
		}

		// 中心光束残留高能粒子
		Vec3 midPoint = origin.add(forward.scale(BEAM_LENGTH * 0.5));
		for (int i = 0; i < 10; i++) {
			Vec3 p = midPoint.add(
					(random.nextDouble() - 0.5) * 3.0,
					(random.nextDouble() - 0.5) * 2.0,
					(random.nextDouble() - 0.5) * 3.0);
			serverLevel.sendParticles(BEAM_HIGH,
					p.x, p.y, p.z,
					2, 0.1, 0.1, 0.1, 0.01);

			// 大闪电弧
			Vec3 arcEnd = p.add(
					(random.nextDouble() - 0.5) * 6.0,
					(random.nextDouble() - 0.5) * 5.0,
					(random.nextDouble() - 0.5) * 6.0);
			spawnLightningArc(serverLevel, p, arcEnd);
		}
	}

	/**
	 * 生成闪电弧线
	 */
	private void spawnLightningArc(ServerLevel serverLevel, Vec3 start, Vec3 end) {
		int segments = 6 + random.nextInt(6);
		Vec3 direction = end.subtract(start).scale(1.0 / segments);

		Vec3 current = start;
		for (int i = 0; i < segments; i++) {
			Vec3 next = current.add(direction);
			next = next.add(
					(random.nextDouble() - 0.5) * 0.8,
					(random.nextDouble() - 0.5) * 0.5,
					(random.nextDouble() - 0.5) * 0.8);

			serverLevel.sendParticles(
					KamenRiderInterwovenWorldsModParticleTypes.LIGHTNING.get(),
					next.x, next.y, next.z,
					2, 0, 0, 0, 0.02);

			current = next;
		}
	}

	/**
	 * 穿透伤害：沿光束路径对所有实体造成多次伤害（穿透效果）
	 */
	private void applyBeamDamage(ServerLevel serverLevel, Vec3 origin, Vec3 direction, double maxDist) {
		float baseDamage = (float) entity.getAttributeValue(Attributes.ATTACK_DAMAGE);

		AABB beamAABB = new AABB(
				origin.x - 3, origin.y - 3, origin.z - 3,
				origin.x + maxDist + 3, origin.y + maxDist + 3, origin.z + maxDist + 3);

		List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, beamAABB);

		for (LivingEntity targetEntity : entities) {
			if (targetEntity == entity || !targetEntity.isAlive()) continue;

			// 检查目标是否在光束锥形范围内
			Vec3 toTarget = targetEntity.position().add(0, targetEntity.getBbHeight() * 0.3, 0)
					.subtract(origin);
			double dist = toTarget.length();
			if (dist > maxDist || dist < 0.5) continue;

			// 计算与光束轴线的垂直距离
			Vec3 projection = direction.scale(toTarget.dot(direction));
			Vec3 rejection = toTarget.subtract(projection);
			double offAxisDist = rejection.length();

			// 锥形判定：距离越远，允许的偏离越大
			double maxOffAxis = Math.tan(Math.toRadians(CONE_ANGLE_DEG * 1.2)) * dist;
			if (offAxisDist > maxOffAxis + 1.5) continue;

			// 距离衰减伤害（穿透：近处伤害高，远处伤害低）
			float distanceFactor = 1.0f - (float) (dist / maxDist) * 0.5f;
			float damage = baseDamage * 0.4f * Math.max(0.2f, distanceFactor);

			// 穿透：多次伤害命中
			for (int hit = 0; hit < DAMAGE_HIT_COUNT; hit++) {
				if (!targetEntity.isAlive()) break;
				int finalHit = hit;
				float finalDamage = damage * 0.3f;
				DelayedTask.run(serverLevel, finalHit * 3, () -> {
					if (!targetEntity.isAlive()) return;
					targetEntity.invulnerableTime = 0;
					targetEntity.hurt(entity.damageSources().mobAttack(entity), finalDamage);

					// 击退：沿光束方向
					targetEntity.push(direction.x * 0.3 + 0.1, 0.15, direction.z * 0.3 + 0.1);
				});
			}
		}
	}
}