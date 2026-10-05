package com.qianyunyi.rider.entity.kaijin.damage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.HashMap;
import java.util.Map;

/**
 * 伤害适应性
 * 机制：
 * 1. 相同伤害值重复命中 → 减伤递增（每次+20%，最高100%）
 * 2. 受到不同伤害值时 → 该伤害全额生效，其他伤害值的适应度衰减5%
 * 3. 最低减伤0%，击破时重置
 */
public class DynamicDamageAdaptationSystem {

    private static final float REDUCTION_PER_HIT = 0.10f;  // 每次命中+20%减伤
    private static final float CROSS_DECAY_RATE = 0.05f;   // 交叉衰减5%
    private static final float MAX_REDUCTION = 1.0f;       // 最高100%减伤

    private static class DamageRecord {
        public final float damageValue;
        public float reductionLevel;  // 0.0 ~ 1.0 的减伤等级

        public DamageRecord(float damageValue) {
            this.damageValue = damageValue;
            this.reductionLevel = 0.0f;
        }

        public float getReduction() {
            return Math.min(MAX_REDUCTION, reductionLevel);
        }

        // 增加适应度
        public void increase() {
            this.reductionLevel = Math.min(MAX_REDUCTION, this.reductionLevel + REDUCTION_PER_HIT);
        }

        // 交叉衰减（受到其他伤害时）
        public void decay() {
            this.reductionLevel = Math.max(0.0f, this.reductionLevel - CROSS_DECAY_RATE);
        }
    }

    private final Map<Float, DamageRecord> records = new HashMap<>();

    // 应用减伤
    public float applyReduction(float amount) {
        if (amount <= 0) return 0;
        DamageRecord record = records.get(amount);
        if (record == null) return amount;
        return amount * (1.0f - record.getReduction());
    }

    // 记录伤害 - 核心逻辑：同值递增，异值衰减
    public void recordDamage(float amount) {
        if (amount <= 0) return;

        // 1. 先对所有其他伤害值进行交叉衰减
        for (DamageRecord record : records.values()) {
            if (Math.abs(record.damageValue - amount) > 0.001f) { // 不是当前伤害值
                record.decay();
            }
        }

        // 2. 当前伤害值增加适应度
        DamageRecord currentRecord = records.computeIfAbsent(amount, DamageRecord::new);
        currentRecord.increase();
    }

    // 破防 / 重置
    public void reset() {
        records.clear();
    }

    // NBT 存档
    public void save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (DamageRecord r : records.values()) {
            CompoundTag c = new CompoundTag();
            c.putFloat("value", r.damageValue);
            c.putFloat("level", r.reductionLevel);  // 改为float存储
            list.add(c);
        }
        tag.put("ValueAdaptation", list);
    }

    public void load(CompoundTag tag) {
        records.clear();
        if (tag.contains("ValueAdaptation", Tag.TAG_LIST)) {
            ListTag list = tag.getList("ValueAdaptation", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag c = list.getCompound(i);
                float v = c.getFloat("value");
                float level = c.getFloat("level");  // 读取float
                DamageRecord r = new DamageRecord(v);
                r.reductionLevel = level;
                records.put(v, r);
            }
        }
    }
}