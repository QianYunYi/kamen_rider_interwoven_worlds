package com.qianyunyi.rider.entity.kaijin.gurongi.gamio.skills;

import com.qianyunyi.rider.entity.kaijin.gurongi.gamio.NGamioZedaEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

public class GamioSkillManager {
	private final NGamioZedaEntity entity;
	private final Map<Class<? extends AbstractGamioSkill>, Integer> cooldowns = new HashMap<>();
	private final List<AbstractGamioSkill> skills = new ArrayList<>();
	private AbstractGamioSkill currentSkill;
	private final Random random = new Random();

	public GamioSkillManager(NGamioZedaEntity entity) {
		this.entity = entity;
		registerSkills();
	}

	private void registerSkills() {
		addSkill(new SlaughterWaveSkill(entity));
		addSkill(new LightningWaveSkill(entity));
		addSkill(new ThunderExplosionSkill(entity));
		addSkill(new UltimateDarknessSkill(entity));
		addSkill(new WolfKingAbsorptionSkill(entity));
	}

	private void addSkill(AbstractGamioSkill skill) {
		skills.add(skill);
		cooldowns.put(skill.getClass(), 0);
	}

	public void tick() {
		cooldowns.replaceAll((k, v) -> v > 0 ? v - 1 : 0);
	}

	public List<AbstractGamioSkill> getAvailableSkills(LivingEntity target) {
		List<AbstractGamioSkill> available = new ArrayList<>();
		for (AbstractGamioSkill skill : skills) {
			if (getCooldown(skill) <= 0 && skill.canExecute(target)) {
				available.add(skill);
			}
		}
		available.sort(Comparator.comparingInt(AbstractGamioSkill::getPriority));
		return available;
	}

	public boolean executeSkill(AbstractGamioSkill skill) {
		LivingEntity currentTarget = entity.getTarget();
		if (getCooldown(skill) > 0 || !skill.canExecute(currentTarget)) {
			return false;
		}
		setCooldown(skill, skill.getCooldown());
		skill.execute();
		currentSkill = skill;
		return true;
	}

	public boolean tryAutoExecute(LivingEntity target, double distance) {
		List<AbstractGamioSkill> available = getAvailableSkills(target);
		available.removeIf(skill -> distance > skill.getMaxRange());
		if (available.isEmpty()) {
			return false;
		}

		int totalWeight = 0;
		for (AbstractGamioSkill skill : available) {
			totalWeight += Math.max(1, 10 - skill.getPriority());
		}

		int roll = random.nextInt(totalWeight);
		int cumulative = 0;
		for (AbstractGamioSkill skill : available) {
			cumulative += Math.max(1, 10 - skill.getPriority());
			if (roll < cumulative) {
				return executeSkill(skill);
			}
		}

		return executeSkill(available.get(0));
	}

	public int getCooldown(AbstractGamioSkill skill) {
		return cooldowns.getOrDefault(skill.getClass(), 0);
	}

	public void setCooldown(AbstractGamioSkill skill, int cooldown) {
		cooldowns.put(skill.getClass(), cooldown);
	}

	public void resetAllCooldowns() {
		cooldowns.replaceAll((k, v) -> 0);
	}

	public boolean isAnySkillExecuting() {
		return currentSkill != null && currentSkill.isExecuting();
	}

	/**
	 * 获取所有已注册的技能
	 */
	public List<AbstractGamioSkill> getSkills() {
		return java.util.Collections.unmodifiableList(skills);
	}

	/**
	 * 获取当前正在执行的技能的锁定时长
	 */
	public int getCurrentSkillLockDuration() {
		if (currentSkill != null && currentSkill.isExecuting()) {
			return currentSkill.getSkillLockDuration();
		}
		return 20; // 默认1S
	}

	public void save(CompoundTag tag) {
		CompoundTag cooldownTag = new CompoundTag();
		for (Map.Entry<Class<? extends AbstractGamioSkill>, Integer> entry : cooldowns.entrySet()) {
			cooldownTag.putInt(entry.getKey().getSimpleName(), entry.getValue());
		}
		tag.put("SkillCooldowns", cooldownTag);
	}

	public void load(CompoundTag tag) {
		if (tag.contains("SkillCooldowns")) {
			CompoundTag cooldownTag = tag.getCompound("SkillCooldowns");
			for (String key : cooldownTag.getAllKeys()) {
				for (AbstractGamioSkill skill : skills) {
					if (skill.getClass().getSimpleName().equals(key)) {
						cooldowns.put(skill.getClass(), cooldownTag.getInt(key));
					}
				}
			}
		}
	}
}