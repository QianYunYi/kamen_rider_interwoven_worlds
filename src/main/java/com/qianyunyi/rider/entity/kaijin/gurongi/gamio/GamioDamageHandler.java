package com.qianyunyi.rider.entity.kaijin.gurongi.gamio;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;

public class GamioDamageHandler {

	public static boolean handleDamage(NGamioZedaEntity entity, DamageSource source, float amount) {
		if (entity.isStunned()) {
			return entity.superHurt(source, amount);
		}

		float minDamage = entity.getMaxHealth() * 0.005f;
		if (amount < minDamage) {
			return false;
		}

		if (source.is(DamageTypeTags.IS_FALL) ||
				source.is(DamageTypeTags.IS_DROWNING) ||
				source.is(DamageTypeTags.IS_FREEZING)) {
			return false;
		}

		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return entity.superHurt(source, amount);
		}

		float maxDamage = entity.getMaxHealth() * 0.08f;
		if (amount > maxDamage) {
			amount = maxDamage;
		}

		if (source.is(DamageTypeTags.IS_FIRE)) {
			amount *= 0.5f;
		}

		// 浮空状态下，记录伤害计数
		if (entity.isFloating()) {
			entity.onDamageDuringFlight();
		}

		// 加米奥已移除瞬移被动，直接承受伤害
		return entity.superHurt(source, amount);
	}
}