package com.qianyunyi.rider.entity.kaijin.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class ConfrontationStanceGoal extends Goal {
    private final Mob mob;
    private LivingEntity target;

    private static final double IDEAL_DIST = 3.5D;
    private static final double NO_CIRCLE_DIST = 2.8D;
    private static final double EXIT_DIST = 3.0D;
    private static final double MAX_DIST = 7.0D;
    private static final float TURN_SPEED = 20.0F;
    private static final float STRAFE_SPEED = 0.22F; // 已减速

    // 6秒 = 120 tick
    private static final int MAX_STANCE_TICKS = 120;
    private int stanceTickTimer;

    public ConfrontationStanceGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        if (target == null || target.isDeadOrDying()) return false;
        if (mob.hurtTime > 0) return false;

        double dist = mob.distanceTo(target);
        return dist > EXIT_DIST && dist < MAX_DIST;
    }

    @Override
    public boolean canContinueToUse() {
        if (target == null || target.isDeadOrDying()) return false;
        if (mob.hurtTime > 0) return false;
        if (stanceTickTimer >= MAX_STANCE_TICKS) return false;

        double dist = mob.distanceTo(target);
        return dist > EXIT_DIST && dist < MAX_DIST + 1;
    }

    @Override
    public void start() {
        this.target = mob.getTarget();
        this.stanceTickTimer = 0; // 重置计时
        mob.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.target = null;
        this.stanceTickTimer = 0;
        mob.setDeltaMovement(Vec3.ZERO);
        mob.getMoveControl().strafe(0, 0);
    }

    @Override
    public void tick() {
        if (target == null) return;

        // ======================
        // 超时强制停止对峙
        // ======================
        stanceTickTimer++;
        if (stanceTickTimer >= MAX_STANCE_TICKS) {
            stop();
            return;
        }

        // 朝向玩家
        Vec3 toTarget = target.position().subtract(mob.position()).normalize();
        float targetYaw = (float) (Math.atan2(-toTarget.x, toTarget.z) * (180F / Math.PI));
        mob.yBodyRot = smoothYaw(mob.yBodyRot, targetYaw, TURN_SPEED);
        mob.yHeadRot = mob.yBodyRot;
        mob.yHeadRotO = mob.yHeadRot;
        mob.setYBodyRot(mob.yBodyRot);
        mob.setYRot(mob.yBodyRot);

        double dist = mob.distanceTo(target);

        // 太近直接退出
        if (dist < NO_CIRCLE_DIST) {
            mob.getMoveControl().strafe(-0.25F, 0);
            stop();
            return;
        }

        // 左右横移
        Vec3 playerMove = new Vec3(target.getX() - target.xOld, 0, target.getZ() - target.zOld);
        Vec3 right = new Vec3(-toTarget.z, 0, toTarget.x);
        double dot = playerMove.dot(right);

        float sideways = 0;
        if (dot > 0.01D) {
            sideways = STRAFE_SPEED;
        } else if (dot < -0.01D) {
            sideways = -STRAFE_SPEED;
        }

        // 前后移动
        float forward = 0;
        if (dist < IDEAL_DIST - 0.3) {
            forward = -0.3F;
        } else if (dist > IDEAL_DIST + 0.8) {
            forward = 0.5F;
        }

        mob.getMoveControl().strafe(forward, sideways);
    }

    private float smoothYaw(float current, float target, float speed) {
        float diff = net.minecraft.util.Mth.wrapDegrees(target - current);
        diff = net.minecraft.util.Mth.clamp(diff, -speed, speed);
        return current + diff;
    }
}