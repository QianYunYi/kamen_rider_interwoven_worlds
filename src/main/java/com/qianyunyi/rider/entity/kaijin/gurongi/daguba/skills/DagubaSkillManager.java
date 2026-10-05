package com.qianyunyi.rider.entity.kaijin.gurongi.daguba.skills;

import com.qianyunyi.rider.entity.kaijin.gurongi.daguba.NDagubaZebaEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

/**
 * 达古巴技能管理器
 * 集中管理所有技能的冷却、优先级和执行
 */
public class DagubaSkillManager {
    private final NDagubaZebaEntity entity;
    private final Map<Class<? extends AbstractDagubaSkill>, Integer> cooldowns = new HashMap<>();
    private final List<AbstractDagubaSkill> skills = new ArrayList<>();
    private AbstractDagubaSkill currentSkill;
    private final Random random = new Random();

    public DagubaSkillManager(NDagubaZebaEntity entity) {
        this.entity = entity;
        registerSkills();
    }

    private void registerSkills() {
        addSkill(new SupernaturalFireSkill(entity));
        addSkill(new SlaughterWaveSkill(entity));
        addSkill(new DarkWavePalmSkill(entity));
    }

    private void addSkill(AbstractDagubaSkill skill) {
        skills.add(skill);
        cooldowns.put(skill.getClass(), 0);
    }

    /**
     * 每tick更新冷却
     */
    public void tick() {
        cooldowns.replaceAll((k, v) -> v > 0 ? v - 1 : 0);
    }

    /**
     * 获取按优先级排序的可用技能列表
     */
    public List<AbstractDagubaSkill> getAvailableSkills(LivingEntity target) {
        List<AbstractDagubaSkill> available = new ArrayList<>();
        for (AbstractDagubaSkill skill : skills) {
            if (getCooldown(skill) <= 0 && skill.canExecute(target)) {
                available.add(skill);
            }
        }
        available.sort(Comparator.comparingInt(AbstractDagubaSkill::getPriority));
        return available;
    }

    /**
     * 执行指定技能
     */
    public boolean executeSkill(AbstractDagubaSkill skill) {
        LivingEntity currentTarget = entity.getTarget();
        if (getCooldown(skill) > 0 || !skill.canExecute(currentTarget)) {
            return false;
        }
        setCooldown(skill, skill.getCooldown());
        skill.execute();
        currentSkill = skill;
        return true;
    }

    /**
     * 尝试自动选择并执行最佳技能
     * 在可用技能中随机选择一个，避免总是使用优先级最高的技能
     */
    public boolean tryAutoExecute(LivingEntity target, double distance) {
        List<AbstractDagubaSkill> available = getAvailableSkills(target);
        available.removeIf(skill -> distance > skill.getMaxRange());
        if (available.isEmpty()) {
            return false;
        }

        // 按优先级加权随机选择：优先级越高（数字越小）权重越大
        int totalWeight = 0;
        for (AbstractDagubaSkill skill : available) {
            totalWeight += Math.max(1, 10 - skill.getPriority());
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (AbstractDagubaSkill skill : available) {
            cumulative += Math.max(1, 10 - skill.getPriority());
            if (roll < cumulative) {
                return executeSkill(skill);
            }
        }

        return executeSkill(available.get(0));
    }

    public int getCooldown(AbstractDagubaSkill skill) {
        return cooldowns.getOrDefault(skill.getClass(), 0);
    }

    public void setCooldown(AbstractDagubaSkill skill, int cooldown) {
        cooldowns.put(skill.getClass(), cooldown);
    }

    public void resetAllCooldowns() {
        cooldowns.replaceAll((k, v) -> 0);
    }

    public boolean isAnySkillExecuting() {
        return currentSkill != null && currentSkill.isExecuting();
    }

    public void save(CompoundTag tag) {
        // 保存冷却时间
        CompoundTag cooldownTag = new CompoundTag();
        for (Map.Entry<Class<? extends AbstractDagubaSkill>, Integer> entry : cooldowns.entrySet()) {
            cooldownTag.putInt(entry.getKey().getSimpleName(), entry.getValue());
        }
        tag.put("SkillCooldowns", cooldownTag);
    }

    public void load(CompoundTag tag) {
        if (tag.contains("SkillCooldowns")) {
            CompoundTag cooldownTag = tag.getCompound("SkillCooldowns");
            for (String key : cooldownTag.getAllKeys()) {
                for (AbstractDagubaSkill skill : skills) {
                    if (skill.getClass().getSimpleName().equals(key)) {
                        cooldowns.put(skill.getClass(), cooldownTag.getInt(key));
                    }
                }
            }
        }
    }
}