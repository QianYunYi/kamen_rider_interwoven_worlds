package com.qianyunyi.rider.entity.kaijin.gurongi.daguba.skills;

import com.qianyunyi.rider.entity.kaijin.gurongi.daguba.NDagubaZebaEntity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 达古巴技能抽象基类
 * 统一所有技能的生命周期管理
 */
public abstract class AbstractDagubaSkill {
    protected final NDagubaZebaEntity entity;
    protected LivingEntity target;
    protected boolean isExecuting;

    protected AbstractDagubaSkill(NDagubaZebaEntity entity) {
        this.entity = entity;
    }

    /**
     * 检查技能是否可以执行
     * @param target 当前目标（可能为null）
     */
    public boolean canExecute(LivingEntity target) {
        return true;
    }

    /**
     * 获取技能冷却时间（tick）
     */
    public abstract int getCooldown();

    /**
     * 获取技能优先级（数字越小优先级越高）
     */
    public abstract int getPriority();

    /**
     * 获取技能最大施放距离
     */
    public abstract double getMaxRange();

    /**
     * 是否需要面向目标才能施放
     */
    public boolean requiresFacingTarget() {
        return false;
    }

    /**
     * 开始执行技能
     */
    public void execute() {
        this.target = entity.getTarget();
        if (target == null || !canExecute(target)) {
            return;
        }
        // 执行技能前强制面向目标
        if (target != null) {
            rotateToTarget(target);
        }
        isExecuting = true;
        entity.setUsingSkill(true);
        onStart();
    }

    /**
     * 强制实体旋转面向目标（同时设置头部旋转和身体旋转）
     */
    private void rotateToTarget(LivingEntity target) {
        entity.getLookControl().setLookAt(target, 60.0F, 60.0F);
        double dx = target.getX() - entity.getX();
        double dz = target.getZ() - entity.getZ();
        float yaw = (float) (Math.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
        entity.setYRot(yaw);
        entity.yBodyRot = yaw;
        entity.yBodyRotO = yaw;
    }

    /**
     * 技能具体逻辑，子类实现
     */
    protected abstract void onStart();

    /**
     * 技能执行完毕时调用
     */
    protected void onFinish() {
        isExecuting = false;
        entity.setUsingSkill(false);
    }

    public boolean isExecuting() {
        return isExecuting;
    }
}