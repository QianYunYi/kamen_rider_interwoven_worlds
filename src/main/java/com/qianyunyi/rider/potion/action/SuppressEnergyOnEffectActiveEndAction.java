package com.qianyunyi.rider.potion.action;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModParticleTypes;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

/**
 * 当镇压能量效果结束时触发的过程
 */
public class SuppressEnergyOnEffectActiveEndAction {

	// 存储初始生命值的键
	private static final String INITIAL_HEALTH_KEY = "suppress_energy_initial_health";

	// 形态常量
	private static final String FORM_TAG_KEY = "form";
	private static final String AMAZING_MIGHTY_FORM = "amazing_mighty_form";

	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null) return;

		// 获取粒子类型
		SimpleParticleType particle = KamenRiderInterwovenWorldsModParticleTypes.SUPPRESS_ENERGY_PARTICLE.get();

		// 计算基础位置
		double x = entity.getX();
		double y = entity.getY() + entity.getBbHeight() / 1.25;
		double z = entity.getZ();

		// 检查是否为服务器端世界
		if (!(world instanceof ServerLevel serverLevel)) return;
		serverLevel.playSound(null, entity.blockPosition(),
				KamenRiderInterwovenWorldsModSounds.KUUGA_AFTER_HISATSU_EFFECT.get(), SoundSource.PLAYERS, 1, 1);

		// 读取形态标签，惊异全能形态显示两个错开的镇压粒子
		String form = entity.getPersistentData().getString(FORM_TAG_KEY);
		if (AMAZING_MIGHTY_FORM.equals(form)) {
			spawnTwoStaggeredParticles(serverLevel, particle, entity, y);
		} else {
			// 其他形态：原有逻辑，在实体上方显示一个粒子
			spawnParticle(serverLevel, particle, x, y, z);
		}

		// 获取当前镇压能量
		double suppressPower = entity.getPersistentData().getDouble("suppress_power");
		// 增加镇压能量
		double newSuppressPower = Math.min(suppressPower + 10, 2000);
		// 设置新的镇压能量值
		entity.getPersistentData().putDouble("suppress_power", newSuppressPower);
		// 移除初始生命值记录
		entity.getPersistentData().remove(INITIAL_HEALTH_KEY);
	}

	/**
	 * 惊异全能形态：在实体上方生成两个临近错开的镇压粒子效果
	 * 两个粒子以约22.5度夹角错开，临近显示
	 */
	private static void spawnTwoStaggeredParticles(ServerLevel level, SimpleParticleType particle,
												   Entity entity, double baseY) {
		double x = entity.getX();
		double z = entity.getZ();

		// 小半径：两个粒子靠得很近
		double radius = 1.0;

		// 22.5度 = PI/8，两个粒子错开这个角度
		double angleStep = Math.PI / 8.0;

		// 粒子1：左侧偏下
		double x1 = x + Math.cos(0) * radius;
		double z1 = z + Math.sin(0) * radius;
		spawnParticle(level, particle, x1, baseY, z1);

		// 粒子2：以22.5度错开，右上侧，临近显示
		double x2 = x + Math.cos(angleStep) * radius;
		double z2 = z + Math.sin(angleStep) * radius;
		spawnParticle(level, particle, x2, baseY, z2);
	}

	private static void spawnParticle(ServerLevel level, SimpleParticleType particle,
									  double x, double y, double z) {
		level.sendParticles(particle, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
	}
}